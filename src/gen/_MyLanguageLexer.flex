package example.parser;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static example.psi.MyLanguageTypes.*;

%%

%{
  public _MyLanguageLexer() {
    this((java.io.Reader)null);
  }
%}

%public
%class _MyLanguageLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

EOL=\R
WHITE_SPACE=\s+

PARENT_KEYWORD=[pP]arent
CONTAINER_KEYWORD=[cC]ontainer
MACRO_IDENTIFIER=Macro_[a-zA-Z_][a-zA-Z0-9_]*
JAVA_PATH_IDENTIFIER=com*(\.[a-zA-Z_][a-zA-Z0-9_]*)+
IDENTIFIER=[a-zA-Z_][a-zA-Z0-9_]*
SINGLE_QUOTED_STRING='[^']*'
DOUBLE_QUOTED_STRING=\"[^\"]*\"
NUMBER=[0-9]+

%%
<YYINITIAL> {
  {WHITE_SPACE}                { return WHITE_SPACE; }

  "if"                         { return IF_KEYWORD; }
  "else"                       { return ELSE_KEYWORD; }
  "while"                      { return WHILE_KEYWORD; }
  "var"                        { return VAR_KEYWORD; }
  "true"                       { return TRUE_KEYWORD; }
  "false"                      { return FALSE_KEYWORD; }
  "IIF"                        { return IIF_KEYWORD; }
  "&amp;"                      { return AMP_ENTITY; }
  "&lt;"                       { return LT_ENTITY; }
  "&gt;"                       { return GT_ENTITY; }
  "&amp;&amp;"                 { return AND_OP_ENTITY; }
  "+"                          { return PLUS; }
  "-"                          { return MINUS; }
  "*"                          { return MUL; }
  "/"                          { return DIV; }
  "&"                          { return AMPERSAND; }
  "&&"                         { return AND_OP; }
  "||"                         { return OR_OP; }
  "."                          { return DOT; }
  "!"                          { return NOT_OP; }
  "="                          { return EQ; }
  "<="                         { return LESS_EQUAL; }
  ">="                         { return GREATER_EQUAL; }
  "=="                         { return EQUAL_EQUAL; }
  "!="                         { return NOT_EQUAL; }
  "<"                          { return LESS; }
  ">"                          { return GREATER; }
  "<>"                         { return NOT_EQUAL_ALT; }
  "("                          { return LPAREN; }
  ")"                          { return RPAREN; }
  ";"                          { return SEMICOLON; }
  ","                          { return COMMA; }
  "{"                          { return LBRACE; }
  "}"                          { return RBRACE; }

  {PARENT_KEYWORD}             { return PARENT_KEYWORD; }
  {CONTAINER_KEYWORD}          { return CONTAINER_KEYWORD; }
  {MACRO_IDENTIFIER}           { return MACRO_IDENTIFIER; }
  {JAVA_PATH_IDENTIFIER}       { return JAVA_PATH_IDENTIFIER; }
  {IDENTIFIER}                 { return IDENTIFIER; }
  {SINGLE_QUOTED_STRING}       { return SINGLE_QUOTED_STRING; }
  {DOUBLE_QUOTED_STRING}       { return DOUBLE_QUOTED_STRING; }
  {NUMBER}                     { return NUMBER; }

}

[^] { return BAD_CHARACTER; }
