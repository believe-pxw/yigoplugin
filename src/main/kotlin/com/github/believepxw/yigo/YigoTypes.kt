package com.github.believepxw.yigo

import com.intellij.psi.tree.IElementType

object YigoTypes {
    val VAR = IElementType("VAR", YigoLanguage)
    val PARENT = IElementType("PARENT", YigoLanguage)
    val MACRO = IElementType("MACRO", YigoLanguage)

    val STRING = IElementType("STRING", YigoLanguage)
    val IDENTIFIER = IElementType("IDENTIFIER", YigoLanguage)
    val FULL_IDENT = IElementType("FULL_IDENT", YigoLanguage)

    val NUMBER = IElementType("NUMBER", YigoLanguage)

    val AMP = IElementType("AMP", YigoLanguage)
    val PLUS = IElementType("PLUS", YigoLanguage)
    val MINUS = IElementType("MINUS", YigoLanguage)
    val MUL = IElementType("MUL", YigoLanguage)
    val DIV = IElementType("DIV", YigoLanguage)

    val LPAREN = IElementType("LPAREN", YigoLanguage)
    val RPAREN = IElementType("RPAREN", YigoLanguage)
    val COMMA = IElementType("COMMA", YigoLanguage)
    val SEMICOLON = IElementType("SEMICOLON", YigoLanguage)
}
