---
name: sap-permission-data-generator
description: 生成与解读 SAP 权限（SU21, SU24, TCode等）相关预定义数据 XML，用于补充 YIGO ERP 中的权限对象、权限字段、事务码及事务码权限缺省值等初始化数据。
---

# SAP 权限相关预定义数据生成

## 概述

本 Skill 基于 `yigo-predefined-data-generator`（YIGO 预定义数据生成规范），专门针对 **SAP 权限体系** 的相关表单（非报表）生成初始化数据或升级数据。
本 Skill 负责在对应的模块（如 `erp-solution-core/authorityConfig/initializeData/` 或其它业务模块的初始化数据目录）下提供配置数据。

> **核心原则**：SAP 的权限体系核心对象（权限对象类、权限对象、权限字段、作业值、事务码、SU24缺省值）均有对应的 Form 和 DataObject，生成的 XML 须符合 DataObject 的字段映射。

---

## 核心权限模型映射表

在补充 SAP 权限数据时，需参考以下映射关系组装 XML：

| SAP 概念 | 对应 Form Key | 对应 DataObject Key | Root 元素名 | 表名 (Row 元素名) | 核心字段说明 |
|---|---|---|---|---|---|
| **权限对象类 (TOBC)** | `AuthorityObjectClass` | `AuthorityObjectClass` | `<AuthorityObjectClasss>` | `<EAU_AuthorityObjectClass>` | `Code` (代码), `Name` (多语言名称) |
| **权限对象 (TOBJ)** | `AuthorityObject` | `AuthorityObject` | `<AuthorityObjects>` | `<EAU_AuthorityObject>` | `Code` (代码), `AuthorityObjectClassID` (权限类), `AuthorityFieldID01`~`10` (引用权限字段) |
| **权限字段 (AUTH_FLD)** | `AuthorityField` | `AuthorityField` | `<AuthorityFields>` | `<EAU_AuthorityField>` | `Code` (字段名), `DataElementKey` (数据元素) |
| **权限作业值 (TACT)** | `AuthorityActivity` | `AuthorityActivity` | `<AuthorityActivitys>` | `<EAU_AuthorityActivity>` | `Code` (作业值), `Name` (描述) |
| **事务码 (TCode)** | `TCode` | `TCode` | `<TCodes>` | `<EGS_TCode>` | `Code` (事务码), `TransactionCodePackageID` (包) |
| **权限组织变量** | `AuthorityOrgVariable` | `AuthorityOrgVariable` | `<AuthorityOrgVariables>` | `<EAU_AuthorityOrgVariable>` | `Code` (代码), `Name` (多语言名称) |
| **权限对象作业值** | `AuthorityObjectActivity` | `AuthorityObjectActivity` | `<AuthorityObjectActivitys>` | `<EAU_AuthorityObjectActivity>` | `AuthorityObjectID` (权限对象), `AuthorityActivityID` (作业值) |
| **菜单与事务码关系** | `EntryTCodeRelation` | `EntryTCodeRelation` | `<EntryTCodeRelations>` | `<EAU_EntryTCodeRelation>` | `EntryKey` (菜单标识), `EntryTCode` (对应的事务码) |
| **事务码权限缺省值 (标准模板)**| `TCodeAuthorityObjectFieldDefaultValue`| `TCodeAuthorityFieldDefaultValue` | `<TCodeAuthorityFieldDefaultValues>` | `<EAU_TCodeAuthObjectDefRel>` | `TCodeID` (事务代码), `AuthorityObjectID` (权限对象) |
| **事务码权限缺省值 (项目可改)**| `TCodeAuthorityObjectFieldValue` | `TCodeAuthorityFieldValue` | `<TCodeAuthorityFieldValues>` | `<EAU_TCodeAuthorityObjectRelDtl>`| 同上，主表变为 `EAU_TCodeAuthorityObjectRelDtl` |

---

## 常用生成示例

### 1. 权限对象类 (AuthorityObjectClass)

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<AuthorityObjectClasss>
    <EAU_AuthorityObjectClass ClientID="000" Code="MM_M" Enable="1" NodeType="0" Notes="">
        <EAU_AuthorityObjectClass_Ts>
            <EAU_AuthorityObjectClass_T Lang="zh-CN" Name="物料管理：主数据"/>
            <EAU_AuthorityObjectClass_T Lang="en-US" Name="Materials Management: Master Data"/>
        </EAU_AuthorityObjectClass_Ts>
    </EAU_AuthorityObjectClass>
</AuthorityObjectClasss>
```

### 2. 权限字段 (AuthorityField)

> 需注意：权限字段通常会配置其对应的数据元素 `DataElementKey`。

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<AuthorityFields>
    <EAU_AuthorityField ClientID="000" Code="ACTVT" Enable="1" NodeType="0" DataElementKey="AuthorityActivity" IsEmptyHasAuthority="1" Notes="作业">
        <EAU_AuthorityField_Ts>
            <EAU_AuthorityField_T Lang="zh-CN" Name="作业"/>
        </EAU_AuthorityField_Ts>
    </EAU_AuthorityField>
</AuthorityFields>
```

### 3. 权限对象 (AuthorityObject)

> 权限对象一般需引用权限对象类 (`AuthorityObjectClassID`) 以及 1-10 个权限字段 (`AuthorityFieldID01` 等)。

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<AuthorityObjects>
    <EAU_AuthorityObject ClientID="000" Code="M_MATE_MAR" Enable="1" NodeType="0" 
                         AuthorityObjectClassID="MM_M" AuthorityFieldID01="ACTVT" AuthorityFieldID02="BEGRU">
        <EAU_AuthorityObject_Ts>
            <EAU_AuthorityObject_T Lang="zh-CN" Name="物料主记录：物料类型"/>
        </EAU_AuthorityObject_Ts>
    </EAU_AuthorityObject>
</AuthorityObjects>
```

### 4. 权限组织变量 (AuthorityOrgVariable)

> 定义权限组织相关的变量，补充组织权限时使用。

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<AuthorityOrgVariables>
    <EAU_AuthorityOrgVariable ClientID="000" Code="WERKS" Enable="1" NodeType="0">
        <EAU_AuthorityOrgVariable_Ts>
            <EAU_AuthorityOrgVariable_T Lang="zh-CN" Name="工厂"/>
        </EAU_AuthorityOrgVariable_Ts>
    </EAU_AuthorityOrgVariable>
</AuthorityOrgVariables>
```

### 5. 权限对象可使用的作业值 (AuthorityObjectActivity)

> 定义权限对象能使用哪些权限作业值字段。用于初始化绑定 AuthObject 和 Activity。

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<AuthorityObjectActivitys>
    <!-- M_MATE_MAR 权限对象允许的作业值: 01, 02, 03 -->
    <EAU_AuthorityObjectActivity AuthorityObjectID="M_MATE_MAR" AuthorityActivityID="01"/>
    <EAU_AuthorityObjectActivity AuthorityObjectID="M_MATE_MAR" AuthorityActivityID="02"/>
    <EAU_AuthorityObjectActivity AuthorityObjectID="M_MATE_MAR" AuthorityActivityID="03"/>
</AuthorityObjectActivitys>
```

### 6. 菜单与事务码关系 (EntryTCodeRelation)

> 将菜单(Entry)和主权限事务码绑定，用于在新增菜单时默认赋予其对应的底层事务码。
> 可选子表 `EAU_EntryOptTCodeRelation` 用于绑定该菜单操作下的其他事务码。

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<EntryTCodeRelations>
    <!-- EntryKey 中通常会有 __OldPrimaryValue 用于升级模式映射 -->
    <EAU_EntryTCodeRelation EntryKey="subsys_MM_MM01" EntryTCode="MM01" RefFormKey="Material" __OldPrimaryValue="10001">
        <EAU_EntryOptTCodeRelations>
            <EAU_EntryOptTCodeRelation Sequence="1" TCode="MM02" TCodeText="修改物料" POID="10001"/>
            <EAU_EntryOptTCodeRelation Sequence="2" TCode="MM03" TCodeText="显示物料" POID="10001"/>
        </EAU_EntryOptTCodeRelations>
    </EAU_EntryTCodeRelation>
</EntryTCodeRelations>
```

### 7. 事务码权限缺省值 (SU24)

事务码权限缺省值存在两份相似但用途不同的定义：
1. **TCodeAuthorityObjectFieldDefaultValue**: 系统标准参考模板 (不允许项目直接修改)。主表 `EAU_TCodeAuthObjectDefRel`，子表 `EAU_TCodeValidAuthFieldDef`。
2. **TCodeAuthorityObjectFieldValue**: 项目可修改版本 (作为项目实施时的独立隔离态)。主表 `EAU_TCodeAuthorityObjectRelDtl`，子表 `EAU_TCodeValidAuthFieldValue`。二者映射和结构相似但使用不同的 XML 标签和表名。

以项目可修版本的 **TCodeAuthorityObjectFieldValue** 为例：

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<!-- 注意这里的 Root、主子表名字 与标准不可改模板版不完全相同 -->
<TCodeAuthorityFieldValues>
    <EAU_TCodeAuthorityObjectRelDtl CheckType="Y" AuthorityObjectID="M_MATE_MAR" TCodeID="MM01" IsSpecialCheck="0">
        <!-- 权限字段默认值子表 -->
        <EAU_TCodeValidAuthFieldValues>
            <EAU_TCodeValidAuthFieldValue AuthorityFieldID="ACTVT" AuthorityFieldValue="01"/>
            <EAU_TCodeValidAuthFieldValue AuthorityFieldID="ACTVT" AuthorityFieldValue="02"/>
            <EAU_TCodeValidAuthFieldValue AuthorityFieldID="ACTVT" AuthorityFieldValue="03"/>
        </EAU_TCodeValidAuthFieldValues>
        <!-- 权限字段属性子表 (可选) -->
        <EAU_TCodeValidAuthFieldProps>
            <EAU_TCodeValidAuthFieldProp AuthorityFieldID="BEGRU" DataElementKey="MaterialGroup"/>
        </EAU_TCodeValidAuthFieldProps>
    </EAU_TCodeAuthorityObjectRelDtl>
</TCodeAuthorityFieldValues>
```

> **字段说明：**
> - **CheckType (检查类型)**: `Y` (检查/保持), `N` (不检查), `U` (未维护)。通常使用 `Y`。
> - **AuthorityFieldValue**: 权限字段具体的值，如对于 `ACTVT` 可能是 `01`(创建), `02`(修改), `03`(显示)。

### 8. 事务码 (TCode)

> 定义系统的事务码，及其默认的权限检查对象、挂载的表单等。TCode数据通常放在各个业务模块的基础预定义数据中。

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<TCodes>
    <EGS_TCode Code="MM01" Enable="1" NodeType="0" Notes="" OperateType="1" TransactionCodePackageID="MM">
        <EGS_TCode_Ts>
            <EGS_TCode_T Lang="zh-CN" Name="创建物料"/>
        </EGS_TCode_Ts>
        <!-- 默认检查的权限对象集合 -->
        <EGS_TCode_DefaultCheckObjectss>
            <EGS_TCode_DefaultCheckObjects Sequence="1" AuthorityObjectID="M_MATE_MAR">
                <!-- 权限字段值集合 (挂在默认检查对象下) -->
                <EGS_TCode_ObjectFieldValuess>
                    <EGS_TCode_ObjectFieldValues Sequence="1" AuthorityFieldID="ACTVT" AuthorityFieldValue="01"/>
                </EGS_TCode_ObjectFieldValuess>
            </EGS_TCode_DefaultCheckObjects>
        </EGS_TCode_DefaultCheckObjectss>
        <!-- 绑定的表单集合 -->
        <EGS_TCode_FormLists>
            <EGS_TCode_FormList Sequence="1" FormKey="Material"/>
        </EGS_TCode_FormLists>
    </EGS_TCode>
</TCodes>
```

---

## 前置动作与校验规则

1.  **依赖字典的存在性**：在写入 `TCodeAuthorityFieldDefaultValues` 时，务必保证引用的 `AuthorityObjectID`、`AuthorityFieldID` 以及 `TCodeID` 在系统中已有数据或同时提供预定义数据。
2.  **遵守 YIGO 预定义生成规范**：本 Skill 在生成 XML 时，遵循 `yigo-predefined-data-generator` 中的所有原则（主外键忽略 `OID` 等，多语言表 `_Ts`，外键写 `Code` 值等）。
3.  **定位目录**：这类字典数据通常是在各个系统模块（如 `pmconfig/initializeData/` 等）存放，以配合该模块的 TCode 初始化。