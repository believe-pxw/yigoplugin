---
name: yigo-grid-generator
description: 生成与解读 YIGO Form XML 中的 Grid 表格控件，包含 GridColumnCollection、GridRowCollection、GridCell 单元格配置、事件绑定和分页设置
---

# YIGO Grid 表格控件生成

## 概述

本 Skill 负责生成 YIGO Form XML 中最复杂的组件 —— **Grid 表格控件**。Grid 用于展示和编辑明细数据，包含列定义、行定义、单元格配置、事件和分页等。

> **Grid 内所有事件公式都必须用 `<![CDATA[]]>` 包裹**，包括 `RowClick`, `RowDblClick`, `OnRowDelete`, `CheckRule`, `ValueChanged` 等。

## XSD 参考文件

- 主文件：[Grid.xsd](../xsd/element/complex/Grid.xsd)（913 行）
- 控件属性：[ControlAttributeGroupDefinition.xsd](../xsd/attribute/ControlAttributeGroupDefinition.xsd)

## Grid 完整结构

```xml
<Grid Key="gridKey" Caption="表格名称" 
      TableKey="绑定的主表Key" 
      SelectionMode="Row" ShowRowHead="true" 
      PageLoadType="UI" PageRowCount="20">
    
    <!-- 条件 -->
    <Condition>...</Condition>
    
    <!-- 列定义集合 -->
    <GridColumnCollection>
        <GridColumn Key="col1" Caption="列标题1" Width="120" Sortable="true" />
        <GridColumn Key="col2" Caption="列标题2" Width="150" ColumnType="Group" />
    </GridColumnCollection>
    
    <!-- 行定义集合 -->
    <GridRowCollection>
        <GridRow Key="row1" TableKey="表标识" RowType="Detail">
            <GridCell Key="cell1" CellType="TextEditor">
                <DataBinding TableKey="表标识" ColumnKey="列标识" />
            </GridCell>
        </GridRow>
    </GridRowCollection>
    
    <!-- 事件（内容用 CDATA 包裹） -->
    <RowClick><![CDATA[行点击事件公式]]></RowClick>
    <RowDblClick><![CDATA[行双击事件公式]]></RowDblClick>
    <BeforeRowInsert><![CDATA[行添加前事件]]></BeforeRowInsert>
    <RowInsert><![CDATA[行添加事件]]></RowInsert>
    <RowDelete><![CDATA[行删除后事件]]></RowDelete>
    <OnRowDelete><![CDATA[行删除事件]]></OnRowDelete>
    <onBatchRowDelete><![CDATA[批量删除事件]]></onBatchRowDelete>
    
    <!-- 追溯集合 -->
    <TraceCollection>
        <Trace Caption="追溯标题" Condition="条件">追溯公式</Trace>
    </TraceCollection>
    
    <!-- 额外操作集合 -->
    <ExtOptCollection>
        <ExtOpt Key="opt1" Caption="操作1" Icon="icon.png">操作公式</ExtOpt>
    </ExtOptCollection>
    
    <!-- 焦点行改变事件 -->
    <FocusRowChanged>焦点行改变公式</FocusRowChanged>
    
    <!-- 数据过滤 -->
    <GridFilter Op="And">
        <FilterValue FieldKey="字段" CondSign="=" ParaValue="值" />
    </GridFilter>
</Grid>
```

## Grid 属性

| 属性 | 类型 | 说明 |
|------|------|------|
| `Key` | string | ✅ 表格唯一标识 |
| `Caption` | string | 表格标题 |
| `SelectionMode` | `Cell`/`Row` | 选择模式（默认范围选择） |
| `ShowRowHead` | Boolean | 是否显示行头序号列 |
| `SerialSeq` | Boolean | 序列号是否连续 |
| `DefaultFitWidth` | Boolean | 是否默认最佳列宽 |
| `Option` | string | 需要的操作定义 |
| `DisabledOption` | string | 不需要的操作定义 |
| `GridDefaultSortField` | string | 默认排序公式 |
| `EndEditByNav` | Boolean | 方向键结束编辑 |
| `NewEmptyRow` | Boolean/Formula | 编辑状态下是否新增空行 |
| `OneTimeCompute` | Boolean | 一次性计算 |
| `ShowTotalRowCount` | Boolean | 显示总行数 |
| `AddDataRow` | Boolean | 插行时是否同步插入数据行 |
| `Zoom` | Boolean | 是否缩放 |
| `Sortable` | Boolean | 列是否可拖动排序 |
| `Custom` | Boolean | 是否有定制数据 |
| `Locate` | Boolean | 是否可定位行 |

### 分页属性（yigo-Grid-Page）

| 属性 | 类型 | 说明 |
|------|------|------|
| `PageLoadType` | `UI`/`DB` | 分页方式（前端分页/后端分页） |
| `PageRowCount` | int | 每页行数 |
| `PageIndicatorCount` | int | 显示最大页码 |
| `SerialRowNum` | Boolean | 序列号行号 |
| `RowRange` | string | 可选每页行数列表 |

## GridColumn（列定义）

```xml
<GridColumn Key="colKey" Caption="列标题" Width="120" 
            ColumnType="Detail" Sortable="true" Freeze="false">
    <!-- 列拓展（可选） -->
    <ColumnExpand ExpandType="..." ExpandSourceType="..." />
    <!-- 嵌套列（可选） -->
    <GridColumnCollection>
        <GridColumn Key="subCol1" Caption="子列1" />
    </GridColumnCollection>
    <!-- 选中事件（复选框列用） -->
    <OnSelect>选中事件公式</OnSelect>
</GridColumn>
```

| 属性 | 类型 | 说明 |
|------|------|------|
| `Key` | string | ✅ 列标识（同一集合内唯一） |
| `Caption` | string | 列标题 |
| `Width` | string | 列宽 |
| `ColumnType` | `Detail`/`Group`/`Total` | 列类型（默认 Fix） |
| `Sortable` | Boolean | 是否可排序 |
| `Freeze` | Boolean | 是否冻结 |
| `Fixed` | `left`/`right` | 冻结位置 |
| `Visible` | Boolean/Formula | 可见性 |
| `Enable` | Boolean/Formula | 可用性 |
| `FormulaCaption` | Formula | 动态标题 |
| `LabelType` | 枚举 | 标题标签类型 |
| `SumFormula` | Formula | 汇总公式 |
| `GroupSumFormula` | Formula | 分组汇总公式 |
| `Image` | string | 图片 |

### ColumnExpand（列拓展）

```xml
<ColumnExpand ExpandType="Horizontal" ExpandSourceType="Formula" 
              TableKey="表标识" ColumnKey="列标识" ItemKey="字典标识" 
              ExpandDependency="依赖列">
    拓展公式内容
</ColumnExpand>
```

## GridRow（行定义）

```xml
<GridRow Key="rowKey" TableKey="表标识" RowType="Detail" RowHeight="32">
    <GridCell Key="cellKey" CellType="TextEditor">
        <DataBinding TableKey="表" ColumnKey="列" />
    </GridCell>
    <!-- 行拓展（可选） -->
    <RowExpand ExpandType="..." CellKey="..." />
    <!-- 行树形（可选） -->
    <RowTree CellKey="treeCell" TreeType="..." Expand="true" ExpandLevel="2" />
</GridRow>
```

| 属性 | 类型 | 说明 |
|------|------|------|
| `Key` | string | ✅ 行标识 |
| `TableKey` | string(50) | 绑定的表标识 |
| `RowType` | `Fix`/`Group`/`Total`/`TreeRow` | 行类型（默认 Detail） |
| `RowHeight` | int | 行高 |
| `Visible` | Boolean/Formula | 可见性（非明细行） |
| `GroupKey` | string | 分组标识（分组行用） |
| `BackColor` | Color | 背景色 |

## GridCell（单元格） ⭐ 核心配置

GridCell 是 Grid 最核心的配置，它定义了每个单元格的控件类型和数据绑定。

```xml
<GridCell Key="cellKey" CellType="控件类型" Caption="显示名"
          Visible="true" Enable="true">
    <DataBinding TableKey="表标识" ColumnKey="列标识" />
    <ItemFilter Key="过滤标识" Type="..." Query="条件" />
    <Item Key="选项Key" Caption="选项名" />
    <OnClick>点击事件</OnClick>
    <DblClick>双击事件</DblClick>
    <CellFormat>单元格格式</CellFormat>
</GridCell>
```

### CellType 对应的控件类型

GridCell 的 `CellType` 属性决定单元格使用哪种控件，取值参考 `yigo-ControlType-Biz`：

| CellType 值 | 对应控件 | 常用附加属性 |
|-------------|----------|-------------|
| `TextEditor` | 文本输入 | `MaxLength`, `Trim`, `Case` |
| `NumberEditor` | 数字输入 | `Precision`, `Scale`, `ShowZero` |
| `ComboBox` | 下拉框 | `SourceType`, `GroupKey`, `IntegerValue` |
| `CheckBox` | 复选框 | `CheckedType`, `UnCheckedType` |
| `CheckListBox` | 复选列表 | `SourceType` |
| `RadioButton` | 单选按钮 | `GroupKey`, `Value` |
| `DatePicker` | 日期选择 | `Format`, `EditType` |
| `UTCDatePicker` | UTC 日期 | `Format` |
| `MonthPicker` | 月份选择 | `Format` |
| `TimePicker` | 时间选择 | `Format` |
| `Dict` | 字典选择 | `ItemKey`, `AllowMultiSelection`, `LoadType` |
| `DynamicDict` | 动态字典 | `ItemKey`, `RefKey`, `IsDynamic` |
| `Label` | 标签显示 | — |
| `Button` | 按钮 | `NeedAccessLog`, `OnlyIcon` |
| `TextButton` | 文本按钮 | `UseFormulaModel` |
| `Image` | 图片 | `ImageScaleType` |
| `HyperLink` | 超链接 | — |
| `Icon` | 图标 | `Icon`, `ImageScaleType` |
| `RichEditor` | 富文本 | — |
| `Custom` | 自定义 | — |
| `Dynamic` | 动态控件 | — |

> **注意**：GridCell 上会附带对应控件类型的全部属性组。例如 `CellType="Dict"` 时，GridCell 可使用 Dict 控件的所有属性（如 `ItemKey`, `AllowMultiSelection` 等）。

### 单元格其他重要属性

| 属性 | 说明 |
|------|------|
| `IsSelect` | 是否选择字段（复选框列） |
| `SingleSelect` | 选择是否单选 |
| `CopyNew` | 是否可复制新增 |
| `AsQuery` | 是否叙时簿查询字段 |
| `Tip` | 提示信息 |
| `CellGroupType` | 分组类型 |
| `IsMerged` | 是否在合并区域 |
| `Merge` | 是否合并 |

## RowTree（行树形配置）

```xml
<RowTree CellKey="treeCell" TreeType="Standard" Type="selfReferencing"
         Expand="true" ExpandLevel="2" 
         Foreign="外键字段" Parent="父节点字段"
         LoadMethod="Full" Image="tree.png" />
```

## GridFilter（数据过滤）

```xml
<GridFilter Op="And">
    <FilterValue FieldKey="Status" CondSign="=" ParaValue="1" DataType="Int" Type="Const" />
    <FilterValue FieldKey="DeptID" CondSign="=" Type="Field" RefValue="当前部门公式" />
</GridFilter>
```

---

## 使用示例

### 示例 1：标准明细表格（采购订单明细）

```xml
<Grid Key="gridDtl" Caption="订单明细" ShowRowHead="true" NewEmptyRow="true">
    <GridColumnCollection>
        <GridColumn Key="colLineNo" Caption="行号" Width="60" />
        <GridColumn Key="colMaterial" Caption="物料" Width="150" />
        <GridColumn Key="colQty" Caption="数量" Width="100" />
        <GridColumn Key="colPrice" Caption="单价" Width="120" />
        <GridColumn Key="colAmount" Caption="金额" Width="120" />
        <GridColumn Key="colRemark" Caption="备注" Width="200" />
    </GridColumnCollection>
    <GridRowCollection>
        <GridRow Key="dtlRow" TableKey="PurchaseOrderDtl">
            <GridCell Key="LineNo" CellType="NumberEditor" Precision="10" Scale="0" Enable="false">
                <DataBinding TableKey="PurchaseOrderDtl" ColumnKey="LineNo" />
            </GridCell>
            <GridCell Key="MaterialID" CellType="Dict" ItemKey="Material">
                <DataBinding TableKey="PurchaseOrderDtl" ColumnKey="MaterialID" />
            </GridCell>
            <GridCell Key="Qty" CellType="NumberEditor" Precision="18" Scale="4">
                <DataBinding TableKey="PurchaseOrderDtl" ColumnKey="Qty" />
            </GridCell>
            <GridCell Key="Price" CellType="NumberEditor" Precision="18" Scale="4">
                <DataBinding TableKey="PurchaseOrderDtl" ColumnKey="Price" />
            </GridCell>
            <GridCell Key="Amount" CellType="NumberEditor" Precision="18" Scale="2" Enable="false">
                <DataBinding TableKey="PurchaseOrderDtl" ColumnKey="Amount" />
            </GridCell>
            <GridCell Key="Remark" CellType="TextEditor" MaxLength="200">
                <DataBinding TableKey="PurchaseOrderDtl" ColumnKey="Remark" />
            </GridCell>
        </GridRow>
    </GridRowCollection>
</Grid>
```

### 示例 2：叙时簿列表表格（带分页）

```xml
<Grid Key="gridList" Caption="采购订单列表" 
      SelectionMode="Row" PageLoadType="DB" ShowRowHead="true">
    <GridColumnCollection>
        <GridColumn Key="colPONo" Caption="订单编号" Width="120" Sortable="true" />
        <GridColumn Key="colDate" Caption="订单日期" Width="100" Sortable="true" />
        <GridColumn Key="colSupplier" Caption="供应商" Width="150" />
        <GridColumn Key="colAmount" Caption="总金额" Width="120" />
        <GridColumn Key="colStatus" Caption="状态" Width="80" />
    </GridColumnCollection>
    <GridRowCollection>
        <GridRow Key="listRow" TableKey="PurchaseOrder">
            <GridCell Key="PONo" CellType="TextEditor" Enable="false">
                <DataBinding TableKey="PurchaseOrder" ColumnKey="PONo" />
            </GridCell>
            <GridCell Key="PODate" CellType="DatePicker" Format="yyyy-MM-dd" Enable="false">
                <DataBinding TableKey="PurchaseOrder" ColumnKey="PODate" />
            </GridCell>
            <GridCell Key="SupplierID" CellType="Dict" ItemKey="Supplier" Enable="false">
                <DataBinding TableKey="PurchaseOrder" ColumnKey="SupplierID" />
            </GridCell>
            <GridCell Key="TotalAmount" CellType="NumberEditor" Precision="18" Scale="2" Enable="false">
                <DataBinding TableKey="PurchaseOrder" ColumnKey="TotalAmount" />
            </GridCell>
            <GridCell Key="Status" CellType="ComboBox" SourceType="Static" Enable="false">
                <DataBinding TableKey="PurchaseOrder" ColumnKey="Status" />
                <Item Key="0" Caption="草稿" />
                <Item Key="1" Caption="已提交" />
            </GridCell>
        </GridRow>
    </GridRowCollection>
    <RowDblClick><![CDATA[Open("PurchaseOrder")]]></RowDblClick>
</Grid>
```

## 与其他 Skill 的配合

- Grid 放在 `yigo-panel-layout` 生成的面板内（如 Block/FlexFlowLayoutPanel）
- GridCell 的 `CellType` 与 `yigo-control-generator` 中的控件类型对应，属性规格一致
- GridCell 的 `DataBinding` 引用 `yigo-dataobject-generator` 定义的 Table/Column
- Grid 的事件公式内容 → 参考 `yigo-expression-helper`
