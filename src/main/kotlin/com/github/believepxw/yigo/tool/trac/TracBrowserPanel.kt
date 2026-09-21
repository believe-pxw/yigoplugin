package com.github.believepxw.yigo.tool.trac

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.awt.BorderLayout
import java.awt.GridLayout
import java.net.Proxy
import javax.swing.*

class TracBrowserPanel(private val project: Project) {
    companion object {
        // Cache cookies across multiple fetches to avoid slow logins every time
        private var cachedCookies: MutableMap<String, String> = mutableMapOf()

        fun loadCookies(globalState: TracGlobalSettingsState) {
            if (globalState.serializedCookies.isNotBlank()) {
                val parts = globalState.serializedCookies.split(";")
                for (p in parts) {
                    val kv = p.trim().split("=", limit = 2)
                    if (kv.size == 2) {
                        cachedCookies[kv[0].trim()] = kv[1].trim()
                    }
                }
            }
        }

        fun saveCookies(globalState: TracGlobalSettingsState) {
            globalState.serializedCookies = cachedCookies.entries.joinToString(";") { "${it.key}=${it.value}" }
        }
    }

    private val mainPanel: JPanel = JPanel(BorderLayout())

    init {
        val projectState = TracSettingsState.getInstance(project)
        val globalState = TracGlobalSettingsState.getInstance()
        loadCookies(globalState)

        val inputPanel = JPanel(GridLayout(7, 2, 5, 5))
        inputPanel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        val ticketIDField = JTextField()
        val usernameField = JTextField(globalState.tracUsername)
        val passwordField = JPasswordField(globalState.tracPassword)
        val mainClassField = JTextField(projectState.defaultMainClass)
        val envVarsField = JTextField(projectState.defaultEnvVars)

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
            globalState.tracUsername = usernameField.text
            globalState.tracPassword = String(passwordField.password)
            projectState.defaultMainClass = mainClassField.text
            projectState.defaultEnvVars = envVarsField.text
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
            
            val username = globalState.tracUsername
            val password = globalState.tracPassword
            
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
                    
                    if (cachedCookies.isNotEmpty()) {
                        try {
                            val candidateDoc = Jsoup.connect(url)
                                .header("Authorization", "Basic $encodedAuth")
                                .userAgent(userAgent)
                                .cookies(cachedCookies)
                                .proxy(Proxy.NO_PROXY)
                                .timeout(20000)
                                .get()
                            
                            // Check if page actually contains the ticket and is authenticated
                            if (candidateDoc.selectFirst(".trac-id") != null) {
                                doc = candidateDoc
                            } else {
                                // Missing .trac-id means session expired or redirected to login/forbidden
                                cachedCookies.clear()
                            }
                        } catch (e: org.jsoup.HttpStatusException) {
                            if (e.statusCode == 401 || e.statusCode == 403) {
                                // Authentication/authorization failure -> invalidate cache
                                cachedCookies.clear()
                            } else {
                                // 404 Not Found, 500, etc. -> do NOT clear cookies, rethrow!
                                throw e
                            }
                        } catch (e: Exception) {
                            // Network timeout or connection error -> do NOT clear cookies, rethrow!
                            throw e
                        }
                    }
                    
                    if (doc == null) {
                        // 1. Visit Login page with Basic Auth.
                        // Important: followRedirects(false) so we reliably capture the 302 redirect's Set-Cookie (trac_auth)
                        val loginPageResponse = Jsoup.connect(loginUrl)
                            .method(Connection.Method.GET)
                            .header("Authorization", "Basic $encodedAuth")
                            .userAgent(userAgent)
                            .followRedirects(false)
                            .proxy(Proxy.NO_PROXY)
                            .timeout(20000)
                            .execute()
                        
                        val newCookies = mutableMapOf<String, String>()
                        newCookies.putAll(loginPageResponse.cookies())
                        
                        // 2. If Form Auth plugin is present (HTTP 200 with an actual user input field)
                        if (loginPageResponse.statusCode() == 200) {
                            val loginDoc = loginPageResponse.parse()
                            val formToken = loginDoc.selectFirst("input[name=__FORM_TOKEN]")?.attr("value") ?: ""
                            val hasUserField = loginDoc.selectFirst("input[name=user]") != null
                            
                            if (formToken.isNotEmpty() && hasUserField) {
                                val loginPostResponse = Jsoup.connect(loginUrl)
                                    .method(Connection.Method.POST)
                                    .header("Authorization", "Basic $encodedAuth")
                                    .userAgent(userAgent)
                                    .cookies(newCookies)
                                    .proxy(Proxy.NO_PROXY)
                                    .data("__FORM_TOKEN", formToken)
                                    .data("user", username)
                                    .data("password", password)
                                    .data("referer", url)
                                    .followRedirects(false)
                                    .timeout(20000)
                                    .execute()
                                
                                newCookies.putAll(loginPostResponse.cookies())
                            }
                        }
                        
                        if (newCookies.isNotEmpty()) {
                            cachedCookies.putAll(newCookies)
                            saveCookies(globalState)
                        }
                        
                        // 3. Fetch the actual ticket page using fresh authenticated cookies
                        doc = Jsoup.connect(url)
                            .header("Authorization", "Basic $encodedAuth")
                            .userAgent(userAgent)
                            .cookies(cachedCookies)
                            .proxy(Proxy.NO_PROXY)
                            .timeout(20000)
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
