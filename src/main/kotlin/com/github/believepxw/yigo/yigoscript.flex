%%
%public
%class YigoLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

%{
  import com.intellij.psi.TokenType;
  import your.dsl.language.psi.YigoTypes;
%}

// ==== Token Definitions ====
WHITESPACE     = [ \t\r\n]+
IDENTIFIER     = [a-zA-Z_][a-zA-Z0-9_]*
FULL_IDENT     = {IDENTIFIER}("."{IDENTIFIER})+
QUOTED_STRING1 = \'([^\']|\\\')*\'         // 'abc'
QUOTED_STRING2 = \"([^\"]|\\\")*\"         // "abc"
BRACED_STRING  = \{([^}]|\\\})*\}          // {abc}

// ==== Digit (if needed) ====
DIGIT          = [0-9]+

%%
<YYINITIAL> {
  // --- Whitespace ---
  {WHITESPACE}         { return TokenType.WHITE_SPACE; }

  // --- Keywords ---
  "var"                { return YigoTypes.VAR; }
  "parent"             { return YigoTypes.PARENT; }

  // --- Macro call ---
  "Macro_"{IDENTIFIER} { return YigoTypes.MACRO; }

  // --- String literals ---
  {QUOTED_STRING1}     { return YigoTypes.STRING; }
  {QUOTED_STRING2}     { return YigoTypes.STRING; }
  {BRACED_STRING}      { return YigoTypes.STRING; }

  // --- Fully qualified Java / parent call ---
  {FULL_IDENT}         { return YigoTypes.FULL_IDENT; }

  // --- Identifier (variable, function) ---
  {IDENTIFIER}         { return YigoTypes.IDENTIFIER; }

  // --- Numbers ---
  {DIGIT}              { return YigoTypes.NUMBER; }

  // --- Operators ---
  "&"                  { return YigoTypes.AMP; }
  "+"                  { return YigoTypes.PLUS; }
  "-"                  { return YigoTypes.MINUS; }
  "*"                  { return YigoTypes.MUL; }
  "/"                  { return YigoTypes.DIV; }

  // --- Symbols ---
  "("                  { return YigoTypes.LPAREN; }
  ")"                  { return YigoTypes.RPAREN; }
  ","                  { return YigoTypes.COMMA; }
  ";"                  { return YigoTypes.SEMICOLON; }

  // --- Error fallback ---
  .                    { return TokenType.BAD_CHARACTER; }
}
