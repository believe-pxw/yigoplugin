package com.github.believepxw.yigo.example;

import com.github.believepxw.yigo.example.psi.MyLanguageTypes;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

public class MyLanguageSyntaxHighlighter extends SyntaxHighlighterBase {

    // 定义各种元素的文本属性键 (TextAttributesKey)
    public static final TextAttributesKey KEYWORD =
            createTextAttributesKey("MYLANG_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey IDENTIFIER =
            createTextAttributesKey("MYLANG_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER);
    public static final TextAttributesKey STRING =
            createTextAttributesKey("MYLANG_STRING", DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey OPERATOR =
            createTextAttributesKey("MYLANG_OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN);
    public static final TextAttributesKey PARENTHESES =
            createTextAttributesKey("MYLANG_PARENTHESES", DefaultLanguageHighlighterColors.PARENTHESES);
    public static final TextAttributesKey SEMICOLON_ATTR =
            createTextAttributesKey("MYLANG_SEMICOLON", DefaultLanguageHighlighterColors.SEMICOLON);
    // ... 其他你需要的类型

    public static final TextAttributesKey BAD_CHARACTER =
            createTextAttributesKey("MYLANG_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER);


    private static final TextAttributesKey[] KEYWORD_KEYS = new TextAttributesKey[]{KEYWORD};
    private static final TextAttributesKey[] IDENTIFIER_KEYS = new TextAttributesKey[]{IDENTIFIER};
    private static final TextAttributesKey[] STRING_KEYS = new TextAttributesKey[]{STRING};
    private static final TextAttributesKey[] OPERATOR_KEYS = new TextAttributesKey[]{OPERATOR};
    private static final TextAttributesKey[] PARENTHESES_KEYS = new TextAttributesKey[]{PARENTHESES};
    private static final TextAttributesKey[] SEMICOLON_KEYS = new TextAttributesKey[]{SEMICOLON_ATTR};
    private static final TextAttributesKey[] BAD_CHAR_KEYS = new TextAttributesKey[]{BAD_CHARACTER};
    private static final TextAttributesKey[] EMPTY_KEYS = new TextAttributesKey[0];

    @NotNull
    @Override
    public Lexer getHighlightingLexer() {
        return new MyLanguageLexerAdapter();
    }

    @NotNull
    @Override
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        if (tokenType.equals(MyLanguageTypes.IF_KEYWORD) ||
                tokenType.equals(MyLanguageTypes.ELSE_KEYWORD) ||
                tokenType.equals(MyLanguageTypes.WHILE_KEYWORD) ||
                tokenType.equals(MyLanguageTypes.VAR_KEYWORD) ||
                tokenType.equals(MyLanguageTypes.PARENT_KEYWORD)) {
            return KEYWORD_KEYS;
        } else if (tokenType.equals(MyLanguageTypes.IDENTIFIER) ||
                tokenType.equals(MyLanguageTypes.MACRO_IDENTIFIER) ||
                tokenType.equals(MyLanguageTypes.JAVA_PATH_IDENTIFIER)) {
            return IDENTIFIER_KEYS;
        } else if (tokenType.equals(MyLanguageTypes.SINGLE_QUOTED_STRING) ||
                tokenType.equals(MyLanguageTypes.DOUBLE_QUOTED_STRING) ||
                tokenType.equals(MyLanguageTypes.BRACE_QUOTED_STRING)) {
            return STRING_KEYS;
        } else if (tokenType.equals(MyLanguageTypes.PLUS) ||
                tokenType.equals(MyLanguageTypes.MINUS) ||
                tokenType.equals(MyLanguageTypes.MUL) ||
                tokenType.equals(MyLanguageTypes.DIV) ||
                tokenType.equals(MyLanguageTypes.AMPERSAND) ||
                tokenType.equals(MyLanguageTypes.AND_OP) ||
                tokenType.equals(MyLanguageTypes.OR_OP) ||
                tokenType.equals(MyLanguageTypes.DOT)) {
            return OPERATOR_KEYS;
        } else if (tokenType.equals(MyLanguageTypes.LPAREN) ||
                tokenType.equals(MyLanguageTypes.RPAREN)) {
            return PARENTHESES_KEYS;
        } else if (tokenType.equals(MyLanguageTypes.SEMICOLON)) {
            return SEMICOLON_KEYS;
        } else if (tokenType.equals(TokenType.BAD_CHARACTER)) {
            return BAD_CHAR_KEYS;
        } else {
            return EMPTY_KEYS; // 其他 token 使用默认颜色
        }
    }
}