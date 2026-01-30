package com.github.believepxw.yigo.tool

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.JBUI
import example.index.FormIndex
import java.awt.*
import java.awt.datatransfer.StringSelection
import java.awt.dnd.DnDConstants
import java.awt.dnd.DragSource
import java.awt.event.*
import javax.swing.*
import javax.swing.border.Border
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class YigoLayoutPanel(private val project: Project, private val toolWindow: ToolWindow) : JPanel(BorderLayout()) {

    private val rootPanel = object : JPanel(), Scrollable {
        // Implement Scrollable with smart width tracking: 
        // Expand to viewport if minimum content fits (true), but allow scroll if even minimum is wider (false).
        override fun getScrollableTracksViewportWidth(): Boolean {
            val viewport = parent as? javax.swing.JViewport ?: return true
            return minimumSize.width <= viewport.width
        }
        
        // Let height grow as needed
        override fun getScrollableTracksViewportHeight(): Boolean = false
        
        // Standard Scrollable implementation
        override fun getPreferredScrollableViewportSize(): Dimension = preferredSize
        override fun getScrollableUnitIncrement(visibleRect: Rectangle?, orientation: Int, direction: Int): Int = 20
        override fun getScrollableBlockIncrement(visibleRect: Rectangle?, orientation: Int, direction: Int): Int = 60
    }
    private val scrollPane = JBScrollPane(rootPanel)
    
    // Search Components
    private lateinit var searchPanel: JPanel
    private val searchField = SearchTextField()
    private val prevButton = JButton("Prev")
    private val nextButton = JButton("Next")
    private val countLabel = JLabel("0/0")

    // State
    private val tagToComponent = java.util.WeakHashMap<XmlTag, JComponent>()
    private var lastSelectedComponent: JComponent? = null // From Editor Caret
    private var lastSearchHighlight: JComponent? = null // From Search
    
    private var searchMatches = listOf<Pair<XmlTag, JComponent>>()
    private var currentMatchIndex = -1
    
    // Constant for client property key
    private val KEY_ORIGINAL_BORDER = "YigoOriginalBorder"
    private val KEY_XML_TAG = "YigoXmlTag"
    
    // Embed Loading Queue
    private val embedLoadQueue = java.util.ArrayDeque<() -> Unit>()
    private var activeEmbedLoads = 0
    private val MAX_CONCURRENT_EMBED_LOADS = 5
    
    // Flag to prevent recursive navigation updates
    private var isProgrammaticSwitch = false
    private var isNavigatingToXml = false

    init {
        setupSearchPanel()

        rootPanel.layout = BoxLayout(rootPanel, BoxLayout.Y_AXIS)
        rootPanel.border = JBUI.Borders.empty(10)
        
        add(scrollPane, BorderLayout.CENTER)
        
        // Register Search Shortcut (Ctrl+F / Cmd+F)
        val searchAction = object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                searchPanel.isVisible = true
                searchField.requestFocusInWindow()
                searchField.textEditor.selectAll()
            }
        }
        val keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx)
        this.registerKeyboardAction(searchAction, keyStroke, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        rootPanel.registerKeyboardAction(searchAction, keyStroke, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)

        updateUIFromXml()

        // 1. Real-time Focus Sync (CaretListener)
        EditorFactory.getInstance().eventMulticaster.addCaretListener(object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) {
                if (event.editor.project == project) {
                     // Wrap in ReadAction as highlightComponentAtCaret accesses PSI
                     ApplicationManager.getApplication().runReadAction {
                         highlightComponentAtCaret(event.editor)
                     }
                }
            }
        }, toolWindow.contentManager)

        // 2. File Selection Listener (Refresh UI)
        project.messageBus.connect(toolWindow.contentManager).subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    if (event.newFile == null || event.oldFile == event.newFile) return 
                    updateUIFromXml(preserveScroll = false) 
                }
            }
        )

        // 3. PSI Change Listener (Refresh UI)
        PsiManager.getInstance(project).addPsiTreeChangeListener(object : com.intellij.psi.PsiTreeChangeAdapter() {
            override fun childrenChanged(event: com.intellij.psi.PsiTreeChangeEvent) { processEvent(event) }
            override fun childAdded(event: com.intellij.psi.PsiTreeChangeEvent) { processEvent(event) }
            override fun childRemoved(event: com.intellij.psi.PsiTreeChangeEvent) { processEvent(event) }
            override fun childMoved(event: com.intellij.psi.PsiTreeChangeEvent) { processEvent(event) }
            
            fun processEvent(event: com.intellij.psi.PsiTreeChangeEvent) {
                 val element = event.parent ?: event.child ?: return
                 val tag = PsiTreeUtil.getParentOfType(element, XmlTag::class.java, false)
                 
                 if (tag != null) {
                     val gridTag = findParentGridTag(tag)
                     if (gridTag != null && tagToComponent.containsKey(gridTag)) {
                         javax.swing.SwingUtilities.invokeLater {
                             if (project.isDisposed) return@invokeLater
                             ApplicationManager.getApplication().runReadAction {
                                 refreshGridComponent(gridTag)
                             }
                         }
                         return
                     }
                 }
                 
                 javax.swing.SwingUtilities.invokeLater {
                    if (project.isDisposed) return@invokeLater
                    ApplicationManager.getApplication().runReadAction {
                        updateUIFromXml(preserveScroll = true)
                    }
                 }
            }
        }, toolWindow.contentManager)
    }

    private fun setupSearchPanel() {
        searchPanel = JPanel(BorderLayout())
        searchPanel.border = JBUI.Borders.empty(5)
        searchPanel.isVisible = true  
        
        searchField.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) { runSearch() }
            override fun removeUpdate(e: DocumentEvent?) { runSearch() }
            override fun changedUpdate(e: DocumentEvent?) { runSearch() }
        })
        
        searchField.addKeyboardListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER) {
                    if (e.isShiftDown) moveSelection(-1) else moveSelection(1)
                } else if (e.keyCode == KeyEvent.VK_ESCAPE) {
                    searchPanel.isVisible = false
                    clearSearch()
                    rootPanel.requestFocusInWindow()
                }
            }
        })
        
        val controls = JPanel(FlowLayout(FlowLayout.RIGHT, 5, 0))
        prevButton.margin = Insets(0, 5, 0, 5)
        nextButton.margin = Insets(0, 5, 0, 5)
        
        prevButton.addActionListener { moveSelection(-1) }
        nextButton.addActionListener { moveSelection(1) }
        
        controls.add(countLabel)
        controls.add(prevButton)
        controls.add(nextButton)
        
        searchPanel.add(searchField, BorderLayout.CENTER)
        searchPanel.add(controls, BorderLayout.EAST)
        
        add(searchPanel, BorderLayout.NORTH)
    }
    
    // --- Helper Methods to Register Component and Store Border ---
    private fun registerComponent(tag: XmlTag, component: JComponent) {
        tagToComponent[tag] = component
        saveOriginalBorder(component)
        attachNavigationListener(component, tag)
    }

    private fun saveOriginalBorder(component: JComponent) {
        val border = component.border
        if (border != null) {
            component.putClientProperty(KEY_ORIGINAL_BORDER, border)
        }
    }
    
    private fun restoreBorder(component: JComponent) {
        val original = component.getClientProperty(KEY_ORIGINAL_BORDER) as? Border
        component.border = original ?: BorderFactory.createEmptyBorder()
    }
    
    private fun setHighlightBorder(component: JComponent, color: Color, thickness: Int) {
        val original = component.getClientProperty(KEY_ORIGINAL_BORDER) as? Border
        val highlight = BorderFactory.createLineBorder(color, thickness)
        
        // Wrap original border instead of replacing it
        if (original != null) {
            component.border = BorderFactory.createCompoundBorder(highlight, original)
        } else {
            component.border = highlight
        }
    }
    // -------------------------------------------------------------
    
    private fun requestEmbedLoad(task: () -> Unit) {
        embedLoadQueue.add(task)
        processEmbedQueue()
    }
    
    private fun processEmbedQueue() {
        if (activeEmbedLoads >= MAX_CONCURRENT_EMBED_LOADS || embedLoadQueue.isEmpty()) return
        
        val task = embedLoadQueue.poll() ?: return
        activeEmbedLoads++
        
        try {
            task()
        } catch (e: Exception) {
            activeEmbedLoads--
            processEmbedQueue()
        }
    }
    
    private fun onEmbedLoadFinished() {
        activeEmbedLoads--
        processEmbedQueue()
    }

    private fun runSearch() {
        val text = searchField.text.trim().lowercase()
        if (text.isEmpty()) {
            clearSearch()
            return
        }
        
        ApplicationManager.getApplication().runReadAction {
            val validComponents = tagToComponent.entries.filter { it.key.isValid }
            searchMatches = validComponents.filter { (tag, _) ->
                val key = tag.getAttributeValue("Key")?.lowercase() ?: ""
                val cap = tag.getAttributeValue("Caption")?.lowercase() ?: ""
                key.contains(text) || cap.contains(text)
            }.map { it.toPair() }.sortedBy { it.second.y }
        }
        
        currentMatchIndex = if (searchMatches.isNotEmpty()) 0 else -1
        updateSearchUI()
    }
    
    private fun clearSearch() {
        searchMatches = emptyList()
        currentMatchIndex = -1
        updateSearchUI()
    }
    
    private fun moveSelection(direction: Int) {
        if (searchMatches.isEmpty()) return
        currentMatchIndex = (currentMatchIndex + direction).mod(searchMatches.size)
        updateSearchUI()
    }
    
    private fun updateSearchUI() {
        if (searchMatches.isEmpty()) {
            countLabel.text = "0/0"
            countLabel.foreground = Color.RED
            prevButton.isEnabled = false
            nextButton.isEnabled = false
            if (lastSearchHighlight != null) {
                restoreBorder(lastSearchHighlight!!)
                lastSearchHighlight = null
            }
        } else {
            countLabel.text = "${currentMatchIndex + 1}/${searchMatches.size}"
            countLabel.foreground = Color.BLACK
            prevButton.isEnabled = true
            nextButton.isEnabled = true
            
            if (lastSearchHighlight != null && lastSearchHighlight != searchMatches[currentMatchIndex].second) {
                 restoreBorder(lastSearchHighlight!!)
            }
            
            val (tag, comp) = searchMatches[currentMatchIndex]
            setHighlightBorder(comp, Color.MAGENTA, 3)
            ensureComponentVisible(comp)
            lastSearchHighlight = comp
            
            // Sync to XML
            val currentEditor = FileEditorManager.getInstance(project).selectedTextEditor
            val currentPsi = if (currentEditor != null) PsiDocumentManager.getInstance(project).getPsiFile(currentEditor.document) else null
            
            if (currentPsi != null && tag.containingFile == currentPsi) {
                navigateToTag(tag, false)
            }
        }
    }

    private fun findParentGridTag(startTag: XmlTag): XmlTag? {
        var current: XmlTag? = startTag
        while (current != null) {
            if (current.name == "Grid") return current
            if (current.name == "Form" || current.name == "Body") return null 
            current = current.parentTag
        }
        return null
    }

    private fun refreshGridComponent(gridTag: XmlTag) {
        val oldComp = tagToComponent[gridTag] as? JPanel ?: return
        oldComp.removeAll()
        populateWrappedGridTable(gridTag, oldComp)
        oldComp.revalidate()
        oldComp.repaint()
        
        // Re-attach listener and save border?
        // Wait, populateWrappedGridTable doesn't set main border, createGridPanel does.
        // But populate adds children. 
        // We need to re-attach listener to the PARENT grid panel? No, createGridPanel does that.
        // refreshGridComponent is called on the container.
        
        attachNavigationListener(oldComp, gridTag)
        // Original border is on oldComp, stored in client property, so it persists?
        // Yes, ClientProperties persist unless cleared.
        // But if we change structure, we might need to restore it?
        // No, we are just changing children.
    }

    private fun updateUIFromXml(preserveScroll: Boolean = false) {
        val vScroll = scrollPane.verticalScrollBar.value
        val hScroll = scrollPane.horizontalScrollBar.value

        rootPanel.removeAll()
        tagToComponent.clear()
        // renderStack.clear() // Removed
        embedLoadQueue.clear() // Clear pending queue on refresh
        // activeEmbedLoads is not reset to 0 because pending tasks might still come back. 
        // But we are rebuilding UI, so we don't care about old results?
        // Ideally we should cancel them, but ReadAction cancellation is via promise. 
        // For now, allow them to finish and just update dead components? 
        // Actually, finishOnUiThread updating 'embedPanel' which is removed from rootPanel. 
        // So it's fine.
        
        clearSearch() 
        
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        if (editor == null) {
            rootPanel.add(JLabel("No active editor"))
            rootPanel.revalidate()
            rootPanel.repaint()
            return
        }

        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
        if (psiFile !is XmlFile) {
            rootPanel.add(JLabel("Not an XML file"))
            rootPanel.revalidate()
            rootPanel.repaint()
            return
        }

        val rootTag = psiFile.rootTag
        if (rootTag == null || rootTag.name != "Form") {
             rootPanel.add(JLabel("Not a Yigo Form"))
             rootPanel.revalidate()
             rootPanel.repaint()
             return
        }
        
        val key = rootTag.getAttributeValue("Key") ?: ""
        val initialVisited = if (key.isNotEmpty()) setOf(key) else emptySet()

        val bodyTag = rootTag.findFirstSubTag("Body")
        if (bodyTag != null) {
            renderTag(bodyTag, rootPanel, initialVisited)
        } else {
             rootPanel.add(JLabel("No Body tag found"))
        }

        rootPanel.revalidate()
        rootPanel.repaint()
        
        javax.swing.SwingUtilities.invokeLater {
            if (project.isDisposed) return@invokeLater
            ApplicationManager.getApplication().runReadAction {
                if (preserveScroll) {
                     scrollPane.verticalScrollBar.value = vScroll
                     scrollPane.horizontalScrollBar.value = hScroll
                } else {
                     highlightComponentAtCaret(editor)
                }
                
                if (searchPanel.isVisible && searchField.text.isNotBlank()) {
                    runSearch()
                }
            }
        }
    }

    private fun highlightComponentAtCaret(editor: Editor) {
        val offset = editor.caretModel.offset
        // 1. Read PSI to find tag (Must be in ReadAction)
        var targetTag: XmlTag? = null
        
        // Check if we are already in a read action? 
        // If called from updateUIFromXml, we are. If called from Listener, maybe not.
        // We will assume caller handles ReadAction for finding element, OR we wrap it.
        // BUT, highlightComponentAtCaret is called from:
        // 1. CaretListener (calls runReadAction explicitly)
        // 2. updateUIFromXml (calls runReadAction explicitly)
        
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return
        val element = psiFile.findElementAt(offset)
        var tag = PsiTreeUtil.getParentOfType(element, XmlTag::class.java, false)
        
        while (tag != null && !tagToComponent.containsKey(tag)) {
            tag = tag.parentTag
            if (tag?.name == "Form" || tag?.name == "Body") break 
        }
        targetTag = tag
        
        if (targetTag != null) {
            // 2. Update UI (Must be on EDT, OUTSIDE ReadAction ideally, but ReadAction on EDT is allowed)
            // However, modifying Swing components inside a ReadAction is fine as long as we are on EDT.
            // The crash happened because of re-entrant layout.
            // Let's defer strict UI updates to invokeLater to break the layout cycle if strictly needed.
            
            // Capture state immediately! invokeLater runs when flag is reset.
            val shouldSuppressScroll = isNavigatingToXml
            
            ApplicationManager.getApplication().invokeLater {
                val component = tagToComponent[targetTag]
                if (component != null && component != lastSelectedComponent) {
                    if (lastSelectedComponent != null && lastSelectedComponent != lastSearchHighlight) {
                        restoreBorder(lastSelectedComponent!!)
                    }
                    
                    if (component != lastSearchHighlight) {
                         setHighlightBorder(component, Color.BLUE, 2)
                    }
                    
                    if (!shouldSuppressScroll) {
                        ensureComponentVisible(component)
                    }
                    lastSelectedComponent = component
                }
            }
        }
    }
    
        private fun ensureComponentVisible(component: JComponent) {
        // 1. Walk up hierarchy to find JTabbedPane and switch tabs
        var current: Container? = component
        while (current != null) {
            val parent = current.parent
            if (parent is JTabbedPane) {
                val index = parent.indexOfComponent(current)
                if (index != -1 && parent.selectedIndex != index) {
                    isProgrammaticSwitch = true
                    try {
                        parent.selectedIndex = index
                    } finally {
                        isProgrammaticSwitch = false
                    }
                }
            }
            current = parent
        }
        
        // 2. Scroll to visible
        component.scrollRectToVisible(component.bounds)
    }

    private fun navigateToTag(tag: XmlTag, requestFocus: Boolean = true) {
        if (project.isDisposed) return
        
        // Read PSI to get offset and file info
        var offset = 0
        var vFile: com.intellij.openapi.vfs.VirtualFile? = null
        var psiFile: com.intellij.psi.PsiFile? = null
        
        ApplicationManager.getApplication().runReadAction {
            if (tag.isValid) {
                offset = tag.textOffset
                psiFile = tag.containingFile
                vFile = psiFile?.virtualFile
            }
        }
        
        if (vFile == null || psiFile == null) return
        
        val fileEditorHelper = FileEditorManager.getInstance(project)
        val currentEditor = fileEditorHelper.selectedTextEditor
        
        if (currentEditor == null || currentEditor.document != PsiDocumentManager.getInstance(project).getDocument(psiFile!!)) {
             fileEditorHelper.openFile(vFile!!, true)
        } else {
             isNavigatingToXml = true
        }
        
        try {
            val targetEditor = fileEditorHelper.selectedTextEditor ?: return
            targetEditor.caretModel.moveToOffset(offset)
            targetEditor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
            if (requestFocus) {
                targetEditor.contentComponent.requestFocus()
            }
        } finally {
            isNavigatingToXml = false
        }
    }

    private fun attachNavigationListener(component: JComponent, tag: XmlTag) {
        component.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                // Special handling for TabbedPane: 
                // If user clicks a specific tab, let the ChangeListener handle navigation to the inner component.
                // We only navigate to the TabPanel tag if clicking the empty background.
                if (component is JTabbedPane) {
                     val tabIndex = component.indexAtLocation(e.x, e.y)
                     if (tabIndex != -1) return
                }

                // Do NOT consume. Let Swing handle it (e.g. Tab selection).
                // e.consume() 
                navigateToTag(tag)
            }
        })
    }

    private fun renderTag(tag: XmlTag, parentPanel: JComponent, visitedForms: Set<String> = emptySet()) {
        val tagName = tag.name
        val component: JComponent?
        
        // --- 1. Create Component based on Type ---
        when (tagName) {
            "Body", "Block" -> {
                for (subTag in tag.subTags) {
                    renderTag(subTag, parentPanel, visitedForms)
                }
                return // Container-only tags, no component to resize
            }
            else -> {
                val componentReq = createComponent(tag, visitedForms)
                if (componentReq != null) {
                    registerComponent(tag, componentReq)
                    val finalComp = wrapWithScrollIfNeeded(tag, componentReq)
                    parentPanel.add(finalComp)
                    component = componentReq
                } else {
                    component = null
                }
            }
        }
    }

    private fun wrapWithScrollIfNeeded(tag: XmlTag, component: JComponent): JComponent {
        val overflowX = tag.getAttributeValue("OverflowX")
        val overflowY = tag.getAttributeValue("OverflowY")
        
        val needScroll = (overflowX == "Auto" || overflowX == "Scroll" || 
                          overflowY == "Auto" || overflowY == "Scroll")

        if (needScroll) {
             val scroll = JBScrollPane(component)
             scroll.border = JBUI.Borders.empty() 
             
             // Set ScrollBar Policies
             when(overflowX) {
                 "Scroll" -> scroll.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
                 "Auto" -> scroll.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
                 "Hidden" -> scroll.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
                 else -> scroll.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED // Default to As Needed if implicit
             }
             
             when(overflowY) {
                 "Scroll" -> scroll.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
                 "Auto" -> scroll.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
                 "Hidden" -> scroll.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
                 else -> scroll.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
             }
             
             // Pass through TitledBorder if intended? 
             // No, the inner has the border.
             return scroll
        }
        return component
    }

    private fun createComponent(tag: XmlTag, visitedForms: Set<String> = emptySet()): JComponent? {
        val tagName = tag.name
        return when (tagName) {
             "GridLayoutPanel" -> createCoordinateGridPanel(tag, visitedForms)
             "FlexGridLayoutPanel" -> createFlexGridPanel(tag, visitedForms)
             "SplitPanel" -> createSplitPanel(tag, visitedForms)
             "TabPanel" -> createTabPanel(tag, visitedForms)
             "FlexFlowLayoutPanel", "SubDetail", "LinearLayoutPanel", "FlowLayoutPanel" -> createFlexFlowPanel(tag, visitedForms)
             "Grid" -> createWrappedGridTable(tag)
             "Embed" -> createEmbedPanel(tag, visitedForms)
             "ToolBar", "SplitSize", "RowDefCollection", "ColumnDefCollection" -> null
             else -> createLeafComponent(tag)
        }
    }

    private fun createSplitPanel(tag: XmlTag, visitedForms: Set<String>): JPanel {
         val container = JPanel()
         val orientation = tag.getAttributeValue("Orientation")
         val axis = if (orientation.equals("Vertical", ignoreCase = true)) BoxLayout.Y_AXIS else BoxLayout.X_AXIS
         container.layout = BoxLayout(container, axis)
         container.border = BorderFactory.createTitledBorder(getTitle(tag))
         
         for (subTag in tag.subTags) {
             renderTag(subTag, container, visitedForms)
         }
         return container
    }

    private fun createTabPanel(tag: XmlTag, visitedForms: Set<String>): JBTabbedPane {
         val tabbedPane = object : JBTabbedPane() {
             var isCalculating = false
             override fun getPreferredSize(): Dimension {
                 if (isCalculating) return super.getPreferredSize()
                 isCalculating = true
                 try {
                     val baseSize = super.getPreferredSize()
                     val selected = selectedComponent ?: return baseSize
                     var maxContentHeight = 0
                     for (i in 0 until tabCount) {
                         val c = getComponentAt(i)
                         val h = c.preferredSize.height
                         if (h > maxContentHeight) maxContentHeight = h
                     }
                     val overhead = baseSize.height - maxContentHeight
                     val targetHeight = overhead + selected.preferredSize.height
                     return Dimension(baseSize.width, targetHeight)
                 } finally {
                     isCalculating = false
                 }
             }
         }

         tabbedPane.border = BorderFactory.createTitledBorder(getTitle(tag))
         saveOriginalBorder(tabbedPane)

         val tabTags = mutableListOf<XmlTag>()
         for (subTag in tag.subTags) {
             if(subTag.name == "ItemChanged") continue
             val tabContainer = JPanel()
             tabContainer.layout = BoxLayout(tabContainer, BoxLayout.Y_AXIS)
             renderTag(subTag, tabContainer, visitedForms)
             val title = getTitle(subTag)
             tabbedPane.addTab(title, tabContainer)
             tabTags.add(subTag)
         }

         tabbedPane.addChangeListener {
             if (isProgrammaticSwitch) return@addChangeListener
             val index = tabbedPane.selectedIndex
             if (index >= 0 && index < tabTags.size) {
                 navigateToTag(tabTags[index])
             }
         }
         return tabbedPane
    }

    private fun createFlexFlowPanel(tag: XmlTag, visitedForms: Set<String>): JPanel {
         val container = JPanel()
         container.layout = BoxLayout(container, BoxLayout.Y_AXIS)
         container.border = BorderFactory.createTitledBorder(getTitle(tag))
         for (subTag in tag.subTags) {
             renderTag(subTag, container, visitedForms)
         }
         return container
    }

    private fun createEmbedPanel(tag: XmlTag, visitedForms: Set<String>): JPanel {
         val embedPanel = JPanel(BorderLayout())
         embedPanel.border = BorderFactory.createTitledBorder(getTitle(tag) + " [Embedded]")
         
         val formKey = tag.getAttributeValue("FormKey")
         if (!formKey.isNullOrEmpty() && !visitedForms.contains(formKey)) {
             val placeholder = JLabel("Loading $formKey...")
             embedPanel.add(placeholder, BorderLayout.CENTER)
             
             requestEmbedLoad {
                 ReadAction.nonBlocking<XmlTag?> {
                     val defAttr = FormIndex.findFormDefinition(project, formKey)
                     if (defAttr != null) {
                         PsiTreeUtil.getParentOfType(defAttr, XmlTag::class.java)
                     } else null
                 }
                 .inSmartMode(project)
                 .coalesceBy(this, formKey)
                 .finishOnUiThread(ModalityState.defaultModalityState()) { defTag ->
                     try {
                         embedPanel.remove(placeholder)
                         if (defTag != null) {
                             val body = defTag.findFirstSubTag("Body")
                             if (body != null) {
                                 val innerPanel = JPanel()
                                 innerPanel.layout = BoxLayout(innerPanel, BoxLayout.Y_AXIS)
                                 embedPanel.add(innerPanel, BorderLayout.CENTER)
                                 renderTag(body, innerPanel, visitedForms + formKey)
                             } else {
                                 embedPanel.add(JLabel("Empty Body in $formKey"), BorderLayout.CENTER)
                             }
                         } else {
                             embedPanel.add(JLabel("Form not found: $formKey"), BorderLayout.CENTER)
                         }
                         embedPanel.revalidate()
                         embedPanel.repaint()
                     } finally {
                         onEmbedLoadFinished()
                     }
                 }
                 .submit(AppExecutorUtil.getAppExecutorService())
                 .onError { onEmbedLoadFinished() }
             }
         } else if (visitedForms.contains(formKey)) {
             embedPanel.add(JLabel("Recursion detected: $formKey"), BorderLayout.CENTER)
         } else {
             val comp = createLeafComponent(tag)
             embedPanel.add(comp, BorderLayout.CENTER)
         }
         return embedPanel
        }
        


    private fun isValidGridChild(tagName: String): Boolean {
        if (tagName.endsWith("DefCollection")) return false
        return com.github.believepxw.yigo.ref.VariableReference.variableDefinitionTagNames.contains(tagName) || 
               tagName == "Grid" || tagName == "Embed" ||
               tagName == "GridLayoutPanel" || tagName == "FlexGridLayoutPanel" || 
               tagName == "FlexFlowLayoutPanel" || tagName == "LinearLayoutPanel" ||
               tagName == "SplitPanel" || tagName == "TabPanel"
    }

    private fun getTitle(tag: XmlTag): String {
        val key = tag.getAttributeValue("Key") ?: ""
        val caption = tag.getAttributeValue("Caption")
        return if (!caption.isNullOrEmpty()) caption else key.ifEmpty { tag.name }
    }
    
    private fun getIconForTag(tag: XmlTag): Icon? {
        val tagName = tag.name
        if (tagName == "GridCell") {
            val cellType = tag.getAttributeValue("CellType")
            return if (!cellType.isNullOrEmpty()) getIconForTag(cellType) else null
        }
        return getIconForTag(tagName)
    }

    private fun getIconForTag(tagName: String): Icon? {
        return when (tagName) {
            "CheckBox", "CheckListBox", "RadioButton" -> com.intellij.icons.AllIcons.Actions.Checked
            "ComboBox", "DropdownButton" -> com.intellij.icons.AllIcons.General.ArrowDown
            "Dict", "DynamicDict" -> com.intellij.icons.AllIcons.Actions.Find
            "TextEditor", "TextArea", "RichEditor", "PasswordEditor" -> com.intellij.icons.AllIcons.FileTypes.Text
            "NumberEditor" -> com.intellij.icons.AllIcons.Debugger.Db_array
            "DatePicker", "UTCDatePicker", "MonthPicker", "TimePicker" -> com.intellij.icons.AllIcons.Vcs.History
            "Button", "TextButton" -> null 
            "Label" -> com.intellij.icons.AllIcons.General.Note
            "Image", "Icon" -> com.intellij.icons.AllIcons.FileTypes.Image
            "HyperLink" -> com.intellij.icons.AllIcons.Ide.Link
            else -> null
        }
    }

    private fun createLeafComponent(tag: XmlTag): JComponent {
        val panel = object : JPanel(BorderLayout()) {
            override fun paintComponent(g: Graphics) {
                // Rounded background
                val g2 = g.create() as Graphics2D
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2.color = background
                g2.fillRoundRect(0, 0, width - 1, height - 1, 8, 8)
                g2.color = com.intellij.ui.JBColor.border()
                g2.drawRoundRect(0, 0, width - 1, height - 1, 8, 8)
                g2.dispose()
                super.paintComponent(g) // Paint children (label)
            }
        }
        panel.isOpaque = false // For rounded corners
        panel.background = com.intellij.ui.JBColor(Color(245, 245, 245), Color(60, 63, 65))
        panel.border = JBUI.Borders.empty(2, 5)
        
        val label = JLabel(getTitle(tag))
        label.icon = getIconForTag(tag)
        label.iconTextGap = 8
        label.horizontalAlignment = SwingConstants.LEFT // Align left to show icon properly
        label.foreground = com.intellij.ui.JBColor.foreground()
        label.font = JBUI.Fonts.label().deriveFont(12f)
        panel.add(label, BorderLayout.CENTER)
        
        panel.preferredSize = Dimension(80, 32)
        panel.minimumSize = Dimension(20, 32) // Allow shrinking width to fit compact layouts
        panel.putClientProperty(KEY_XML_TAG, tag)
        
        val ds = DragSource.getDefaultDragSource()
        val listener = object : java.awt.dnd.DragGestureListener {
             override fun dragGestureRecognized(dge: java.awt.dnd.DragGestureEvent) {
                 val transferable = StringSelection(tag.getAttributeValue("Key") ?: "")
                 ds.startDrag(dge, DragSource.DefaultMoveDrop, transferable, null)
                 DragContext.draggedTag = tag
             }
        }
        ds.createDefaultDragGestureRecognizer(panel, DnDConstants.ACTION_MOVE, listener)
        
        return panel
    }
    
    private fun createWrappedGridTable(tag: XmlTag): JComponent {
        val mainPanel = JPanel(GridBagLayout())
        mainPanel.border = BorderFactory.createTitledBorder("Table: ${getTitle(tag)}")
        mainPanel.transferHandler = TableDropHandler(tag, project)
        populateWrappedGridTable(tag, mainPanel)
        return mainPanel
    }
    
    private fun populateWrappedGridTable(tag: XmlTag, mainPanel: JPanel) {
        val colCollection = tag.findFirstSubTag("GridColumnCollection")
        val columns = colCollection?.findSubTags("GridColumn") ?: emptyArray()
        
        val rowCollection = tag.findFirstSubTag("GridRowCollection")
        val rows = rowCollection?.findSubTags("GridRow") ?: emptyArray()
        
        val maxColsPerRow = 8
        val chunks = columns.toList().chunked(maxColsPerRow)
        
        var gridYCounter = 0
        
        chunks.forEachIndexed { chunkIndex, chunkCols ->
             chunkCols.forEachIndexed { colInChunk, colTag ->
                 val c = GridBagConstraints()
                 c.gridx = colInChunk
                 c.gridy = gridYCounter
                 c.fill = GridBagConstraints.HORIZONTAL
                 c.weightx = 1.0
                 c.insets = JBUI.insets(2)
                 
                 val header = createLeafComponent(colTag)
                 header.background = com.intellij.ui.JBColor(Color(225, 230, 240), Color(60, 70, 80))
                 (header.getComponent(0) as? JLabel)?.font = JBUI.Fonts.label().asBold() 
                 registerComponent(colTag, header)
                 mainPanel.add(header, c)
             }
             gridYCounter++
        }
        
        val sep = JSeparator(JSeparator.HORIZONTAL)
        val sepC = GridBagConstraints()
        sepC.gridx = 0
        sepC.gridy = gridYCounter
        sepC.gridwidth = maxColsPerRow
        sepC.fill = GridBagConstraints.HORIZONTAL
        mainPanel.add(sep, sepC)
        gridYCounter++
        
        rows.forEachIndexed { rIndex, rowTag ->
             val cells = rowTag.findSubTags("GridCell")
             val cellMap = cells.associateBy { it.getAttributeValue("Key") }
             
             chunks.forEachIndexed { chunkIndex, chunkCols ->
                 chunkCols.forEachIndexed { colInChunk, colTag ->
                     val key = colTag.getAttributeValue("Key")
                     val cellTag = cellMap[key]
                     
                     if (cellTag != null) {
                         val c = GridBagConstraints()
                         c.gridx = colInChunk
                         c.gridy = gridYCounter
                         c.fill = GridBagConstraints.HORIZONTAL
                         c.weightx = 1.0
                         c.insets = JBUI.insets(1)
                         
                         val cell = createLeafComponent(cellTag)
                         registerComponent(cellTag, cell)
                         mainPanel.add(cell, c)
                     } else {
                         val placeholder = createPlaceholder(colInChunk, gridYCounter) 
                         val c = GridBagConstraints()
                         c.gridx = colInChunk
                         c.gridy = gridYCounter
                         c.fill = GridBagConstraints.HORIZONTAL
                         c.weightx = 1.0
                         c.insets = JBUI.insets(1)
                         
                         mainPanel.add(placeholder, c)
                     }
                 }
                 gridYCounter++
             }
             
             if (rIndex < rows.size - 1) {
                 val rowSep = JSeparator(JSeparator.HORIZONTAL)
                 val rc = GridBagConstraints()
                 rc.gridx = 0
                 rc.gridy = gridYCounter
                 rc.gridwidth = maxColsPerRow
                 rc.fill = GridBagConstraints.HORIZONTAL
                 rc.insets = JBUI.insets(5, 0, 5, 0)
                 mainPanel.add(rowSep, rc)
                 gridYCounter++
             }
        }
    }

    private fun createComponentForGridCell(tag: XmlTag): JComponent {
        // Now delegate to central createComponent which handles nested containers correcty
        return createComponent(tag) ?: createLeafComponent(tag)
    }

    private fun createFlexGridPanel(tag: XmlTag, visitedForms: Set<String> = emptySet()): JPanel {
        val layout = GridBagLayout()
        val panel = JPanel(layout)
        panel.border = BorderFactory.createTitledBorder("FlexGrid: ${getTitle(tag)}")
        panel.transferHandler = FlexDropHandler(tag, project)
        
        val columnCount = tag.getAttributeValue("ColumnCount")?.toIntOrNull() ?: 1
        
        // Collect valid child components in order
        // Collect valid child components in order
        val children = tag.subTags.filter { isValidGridChild(it.name) }
        
        children.forEachIndexed { index, child ->
            val x = index % columnCount
            val y = index / columnCount
            
            val c = GridBagConstraints()
            c.gridx = x
            c.gridy = y
            c.weightx = 1.0
            c.weighty = 0.0 // Don't stretch vertically by default in flex
            c.fill = GridBagConstraints.HORIZONTAL
            c.insets = JBUI.insets(2)
            c.anchor = GridBagConstraints.NORTHWEST
            
            // XSpan/YSpan support in Flex? Usually FlexGrid is 1x1, but let's clear if supported.
            // Requirement says "reduce whitespace", implying simple flow. LayoutPanel usually supports spans. 
            // If user adds XSpan/YSpan, we should respect it? 
            // "FlexGridLayoutPanel" typically implies just sequential.
            // Let's check attributs... assuming 1x1 for now as per "only ColumnCount determines".
            // Actually, if a component HAS XSpan, it might take multiple slots. 
            // But simplifying: Sequential filling usually ignores Span's impact on *position*, but Span impacts *size*.
            // Let's respect XSpan if present, but calculating next slot becomes complex (bin packing).
            // Request said "just ColumnCount determines how many columns... put elements one by one".
            // So we stick to simple index-based mapping.
            
            val compCode = createComponent(child, visitedForms) ?: createLeafComponent(child)
            registerComponent(child, compCode)
            panel.add(compCode, c)
        }
        
        // Add spacer to push content up/left
        val spacer = JPanel()
        val spacerC = GridBagConstraints()
        spacerC.gridx = 0
        spacerC.gridy = (children.size / columnCount) + 1
        spacerC.weighty = 1.0
        spacerC.fill = GridBagConstraints.VERTICAL
        panel.add(spacer, spacerC)
        
        return panel
    }

    private inner class FlexDropHandler(private val parentTag: XmlTag, private val project: Project) : TransferHandler() {
        override fun canImport(support: TransferSupport): Boolean {
             // Check if dragging a tag
             return DragContext.draggedTag != null
        }
        
        override fun importData(support: TransferSupport): Boolean {
             val draggedTag = DragContext.draggedTag ?: return false
             // Confirm draggedTag is not an ancestor of parentTag (prevent cycle)
             if (PsiTreeUtil.isAncestor(draggedTag, parentTag, false)) return false
             
             val dropLocation = support.dropLocation as? javax.swing.TransferHandler.DropLocation ?: return false
             val panel = support.component as? Container ?: return false
             
             // Find insertion point
             var targetIndex = -1
             var insertBefore = false
             
             // Simple geometry check against children
             val p = dropLocation.dropPoint
             var closestDist = Double.MAX_VALUE
             var closestComp: Component? = null
             
             for (comp in panel.components) {
                 if (comp !is JComponent || comp.getClientProperty(KEY_XML_TAG) == null) continue
                 
                 val cx = comp.x + comp.width / 2
                 val cy = comp.y + comp.height / 2
                 val dist = Math.pow((p.x - cx).toDouble(), 2.0) + Math.pow((p.y - cy).toDouble(), 2.0)
                 
                 if (dist < closestDist) {
                     closestDist = dist
                     closestComp = comp
                 }
             }
             
             if (closestComp != null) {
                 // Determine if before or after based on flow
                 // FlexGrid flow is Left->Right, Top->Bottom
                 val refTag = (closestComp as JComponent).getClientProperty(KEY_XML_TAG) as XmlTag
                 
                 // If point is to the LEFT of center, insert BEFORE
                 val center = closestComp.x + closestComp.width / 2
                 insertBefore = p.x < center
                 
                 ApplicationManager.getApplication().invokeLater {
                     WriteCommandAction.runWriteCommandAction(project) {
                         val newCopy = draggedTag.copy()
                         draggedTag.delete()
                         if (insertBefore) {
                             parentTag.addBefore(newCopy, refTag)
                         } else {
                             parentTag.addAfter(newCopy, refTag)
                         }
                     }
                 }
                 return true
             } else {
                 // Append to end if empty or far away
                 ApplicationManager.getApplication().invokeLater {
                     WriteCommandAction.runWriteCommandAction(project) {
                         val newCopy = draggedTag.copy()
                         draggedTag.delete()
                         parentTag.add(newCopy)
                     }
                 }
                 return true
             }
        }
    }

    private fun createCoordinateGridPanel(tag: XmlTag, visitedForms: Set<String> = emptySet()): JPanel {
        val layout = GridBagLayout()
        val panel = JPanel(layout)
        panel.border = BorderFactory.createTitledBorder("Grid: ${getTitle(tag)}")
        
        // Add drop support
        panel.transferHandler = GridDropHandler(tag, project)
        
        // 1. Analyze Grid Dimensions and Weights
        var rowCount = 0
        var colCount = 0
        
        val rowDefCollection = tag.findFirstSubTag("RowDefCollection")
        if (rowDefCollection != null) {
            val rows = rowDefCollection.findSubTags("RowDef")
            rowCount = rows.size
            
            // Parse Row Heights / Weights
            val heights = IntArray(rowCount)
            val weights = DoubleArray(rowCount)
            
            rows.forEachIndexed { i, row ->
                val hAttr = row.getAttributeValue("Height") ?: ""
                if (hAttr.endsWith("%")) {
                    weights[i] = hAttr.removeSuffix("%").toDoubleOrNull()?.div(100.0) ?: 0.0
                    heights[i] = 0 // Weight determines size
                } else if (hAttr.endsWith("px")) {
                    heights[i] = hAttr.removeSuffix("px").toIntOrNull() ?: 0
                    weights[i] = 0.0 // Fixed size
                } else {
                     // Default or explicit number without unit?
                     val v = hAttr.toIntOrNull()
                     if (v != null) heights[i] = v
                }
            }
            layout.rowHeights = heights
            layout.rowWeights = weights
        }
        
        val colDefCollection = tag.findFirstSubTag("ColumnDefCollection")
        if (colDefCollection != null) {
            val cols = colDefCollection.findSubTags("ColumnDef")
            colCount = cols.size
            
            // Parse Column Widths / Weights
            val widths = IntArray(colCount)
            val weights = DoubleArray(colCount)
            
            cols.forEachIndexed { i, col ->
                val wAttr = col.getAttributeValue("Width") ?: ""
                if (wAttr.endsWith("%")) {
                    weights[i] = wAttr.removeSuffix("%").toDoubleOrNull()?.div(100.0) ?: 0.0
                    widths[i] = 0
                } else if (wAttr.endsWith("px")) {
                    widths[i] = wAttr.removeSuffix("px").toIntOrNull() ?: 0
                    weights[i] = 0.0
                } else {
                     val v = wAttr.toIntOrNull()
                     if (v != null) widths[i] = v
                }
            }
            layout.columnWidths = widths
            layout.columnWeights = weights
        }

        var maxX = 0
        var maxY = 0
        val componentChildren = mutableListOf<XmlTag>()
        
        for (child in tag.subTags) {
             if (isValidGridChild(child.name)) {
                 componentChildren.add(child)
                 val x = child.getAttributeValue("X")?.toIntOrNull() ?: 0
                 val y = child.getAttributeValue("Y")?.toIntOrNull() ?: 0
                 val xSpan = child.getAttributeValue("XSpan")?.toIntOrNull() ?: 1
                 val ySpan = child.getAttributeValue("YSpan")?.toIntOrNull() ?: 1
                 if (x + xSpan > maxX) maxX = x + xSpan
                 if (y + ySpan > maxY) maxY = y + ySpan
             }
        }
        
        if (rowCount == 0) rowCount = if (maxY > 0) maxY else 1
        if (colCount == 0) colCount = if (maxX > 0) maxX else 4
        if (maxX > colCount) colCount = maxX
        if (maxY > rowCount) rowCount = maxY

        val occupied = Array(rowCount) { BooleanArray(colCount) }
        val componentMap = mutableMapOf<String, MutableList<XmlTag>>()
        for (comp in componentChildren) {
             val x = comp.getAttributeValue("X")?.toIntOrNull() ?: 0
             val y = comp.getAttributeValue("Y")?.toIntOrNull() ?: 0
             componentMap.computeIfAbsent("$x,$y") { mutableListOf() }.add(comp)
        }
        
        for (y in 0 until rowCount) {
            for (x in 0 until colCount) {
                // FIX: Check if we have explicit components here.
                val compTags = componentMap["$x,$y"]
                
                // If we have components here, we MUST render them, even if occupied says true from a previous span.
                // Exception: If we are occupied AND no components here, then we skip.
                if (occupied.getOrNull(y)?.getOrNull(x) == true && (compTags == null || compTags.isEmpty())) continue
                
                val c = GridBagConstraints()
                c.gridx = x
                c.gridy = y
                c.weightx = 1.0
                c.weighty = 1.0
                c.fill = GridBagConstraints.BOTH
                c.insets = JBUI.insets(1)

                if (compTags != null && compTags.isNotEmpty()) {
                    // Sort tags: Visible=true first. This ensures strict layout uses the visible component's span.
                    // If multiple visible, use first.
                    compTags.sortByDescending { it.getAttributeValue("Visible") != "false" }
                
                    val firstTag = compTags[0]
                    val xSpan = firstTag.getAttributeValue("XSpan")?.toIntOrNull() ?: 1
                    val ySpan = firstTag.getAttributeValue("YSpan")?.toIntOrNull() ?: 1
                    
                    c.gridwidth = xSpan
                    c.gridheight = ySpan
                    
                    val cellContent: Component
                    val childComp = createComponent(firstTag, visitedForms) ?: createLeafComponent(firstTag)
                    if (compTags.size == 1) {
                         cellContent = childComp
                         registerComponent(firstTag, cellContent)
                    } else {
                         val cellPanel = JPanel()
                         cellPanel.layout = BoxLayout(cellPanel, BoxLayout.Y_AXIS)
                         cellPanel.border = BorderFactory.createLineBorder(com.intellij.ui.JBColor.ORANGE) 
                         compTags.forEach { 
                             val comp = createComponentForGridCell(it)
                             registerComponent(it, comp)
                             cellPanel.add(comp)
                         }
                         cellContent = cellPanel
                    }
                    panel.add(cellContent, c)
                    
                    for (dy in 0 until ySpan) {
                        for (dx in 0 until xSpan) {
                            if (y + dy < rowCount && x + dx < colCount) {
                                occupied[y + dy][x + dx] = true
                            }
                        }
                    }
                } else {
                    c.gridwidth = 1
                    c.gridheight = 1
                    panel.add(createPlaceholder(x, y), c)
                }
            }
        }
        
        // Add a vertical spacer to push content to the top
        val spacer = JPanel()
        spacer.isOpaque = false
        val spacerC = GridBagConstraints()
        spacerC.gridx = 0
        spacerC.gridy = rowCount
        spacerC.weighty = 1.0 // Consume all vertical space
        panel.add(spacer, spacerC)
        
        return panel
    }
    
    private fun createPlaceholder(x: Int, y: Int): JComponent {
        val panel = JPanel()
        panel.background = com.intellij.ui.JBColor(Color(250, 250, 250), Color(43, 43, 43))
        panel.border = BorderFactory.createDashedBorder(com.intellij.ui.JBColor.border(), 1.0f, 3.0f, 3.0f, true)
        panel.toolTipText = "Empty Cell ($x, $y)"
        panel.name = "Placeholder:$x,$y" 
        return panel
    }

    object DragContext {
        var draggedTag: XmlTag? = null
    }

    private inner class GridDropHandler(private val gridTag: XmlTag, private val project: Project) : TransferHandler() {
        override fun canImport(support: TransferSupport): Boolean {
             return DragContext.draggedTag != null
        }

        override fun importData(support: TransferSupport): Boolean {
             val draggedTag = DragContext.draggedTag ?: return false
             val dropLocation = support.dropLocation as? javax.swing.TransferHandler.DropLocation ?: return false
             val component = support.component as? Container ?: return false
             
             // Find target X, Y
             var targetX = 0
             var targetY = 0
             var found = false
             
             for (comp in component.components) {
                 if (comp.bounds.contains(dropLocation.dropPoint)) {
                     if (comp.name?.startsWith("Placeholder") == true) {
                         val parts = comp.name.substringAfter(":").split(",")
                         targetX = parts[0].toInt()
                         targetY = parts[1].toInt()
                         found = true
                     } else {
                         val layout = component.layout as? GridBagLayout
                         if (layout != null) {
                             val c = layout.getConstraints(comp)
                             targetX = c.gridx
                             targetY = c.gridy
                             found = true
                         }
                     }
                     break
                 }
             }

             if (!found) return false

             ApplicationManager.getApplication().invokeLater {
                WriteCommandAction.runWriteCommandAction(project) {
                    if (draggedTag.parentTag != gridTag) {
                        try {
                             val copy = draggedTag.copy() as XmlTag
                             copy.setAttribute("X", targetX.toString())
                             copy.setAttribute("Y", targetY.toString())
                             gridTag.add(copy)
                             draggedTag.delete()
                        } catch (e: Exception) { e.printStackTrace() }
                    } else {
                        draggedTag.setAttribute("X", targetX.toString())
                        draggedTag.setAttribute("Y", targetY.toString())
                    }
                }
             }
             return true
        }
    }
    
    private inner class TableDropHandler(private val gridTag: XmlTag, private val project: Project) : TransferHandler() {
        override fun canImport(support: TransferSupport): Boolean {
             val dragged = DragContext.draggedTag
             return dragged != null && dragged.name == "GridColumn"
        }
        
        override fun importData(support: TransferSupport): Boolean {
             val draggedTag = DragContext.draggedTag ?: return false
             if (draggedTag.name != "GridColumn") return false
             
             val dropLocation = support.dropLocation as? javax.swing.TransferHandler.DropLocation ?: return false
             val component = support.component as? Container ?: return false
             
             var targetTag: XmlTag? = null
             var insertBefore = false
             
             ApplicationManager.getApplication().runReadAction {
                 for (comp in component.components) {
                     if (comp.bounds.contains(dropLocation.dropPoint)) {
                         // Optimization: Use client property instead of linear search in map
                         val tag = (comp as? JComponent)?.getClientProperty(KEY_XML_TAG) as? XmlTag
                         if (tag != null && tag.isValid && tag.name == "GridColumn") {
                             targetTag = tag
                             val center = comp.x + comp.width / 2
                             insertBefore = dropLocation.dropPoint.x < center
                         }
                         break
                     }
                 }
             }
             
             if (targetTag == null) return false
             if (targetTag == draggedTag) return false

             ApplicationManager.getApplication().invokeLater {
                 WriteCommandAction.runWriteCommandAction(project) {
                     val parent = targetTag!!.parentTag
                     if (parent != null) {
                         // Perform atomic move of column and corresponding cells
                         moveColumnAndCells(gridTag, draggedTag, targetTag!!, insertBefore)
                     }
                 }
             }
             return true
        }
    }
    
    private fun moveColumnAndCells(gridTag: XmlTag, draggedCol: XmlTag, targetCol: XmlTag, insertBefore: Boolean) {
        val colCollection = draggedCol.parentTag ?: return
        val rowCollection = gridTag.findFirstSubTag("GridRowCollection")
        
        // 1. Move Column
        val newCopy = draggedCol.copy() as XmlTag
        draggedCol.delete()
        if (insertBefore) {
            colCollection.addBefore(newCopy, targetCol)
        } else {
            colCollection.addAfter(newCopy, targetCol)
        }
        
        // 2. Move correponding cells in each row (Optimization: Move only one cell per row)
        val colKey = newCopy.getAttributeValue("Key") ?: return
        val targetKey = targetCol.getAttributeValue("Key") ?: return
        
        if (rowCollection != null) {
             for (row in rowCollection.findSubTags("GridRow")) {
                 val cells = row.findSubTags("GridCell")
                 val cellToMove = cells.find { it.getAttributeValue("Key") == colKey }
                 val targetCell = cells.find { it.getAttributeValue("Key") == targetKey }
                 
                 if (cellToMove != null && targetCell != null) {
                     val cellCopy = cellToMove.copy()
                     cellToMove.delete()
                     if (insertBefore) {
                         row.addBefore(cellCopy, targetCell)
                     } else {
                         row.addAfter(cellCopy, targetCell)
                     }
                 }
             }
        }
    }
}
    

