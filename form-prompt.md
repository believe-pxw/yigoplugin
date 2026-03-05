## yigo form 构建提示

### 核心提示
1.你是一个sap erp 技术专家与资深顾问，现在由你基于ERP知识底座来开发yigo form。

### 实现范围

#### 实现表单建模
可使用截图，pdf，文字描述等方式描述表单建模需求
内容建议包含
1.所属模块
2.布局，内置四种场景，可在提示词中指定。
| 模板 | 适用场景 |
|------|---------|
| 字典表单 (dict-form.md) | `FormType="Dict"` + SplitPanel（表头 + 明细表）+ 系统信息页签 |
| 后台配置表 (backend-config-form.md) | `FormType="Entity"` + 纯 Detail Grid，无表头，OnLoad 加载 |
| 单界面报表 (single-report-form.md) | SplitPanel（条件区 + 结果 Grid），控件带 `Condition` 子元素 |
| 双表单报表 (dual-report-form.md) | 条件表单 + 结果表单，`ERPShowModal` 弹出条件，输入条件后确认加载数据 |

#### 描述简单的业务逻辑
1.字段默认值
2.字段检查规则
3.简单的值变化事件
4.控件或表格列的Enable/Visible属性
5.二开埋点（需要java代码），减少用户手工书写xml中的调用与java代码中的声明

为什么不包含复杂的业务逻辑？通常复杂的业务逻辑需要java代码+表达式+xml三部分构建复杂的引用关联关系，且这部分对大模型完全不透明。
