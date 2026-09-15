# yigo

![Build](https://github.com/believe-pxw/yigo/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

<!-- Plugin description -->
<p>
<b>Yigo Developer Assistant</b> is an IntelliJ IDEA extension designed for Yigo-ERP and low-code framework secondary development, enhancing your coding and layout design workflow.
</p>

<h3>Yigo 开发者辅助插件</h3>
<p>
<b>Yigo</b> 是一款专为 Yigo-ERP 及低代码平台二次开发打造的 IntelliJ IDEA 增强插件，旨在提升配置开发与代码编写效率。
</p>

<h4>核心功能 (Key Features)</h4>
<ul>
  <li><b>公式与表达式语言支持</b>：支持 Yigo 自定义语法的高亮显示、智能代码补全（含上下文感知提示）、括号匹配、代码格式化与常用代码模板（Live Templates）。</li>
  <li><b>XML 模型导航与引用跳转</b>：内置 Yigo XML Schema 校验，支持表单 (Form)、数据对象 (DataObject)、宏 (Macro)、实体域 (Domain) 的跨文件 <code>Ctrl + Click</code> 快速跳转、查找引用 (Find Usages) 及安全重命名重构。</li>
  <li><b>可视化表单布局预览 (Yigo Layout)</b>：内置可视化设计面板（快捷键 <code>Alt + Y</code>），支持实时解析并预览 Form XML 布局结构，提供拖拽与控件快速检索定位。</li>
  <li><b>编辑器行内增强</b>：提供行内类型标注 (Inlay Hints) 与行标记 (Line Markers)，直观呈现数据绑定关系与标签说明。</li>
  <li><b>协同与联动工具</b>：集成 Trac 根据回归变更ID快速生成ServiceConfig与databaseSource配置,减少手动复制粘贴工作。</li>
</ul>

<h4>快速上手 (Getting Started)</h4>
<ol>
  <li>在编辑器中打开任意 Yigo XML 表单或公式文件即可获得语法高亮与智能提示。</li>
  <li>按住 <code>Ctrl</code> 点击 XML 中的字段绑定、数据对象引用，即可一键穿透跳转。</li>
  <li>按下 <code>Alt + Y</code> 或点击右侧边栏 <b>Yigo Layout</b> 选项卡即可打开可视化布局面板。</li>
</ol>
<!-- Plugin description end -->

## Installation

- Using the IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "yigo"</kbd> >
  <kbd>Install</kbd>
  
- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID/versions) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/believe-pxw/yigo/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation

---
## For AI Assistants
This repository provides an `llms.txt` file at `<root>/llms.txt` designed to help Large Language Models (LLMs) quickly understand the architectural structure, source code organization, and core plugin implementations. Please refer to it before attempting major refactors or adding complex new features.
