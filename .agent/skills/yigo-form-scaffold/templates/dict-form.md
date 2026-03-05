# 模板：字典表单

典型结构：`FormType="Dict"` + SplitPanel（上 GridLayoutPanel 表头 + 下 Grid 明细） + TabPanel 系统信息页签。

```xml
<Form Key="{字典Key}" Caption="{字典名称}" FormType="Dict" Version="1">
    <DataSource>
        <DataObject Key="{字典Key}" Caption="{名称}" PrimaryTableKey="{主表Key}" SecondaryType="Dict" PrimaryType="Entity">
            <!-- 由 yigo-dataobject-generator 生成 -->
        </DataObject>
    </DataSource>
    <OperationCollection>
        <!-- 字典表单标准操作集 -->
        <Operation Key="DicNew" Caption="新增" RefKey="DicNew" TCode="GetEntryTCode()" Activity="01"/>
        <Operation Key="DicCopyNew" Caption="复制新增" RefKey="DicCopyNew" TCode="GetEntryTCode()" Activity="01"/>
        <Operation Key="DicModify" Caption="修改" RefKey="DicModify" TCode="GetEntryTCode()" Activity="02"/>
        <Operation Key="DicSave" Caption="保存" RefKey="DicSave"/>
        <Operation Key="DicCancel" Caption="取消" RefKey="DicCancel"/>
        <Operation Key="DicEnabled" Caption="启用" RefKey="DicEnabled" Activity="Y02"/>
        <Operation Key="DicDisabled" Caption="停用" RefKey="DicDisabled" Activity="Y01"/>
        <Operation Key="DicInvalid" Caption="作废" RefKey="DicInvalid" Activity="Y03"/>
        <Operation Key="DicDelete" Caption="删除" RefKey="DicDelete" Activity="06"/>
        <Operation Key="DicRefresh" Caption="刷新" RefKey="DicRefresh"/>
        <Operation Key="Lang" Caption="多语言" RefKey="Lang" Activity="Y10"/>
        <Operation Key="ShowDataLog" Caption="查看数据日志" RefKey="ShowDataLog" Activity="Y08"/>
        <Operation Key="DicExit" Caption="关闭" RefKey="DicExit"/>
    </OperationCollection>
    <Body>
        <Block>
            <FlexFlowLayoutPanel Key="root">
                <TabPanel Key="body" Height="100%">
                    <SplitPanel Key="BasicInformation" Caption="基本信息" Orientation="Vertical">
                        <!-- 表头：GridLayoutPanel（X/Y 精确定位） -->
                        <GridLayoutPanel Key="body_basic" Padding="8px" OverflowY="Auto" TopPadding="24px">
                            <!-- 由 yigo-control-generator 生成控件 -->
                            <RowDefCollection RowGap="24">
                                <RowDef Height="32px"/>
                                <!-- 按需添加行 -->
                            </RowDefCollection>
                            <ColumnDefCollection ColumnGap="16">
                                <ColumnDef Width="25%"/>
                                <ColumnDef Width="25%"/>
                                <ColumnDef Width="25%"/>
                                <ColumnDef Width="25%"/>
                                <!-- 按需添加列，一般是三列或四列 -->
                            </ColumnDefCollection>
                        </GridLayoutPanel>
                        <!-- 明细 Grid -->
                        <Grid Key="{DtlGridKey}" Caption="{明细名称}" SerialSeq="true" Padding="8px">
                            <!-- 由 yigo-grid-generator 生成 -->
                        </Grid>
                        <SplitSize Size="425px"/><!-- 根据行高和行数 生成 -->
                        <SplitSize Size="100%"/>
                    </SplitPanel>
                    <!-- 系统信息页签 -->
                    <FlexFlowLayoutPanel Key="BodySystem" Caption="系统信息" Padding="8px">
                        <Embed Key="BodySystemEmbed" FormKey="BodySystemDictForm" RootKey="SystemInfoPanel">
                            <Var Key="SystemInfoTableKey" Value="{主表Key}"/>
                        </Embed>
                    </FlexFlowLayoutPanel>
                </TabPanel>
            </FlexFlowLayoutPanel>
        </Block>
    </Body>
    <MacroCollection>
        <!-- 由 yigo-operation-script 生成 -->
    </MacroCollection>
</Form>
```
