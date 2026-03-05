---
name: yigo-domain-generator
description: 生成与解读 YIGO Domain XML 配置，定义数据域的控件类型、数据类型、精度长度及 ComboBox 枚举值，按 RefControlType 追加到对应的 DomainDef_{Type}.xml 文件
---

# YIGO Domain 数据域生成

## 概述

Domain（数据域）是 YIGO 系统的 **字段类型定义层**，介于 DataElement（字段元数据）和 UI 控件之间。Domain 定义了：

- 关联的 UI 控件类型（RefControlType）
- 底层数据类型（DataType）
- 类型特有参数（长度、精度、字典引用、枚举值等）

Domain 文件按 **RefControlType 分文件组织**，每种控件类型对应一个 `DomainDef_{Type}.xml`。生成新条目时需追加到对应文件中。

## XSD 参考

- 根定义：[DomainDef.xsd](../xsd/DomainDef.xsd)
- 专有属性：[DomainDefDefine.xsd](../xsd/element/complex/DomainDefDefine.xsd)

## XML 骨架

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<DomainDef>
    <DomainCollection>
        <Domain .../>
        <!-- 或含 Item 子元素（ComboBox） -->
        <Domain ...>
            <Item Key="..." Caption="..." Value="..."/>
        </Domain>
    </DomainCollection>
</DomainDef>
```

## Domain 通用属性

| 属性 | 必须 | 说明 |
|------|------|------|
| `Key` | ✅ | 唯一标识，PascalCase |
| `Caption` | ✅ | 中文显示名 |
| `RefControlType` | ✅ | 关联 UI 控件类型（见下方分类表） |
| `DataType` | ✅ | 底层数据类型（见下方取值） |

### DataType 取值

| DataType | 说明 | 典型搭配 |
|----------|------|----------|
| `Varchar` | 变长字符串 | TextEditor, ComboBox |
| `Integer` | 整数 | NumberEditor, ComboBox, CheckBox |
| `Long` | 长整数 | NumberEditor, Dict, DynamicDict, UTCDatePicker |
| `Numeric` | 定点数（需 Precision + Scale） | NumberEditor |
| `Date` | 日期 | DatePicker, MonthPicker |
| `DateTime` | 日期时间 | DatePicker, TimePicker |

## 按 RefControlType 分类的属性规则

### TextEditor — 文本编辑器

```xml
<Domain Key="Varchar_255" Caption="文本_255" RefControlType="TextEditor" DataType="Varchar" Length="255"/>
<Domain Key="Code_30" Caption="代码_30" RefControlType="TextEditor" DataType="Varchar" Length="30"/>
```

| 特有属性 | 必须 | 说明 |
|----------|------|------|
| `Length` | ✅ | 最大字符长度 |

→ 追加到 `DomainDef_TextEditor.xml`

---

### NumberEditor — 数字编辑器

**Numeric 类型（定点数）**：
```xml
<Domain Key="Money_16_2" Caption="金额_精度16_小数位2" RefControlType="NumberEditor" DataType="Numeric" Precision="16" Scale="2"/>
<Domain Key="Quantity_16_3" Caption="数量_精度16_小数位3" RefControlType="NumberEditor" DataType="Numeric" Precision="16" Scale="3"/>
```

**Integer / Long 类型**：
```xml
<Domain Key="Numeric_Integer" Caption="数值_整型" RefControlType="NumberEditor" DataType="Integer"/>
<Domain Key="Numeric_Long" Caption="数值_长整型" RefControlType="NumberEditor" DataType="Long"/>
```

| 特有属性 | 条件 | 说明 |
|----------|------|------|
| `Precision` | DataType=Numeric 时必须 | 总精度位数 |
| `Scale` | DataType=Numeric 时必须 | 小数位数 |

→ 追加到 `DomainDef_NumberEditor.xml`

---

### DatePicker — 日期选择器

```xml
<Domain Key="Date" Caption="日期" RefControlType="DatePicker" DataType="Date"/>
<Domain Key="DateTime" Caption="时间" RefControlType="DatePicker" DataType="DateTime"/>
```

无特有属性。→ 追加到 `DomainDef_DatePicker.xml`

---

### CheckBox — 复选框

```xml
<Domain Key="CheckBox" Caption="复选框" RefControlType="CheckBox" DataType="Integer"/>
```

无特有属性，通常 DataType 为 `Integer`。→ 追加到 `DomainDef_CheckBox.xml`

---

### Dict — 字典选择

```xml
<Domain Key="IM_InvestProgram" Caption="投资程序" RefControlType="Dict" DataType="Long" ItemKey="IM_InvestProgram"/>
```

| 特有属性 | 必须 | 说明 |
|----------|------|------|
| `ItemKey` | ✅ | 引用的字典 Key |
| `AllowMultiSelection` | ❌ | 是否多选（`true`/`false`） |

→ 追加到 `DomainDef_Dictionary.xml`（多选字典追加到 `DomainDef_DynamicDict.xml`）

---

### DynamicDict — 动态字典

```xml
<Domain Key="DY_Factory" Caption="工厂（动态）" RefControlType="DynamicDict" DataType="Long"/>
```

属性同 Dict。→ 追加到 `DomainDef_DynamicDict.xml` 提醒用户DynamicDict需要自行指定ItemKeyCollection

---

### ComboBox — 下拉框 ⭐

ComboBox 最复杂，有 **三种 SourceType 模式**：

#### 模式 1：ParaGroup（参数组引用）

```xml
<Domain Key="BudgetType" Caption="预算类型" RefControlType="ComboBox" DataType="Integer" SourceType="ParaGroup" GroupKey="BudgetType"/>
```

| 属性 | 说明 |
|------|------|
| `SourceType` | 固定为 `ParaGroup` |
| `GroupKey` | 引用的参数组 Key |
| `Length` | Varchar 时可选 |

无子元素。

#### 模式 2：Items（内联枚举）

```xml
<Domain Key="AdjustStatus" Caption="状态" RefControlType="ComboBox" DataType="Integer" SourceType="Items">
    <Item Key="0" Caption="未开始" Value="0"/>
    <Item Key="1" Caption="进行中" Value="1"/>
    <Item Key="2" Caption="已完成" Value="2"/>
</Domain>
```

| 属性 | 说明 |
|------|------|
| `SourceType` | 固定为 `Items` |
| `Length` | Varchar 时可选 |
| 子元素 `Item` | `Key`(必须) + `Caption`(必须) + `Value`(必须) |

#### 模式 3：Formula（公式计算）

```xml
<Domain Key="AvsCondition" Caption="条件" RefControlType="ComboBox" DataType="Integer" SourceType="Formula"/>
```

| 属性 | 说明 |
|------|------|
| `SourceType` | 固定为 `Formula` |
| `Length` | Varchar 时可选 |

无子元素、无 GroupKey。

#### 模式 4：Status（状态）

```xml
<Domain Key="BusinessTripStatus" Caption="状态" RefControlType="ComboBox" DataType="Integer" SourceType="Status"/>
```

无子元素。

→ 所有 ComboBox 追加到 `DomainDef_ComboBox.xml`

---

### 其他控件类型

| RefControlType | DataType | 目标文件 | 说明 |
|----------------|----------|----------|------|
| `UTCDatePicker` | `Long` | `DomainDef_UTCDatePicker.xml` | UTC 日期 |
| `MonthPicker` | `Date` | `DomainDef_MonthPicker.xml` | 月份 |
| `TimePicker` | `DateTime` | `DomainDef_TimePicker.xml` | 时间 |
| `CheckListBox` | `Varchar` | `DomainDef_CheckListBox.xml` | 复选列表 |
| `TextArea` | `Varchar` | `DomainDef_TextArea.xml` | 多行文本 |
| `PasswordEditor` | `Varchar` | `DomainDef_PasswordEditor.xml` | 密码 |
| `RichEditor` | `Varchar` | `DomainDef_RichEditor.xml` | 富文本 |
| `Image` | `Varchar` | `DomainDef_Image.xml` | 图片 |
| `Button` | — | `DomainDef_Button.xml` | 按钮 |
| `TextButton` | — | `DomainDef_TextButton.xml` | 文本按钮 |
| `Separator` | — | `DomainDef_Separator.xml` | 分隔线 |
| `WebBrowser` | `Varchar` | `DomainDef_WebBrowser.xml` | 浏览器 |

## 生成规则

### 1. 选择 RefControlType

根据用户描述的字段用途自动推断：

| 字段描述 | RefControlType | DataType |
|----------|----------------|----------|
| 文本、代码、名称 | TextEditor | Varchar |
| 数字、金额、数量、价格 | NumberEditor | Numeric/Integer/Long |
| 日期 | DatePicker | Date/DateTime |
| 是否、勾选 | CheckBox | Integer |
| 字典引用、选择实体 | Dict | Long |
| 下拉选择、状态、类型枚举 | ComboBox | Varchar/Integer |
| 密码 | PasswordEditor | Varchar |
| 大段文本、描述、备注 | TextArea | Varchar |

### 2. Key 命名

- PascalCase，含模块前缀时用 `_` 分隔（如 `AM_InvestmentReason`）
- 描述精度的 Key 格式：`{类型}_{精度}_{小数位}`（如 `Money_16_2`、`Numeric_5_2`）

### 3. 追加位置

新条目追加到目标文件的 `</DomainCollection>` **前一行**（保持缩进一致）。

## 生成示例

用户请求：生成"订单状态"下拉框 Domain，枚举值为 0=未提交、1=已提交、2=已审批

```xml
<Domain Key="OrderStatus" Caption="订单状态" RefControlType="ComboBox" DataType="Integer" SourceType="Items">
    <Item Key="0" Caption="未提交" Value="0"/>
    <Item Key="1" Caption="已提交" Value="1"/>
    <Item Key="2" Caption="已审批" Value="2"/>
</Domain>
```

→ 追加到 `DomainDef_ComboBox.xml` 的 `<DomainCollection>` 内。
