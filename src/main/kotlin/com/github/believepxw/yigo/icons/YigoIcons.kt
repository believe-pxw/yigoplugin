package com.github.believepxw.yigo.icons

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object YigoIcons {
    @JvmField
    val Logo: Icon = IconLoader.getIcon("/icons/yigo.svg", YigoIcons::class.java)
    @JvmField
    val Trac: Icon = IconLoader.getIcon("/icons/trac.svg", YigoIcons::class.java)

    object Controls {
        @JvmField val CheckBox = IconLoader.getIcon("/icons/controls/checkbox.svg", YigoIcons::class.java)
        @JvmField val CheckListBox = IconLoader.getIcon("/icons/controls/checklistbox.svg", YigoIcons::class.java)
        @JvmField val RadioButton = IconLoader.getIcon("/icons/controls/radiobutton.svg", YigoIcons::class.java)
        @JvmField val ComboBox = IconLoader.getIcon("/icons/controls/combobox.svg", YigoIcons::class.java)
        @JvmField val DropdownButton = IconLoader.getIcon("/icons/controls/dropdownbutton.svg", YigoIcons::class.java)
        @JvmField val Dict = IconLoader.getIcon("/icons/controls/dict.svg", YigoIcons::class.java)
        @JvmField val DynamicDict = IconLoader.getIcon("/icons/controls/dynamicdict.svg", YigoIcons::class.java)
        @JvmField val TextEditor = IconLoader.getIcon("/icons/controls/texteditor.svg", YigoIcons::class.java)
        @JvmField val TextArea = IconLoader.getIcon("/icons/controls/textarea.svg", YigoIcons::class.java)
        @JvmField val RichEditor = IconLoader.getIcon("/icons/controls/richeditor.svg", YigoIcons::class.java)
        @JvmField val PasswordEditor = IconLoader.getIcon("/icons/controls/passwordeditor.svg", YigoIcons::class.java)
        @JvmField val NumberEditor = IconLoader.getIcon("/icons/controls/numbereditor.svg", YigoIcons::class.java)
        @JvmField val DatePicker = IconLoader.getIcon("/icons/controls/datepicker.svg", YigoIcons::class.java)
        @JvmField val UTCDatePicker = IconLoader.getIcon("/icons/controls/utcdatepicker.svg", YigoIcons::class.java)
        @JvmField val MonthPicker = IconLoader.getIcon("/icons/controls/monthpicker.svg", YigoIcons::class.java)
        @JvmField val TimePicker = IconLoader.getIcon("/icons/controls/timepicker.svg", YigoIcons::class.java)
        @JvmField val Button = IconLoader.getIcon("/icons/controls/button.svg", YigoIcons::class.java)
        @JvmField val TextButton = IconLoader.getIcon("/icons/controls/textbutton.svg", YigoIcons::class.java)
        @JvmField val Label = IconLoader.getIcon("/icons/controls/label.svg", YigoIcons::class.java)
        @JvmField val Image = IconLoader.getIcon("/icons/controls/image.svg", YigoIcons::class.java)
        @JvmField val IconControl = IconLoader.getIcon("/icons/controls/icon_control.svg", YigoIcons::class.java)
        @JvmField val HyperLink = IconLoader.getIcon("/icons/controls/hyperlink.svg", YigoIcons::class.java)
        @JvmField val Grid = IconLoader.getIcon("/icons/controls/grid.svg", YigoIcons::class.java)
    }
}
