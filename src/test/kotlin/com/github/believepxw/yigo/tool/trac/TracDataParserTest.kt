package com.github.believepxw.yigo.tool.trac

import org.junit.Assert.assertEquals
import org.junit.Test

class TracDataParserTest {

    @Test
    fun testParseEnvVars() {
        val rawText = """
            用户信息:{test=BokeERP708, administrator=BokeERP708}
            当前登录用户为:test,密码为:BokeERP708
            
            系统日期:2023-06-06 02:02:27
            打开:设备维护;维护工单;查询与修改
            
            REM 数据库类型
            set DB_TYPE=MYSQLS
            REM 数据库服务器的IP和端口
            set DB_SERVER=1.1.9.46:3308
            REM 数据库名称
            set DB_NAME=MYSQLS
            REM 数据库用户名
            set DB_USER=root
            REM 数据库用户密码
            set DB_PASS=123456
            REM 启用分库
            set MULTI_DB=true
            REM 分库分表表名参数
            set MULTI_DB_DSNFILTER=CC:0001,0002,0003_Year:2013,2014,2023
            REM 分库数据库名前缀
            set MULTI_DB_PREFIX=ticket_ + 变更号
            set MASTER=false
        """.trimIndent()

        val envs = TracDataParser.parseEnvVars(TracTicketData(rawText = rawText, ticketId = "12345", title = "测试工单"))

        assertEquals("MYSQLS", envs["DB_TYPE"])
        assertEquals("1.1.9.46:3308", envs["DB_SERVER"])
        assertEquals("root", envs["DB_USER"])
        assertEquals("123456", envs["DB_PASS"])
        assertEquals("true", envs["MULTI_DB"])
        assertEquals("CC:0001,0002,0003_Year:2013,2014,2023", envs["MULTI_DB_DSNFILTER"])
        assertEquals("ticket_12345", envs["MULTI_DB_PREFIX"])
        assertEquals("false", envs["MASTER"])
        assertEquals("2023-06-06T02:02:27", envs["ERP_FIXED_TIME"])
    }
}
