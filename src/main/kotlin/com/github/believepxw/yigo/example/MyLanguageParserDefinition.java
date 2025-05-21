package com.github.believepxw.yigo.example;

import com.github.believepxw.yigo.example.parser.MyLanguageParser;
import com.github.believepxw.yigo.example.psi.MyLanguageTypes;
import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;

public class MyLanguageParserDefinition implements ParserDefinition{
    public static final TokenSet WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE);
    // 根据你的 .bnf 文件定义，你可能没有定义 COMMENT token
    // public static final TokenSet COMMENTS = TokenSet.create(MyLanguageTypes.COMMENT_TOKEN); // 如果有注释
    public static final TokenSet STRING_LITERALS = TokenSet.create(
            MyLanguageTypes.SINGLE_QUOTED_STRING,
            MyLanguageTypes.DOUBLE_QUOTED_STRING,
            MyLanguageTypes.BRACE_QUOTED_STRING
    ); // 示例，根据你的 tokens 定义

    // 这个 IFileElementType 需要和 plugin.xml 中 filetype 的 language 属性匹配
    public static final IFileElementType FILE = new IFileElementType(MyLanguage.INSTANCE);

    @Override
    public @NotNull Lexer createLexer(Project project) {
        return new MyLanguageLexerAdapter();
    }

    @Override
    public @NotNull PsiParser createParser(Project project) {
        return new MyLanguageParser(); // 使用生成的 Parser
    }

    @Override
    public @NotNull IFileElementType getFileNodeType() {
        return FILE;
    }

    @Override
    public @NotNull TokenSet getCommentTokens() {
        return TokenSet.EMPTY;
    }

    @Override
    public @NotNull TokenSet getStringLiteralElements() {
        return STRING_LITERALS; // 返回所有字符串字面量 Token 类型
    }

    @NotNull
    @Override
    public TokenSet getWhitespaceTokens() {
        return WHITE_SPACES; // 如果你的.flex文件定义了空白，这里返回
    }

    @Override
    public @NotNull PsiElement createElement(ASTNode node) {
        // 这是 PSI 工厂，由 Grammar-Kit 生成的 MyLanguageTypes.Factory.createElement(node) 通常会处理
        return MyLanguageTypes.Factory.createElement(node);
    }

    @Override
    public @NotNull PsiFile createFile(@NotNull FileViewProvider viewProvider) {
        return new MyLanguageFile(viewProvider); // PSI 文件的根节点
    }

    @Override
    public @NotNull SpaceRequirements spaceExistenceTypeBetweenTokens(ASTNode left, ASTNode right) {
        return SpaceRequirements.MAY; // 或者根据你的语言具体需求调整
    }
}
