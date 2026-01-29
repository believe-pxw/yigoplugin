package com.github.believepxw.yigo.tool

import com.intellij.openapi.application.ApplicationManager
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
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import example.index.FormIndex
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.ModalityState
import com.intellij.util.concurrency.AppExecutorUtil
import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.dnd.DnDConstants
import java.awt.dnd.DragSource
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.ActionListener
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.border.Border
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class YigoLayoutPanel(private val project: Project, private val toolWindow: ToolWindow) : JPanel(BorderLayout()) {

    private val rootPanel = JPanel()
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
            comp.scrollRectToVisible(comp.bounds)
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

    private fun highlightComponentAtCaret(editor: Editor) {
        val offset = editor.caretModel.offset
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return
        val element = psiFile.findElementAt(offset)
        var tag = PsiTreeUtil.getParentOfType(element, XmlTag::class.java, false)
        
        // Traverse up to find a registered component
        // Note: PsiTreeUtil accesses PSI, needs read action. 
        // This is called inside updateUIFromXml's invokeLater, BUT 
        // updateUIFromXml now wraps itself in runReadAction.
        // HOWEVER, highlightComponentAtCaret is also called from CaretListener (line 96) which might NOT be wrapped.
        // Wait, line 96 calls it directly. CaretListener is often on EDT but NOT holding read lock.
        
        while (tag != null && !tagToComponent.containsKey(tag)) {
            tag = tag.parentTag
            if (tag?.name == "Form" || tag?.name == "Body") break // Stop at top level
        }
        
        if (tag != null) {
            val component = tagToComponent[tag]
            if (component != null && component != lastSelectedComponent) {
                if (lastSelectedComponent != null && lastSelectedComponent != lastSearchHighlight) {
                    restoreBorder(lastSelectedComponent!!)
                }
                
                if (component != lastSearchHighlight) {
                     setHighlightBorder(component, Color.BLUE, 2)
                }
                
                component.scrollRectToVisible(component.bounds)
                lastSelectedComponent = component
            }
        }
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
        }
        
        val targetEditor = fileEditorHelper.selectedTextEditor ?: return
        targetEditor.caretModel.moveToOffset(offset)
        targetEditor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
        if (requestFocus) {
            targetEditor.contentComponent.requestFocus()
        }
    }

    private fun attachNavigationListener(component: JComponent, tag: XmlTag) {
        component.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                e.consume()
                navigateToTag(tag)
            }
        })
    }

    private fun renderTag(tag: XmlTag, parentPanel: JComponent, visitedForms: Set<String> = emptySet()) {
        when (tag.name) {
            "Body", "Block" -> {
                for (subTag in tag.subTags) {
                    renderTag(subTag, parentPanel, visitedForms)
                }
            }
            "GridLayoutPanel", "FlexGridLayoutPanel" -> {
                val gridPanel = createGridPanel(tag)
                registerComponent(tag, gridPanel)
                parentPanel.add(gridPanel)
            }
            "FlexFlowLayoutPanel", "SplitPanel", "TabPanel", "SubDetail" -> {
                 val container = JPanel()
                 container.layout = BoxLayout(container, BoxLayout.Y_AXIS)
                 container.border = BorderFactory.createTitledBorder(getTitle(tag))
                 registerComponent(tag, container)
                 parentPanel.add(container)
                 for (subTag in tag.subTags) {
                     renderTag(subTag, container, visitedForms)
                 }
            }
            "Grid" -> {
                val grid = createWrappedGridTable(tag)
                registerComponent(tag, grid)
                parentPanel.add(grid)
            }
             "SplitSize" -> {}
             "ToolBar" -> {} // Skip rendering ToolBar
             "Embed" -> { 
                 val embedPanel = JPanel(BorderLayout())
                 embedPanel.border = BorderFactory.createTitledBorder(getTitle(tag) + " [Embedded]")
                 registerComponent(tag, embedPanel)
                 parentPanel.add(embedPanel)
                 
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
             }
            "RowDefCollection", "ColumnDefCollection" -> {}
            else -> {
                val componentReq = createLeafComponent(tag)
                registerComponent(tag, componentReq)
                parentPanel.add(componentReq)
            }
        }
    }

    private fun getTitle(tag: XmlTag): String {
        val key = tag.getAttributeValue("Key") ?: ""
        val caption = tag.getAttributeValue("Caption")
        return if (!caption.isNullOrEmpty()) "$caption ($key)" else key.ifEmpty { tag.name }
    }

    private fun createLeafComponent(tag: XmlTag): JComponent {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createLineBorder(Color.GRAY)
        panel.background = Color(240, 240, 240)
        
        val label = JLabel(getTitle(tag))
        label.horizontalAlignment = SwingConstants.CENTER
        panel.add(label, BorderLayout.CENTER)
        
        panel.preferredSize = Dimension(80, 30)
        panel.putClientProperty(KEY_XML_TAG, tag)
        
        // attachNavigationListener call removed here because registerComponent does it
        // BUT drag source logic needs to stay
        
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
                 header.background = Color(220, 220, 240)
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
        return when (tag.name) {
            "Grid" -> createWrappedGridTable(tag)
            "GridLayoutPanel", "FlexGridLayoutPanel" -> createGridPanel(tag)
            else -> createLeafComponent(tag)
        }
    }

    private fun createGridPanel(tag: XmlTag): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.border = BorderFactory.createTitledBorder("Grid: ${getTitle(tag)}")
        
        panel.transferHandler = GridDropHandler(tag, project)

        var rowCount = 0
        var colCount = 0
        val rowDefCollection = tag.findFirstSubTag("RowDefCollection")
        if (rowDefCollection != null) rowCount = rowDefCollection.findSubTags("RowDef").size
        val colDefCollection = tag.findFirstSubTag("ColumnDefCollection")
        if (colDefCollection != null) colCount = colDefCollection.findSubTags("ColumnDef").size
        
        var maxX = 0
        var maxY = 0
        val componentChildren = mutableListOf<XmlTag>()
        
        for (child in tag.subTags) {
             if (child.name.endsWith("DefCollection")) continue
             val name = child.name
             if (com.github.believepxw.yigo.ref.VariableReference.variableDefinitionTagNames.contains(name) || name == "Grid" || name == "Embed") {
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
                    if (compTags.size == 1) {
                         cellContent = createComponentForGridCell(firstTag)
                         registerComponent(firstTag, cellContent)
                    } else {
                         val cellPanel = JPanel()
                         cellPanel.layout = BoxLayout(cellPanel, BoxLayout.Y_AXIS)
                         cellPanel.border = BorderFactory.createLineBorder(Color.ORANGE) 
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
        
        return panel
    }
    
    private fun createPlaceholder(x: Int, y: Int): JComponent {
        val panel = JPanel()
        panel.border = BorderFactory.createDashedBorder(Color.LIGHT_GRAY)
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
    

