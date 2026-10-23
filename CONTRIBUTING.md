# 贡献指南

## 环境

- JDK 25（构建脚本通过 toolchain 选择各版本所需的 Java 17 / 21 / 25）
- Gradle 由 wrapper 提供，无需本机安装
- Windows 下需保留 `gradle.properties` 中的 `-Dfile.encoding=GBK`

## 构建与测试

```
./gradlew build
```

- 一次构建全部 22 个 Fabric 目标
- 单元测试仅在主版本（`1.21.1-fabric`）运行
- 产物位于 `versions/<版本>-fabric/build/libs/`

## 分支与提交

- 主线分支：`fabric-multi`
- 提交信息使用 Conventional Commits：`feat:`、`fix:`、`docs:`、`chore:`、`style:`、`refactor:`、`test:`
- 一个提交只做一件事，禁止把格式化与逻辑修改混在一起
- 禁止强推公共分支；历史重写需事先确认

## 代码约定

- 源文件保持零注释，仅允许 `//#if` / `//#else` / `//#endif` / `//$$` 预处理器指令
- 版本差异必须通过预处理器表达，不要复制整份文件
- Minecraft 类型不得进入 `config/` 纯逻辑类
- 新增或修改的 lang 键必须同时补齐 `zh_cn.json` 与 `en_us.json`

## 版本号与发布

- `gradle.properties` 的 `mod_version` 是唯一版本来源
- 版本号遵循 SemVer，预发布使用 `-preN`
- 已发布版本不覆盖重传，修复发新版本
- 发布资产为 22 个 jar，附 CI 生成的 `checksums.sha256`

## 提交前检查

1. `./gradlew build` 通过
2. 测试全绿
3. `git status` 无预期外文件（`.trae/`、`.idea/`、日志不得入库）
4. 无本机路径、凭据、邮箱、内网 IP 等敏感信息
5. `CHANGELOG.md` 已按 Added / Changed / Fixed / Removed 记录
6. 涉及架构选择时补充 `docs/adr/`

## Issue

提交 Issue 时请填写 Minecraft 版本、Fabric Loader 版本、模组版本与完整日志，并说明是否安装 CustomSkinLoader / No Chat Reports 等兼容模组。
