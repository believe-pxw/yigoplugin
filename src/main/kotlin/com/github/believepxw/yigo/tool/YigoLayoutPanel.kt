package com.github.believepxw.yigo.tool

import com.github.believepxw.yigo.icons.YigoIcons
import com.github.believepxw.yigo.ref.VariableReference
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.JBUI
import example.index.FormIndex
import java.awt.*
import java.awt.datatransfer.StringSelection
import java.awt.dnd.DnDConstants
import java.awt.dnd.DragGestureEvent
import java.awt.dnd.DragGestureListener
import java.awt.dnd.DragSource
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.util.*
import javax.swing.*
import javax.swing.border.Border

class YigoLayoutPanel(val project: Project, private val toolWindow: ToolWindow) : JPanel(BorderLayout()) {

    val rootPanel: JPanel = object : JPanel(), Scrollable {
        // Implement Scrollable with smart width tracking: 
        // Expand to viewport if minimum content fits (true), but allow scroll if even minimum is wider (false).
        override fun getScrollableTracksViewportWidth(): Boolean {
            val viewport = parent as? JViewport ?: return true
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
    
    // State
    internal val tagToComponent = WeakHashMap<XmlTag, JComponent>()
    private var lastSelectedComponent: JComponent? = null // From Editor Caret
    internal var lastSearchHighlight: JComponent? = null // From Search
    
    // Handlers
    private val searchHandler = YigoSearchHandler(this)
    private val deleteHandler = YigoDeleteHandler(this)
    
    // Constant for client property key
    companion object {
        private const val KEY_ORIGINAL_BORDER = "YigoOriginalBorder"
        private const val KEY_XML_TAG = "YigoXmlTag"
        private const val KEY_NAV_LISTENER = "YigoNavigationListener"
    }
    
    // Embed Loading Queue
    private val embedLoadQueue = ArrayDeque<() -> Unit>()
    private var activeEmbedLoads = 0
    private val MAX_CONCURRENT_EMBED_LOADS = 5
    
    // Flag to prevent recursive navigation updates
    private var isProgrammaticSwitch = false
    private var isNavigatingToXml = false
    internal var isDeletingControls = false
    var isBatchUpdating = false

    init {
        rootPanel.layout = BoxLayout(rootPanel, BoxLayout.Y_AXIS)
        rootPanel.border = JBUI.Borders.empty(10)
        
        add(scrollPane, BorderLayout.CENTER)
        
        // Register Search Shortcut (Ctrl+F / Cmd+F)
        val searchAction = object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                val currentEditor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
                val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(currentEditor.document) as? XmlFile ?: return
                val rootTag = psiFile.rootTag ?: return
                val bodyTag = rootTag.findFirstSubTag("Body") ?: rootTag
                searchHandler.ScopeSearchDialog(bodyTag).showDialog()
            }
        }
        val keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx)
        this.registerKeyboardAction(searchAction, keyStroke, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        rootPanel.registerKeyboardAction(searchAction, keyStroke, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)

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
        PsiManager.getInstance(project).addPsiTreeChangeListener(object : PsiTreeChangeAdapter() {
            override fun childrenChanged(event: PsiTreeChangeEvent) { processEvent(event) }
            override fun childAdded(event: PsiTreeChangeEvent) { processEvent(event) }
            override fun childRemoved(event: PsiTreeChangeEvent) { processEvent(event) }
            override fun childMoved(event: PsiTreeChangeEvent) { processEvent(event) }
            
            fun processEvent(event: PsiTreeChangeEvent) {
                 if (isDeletingControls || isBatchUpdating) return
                 val element = event.parent ?: event.child ?: return
                 val tag = PsiTreeUtil.getParentOfType(element, XmlTag::class.java, false)
                 
                 if (tag != null) {
                     val mappableContainer = findMappableContainer(tag)
                     if (mappableContainer != null) {
                         val pointer = com.intellij.psi.SmartPointerManager.getInstance(project).createSmartPsiElementPointer(mappableContainer)
                         SwingUtilities.invokeLater {
                             if (project.isDisposed) return@invokeLater
                             ApplicationManager.getApplication().runReadAction {
                                 val liveContainer = pointer.element
                                 if (liveContainer != null && liveContainer.isValid) {
                                     // If the PSI node was completely replaced by parser, rebuild UI to prevent stale references in Parent components/listeners
                                     if (liveContainer !== mappableContainer && !tagToComponent.containsKey(liveContainer)) {
                                         updateUIFromXml(preserveScroll = true)
                                     } else {
                                         refreshComponent(liveContainer)
                                         highlightComponentAtCaret(FileEditorManager.getInstance(project).selectedTextEditor ?: return@runReadAction)
                                     }
                                 } else {
                                     updateUIFromXml(preserveScroll = true)
                                 }
                             }
                         }
                         return
                     }
                 }
                 
                 SwingUtilities.invokeLater {
                    if (project.isDisposed) return@invokeLater
                    ApplicationManager.getApplication().runReadAction {
                        updateUIFromXml(preserveScroll = true)
                    }
                 }
            }
        }, toolWindow.contentManager)
    }

    private fun findMappableContainer(startTag: XmlTag): XmlTag? {
        var current: XmlTag? = startTag
        while (current != null) {
            if (tagToComponent.containsKey(current) && isContainerTag(current.name)) return current
            if (current.name == "Form" || current.name == "Body") return null 
            current = current.parentTag
        }
        return null
    }

    private fun isContainerTag(name: String): Boolean {
        return name == "GridLayoutPanel" || name == "FlexGridLayoutPanel" || 
               name == "Grid" || name == "TabPanel" || 
               name == "FlexFlowLayoutPanel" || name == "SubDetail" || 
               name == "LinearLayoutPanel" || name == "FlowLayoutPanel" ||
               name == "SplitPanel"
    }

    fun refreshComponent(tag: XmlTag) {
        val comp = tagToComponent[tag] ?: return
        when (tag.name) {
            "Grid" -> {
                if (comp is JPanel) {
                    comp.removeAll()
                    populateWrappedGridTable(tag, comp)
                    comp.revalidate()
                    comp.repaint()
                    attachNavigationListener(comp, tag)
                }
            }
            "GridLayoutPanel" -> {
                if (comp is TransparentHighlightPanel) {
                    comp.removeAll()
                    populateCoordinateGridPanel(tag, comp, emptySet()) // Note: visitedForms simplified here
                    comp.revalidate()
                    comp.repaint()
                    attachNavigationListener(comp, tag)
                }
            }
            "FlexGridLayoutPanel" -> {
                if (comp is JPanel) {
                    comp.removeAll()
                    populateFlexGridPanel(tag, comp, emptySet())
                    comp.revalidate()
                    comp.repaint()
                    attachNavigationListener(comp, tag)
                }
            }
            "TabPanel" -> {
                if (comp is JBTabbedPane) {
                    isProgrammaticSwitch = true
                    try {
                        val selectedIndex = comp.selectedIndex
                        comp.removeAll()
                        populateTabPanel(tag, comp, emptySet())
                        if (selectedIndex >= 0 && selectedIndex < comp.tabCount) {
                            comp.selectedIndex = selectedIndex
                        }
                    } finally {
                        isProgrammaticSwitch = false
                    }
                    comp.revalidate()
                    comp.repaint()
                }
            }
            "FlexFlowLayoutPanel", "SubDetail", "LinearLayoutPanel", "FlowLayoutPanel" -> {
                if (comp is JPanel) {
                    comp.removeAll()
                    populateFlexFlowPanel(tag, comp, emptySet())
                    comp.revalidate()
                    comp.repaint()
                    attachNavigationListener(comp, tag)
                }
            }
            "SplitPanel" -> {
                if (comp is JPanel) {
                    comp.removeAll()
                    populateSplitPanel(tag, comp, emptySet())
                    comp.revalidate()
                    comp.repaint()
                    attachNavigationListener(comp, tag)
                }
            }
        }
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
    
    internal fun restoreBorder(component: JComponent) {
        val original = component.getClientProperty(KEY_ORIGINAL_BORDER) as? Border
        component.border = original ?: BorderFactory.createEmptyBorder()
    }
    
    internal fun setHighlightBorder(component: JComponent, color: Color, thickness: Int) {
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
    
    internal fun requestEmbedLoad(task: () -> Unit) {
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


    fun findParentGridTag(startTag: XmlTag): XmlTag? {
        var current: XmlTag? = startTag
        while (current != null) {
            if (current.name == "Grid") return current
            if (current.name == "Form" || current.name == "Body") return null 
            current = current.parentTag
        }
        return null
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
        
        if (lastSearchHighlight != null) {
            restoreBorder(lastSearchHighlight!!)
            lastSearchHighlight = null
        }
        
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
        
        SwingUtilities.invokeLater {
            if (project.isDisposed) return@invokeLater
            ApplicationManager.getApplication().runReadAction {
                if (preserveScroll) {
                     scrollPane.verticalScrollBar.value = vScroll
                     scrollPane.horizontalScrollBar.value = hScroll
                     highlightComponentAtCaret(editor, suppressScroll = true)
                } else {
                     highlightComponentAtCaret(editor, suppressScroll = false)
                }
                
            }
        }
    }

    // Track the currently highlighted grid to clear it later
    private var lastHighlightedGrid: TransparentHighlightPanel? = null
    
    internal fun highlightComponentAtCaret(editor: Editor, suppressScroll: Boolean = false) {
        val offset = editor.caretModel.offset
        // 1. Read PSI to find tag (Must be in ReadAction)
        var targetTag: XmlTag? = null
        var highlightRowToSet = -1
        var highlightColToSet = -1
        var targetGridTag: XmlTag? = null
        
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return
        val element = psiFile.findElementAt(offset)
        var tag = PsiTreeUtil.getParentOfType(element, XmlTag::class.java, false)
        
        // --- Highlighting Logic for RowDef / ColumnDef ---
        // Check if we are inside a RowDef or ColumnDef
        var current = tag
        while (current != null) {
            if (current.name == "RowDef") {
                // Find index
                var index = 0
                var sibling = current.prevSibling
                while (sibling != null) {
                    if (sibling is XmlTag && sibling.name == "RowDef") index++
                    sibling = sibling.prevSibling
                }
                highlightRowToSet = index
                // Find parent GridLayoutPanel (RowDef -> RowDefCollection -> GridLayoutPanel)
                targetGridTag = current.parentTag?.parentTag
                break
            } else if (current.name == "ColumnDef") {
                 // Find index
                var index = 0
                var sibling = current.prevSibling
                while (sibling != null) {
                    if (sibling is XmlTag && sibling.name == "ColumnDef") index++
                    sibling = sibling.prevSibling
                }
                highlightColToSet = index
                targetGridTag = current.parentTag?.parentTag
                break
            }
            // Stop if we hit Form/Body or a mapped component to avoid walking too far
            if (tagToComponent.containsKey(current) || current.name == "Form") break
            current = current.parentTag
        }
        // -----------------------------------------------

        while (tag != null && !tagToComponent.containsKey(tag)) {
            tag = tag.parentTag
            if (tag?.name == "Form" || tag?.name == "Body") break 
        }
        targetTag = tag
        
        // Update UI
        val finalGridTag = targetGridTag
        val finalRow = highlightRowToSet
        val finalCol = highlightColToSet
        val shouldSuppressScroll = isNavigatingToXml || suppressScroll
        
        ApplicationManager.getApplication().invokeLater {
            if (project.isDisposed) return@invokeLater
            rootPanel.validate() // Force layout to get valid bounds for new components
            
            // 1. Handle Grid Highlighting
            if (finalGridTag != null && (finalRow != -1 || finalCol != -1)) {
                 val gridComponent = tagToComponent[finalGridTag]
                 if (gridComponent is TransparentHighlightPanel) {
                     gridComponent.setHighlight(finalRow, finalCol)
                     if (lastHighlightedGrid != null && lastHighlightedGrid != gridComponent) {
                         lastHighlightedGrid?.setHighlight(-1, -1)
                     }
                     lastHighlightedGrid = gridComponent
                     
                     ensureComponentVisible(gridComponent, shouldSuppressScroll)
                 }
            } else {
                // Clear previous highlight if we moved away from Row/Col def
                if (lastHighlightedGrid != null) {
                    lastHighlightedGrid?.setHighlight(-1, -1)
                    lastHighlightedGrid = null
                }
            }

            // 2. Handle Normal Component Highlighting
            if (targetTag != null) {
                val component = tagToComponent[targetTag]
                if (component != null && component != lastSelectedComponent) {
                    if (lastSelectedComponent != null && lastSelectedComponent != lastSearchHighlight) {
                        restoreBorder(lastSelectedComponent!!)
                    }
                    
                    if (component != lastSearchHighlight) {
                         setHighlightBorder(component, Color.BLUE, 2)
                    }
                    
                    ensureComponentVisible(component, shouldSuppressScroll)
                    lastSelectedComponent = component
                }
            }
        }
    }
    
        internal fun ensureComponentVisible(component: JComponent, suppressScroll: Boolean = false) {
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
        if (!suppressScroll) {
            component.scrollRectToVisible(component.bounds)
        }
    }

    internal fun navigateToTag(tag: XmlTag, requestFocus: Boolean = true) {
        if (project.isDisposed) return
        
        // Read PSI to get offset and file info
        var offset = 0
        var vFile: VirtualFile? = null
        var psiFile: PsiFile? = null
        
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
            highlightComponentAtCaret(fileEditorHelper.selectedTextEditor ?: return)
        }
    }

    private fun attachNavigationListener(component: JComponent, tag: XmlTag) {
        // Remove existing listener if any
        val old = component.getClientProperty(KEY_NAV_LISTENER) as? MouseListener
        if (old != null) component.removeMouseListener(old)

        val listener = object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (component is JTabbedPane) {
                     val tabIndex = component.indexAtLocation(e.x, e.y)
                     if (tabIndex != -1) return
                }
                navigateToTag(tag)
            }
        }
        component.addMouseListener(listener)
        component.putClientProperty(KEY_NAV_LISTENER, listener)
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
         addContainerContextMenu(container, tag)
         
         populateSplitPanel(tag, container, visitedForms)
         return container
    }

    private fun populateSplitPanel(tag: XmlTag, container: JPanel, visitedForms: Set<String>) {
        for (subTag in tag.subTags) {
            renderTag(subTag, container, visitedForms)
        }
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
         addContainerContextMenu(tabbedPane, tag)

         populateTabPanel(tag, tabbedPane, visitedForms)
         return tabbedPane
    }

    private fun populateTabPanel(tag: XmlTag, tabbedPane: JBTabbedPane, visitedForms: Set<String>) {
        // Remove existing listener as we are going to rebuild tabs and it might refer to old tabTags
        val existingListeners = tabbedPane.changeListeners
        for (list in existingListeners) {
            if (list !is Component) tabbedPane.removeChangeListener(list)
        }

        val tabTags = mutableListOf<XmlTag>()
        for (subTag in tag.subTags) {
            if (subTag.name == "ItemChanged") continue
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
    }

    private fun createFlexFlowPanel(tag: XmlTag, visitedForms: Set<String>): JPanel {
         val container = JPanel()
         container.layout = BoxLayout(container, BoxLayout.Y_AXIS)
         container.border = BorderFactory.createTitledBorder(getTitle(tag))
         container.transferHandler = FlexDropHandler(this, tag, project)
         addContainerContextMenu(container, tag)
         populateFlexFlowPanel(tag, container, visitedForms)
         return container
    }

    private fun populateFlexFlowPanel(tag: XmlTag, container: JPanel, visitedForms: Set<String>) {
        for (subTag in tag.subTags) {
            renderTag(subTag, container, visitedForms)
        }
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
        


    internal fun isValidGridChild(tagName: String): Boolean {
        if (tagName.endsWith("DefCollection")) return false
        return VariableReference.variableDefinitionTagNames.contains(tagName) ||
               tagName == "Grid" || tagName == "Embed" ||
               tagName == "GridLayoutPanel" || tagName == "FlexGridLayoutPanel" || 
               tagName == "FlexFlowLayoutPanel" || tagName == "LinearLayoutPanel" ||
               tagName == "SplitPanel" || tagName == "TabPanel"
    }

    internal fun getTitle(tag: XmlTag): String {
        val key = tag.getAttributeValue("Key") ?: ""
        val caption = tag.getAttributeValue("Caption")
        return if (!caption.isNullOrEmpty()) caption else key.ifEmpty { tag.name }
    }
    
    internal fun getIconForTag(tag: XmlTag): Icon? {
        val tagName = tag.name
        if (tagName == "GridCell") {
            val cellType = tag.getAttributeValue("CellType")
            return if (!cellType.isNullOrEmpty()) getIconForTag(cellType) else null
        }
        return getIconForTag(tagName)
    }

    internal fun getIconForTag(tagName: String): Icon? {
        return YigoIcons.getIconForTag(tagName)
    }

    private fun createLeafComponent(tag: XmlTag): JComponent {
        val panel = object : JPanel(BorderLayout()) {
            override fun paintComponent(g: Graphics) {
                // Rounded background
                val g2 = g.create() as Graphics2D
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2.color = background
                g2.fillRoundRect(0, 0, width - 1, height - 1, 8, 8)
                g2.color = JBColor.border()
                g2.drawRoundRect(0, 0, width - 1, height - 1, 8, 8)
                g2.dispose()
                super.paintComponent(g) // Paint children (label)
            }
        }
        panel.isOpaque = false // For rounded corners
        panel.background = JBColor(Color(245, 245, 245), Color(60, 63, 65))
        panel.border = JBUI.Borders.empty(2, 5)
        
        val label = JLabel(getTitle(tag))
        label.icon = getIconForTag(tag)
        label.iconTextGap = 8
        label.horizontalAlignment = SwingConstants.LEFT // Align left to show icon properly
        label.foreground = JBColor.foreground()
        label.font = JBUI.Fonts.label().deriveFont(12f)
        panel.add(label, BorderLayout.CENTER)
        
        panel.preferredSize = Dimension(80, 32)
        panel.minimumSize = Dimension(20, 32) // Allow shrinking width to fit compact layouts
        panel.putClientProperty(KEY_XML_TAG, tag)
        
        val ds = DragSource.getDefaultDragSource()
        val listener = object : DragGestureListener {
             override fun dragGestureRecognized(dge: DragGestureEvent) {
                 val transferable = StringSelection(tag.getAttributeValue("Key") ?: "")
                 ds.startDrag(dge, DragSource.DefaultMoveDrop, transferable, null)
                 DragContext.draggedTag = tag
             }
         }
        ds.createDefaultDragGestureRecognizer(panel, DnDConstants.ACTION_MOVE, listener)
        
        // Right-click menu for leaf controls
        panel.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) { handlePopup(e) }
            override fun mouseReleased(e: MouseEvent) { handlePopup(e) }

            private fun handlePopup(e: MouseEvent) {
                if (e.isPopupTrigger) {
                    // Priority 1: Grid elements (Column/Cell) -> Show Grid menu
                    if (tag.name == "GridColumn" || tag.name == "GridCell") {
                        val gridTag = findParentGridTag(tag)
                        if (gridTag != null) {
                            val parentComp = tagToComponent[gridTag]
                            if (parentComp != null) {
                                val p = SwingUtilities.convertPoint(e.component, e.point, parentComp)
                                showContainerContextMenu(parentComp, gridTag, p.x, p.y, targetTag = tag)
                                return
                            }
                        }
                    }

                    // Priority 2: Inside GridLayoutPanel -> Show GLP menu (adding controls)
                    val glpTag = findParentGridLayoutPanelTag(tag)
                    if (glpTag != null) {
                        val parentComp = tagToComponent[glpTag]
                        if (parentComp != null) {
                            val p = SwingUtilities.convertPoint(e.component, e.point, parentComp)
                            showContainerContextMenu(parentComp, glpTag, p.x, p.y, targetTag = tag)
                            return
                        }
                    }
                    
                    showLeafDeleteMenu(e, tag)
                }
            }
        })
        
        return panel
    }

    private fun createWrappedGridTable(tag: XmlTag): JComponent {
        val mainPanel = JPanel(GridBagLayout())
        val rowCollection = tag.findFirstSubTag("GridRowCollection")
        val rows = rowCollection?.findSubTags("GridRow") ?: emptyArray()
        val firstRow = rows.firstOrNull()
        val tableKey = firstRow?.getAttributeValue("TableKey")
        mainPanel.border = BorderFactory.createTitledBorder("Table ${getTitle(tag)} [$tableKey]")
        mainPanel.transferHandler = TableDropHandler(tag, project)
        addContainerContextMenu(mainPanel, tag)
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
                 header.background = JBColor(Color(225, 230, 240), Color(60, 70, 80))
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
                          val placeholder = createPlaceholder(colInChunk, gridYCounter, tag) 
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
        panel.transferHandler = FlexDropHandler(this, tag, project)
        addContainerContextMenu(panel, tag)
        
        populateFlexGridPanel(tag, panel, visitedForms)
        return panel
    }

    private fun populateFlexGridPanel(tag: XmlTag, panel: JPanel, visitedForms: Set<String>) {
        val columnCount = tag.getAttributeValue("ColumnCount")?.toIntOrNull() ?: 1
        val children = tag.subTags.filter { isValidGridChild(it.name) }
        
        children.forEachIndexed { index, child ->
            val x = index % columnCount
            val y = index / columnCount
            
            val c = GridBagConstraints()
            c.gridx = x
            c.gridy = y
            c.weightx = 1.0
            c.weighty = 0.0
            c.fill = GridBagConstraints.HORIZONTAL
            c.insets = JBUI.insets(2)
            c.anchor = GridBagConstraints.NORTHWEST
            
            val compCode = createComponent(child, visitedForms) ?: createLeafComponent(child)
            registerComponent(child, compCode)
            panel.add(compCode, c)
        }
        
        val spacer = JPanel()
        val spacerC = GridBagConstraints()
        spacerC.gridx = 0
        spacerC.gridy = (children.size / columnCount) + 1
        spacerC.weighty = 1.0
        spacerC.fill = GridBagConstraints.VERTICAL
        panel.add(spacer, spacerC)
    }

    // --- Flex Drag & Drop Extracted to YigoDragDropHandlers.kt ---

    private fun createCoordinateGridPanel(tag: XmlTag, visitedForms: Set<String> = emptySet()): JPanel {
        val layout = GridBagLayout()
        val panel = TransparentHighlightPanel(layout) // Use custom panel for highlighting
        panel.border = BorderFactory.createTitledBorder("Grid: ${getTitle(tag)}")
        panel.transferHandler = GridDropHandler(this, tag, project)
        addContainerContextMenu(panel, tag)
        
        populateCoordinateGridPanel(tag, panel, visitedForms)
        return panel
    }

    private fun populateCoordinateGridPanel(tag: XmlTag, panel: TransparentHighlightPanel, visitedForms: Set<String>) {
        val layout = panel.layout as GridBagLayout
        
        // 1. Analyze Grid Dimensions and Weights
        var rowCount = 0
        var colCount = 0
        
        val rowDefCollection = tag.findFirstSubTag("RowDefCollection")
        if (rowDefCollection != null) {
            val rows = rowDefCollection.findSubTags("RowDef")
            rowCount = rows.size
            val heights = IntArray(rowCount)
            val weights = DoubleArray(rowCount)
            rows.forEachIndexed { i, row ->
                val hAttr = row.getAttributeValue("Height") ?: ""
                if (hAttr.endsWith("%")) {
                    weights[i] = hAttr.removeSuffix("%").toDoubleOrNull()?.div(100.0) ?: 0.0
                    heights[i] = 0
                } else if (hAttr.endsWith("px")) {
                    heights[i] = 0
                    weights[i] = 0.0
                } else {
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
            val widths = IntArray(colCount)
            val weights = DoubleArray(colCount)
            cols.forEachIndexed { i, col ->
                val wAttr = col.getAttributeValue("Width") ?: ""
                if (wAttr.endsWith("%")) {
                    weights[i] = wAttr.removeSuffix("%").toDoubleOrNull()?.div(100.0) ?: 0.0
                    widths[i] = 0
                } else if (wAttr.endsWith("px")) {
                    widths[i] = 0
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
        
        panel.setGridDimensions(rowCount, colCount)

        val occupied = Array(rowCount) { BooleanArray(colCount) }
        val componentMap = mutableMapOf<String, MutableList<XmlTag>>()
        for (comp in componentChildren) {
             val x = comp.getAttributeValue("X")?.toIntOrNull() ?: 0
             val y = comp.getAttributeValue("Y")?.toIntOrNull() ?: 0
             componentMap.computeIfAbsent("$x,$y") { mutableListOf() }.add(comp)
        }
        
        for (y in 0 until rowCount) {
            for (x in 0 until colCount) {
                val compTags = componentMap["$x,$y"]
                if (occupied.getOrNull(y)?.getOrNull(x) == true && (compTags == null || compTags.isEmpty())) continue
                
                val c = GridBagConstraints()
                c.gridx = x
                c.gridy = y
                c.weightx = 1.0
                c.weighty = 1.0
                c.fill = GridBagConstraints.BOTH
                c.insets = JBUI.insets(1)

                if (compTags != null && compTags.isNotEmpty()) {
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
                         cellPanel.border = BorderFactory.createLineBorder(JBColor.ORANGE)
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
                    panel.add(createPlaceholder(x, y, tag), c)
                }
            }
        }
    }
    
    // Custom JPanel to support drawing highlights over/under children
    private class TransparentHighlightPanel(layout: LayoutManager) : JPanel(layout) {
        var highlightRow = -1
        var highlightCol = -1
        private var rowCount = 0
        private var colCount = 0
        
        fun setGridDimensions(rows: Int, cols: Int) {
            this.rowCount = rows
            this.colCount = cols
        }
        
        fun setHighlight(row: Int, col: Int) {
            if (highlightRow != row || highlightCol != col) {
                highlightRow = row
                highlightCol = col
                repaint()
            }
        }

        override fun paintChildren(g: Graphics) {
            super.paintChildren(g) // Paint components first
            
            if (highlightRow >= 0 || highlightCol >= 0) {
                val layout = layout as? GridBagLayout ?: return
                
                // Use getLayoutDimensions to calculate exact grid lines
                // dimensions[0] = column widths
                // dimensions[1] = row heights
                val dimensions = layout.getLayoutDimensions()
                val origin = layout.getLayoutOrigin()
                val columnWidths = dimensions[0]
                val rowHeights = dimensions[1]
                
                var x = origin.x
                var y = origin.y
                var w = 0
                var h = 0
                
                // Calculate Total Size if needed or specific component location
                // We want to highlight the entire "Strip".
                
                val totalW = columnWidths.sum()
                val totalH = rowHeights.sum()
                
                val g2 = g.create() as Graphics2D
                g2.color = Color(255, 255, 0, 40) // Semi-transparent yellow
                
                if (highlightCol >= 0 && highlightCol < columnWidths.size) {
                    // Calculate X start and Width of the column
                    var colX = origin.x
                    for (i in 0 until highlightCol) {
                        colX += columnWidths[i]
                    }
                    val colW = columnWidths[highlightCol]
                    
                    // Highlight Column Strip (Full Height)
                    g2.fillRect(colX, origin.y, colW, totalH)
                    g2.color = Color(255, 200, 0, 180)
                    g2.stroke = BasicStroke(2f)
                    g2.drawRect(colX, origin.y, colW, totalH)
                    // Reset color for next draw
                    g2.color = Color(255, 255, 0, 40)
                }
                
                if (highlightRow >= 0 && highlightRow < rowHeights.size) {
                    // Calculate Y start and Height of the row
                    var rowY = origin.y
                    for (i in 0 until highlightRow) {
                        rowY += rowHeights[i]
                    }
                    val rowH = rowHeights[highlightRow]
                    
                    // Highlight Row Strip (Full Width)
                    g2.fillRect(origin.x, rowY, totalW, rowH)
                    g2.color = Color(255, 200, 0, 180)
                    g2.stroke = BasicStroke(2f)
                    g2.drawRect(origin.x, rowY, totalW, rowH)
                }
                
                g2.dispose()
            }
        }
    }

    private fun findParentGridLayoutPanelTag(tag: XmlTag): XmlTag? {
        var current = tag.parentTag
        while (current != null) {
            if (current.name == "GridLayoutPanel") return current
            if (current.name == "Form" || current.name == "Body") break
            current = current.parentTag
        }
        return null
    }

    private fun addContainerContextMenu(panel: JComponent, tag: XmlTag) {
        panel.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) { handlePopup(e) }
            override fun mouseReleased(e: MouseEvent) { handlePopup(e) }

            private fun handlePopup(e: MouseEvent) {
                if (e.isPopupTrigger) {
                    showContainerContextMenu(e.component as JComponent, tag, e.x, e.y)
                }
            }
        })
    }

    private fun showContainerContextMenu(component: JComponent, tag: XmlTag, x: Int, y: Int, targetTag: XmlTag? = null) {
        val menu = JPopupMenu()

        if (tag.name == "GridLayoutPanel") {
            val addControlItem = JMenuItem("Add Control...")
            addControlItem.addActionListener {
                YigoControlBuilder(project).showAddControlDialog(addControlItem, tag, x, y, component)
            }
            menu.add(addControlItem)

            val addByDEItem = JMenuItem("Add by DataElementKey...")
            addByDEItem.addActionListener {
                YigoControlBuilder(project).showAddByDEDialog(addByDEItem, tag, x, y, component)
            }
            menu.add(addByDEItem)
            menu.addSeparator()

            val itemRow = JMenuItem("Go to RowDefCollection")
            itemRow.addActionListener {
                findChildTag(tag, "RowDefCollection")?.let { navigateToTag(it) }
            }
            menu.add(itemRow)

            val itemCol = JMenuItem("Go to ColumnDefCollection")
            itemCol.addActionListener {
                findChildTag(tag, "ColumnDefCollection")?.let { navigateToTag(it) }
            }
            menu.add(itemCol)
            menu.addSeparator()
        } else if (tag.name == "Grid") {
            val afterColumnKey = if (targetTag?.name == "GridColumn") targetTag.getAttributeValue("Key") else null
            val addColumnItem = JMenuItem(if (afterColumnKey != null) "Add Column After..." else "Add Column...")
            addColumnItem.addActionListener {
                YigoControlBuilder(project).showAddGridColumnDialog(component, tag, afterColumnKey)
            }
            menu.add(addColumnItem)

            val addByDEItem = JMenuItem("Add by DataElementKey...")
            addByDEItem.addActionListener {
                YigoControlBuilder(project).showAddByDEDialog(addByDEItem, tag, containerComponent = component, afterColumnKey = afterColumnKey)
            }
            menu.add(addByDEItem)
            menu.addSeparator()
        }

        val itemSearch = JMenuItem("Search in this container...")
        itemSearch.addActionListener {
            searchHandler.ScopeSearchDialog(tag).showDialog()
        }
        menu.add(itemSearch)

        menu.addSeparator()
        val batchDeleteItem = JMenuItem("Batch Delete...")
        batchDeleteItem.addActionListener {
            deleteHandler.showBatchDeleteDialog(tag)
        }
        menu.add(batchDeleteItem)

        if (targetTag != null) {
            val label = if (targetTag.name != "GridColumn") "Control" else "Column"
            val deleteTagItem = JMenuItem("Delete Current $label")
            deleteTagItem.icon = AllIcons.Actions.GC
            deleteTagItem.addActionListener {
                deleteHandler.deleteTagsWithCascade(listOf(targetTag), cascade = false)
            }
            menu.add(deleteTagItem)
            
            val deleteCascadeItem = JMenuItem("Delete Current $label with Cascade")
            deleteCascadeItem.icon = AllIcons.Actions.GC
            deleteCascadeItem.addActionListener {
                deleteHandler.deleteTagsWithCascade(listOf(targetTag), cascade = true)
            }
            menu.add(deleteCascadeItem)
        } else {
            val deleteItem = JMenuItem("Delete Container")
            deleteItem.icon = AllIcons.Actions.GC
            deleteItem.addActionListener {
                deleteHandler.deleteTagsWithCascade(listOf(tag), cascade = false)
            }
            menu.add(deleteItem)
            
            val deleteCascadeItem = JMenuItem("Delete Container with Cascade")
            deleteCascadeItem.icon = AllIcons.Actions.GC
            deleteCascadeItem.addActionListener {
                deleteHandler.deleteTagsWithCascade(listOf(tag), cascade = true)
            }
            menu.add(deleteCascadeItem)
        }

        menu.show(component, x, y)
    }
    
    // Helper search because tag.findFirstSubTag is sometimes not enough if we need recursive? 
    // No, standard subtag is fine for this structure.
    internal fun findChildTag(parent: XmlTag, name: String): XmlTag? {
        var result: XmlTag? = null
        ApplicationManager.getApplication().runReadAction {
            if (parent.isValid) {
                result = parent.findFirstSubTag(name)
            }
        }
        return result
    }
    
    private fun createPlaceholder(x: Int, y: Int, parentTag: XmlTag? = null): JComponent {
        val panel = JPanel()
        panel.background = JBColor(Color(250, 250, 250), Color(43, 43, 43))
        panel.border = BorderFactory.createDashedBorder(JBColor.border(), 1.0f, 3.0f, 3.0f, true)
        panel.toolTipText = "Empty Cell ($x, $y)"
        panel.name = "Placeholder:$x,$y" 

        panel.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) { handlePopup(e) }
            override fun mouseReleased(e: MouseEvent) { handlePopup(e) }

            private fun handlePopup(e: MouseEvent) {
                if (e.isPopupTrigger && parentTag != null) {
                    val p = SwingUtilities.convertPoint(e.component, e.point, tagToComponent[parentTag] ?: e.component)
                    showContainerContextMenu(tagToComponent[parentTag] ?: e.component as JComponent, parentTag, p.x, p.y)
                }
            }
        })

        return panel
    }

    // ===================== Delete Feature =====================
    
    private fun showLeafDeleteMenu(e: MouseEvent, tag: XmlTag) {
        deleteHandler.showLeafDeleteMenu(e, tag)
    }

}

    

