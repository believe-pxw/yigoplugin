<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# yigo Changelog

## [Unreleased]

## [1.2.0] - 2026-09-30

### Added

- **文件业务结构检索 (File Structure / `Ctrl + F12`)**：
  - 针对 Yigo XML 定制专属的业务结构大纲工厂 (`YigoStructureViewFactory`)，深度支持 `Ctrl + F12` 快速搜索弹窗和 `Alt + 7` Structure 视窗。
  - 按照业务语义清晰分类大纲：数据源与对象 (DataSources)、操作与按钮 (OperationCollection)、界面控件 (UI Controls)、嵌入表单 (Embedded Forms)、宏与业务逻辑 (Macros)。
  - **全量跨文件展开嵌入表单 (Embed Form)**：自动识别 `<Embed>` 标签并跨文件提取外部子表单，全量展开其内部包含的所有界面控件及引用的数据源，支持在弹窗中直接搜索子表单字段并一键点击跳转至外部 XML。
  - **全量跨文件展开嵌入数据表 (EmbedTable)**：深度解析 `<EmbedTable>`，跨文件加载外部 DataObject 并展开所有 Column 字段。
  - **四重兜底查找体系**：融合 Index 快速索引、全局 AllScope 索引、当前文件同级目录同名文件检测与全局文件名匹配，确保在 prompt 目录、测试工程或复杂工作空间中 100% 毫秒级命中目标表单与数据对象。
  - **操作集合层级透视**：支持平级与嵌套 `OperationCollection` 的完整层级展示，提供动作公式说明。
  - **DataObject 深度解析**：支持 `DataSource` 下的 `DataObject` 与 `TableCollection` 嵌套解析，全面支持主子表与列字段展示。
- **高级查询标识**：增加高级查询识别与高亮展示。

### Changed

- **Yigo Layout 布局设计器交互优化**：
  - 工具栏 (Toolbar) 支持自适应宽度折叠与“更多”下拉菜单。
  - 优化圆角 Tab 风格按钮，彻底修复文字省略号截断的问题。
  - 表格表头紧凑化展示，支持点击 TableKey 快速跳转定位。
- **Trac 变更信息提取与性能优化**：
  - Cookie 调整为应用级别持久化，优化缓存失效机制。
  - 数据库适配增强，全面兼容 Kingbase（人大金仓）并支持自定义驱动。

### Fixed

- **线程安全性与并发稳定性**：全面接入 `ReadAction`（读锁）保护所有 PSI 读取与遍历逻辑，杜绝并发与异步读取报错。
- **平台 API 兼容性**：替换弃用的 IntelliJ 平台 API，适配新版 IDE 运行规范。

## [1.0.2] - 2025-06-16

### Added

- Initial scaffold created from [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)

[Unreleased]: https://github.com/believe-pxw/yigo/compare/v1.2.0...HEAD
[1.2.0]: https://github.com/believe-pxw/yigo/compare/v1.0.2...v1.2.0
[1.0.2]: https://github.com/believe-pxw/yigo/commits/v1.0.2
