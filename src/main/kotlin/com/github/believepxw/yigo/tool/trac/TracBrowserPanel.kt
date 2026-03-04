package com.github.believepxw.yigo.tool.trac

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.awt.BorderLayout
import java.awt.GridLayout
import javax.swing.*

class TracBrowserPanel(private val project: Project) {
    companion object {
        // Cache cookies across multiple fetches to avoid slow logins every time
        private var cachedCookies: MutableMap<String, String> = mutableMapOf()

        fun loadCookies(state: TracSettingsState) {
            if (state.serializedCookies.isNotBlank()) {
                val parts = state.serializedCookies.split(";")
                for (p in parts) {
                    val kv = p.split("=", limit = 2)
                    if (kv.size == 2) {
                        cachedCookies[kv[0]] = kv[1]
                    }
                }
            }
        }

        fun saveCookies(state: TracSettingsState) {
            state.serializedCookies = cachedCookies.entries.joinToString(";") { "${it.key}=${it.value}" }
        }
    }

    private val mainPanel: JPanel = JPanel(BorderLayout())

    init {
        val state = TracSettingsState.getInstance(project)
        loadCookies(state)

        // Async pre-warm: Validate and keep the session alive by touching the base login endpoint
        Thread {
            try {
                if (cachedCookies.isNotEmpty()) {
                    val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                    Jsoup.connect("http://dev.bokesoft.com:8000/trac/eri-erp/login")
                        .userAgent(userAgent)
                        .cookies(cachedCookies)
                        .execute()
                }
            } catch (ignore: Exception) {}
        }.start()

        val inputPanel = JPanel(GridLayout(7, 2, 5, 5))
        inputPanel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        val ticketIDField = JTextField()
        val usernameField = JTextField(state.tracUsername)
        val passwordField = JPasswordField(state.tracPassword)
        val mainClassField = JTextField(state.defaultMainClass)
        val envVarsField = JTextField(state.defaultEnvVars)

        val checkClipboard = {
            try {
                val contents = com.intellij.openapi.ide.CopyPasteManager.getInstance().contents
                if (contents != null && contents.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.stringFlavor)) {
                    val text = contents.getTransferData(java.awt.datatransfer.DataFlavor.stringFlavor) as? String
                    if (!text.isNullOrBlank()) {
                        val trimmed = text.trim()
                        val ticketId = if (trimmed.startsWith("http")) {
                            trimmed.substringAfterLast("/")
                        } else if (trimmed.startsWith("#")) {
                            trimmed.substring(1)
                        } else {
                            trimmed
                        }
                        
                        if (ticketIDField.text != ticketId) {
                            ticketIDField.text = ticketId
                        }
                    }
                }
            } catch (ignore: Exception) {}
        }

        mainPanel.addAncestorListener(object : javax.swing.event.AncestorListener {
            override fun ancestorAdded(event: javax.swing.event.AncestorEvent?) = checkClipboard()
            override fun ancestorRemoved(event: javax.swing.event.AncestorEvent?) {}
            override fun ancestorMoved(event: javax.swing.event.AncestorEvent?) {}
        })

        project.messageBus.connect(project).subscribe(ToolWindowManagerListener.TOPIC, object : ToolWindowManagerListener {
            override fun stateChanged(toolWindowManager: com.intellij.openapi.wm.ToolWindowManager) {
                val toolWindow = toolWindowManager.getToolWindow("Trac")
                if (toolWindow != null && toolWindow.isActive) {
                    checkClipboard()
                }
            }
        })

        val saveAction = {
            state.tracUsername = usernameField.text
            state.tracPassword = String(passwordField.password)
            state.defaultMainClass = mainClassField.text
            state.defaultEnvVars = envVarsField.text
        }

        usernameField.document.addDocumentListener(object: javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = saveAction()
            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = saveAction()
            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = saveAction()
        })
        passwordField.document.addDocumentListener(object: javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = saveAction()
            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = saveAction()
            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = saveAction()
        })
        mainClassField.document.addDocumentListener(object: javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = saveAction()
            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = saveAction()
            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = saveAction()
        })
        envVarsField.document.addDocumentListener(object: javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = saveAction()
            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = saveAction()
            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = saveAction()
        })

        inputPanel.add(JLabel("Ticket ID:"))
        inputPanel.add(ticketIDField)
        inputPanel.add(JLabel("Trac Username:"))
        inputPanel.add(usernameField)
        inputPanel.add(JLabel("Trac Password:"))
        inputPanel.add(passwordField)
        inputPanel.add(JLabel("Main Class:"))
        inputPanel.add(mainClassField)
        inputPanel.add(JLabel("Extra Env Vars (k=v;...):"))
        inputPanel.add(envVarsField)

        val fetchButton = JButton("Generate Service Config")
        val dbButton = JButton("Generate Database Source")


        val executeExtraction = { isGenerateDb: Boolean ->
            saveAction()
            var url = ticketIDField.text.trim()
            if (url.isEmpty()) {
                Messages.showErrorDialog("Please enter a valid Trac Ticket", "Error")
            }
            // support plain ticket numbers like "164384"
            url = "http://dev.bokesoft.com:8000/trac/eri-erp/ticket/$url"
            
            val username = state.tracUsername
            val password = state.tracPassword
            
            if (username.isEmpty() || password.isEmpty()) {
                Messages.showErrorDialog("Username and Password are required for Basic Auth.", "Error")
            }

            // run in background
            Thread {
                try {
                    val basePath = url.substringBefore("/ticket/")
                    val loginUrl = "$basePath/login"
                    
                    val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                    
                    val authString = "$username:$password"
                    val encodedAuth = java.util.Base64.getEncoder().encodeToString(authString.toByteArray(Charsets.UTF_8))
                    
                    // Attempt to use cached cookies if available
                    var doc: org.jsoup.nodes.Document? = null
                    var useCache = false
                    
                    if (cachedCookies.isNotEmpty()) {
                        try {
                            doc = Jsoup.connect(url)
                                .header("Authorization", "Basic $encodedAuth")
                                .userAgent(userAgent)
                                .cookies(cachedCookies)
                                .get()
                            useCache = true
                        } catch (e: Exception) {
                            // Cache expired or invalid, clear it and fall through to login
                            cachedCookies.clear()
                            useCache = false
                        }
                    }
                    
                    if (!useCache) {
                        // 1. Visit Login page to get __FORM_TOKEN and init cookies
                        val loginPageResponse = Jsoup.connect(loginUrl)
                            .method(Connection.Method.GET)
                            .header("Authorization", "Basic $encodedAuth")
                            .userAgent(userAgent)
                            .execute()
                        
                        var cookies = loginPageResponse.cookies()
                        val loginDoc = loginPageResponse.parse()
                        
                        // Extract __FORM_TOKEN
                        val formTokenInput = loginDoc.selectFirst("input[name=__FORM_TOKEN]")
                        val formToken = formTokenInput?.attr("value") ?: ""
                        
                        // 2. Post Login credentials (if Form Auth plugin is used)
                        if (formToken.isNotEmpty()) {
                            val loginPostResponse = Jsoup.connect(loginUrl)
                                .method(Connection.Method.POST)
                                .header("Authorization", "Basic $encodedAuth")
                                .userAgent(userAgent)
                                .cookies(cookies)
                                .data("__FORM_TOKEN", formToken)
                                .data("user", username)
                                .data("password", password)
                                .data("referer", url) // Redirect back to ticket
                                .followRedirects(false) // We want to capture the auth cookies manually
                                .execute()
                                
                            // Update cookies with auth cookies (trac_auth, etc.)
                            cookies.putAll(loginPostResponse.cookies())
                        }
                        
                        cachedCookies.putAll(cookies)
                        saveCookies(state)
                        
                        // 3. Fetch the actual ticket page using authenticated cookies
                        doc = Jsoup.connect(url)
                            .header("Authorization", "Basic $encodedAuth")
                            .userAgent(userAgent)
                            .cookies(cachedCookies)
                            .get()
                    }
                        
                    val ticketIdField = doc!!.selectFirst(".trac-id")
                    val ticketId = ticketIdField?.text()?.replace("#", "") ?: ""
                    
                    val titleField = doc.selectFirst(".summary")
                    val title = titleField?.text() ?: ""
                    
                    // If login failed, the page might not have the expected contents
                    if (ticketId.isEmpty()) {
                         throw Exception("Login failed or ticket not found. Missing .trac-id element. Ensure Username and Password are correct.")
                    }
                    
                    val pres = doc.select("pre.wiki")
                    val rawText = java.lang.StringBuilder()
                    for (pre in pres) {
                        rawText.append(pre.text()).append("\n")
                    }
                    
                    val data = TracTicketData(
                        ticketId = ticketId,
                        title = title,
                        rawText = rawText.toString()
                    )
                    
                    val parsedEnvs = TracDataParser.parseEnvVars(data)
                    val finalData = data.copy(envVars = parsedEnvs)
                    
                    if (isGenerateDb) {
                        TracDatabaseGenerator.generate(project, finalData)
                    } else {
                        TracRunConfigGenerator.generate(project, finalData)
                    }
                    
                } catch (e: Exception) {
                    e.printStackTrace()
                    SwingUtilities.invokeLater {
                        Messages.showErrorDialog("Failed to fetch or parse ticket: ${e.message}", "Error")
                    }
                }
            }.start()
        }

        fetchButton.addActionListener { executeExtraction(false) }
        dbButton.addActionListener { executeExtraction(true) }
        
        inputPanel.add(fetchButton)
        inputPanel.add(dbButton)

        mainPanel.add(inputPanel, BorderLayout.NORTH)
        mainPanel.add(JPanel(), BorderLayout.CENTER) // Empty filler
    }

    fun getContent(): JComponent = mainPanel
}
