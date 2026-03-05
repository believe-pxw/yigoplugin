# 模板：双表单报表（条件表单 + 结果表单）

由**条件选择表单**和**结果展示表单**两个 Form 组成：
- 结果表单（parent）在 `OnLoad` 中通过 `ERPShowModal` 弹出条件表单
- 结果表单有"重新选择"操作按钮，再次弹出条件表单
- 条件表单通过 `DealCondition(true);parent.Macro_LoadObject()` 将条件传递给结果表单

## 结果表单

```xml
<Form Key="{ResultFormKey}" FormType="Entity" FormulaCaption="{动态标题公式}" InitState="Default">
    <DataSource>
        <DataObject Key="{ResultDOKey}" Caption="{名称}" PrimaryType="Entity">
            <TableCollection>
                <Table Key="{ResultTableKey}" TableMode="Detail" SourceType="Query" Persist="false">
                    <Statement Type="Formula"><![CDATA['SELECT ... FROM ...']]></Statement>
                </Table>
            </TableCollection>
        </DataObject>
    </DataSource>
    <OperationCollection>
        <!-- 重新选择：再次弹出条件表单 -->
        <Operation Key="Filter" Caption="重新选择" Enable="true">
            <Action><![CDATA[ERPShowModal('{CondFormKey}')]]></Action>
        </Operation>
        <Operation Key="Refresh" Caption="刷新" RefKey="Refresh"/>
        <OperationCollection Key="NewPrint" Caption="打印" SelfDisable="true">
            <Operation Key="NewPrintDefault" Caption="默认模板打印" RefKey="NewPrintDefault" Activity="04"/>
            <Operation Key="NewPrintSelect" Caption="其他模板选择" RefKey="NewPrintSelect" Activity="04"/>
        </OperationCollection>
        <Operation Key="ERPExportExcel" Caption="导出" RefKey="ERPExportExcel" Activity="Y11"/>
        <Operation Key="UIClose" Caption="关闭" RefKey="UIClose"/>
    </OperationCollection>
    <Body>
        <Block>
            <FlexFlowLayoutPanel Key="root">
                <ToolBar Key="ToolBar1" Caption="ToolBar1" Height="pref">
                    <ToolBarItemCollection/>
                </ToolBar>
                <GridLayoutPanel Key="main_container" Height="100%" OverflowY="Auto">
                    <Grid Key="{ResultGridKey}" PageLoadType="DB" SerialSeq="true" X="0" Y="0" Padding="8px">
                        <!-- 由 yigo-grid-generator 生成 -->
                    </Grid>
                    <RowDefCollection RowGap="24">
                        <RowDef Height="100%"/>
                    </RowDefCollection>
                    <ColumnDefCollection ColumnGap="16">
                        <ColumnDef Width="100%"/>
                    </ColumnDefCollection>
                </GridLayoutPanel>
            </FlexFlowLayoutPanel>
        </Block>
    </Body>
    <!-- OnLoad 弹出条件选择界面 -->
    <OnLoad><![CDATA[ERPShowModal('{CondFormKey}')]]></OnLoad>
</Form>
```

## 条件表单

```xml
<Form Key="{CondFormKey}" Caption="{条件界面名称}">
    <DataSource>
        <DataObject Key="{CondFormKey}" Caption="{名称}" PrimaryType="Entity">
            <TableCollection>
                <!-- 非持久化条件表 -->
                <Table Key="{CondTableKey}" Persist="false">
                    <!-- 条件字段：Persist="false", IgnoreQuery="true" -->
                </Table>
            </TableCollection>
        </DataObject>
    </DataSource>
    <!-- 条件表单无 OperationCollection，按钮在 Body 内 -->
    <Body PopHeight="800px" PopWidth="600px">
        <Block>
            <FlexFlowLayoutPanel Key="root">
                <!-- 条件区域 -->
                <GridLayoutPanel Key="main_container" Height="100%" Padding="8px" OverflowY="Auto">
                    <!-- 分组标题 -->
                    <Label Key="HeadLabel" Caption="选择条件" X="0" Y="0" Class="erp-group-title" XSpan="2">
                        <DataBinding/>
                    </Label>
                    <!-- 条件控件 + Condition 子元素 -->
                    <!--
                    <Dict Key="FieldID" Caption="字段" X="0" Y="1" ItemKey="..." OneTimeCompute="true">
                        <DataBinding TableKey="{CondTableKey}" ColumnKey="FieldID"/>
                        <Condition ColumnKey="FieldID" TableKey="{ResultTableKey}" CondSign="=" UseAdvancedQuery="true"/>
                    </Dict>
                    -->
                    <RowDefCollection RowGap="24">
                        <RowDef Height="32px"/>
                    </RowDefCollection>
                    <ColumnDefCollection ColumnGap="16">
                        <ColumnDef Width="50%"/>
                        <ColumnDef Width="50%"/>
                    </ColumnDefCollection>
                </GridLayoutPanel>
                <!-- 按钮区域：确定/取消/查询变式 -->
                <GridLayoutPanel Key="ButtonPanel" Height="100px" Padding="8px" TopPadding="24px">
                    <Button Key="OK" Caption="确定" X="3" Y="1" Type="Primary">
                        <OnClick><![CDATA[UICheck();
DealCondition(true);
parent.LoadData();
Close('OK');]]></OnClick>
                    </Button>
                    <Button Key="Cancel" Caption="取消" X="2" Y="1" Type="Normal">
                        <OnClick><![CDATA[Close();]]></OnClick>
                    </Button>
                    <Embed Key="UserFavorite" Caption="查询变式" FormKey="V_Favorite_Impl" RootKey="Favorite_ImplFavoriteGridLayoutPanel" IncludeDataTable="false" X="0" Y="0" YSpan="2"/>
                    <RowDefCollection>
                        <RowDef Height="32px"/>
                        <RowDef Height="32px"/>
                    </RowDefCollection>
                    <ColumnDefCollection ColumnGap="16">
                        <ColumnDef Width="340px"/>
                        <ColumnDef Width="100%"/>
                        <ColumnDef Width="80px"/>
                        <ColumnDef Width="80px"/>
                    </ColumnDefCollection>
                </GridLayoutPanel>
            </FlexFlowLayoutPanel>
        </Block>
    </Body>
</Form>
```
