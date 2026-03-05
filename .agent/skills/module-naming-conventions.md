# YIGO ERP 模块命名规范

> 本文件定义了各业务模块的 FormKey、Table Key 命名前缀规则。
> 所有生成 DataObject、Form、预定义数据的 Skill 在创建新对象时**必须遵循**本规范。

## 命名规则总览

**通用规则**：
- **FormKey / DataObject Key** = `{模块前缀}_{业务名称}`，如 `MM_PurchaseOrder`
- **Table Key** = `E{模块前缀}_{业务名称}`，如 `EMM_PurchaseOrderHead`
- Table Key 以 `E` 开头（Entity 的缩写），后接模块前缀

## 模块命名映射表

| Project Key | Caption | 模块前缀 | FormKey 示例 | Table 前缀 | Table 示例 |
|---|---|---|---|---|---|
| `basisconfig` | BASIS | GS | `GS_UserParam` | `EGS_` | `EGS_UserParamHead` |
| `BK_Basic` | 基本资料 | BK | `FavoriteVariant` | `BK_` | `BK_FavoriteVariant` |
| `mmconfig` | MM | MM | `MM_PurchaseOrder` | `EMM_` | `EMM_PurchaseOrderHead` |
| `sdconfig` | SD | SD | `SD_SaleOrder` | `ESD_` | `ESD_SaleOrderHead` |
| `pmconfig` | PM | PM | `PM_MaintenanceOrder` | `EPM_` | `EPM_MaintenanceOrderHead` |
| `ppconfig` | PP | PP | `PP_ProductionOrder` | `EPP_` | `EPP_ProductionOrder` |
| `ficonfig` | FI | FI | `FI_Voucher` | `EFI_` | `EFI_VoucherHead` |
| `coconfig` | CO | CO | `CO_ProductionOrder` | `ECO_` | `ECO_ProductionOrder` |
| `hrconfig` | HR | HR | `HR_Organization` | `EHR_` | `EHR_Object` |
| `qmconfig` | QM | QM | `QM_InspectionLot` | `EQM_` | `EQM_InspectionLot` |
| `psconfig` | PS | PS | `PS_Project` | `EPS_` | `EPS_Project` |
| `wmsconfig` | WMS | WM | `WM_ReceiptOrder` | `EWM_` | `EWM_ReceiptOrderHead` |
| `tmconfig` | TM | TM | `TM_Shipment` | `ETM_` | `ETM_ShipmentHead` |
| `fmconfig` | 基金管理 | FM | `FM_FundVoucher` | `EFM_` | `EFM_FundVoucherHead` |
| `copaconfig` | COPA | COPA | `COPA_ProfitSegment` | `ECOPA_` | `ECOPA_ProfitSegment` |
| `tcmconfig` | 资金管理 | TCM | `TCM_CollectionOrder` | `ETCM_` | `ETCM_CollectionOrderHead` |
| `authorityConfig` | 权限 | AU | `AuthorityFieldValue` | `EAU_` | `EAU_StructureParameterFileHead` |

### FI 模块子模块

FI 模块内包含若干子模块，使用独立前缀：

| 子模块 | 业务域 | FormKey 前缀 | Table 前缀 | 示例 |
|---|---|---|---|---|
| AM | 资产管理 | `AM_` | `EAM_` | `EAM_AssetCard` |
| BM | 票据管理 | `BM_` | `EBM_` | `EBM_CommercialDraftHead` |
| ECS | 费用管理 | `ECS_` | `EECS_` | `EECS_ExpenseRequisitionHead` |

## 跨模块公共对象

以下对象不属于特定业务模块，使用 `EGS_` 前缀，可出现在任意模块的预定义数据中：

| 对象 | Table 前缀 | 说明 |
|---|---|---|
| `TCode` | `EGS_TCode` | 事务码 |
| `Message` | `EGS_Message` | 系统消息 |
| `MessageClass` | `EGS_MessageClass` | 消息类 |
| `EntryTCodeRelation` | `EAU_EntryTCodeRelation` | 菜单事务码关系 |
| `TCodeAuthorityObjectFieldDefaultValue` | `EAU_TCodeAuthObjectDefRel` | 事务码权限对象默认值 |
| `TCodeAuthorityObjectFieldValue` | `EAU_TCodeAuthorityObjectRelDtl` | 事务码权限对象字段值 |
| `AuthorityObject` | `EAU_AuthorityObject` | 权限对象 |

## 使用指南

### 确定新对象前缀

1. 根据业务需求确定目标模块（如采购 → MM）
2. 查表获取 FormKey 前缀（`MM_`）和 Table 前缀（`EMM_`）
3. FormKey = `{前缀}{业务名}` → `MM_PurchaseOrder`
4. 主表 Key = `{Table前缀}{业务名}Head` → `EMM_PurchaseOrderHead`（单据类）
5. 主表 Key = `{Table前缀}{业务名}` → `EMM_MoveType`（字典类，不加 Head）
6. 明细表 Key = `{Table前缀}{业务名}Dtl` → `EMM_PurchaseOrderDtl`

### 主表命名后缀

| 类型 | 主表是否加 Head | 示例 |
|---|---|---|
| 单据表单 | 加 `Head` | `EMM_PurchaseOrderHead` |
| 字典表单 | 不加 | `EPM_OrderType` |
| 迁移表 | 不加 | `EMM_MaterialStorage` |
