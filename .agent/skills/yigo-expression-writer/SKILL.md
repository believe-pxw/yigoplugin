---
name: yigo-expression-writer
description: 书写与解读 YIGO 表达式（公式），覆盖 IIF/if-else/var/Macro/Java 静态调用/parent-container 上下文/XML 实体转义/ConfirmMsg 等语法
---

# YIGO 表达式书写

## 概述

YIGO 表达式（Formula）是嵌入在 Form XML 中的脚本语言，用于控制界面行为、计算字段值、校验数据等。表达式出现在 `Action`、`OnLoad`、`OnClick`、`CheckRule`、`Macro`、`DefaultValue`（公式类）、`Visible`/`Enable`（公式类）、`ValueChanged` 等位置。

> **核心规则**：在 XML 中，所有表达式体必须用 `<![CDATA[...]]>` 包裹。

## BNF 语法参考

完整 BNF 定义：[peixw.bnf](../peixw.bnf)

## XML 实体转义 ⚠️

在 XML **属性值**中（如 `Visible`、`Enable`），不能使用 CDATA，因此必须对特殊字符进行

### IIF 三元表达式

```
IIF(条件, 真值, 假值)
```

嵌套 IIF：

```
IIF(条件1, 值1, IIF(条件2, 值2, 值3))
```

IIFS（多条件分支）：

```
IIFS(条件1, 值1, 条件2, 值2, ...)
```


### Return

```
return 表达式;
```

## 函数调用

### 内置函数

直接调用：`FunctionName(参数1, 参数2, ...)`

常见内置函数：

| 函数 | 说明 |
|------|------|
| `ReadOnly()` | 当前是否只读状态 |
| `Length(value)` | 转整数 |
| `SetPara(paraKey, paraValue)` | 设置参数 |
| `GetPara(paraKey)` | 获取参数 |
| `MessageFacade(code, text)` | 弹出消息 |

### Macro 宏调用

在 Form XML 的 `MacroCollection` 中定义的宏：

```
Macro_宏名称(参数1, 参数2)
```

- 宏名以 `Macro_` 开头
- 在 `<MacroCollection>` 中定义，Key 为 `宏名称`（不含 `Macro_` 前缀）
- Form 中同名宏会覆盖 `CommonDef.xml` 中的同名宏

### Java 静态方法调用

通过完整类路径调用 Java 二开方法：

```
com.bokesoft.erp.pm.function.StrategiesFormula.isExistStrategiesPackage()
com.bokesoft.erp.pm.function.StrategiesFormula.checkCycleLength(SchedulingIndicator, PackageUnitID, CycleLength, IsCycleSet)
```

- 使用完整包路径 `com.xxx.yyy.ClassName.methodName(...)`
- Java 方法必须继承 `EntityContextAction`（参考 `yigo-java-customization` skill）
- 参数可以是字段标识、常量或表达式

### parent / container 上下文

```
parent.方法名()       // 调用父表单的方法
container.方法名()    // 调用容器的方法
Parent.方法名()       // 大小写不敏感
```

典型用途：条件表单调用父表单加载数据

```
DealCondition(true);parent.Macro_LoadObject()
```

## ConfirmMsg 确认对话框

```
ConfirmMsg(消息代码, 消息文本, 消息参数, 样式, 回调对象)
```

| 参数 | 必须 | 说明 |
|------|------|------|
| 消息代码 | ✅ | 消息 Key |
| 消息文本 | ✅ | 显示文本 |
| 消息参数 | ❌ | `{{参数1},{参数2}}` 格式 |
| 样式 | ❌ | `OK`、`YES_NO`、`YES_NO_CANCEL` |
| 回调对象 | ❌ | `{yes: {执行语句}, no: {执行语句}}` |

### 示例

```
ConfirmMsg('MSG001', '确定要删除吗？', {}, 'YES_NO', {yes: {Delete()}, no: {}})
```

## 多语句书写

多条语句用 `;` 分隔：

```xml
<Action><![CDATA[
var x = GetFieldValue('Status');
if (x == 0) {
    SetFieldValue('Status', 1);
    Save();
};
Refresh()
]]></Action>
```

## 常见表达式模式

### 1. 条件可见/可用（属性中）

```xml
<TextEditor Visible="!ToInt(IsCycleSet)" Enable="!ReadOnly()&amp;&amp;Enable&lt;0"/>
```

### 2. 校验规则（CDATA 中）

```xml
<CheckRule><![CDATA[
IIFS(
    com.bokesoft.erp.documentNumber.DocumentNumber.docNumberFieldEnable('Code')&&Code=='',
    '采用外部给号编码规则，请输入代码',
    true,
    true
)
]]></CheckRule>
```

### 3. 默认值公式

```xml
<DataBinding DefaultFormulaValue="Macro_MultiLangText('EPM_Strategy','Name')"/>
```

### 4. 调用 Java 方法做字典过滤

```xml
<ItemFilter ItemKey="Unit">
    <Filter Key="Filter" Impl="com.bokesoft.yes.erp.condition.Filter" Type="Custom">
        <FilterValue Index="1" ParaValue="IIF(SchedulingIndicator&gt;=0, ' soid in ('&amp;com.bokesoft.erp.pm.function.StrategiesFormula.getStrategyUnitIDbyIndicator(SchedulingIndicator)&amp;' )', '1=2')"/>
    </Filter>
</ItemFilter>
```

## 与其他 Skill 的配合

| 配合 Skill | 使用位置 |
|------------|----------|
| `yigo-operation-script` | `Action`、`ExceptionHandler`、`Macro` 的内容 |
| `yigo-control-generator` | 控件的 `Visible`、`Enable`、`CheckRule`、`ValueChanged` 等 |
| `yigo-form-scaffold` | `OnLoad`、`OnClose`、`OnPostShow` 事件 |
| `yigo-java-customization` | Java 静态方法调用的实现端 |
