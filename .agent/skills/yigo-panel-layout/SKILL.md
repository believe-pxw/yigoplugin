---
name: yigo-panel-layout
description: 生成与解读 YIGO Form XML 中的面板和布局结构，支持 GridLayoutPanel、FlexFlowLayoutPanel、TabPanel、SplitPanel 等 18 种面板类型的嵌套组合
---

# YIGO 面板与布局生成

## 概述

本 Skill 负责生成 YIGO Form XML 中 **Body > Block** 下的面板和布局结构。YIGO 支持 18 种面板类型，面板之间可以嵌套组合，面板内可以放置 UI 控件或子面板。

> **抬头控件优先使用 `GridLayoutPanel`**（X/Y 精确定位），而不是 `FlexFlowLayoutPanel` 或 `FlexGridLayoutPanel`。参考 PM_Strategy.xml 和 Cond_PM_EquipmentQuery.xml 的实际用法。

## XSD 参考文件

- 面板定义：[PanelDefine.xsd](../xsd/element/complex/PanelDefine.xsd)
- 网格面板：[GridLayoutPanel.xsd](../xsd/element/complex/GridLayoutPanel.xsd)
- 面板属性：[PanelAttributeGroupDefinition.xsd](../xsd/attribute/PanelAttributeGroupDefinition.xsd)

## Block 容器

Block 是 Body 下的直接子元素，用于组织面板和控件。

```xml
<Body>
    <Block Key="blockKey" Caption="区域名称">
        <!-- 面板或 Format 元素 -->
    </Block>
</Body>
```

## 面板类型速查

| 面板类型 | 说明 | 可包含控件 | 可包含子面板 |
|----------|------|-----------|-------------|
| `GridLayoutPanel` | 网格布局（行列定义） | ✅ | ✅ |
| `FlexFlowLayoutPanel` | 弹性流式布局（**最常用**） | ✅ | ✅ |
| `FlexGridLayoutPanel` | 弹性网格布局 | ✅ | ✅ |
| `FlowLayoutPanel` | 流式布局 | ✅ | ✅ |
| `BorderLayoutPanel` | 边框布局（上下左右中） | ✅ | ✅ |
| `TabPanel` | 分页面板 | ✅ | ✅ |
| `SplitPanel` | 分割面板 | ✅ | ✅ |
| `Container` | 容器（可嵌入其他表单） | ❌ | ✅ |
| `SubDetail` | 嵌入子明细 | ❌ | ✅ |
| `Grid` | 表格控件 | ❌ | ❌ |
| `DictView` | 字典视图 | ❌ | ❌ |
| `Embed` | 嵌入其他表单 | ❌ | ❌ |
| `LinearLayoutPanel` | 线性布局 | ❌ | ✅ |
| `PopView` | 弹出视图 | ❌ | ✅ |
| `HoverButton` | 悬浮按钮 | ❌ | ❌ |
| `TableView` | 表格视图 | ❌ | ❌ |
| `TabGroup` | 页签组 | ❌ | ✅ |
| `Chart` | 图表 | ❌ | ❌ |

## 公共属性

所有面板共享以下属性组：

### yigo-Key-Caption（标识与名称）

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `Key` | string | ✅ | 面板唯一标识 |
| `Caption` | string | ❌ | 显示名称 |

### yigo-Visible-Enable（可见性与可用性）

| 属性 | 类型 | 说明 |
|------|------|------|
| `Visible` | Boolean/Formula | 是否可见 |
| `Enable` | Boolean/Formula | 是否可用 |
| `VisibleDependency` | string | 可见性依赖项 |
| `EnableDependency` | string | 可用性依赖项 |

### yigo-Component-XY（网格布局定位）

| 属性 | 类型 | 说明 |
|------|------|------|
| `X` | int(≥0) | X 坐标（列号，从 0 开始） |
| `Y` | int(≥0) | Y 坐标（行号，从 0 开始） |
| `XSpan` | int(≥0) | 列跨度 |
| `YSpan` | int(≥0) | 行跨度 |

### yigo-Panel-layout-collection（面板布局属性）

面板在父容器中的尺寸和对齐属性。参见 `yigo-Control-layout-collection`。

---

## 常用面板详细说明

### 1. FlexFlowLayoutPanel（弹性流式布局） ⭐ 最常用

```xml
<FlexFlowLayoutPanel Key="headerPanel" Caption="表头区域">
    <Format>...</Format>           <!-- 可选：格式 -->
    <ToolBar Key="tb" Caption="工具栏" />  <!-- 可选：工具栏 -->
    <!-- 控件和子面板混合排列 -->
    <TextEditor Key="PONo" Caption="订单编号" />
    <Dict Key="SupplierID" Caption="供应商" />
</FlexFlowLayoutPanel>
```

### 2. FlexGridLayoutPanel（弹性网格布局）

```xml
<FlexGridLayoutPanel Key="gridPanel" Caption="网格区域" 
                     ColumnCount="3" ColumnGap="10" RowGap="8" RowHeight="32">
    <TextEditor Key="Field1" Caption="字段1" />
    <TextEditor Key="Field2" Caption="字段2" />
    <TextEditor Key="Field3" Caption="字段3" />
</FlexGridLayoutPanel>
```

| 专有属性 | 说明 |
|----------|------|
| `ColumnCount` | 列数 |
| `ColumnGap` | 列间距 |
| `RowGap` | 行间距 |
| `RowHeight` | 行高 |

### 3. GridLayoutPanel（网格布局面板）

```xml
<GridLayoutPanel Key="gridLPanel" Caption="精确布局">
    <RowDefCollection RowHeight="32" RowGap="5">
        <RowDef Height="32" />
        <RowDef Height="32" />
    </RowDefCollection>
    <ColumnDefCollection ColumnGap="10">
        <ColumnDef Width="120" />
        <ColumnDef Width="200" />
    </ColumnDefCollection>
    <TextEditor Key="f1" Caption="字段1" X="0" Y="0" />
    <TextEditor Key="f2" Caption="字段2" X="1" Y="0" />
</GridLayoutPanel>
```

### 4. TabPanel（分页面板）

```xml
<TabPanel Key="tabMain" Caption="主页签" TabPosition="Top">
    <FlexFlowLayoutPanel Key="tab1" Caption="基本信息">
        <!-- 页签 1 的内容 -->
    </FlexFlowLayoutPanel>
    <FlexFlowLayoutPanel Key="tab2" Caption="附加信息">
        <!-- 页签 2 的内容 -->
    </FlexFlowLayoutPanel>
    <ItemChanged />  <!-- 可选：页签切换事件 -->
</TabPanel>
```

| 专有属性 | 说明 |
|----------|------|
| `TabPosition` | Tab 页位置：`Top`/`Bottom`/`Left`/`Right` |

### 5. SplitPanel（分割面板）

```xml
<SplitPanel Key="splitMain" Caption="分割布局" Orientation="Horizontal">
    <FlexFlowLayoutPanel Key="leftPanel" Caption="左侧" />
    <FlexFlowLayoutPanel Key="rightPanel" Caption="右侧" />
    <SplitSize MinWidth="200" MaxWidth="500" />
</SplitPanel>
```

| 专有属性 | 说明 |
|----------|------|
| `Orientation` | 方向：`Horizontal`/`Vertical` |

### 6. Container（容器 — 嵌入表单）

```xml
<Container Key="detailContainer" Caption="明细容器" 
           DefaultFormKey="PurchaseOrderDetail" Style="Normal">
</Container>
```

| 专有属性 | 说明 |
|----------|------|
| `Style` | 容器样式 |
| `MergeOperation` | 是否合并操作至父界面 |
| `DefaultFormKey` | 默认打开的表单标识 |
| `FormulaFormKey` | 公式表单标识（优先级更高） |

### 7. SubDetail（嵌入子明细）

```xml
<SubDetail Key="subDtl" Caption="嵌入子明细" BindingGridKey="gridDtl">
    <FlexFlowLayoutPanel Key="dtlPanel" Caption="明细面板">
        <!-- 子明细的控件 -->
    </FlexFlowLayoutPanel>
</SubDetail>
```

| 专有属性 | 说明 |
|----------|------|
| `BindingGridKey` | 关联表格的 Key |

### 8. ToolBar（工具栏）

```xml
<ToolBar Key="toolbar1" Caption="工具栏">
    <ToolBarItemCollection>按钮1,按钮2</ToolBarItemCollection>
</ToolBar>
```

### 9. DictView（字典视图）

```xml
<DictView Key="dictView1" Caption="字典视图" FormulaItemKey="公式" LoadType="Full" PageRowCount="20">
    <RowClick>点击事件公式</RowClick>
    <RowDblClick>双击事件公式</RowDblClick>
    <DictViewColumnCollection>...</DictViewColumnCollection>
</DictView>
```

---

## 典型布局模式

### 模式 1：标准单据表单（表头 + 明细）

```xml
<Body>
    <Block>
        <FlexFlowLayoutPanel Key="root">
            <SplitPanel Key="mainSplit" Orientation="Vertical">
                <!-- 表头区域：优先使用 GridLayoutPanel -->
                <GridLayoutPanel Key="headerPanel" Caption="表头" Padding="8px" OverflowY="Auto" TopPadding="24px">
                    <!-- 表头控件由 yigo-control-generator 生成，用 X/Y 定位 -->
                    <RowDefCollection RowGap="24">
                        <RowDef Height="32px"/>
                    </RowDefCollection>
                    <ColumnDefCollection ColumnGap="16">
                        <ColumnDef Width="190px"/>
                        <ColumnDef Width="30px"/>
                        <ColumnDef Width="190px"/>
                        <ColumnDef Width="30px"/>
                    </ColumnDefCollection>
                </GridLayoutPanel>
                <!-- 明细区域 -->
                <Grid Key="gridDetail" Padding="8px">
                    <!-- 表格由 yigo-grid-generator 生成 -->
                </Grid>
                <SplitSize Size="425px"/>
                <SplitSize Size="100%"/>
            </SplitPanel>
        </FlexFlowLayoutPanel>
    </Block>
</Body>
```

### 模式 2：带页签的表单

```xml
<Body>
    <Block Key="mainBlock">
        <FlexFlowLayoutPanel Key="headerPanel" Caption="基本信息">
            <!-- 表头控件 -->
        </FlexFlowLayoutPanel>
        <TabPanel Key="tabDetail" TabPosition="Top">
            <FlexFlowLayoutPanel Key="tab1" Caption="明细1">
                <Grid Key="grid1">...</Grid>
            </FlexFlowLayoutPanel>
            <FlexFlowLayoutPanel Key="tab2" Caption="明细2">
                <Grid Key="grid2">...</Grid>
            </FlexFlowLayoutPanel>
        </TabPanel>
    </Block>
</Body>
```

### 模式 3：叙时簿（View 表单）

```xml
<Body>
    <Block Key="mainBlock">
        <FlexFlowLayoutPanel Key="queryPanel" Caption="查询条件">
            <!-- 查询条件控件 -->
        </FlexFlowLayoutPanel>
        <Grid Key="gridList">
            <!-- 数据列表 Grid -->
        </Grid>
    </Block>
</Body>
```

## 与其他 Skill 的配合

- Panel 作为 **容器**，放置在 `yigo-form-scaffold` 生成的 Body > Block 内
- Panel 内放置 **控件** → 使用 `yigo-control-generator`
- Panel 内放置 **Grid 表格** → 使用 `yigo-grid-generator`
- Panel 的 Key 在整个 Block 内必须唯一
