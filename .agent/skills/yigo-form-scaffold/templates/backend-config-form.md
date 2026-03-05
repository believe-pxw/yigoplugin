# 模板：后台配置单明细表

典型结构：`FormType="Entity"` + 只有 Detail Grid（无主表表头），ToolBar + GridLayoutPanel 包裹 Grid，OnLoad 加载数据。

```xml
<Form Key="{ConfigKey}" Caption="{配置名称}" FormType="Entity" InitState="Default" DeliveryClass="C">
    <DataSource>
        <DataObject Key="{ConfigKey}" Caption="{名称}" PrimaryType="Entity">
            <TableCollection>
                <!-- 只有一张 Detail 表，无主表 -->
                <Table Key="{DetailTableKey}" Caption="{明细名称}" TableMode="Detail">
                    <!-- 由 yigo-dataobject-generator 生成 Column -->
                </Table>
            </TableCollection>
        </DataObject>
    </DataSource>
    <OperationCollection>
        <Operation Key="BillEdit" Caption="修改" RefKey="BillEdit" TCode="'{TCode}'" Activity="02"/>
        <Operation Key="BillSave" Caption="保存" RefKey="BillSave"/>
        <Operation Key="BillCancel" Caption="取消" RefKey="BillCancel"/>
        <Operation Key="ShowDataLog" Caption="查看数据日志" RefKey="ShowDataLog"/>
        <Operation Key="PositionCursor" Caption="定位" RefKey="PositionCursor"/>
        <Operation Key="UIClose" Caption="关闭" RefKey="UIClose"/>
    </OperationCollection>
    <Body>
        <Block>
            <FlexFlowLayoutPanel Key="root">
                <ToolBar Key="ToolBar1" Caption="ToolBar1" Height="pref">
                    <ToolBarItemCollection/>
                </ToolBar>
                <GridLayoutPanel Key="main_container" Height="100%" OverflowY="Auto">
                    <Grid Key="{GridKey}" Caption="{名称}" DefaultFitWidth="true" SerialSeq="true" X="0" Y="0" Padding="8px">
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
    <OnLoad><![CDATA[Macro_LoadObject()]]></OnLoad>
</Form>
```
