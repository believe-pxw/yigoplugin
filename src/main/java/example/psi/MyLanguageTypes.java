// This is a generated file. Not intended for manual editing.
package example.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import example.psi.impl.*;

public interface MyLanguageTypes {

  IElementType ARGUMENT_LIST = new MyLanguageElementType("ARGUMENT_LIST");
  IElementType BLOCK_STATEMENT = new MyLanguageElementType("BLOCK_STATEMENT");
  IElementType BOOLEAN_CONSTANT = new MyLanguageElementType("BOOLEAN_CONSTANT");
  IElementType CALLBACK_KEY = new MyLanguageElementType("CALLBACK_KEY");
  IElementType CALLBACK_OBJECT = new MyLanguageElementType("CALLBACK_OBJECT");
  IElementType CALLBACK_PROPERTY = new MyLanguageElementType("CALLBACK_PROPERTY");
  IElementType CODE_BLOCK_LITERAL = new MyLanguageElementType("CODE_BLOCK_LITERAL");
  IElementType COMMENT = new MyLanguageElementType("COMMENT");
  IElementType CONFIRM_MSG_ARGS = new MyLanguageElementType("CONFIRM_MSG_ARGS");
  IElementType CONFIRM_MSG_CALL = new MyLanguageElementType("CONFIRM_MSG_CALL");
  IElementType CONSTANT = new MyLanguageElementType("CONSTANT");
  IElementType DOUBLE_BRACE_EXPRESSION = new MyLanguageElementType("DOUBLE_BRACE_EXPRESSION");
  IElementType EXPRESSION = new MyLanguageElementType("EXPRESSION");
  IElementType EXPRESSION_SEQUENCE = new MyLanguageElementType("EXPRESSION_SEQUENCE");
  IElementType EXPRESSION_STATEMENT = new MyLanguageElementType("EXPRESSION_STATEMENT");
  IElementType FUNCTION_CALL = new MyLanguageElementType("FUNCTION_CALL");
  IElementType IF_STATEMENT = new MyLanguageElementType("IF_STATEMENT");
  IElementType IIF_FUNCTION_CALL = new MyLanguageElementType("IIF_FUNCTION_CALL");
  IElementType JAVA_METHOD_CALL = new MyLanguageElementType("JAVA_METHOD_CALL");
  IElementType MACRO_CALL_EXPRESSION = new MyLanguageElementType("MACRO_CALL_EXPRESSION");
  IElementType MESSAGE_PARAMS_EXPRESSION = new MyLanguageElementType("MESSAGE_PARAMS_EXPRESSION");
  IElementType NUMERIC_CONSTANT = new MyLanguageElementType("NUMERIC_CONSTANT");
  IElementType OBJECT_LITERAL = new MyLanguageElementType("OBJECT_LITERAL");
  IElementType OBJECT_LITERAL_CONTENT = new MyLanguageElementType("OBJECT_LITERAL_CONTENT");
  IElementType OBJECT_PROPERTY = new MyLanguageElementType("OBJECT_PROPERTY");
  IElementType PARAMETER_ARRAY = new MyLanguageElementType("PARAMETER_ARRAY");
  IElementType PRIMARY_EXPRESSION = new MyLanguageElementType("PRIMARY_EXPRESSION");
  IElementType REGULAR_FUNCTION_CALL = new MyLanguageElementType("REGULAR_FUNCTION_CALL");
  IElementType RETURN_STATEMENT = new MyLanguageElementType("RETURN_STATEMENT");
  IElementType STATEMENT = new MyLanguageElementType("STATEMENT");
  IElementType STATEMENT_BLOCK = new MyLanguageElementType("STATEMENT_BLOCK");
  IElementType VARIABLE_ASSIGNMENT = new MyLanguageElementType("VARIABLE_ASSIGNMENT");
  IElementType VARIABLE_DECLARATION = new MyLanguageElementType("VARIABLE_DECLARATION");
  IElementType VARIABLE_REFERENCE = new MyLanguageElementType("VARIABLE_REFERENCE");
  IElementType WHILE_STATEMENT = new MyLanguageElementType("WHILE_STATEMENT");

  IElementType AMPERSAND = new MyLanguageTokenType("&");
  IElementType AMP_ENTITY = new MyLanguageTokenType("&amp;");
  IElementType AND_OP = new MyLanguageTokenType("&&");
  IElementType AND_OP_ENTITY = new MyLanguageTokenType("&amp;&amp;");
  IElementType BLOCK_COMMENT = new MyLanguageTokenType("BLOCK_COMMENT");
  IElementType COLON = new MyLanguageTokenType(":");
  IElementType COMMA = new MyLanguageTokenType(",");
  IElementType CONFIRM_MSG = new MyLanguageTokenType("ConfirmMsg");
  IElementType CONTAINER_KEYWORD = new MyLanguageTokenType("CONTAINER_KEYWORD");
  IElementType DECIMAL_NUMBER = new MyLanguageTokenType("DECIMAL_NUMBER");
  IElementType DIV = new MyLanguageTokenType("/");
  IElementType DOT = new MyLanguageTokenType(".");
  IElementType DOUBLE_QUOTED_STRING = new MyLanguageTokenType("DOUBLE_QUOTED_STRING");
  IElementType ELSE_KEYWORD = new MyLanguageTokenType("else");
  IElementType EQ = new MyLanguageTokenType("=");
  IElementType EQUAL_EQUAL = new MyLanguageTokenType("==");
  IElementType FALSE_KEYWORD = new MyLanguageTokenType("false");
  IElementType GREATER = new MyLanguageTokenType(">");
  IElementType GREATER_EQUAL = new MyLanguageTokenType(">=");
  IElementType GT_ENTITY = new MyLanguageTokenType("&gt;");
  IElementType IDENTIFIER = new MyLanguageTokenType("IDENTIFIER");
  IElementType IF_KEYWORD = new MyLanguageTokenType("if");
  IElementType IIF_KEYWORD = new MyLanguageTokenType("IIF");
  IElementType INTEGER_NUMBER = new MyLanguageTokenType("INTEGER_NUMBER");
  IElementType JAVA_PATH_IDENTIFIER = new MyLanguageTokenType("JAVA_PATH_IDENTIFIER");
  IElementType LBRACE = new MyLanguageTokenType("{");
  IElementType LESS = new MyLanguageTokenType("<");
  IElementType LESS_EQUAL = new MyLanguageTokenType("<=");
  IElementType LINE_COMMENT = new MyLanguageTokenType("LINE_COMMENT");
  IElementType LPAREN = new MyLanguageTokenType("(");
  IElementType LT_ENTITY = new MyLanguageTokenType("&lt;");
  IElementType MACRO_IDENTIFIER = new MyLanguageTokenType("MACRO_IDENTIFIER");
  IElementType MINUS = new MyLanguageTokenType("-");
  IElementType MUL = new MyLanguageTokenType("*");
  IElementType NOT_EQUAL = new MyLanguageTokenType("!=");
  IElementType NOT_EQUAL_ALT = new MyLanguageTokenType("<>");
  IElementType NOT_OP = new MyLanguageTokenType("!");
  IElementType NUMBER = new MyLanguageTokenType("NUMBER");
  IElementType OR_OP = new MyLanguageTokenType("||");
  IElementType PARENT_KEYWORD = new MyLanguageTokenType("PARENT_KEYWORD");
  IElementType PLUS = new MyLanguageTokenType("+");
  IElementType RBRACE = new MyLanguageTokenType("}");
  IElementType RETURN_KEYWORD = new MyLanguageTokenType("return");
  IElementType RPAREN = new MyLanguageTokenType(")");
  IElementType SEMICOLON = new MyLanguageTokenType(";");
  IElementType SINGLE_QUOTED_STRING = new MyLanguageTokenType("SINGLE_QUOTED_STRING");
  IElementType TRUE_KEYWORD = new MyLanguageTokenType("true");
  IElementType VAR_KEYWORD = new MyLanguageTokenType("var");
  IElementType WHILE_KEYWORD = new MyLanguageTokenType("while");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
      if (type == ARGUMENT_LIST) {
        return new MyLanguageArgumentListImpl(node);
      }
      else if (type == BLOCK_STATEMENT) {
        return new MyLanguageBlockStatementImpl(node);
      }
      else if (type == BOOLEAN_CONSTANT) {
        return new MyLanguageBooleanConstantImpl(node);
      }
      else if (type == CALLBACK_KEY) {
        return new MyLanguageCallbackKeyImpl(node);
      }
      else if (type == CALLBACK_OBJECT) {
        return new MyLanguageCallbackObjectImpl(node);
      }
      else if (type == CALLBACK_PROPERTY) {
        return new MyLanguageCallbackPropertyImpl(node);
      }
      else if (type == CODE_BLOCK_LITERAL) {
        return new MyLanguageCodeBlockLiteralImpl(node);
      }
      else if (type == COMMENT) {
        return new MyLanguageCommentImpl(node);
      }
      else if (type == CONFIRM_MSG_ARGS) {
        return new MyLanguageConfirmMsgArgsImpl(node);
      }
      else if (type == CONFIRM_MSG_CALL) {
        return new MyLanguageConfirmMsgCallImpl(node);
      }
      else if (type == CONSTANT) {
        return new MyLanguageConstantImpl(node);
      }
      else if (type == DOUBLE_BRACE_EXPRESSION) {
        return new MyLanguageDoubleBraceExpressionImpl(node);
      }
      else if (type == EXPRESSION) {
        return new MyLanguageExpressionImpl(node);
      }
      else if (type == EXPRESSION_SEQUENCE) {
        return new MyLanguageExpressionSequenceImpl(node);
      }
      else if (type == EXPRESSION_STATEMENT) {
        return new MyLanguageExpressionStatementImpl(node);
      }
      else if (type == FUNCTION_CALL) {
        return new MyLanguageFunctionCallImpl(node);
      }
      else if (type == IF_STATEMENT) {
        return new MyLanguageIfStatementImpl(node);
      }
      else if (type == IIF_FUNCTION_CALL) {
        return new MyLanguageIifFunctionCallImpl(node);
      }
      else if (type == JAVA_METHOD_CALL) {
        return new MyLanguageJavaMethodCallImpl(node);
      }
      else if (type == MACRO_CALL_EXPRESSION) {
        return new MyLanguageMacroCallExpressionImpl(node);
      }
      else if (type == MESSAGE_PARAMS_EXPRESSION) {
        return new MyLanguageMessageParamsExpressionImpl(node);
      }
      else if (type == NUMERIC_CONSTANT) {
        return new MyLanguageNumericConstantImpl(node);
      }
      else if (type == OBJECT_LITERAL) {
        return new MyLanguageObjectLiteralImpl(node);
      }
      else if (type == OBJECT_LITERAL_CONTENT) {
        return new MyLanguageObjectLiteralContentImpl(node);
      }
      else if (type == OBJECT_PROPERTY) {
        return new MyLanguageObjectPropertyImpl(node);
      }
      else if (type == PARAMETER_ARRAY) {
        return new MyLanguageParameterArrayImpl(node);
      }
      else if (type == PRIMARY_EXPRESSION) {
        return new MyLanguagePrimaryExpressionImpl(node);
      }
      else if (type == REGULAR_FUNCTION_CALL) {
        return new MyLanguageRegularFunctionCallImpl(node);
      }
      else if (type == RETURN_STATEMENT) {
        return new MyLanguageReturnStatementImpl(node);
      }
      else if (type == STATEMENT) {
        return new MyLanguageStatementImpl(node);
      }
      else if (type == STATEMENT_BLOCK) {
        return new MyLanguageStatementBlockImpl(node);
      }
      else if (type == VARIABLE_ASSIGNMENT) {
        return new MyLanguageVariableAssignmentImpl(node);
      }
      else if (type == VARIABLE_DECLARATION) {
        return new MyLanguageVariableDeclarationImpl(node);
      }
      else if (type == VARIABLE_REFERENCE) {
        return new MyLanguageVariableReferenceImpl(node);
      }
      else if (type == WHILE_STATEMENT) {
        return new MyLanguageWhileStatementImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
