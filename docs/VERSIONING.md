# 版本接入与兼容维护手册

本仓库用 [Fallen-Breath preprocessor](https://github.com/Fallen-Breath/preprocessor) 风格的自研预处理任务让 22 个 Fabric 目标共用一份源码。本文沉淀接入新版本（如未来的 26.4 / 27.x）的完整流程与本仓库特有的坑，**接新版本前先通读一遍**。

## 一、添加新版本的清单

1. **注册节点**（两处，缺一不可）：
   - `settings.json` 的 `versions` 数组加目录名；
   - 根 `build.gradle` 的 `preprocess` 块：`createNode('26.4-fabric', 26_04_00, '')` 并把上一版 link 过来。
2. **建目录**：复制最近的同线版本目录（如 `versions/26.3-fabric`），只保留 `gradle.properties`（改 `minecraft_version` / `fabric_api_version` / `archives_base_name` / `mcVersion`）和 `src/main/resources/fabric.mod.json`。
   - `mcVersion` 是**六位数**：26.4 → `260400`。
3. **fabric.mod.json 的 `minecraft` 依赖范围**：
   - 新版本写 `">=26.4"`；
   - **给上一版加上限**（如 `">=26.3 <26.4"`）——大版本之间的 API 断裂（如 26.3 移除 GLFW）会让旧 jar 在新版本上崩溃，收窄后加载器会明确拒绝而不是崩游戏。
4. **编译**：`gradlew :26.4-fabric:compileJava`，错误分类处理：
   - Java API 差异 → 源码加 `//#if` 分叉，或 `versions/shared/build.gradle` 的 `applyMojangMapping` 加映射规则；
   - 记录每个差异到 CHANGELOG。
5. **mixin 审计**（见下文第三节）——**必做**，mixin 描述符是字符串，javac 不校验。
6. **全量回归**：`gradlew build --continue`，22 个目标全部 BUILD SUCCESSFUL。
7. **实机测试**（编译通过 ≠ 运行正常，26.x 三轮实机问题全是编译期看不见的）：按下文第五节的清单。

## 二、预处理器规则与坑

- 指令只有 `//#if` / `//#else` / `//#endif`，**不支持 `//#elseif`**（写嵌套 if/else）。
- 表达式支持原子 `MC <op> <整数>` 的 `!` / `&&` / `||` 组合（`&&` 优先于 `||`，不支持括号）。
- **阈值必须用与节点相同的位数**：26.x 节点是六位数（26.1=260100、26.2=260200、26.3=260300）。写五位数（26030）不会报错——`260200 >= 26030` 恒真，26.1 会静默选错分支。预处理任务现在会对这类可疑阈值直接打 WARNING，构建日志里见到就该修。
- 无法识别的表达式按 **false** 处理并告警（旧实现静默按 true，同样掩盖笔误）。
- 多分支结构模板（26.1 / 26.2+ / 旧版）：

```java
//#if MC >= 260200
<26.2+ 代码>
//#else
//#if MC >= 26000
//$$ <26.1 代码>
//#else
//$$ <1.x 代码>
//#endif
//#endif
```

## 三、Mixin 审计（26.x 必做）

mixin 的 `@Accessor("字段")` / `method = "描述符"` 是字符串，javac 从不校验；每个新版本都要对照官方 jar 逐个核验。审计脚本把这件事自动化（仅 26.x 可用——官方未混淆，源码名与 jar 名一致；1.x 是 yarn 名对不上 intermediary）：

```bash
gradlew :26.4-fabric:preprocessJava          # 先生成预处理产物
scripts/mixin-audit.sh versions/26.4-fabric \
  ~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged/<对应 jar>/minecraft-merged-*.jar
```

输出逐条 `OK/FAIL`，`failures>0` 时退出码非 0。jar 路径在首次编译后于 loom 缓存里找（`minecraftMaven/net/minecraft/minecraft-merged/` 下对应版本目录）。

## 四、已知的版本结构分界（截至 26.3）

| 关注点 | 26.1 | 26.2 / 26.3 |
|---|---|---|
| HUD 提取 | `Gui.extractRenderState(GuiGraphicsExtractor, DeltaTracker)`（本身即 HUD 专属） | 独立 `Hud` 类的同名方法；`Gui.extractRenderState(DeltaTracker,boolean,boolean)` 是 HUD+Overlay+屏幕的**总编排，禁止在其上全量 cancel**（会连屏幕一起不渲染） |
| `setScreen` | 还在 `Minecraft` 上 | 迁到 `Gui.setScreen`（`Minecraft.setScreenAndShow` 汇入它） |
| 键盘键值 | GLFW 数值（ESC=256、Enter=257） | **USB HID 键码**（ESC=41、Enter=40）+ 新修饰位（Ctrl=0xC0）；原版走 `KeyEvent.isEscape()/isConfirmation()` 语义方法 |
| 鼠标按钮编号 | GLFW（左键=0） | 26.3 起 **HID**（左键=1、右键=3）；26.2 仍是 GLFW |
| `FriendlyByteBuf` 集合读写 | `writeCollection/readList` | 26.3 移除 → 用手写 VarInt 计数+元素（见 ChatMetaPayload/GroupListPayload） |
| GUI 渲染 | `GuiGraphicsExtractor` + `pose()`（Matrix3x2fStack）+ `nextStratum()` 分层；`blit/fill/text` | 同左；`Can only blur once per frame`——每帧只允许一次 `blurBeforeThisStratum`，屏幕渲染里禁止再调 `renderBackground/extractBackground` |
| 屏幕覆写 | `keyPressed(KeyEvent)` / `mouseClicked(MouseButtonEvent,boolean)` / `mouseScrolled` 4 参 | 同左；输入参数的版本差异**在入口归一**（见第五节） |

## 五、输入兼容模式（InputCompat）

26.x 的输入 API 每逢大版本可能换编号体系。约定：

1. 版本差异只在两处出现：`InputCompat` 的换算方法 + 各屏幕**入口处的一次归一调用**；
2. 方法体内的既有逻辑永远写 GLFW 风格（ESC=256、左键=0），由入口换算保证正确：
   - 键盘：`keyPressed` 入口 `keyCode = InputCompat.glfwKey(keyInput)`；
   - 鼠标：`mouseClicked/mouseDragged/mouseReleased` 入口 `button = InputCompat.glfwButton(button)`；
3. 反向传给原版 widget 时由 `InputCompat.click()/mouseClicked()/onClick()` 自动逆换算（`rawButton`），调用方无需关心；
4. 接新版本时先 `javap` 官方 jar 的 `InputConstants` 常量与 `KeyEvent`/`MouseButtonInfo` 形状，若编号又变，只改 `InputCompat` + 入口归一的分界阈值。

## 六、实机测试清单（每个新版本必测）

编译通过只说明 Java 层面成立；mixin 注入与延迟渲染管线只有实机能验证。

1. **启动**到进世界（mixin 失配崩在 `Initializing game`，看日志第一条 `MixinApplyError`）；
2. 打开聊天面板：面板出现、原版 HUD 隐藏；关闭后 HUD 恢复；
3. 键盘：打字、回车发送、ESC 关闭、↑↓ 翻历史、@ 补全（Tab 切换/ESC 收起）、Ctrl+C 复制选区、Ctrl+V 粘贴；
4. 鼠标：发送/设置/表情三个图标、标题栏 ✕、汉堡菜单、侧边栏条目、消息右键菜单、滚动条拖拽、文本拖选；
5. 弹层：设置/表情/快捷语/搜索/群组打开与 ESC 关闭；设置界面的分类树与滚动；
6. 服务器功能（有条件时）：群组页签、图片收发、历史同步；
7. 崩溃时取 `crash-report` + `latest.log`，看第一条 `Caused by`。

## 七、历史教训速查

- **mixin 字符串描述符 javac 不校验**——每个新版本必须跑第三节审计脚本（26.3 实机崩溃的根源）；
- **编排方法上全量 cancel = 整棵 GUI 消失**（26.3 "打开面板卡死"的根源），HUD 隐藏只准挂在 HUD 专属提取上；
- **五位数阈值**静默错分支（见第二节）；**复合表达式**曾静默恒真（现 evaluate 已支持且告警）；
- **`Can only blur once per frame`**：自己的屏幕渲染里不要再调 `renderBackground`；
- 旧 jar 的 `minecraft` 依赖范围要加上限，否则 API 断裂的大版本上直接崩游戏。
