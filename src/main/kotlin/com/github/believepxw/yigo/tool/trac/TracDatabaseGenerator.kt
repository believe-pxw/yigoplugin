package com.github.believepxw.yigo.tool.trac

import com.intellij.credentialStore.OneTimeString
import com.intellij.database.access.DatabaseCredentials
import com.intellij.database.dataSource.LocalDataSource
import com.intellij.database.dataSource.LocalDataSource.Storage
import com.intellij.database.dataSource.LocalDataSourceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

object TracDatabaseGenerator {
    fun generate(project: Project, data: TracTicketData) {
        val envVars = data.envVars
        if (envVars == null || envVars.isEmpty()) {
            javax.swing.SwingUtilities.invokeLater {
                Messages.showErrorDialog("No environment variables found in ticket. Cannot create Database Source.", "Database Error")
            }
            return
        }

        // DB properties to look for: DB_TYPE, DB_SERVER, DB_NAME, DB_USER, DB_PASS, DB_PORT
        val dbType = envVars["DB_TYPE"] ?: ""
        val dbServer = envVars["DB_SERVER"]?.split(":")?.first() ?: "127.0.0.1"
        val envDBPort = envVars["DB_SERVER"]?.split(":")?.last()
        var dbName = envVars["DB_NAME"] ?: "postgres"
        if (dbType.equals("PostgreSQL", ignoreCase = true) && envVars["MULTI_DB"] == "true") {
            // If postgres multi-db, usually connect to 'postgres' or leave standard
            dbName = "postgres"
        }
        val dbUser = envVars["DB_USER"] ?: "postgres"
        val dbPass = envVars["DB_PASS"] ?: "postgres"

        if (dbType.isBlank()) {
            javax.swing.SwingUtilities.invokeLater {
                Messages.showErrorDialog("DB_TYPE is missing in ticket variables.", "Database Error")
            }
            return
        }

        var driverName = ""
        var jdbcUrl = ""

        if (dbType.equals("PostgreSQL", ignoreCase = true)) {
            val dbPort = envDBPort ?: "5432"
            driverName = "PostgreSQL"
            jdbcUrl = "jdbc:postgresql://$dbServer:$dbPort/$dbName"
        } else if (dbType.equals("MySQL", ignoreCase = true)) {
            val dbPort = envDBPort ?: "3306"
            driverName = "MySQL"
            jdbcUrl = "jdbc:mysql://$dbServer:$dbPort/$dbName"
        } else if (dbType.equals("Oracle", ignoreCase = true)) {
            val dbPort = envDBPort ?: "1521"
            driverName = "Oracle"
            jdbcUrl = "jdbc:oracle:thin:@//$dbServer:$dbPort/$dbName" // Assuming modern service name format
        }else if (dbType.equals("dm", ignoreCase = true)) {
            val dbPort = envDBPort ?: "1521"
            driverName = "DmDriver"
            jdbcUrl = "jdbc:dm://$dbServer:$dbPort" // Assuming modern service name format
        } else if (dbType.equals("SQLServer", ignoreCase = true) || dbType.equals("SQL Server", ignoreCase = true)) {
            val dbPort = envDBPort ?: "1433"
            driverName = "SQL Server"
            jdbcUrl = "jdbc:sqlserver://$dbServer:$dbPort;databaseName=$dbName"
        } else {
            javax.swing.SwingUtilities.invokeLater {
                Messages.showErrorDialog("Unsupported or unknown DB_TYPE: $dbType", "Database Error")
            }
            return
        }

        val titleSafe = data.title.replace("[^a-zA-Z0-9.\\u4e00-\\u9fa5 -_]".toRegex(), "")
        val dsName = "Ticket ${data.ticketId}: $titleSafe"

        val dsManager = LocalDataSourceManager.getInstance(project)
        
        // Check if already exists
        val exists = dsManager.dataSources.any { it.name == dsName }
        if (exists) {
            javax.swing.SwingUtilities.invokeLater {
                Messages.showInfoMessage("Database Source already exists: $dsName", "Info")
            }
            return
        }

        val dataSource = LocalDataSource()
        dataSource.name = dsName
        dataSource.url = jdbcUrl
        dataSource.username = dbUser

        // Resolve Driver
        val driverManager = com.intellij.database.dataSource.DatabaseDriverManager.getInstance()
        var driver = driverManager.getDriver(driverName.lowercase())
        if (driver == null|| !driver.hasDriverFiles()) {
            // Fallback search by name if explicit ID didn't match
            driver = driverManager.drivers.find { it.name.contains(driverName, ignoreCase = true) && it.hasDriverFiles() }
        }
        if (driver != null) {
            dataSource.databaseDriver = driver
        }else{
            javax.swing.SwingUtilities.invokeLater {
                Messages.showErrorDialog("Driver not found or missing driver files for: $driverName", "Database Error")
            }
            return
        }

        // In LocalDataSource, password storage is delegated to DatabaseCredentials.
        DatabaseCredentials.getInstance().storePassword(dataSource, OneTimeString(dbPass))

        dataSource.passwordStorage = Storage.PERSIST

        dsManager.addDataSource(dataSource)

        javax.swing.SwingUtilities.invokeLater {
            Messages.showInfoMessage("Successfully created Database Source: $dsName", "Success")
        }
    }
}
