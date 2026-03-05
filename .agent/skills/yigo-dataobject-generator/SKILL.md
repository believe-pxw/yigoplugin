---
name: yigo-dataobject-generator
description: 生成与解读 YIGO DataObject XML 配置，包含 TableCollection、Column、IndexCollection 等数据模型定义
---

# YIGO DataObject 数据对象生成

## 概述

本 Skill 负责生成 YIGO 系统的**数据对象（DataObject）XML 配置**。DataObject 定义了表单的数据模型，包含表集合、列定义、表间关系、嵌入表和索引等。

> **核心原则**：Column 优先使用 `DataElementKey` 引用数据元素（由 `yigo-dataelement-generator` 管理），通过 DataElement → Domain 链获取数据类型。如果DataObject是一个单独的文件，需要文件名与DataObject的Key相同。表标识必须要以E开头，如果已指定前缀，则在前缀前加上E

## XSD 参考文件

- 主文件：[DataObject.xsd](../xsd/DataObject.xsd)
- 详细定义：[DataObjectDefine.xsd](../xsd/element/complex/DataObjectDefine.xsd)

## 数据模型关系

```
Column.DataElementKey → DataElement.Key → DataElement.DomainKey → Domain.Key
                         (字段元数据)         (关联域)              (数据域定义)
```

- **DataElement**（`yigo-dataelement-generator`）：定义字段的 Key/Caption/DomainKey + 多语言标签
- **Domain**（`yigo-domain-generator`）：定义字段的 RefControlType/DataType/Length/Precision/Scale

## DataObject 完整结构

```xml
<DataObject Key="数据对象标识" Caption="名称" PrimaryType="Entity" 
            SecondaryType="Normal" PrimaryTableKey="主表Key">
    <!-- 1. 表集合 -->
    <TableCollection>
        <Table Key="表标识" Caption="表名称">
            <Column Key="列标识" Caption="列名称" DataElementKey="数据元素Key"/>
            <Column Key="Amount" DataType="Decimal" Precision="18" Scale="2"/>
            <TableFilter Type="Const">过滤条件</TableFilter>
            <ParameterCollection>
                <Parameter FieldKey="字段" TargetColumn="目标列" SourceType="Field"/>
            </ParameterCollection>
            <Statement Type="Formula">
                <![CDATA[SQL或公式]]>
            </Statement>
            <IndexCollection>
                <Index Key="idx1" Columns="col1,col2" IsUnique="true"/>
            </IndexCollection>
        </Table>
    </TableCollection>

    <!-- 2. 嵌入表集合 -->
    <EmbedTableCollection>
        <EmbedTable ObjectKey="对象标识" TableKeys="表1,表2"/>
    </EmbedTableCollection>
</DataObject>
```

## DataObject 属性

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `Key` | string(50) | ✅ | 数据对象唯一标识 |
| `Caption` | string | ❌ | 显示名称 |
| `PrimaryType` | 枚举 | ❌ | 主类型：`Entity`（实体）/ `Template`（模板） |
| `SecondaryType` | 枚举 | ❌ | 辅助类型，见下表 |
| `PrimaryTableKey` | string(50) | ❌ | 主表标识 |
| `Version` | string | ❌ | 配置版本 |
| `NoPrefix` | string | ❌ | 单据编号前缀 |
| `DisplayFields` | string | ❌ | 字典显示字段 |
| `DropviewFields` | string | ❌ | 字典下拉框显示列 |
| `QueryFields` | string | ❌ | 字典模糊查询字段 |
| `MaintainDict` | Boolean | ❌ | 是否维护字典的 tleft tright |
| `IndexPrefix` | string | ❌ | 索引前缀 |
| `BrowserFormKey` | Formula | ❌ | 浏览表单标识 |
| `QueryFormKey` | Formula | ❌ | 查询表单标识 |
| `RelateObjectKey` | string(50) | ❌ | 关联的数据对象（View 用） |
| `IOProvider` | string | ❌ | IO 工厂类标识 |
| `CheckAfterLoad` | Boolean | ❌ | 权限检查在加载后还是加载前 |
| `LoadRightsType` | `Deny` | ❌ | 加载权限类型 |
| `MigrationUpdateStrategy` | 枚举 | ❌ | 迁移表更新策略（仅迁移表用） |
| `DictCacheCheckMode` | string | ❌ | 字典缓存检查策略（仅字典表单用） |

## SecondaryType 枚举值

| 值 | 说明 |
|----|------|
| `Normal` | 普通实体数据 |
| `Dict` | 字典数据 |
| `ChainDict` | 链式字典数据 |
| `CompDict` | 复合字典数据 |
| `Migration` | 迁移表 |

## Table（表）属性

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `Key` | string(50) | ❌ | 表标识（持久化表 ≤ 30 字符） |
| `Caption` | string | ❌ | 表名称 |
| `TableMode` | `Detail` | ❌ | 表模式（明细表填 `Detail`，主表不填） |
| `PrimaryKey` | string | ❌ | 主键字段 |
| `Persist` | Boolean | ❌ | 是否持久化到数据库 |
| `SourceType` | 枚举 | ❌ | 来源类型：`Table`/`Query`/`Custom`/`Interface`/空 |
| `IndexPrefix` | string | ❌ | 索引前缀（明细表推荐配置） |
| `OrderBy` | string | ❌ | 排序方式 |
| `GroupBy` | string | ❌ | 分组方式 |
| `Formula` | Formula | ❌ | 表达式 |
| `Impl` | string | ❌ | 实现类 |
| `ParentKey` | string | ❌ | 父表标识 |
| `DBTableName` | string(30) | ❌ | 数据库表名（当与 Key 不同时） |
| `LazyLoad` | Boolean | ❌ | 是否延迟加载 |
| `RefreshFilter` | Boolean | ❌ | 是否刷新过滤条件 |

## Column（列）属性

### 推荐方式：引用 DataElementKey，如果DataElement中无定义，则新增数据元素

```xml
<Column Key="SchedulingIndicator" Caption="计划标识" Cache="true" DataElementKey="SchedulingIndicatorType"/>
```

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `Key` | string(50) | ✅ | 列标识（持久化列 ≤ 30 字符） |
| `Caption` | string | ❌ | 列名称 |
| `DataElementKey` | string | ❌ | 引用的数据元素 Key（**推荐**） |
| `DefaultValue` | string | ❌ | 默认值 |
| `PrimaryKey` | Boolean | ❌ | 是否主键（实际使用的属性名） |
| `Cache` | Boolean | ❌ | 是否缓存（字典字段推荐 `true`） |
| `Persist` | Boolean | ❌ | 是否持久化 |
| `SupportI18n` | Boolean | ❌ | 是否支持国际化 |
| `SortType` | `Asc`/`Desc` | ❌ | 排序类型 |
| `ItemKey` | string(50) | ❌ | 字典项标识（仅不使用 DataElementKey 时） |
| `RefCol` | string | ❌ | 引用列 |
| `RefItemKeyCol` | string(50) | ❌ | 引用字典项列 |
| `CodeColumnKey` | string | ❌ | 编码列标识 |
| `DBColumnName` | string | ❌ | 数据库列名 |
| `Expand` | Boolean | ❌ | 是否扩展 |
| `NeedRights` | Boolean | ❌ | 是否需要权限 |
| `IgnoreSave` | Boolean | ❌ | 是否忽略保存 |
| `IgnoreQuery` | Boolean | ❌ | 是否忽略查询 |
| `AccessControl` | Boolean | ❌ | 访问控制 |


## 字典表单系统字段

字典表单（`SecondaryType="Dict"`）的主表**必须包含**以下系统字段（使用 DataElementKey 引用）：

```xml
<!-- ========== 系统字段（必要） ========== -->
<Column Key="OID" Caption="对象标识" DataElementKey="OID"/>
<Column Key="SOID" Caption="主对象标识" DataElementKey="SOID"/>
<Column Key="POID" Caption="父对象标识" DataElementKey="POID"/>
<Column Key="VERID" Caption="对象版本" DataElementKey="VERID"/>
<Column Key="DVERID" Caption="对象明细版本" DataElementKey="DVERID"/>
<Column Key="Enable" Caption="启用标记" DefaultValue="1" DataElementKey="Enable"/><!--字典才必要-->
<Column Key="TLeft" Cache="true" DataElementKey="TLeft"/><!--字典才必要-->
<Column Key="TRight" Cache="true" DataElementKey="TRight"/><!--字典才必要-->
<Column Key="NodeType" Caption="节点类型" DataElementKey="NodeType"/><!--字典才必要-->
<Column Key="ParentID" Caption="上级节点" DataElementKey="ParentID"/><!--字典才必要-->
<Column Key="Code" Caption="代码" Cache="true" DefaultValue="" DataElementKey="Code" PrimaryKey="true"/><!--字典才必要-->
<Column Key="Name" Caption="名称" Persist="false" Cache="true" DefaultValue="" SupportI18n="true" DataElementKey="Name"/><!--字典才必要-->
<Column Key="ClientID" Caption="集团" DataElementKey="ClientID"/>
<Column Key="Creator" Caption="创建人员" DataElementKey="Creator"/>
<Column Key="CreateTime" Caption="创建时间" DataElementKey="CreateTime"/>
<Column Key="CreateDate" Caption="制单日期" DataElementKey="CreateDate"/>
<Column Key="Modifier" Caption="修改人员" DataElementKey="Modifier"/>
<Column Key="ModifyTime" Caption="修改时间" DataElementKey="ModifyTime"/>
<Column Key="SystemVestKey" Caption="单据Key" DataElementKey="SystemVestKey"/>
```

字典表单的**明细表**系统字段：

```xml
<Column Key="OID" Caption="对象标识" DataElementKey="OID"/>
<Column Key="SOID" Caption="主对象标识" DataElementKey="SOID"/>
<Column Key="POID" Caption="父对象标识" DataElementKey="POID"/>
<Column Key="VERID" Caption="对象版本" DataElementKey="VERID"/>
<Column Key="DVERID" Caption="对象明细版本" DataElementKey="DVERID"/>
<Column Key="Sequence" Caption="序号" DataElementKey="Sequence"/>
```

## 校验规则

1. **持久化的表** Key 长度 ≤ 30 字符
2. **持久化的列** Key 长度 ≤ 30 字符，否则 ≤ 50 字符
3. **存在 DataElementKey 时**不能配置 `Precision`、`Scale`、`Length`（XSD 强制）
4. **迁移表专有属性**（`GroupType`、`SplitType`、`PeriodImpl`）只有 `SecondaryType='Migration'` 时允许配置
5. **MigrationUpdateStrategy** 只有迁移表（`SecondaryType='Migration'`）允许配置
6. **DictCacheCheckMode** 只有字典表单（`FormType='Dict'`）允许配置
7. **Column Key 唯一**：同一 Table 下的 Column Key 不能重复

## 使用示例

### 示例：字典数据对象

```xml
<DataObject Key="PM_Strategy" Caption="维护策略" PrimaryTableKey="EPM_Strategy" SecondaryType="Dict" PrimaryType="Entity" Version="1">
    <TableCollection>
        <Table Key="EPM_Strategy" Caption="维护策略主表">
            <!-- 系统字段 -->
            <Column Key="OID" Caption="对象标识" DataElementKey="OID"/>
            <Column Key="SOID" Caption="主对象标识" DataElementKey="SOID"/>
            <Column Key="POID" Caption="父对象标识" DataElementKey="POID"/>
            <Column Key="VERID" Caption="对象版本" DataElementKey="VERID"/>
            <Column Key="DVERID" Caption="对象明细版本" DataElementKey="DVERID"/>
            <Column Key="Enable" Caption="启用标记" DefaultValue="1" DataElementKey="Enable"/>
            <Column Key="TLeft" Cache="true" DataElementKey="TLeft"/>
            <Column Key="TRight" Cache="true" DataElementKey="TRight"/>
            <Column Key="NodeType" Caption="节点类型" DataElementKey="NodeType"/>
            <Column Key="ParentID" Caption="上级节点" DataElementKey="ParentID"/>
            <Column Key="Code" Caption="代码" Cache="true" DefaultValue="" DataElementKey="Code" PrimaryKey="true"/>
            <Column Key="Name" Caption="名称" Persist="false" Cache="true" DefaultValue="" SupportI18n="true" DataElementKey="Name"/>
            <Column Key="ClientID" Caption="集团" DataElementKey="ClientID"/>
            <Column Key="Creator" Caption="创建人员" DataElementKey="Creator"/>
            <Column Key="CreateTime" Caption="创建时间" DataElementKey="CreateTime"/>
            <Column Key="CreateDate" Caption="制单日期" DataElementKey="CreateDate"/>
            <Column Key="Modifier" Caption="修改人员" DataElementKey="Modifier"/>
            <Column Key="ModifyTime" Caption="修改时间" DataElementKey="ModifyTime"/>
            <!-- 业务字段 -->
            <Column Key="Notes" Caption="备注" DefaultValue="" DataElementKey="Notes"/>
            <Column Key="SchedulingIndicator" Caption="计划标识" Cache="true" DataElementKey="SchedulingIndicatorType"/>
            <Column Key="StrategyUnitID" Caption="策略单位" Cache="true" DataElementKey="CycleUnitID"/>
            <Column Key="CallHorizon" Caption="调用期" Cache="true" DataElementKey="CallHorizon"/>
            <Column Key="SystemVestKey" Caption="单据Key" DataElementKey="SystemVestKey"/>
        </Table>
        <Table Key="EPM_StrategyDtl" Caption="维护策略明细" TableMode="Detail" IndexPrefix="EPM_StrategyDtl">
            <!-- 明细系统字段 -->
            <Column Key="OID" Caption="对象标识" DataElementKey="OID"/>
            <Column Key="SOID" Caption="主对象标识" DataElementKey="SOID"/>
            <Column Key="POID" Caption="父对象标识" DataElementKey="POID"/>
            <Column Key="VERID" Caption="对象版本" DataElementKey="VERID"/>
            <Column Key="DVERID" Caption="对象明细版本" DataElementKey="DVERID"/>
            <Column Key="Sequence" Caption="序号" DataElementKey="Sequence"/>
            <!-- 业务字段 -->
            <Column Key="CycleNotes" Caption="周期文本" DefaultValue="" DataElementKey="CycleNotes"/>
            <Column Key="CycleLength" Caption="周期长度" DataElementKey="PM_CycleLength"/>
            <Column Key="PackageNo" Caption="数据包" DataElementKey="PackageNo" PrimaryKey="true"/>
        </Table>
    </TableCollection>
</DataObject>
```

## 与其他 Skill 的配合

| 配合 Skill | 关系说明 |
|------------|----------|
| `yigo-dataelement-generator` | Column 的 `DataElementKey` 引用 DataElement 定义 → 需确保被引用的 DataElement 存在于对应的 `DataElementDef_{Type}.xml` 中 |
| `yigo-domain-generator` | DataElement 的 `DomainKey` 引用 Domain 定义 → 需确保被引用的 Domain 存在于对应的 `DomainDef_{Type}.xml` 中 |
| `yigo-form-scaffold` | DataObject 嵌套在 Form XML 的 `<DataSource>` 下 |
| `yigo-control-generator` | DataObject 的 Table/Column 供 UI 控件做 DataBinding（`TableKey` + `ColumnKey`） |
| `yigo-grid-generator` | Grid 列绑定 DataObject 的 Column |

### 生成流程

当生成一个完整的表单数据模型时，需按以下顺序协作：

1. **Domain**（`yigo-domain-generator`）→ 定义或复用数据域，追加到 `DomainDef_{Type}.xml`
2. **DataElement**（`yigo-dataelement-generator`）→ 定义字段元数据，追加到 `DataElementDef_{Type}.xml`
3. **DataObject**（本 Skill）→ 构建数据对象，Column 通过 `DataElementKey` 引用上述定义
