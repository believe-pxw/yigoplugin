package example.formatting;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.codeStyle.CodeStyleSettings;

import example.MyLanguage;
import example.psi.MyLanguageTypes;
import org.jetbrains.annotations.NotNull;

public class MyLanguageFormattingModelBuilder implements FormattingModelBuilder {

    @Override
    public @NotNull FormattingModel createModel(@NotNull FormattingContext formattingContext) {
        final CodeStyleSettings codeStyleSettings = formattingContext.getCodeStyleSettings();
        return FormattingModelProvider.createFormattingModelForPsiFile(
                formattingContext.getContainingFile(),
                new MyLanguageBlock(
                        formattingContext.getNode(),
                        Wrap.createWrap(WrapType.NONE, false),
                        Alignment.createAlignment(),
                        createSpaceBuilder(codeStyleSettings)
                ),
                codeStyleSettings
        );
    }

    private static SpacingBuilder createSpaceBuilder(CodeStyleSettings settings) {
        return new SpacingBuilder(settings, MyLanguage.INSTANCE)
                // 运算符周围的空格
                .around(MyLanguageTypes.EQ).spaces(1)
                .around(MyLanguageTypes.EQUAL_EQUAL).spaces(1)
                .around(MyLanguageTypes.NOT_EQUAL).spaces(1)
                .around(MyLanguageTypes.NOT_EQUAL_ALT).spaces(1)
                .around(MyLanguageTypes.LESS).spaces(1)
                .around(MyLanguageTypes.GREATER).spaces(1)
                .around(MyLanguageTypes.LESS_EQUAL).spaces(1)
                .around(MyLanguageTypes.GREATER_EQUAL).spaces(1)
                .around(MyLanguageTypes.AND_OP).spaces(1)
                .around(MyLanguageTypes.AND_OP_ENTITY).spaces(1)
                .around(MyLanguageTypes.OR_OP).spaces(1)
                .around(MyLanguageTypes.PLUS).spaces(1)
                .around(MyLanguageTypes.MINUS).spaces(1)
                .around(MyLanguageTypes.MUL).spaces(1)
                .around(MyLanguageTypes.DIV).spaces(1)
                .around(MyLanguageTypes.AMPERSAND).spaces(1)
                .around(MyLanguageTypes.AMP_ENTITY).spaces(1)

                // --- 標點符號間距 ---
                .before(MyLanguageTypes.COMMA).spaceIf(false)
                .after(MyLanguageTypes.COMMA).spaces(1)

                .before(MyLanguageTypes.COLON).spaceIf(false) // 冒號前無空格
                .after(MyLanguageTypes.COLON).spaces(1)      // 冒號後一個空格

                .after(MyLanguageTypes.LPAREN).none()   // ( 後沒空格
                .before(MyLanguageTypes.RPAREN).none()  // ) 前沒空格

                // 关键字后的空格
                .after(MyLanguageTypes.IF_KEYWORD).spaces(1)
                .after(MyLanguageTypes.ELSE_KEYWORD).spaces(1)
                .after(MyLanguageTypes.WHILE_KEYWORD).spaces(1)
                .after(MyLanguageTypes.VAR_KEYWORD).spaces(1)

                // --- 核心換行規則 ---
                .after(MyLanguageTypes.SEMICOLON).spacing(0, 0, 1, true, 0) // 分號後必須換行
                .between(MyLanguageTypes.RBRACE, MyLanguageTypes.ELSE_KEYWORD).spaces(1) // } else {
                .between(MyLanguageTypes.RPAREN, MyLanguageTypes.STATEMENT_BLOCK).spaces(1) // if (...) {

                // 在大括號內強制換行
                .after(MyLanguageTypes.LBRACE).spacing(0, 0, 1, true, 0)
                .before(MyLanguageTypes.RBRACE).spacing(0, 0, 1, true, 0)
                // 如果大括號是空的 {}，則不換行
                .between(MyLanguageTypes.LBRACE, MyLanguageTypes.RBRACE).spacing(0, 0, 0, false, 0);
    }
}

