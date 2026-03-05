---
name: yigo-dataelement-generator
description: 生成与解读 YIGO DataElement XML 配置，定义字段级元数据（Key、Caption、DomainKey、FieldLabel 多语言标签），按 RefControlType 追加到对应的 DataElementDef_{Type}.xml 文件
---

# YIGO DataElement 生成

## 概述

DataElement（数据元素）是 YIGO 系统的 **字段级元数据定义**。每个 DataElement 关联一个 Domain（数据域），并携带显示标题、多语言标签、变更日志标记等信息。

DataElement 文件按 **RefControlType 分文件组织**，每种控件类型对应一个 `DataElementDef_{Type}.xml`。生成新条目时需追加到对应文件中。

## XSD 参考

- 根定义：[DataElementDef.xsd](../xsd/DataElementDef.xsd)
- 专有属性：[DataElementDefDefine.xsd](../xsd/element/complex/DataElementDefDefine.xsd)

## XML 骨架

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<DataElementDef>
    <DataElementCollection>
        <DataElement .../>
        <!-- 或 -->
        <DataElement ...>
            <FieldLabelCollection>...</FieldLabelCollection>
        </DataElement>
    </DataElementCollection>
</DataElementDef>
```

## DataElement 属性

| 属性 | 必须 | 说明 |
|------|------|------|
| `Key` | ✅ | 唯一标识，PascalCase（如 `InvestReasonID`） |
| `Caption` | ✅ | 中文显示名（如 `投资原因`） |
| `DomainKey` | ✅ | 关联的 Domain Key（如 `Varchar_255`、`Money_16_2`、`Date`） |
| `DataDiffLog` | ❌ | 变更日志跟踪，`true`/`false`（绝大多数为 `true`） |
| `ParamID` | ❌ | 运行时参数标识（罕见） |

## FieldLabelCollection

可选子元素，提供 4 种长度的多语言标签。

### 完整格式（推荐）

```xml
<DataElement Key="InvestReasonID" Caption="投资原因" DomainKey="AM_InvestmentReason" DataDiffLog="true">
    <FieldLabelCollection>
        <FieldLabel Key="Short" Length="10" Text="投资原因" TextEn="Investment Reason"/>
        <FieldLabel Key="Medium" Length="15" Text="投资原因" TextEn="Investment Reason"/>
        <FieldLabel Key="Long" Length="20" Text="投资原因" TextEn="Investment Reason"/>
        <FieldLabel Key="Header" Text="投资原因" TextEn="Investment Reason"/>
    </FieldLabelCollection>
</DataElement>
```

### 简写格式（无 Length）

当 4 种标签文本完全相同且不需要指定显示长度时：

```xml
<DataElement Key="ResultKey" Caption="信息" DomainKey="Varchar_255" DataDiffLog="true">
    <FieldLabelCollection>
        <FieldLabel Key="Short" Text="信息" TextEn="Info"/>
        <FieldLabel Key="Medium" Text="信息" TextEn="Info"/>
        <FieldLabel Key="Long" Text="信息" TextEn="Info"/>
        <FieldLabel Key="Header" Text="信息" TextEn="Info"/>
    </FieldLabelCollection>
</DataElement>
```

### 最简格式（无 FieldLabel）

仅当不需要多语言标签时使用自闭合标签：

```xml
<DataElement Key="AccessControl" Caption="是否访问控制" DomainKey="AccessControl"/>
```

### FieldLabel 属性

| 属性 | 必须 | 说明 |
|------|------|------|
| `Key` | ✅ | 标签类型：`Short` / `Medium` / `Long` / `Header` |
| `Text` | ✅ | 中文标签文本 |
| `TextEn` | ✅ | 英文标签文本 |
| `Length` | ❌ | 显示长度（数字、字符数） |

## 生成规则

### 1. 确定目标文件

根据字段关联的 DomainKey **对应的 RefControlType** 确定写入哪个文件：

| DomainKey 模式 | RefControlType | 目标文件 |
|----------------|----------------|----------|
| `Varchar_*`、`Name_*`、`Code_*` | TextEditor | `DataElementDef_TextEditor.xml` |
| `Money_16_2` | NumberEditor | `DataElementDef_NumberEditor_Money_16_2.xml` |
| `Quantity_16_3` | NumberEditor | `DataElementDef_NumberEditor_Quantity_16_3.xml` |
| `Numeric_*`、`Price_*` 等其他数值 | NumberEditor | `DataElementDef_NumberEditor.xml` |
| `Date`、`DateTime` | DatePicker | `DataElementDef_DatePicker.xml` |
| `CheckBox` | CheckBox | `DataElementDef_CheckBox.xml` |
| 字典类 DomainKey（DataType=Long, ItemKey=xxx） | Dict | `DataElementDef_Dictionary.xml` |
| ComboBox 类 DomainKey | ComboBox | `DataElementDef_ComboBox.xml` |
| 系统字段类 | SystemField | `DataElementDef_SystemField.xml` |

> 当不确定 DomainKey 对应的 RefControlType 时，查看 `resource/Domain/` 下对应的 DomainDef 文件确认。

### 2. Key 命名

- 使用 **PascalCase**（如 `OrderStatus`、`CompanyCode`）
- 业务术语保持英文（如 `AM_` 前缀表示资产管理模块）
- Key 可以与 DomainKey 相同（如 `AccountType` → DomainKey `AccountType`）
- Key 也可以与 DomainKey 不同（如 `ReconAccountType` → DomainKey `AccountType`）

### 3. DataDiffLog 默认值

- 取决于字段是否要记录差异日志

### 4. FieldLabel 生成

- **默认行为**：生成完整的 4 个 FieldLabel（Short/Medium/Long/Header）
- **Text**：与 Caption 相同或略作简化
- **TextEn**：Caption 的英文翻译
- **Length**：可选，按需设置（Short 常用 10，Medium 常用 15，Long 常用 20）

### 5. 追加位置

新条目追加到目标文件的 `</DataElementCollection>` **前一行**（保持与文件中已有条目同级缩进）。

## 生成示例

用户请求：为"公司代码"字段生成 DataElement，类型为文本（Varchar 30 位）

```xml
<DataElement Key="CompanyCode" Caption="公司代码" DomainKey="Code_30" DataDiffLog="true">
    <FieldLabelCollection>
        <FieldLabel Key="Short" Length="10" Text="公司代码" TextEn="Company Code"/>
        <FieldLabel Key="Medium" Length="15" Text="公司代码" TextEn="Company Code"/>
        <FieldLabel Key="Long" Length="20" Text="公司代码" TextEn="Company Code"/>
        <FieldLabel Key="Header" Text="公司代码" TextEn="Company Code"/>
    </FieldLabelCollection>
</DataElement>
```

→ 追加到 `DataElementDef_Dictionary.xml` 的 `<DataElementCollection>` 内。
