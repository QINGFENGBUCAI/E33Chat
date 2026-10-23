# 0001 使用自定义预处理器替代 ReplayMod 预处理器

## 状态

已采纳（2026-10）

## 背景

仓库需要一份源码同时构建 1.16.5 ~ 26.3 共 22 个 Fabric 目标。早期沿用了 ReplayMod 风格的 `com.replaymod.preprocess` 插件与根 `build.gradle` 的节点链，但：

- 该插件的启用开关 `enableReplayPreprocess` 从未在任何构建入口被赋值，相关配置实际不参与构建
- 插件对复合条件支持有限，26.x 的 Yarn → Mojang 映射需要额外正则改写
- 资源与源码的合并逻辑需要按版本覆盖，插件的默认行为会造成重复写入冲突

## 决策

- 删除根 `build.gradle` 与 `settings.gradle` 中的 preprocess 插件解析
- 由 `versions/shared/build.gradle` 内的 `PreprocessTask` 执行 `//#if` / `//#else` / `//#endif` / `//$$` 处理
- 资源合并由 `sourceSets` 配置，版本目录优先于共享目录
- 测试只对 `versions/mainProject` 指向的主版本编译运行

## 后果

- 22 个版本共用一份 `src/main/java`，版本差异以预处理器指令表达
- 预处理器只支持单一阈值条件，复合条件必须写成嵌套 `#if`
- 清理注释时必须保留全部 `//#` / `//$$` 行，否则会改变各版本产物
- `PreprocessorDirectiveTest` 守护指令配对与格式

## 被否决的方案

- 每个版本维护独立源码副本：22 份代码无法同步维护
- 恢复 ReplayMod 插件：无法满足 26.x 映射与资源合并需求，且入口开关从未启用
