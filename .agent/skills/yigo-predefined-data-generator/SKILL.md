---
name: yigo-predefined-data-generator
description: 生成与解读 YIGO 预定义数据 XML，用于系统初始化（initializeData）和升级（预定义数据），支持单表/父子表/多语言结构，含 __OldPrimaryValue/POID 父子关联规则
---

# YIGO 预定义数据生成

## 概述

本 Skill 负责生成 YIGO 系统的**预定义数据 XML 文件**。预定义数据是与 Form 表单对应的初始化/升级数据，其 XML 结构直接映射 DataObject 的表结构（Table/Column），列值以 XML 属性形式承载。

> **核心原则**：每个预定义数据 XML 文件对应一个 DataObject（Form），文件名与 Form 的 Key 一致。XML 元素标签 = DataObject 中的 Table Key，XML 属性名 = Column Key，属性值 = 预定义的业务数据。

### 数据字典的引用关系与校验
预定义数据中如果存在**字典类型（Dict）**的字段值，务必保证其引用的字典数据在预定义数据文件中已存在，以维持数据的勾稽关系。

**勾稽关系追溯链路：**
1. 获取 Column 的 `DataElementKey` 属性，找到对应的 DataElement 定义。
2. 从 DataElement 的定义中，获取其关联的 `DomainKey`。
3. 从 Domain 的定义中，查找 `ItemKey`，这对应字典的数据对象（DataObject）的 Key。
4. 字典的数据对象 Key 与它对应的 Form Key 一致。
5. 验证该预定义字典数据文件（`{ItemKey}.xml`）中，是否存在一条记录，其 `Code` 字段的值与当前预定义数据中所填入的属性值相等。

**示例**：
在 `TCodeAuthorityObjectFieldValue.xml` 中，有一个属性 `AuthorityObjectID="S_USER_GRP"`：
- 字段 `AuthorityObjectID` 的 DataElement 是 `AuthorityObjectID`。
- DataElement `AuthorityObjectID` 对应的 Domain 是 `AuthorityObject`。
- Domain `AuthorityObject` 中配置了 `ItemKey="AuthorityObject"`。
- 去找数据对象为 `AuthorityObject` （进而 Form 也为 `AuthorityObject`）的预定义数据文件，即 `AuthorityObject.xml`（可能在此模块也可能在其它被依赖模块的预定义数据文件夹中）。
- 在 `AuthorityObject.xml` 中必须存在一条 `Code="S_USER_GRP"` 的记录，以此确保数据的完整性。

| 场景 | 目录 | 用途 |
|------|------|------|
| **系统初始化** | `initializeData/` | 新系统部署时导入的基础数据 |

## 目录位置

```
solutions/
├── erp-solution-core/
│   └── {module}config/          # 如 pmconfig, mmconfig, basisconfig
│       └── initializeData/       # ★ 初始化数据
│           └── {FormKey}.xml
```

## XML 整体结构

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<{FormKey}s>                              <!-- 根元素 = FormKey + "s" -->
    <{主表Key} 列1="值" 列2="值" ...>     <!-- 主表行 = 主表 Table Key -->
        <{子表Key}s>                      <!-- 子表容器 = 子表 Table Key + "s" -->
            <{子表Key} 列A="值" .../> 
        </{子表Key}s>
    </{主表Key}>
</{FormKey}s>
```

## 命名规则

### 根元素

根元素名 = **FormKey（DataObject Key） + "s"**（表示复数集合）

| FormKey | 根元素 |
|---------|--------|
| `PM_Strategy` | `<PM_Strategys>` |
| `PM_OrderType` | `<PM_OrderTypes>` |
| `Message` | `<Messages>` |
| `TCode` | `<TCodes>` |

### 行元素

行元素名 = **DataObject 中对应的 Table Key**（以 `E` 开头的表标识）

| Table Key | 行元素 |
|-----------|--------|
| `EPM_Strategy` | `<EPM_Strategy .../>` |
| `EPM_StrategyDtl` | `<EPM_StrategyDtl .../>` |
| `EGS_TCode` | `<EGS_TCode .../>` |
| `EGS_Message` | `<EGS_Message .../>` |

### 子表容器

子表行被包裹在容器元素中，容器名 = **子表 Table Key + "s"**

```xml
<EPM_Strategy ...>              <!-- 主表行 -->
    <EPM_StrategyDtls>           <!-- 子表容器 = EPM_StrategyDtl + "s" -->
        <EPM_StrategyDtl .../>   <!-- 子表行 -->
    </EPM_StrategyDtls>
</EPM_Strategy>
```

## 列值映射规则

DataObject Column 的 Key 作为 XML 属性名，值为预定义数据：

```xml
<!-- DataObject 定义 -->
<Column Key="Code" Caption="代码" DataElementKey="Code"/>
<Column Key="ClientID" Caption="集团" DataElementKey="ClientID"/>
<Column Key="Enable" Caption="启用标记" DefaultValue="1" DataElementKey="Enable"/>

<!-- 对应的预定义数据 -->
<EPM_ABCIndicator ClientID="000" Code="1_A" Enable="1" NodeType="0" .../>
```

> **注意**：系统字段（OID、SOID、POID、VERID、DVERID）在**初始化数据**中通常**省略**（由系统自动生成），但在**升级数据**中可能包含 `__OldPrimaryValue` 来标识记录。

## 多语言子表 (_Ts / _T)

对于包含 `SupportI18n="true"` 的 Name 字段，预定义数据通过专用的多语言子表承载：

```xml
<EPM_OrderType Code="PM01" ClientID="000" ...>
    <EPM_OrderType_Ts>                               <!-- 容器 = 主表Key + "_Ts" -->
        <EPM_OrderType_T Lang="zh-CN" Name="维护订单"/>  <!-- 行 = 主表Key + "_T" -->
        <EPM_OrderType_T Lang="en-US" Name="Maintain orders"/>
        <EPM_OrderType_T Lang="ja-JP" Name="保守オーダー"/>
    </EPM_OrderType_Ts>
</EPM_OrderType>
```

### 多语言子表命名规则

| 主表 Key | 多语言容器 | 多语言行 |
|----------|-----------|---------|
| `EPM_OrderType` | `EPM_OrderType_Ts` | `EPM_OrderType_T` |
| `EGS_TCode` | `EGS_TCode_Ts` | `EGS_TCode_T` |
| `EGS_Message` | `EGS_Message_Ts` | `EGS_Message_T` |

### 常用语言代码

| Lang | 语言 |
|------|------|
| `zh-CN` | 简体中文（**必填**） |
| `zh-CHT` | 繁体中文 |
| `en-US` | 英文 |
| `ja-JP` | 日文 |
| `ko-KR` | 韩文 |
| `de-DE` | 德文 |
| `fr-FR` | 法文 |
| `es-ES` | 西班牙文 |
| `pt-PT` | 葡萄牙文 |
| `ru-RU` | 俄文 |
| `ar-AE` | 阿拉伯文 |
| `th-TH` | 泰文 |
| `hu-HU` | 匈牙利文 |

> **最低要求**：至少包含 `zh-CN`。完整的初始化数据建议包含所有语言。

## 父子表关联（升级场景）

在升级数据中，父子表通过 `__OldPrimaryValue` 与 `POID` 关联：

```xml
<!-- 父表行：__OldPrimaryValue 标识此记录的主键 -->
<EAU_EntryTCodeRelation EntryTCode="QA33" EntryKey="subsys_QM_QA32_X2" 
                        RefFormKey="QM_QA32" __OldPrimaryValue="988124">
    <EAU_EntryOptTCodeRelations>
        <!-- 子表行：POID 引用父表的 __OldPrimaryValue -->
        <EAU_EntryOptTCodeRelation Sequence="1" TCode="QA02" 
                                   TCodeText="修改检验批" POID="988124"/>
        <EAU_EntryOptTCodeRelation Sequence="2" TCode="QA03" 
                                   TCodeText="显示检验批" POID="988124"/>
    </EAU_EntryOptTCodeRelations>
</EAU_EntryTCodeRelation>
```

### 关联规则

| 属性 | 所在位置 | 说明 |
|------|---------|------|
| `__OldPrimaryValue` | 父表行 | 标识父记录的主键值（数字 ID） |
| `POID` | 子表行 | 子记录指向父记录的外键，值 = 父表的 `__OldPrimaryValue` |

## 常见预定义数据类型

### 1. 字典类数据（Dict）

字典类 Form 的预定义数据最常见，通常包含 Code、Name（多语言）、ClientID 等字段。

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<PM_ABCIndicators>
    <EPM_ABCIndicator ABCIndicatorType="1" ClientID="000" Code="1_A" 
                      Enable="1" NodeType="0" Notes="" UseCode="A">
        <EPM_ABCIndicator_Ts>
            <EPM_ABCIndicator_T Lang="zh-CN" Name="A"/>
        </EPM_ABCIndicator_Ts>
    </EPM_ABCIndicator>
    <EPM_ABCIndicator ABCIndicatorType="1" ClientID="000" Code="1_B" 
                      Enable="1" NodeType="0" Notes="" UseCode="B">
        <EPM_ABCIndicator_Ts>
            <EPM_ABCIndicator_T Lang="zh-CN" Name="B"/>
        </EPM_ABCIndicator_Ts>
    </EPM_ABCIndicator>
</PM_ABCIndicators>
```

### 2. 带明细行的数据（父子表）

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<PM_Strategys>
    <EPM_Strategy CallHorizon="0" ClientID="000" Code="A" Enable="1" 
                  SchedulingIndicator="0" StrategyUnitID="mth">
        <EPM_StrategyDtls>
            <EPM_StrategyDtl CycleLength="1" CycleNotes="月" 
                             PackageNo="1" PackageUnitID="mth"/>
            <EPM_StrategyDtl CycleLength="3" CycleNotes="季度" 
                             PackageNo="2" PackageUnitID="mth"/>
        </EPM_StrategyDtls>
        <EPM_Strategy_Ts>
            <EPM_Strategy_T Lang="zh-CN" Name="A"/>
            <EPM_Strategy_T Lang="en-US" Name="A"/>
        </EPM_Strategy_Ts>
    </EPM_Strategy>
</PM_Strategys>
```

### 3. 事务码数据（TCode）—— 多级子表

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<TCodes>
    <EGS_TCode Code="CL01" Enable="1" NodeType="0" Notes="" 
               OperateType="1" RefTCode="CL01" TransactionCodePackageID="MM">
        <EGS_TCode_Ts>
            <EGS_TCode_T Lang="zh-CN" Name="分类创建"/>
        </EGS_TCode_Ts>
        <EGS_TCode_DefaultCheckObjectss>
            <EGS_TCode_DefaultCheckObjects AuthorityObjectID="C_KLAH_BKP"/>
        </EGS_TCode_DefaultCheckObjectss>
        <EGS_TCode_ObjectFieldValuess>
            <EGS_TCode_ObjectFieldValues AuthorityFieldID="ACTVT" 
                                         AuthorityFieldValue="01"/>
        </EGS_TCode_ObjectFieldValuess>
        <EGS_TCode_FormLists>
            <EGS_TCode_FormList FormKey="Classification"/>
        </EGS_TCode_FormLists>
    </EGS_TCode>
</TCodes>
```

### 4. 消息类数据（Message）

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<Messages>
    <EGS_Message ClientID="000" Code="GLVCHFMCOSETTLE006" Enable="1" 
                 MessageClassID="GLVCHFMCOSETTLE" MsgLongText="" 
                 MsgType="I" NodeType="0" Notes="" UseCode="006">
        <EGS_Message_Ts>
            <EGS_Message_T Lang="zh-CN" Name="成本控制生产订单结算"/>
        </EGS_Message_Ts>
    </EGS_Message>
</Messages>
```

### 5. 纯扁平数据（无子表）

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<TCodeAuthorityObjectFieldDefaultValues>
    <EAU_TCodeAuthObjectDefRel AuthorityObjectID="Y_PLANT" 
                                CheckType="Y" TCodeID="OIOB"/>
</TCodeAuthorityObjectFieldDefaultValues>
```

## 生成步骤

1. **确定 FormKey**：获取目标数据对应的 Form Key（DataObject Key）
2. **获取 DataObject 表结构**：从 DataObject XML 中提取 TableCollection 的所有 Table 和 Column 定义
3. **确定根元素**：根元素名 = `FormKey + "s"`
4. **生成行数据**：每个数据行 = 主表 Table Key 作为元素名，Column Key 作为属性名
5. **处理子表**：如有子表，创建子表容器（子表Key + "s"），内嵌子表行
6. **处理多语言**：如有 `SupportI18n` 字段，生成 `_Ts` / `_T` 多语言子表
7. **设置关联**（仅升级）：父表加 `__OldPrimaryValue`，子表加 `POID` 引用

## 字典数据常用系统字段

字典类预定义数据中，以下字段通常**需要填写**：

| 字段（属性） | 说明 | 典型值 |
|-------------|------|--------|
| `Code` | 字典代码（主键） | 业务编码 |
| `ClientID` | 集团代码 | `"000"` |
| `Enable` | 启用标记 | `"1"` |
| `NodeType` | 节点类型 | `"0"` |
| `Notes` | 备注 | `""` |

以下字段通常**省略**（系统自动处理）：

| 字段 | 说明 |
|------|------|
| `OID` | 对象标识 |
| `SOID` | 主对象标识 |
| `POID` | 父对象标识（初始化时省略） |
| `VERID` | 对象版本 |
| `DVERID` | 对象明细版本 |
| `TLeft` / `TRight` | 树形结构左右值 |
| `Creator` / `CreateTime` | 创建人/时间 |
| `Modifier` / `ModifyTime` | 修改人/时间 |

## 校验规则

1. **文件名必须与 FormKey 一致**：`{FormKey}.xml`
2. **根元素名 = FormKey + "s"**
3. **行元素名 = DataObject 中的 Table Key**
4. **属性名 = DataObject 中的 Column Key**
5. **XML 声明**：必须以 `<?xml version="1.0" encoding="UTF-8" standalone="no"?>` 开头
6. **多语言至少包含 `zh-CN`**
7. **升级数据中有子表时**：父表必须有 `__OldPrimaryValue`，子表必须有对应的 `POID`
8. **属性值中的特殊字符**需转义：`&` → `&amp;`，`<` → `&lt;`，`>` → `&gt;`，`"` → `&quot;`

## 与其他 Skill 的配合

| 配合 Skill | 关系说明 |
|------------|----------|
| `yigo-dataobject-generator` | 预定义数据的表结构（元素/属性）完全映射 DataObject 的 Table/Column 定义 |
| `yigo-form-scaffold` | 预定义数据的 FormKey 对应 Form 的 Key |
| `yigo-dataelement-generator` | 通过 DataElement 可了解字段的数据类型，确保属性值格式正确 |
| `yigo-domain-generator` | 通过 Domain 可获知 ComboBox 枚举值，预定义数据中的值必须在枚举范围内 |

### 生成流程

生成预定义数据前，需先确保以下已就绪：

1. **DataObject**（`yigo-dataobject-generator`）→ 获取表结构和列定义
2. **Domain**（`yigo-domain-generator`）→ 确认枚举值范围，数据类型
3. **预定义数据**（本 Skill）→ 基于表结构填充业务数据
