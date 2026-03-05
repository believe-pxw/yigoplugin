---
name: yigo-java-customization
description: 编写与解读 YIGO ERP 二次开发 Java 代码，基于 EntityContextAction 模式，使用 BillEntity/TableEntity 操作表单数据，SqlString 执行数据库查询
---

# YIGO Java 二次开发（二开）

## 概述

YIGO ERP 的二次开发（二开）是在 XML 表单配置的基础上，通过 Java 代码扩展业务逻辑。二开类在 XML 表达式中通过 `com.包路径.类名.方法名(参数)` 的方式调用。

## 参考文件

- 架构文档：[yigo-erp-architecture.md](file:///d:/Workbench/idea/yigo-ai-assistance-research/yigo-erp-architecture.md)
- 开发流程：[yigo-erp-workflow.md](file:///d:/Workbench/idea/yigo-ai-assistance-research/yigo-erp-workflow.md)

## 核心类继承关系

```
EntityContextAction          ← 二开类继承此基类
  ├── _context               → RichDocumentContext（上下文）
  │    ├── getRichDocument()  → RichDocument（当前表单数据）
  │    ├── getResultSet(sql)  → DataTable（执行 SQL 查询）
  │    └── getEnv()           → 环境信息（语言、用户等）
  ├── getDocument()           → RichDocument（快捷方法）
  └── getResultSet(sql)       → DataTable（快捷方法）

AbstractBillEntity           ← BillEntity 自动生成类继承此基类
  ├── parseEntity(_context)  → 从上下文解析表单实体
  ├── load(_context, oid)    → 从数据库加载实体
  ├── get字段名()             → 获取字段值
  ├── set字段名(value)        → 设置字段值
  └── 明细表名s()             → 获取明细表实体列表

AbstractTableEntity          ← TableEntity 自动生成类继承此基类
  ├── get字段名()             → 获取列值
  ├── set字段名(value)        → 设置列值
  └── valueByFieldKey(key, value) → 按 FieldKey 操作
```

## 二开类编写规范

### 基本结构

```java
package com.bokesoft.erp.模块.function;

import com.bokesoft.erp.entity.util.EntityContextAction;
import com.bokesoft.yes.mid.cmd.richdocument.strut.RichDocumentContext;

public class XxxFormula extends EntityContextAction {

    public XxxFormula(RichDocumentContext _context) {
        super(_context);
    }

    // 二开方法
    public 返回值 方法名(参数...) throws Throwable {
        // 业务逻辑
    }
}
```

### 关键要求

1. **必须继承** `EntityContextAction`
2. **构造方法**必须接受 `RichDocumentContext` 参数并调用 `super(_context)`
3. 方法声明 `throws Throwable`
4. 在 XML 表达式中通过 `com.bokesoft.erp.模块.function.类名.方法名(参数)` 调用

## BillEntity（表单实体）

BillEntity 是由 YigoCAD 工具自动生成的 Java 类，封装了整个 Form 表单的数据操作。

### 主要能力

| 方法 | 说明 |
|------|------|
| `parseEntity(_context)` | 从当前上下文解析表单实体 |
| `parseDocument(doc)` | 从 RichDocument 解析表单实体 |
| `load(_context, oid)` | 从数据库按 OID 加载实体 |
| `get字段名()` | 获取表头字段值（类型安全） |
| `set字段名(value)` | 设置表头字段值（支持链式调用） |
| `明细表名s()` | 获取明细表实体列表 |
| `明细表名(oid)` | 按主键获取单条明细 |
| `明细表名s(filterKey, filterValue)` | 按字段过滤明细 |
| `new明细表名()` | 新增明细行 |
| `delete明细表名(dtl)` | 删除明细行 |

### 使用示例

```java
// 从上下文解析表单
PM_Strategy strategies = PM_Strategy.parseEntity(_context);

// 读取表头字段
Long unitID = strategies.getStrategyUnitID();
String code = strategies.getCode();

// 设置字段值（链式调用）
strategies.setStrategyUnitID(100L).setCallHorizon(30);

// 遍历明细表
for (EPM_StrategyDtl dtl : strategies.epm_strategyDtls()) {
    Long packageUnitID = dtl.getPackageUnitID();
    int cycleLength = dtl.getCycleLength();
}

// 新增明细行
EPM_StrategyDtl newDtl = strategies.newEPM_StrategyDtl();
newDtl.setPackageNo(1).setCycleNotes("新周期");

// 从数据库加载另一个表单
PM_Strategy other = PM_Strategy.load(_context, targetOID);
```

### 字段常量

BillEntity 中为每个控件（Field）生成了 `public static final String` 常量，格式为：
- 表头字段：`字段Key`（如 `PM_Strategy.SchedulingIndicator`）
- 明细字段：`Dtl_字段Key`（如 `PM_Strategy.Dtl_CycleLength`）
- 操作：`Opt_操作Key`（如 `PM_Strategy.Opt_DicNew`）

## TableEntity（表实体）

TableEntity 封装了 DataObject 中某张表的数据，使用 ColumnKey 访问。

### 与 BillEntity 的区别

| | BillEntity | TableEntity |
|---|-----------|-------------|
| 数据范围 | 整个表单 | 单张表 |
| 访问方式 | FieldKey | ColumnKey |
| get/set | `value_String(Key)` → `getString(Key)` | `value_String(ColumnKey)` |
| 加载 | `parseEntity(_context)` | 从 BillEntity 获取 |

### 使用示例

```java
// 获取 TableEntity
EPM_Strategy tableEntity = strategies.epm_strategy();

// 按 ColumnKey 访问
String code = tableEntity.getCode();
Long clientID = tableEntity.getClientID();

// 动态字段访问
Object value = tableEntity.valueByFieldKey("DuePack1");
tableEntity.valueByFieldKey("DuePack1", "新值");
```

## Loader（数据加载器）

BillEntity/TableEntity 提供 `loader(_context)` 链式加载器：

```java
// 链式条件查询
List<BK_Unit> unitList = BK_Unit.loader(_context)
    .UnitSystemID(systemID)
    .loadList();

// 单条查询
BK_Unit unit = BK_Unit.loader(_context)
    .Code("d")
    .loadFirst();

// 按 OID 加载
BK_Unit unit = BK_Unit.load(_context, unitID);
```

## SqlString（SQL 查询）

当需要执行自定义 SQL 时，使用 `SqlString` 防注入拼接：

```java
SqlString sql = new SqlString()
    .append("select * from ", EPP_Routing_MaintenancePack.EPP_Routing_MaintenancePack,
            " where ", EPP_Routing_MaintenancePack.IsRelation, "=")
    .appendPara(1)
    .append(" and PackageShortText = ")
    .appendPara(billDtlID);

DataTable rst = getResultSet(sql);

// 遍历结果
if (rst != null && rst.size() > 0) {
    for (int rowIndex = 0; rowIndex < rst.size(); rowIndex++) {
        Long soid = rst.getLong(rowIndex, "SOID");
        String name = rst.getString(rowIndex, "Name");
    }
}
```

### SqlString 方法

| 方法 | 说明 |
|------|------|
| `append(str...)` | 拼接 SQL 片段 |
| `appendPara(value)` | 拼接参数化值（防注入） |
| `SqlStringUtil.genMultiParameters(str)` | 生成 IN 子句参数 |

## MessageFacade（消息提示）

```java
// 获取消息文本
String msg = MessageFacade.getMsgContent(MessageConstant.MSG_CODE);

// 抛出异常消息（中断操作）
MessageFacade.throwException(MessageConstant.MSG_CODE, param1, param2);
```

## RichDocumentContext 上下文

```java
// 获取当前表单文档
RichDocument doc = _context.getRichDocument();
// 或使用 EntityContextAction 的快捷方法
RichDocument doc = getDocument();

// 执行 SQL
DataTable rst = _context.getResultSet(sqlString);
// 或快捷方法
DataTable rst = getResultSet(sql);

// 获取 Paras 自定义参数
Object para = _context.getParas().get("key");
_context.getParas().put("key", value);

// 获取环境信息
String locale = _context.getEnv().getLocale();
```

## RichDocument（表单数据）

```java
RichDocument doc = getDocument();

// 读写表头字段
Object value = doc.getHeadFieldValue("FieldKey");
doc.setHeadFieldValue("FieldKey", newValue);

// 获取明细表数据
DataTable dtlTable = doc.getDataTable("TableKey");

// 新增明细行
int rowIndex = doc.appendDetail("TableKey");

// 删除明细行
doc.deleteDetail("TableKey", oid);
```

## 在 XML 中调用二开方法

```xml
<!-- 在控件属性中调用（返回布尔值控制 Enable） -->
<Dict Enable="IIF(ToBool(com.bokesoft.erp.pm.function.StrategiesFormula.isExistStrategiesPackage()),false,!ReadOnly())"/>

<!-- 在 CheckRule 中调用（返回错误消息或空串） -->
<CheckRule><![CDATA[
var msg = com.bokesoft.erp.pm.function.StrategiesFormula.checkCycleLength(SchedulingIndicator, PackageUnitID, CycleLength, IsCycleSet);
IIF(msg!='', msg, true)
]]></CheckRule>

<!-- 在 ItemFilter 中调用（返回 SqlString） -->
<FilterValue Index="1" ParaValue="IIF(SchedulingIndicator&gt;=0, ' soid in ('&amp;com.bokesoft.erp.pm.function.StrategiesFormula.getStrategyUnitIDbyIndicator(SchedulingIndicator)&amp;' )', '1=2')"/>
```

## 与其他 Skill 的配合

| 配合 Skill | 关系 |
|------------|------|
| `yigo-expression-writer` | 表达式语法参考，二开方法在表达式中通过完整类路径调用 |
| `yigo-dataobject-generator` | DataObject 的 Table/Column 定义决定了 BillEntity/TableEntity 的字段 |
| `yigo-operation-script` | 操作的 Action 中调用二开方法 |
| `yigo-control-generator` | 控件的 CheckRule/ValueChanged/Enable 中调用二开方法 |
