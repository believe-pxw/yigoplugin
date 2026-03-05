---
name: yigo-operation-script
description: 生成与解读 YIGO Form XML 中的 OperationCollection（操作集合）、ScriptCollection（脚本集合）和 MacroCollection（宏公式集合）
---

# YIGO 操作与脚本生成

## 概述

本 Skill 负责生成 YIGO Form XML 中的**操作定义（OperationCollection）**、**脚本集合（ScriptCollection）**和**宏公式集合（MacroCollection）**。这三个组件定义了表单的业务逻辑行为。

> **所有 `Action`、`ExceptionHandler`、`Macro` 内容、`OnLoad`/`OnClose` 等公式体都必须用 `<![CDATA[]]>` 包裹**。

## XSD 参考文件

- 操作集合：[OperationCollection.xsd](../xsd/element/complex/OperationCollection.xsd)
- 脚本定义：[BaseScriptDefinition.xsd](../xsd/element/complex/BaseScriptDefinition.xsd)
- 宏公式：[MacroCollection.xsd](../xsd/element/complex/MacroCollection.xsd)
- 公共定义：[CommonDefDefine.xsd](../xsd/element/complex/CommonDefDefine.xsd)
- 操作属性：[AttributeGroupDefinition.xsd](../xsd/attribute/AttributeGroupDefinition.xsd) → `yigo-Operation-Attr`

---

## 1. OperationCollection（操作定义集合）

### 结构

```xml
<OperationCollection>
    <!-- 直接的操作 -->
    <Operation Key="optKey" Caption="操作名称">
        <Action><![CDATA[操作执行的公式内容]]></Action>
        <ExceptionHandler><![CDATA[异常处理公式]]></ExceptionHandler>
        <!-- 子操作（可嵌套） -->
        <Operation Key="subOpt" Caption="子操作">
            <Action><![CDATA[子操作公式]]></Action>
        </Operation>
    </Operation>
    
    <!-- 分组的操作集合 -->
    <OperationCollection Key="groupKey" Caption="操作分组">
        <Operation Key="opt1" Caption="操作1">
            <Action><![CDATA[公式]]></Action>
        </Operation>
        <Operation Key="opt2" Caption="操作2">
            <Action><![CDATA[公式]]></Action>
        </Operation>
    </OperationCollection>
</OperationCollection>
```

### 层级说明

- **顶层 OperationCollection**：Form 直接的子元素，**无属性**
- **嵌套 OperationCollection**：有 `yigo-Operation-Attr` 属性，用于分组
- **Operation**：具体的操作项，可嵌套子 Operation

### Operation 属性（yigo-Operation-Attr）

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `Key` | string | ✅ | 操作唯一标识（同级唯一） |
| `Caption` | string | ❌ | 操作显示名称 |
| `CaptionEn` | string | ❌ | 英文名称 |
| `Visible` | Boolean/Formula | ❌ | 是否可见 |
| `Enable` | Boolean/Formula | ❌ | 是否可用 |
| `VisibleDependency` | string | ❌ | 可见性依赖项 |
| `EnableDependency` | string | ❌ | 可用性依赖项 |
| `RefKey` | string | ❌ | 引用标识（引用公共操作） |
| `Icon` | string | ❌ | 图标 |
| `ShortCuts` | string | ❌ | 快捷键（如 `Ctrl+S`） |
| `SelfDisable` | Boolean | ❌ | 是否自动禁用（防连点） |
| `NeedAccessLog` | Boolean | ❌ | 是否需要访问日志 |
| `CssClass` | string | ❌ | CSS 类名 |
| `IconCode` | string | ❌ | 图标编码 |
| `TCode` | string | ❌ | 交易码 |
| `Activity` | string | ❌ | 活动标识 |
| `Tag` | string | ❌ | 标签 |
| `ExpandSource` | string | ❌ | 展开来源 |
| `Expand` | Boolean | ❌ | 是否展开 |
| `IsTransfer` | Boolean | ❌ | 是否转换 |

### Operation 子元素

| 子元素 | 说明 |
|--------|------|
| `Action` | 操作执行的公式/脚本内容 |
| `ExceptionHandler` | 异常处理公式 |
| `Operation` | 嵌套的子操作 |

---

## 2. ScriptCollection（脚本集合）

```xml
<ScriptCollection>
    <!-- 脚本内容，Type 可选 Formula/Java 等（默认 Formula，可省略） -->
</ScriptCollection>
```

### BaseScript 类型

| 属性 | 说明 |
|------|------|
| `Type` | 脚本类型（`Formula` 为默认值，ERP 中通常都是 Formula） |

### 在 Form 中的使用位置

Form 中有多个使用脚本类型的地方：
- `ScriptCollection` — 脚本集合
- `OnLoad` — 表单加载事件
- `OnClose` — 表单关闭事件
- `OnPostShow` — 表单显示后事件
- Grid 中的 `RowClick`/`RowDblClick`/`RowInsert`/`RowDelete` 等
- Button 的 `OnClick`
- 控件的 `KeyEnter`

这些元素都使用 `yigo-BaseScript2` 类型（mixed content，脚本内容直接写在元素体内）：

```xml
<OnLoad><![CDATA[初始化公式内容]]></OnLoad>
<OnClick><![CDATA[按钮点击公式]]></OnClick>
<RowDblClick><![CDATA[SetFormState("Edit")]]></RowDblClick>
```

---

## 3. MacroCollection（宏公式集合）

```xml
<MacroCollection>
    <Macro Key="宏标识" Args="参数列表">
        <![CDATA[宏公式内容]]>
    </Macro>
</MacroCollection>
```

### Macro 属性

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `Key` | string | ✅ | 宏唯一标识（集合内唯一） |
| `Args` | string | ❌ | 参数列表 |

**内容**：宏的公式体，直接作为元素的文本内容。

---

## 使用示例

### 示例 1：标准实体表单操作集合（增删改查审核）

```xml
<OperationCollection>
    <Operation Key="New" Caption="新增" ShortCuts="Ctrl+N">
        <Action><![CDATA[New()]]></Action>
    </Operation>
    <Operation Key="Edit" Caption="编辑">
        <Action><![CDATA[Edit()]]></Action>
    </Operation>
    <Operation Key="Save" Caption="保存" ShortCuts="Ctrl+S" SelfDisable="true">
        <Action><![CDATA[Save()]]></Action>
        <ExceptionHandler><![CDATA[ShowMessage(GetLastError())]]></ExceptionHandler>
    </Operation>
    <Operation Key="Delete" Caption="删除">
        <Action><![CDATA[if(Confirm("确定要删除吗？")) { Delete() }]]></Action>
    </Operation>
    <Operation Key="Cancel" Caption="取消">
        <Action><![CDATA[Cancel()]]></Action>
    </Operation>
    <!-- 审核分组 -->
    <OperationCollection Key="ApproveGroup" Caption="审核">
        <Operation Key="Submit" Caption="提交" Enable="GetFieldValue(&quot;Status&quot;)==0">
            <Action><![CDATA[Submit()]]></Action>
        </Operation>
        <Operation Key="Approve" Caption="审批" Enable="GetFieldValue(&quot;Status&quot;)==1">
            <Action><![CDATA[Approve()]]></Action>
        </Operation>
    </OperationCollection>
    <Operation Key="Close" Caption="关闭">
        <Action><![CDATA[CloseForm()]]></Action>
    </Operation>
</OperationCollection>
```

### 示例 2：View 表单操作集合（叙时簿）

```xml
<OperationCollection>
    <Operation Key="Query" Caption="查询" Icon="query.png">
        <Action>Query()</Action>
    </Operation>
    <Operation Key="New" Caption="新增" Icon="new.png">
        <Action>OpenForm("PurchaseOrder", "New")</Action>
    </Operation>
    <Operation Key="Edit" Caption="编辑" Icon="edit.png">
        <Action>OpenForm("PurchaseOrder", "Edit")</Action>
    </Operation>
    <Operation Key="Delete" Caption="删除" Icon="delete.png">
        <Action>DeleteSelected()</Action>
    </Operation>
    <Operation Key="Export" Caption="导出" Icon="export.png">
        <Action>Export("Excel")</Action>
    </Operation>
    <Operation Key="Close" Caption="关闭" Icon="close.png">
        <Action>CloseForm()</Action>
    </Operation>
</OperationCollection>
```

### 示例 3：宏公式集合

```xml
<MacroCollection>
    <Macro Key="CalcAmount" Args="qty,price">
        <![CDATA[SetFieldValue("Amount", qty * price)]]>
    </Macro>
    <Macro Key="ValidateBeforeSave">
        <![CDATA[IIF(IsEmpty(GetFieldValue("PONo")), ShowMessage("订单编号不能为空"), true)]]>
    </Macro>
</MacroCollection>
```

### 示例 4：表单事件

```xml
<Form Key="PurchaseOrder" Caption="采购订单" FormType="Entity">
    <!-- ... -->
    <OnLoad><![CDATA[IIF(GetFormState()=="New", SetFieldValue("PODate", Today()), '')]]></OnLoad>
    <OnClose><![CDATA[IIF(IsModified(), IIF(Confirm("数据已修改，是否保存？"), Save(), ''), '')]]></OnClose>
</Form>
```

---

## CommonDef 中的操作集合

CommonDef 定义的是**公共操作**，可被多个表单引用（通过 `RefKey`）：

```xml
<CommonDef>
    <OperationCollection>
        <Operation Key="CommonSave" Caption="保存">
            <Action>Save()</Action>
        </Operation>
    </OperationCollection>
    <StatusCollection>...</StatusCollection>
    <ScriptCollection>...</ScriptCollection>
    <MacroCollection>...</MacroCollection>
</CommonDef>
```

在 Form 中引用公共操作：

```xml
<Operation Key="Save" RefKey="CommonSave" />
```

## 与其他 Skill 的配合

- OperationCollection 是 `yigo-form-scaffold` 中 Form 的子元素
- Operation 的 `Action` 公式可能引用 `yigo-dataobject-generator` 定义的字段
- Button 控件的 `OnClick` 事件内容格式与此相同 → 参考 `yigo-control-generator`
- 公式语法细节 → 参考 `yigo-expression-helper`
