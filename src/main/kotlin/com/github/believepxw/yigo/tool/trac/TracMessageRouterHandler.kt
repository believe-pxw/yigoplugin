package com.github.believepxw.yigo.tool.trac

data class TracTicketData(
    val ticketId: String,
    val title: String,
    var envVars: Map<String, String>? = null,
    val rawText: String? = null
)
