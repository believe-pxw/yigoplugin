# 模板：单界面报表（条件 + 结果表格）

典型结构：SplitPanel（上 GridLayoutPanel 条件区 + 下 Grid 结果区），条件控件带 `Condition` 子元素。

```xml
<Form Key="{ReportKey}" Caption="{报表名称}" FormType="Entity">
    <DataSource>
        <DataObject Key="{ReportKey}" Caption="{名称}" PrimaryType="Entity">
            <TableCollection>
                <!-- 结果表：SourceType="Query", Persist="false" -->
                <Table Key="{ResultTableKey}" Caption="{结果}" TableMode="Detail" SourceType="Query" Persist="false">
                    <Statement Type="Formula"><![CDATA['SELECT ... FROM ... ' + Macro_GetWhere()]]></Statement>
                </Table>
                <!-- 条件表：非持久化 -->
                <Table Key="{CondTableKey}" Caption="{条件}" Persist="false">
                    <!-- 条件列（Persist="false", IgnoreQuery="true"） -->
                </Table>
            </TableCollection>
        </DataObject>
    </DataSource>
    <OperationCollection>
        <Operation Key="Query" Caption="查询" RefKey="Query"/>
        <OperationCollection Key="NewPrint" Caption="打印" SelfDisable="true">
            <Operation Key="NewPrintDefault" Caption="默认模板打印" RefKey="NewPrintDefault"/>
            <Operation Key="NewPrintSelect" Caption="其他模板选择" RefKey="NewPrintSelect"/>
            <Operation Key="ManagePrint" Caption="打印模板管理" RefKey="ManagePrint"/>
        </OperationCollection>
        <OperationCollection Key="NewPrePrint" Caption="打印预览" SelfDisable="true">
            <Operation Key="NewPrePrintDefault" Caption="默认模板预览" RefKey="NewPrePrintDefault"/>
            <Operation Key="NewPrePrintSelect" Caption="其他模板选择" RefKey="NewPrePrintSelect"/>
        </OperationCollection>
        <Operation Key="ERPExportExcel" Caption="导出" RefKey="ERPExportExcel"/>
        <Operation Key="UIClose" Caption="关闭" RefKey="UIClose"/>
    </OperationCollection>
    <Body PopHeight="700px" PopWidth="635px">
        <Block>
            <FlexFlowLayoutPanel Key="root">
                <ToolBar Key="ToolBar1" Caption="ToolBar1" Height="pref">
                    <ToolBarItemCollection/>
                </ToolBar>
                <SplitPanel Key="main_container" Orientation="Vertical" Height="100%">
                    <!-- 条件区：GridLayoutPanel + 控件带 Condition -->
                    <GridLayoutPanel Key="{CondPanelKey}" Caption="查询条件" Padding="8px" OverflowY="Auto" TopPadding="24px">
                        <!-- 条件控件示例 -->
                        <!--
                        <Dict Key="Cond_Field" Caption="字段" ItemKey="...">
                            <DataBinding TableKey="{CondTableKey}" ColumnKey="Cond_Field"/>
                            <Condition ColumnKey="Field" TableKey="{ResultTableKey}" CondSign="=" UseAdvancedQuery="true"/>
                        </Dict>
                        -->
                        <RowDefCollection RowGap="24">
                            <RowDef Height="32px"/>
                        </RowDefCollection>
                        <ColumnDefCollection ColumnGap="16">
                            <ColumnDef Width="33%"/>
                            <ColumnDef Width="33%"/>
                            <ColumnDef Width="34%"/>
                        </ColumnDefCollection>
                    </GridLayoutPanel>
                    <!-- 结果区：只读 Grid -->
                    <Grid Key="{ResultGridKey}" Caption="查询结果" Enable="false" SerialSeq="true" DisabledOption="delete" Padding="8px" PageLoadType="DB">
                        <!-- 由 yigo-grid-generator 生成 -->
                    </Grid>
                    <SplitSize Size="125px"/>
                    <SplitSize Size="100%"/>
                </SplitPanel>
            </FlexFlowLayoutPanel>
        </Block>
    </Body>
    <OnLoad><![CDATA[Macro_LoadObject()]]></OnLoad>
</Form>
```
