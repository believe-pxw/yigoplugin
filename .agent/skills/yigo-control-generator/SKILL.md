---
name: yigo-control-generator
description: 生成与解读 YIGO Form XML 中的 UI 控件配置，支持 TextEditor、Dict、ComboBox、DatePicker、Button 等 30 种控件类型，含数据绑定、格式和条件控制
---

# YIGO UI 控件生成

## 概述

本 Skill 负责生成 YIGO Form XML 中的 **UI 控件**。YIGO 支持约 30 种控件类型，每种控件都有特定的属性和子元素，字典控件的ItemKey对应SAP中域的值表。控件放置在面板（Panel）内。

> **所有控件内的表达式（公式）都必须用 `<![CDATA[]]>` 包裹**，包括 `OnClick`、`KeyEnter`、`CheckRule`、`ValueChanged`、`DefaultFormulaValue` 等。

## XSD 参考文件

- 控件定义：[BaseControlDefinition.xsd](../xsd/element/complex/BaseControlDefinition.xsd)
- 子元素定义：[BaseControlChildElementDefinition.xsd](../xsd/element/complex/BaseControlChildElementDefinition.xsd)
- 属性组定义：[ControlAttributeGroupDefinition.xsd](../xsd/attribute/ControlAttributeGroupDefinition.xsd)

## 控件分类速查

| 类别 | 控件 | 元素名 |
|------|------|--------|
| **文本输入** | 文本编辑器 | `TextEditor` |
| | 多行文本 | `TextArea` |
| | 密码编辑器 | `PasswordEditor` |
| | 富文本编辑器 | `RichEditor` |
| **数值输入** | 数字编辑器 | `NumberEditor` |
| **选择控件** | 下拉框 | `ComboBox` |
| | 复选框 | `CheckBox` |
| | 复选列表 | `CheckListBox` |
| | 单选按钮 | `RadioButton` |
| **字典控件** | 字典选择 | `Dict` |
| | 动态字典 | `DynamicDict` |
| **日期时间** | 日期选择器 | `DatePicker` |
| | UTC 日期 | `UTCDatePicker` |
| | 月份选择器 | `MonthPicker` |
| | 时间选择器 | `TimePicker` |
| **按钮类** | 按钮 | `Button` |
| | 文本按钮 | `TextButton` |
| | 下拉按钮 | `DropdownButton` |
| | 超链接 | `HyperLink` |
| **展示类** | 标签 | `Label` |
| | 图片 | `Image` |
| | 图标 | `Icon` |
| | 分隔符 | `Separator` |
| **特殊控件** | 网页浏览器 | `WebBrowser` |
| | 流程图 | `BPMGraph` |
| | 动态控件 | `Dynamic` |
| | 自定义控件 | `Custom` |
| | 嵌入控件 | `Embed` |
| | 甘特图 | `Gantt` |

## 公共属性（yigo-BaseControl-Attr）

所有控件共享的基础属性：

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `Key` | string | ✅ | 控件唯一标识 |
| `Caption` | string | ❌ | 显示标题 |
| `CaptionEn` | string | ❌ | 英文标题 |
| `LabelType` | 枚举 | ❌ | 标签类型 |
| `FormulaCaption` | string | ❌ | 动态标题表达式 |
| `Visible` | Boolean/Formula | ❌ | 可见性 |
| `Enable` | Boolean/Formula | ❌ | 可用性 |
| `InitEnable` | Boolean | ❌ | 初始可用状态 |
| `InitVisible` | Boolean | ❌ | 初始可见状态 |
| `CopyNew` | Boolean | ❌ | 新增时是否复制值 |
| `Clearable` | Boolean | ❌ | 是否可清除 |
| `Tip` | string | ❌ | 鼠标提示信息 |
| `FormulaTip` | string | ❌ | 动态提示信息 |
| `OnlyShow` | Boolean | ❌ | 仅显示（移动端） |
| `OneTimeCompute` | Boolean | ❌ | 默认值仅计算一次 |
| `AsQuery` | Boolean | ❌ | 是否叙时簿查询字段 |
| `X`/`Y`/`XSpan`/`YSpan` | int | ❌ | 网格布局坐标定位 |
| `Width`/`Height` | Dimension | ❌ | 尺寸 |
| `HAlign`/`VAlign` | 枚举 | ❌ | 对齐方式 |

## 公共子元素（ControlBaseChildGroup）

大多数控件包含以下公共子元素：

```xml
<控件 Key="..." Caption="...">
    <DataBinding TableKey="表标识" ColumnKey="列标识" />
    <Format>格式化定义</Format>
    <Condition>条件控制定义</Condition>
</控件>
```

### DataBinding（数据绑定）

将控件绑定到 DataObject 的某个 Table.Column：

```xml
<DataBinding TableKey="PurchaseOrder" ColumnKey="PONo"/>

<!-- 带校验规则 -->
<DataBinding TableKey="EPM_Strategy" ColumnKey="Code">
    <CheckRule><![CDATA[IIF(Code=='', '请输入代码', true)]]></CheckRule>
</DataBinding>

<!-- 带默认值公式 -->
<DataBinding DefaultFormulaValue="Macro_MultiLangText('EPM_Strategy','Name')"/>

<!-- 带值改变事件 -->
<DataBinding ColumnKey="PackageUnitID">
    <ValueChanged><![CDATA[com.bokesoft.erp.pm.function.StrategiesFormula.setMinUnitID();]]></ValueChanged>
</DataBinding>
```

### Format（格式化）

控制控件的显示格式。

### Condition（查询条件） ⭐ 报表/查询表单重要

用于报表/查询表单中，将控件值作为查询条件关联到结果表字段：

```xml
<!-- 简单等值条件 -->
<Condition ColumnKey="MaintPlantID" TableKey="{ResultTableKey}" CondSign="=" UseAdvancedQuery="true"/>

<!-- like 模糊查询 -->
<Condition ColumnKey="DocumentNumber" TableKey="{ResultTableKey}" CondSign="like" LoadHistoryInput="true" UseAdvancedQuery="true"/>

<!-- 自定义条件（日期转换等复杂场景） -->
<Condition TableKey="{ResultTableKey}" CondSign="custom" UseAdvancedQuery="true">
    <CustomCondition Condition="Cond_CreateDate &gt; 0" Filter="CAST(CreateTime as DATE) = ${Cond_CreateDate_Para1}">
        <CustomConditionPara Key="Cond_CreateDate_Para1" Formula="Replace(ToString(ConditionPara('Cond_CreateDate')), '-', '')"/>
    </CustomCondition>
    <CustomCondition Condition="1 == 1" Filter=" 1 = 1 "/>
</Condition>
```

| 属性 | 说明 |
|------|------|
| `ColumnKey` | 结果表中对应的列名 |
| `TableKey` | 结果表标识 |
| `CondSign` | 条件符号：`=`, `like`, `custom` |
| `UseAdvancedQuery` | 是否使用高级查询 |
| `LoadHistoryInput` | 是否加载历史输入 |

---

## 常用控件详细说明

### 1. TextEditor（文本编辑器）

```xml
<TextEditor Key="PONo" Caption="订单编号" MaxLength="30" Trim="true" PromptText="请输入编号">
    <DataBinding TableKey="PurchaseOrder" ColumnKey="PONo" />
    <KeyEnter>回车事件公式</KeyEnter>
</TextEditor>
```

| 专有属性 | 说明 |
|----------|------|
| `MaxLength` | 最大长度 |
| `Trim` | 是否去除首尾空格 |
| `PromptText` | 输入提示文字 |
| `EmbedText` | 嵌入文本 |
| `InvalidChars` | 无效字符 |
| `Case` | 大小写转换：`Upper`/`Lower` |
| `PreIcon` | 前置图标 |
| `DisableKeyboard` | 禁用键盘 |

### 2. NumberEditor（数字编辑器）

```xml
<NumberEditor Key="Amount" Caption="金额" Precision="18" Scale="2" 
              ShowZero="false" UseGroupingSeparator="true">
    <DataBinding TableKey="PurchaseOrder" ColumnKey="TotalAmount" />
</NumberEditor>
```

| 专有属性 | 说明 |
|----------|------|
| `Precision` | 数值精度（整数位+小数位） |
| `Scale` | 小数位数 |
| `ZeroString` | 零值显示文本 |
| `UseGroupingSeparator` | 千分位分隔符 |
| `StripTrailingZeros` | 去除尾部零 |
| `ShowZero` | 是否显示零值 |
| `SelectOnFocus` | 获得焦点时全选 |
| `RoundingMode` | 舍入模式 |

### 3. Dict（字典选择控件）

```xml
<Dict Key="SupplierID" Caption="供应商" ItemKey="Supplier" 
      AllowMultiSelection="false" Editable="true" PromptText="选择供应商">
    <DataBinding TableKey="PurchaseOrder" ColumnKey="SupplierID" />
    <ItemFilter Key="filterKey" Type="..." Query="过滤条件" />
</Dict>
```

| 专有属性 | 说明 |
|----------|------|
| `ItemKey` | 字典项标识（关联 DomainDef），意味着有一个数据对象的key与ItemKey一致 |
| `AllowMultiSelection` | 是否允许多选 |
| `Editable` | 是否可编辑 |
| `Independent` | 是否独立 |
| `Root` | 字典根节点 |
| `TextField` | 文本字段 |
| `LoadType` | 加载策略：`Full`/`Lazy` 等 |
| `EditValue` | 是否可编辑值 |
| `QueryMatchType` | 模糊匹配类型 |
| `StateMask` | 状态掩码 |
| `FormulaText` | 表达式文本 |

### 4. ComboBox（下拉框）

```xml
<ComboBox Key="Status" Caption="状态" SourceType="Static" IntegerValue="true">
    <DataBinding TableKey="PurchaseOrder" ColumnKey="Status" />
    <Item Key="0" Caption="草稿" />
    <Item Key="1" Caption="已提交" />
    <Item Key="2" Caption="已审批" />
</ComboBox>
```

| 专有属性 | 说明 |
|----------|------|
| `SourceType` | 来源类型：`Static`/`Domain`/`Formula`/`ParaGroup` |
| `IntegerValue` | 值是否为整数 |
| `Editable` | 是否可编辑 |
| `GroupKey` | 分组标识（`ParaGroup` 时需要） |
| `TextShowType` | 文本显示类型 |
| `Cache` | 是否缓存 |

**校验规则：**
- `SourceType='Formula'` 时必须配置 `FormulaItems` 子元素
- `SourceType='ParaGroup'` 时必须配置 `GroupKey` 属性

### 5. DatePicker（日期选择器）

```xml
<DatePicker Key="PODate" Caption="订单日期" Format="yyyy-MM-dd" EditType="Calendar">
    <DataBinding TableKey="PurchaseOrder" ColumnKey="PODate" />
</DatePicker>
```

| 专有属性 | 说明 |
|----------|------|
| `Format` | 日期格式 |
| `EditType` | 编辑样式：`Calendar`/`Spinner` |

### 6. CheckBox（复选框）

```xml
<CheckBox Key="IsUrgent" Caption="是否紧急">
    <DataBinding TableKey="PurchaseOrder" ColumnKey="IsUrgent" />
</CheckBox>
```

| 专有属性 | 说明 |
|----------|------|
| `CheckedType` | 选中联动类型 |
| `UnCheckedType` | 取消选中联动类型 |
| `IconLocation` | 图标位置 |
| `CheckOnClickNode` | 点击节点时是否选中 |

### 7. Button（按钮）

```xml
<Button Key="btnCalc" Caption="计算" Type="Normal">
    <OnClick><![CDATA[ERPShowModal('TargetForm', GetCallFormula('Macro_ShowEvent', SOID));]]></OnClick>
</Button>
```

| 专有属性 | 说明 |
|----------|------|
| `Icon` | 图标路径 |
| `IconLocation` | 图标位置 |
| `NeedAccessLog` | 需要访问日志 |
| `OnlyIcon` | 仅显示图标 |
| `Type` | 按钮类型 |
| `Activity` | 活动标识 |
| `TCode` | 交易码 |

### 8. Label（标签）

```xml
<Label Key="lblTitle" Caption="采购订单">
    <DataBinding TableKey="PurchaseOrder" ColumnKey="PONo" />
</Label>
```

### 9. TextButton（文本按钮）

```xml
<TextButton Key="vendorSelect" Caption="供应商" UseFormulaModel="false">
    <DataBinding TableKey="PurchaseOrder" ColumnKey="SupplierID" />
    <OnClick>点击事件</OnClick>
    <KeyEnter>回车事件</KeyEnter>
</TextButton>
```

### 10. Image（图片控件）

```xml
<Image Key="productImg" Caption="产品图片" SourceType="DataObject">
    <DataBinding TableKey="PurchaseOrder" ColumnKey="ImageData" />
</Image>
```

### 11. Embed（嵌入控件）

```xml
<Embed Key="embedForm" Caption="嵌入表单" FormKey="TargetFormKey" IncludeDataTable="true">
    <Var Key="varName" />
</Embed>
```

### 12. DropdownButton（下拉按钮）

```xml
<DropdownButton Key="btnMore" Caption="更多操作">
    <DropdownItem Key="item1" Text="操作1">
        <OnClick>操作1事件</OnClick>
    </DropdownItem>
    <DropdownItem Key="item2" Text="操作2">
        <OnClick>操作2事件</OnClick>
    </DropdownItem>
</DropdownButton>
```

---

## 控件放置位置

控件可以放在以下面板内：

- `GridLayoutPanel`（网格布局 — 通过 X/Y 定位，**抬头控件优先使用**）
- `FlexFlowLayoutPanel`（弹性流式布局）
- `FlexGridLayoutPanel`（弹性网格布局）
- `FlowLayoutPanel`（流式布局）
- `BorderLayoutPanel`（边框布局）
- `TabPanel`（分页面板）
- `SplitPanel`（分割面板）

## 与其他 Skill 的配合

- 控件放置在 `yigo-panel-layout` 生成的面板内
- 控件的 `DataBinding` 引用 `yigo-dataobject-generator` 定义的 Table/Column
- 表格中的单元格也用到控件属性 → 参考 `yigo-grid-generator`
- 报表/查询表单中控件的 `Condition` 子元素 → 参考模板 3/4 in `yigo-form-scaffold`
- 按钮/事件中的公式内容 → 参考 `yigo-expression-helper`
