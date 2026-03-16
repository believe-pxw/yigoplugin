package com.github.believepxw.yigo.tool

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlTag
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.event.*
import javax.swing.*

class YigoSearchHandler(private val panel: YigoLayoutPanel) {

    inner class ScopeSearchDialog(private val scopeTag: XmlTag) : JDialog(SwingUtilities.getWindowAncestor(panel), "Search in ${panel.getTitle(scopeTag)}", Dialog.ModalityType.MODELESS) {
        private val searchField = SearchTextField()
        private val prevButton = JButton("Prev")
        private val nextButton = JButton("Next")
        private val findAllButton = JButton("Find All")
        private val countLabel = JLabel("0/0")
        private val resultsList = com.intellij.ui.components.JBList<Pair<XmlTag, JComponent>>()
        private val resultsScrollPane = JBScrollPane(resultsList)

        private var searchMatches = listOf<Pair<XmlTag, JComponent>>()
        private var currentMatchIndex = -1

        init {
            layout = BorderLayout()
            val topPanel = JPanel(BorderLayout())
            topPanel.border = JBUI.Borders.empty(5)

            val controls = JPanel(FlowLayout(FlowLayout.RIGHT, 5, 0))
            prevButton.margin = Insets(0, 5, 0, 5)
            nextButton.margin = Insets(0, 5, 0, 5)
            findAllButton.margin = Insets(0, 5, 0, 5)

            controls.add(countLabel)
            controls.add(prevButton)
            controls.add(nextButton)
            controls.add(findAllButton)

            topPanel.add(searchField, BorderLayout.CENTER)
            topPanel.add(controls, BorderLayout.EAST)

            add(topPanel, BorderLayout.NORTH)

            resultsScrollPane.isVisible = false
            resultsScrollPane.preferredSize = Dimension(400, 200)
            add(resultsScrollPane, BorderLayout.CENTER)

            // Set up renderer for the list
            resultsList.cellRenderer = object : DefaultListCellRenderer() {
                override fun getListCellRendererComponent(list: JList<*>, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {
                    val comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                    if (value is Pair<*, *>) {
                        val tag = value.first as? XmlTag
                        if (tag != null) {
                            val key = tag.getAttributeValue("Key") ?: ""
                            val caption = tag.getAttributeValue("Caption") ?: ""
                            val parentTag = panel.findParentGridTag(tag) ?: tag.parentTag
                            val parentKey = parentTag?.getAttributeValue("Key") ?: parentTag?.name ?: ""
                            val parentCaption = parentTag?.getAttributeValue("Caption") ?: ""

                            val itemText = "[$key] $caption"
                            val contextText = " in [$parentKey] $parentCaption"
                            text = "$itemText - $contextText"
                            icon = panel.getIconForTag(tag)
                        }
                    }
                    return comp
                }
            }

            // Single-click navigation for list items
            resultsList.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    val index = resultsList.locationToIndex(e.point)
                    if (index >= 0 && index < searchMatches.size) {
                        currentMatchIndex = index
                        updateSearchUI(true)
                    }
                }
            })

            // Search actions
            searchField.addKeyboardListener(object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent) {
                    if (e.keyCode == KeyEvent.VK_ENTER) {
                        runSearch()
                        if (e.isShiftDown) moveSelection(-1) else moveSelection(1)
                    } else if (e.keyCode == KeyEvent.VK_ESCAPE) {
                        clearHighlight()
                        dispose()
                    }
                }
            })

            prevButton.addActionListener {
                runSearch()
                moveSelection(-1)
            }
            nextButton.addActionListener {
                runSearch()
                moveSelection(1)
            }
            findAllButton.addActionListener {
                runSearch()
                showAllResults()
            }

            pack()
            isAlwaysOnTop = true
            setLocationRelativeTo(panel)

            addWindowListener(object : WindowAdapter() {
                override fun windowClosing(e: WindowEvent?) {
                    clearHighlight()
                }
            })
        }

        fun showDialog() {
            isVisible = true
            searchField.requestFocusInWindow()
        }

        private fun runSearch() {
            val text = searchField.text.trim().lowercase()
            if (text.isEmpty()) {
                searchMatches = emptyList()
                currentMatchIndex = -1
                updateSearchUI(false)
                return
            }

            ApplicationManager.getApplication().runReadAction {
                val validComponents = panel.tagToComponent.entries.filter { it.key.isValid }
                searchMatches = validComponents.filter { (tag, _) ->
                    PsiTreeUtil.isAncestor(scopeTag, tag, false) && (
                            (tag.getAttributeValue("Key")?.lowercase()?.contains(text) == true) ||
                                    (tag.getAttributeValue("Caption")?.lowercase()?.contains(text) == true)
                            )
                }.map { it.toPair() }.sortedBy { it.second.y }
            }

            if (searchMatches.isEmpty()) {
                currentMatchIndex = -1
            } else if (currentMatchIndex < 0 || currentMatchIndex >= searchMatches.size) {
                currentMatchIndex = 0
            }
        }

        private fun moveSelection(direction: Int) {
            if (searchMatches.isEmpty()) return
            currentMatchIndex = (currentMatchIndex + direction).mod(searchMatches.size)
            updateSearchUI(true)
        }

        private fun showAllResults() {
            if (searchMatches.isNotEmpty()) {
                val model = DefaultListModel<Pair<XmlTag, JComponent>>()
                searchMatches.forEach { model.addElement(it) }
                resultsList.model = model
                resultsScrollPane.isVisible = true
                pack() // resize to fit list
            } else {
                resultsScrollPane.isVisible = false
                pack()
            }
        }

        private fun updateSearchUI(highlight: Boolean) {
            if (searchMatches.isEmpty()) {
                countLabel.text = "0/0"
                countLabel.foreground = Color.RED
                prevButton.isEnabled = false
                nextButton.isEnabled = false
                clearHighlight()
            } else {
                countLabel.text = "${currentMatchIndex + 1}/${searchMatches.size}"
                countLabel.foreground = Color.BLACK
                prevButton.isEnabled = true
                nextButton.isEnabled = true

                if (highlight) {
                    clearHighlight()
                    val (tag, comp) = searchMatches[currentMatchIndex]
                    panel.setHighlightBorder(comp, Color.MAGENTA, 3)
                    panel.ensureComponentVisible(comp)
                    panel.lastSearchHighlight = comp

                    val currentEditor = FileEditorManager.getInstance(panel.project).selectedTextEditor
                    val currentPsi = if (currentEditor != null) PsiDocumentManager.getInstance(panel.project).getPsiFile(currentEditor.document) else null

                    if (currentPsi != null && tag.containingFile == currentPsi) {
                        panel.navigateToTag(tag, false)
                    }
                }
            }
        }

        private fun clearHighlight() {
            if (panel.lastSearchHighlight != null) {
                panel.restoreBorder(panel.lastSearchHighlight!!)
                panel.lastSearchHighlight = null
            }
        }
    }
}
