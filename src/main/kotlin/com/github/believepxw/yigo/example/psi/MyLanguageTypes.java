// This is a generated file. Not intended for manual editing.
package com.github.believepxw.yigo.example.psi;

import com.github.believepxw.yigo.example.MyLanguageElementType;
import com.github.believepxw.yigo.example.MyLanguageTokenType;
import com.github.believepxw.yigo.example.psi.impl.MyLanguageArgumentListImpl;
import com.github.believepxw.yigo.example.psi.impl.MyLanguageConstantImpl;
import com.github.believepxw.yigo.example.psi.impl.MyLanguageExpressionImpl;
import com.github.believepxw.yigo.example.psi.impl.MyLanguageExpressionStatementImpl;
import com.github.believepxw.yigo.example.psi.impl.MyLanguageFunctionCallImpl;
import com.github.believepxw.yigo.example.psi.impl.MyLanguageIfStatementImpl;
import com.github.believepxw.yigo.example.psi.impl.MyLanguageJavaMethodCallImpl;
import com.github.believepxw.yigo.example.psi.impl.MyLanguageParentCallImpl;
import com.github.believepxw.yigo.example.psi.impl.MyLanguagePrimaryExpressionImpl;
import com.github.believepxw.yigo.example.psi.impl.MyLanguageVariableDeclarationImpl;
import com.github.believepxw.yigo.example.psi.impl.MyLanguageVariableReferenceImpl;
import com.github.believepxw.yigo.example.psi.impl.MyLanguageWhileStatementImpl;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import com.example.psi.impl.*;

public interface MyLanguageTypes {

  IElementType ARGUMENT_LIST = new MyLanguageElementType("ARGUMENT_LIST");
  IElementType CONSTANT = new MyLanguageElementType("CONSTANT");
  IElementType EXPRESSION = new MyLanguageElementType("EXPRESSION");
  IElementType EXPRESSION_STATEMENT = new MyLanguageElementType("EXPRESSION_STATEMENT");
  IElementType FUNCTION_CALL = new MyLanguageElementType("FUNCTION_CALL");
  IElementType IF_STATEMENT = new MyLanguageElementType("IF_STATEMENT");
  IElementType JAVA_METHOD_CALL = new MyLanguageElementType("JAVA_METHOD_CALL");
  IElementType PARENT_CALL = new MyLanguageElementType("PARENT_CALL");
  IElementType PRIMARY_EXPRESSION = new MyLanguageElementType("PRIMARY_EXPRESSION");
  IElementType VARIABLE_DECLARATION = new MyLanguageElementType("VARIABLE_DECLARATION");
  IElementType VARIABLE_REFERENCE = new MyLanguageElementType("VARIABLE_REFERENCE");
  IElementType WHILE_STATEMENT = new MyLanguageElementType("WHILE_STATEMENT");

  IElementType AMPERSAND = new MyLanguageTokenType("&");
  IElementType AND_OP = new MyLanguageTokenType("&&");
  IElementType BRACE_QUOTED_STRING = new MyLanguageTokenType("BRACE_QUOTED_STRING");
  IElementType COMMA = new MyLanguageTokenType(",");
  IElementType DIV = new MyLanguageTokenType("/");
  IElementType DOT = new MyLanguageTokenType(".");
  IElementType DOUBLE_QUOTED_STRING = new MyLanguageTokenType("DOUBLE_QUOTED_STRING");
  IElementType ELSE_KEYWORD = new MyLanguageTokenType("else");
  IElementType IDENTIFIER = new MyLanguageTokenType("IDENTIFIER");
  IElementType IF_KEYWORD = new MyLanguageTokenType("if");
  IElementType JAVA_PATH_IDENTIFIER = new MyLanguageTokenType("JAVA_PATH_IDENTIFIER");
  IElementType LPAREN = new MyLanguageTokenType("(");
  IElementType MACRO_IDENTIFIER = new MyLanguageTokenType("MACRO_IDENTIFIER");
  IElementType MINUS = new MyLanguageTokenType("-");
  IElementType MUL = new MyLanguageTokenType("*");
  IElementType OR_OP = new MyLanguageTokenType("||");
  IElementType PARENT_KEYWORD = new MyLanguageTokenType("parent");
  IElementType PLUS = new MyLanguageTokenType("+");
  IElementType RPAREN = new MyLanguageTokenType(")");
  IElementType SEMICOLON = new MyLanguageTokenType(";");
  IElementType SINGLE_QUOTED_STRING = new MyLanguageTokenType("SINGLE_QUOTED_STRING");
  IElementType VAR_KEYWORD = new MyLanguageTokenType("var");
  IElementType WHILE_KEYWORD = new MyLanguageTokenType("while");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
      if (type == ARGUMENT_LIST) {
        return new MyLanguageArgumentListImpl(node);
      }
      else if (type == CONSTANT) {
        return new MyLanguageConstantImpl(node);
      }
      else if (type == EXPRESSION) {
        return new MyLanguageExpressionImpl(node);
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
      else if (type == JAVA_METHOD_CALL) {
        return new MyLanguageJavaMethodCallImpl(node);
      }
      else if (type == PARENT_CALL) {
        return new MyLanguageParentCallImpl(node);
      }
      else if (type == PRIMARY_EXPRESSION) {
        return new MyLanguagePrimaryExpressionImpl(node);
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
