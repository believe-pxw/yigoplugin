---
name: yigo-form-scaffold
description: 生成与解读 YIGO Form XML 的骨架结构，包括 Form 根节点、DataSource、Body、OperationCollection、ScriptCollection 等顶层容器
---

# YIGO Form 脚手架生成

## 概述

本 Skill 负责生成 YIGO Form XML 的**顶层骨架结构**。Form 是 YIGO 系统中窗口配置的根对象，包含数据源、操作集合、脚本集合、窗体等核心子元素。文件名与Form的Key要相同

## CDATA 规约

> **所有 XML 元素内的表达式（公式）都必须用 `<![CDATA[]]>` 包裹**，避免 `&`, `<`, `>` 等特殊字符导致 XML 解析错误。

```xml
<!-- 正确 -->
<OnLoad><![CDATA[Macro_LoadObject()]]></OnLoad>
<Macro Key="m"><![CDATA[IIF(a>0 && b<10, true, false)]]></Macro>
<CheckRule><![CDATA[IIF(Code=='', '请输入代码', true)]]></CheckRule>

<!-- 错误 -->
<OnLoad>IIF(a>0 && b<10, true, false)</OnLoad>
```

**适用范围**：`OnLoad`, `OnClose`, `OnPostShow`, `Action`, `ExceptionHandler`, `Macro` 内容, `CheckRule`, `ValueChanged`, `DefaultFormulaValue`, `OnClick`, `RowDblClick`, `Statement`, `FormulaItems`, `OnRowDelete` 等一切公式体。

## XSD 参考文件

- 主文件：[Form.xsd](../xsd/Form.xsd)
- 枚举定义：[FormDefine.xsd](../xsd/element/simple/FormDefine.xsd)
- 参考表单：[referenceForm/](file:///d:/Workbench/idea/yigo-ai-assistance-research/resource/referenceForm/)

## Form XML 顶层结构

```xml
<Form Key="表单标识" Caption="表单名称" FormType="Entity" InitState="Default" Version="6.1" Platform="PC">
    <!-- 1. 数据源配置 -->
    <DataSource RefObjectKey="关联数据对象Key">
        <DataObject>...</DataObject>  <!-- 可选：内嵌数据对象 -->
    </DataSource>
    
    <!-- 2. 操作定义集合 -->
    <OperationCollection>
        <Operation Key="..." Caption="..." />
    </OperationCollection>
    
    <!-- 3. 脚本集合 -->
    <ScriptCollection>...</ScriptCollection>
    
    <!-- 4. 窗体（UI 布局入口） -->
    <Body PopWidth="800" PopHeight="600" Resizable="true">
        <Block Key="block1" Caption="主区域">
            <!-- 面板/控件放在这里 -->
        </Block>
    </Body>
    
    <!-- 5. 导航条（移动端，可选） -->
    <NavigationBar />
    
    <!-- 6. 事件钩子（可选，内容用 CDATA 包裹） -->
    <OnLoad><![CDATA[公式内容]]></OnLoad>
    <OnClose><![CDATA[公式内容]]></OnClose>
    <OnPostShow><![CDATA[公式内容]]></OnPostShow>
    
    <!-- 7. 宏公式集合（可选） -->
    <MacroCollection>
        <Macro Key="宏标识" Args="参数"><![CDATA[公式内容]]></Macro>
    </MacroCollection>
    
    <!-- 8. 表单关系集合（复合字典用，可选） -->
    <FormRelationCollection>...</FormRelationCollection>
</Form>
```

## Form 根节点属性

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `Key` | string(50) | ✅ | 表单唯一标识 |
| `Caption` | string | ❌ | 表单显示名称 |
| `CaptionEn` | string | ❌ | 表单英文名称 |
| `FormType` | 枚举 | ❌ | 表单类型，见下表 |
| `InitState` | 枚举 | ❌ | 打开初始状态 |
| `Version` | string | ❌ | 配置版本 |
| `Platform` | 枚举 | ❌ | 支持平台 |
| `FormulaCaption` | string | ❌ | 表达式名称 |
| `AbbrCaption` | string | ❌ | 缩写名称 |
| `ConfirmClose` | Boolean | ❌ | 关闭确认弹框 |
| `HasNavigationBar` | Boolean | ❌ | 是否有导航条 |
| `ViewKey` | string(50) | ❌ | 关联的 View 标识 |
| `Extend` | string(50) | ❌ | 继承的表单标识 |
| `SourceForm` | string(50) | ❌ | 源表单（扩展表单用） |
| `FullscreenType` | 枚举 | ❌ | 全屏类型（移动端） |
| `Authenticate` | Boolean | ❌ | 是否认证登录 |
| `DeliveryClass` | 枚举 | ❌ | 交付类 |
| `TransFormKey` | string | ❌ | 传输关联 FormKey |

## FormType 枚举值

| 值 | 说明 |
|----|------|
| `Normal` | 普通表单（基础类型） |
| `Entity` | 实体表单（单据） |
| `View` | 视图表单（叙时簿/浏览） |
| `Dict` | 字典表单 |
| `Detail` | 明细表单 |
| `Report` | 报表表单 |
| `ChainDict` | 链式字典表单 |
| `CompDict` | 复合字典表单 |
| `Template` | 模板表单 |
| `Extension` | 扩展表单 |

## InitState 枚举值

| 值 | 说明 |
|----|------|
| `Default` | 默认状态 |
| `New` | 新增状态 |
| `Edit` | 修改状态 |


## DataSource 节点

```xml
<DataSource RefObjectKey="数据对象Key">
    <!-- 可选：内嵌 DataObject 定义 -->
    <DataObject Key="..." Caption="..." PrimaryType="Entity">
        ...
    </DataObject>
</DataSource>
```

- `RefObjectKey`（可选）：引用已定义的数据对象标识

## Body 节点

```xml
<Body PopWidth="800" PopHeight="600" Resizable="true">
    <Block Key="block1" Caption="表头区域">
        <!-- 此处放置面板和控件 -->
    </Block>
    <ViewCollection>...</ViewCollection>
</Body>
```

| 属性 | 说明 |
|------|------|
| `PopWidth` | 弹出宽度 |
| `PopHeight` | 弹出高度 |
| `Resizable` | 是否可调整大小 |

## 校验规则

1. **有 ViewKey 必须是 Entity 类型**：`if(@ViewKey) then @FormType='Entity'`
2. **Key 长度限制**：不超过 50 字符
3. **子元素顺序**：`DataSource → OperationCollection → ScriptCollection → Body → NavigationBar → OnLoad → OnClose → OnPostShow → MacroCollection → FormRelationCollection`

## 命名规范

1.

## 表单模板

根据需要生成的表单类型，读取对应模板文件获取完整骨架结构：

| 模板 | 文件 | 适用场景 |
|------|------|---------|
| 字典表单 | [dict-form.md](file:///d:/Workbench/idea/yigo-ai-assistance-research/.agent/skills/yigo-form-scaffold/templates/dict-form.md) | `FormType="Dict"` + SplitPanel（表头 + 明细表）+ 系统信息页签 |
| 后台配置表 | [backend-config-form.md](file:///d:/Workbench/idea/yigo-ai-assistance-research/.agent/skills/yigo-form-scaffold/templates/backend-config-form.md) | `FormType="Entity"` + 纯 Detail Grid，无表头，OnLoad 加载 |
| 单界面报表 | [single-report-form.md](file:///d:/Workbench/idea/yigo-ai-assistance-research/.agent/skills/yigo-form-scaffold/templates/single-report-form.md) | SplitPanel（条件区 + 结果 Grid），控件带 `Condition` 子元素 |
| 双表单报表 | [dual-report-form.md](file:///d:/Workbench/idea/yigo-ai-assistance-research/.agent/skills/yigo-form-scaffold/templates/dual-report-form.md) | 条件表单 + 结果表单，`ERPShowModal` 弹出条件，输入条件后确认加载数据 |



## 与其他 Skill 的配合

- **Body > Block** 内的面板/控件 → 使用 `yigo-panel-layout` 和 `yigo-control-generator`
- **DataSource > DataObject** → 使用 `yigo-dataobject-generator`
- **OperationCollection** → 使用 `yigo-operation-script`
- **OnLoad/OnClose 等事件内容** → 使用 `yigo-expression-helper`
- **抬头控件布局** → 优先使用 `GridLayoutPanel`（X/Y 精确定位），参考 `yigo-panel-layout`
