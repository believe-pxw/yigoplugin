package example.editor;

import com.intellij.lang.BracePair;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import example.psi.MyLanguageTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * MyLanguage 括号匹配器
 * 负责匹配语言中的各种括号对，包括：
 * - 圆括号 ()：用于函数调用、表达式分组、条件语句
 * - 花括号 {}：用于代码块、对象字面量、参数数组
 */
public class MyLanguageBraceMatcher implements PairedBraceMatcher {

    /**
     * 定义所有需要匹配的括号对
     */
    private static final BracePair[] PAIRS = new BracePair[]{
            // 圆括号：用于函数调用、表达式分组、if/while条件等
            new BracePair(MyLanguageTypes.LPAREN, MyLanguageTypes.RPAREN, false),

            // 花括号：用于代码块、对象字面量、回调函数等
            new BracePair(MyLanguageTypes.LBRACE, MyLanguageTypes.RBRACE, true)
    };

    @Override
    public BracePair @NotNull [] getPairs() {
        return PAIRS;
    }

    /**
     * 确定是否应该在输入左括号时自动插入匹配的右括号
     *
     * @param lbraceType 左括号类型
     * @param contextType 当前位置的迭代器
     * @return true 如果应该配对插入右括号
     */
    @Override
    public boolean isPairedBracesAllowedBeforeType(@NotNull IElementType lbraceType,
                                                   @Nullable IElementType contextType) {
        // 对于圆括号，在大多数情况下都允许自动配对
        if (lbraceType == MyLanguageTypes.LPAREN) {
            return contextType == null ||
                    contextType == MyLanguageTypes.SEMICOLON ||
                    contextType == MyLanguageTypes.COMMA ||
                    contextType == MyLanguageTypes.RPAREN ||
                    contextType == MyLanguageTypes.RBRACE ||
                    contextType == MyLanguageTypes.COLON ||
                    contextType == MyLanguageTypes.EQ ||
                    contextType == MyLanguageTypes.AND_OP ||
                    contextType == MyLanguageTypes.OR_OP ||
                    contextType == MyLanguageTypes.PLUS ||
                    contextType == MyLanguageTypes.MINUS ||
                    contextType == MyLanguageTypes.MUL ||
                    contextType == MyLanguageTypes.DIV ||
                    contextType == MyLanguageTypes.AMPERSAND ||
                    contextType == MyLanguageTypes.LESS ||
                    contextType == MyLanguageTypes.GREATER ||
                    contextType == MyLanguageTypes.EQUAL_EQUAL ||
                    contextType == MyLanguageTypes.NOT_EQUAL ||
                    contextType == MyLanguageTypes.LESS_EQUAL ||
                    contextType == MyLanguageTypes.GREATER_EQUAL ||
                    contextType == MyLanguageTypes.NOT_EQUAL_ALT;
        }

        // 对于花括号，主要在语句结束、运算符后等位置允许
        if (lbraceType == MyLanguageTypes.LBRACE) {
            return contextType == null ||
                    contextType == MyLanguageTypes.SEMICOLON ||
                    contextType == MyLanguageTypes.COMMA ||
                    contextType == MyLanguageTypes.RPAREN ||
                    contextType == MyLanguageTypes.RBRACE ||
                    contextType == MyLanguageTypes.COLON ||
                    contextType == MyLanguageTypes.EQ ||
                    // 在MyLanguage中，花括号用于多种结构：
                    // 1. 代码块 (if/while语句后)
                    // 2. 对象字面量 (作为函数参数)
                    // 3. 参数数组 (ConfirmMsg的参数格式)
                    // 4. 回调函数对象
                    contextType == MyLanguageTypes.IF_KEYWORD ||
                    contextType == MyLanguageTypes.ELSE_KEYWORD ||
                    contextType == MyLanguageTypes.WHILE_KEYWORD;
        }

        return false;
    }

    /**
     * 返回拥有指定位置开放结构大括号的代码构造的起始偏移量
     * 例如，如果开放大括号属于'if'语句，则返回'if'语句的起始偏移量
     *
     * @param file 执行括号匹配的文件
     * @param openingBraceOffset 开放结构大括号的偏移量
     * @return 对应代码构造的偏移量，如果未定义则返回相同偏移量
     */
    @Override
    public int getCodeConstructStart(final PsiFile file, int openingBraceOffset) {
        // 获取开放大括号位置的PSI元素
        com.intellij.psi.PsiElement element = file.findElementAt(openingBraceOffset);
        if (element == null) {
            return openingBraceOffset;
        }

        // 向上查找父元素，寻找代码构造的起始位置
        com.intellij.psi.PsiElement parent = element.getParent();
        while (parent != null) {
            // 检查是否是if语句
            if (parent.getNode() != null &&
                    parent.getNode().getElementType() == MyLanguageTypes.IF_STATEMENT) {
                return parent.getTextRange().getStartOffset();
            }

            // 检查是否是while语句
            if (parent.getNode() != null &&
                    parent.getNode().getElementType() == MyLanguageTypes.WHILE_STATEMENT) {
                return parent.getTextRange().getStartOffset();
            }

            // 检查是否是块语句
            if (parent.getNode() != null &&
                    parent.getNode().getElementType() == MyLanguageTypes.BLOCK_STATEMENT) {
                // 对于块语句，继续向上查找控制结构
                com.intellij.psi.PsiElement blockParent = parent.getParent();
                if (blockParent != null && blockParent.getNode() != null) {
                    com.intellij.lang.ASTNode blockParentNode = blockParent.getNode();
                    if (blockParentNode.getElementType() == MyLanguageTypes.IF_STATEMENT ||
                            blockParentNode.getElementType() == MyLanguageTypes.WHILE_STATEMENT) {
                        return blockParent.getTextRange().getStartOffset();
                    }
                }
                return parent.getTextRange().getStartOffset();
            }

            // 检查是否是函数调用中的回调对象或对象字面量
            if (parent.getNode() != null &&
                    (parent.getNode().getElementType() == MyLanguageTypes.CALLBACK_OBJECT ||
                            parent.getNode().getElementType() == MyLanguageTypes.OBJECT_LITERAL)) {
                return parent.getTextRange().getStartOffset();
            }

            parent = parent.getParent();
        }

        // 如果没有找到特定的代码构造，返回原始偏移量
        return openingBraceOffset;
    }
}