package com.github.believepxw.yigo.tool.trac

import java.util.regex.Pattern

object TracDataParser {
    
    fun parseEnvVars(data: TracTicketData): Map<String, String> {
        val envs = mutableMapOf<String, String>()
        val rawText: String = data.rawText ?: ""
        // Match "set KEY=VALUE"
        val setPattern = Pattern.compile("^set\\s+([A-Z0-9_]+)=(.*)$", Pattern.MULTILINE)
        val matcher = setPattern.matcher(rawText)
        while (matcher.find()) {
            val key = matcher.group(1).trim()
            var value = matcher.group(2).trim()
            if (value.endsWith(" + 变更号")) {
                value = value.replace(" + 变更号", data.ticketId)
            }
            envs[key] = value
        }
        
        // Match "系统日期:YYYY-MM-DD HH:mm:ss"
        val datePattern = Pattern.compile("系统日期[:：]\\s*(\\d{4}-\\d{2}-\\d{2})\\s+(\\d{2}:\\d{2}:\\d{2})")
        val dateMatcher = datePattern.matcher(rawText)
        while (dateMatcher.find()) {
            val date = dateMatcher.group(1)
            val time = dateMatcher.group(2)
            envs["ERP_FIXED_TIME"] = "${date}T${time}"
        }
        
        return envs
    }
}
