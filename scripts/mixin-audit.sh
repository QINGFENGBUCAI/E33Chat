#!/usr/bin/env bash
# Mixin 注入目标审计（26.x 专用）。
#
# 用法:
#   scripts/mixin-audit.sh <versions/26.x-fabric> <minecraft-jar> [more-jars...]
# 例（26.3 一个 merged jar 就够；26.1/26.2 需要客户端 jar + 服务端 common jar，
# 服务端 mixin（如 CommandManagerMixin -> Commands）的类在 common 里）:
#   scripts/mixin-audit.sh versions/26.2-fabric \
#     ~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-clientonly-deobf/26.2/minecraft-clientonly-deobf-26.2.jar \
#     ~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-common-deobf/26.2/minecraft-common-deobf-26.2.jar
#
# 从预处理产物（build/preprocessed）提取全部 @Mixin 目标类、@Accessor/@Invoker
# 成员名、@Inject(method="...") 描述符，用 javap -p -s 对照官方 jar 逐一验证存在性。
# 有失配时退出码非 0。先跑 gradlew :<版本>-fabric:preprocessJava 生成产物。
#
# 仅适用于 26.x：官方未混淆映射，源码名与 jar 名一致。
# 1.x 版本用 yarn 名，jar 是 intermediary/混淆名，对不上（历史上靠实机验证）。
#
# 本脚本就是那轮 "26.3 实机启动崩溃（activeButton 字段消失）" 的教训的产物：
# mixin 描述符是字符串，javac 不校验，只有对照真实 jar 才能发现失配。
set -uo pipefail

VER_DIR="${1:?usage: mixin-audit.sh <version-dir> <mc-jar> [more-jars...]}"
shift
[ $# -ge 1 ] || { echo "need at least one mc jar"; exit 2; }
MIXIN_DIR="$VER_DIR/build/preprocessed/main/java/com/niuqu/chatbubble/mixin"
[ -d "$MIXIN_DIR" ] || { echo "preprocessed mixins not found: $MIXIN_DIR"; echo "run: gradlew :$(basename "$VER_DIR"):preprocessJava"; exit 2; }
for j in "$@"; do [ -f "$j" ] || { echo "mc jar not found: $j"; exit 2; }; done
# javap 是 Windows 程序：classpath 用 `;` 分隔 + Windows 路径（Git Bash 的 /c/... 它读不懂）
CP=""
for j in "$@"; do
  wj=$(cygpath -w "$j" 2>/dev/null || echo "$j")
  CP="${CP}${CP:+;}${wj}"
done

fail=0
checked=0
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

# 解析类引用为二进制类名（借助文件内的 import 行）。
# 处理三种形态：FQN 原样返回；简单名（MouseHandler）按 import 展开；
# 内部类引用（CommandSuggestions.SuggestionsList）按外部类的 import 展开成
# FQN 后再把最后一个点换成 $。
resolve_class() {
  local f="$1" name="$2"
  local esc imp
  if [[ "$name" == *.* ]]; then
    if grep -qE "^import .*\.$(echo "${name%%.*}" | sed 's/[][$.*\\^]/\\&/g');$" "$f" 2>/dev/null; then
      : # Outer.Simple 形态：外部类是 import 的简单名
    else
      echo "$name"; return  # 已经是完整 FQN
    fi
  fi
  local head="${name%%.*}"
  esc=$(printf '%s' "$head" | sed 's/[][$.*\\^]/\\&/g')
  imp=$(grep -E "^import .*\.${esc};$" "$f" | head -1 | sed -E 's/^import (.*);$/\1/')
  local resolved="${imp:-$head}"
  local rest="${name#*.}"
  if [[ "$rest" != "$name" ]]; then
    # 有内部类部分：包.外部$内部
    local pkg=""
    if [[ "$resolved" == *.* ]]; then pkg="${resolved%.*}"; fi
    local outer="${resolved##*.}"
    echo "${pkg:+$pkg.}${outer}\$${rest}"
  else
    echo "$resolved"
  fi
}

# javap -p -s 一个类（带缓存）；内部类名 A.B 转成 A$B 再试一次
class_dump() {
  local f="$1" cls="$2"
  local key="$cls"
  local cache="$TMP/$(echo "$key" | md5sum | cut -d' ' -f1)"
  if [ -f "$cache" ]; then
    cat "$cache"
    return
  fi
  local out
  out=$(javap -p -s -classpath "$CP" "$cls" 2>&1)
  if ! grep -q "Compiled from" <<<"$out" && [[ "$cls" == *.* && "$cls" != *'$'* ]]; then
    # 内部类：a.b.Outer.Inner -> a.b.Outer$Inner
    local alt="${cls%.*}"'$'"${cls##*.}"
    local out2
    out2=$(javap -p -s -classpath "$CP" "$alt" 2>&1)
    if grep -q "Compiled from" <<<"$out2"; then out="$out2"; fi
  fi
  # 保留换行写缓存（grep -A 1 依赖方法行与 descriptor 行相邻）
  printf '%s\n' "$out" > "$cache"
  printf '%s\n' "$out"
}

for f in "$MIXIN_DIR"/*.java; do
  base=$(basename "$f")
  targets=$(grep -oE '@Mixin\((value = )?[A-Za-z0-9_.$]+\.class' "$f" | sed -E 's/@Mixin\((value = )?//; s/\.class$//')
  [ -z "$targets" ] && continue
  for t in $targets; do
    cls=$(resolve_class "$f" "$t")
    dump=$(class_dump "$f" "$cls")
    if grep -q "Compiled from" <<<"$dump"; then
      echo "OK   class $cls  ($base)"
    else
      echo "FAIL class $t -> $cls not found in jar  ($base)"
      fail=$((fail+1))
      continue
    fi

    # @Accessor("field") / @Invoker("method") —— 名字出现在字段或方法列表即可
    for n in $(grep -oE '@(Accessor|Invoker)\("[^"]+"\)' "$f" | sed -E 's/@(Accessor|Invoker)\("//; s/"\)//'); do
      checked=$((checked+1))
      if grep -qE "(^| )${n}(\(|;)" <<<"$dump"; then
        echo "OK   member ${n} in $cls"
      else
        echo "FAIL member ${n} not found in $cls  ($base)"
        fail=$((fail+1))
      fi
    done

    # method = "name" 或 "name(desc)ret"（javap -s 的 descriptor: 行可精确对描述符）
    while IFS= read -r raw; do
      m=$(sed -E 's/.*method = "([^"]+)".*/\1/' <<<"$raw")
      [ -z "$m" ] && continue
      checked=$((checked+1))
      case "$m" in
        *\(*)
          name=${m%%(*}
          args=$(sed -E 's/^[^(]+\((.*)\)[^()]*$/\1/' <<<"$m")
          if grep -A 1 " ${name}(" <<<"$dump" | grep -qF "descriptor: (${args})"; then
            echo "OK   method ${m} in $cls"
          else
            echo "FAIL method ${m} not found in $cls  ($base)"
            fail=$((fail+1))
          fi
          ;;
        *)
          if grep -qE " ${m}\(" <<<"$dump"; then
            echo "OK   method ${m} in $cls"
          else
            echo "FAIL method ${m} not found in $cls  ($base)"
            fail=$((fail+1))
          fi
          ;;
      esac
    done < <(grep -oE 'method = "[^"]+"' "$f")
  done
done

echo "-----"
echo "checked=$checked failures=$fail"
[ "$fail" -eq 0 ] && exit 0 || exit 1
