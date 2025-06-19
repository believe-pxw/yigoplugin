// This is a generated file. Not intended for manual editing.
package example.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static example.psi.MyLanguageTypes.*;
import static com.intellij.lang.parser.GeneratedParserUtilBase.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class MyLanguageParser implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType t, PsiBuilder b) {
    parseLight(t, b);
    return b.getTreeBuilt();
  }

  public void parseLight(IElementType t, PsiBuilder b) {
    boolean r;
    b = adapt_builder_(t, b, this, null);
    Marker m = enter_section_(b, 0, _COLLAPSE_, null);
    r = parse_root_(t, b);
    exit_section_(b, 0, m, t, r, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType t, PsiBuilder b) {
    return parse_root_(t, b, 0);
  }

  static boolean parse_root_(IElementType t, PsiBuilder b, int l) {
    return root(b, l + 1);
  }

  /* ********************************************************** */
  // multiplicative_expression ((PLUS | MINUS | AMPERSAND | AMP_ENTITY) multiplicative_expression)*
  static boolean additive_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "additive_expression")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = multiplicative_expression(b, l + 1);
    r = r && additive_expression_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ((PLUS | MINUS | AMPERSAND | AMP_ENTITY) multiplicative_expression)*
  private static boolean additive_expression_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "additive_expression_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!additive_expression_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "additive_expression_1", c)) break;
    }
    return true;
  }

  // (PLUS | MINUS | AMPERSAND | AMP_ENTITY) multiplicative_expression
  private static boolean additive_expression_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "additive_expression_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = additive_expression_1_0_0(b, l + 1);
    r = r && multiplicative_expression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // PLUS | MINUS | AMPERSAND | AMP_ENTITY
  private static boolean additive_expression_1_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "additive_expression_1_0_0")) return false;
    boolean r;
    r = consumeToken(b, PLUS);
    if (!r) r = consumeToken(b, MINUS);
    if (!r) r = consumeToken(b, AMPERSAND);
    if (!r) r = consumeToken(b, AMP_ENTITY);
    return r;
  }

  /* ********************************************************** */
  // expression (COMMA expression)*
  public static boolean argument_list(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "argument_list")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, ARGUMENT_LIST, "<argument list>");
    r = expression(b, l + 1);
    r = r && argument_list_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // (COMMA expression)*
  private static boolean argument_list_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "argument_list_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!argument_list_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "argument_list_1", c)) break;
    }
    return true;
  }

  // COMMA expression
  private static boolean argument_list_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "argument_list_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && expression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // LBRACE statement* RBRACE COLON*
  public static boolean block_statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "block_statement")) return false;
    if (!nextTokenIs(b, LBRACE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LBRACE);
    r = r && block_statement_1(b, l + 1);
    r = r && consumeToken(b, RBRACE);
    r = r && block_statement_3(b, l + 1);
    exit_section_(b, m, BLOCK_STATEMENT, r);
    return r;
  }

  // statement*
  private static boolean block_statement_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "block_statement_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!statement(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "block_statement_1", c)) break;
    }
    return true;
  }

  // COLON*
  private static boolean block_statement_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "block_statement_3")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, COLON)) break;
      if (!empty_element_parsed_guard_(b, "block_statement_3", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // TRUE_KEYWORD | FALSE_KEYWORD
  public static boolean boolean_constant(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "boolean_constant")) return false;
    if (!nextTokenIs(b, "<boolean constant>", FALSE_KEYWORD, TRUE_KEYWORD)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, BOOLEAN_CONSTANT, "<boolean constant>");
    r = consumeToken(b, TRUE_KEYWORD);
    if (!r) r = consumeToken(b, FALSE_KEYWORD);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // SINGLE_QUOTED_STRING | DOUBLE_QUOTED_STRING |  // 字符串形式的键
  //   IDENTIFIER
  public static boolean callback_key(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "callback_key")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, CALLBACK_KEY, "<callback key>");
    r = consumeToken(b, SINGLE_QUOTED_STRING);
    if (!r) r = consumeToken(b, DOUBLE_QUOTED_STRING);
    if (!r) r = consumeToken(b, IDENTIFIER);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // LBRACE callback_property (COMMA callback_property)* RBRACE
  public static boolean callback_object(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "callback_object")) return false;
    if (!nextTokenIs(b, LBRACE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LBRACE);
    r = r && callback_property(b, l + 1);
    r = r && callback_object_2(b, l + 1);
    r = r && consumeToken(b, RBRACE);
    exit_section_(b, m, CALLBACK_OBJECT, r);
    return r;
  }

  // (COMMA callback_property)*
  private static boolean callback_object_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "callback_object_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!callback_object_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "callback_object_2", c)) break;
    }
    return true;
  }

  // COMMA callback_property
  private static boolean callback_object_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "callback_object_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && callback_property(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // callback_key COLON code_block_literal
  public static boolean callback_property(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "callback_property")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, CALLBACK_PROPERTY, "<callback property>");
    r = callback_key(b, l + 1);
    r = r && consumeToken(b, COLON);
    r = r && code_block_literal(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // LBRACE statement* RBRACE
  public static boolean code_block_literal(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "code_block_literal")) return false;
    if (!nextTokenIs(b, LBRACE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LBRACE);
    r = r && code_block_literal_1(b, l + 1);
    r = r && consumeToken(b, RBRACE);
    exit_section_(b, m, CODE_BLOCK_LITERAL, r);
    return r;
  }

  // statement*
  private static boolean code_block_literal_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "code_block_literal_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!statement(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "code_block_literal_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // additive_expression ((LESS_EQUAL | GREATER_EQUAL | EQUAL_EQUAL | NOT_EQUAL | LESS | GREATER | NOT_EQUAL_ALT | LT_ENTITY | GT_ENTITY) additive_expression)*
  static boolean comparison_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "comparison_expression")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = additive_expression(b, l + 1);
    r = r && comparison_expression_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ((LESS_EQUAL | GREATER_EQUAL | EQUAL_EQUAL | NOT_EQUAL | LESS | GREATER | NOT_EQUAL_ALT | LT_ENTITY | GT_ENTITY) additive_expression)*
  private static boolean comparison_expression_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "comparison_expression_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!comparison_expression_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "comparison_expression_1", c)) break;
    }
    return true;
  }

  // (LESS_EQUAL | GREATER_EQUAL | EQUAL_EQUAL | NOT_EQUAL | LESS | GREATER | NOT_EQUAL_ALT | LT_ENTITY | GT_ENTITY) additive_expression
  private static boolean comparison_expression_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "comparison_expression_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = comparison_expression_1_0_0(b, l + 1);
    r = r && additive_expression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // LESS_EQUAL | GREATER_EQUAL | EQUAL_EQUAL | NOT_EQUAL | LESS | GREATER | NOT_EQUAL_ALT | LT_ENTITY | GT_ENTITY
  private static boolean comparison_expression_1_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "comparison_expression_1_0_0")) return false;
    boolean r;
    r = consumeToken(b, LESS_EQUAL);
    if (!r) r = consumeToken(b, GREATER_EQUAL);
    if (!r) r = consumeToken(b, EQUAL_EQUAL);
    if (!r) r = consumeToken(b, NOT_EQUAL);
    if (!r) r = consumeToken(b, LESS);
    if (!r) r = consumeToken(b, GREATER);
    if (!r) r = consumeToken(b, NOT_EQUAL_ALT);
    if (!r) r = consumeToken(b, LT_ENTITY);
    if (!r) r = consumeToken(b, GT_ENTITY);
    return r;
  }

  /* ********************************************************** */
  // expression COMMA expression                                              // 必需：args[0]消息代码, args[1]消息文本
  //   (COMMA message_params_expression                                        // 可选：args[2]消息参数
  //     (COMMA expression                                                     // 可选：args[3]样式(OK,YES_NO,YES_NO_CANCEL)
  //       (COMMA callback_object)?                                            // 可选：args[4]回调函数对象
  //     )?
  //   )?
  public static boolean confirm_msg_args(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "confirm_msg_args")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, CONFIRM_MSG_ARGS, "<confirm msg args>");
    r = expression(b, l + 1);
    r = r && consumeToken(b, COMMA);
    r = r && expression(b, l + 1);
    r = r && confirm_msg_args_3(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // (COMMA message_params_expression                                        // 可选：args[2]消息参数
  //     (COMMA expression                                                     // 可选：args[3]样式(OK,YES_NO,YES_NO_CANCEL)
  //       (COMMA callback_object)?                                            // 可选：args[4]回调函数对象
  //     )?
  //   )?
  private static boolean confirm_msg_args_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "confirm_msg_args_3")) return false;
    confirm_msg_args_3_0(b, l + 1);
    return true;
  }

  // COMMA message_params_expression                                        // 可选：args[2]消息参数
  //     (COMMA expression                                                     // 可选：args[3]样式(OK,YES_NO,YES_NO_CANCEL)
  //       (COMMA callback_object)?                                            // 可选：args[4]回调函数对象
  //     )?
  private static boolean confirm_msg_args_3_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "confirm_msg_args_3_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && message_params_expression(b, l + 1);
    r = r && confirm_msg_args_3_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (COMMA expression                                                     // 可选：args[3]样式(OK,YES_NO,YES_NO_CANCEL)
  //       (COMMA callback_object)?                                            // 可选：args[4]回调函数对象
  //     )?
  private static boolean confirm_msg_args_3_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "confirm_msg_args_3_0_2")) return false;
    confirm_msg_args_3_0_2_0(b, l + 1);
    return true;
  }

  // COMMA expression                                                     // 可选：args[3]样式(OK,YES_NO,YES_NO_CANCEL)
  //       (COMMA callback_object)?
  private static boolean confirm_msg_args_3_0_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "confirm_msg_args_3_0_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && expression(b, l + 1);
    r = r && confirm_msg_args_3_0_2_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (COMMA callback_object)?
  private static boolean confirm_msg_args_3_0_2_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "confirm_msg_args_3_0_2_0_2")) return false;
    confirm_msg_args_3_0_2_0_2_0(b, l + 1);
    return true;
  }

  // COMMA callback_object
  private static boolean confirm_msg_args_3_0_2_0_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "confirm_msg_args_3_0_2_0_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && callback_object(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // CONFIRM_MSG LPAREN confirm_msg_args RPAREN
  public static boolean confirm_msg_call(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "confirm_msg_call")) return false;
    if (!nextTokenIs(b, CONFIRM_MSG)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, CONFIRM_MSG, LPAREN);
    r = r && confirm_msg_args(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, CONFIRM_MSG_CALL, r);
    return r;
  }

  /* ********************************************************** */
  // SINGLE_QUOTED_STRING
  //   | DOUBLE_QUOTED_STRING
  //   | numeric_constant
  public static boolean constant(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "constant")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, CONSTANT, "<constant>");
    r = consumeToken(b, SINGLE_QUOTED_STRING);
    if (!r) r = consumeToken(b, DOUBLE_QUOTED_STRING);
    if (!r) r = numeric_constant(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // LBRACE LBRACE expression RBRACE RBRACE
  public static boolean double_brace_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "double_brace_expression")) return false;
    if (!nextTokenIs(b, LBRACE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, LBRACE, LBRACE);
    r = r && expression(b, l + 1);
    r = r && consumeTokens(b, 0, RBRACE, RBRACE);
    exit_section_(b, m, DOUBLE_BRACE_EXPRESSION, r);
    return r;
  }

  /* ********************************************************** */
  // logical_or_expression
  public static boolean expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, EXPRESSION, "<expression>");
    r = logical_or_expression(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // expression_statement (SEMICOLON expression_statement)*
  public static boolean expression_sequence(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_sequence")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, EXPRESSION_SEQUENCE, "<expression sequence>");
    r = expression_statement(b, l + 1);
    r = r && expression_sequence_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // (SEMICOLON expression_statement)*
  private static boolean expression_sequence_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_sequence_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!expression_sequence_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "expression_sequence_1", c)) break;
    }
    return true;
  }

  // SEMICOLON expression_statement
  private static boolean expression_sequence_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_sequence_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, SEMICOLON);
    r = r && expression_statement(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // expression
  public static boolean expression_statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_statement")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, EXPRESSION_STATEMENT, "<expression statement>");
    r = expression(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // ((PARENT_KEYWORD | CONTAINER_KEYWORD) DOT)?
  //   (
  //     confirm_msg_call |          // 特殊处理 ConfirmMsg
  //     regular_function_call       // 普通函数调用
  //   )
  public static boolean function_call(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "function_call")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, FUNCTION_CALL, "<function call>");
    r = function_call_0(b, l + 1);
    r = r && function_call_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // ((PARENT_KEYWORD | CONTAINER_KEYWORD) DOT)?
  private static boolean function_call_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "function_call_0")) return false;
    function_call_0_0(b, l + 1);
    return true;
  }

  // (PARENT_KEYWORD | CONTAINER_KEYWORD) DOT
  private static boolean function_call_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "function_call_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = function_call_0_0_0(b, l + 1);
    r = r && consumeToken(b, DOT);
    exit_section_(b, m, null, r);
    return r;
  }

  // PARENT_KEYWORD | CONTAINER_KEYWORD
  private static boolean function_call_0_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "function_call_0_0_0")) return false;
    boolean r;
    r = consumeToken(b, PARENT_KEYWORD);
    if (!r) r = consumeToken(b, CONTAINER_KEYWORD);
    return r;
  }

  // confirm_msg_call |          // 特殊处理 ConfirmMsg
  //     regular_function_call
  private static boolean function_call_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "function_call_1")) return false;
    boolean r;
    r = confirm_msg_call(b, l + 1);
    if (!r) r = regular_function_call(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // IF_KEYWORD LPAREN expression RPAREN statement_block (ELSE_KEYWORD statement_block)?
  public static boolean if_statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "if_statement")) return false;
    if (!nextTokenIs(b, IF_KEYWORD)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, IF_KEYWORD, LPAREN);
    r = r && expression(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    r = r && statement_block(b, l + 1);
    r = r && if_statement_5(b, l + 1);
    exit_section_(b, m, IF_STATEMENT, r);
    return r;
  }

  // (ELSE_KEYWORD statement_block)?
  private static boolean if_statement_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "if_statement_5")) return false;
    if_statement_5_0(b, l + 1);
    return true;
  }

  // ELSE_KEYWORD statement_block
  private static boolean if_statement_5_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "if_statement_5_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, ELSE_KEYWORD);
    r = r && statement_block(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // IIF_KEYWORD
  public static boolean iif_function_call(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "iif_function_call")) return false;
    if (!nextTokenIs(b, IIF_KEYWORD)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, IIF_KEYWORD);
    exit_section_(b, m, IIF_FUNCTION_CALL, r);
    return r;
  }

  /* ********************************************************** */
  // JAVA_PATH_IDENTIFIER
  public static boolean java_method_call(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "java_method_call")) return false;
    if (!nextTokenIs(b, JAVA_PATH_IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, JAVA_PATH_IDENTIFIER);
    exit_section_(b, m, JAVA_METHOD_CALL, r);
    return r;
  }

  /* ********************************************************** */
  // comparison_expression ((AND_OP|AND_OP_ENTITY) comparison_expression)*
  static boolean logical_and_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "logical_and_expression")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = comparison_expression(b, l + 1);
    r = r && logical_and_expression_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ((AND_OP|AND_OP_ENTITY) comparison_expression)*
  private static boolean logical_and_expression_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "logical_and_expression_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!logical_and_expression_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "logical_and_expression_1", c)) break;
    }
    return true;
  }

  // (AND_OP|AND_OP_ENTITY) comparison_expression
  private static boolean logical_and_expression_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "logical_and_expression_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = logical_and_expression_1_0_0(b, l + 1);
    r = r && comparison_expression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // AND_OP|AND_OP_ENTITY
  private static boolean logical_and_expression_1_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "logical_and_expression_1_0_0")) return false;
    boolean r;
    r = consumeToken(b, AND_OP);
    if (!r) r = consumeToken(b, AND_OP_ENTITY);
    return r;
  }

  /* ********************************************************** */
  // logical_and_expression (OR_OP logical_and_expression)*
  static boolean logical_or_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "logical_or_expression")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = logical_and_expression(b, l + 1);
    r = r && logical_or_expression_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (OR_OP logical_and_expression)*
  private static boolean logical_or_expression_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "logical_or_expression_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!logical_or_expression_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "logical_or_expression_1", c)) break;
    }
    return true;
  }

  // OR_OP logical_and_expression
  private static boolean logical_or_expression_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "logical_or_expression_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, OR_OP);
    r = r && logical_and_expression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // MACRO_IDENTIFIER
  public static boolean macro_call_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "macro_call_expression")) return false;
    if (!nextTokenIs(b, MACRO_IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, MACRO_IDENTIFIER);
    exit_section_(b, m, MACRO_CALL_EXPRESSION, r);
    return r;
  }

  /* ********************************************************** */
  // parameter_array |           // {{参数1},{参数2},{参数3}} 的形式
  //   double_brace_expression |   // {{表达式}} 的形式
  //   object_literal |            // {} 空对象形式
  //   expression
  public static boolean message_params_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "message_params_expression")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, MESSAGE_PARAMS_EXPRESSION, "<message params expression>");
    r = parameter_array(b, l + 1);
    if (!r) r = double_brace_expression(b, l + 1);
    if (!r) r = object_literal(b, l + 1);
    if (!r) r = expression(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // unary_expression ((MUL | DIV) unary_expression)*
  static boolean multiplicative_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "multiplicative_expression")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = unary_expression(b, l + 1);
    r = r && multiplicative_expression_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ((MUL | DIV) unary_expression)*
  private static boolean multiplicative_expression_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "multiplicative_expression_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!multiplicative_expression_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "multiplicative_expression_1", c)) break;
    }
    return true;
  }

  // (MUL | DIV) unary_expression
  private static boolean multiplicative_expression_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "multiplicative_expression_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = multiplicative_expression_1_0_0(b, l + 1);
    r = r && unary_expression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // MUL | DIV
  private static boolean multiplicative_expression_1_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "multiplicative_expression_1_0_0")) return false;
    boolean r;
    r = consumeToken(b, MUL);
    if (!r) r = consumeToken(b, DIV);
    return r;
  }

  /* ********************************************************** */
  // DECIMAL_NUMBER      // 优先匹配小数
  //   | INTEGER_NUMBER
  public static boolean numeric_constant(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "numeric_constant")) return false;
    if (!nextTokenIs(b, "<numeric constant>", DECIMAL_NUMBER, INTEGER_NUMBER)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, NUMERIC_CONSTANT, "<numeric constant>");
    r = consumeToken(b, DECIMAL_NUMBER);
    if (!r) r = consumeToken(b, INTEGER_NUMBER);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // LBRACE object_literal_content RBRACE
  public static boolean object_literal(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "object_literal")) return false;
    if (!nextTokenIs(b, LBRACE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LBRACE);
    r = r && object_literal_content(b, l + 1);
    r = r && consumeToken(b, RBRACE);
    exit_section_(b, m, OBJECT_LITERAL, r);
    return r;
  }

  /* ********************************************************** */
  // (object_property (COMMA object_property)*)?
  public static boolean object_literal_content(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "object_literal_content")) return false;
    Marker m = enter_section_(b, l, _NONE_, OBJECT_LITERAL_CONTENT, "<object literal content>");
    object_literal_content_0(b, l + 1);
    exit_section_(b, l, m, true, false, null);
    return true;
  }

  // object_property (COMMA object_property)*
  private static boolean object_literal_content_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "object_literal_content_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = object_property(b, l + 1);
    r = r && object_literal_content_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (COMMA object_property)*
  private static boolean object_literal_content_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "object_literal_content_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!object_literal_content_0_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "object_literal_content_0_1", c)) break;
    }
    return true;
  }

  // COMMA object_property
  private static boolean object_literal_content_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "object_literal_content_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && object_property(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // IDENTIFIER COLON (object_literal | expression | code_block_literal)
  public static boolean object_property(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "object_property")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, IDENTIFIER, COLON);
    r = r && object_property_2(b, l + 1);
    exit_section_(b, m, OBJECT_PROPERTY, r);
    return r;
  }

  // object_literal | expression | code_block_literal
  private static boolean object_property_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "object_property_2")) return false;
    boolean r;
    r = object_literal(b, l + 1);
    if (!r) r = expression(b, l + 1);
    if (!r) r = code_block_literal(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // LBRACE LBRACE expression RBRACE (COMMA LBRACE expression RBRACE)* RBRACE
  public static boolean parameter_array(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_array")) return false;
    if (!nextTokenIs(b, LBRACE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, LBRACE, LBRACE);
    r = r && expression(b, l + 1);
    r = r && consumeToken(b, RBRACE);
    r = r && parameter_array_4(b, l + 1);
    r = r && consumeToken(b, RBRACE);
    exit_section_(b, m, PARAMETER_ARRAY, r);
    return r;
  }

  // (COMMA LBRACE expression RBRACE)*
  private static boolean parameter_array_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_array_4")) return false;
    while (true) {
      int c = current_position_(b);
      if (!parameter_array_4_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "parameter_array_4", c)) break;
    }
    return true;
  }

  // COMMA LBRACE expression RBRACE
  private static boolean parameter_array_4_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_array_4_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, COMMA, LBRACE);
    r = r && expression(b, l + 1);
    r = r && consumeToken(b, RBRACE);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // ((PARENT_KEYWORD | CONTAINER_KEYWORD) DOT)? IDENTIFIER
  static boolean path(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "path")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = path_0(b, l + 1);
    r = r && consumeToken(b, IDENTIFIER);
    exit_section_(b, m, null, r);
    return r;
  }

  // ((PARENT_KEYWORD | CONTAINER_KEYWORD) DOT)?
  private static boolean path_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "path_0")) return false;
    path_0_0(b, l + 1);
    return true;
  }

  // (PARENT_KEYWORD | CONTAINER_KEYWORD) DOT
  private static boolean path_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "path_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = path_0_0_0(b, l + 1);
    r = r && consumeToken(b, DOT);
    exit_section_(b, m, null, r);
    return r;
  }

  // PARENT_KEYWORD | CONTAINER_KEYWORD
  private static boolean path_0_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "path_0_0_0")) return false;
    boolean r;
    r = consumeToken(b, PARENT_KEYWORD);
    if (!r) r = consumeToken(b, CONTAINER_KEYWORD);
    return r;
  }

  /* ********************************************************** */
  // constant
  //   | function_call
  //   | variable_reference
  //   | LPAREN expression RPAREN
  //   | boolean_constant
  public static boolean primary_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "primary_expression")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, PRIMARY_EXPRESSION, "<primary expression>");
    r = constant(b, l + 1);
    if (!r) r = function_call(b, l + 1);
    if (!r) r = variable_reference(b, l + 1);
    if (!r) r = primary_expression_3(b, l + 1);
    if (!r) r = boolean_constant(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // LPAREN expression RPAREN
  private static boolean primary_expression_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "primary_expression_3")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LPAREN);
    r = r && expression(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // (macro_call_expression | path | java_method_call | iif_function_call)
  //   LPAREN argument_list? RPAREN
  public static boolean regular_function_call(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "regular_function_call")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, REGULAR_FUNCTION_CALL, "<regular function call>");
    r = regular_function_call_0(b, l + 1);
    r = r && consumeToken(b, LPAREN);
    r = r && regular_function_call_2(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // macro_call_expression | path | java_method_call | iif_function_call
  private static boolean regular_function_call_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "regular_function_call_0")) return false;
    boolean r;
    r = macro_call_expression(b, l + 1);
    if (!r) r = path(b, l + 1);
    if (!r) r = java_method_call(b, l + 1);
    if (!r) r = iif_function_call(b, l + 1);
    return r;
  }

  // argument_list?
  private static boolean regular_function_call_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "regular_function_call_2")) return false;
    argument_list(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // top_level_statement*
  static boolean root(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "root")) return false;
    while (true) {
      int c = current_position_(b);
      if (!top_level_statement(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "root", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // (variable_declaration SEMICOLON)
  //   | (variable_assignment SEMICOLON?)
  //   | ((expression_sequence | if_statement | while_statement) SEMICOLON?)
  public static boolean statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "statement")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, STATEMENT, "<statement>");
    r = statement_0(b, l + 1);
    if (!r) r = statement_1(b, l + 1);
    if (!r) r = statement_2(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // variable_declaration SEMICOLON
  private static boolean statement_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "statement_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = variable_declaration(b, l + 1);
    r = r && consumeToken(b, SEMICOLON);
    exit_section_(b, m, null, r);
    return r;
  }

  // variable_assignment SEMICOLON?
  private static boolean statement_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "statement_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = variable_assignment(b, l + 1);
    r = r && statement_1_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // SEMICOLON?
  private static boolean statement_1_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "statement_1_1")) return false;
    consumeToken(b, SEMICOLON);
    return true;
  }

  // (expression_sequence | if_statement | while_statement) SEMICOLON?
  private static boolean statement_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "statement_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = statement_2_0(b, l + 1);
    r = r && statement_2_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // expression_sequence | if_statement | while_statement
  private static boolean statement_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "statement_2_0")) return false;
    boolean r;
    r = expression_sequence(b, l + 1);
    if (!r) r = if_statement(b, l + 1);
    if (!r) r = while_statement(b, l + 1);
    return r;
  }

  // SEMICOLON?
  private static boolean statement_2_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "statement_2_1")) return false;
    consumeToken(b, SEMICOLON);
    return true;
  }

  /* ********************************************************** */
  // statement | block_statement
  public static boolean statement_block(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "statement_block")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, STATEMENT_BLOCK, "<statement block>");
    r = statement(b, l + 1);
    if (!r) r = block_statement(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // (variable_declaration SEMICOLON)
  //   | (variable_assignment SEMICOLON?)
  //   | ((expression_sequence | if_statement | while_statement) SEMICOLON?)
  static boolean top_level_statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "top_level_statement")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = top_level_statement_0(b, l + 1);
    if (!r) r = top_level_statement_1(b, l + 1);
    if (!r) r = top_level_statement_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // variable_declaration SEMICOLON
  private static boolean top_level_statement_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "top_level_statement_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = variable_declaration(b, l + 1);
    r = r && consumeToken(b, SEMICOLON);
    exit_section_(b, m, null, r);
    return r;
  }

  // variable_assignment SEMICOLON?
  private static boolean top_level_statement_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "top_level_statement_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = variable_assignment(b, l + 1);
    r = r && top_level_statement_1_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // SEMICOLON?
  private static boolean top_level_statement_1_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "top_level_statement_1_1")) return false;
    consumeToken(b, SEMICOLON);
    return true;
  }

  // (expression_sequence | if_statement | while_statement) SEMICOLON?
  private static boolean top_level_statement_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "top_level_statement_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = top_level_statement_2_0(b, l + 1);
    r = r && top_level_statement_2_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // expression_sequence | if_statement | while_statement
  private static boolean top_level_statement_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "top_level_statement_2_0")) return false;
    boolean r;
    r = expression_sequence(b, l + 1);
    if (!r) r = if_statement(b, l + 1);
    if (!r) r = while_statement(b, l + 1);
    return r;
  }

  // SEMICOLON?
  private static boolean top_level_statement_2_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "top_level_statement_2_1")) return false;
    consumeToken(b, SEMICOLON);
    return true;
  }

  /* ********************************************************** */
  // (MINUS | NOT_OP) unary_expression | primary_expression
  static boolean unary_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "unary_expression")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = unary_expression_0(b, l + 1);
    if (!r) r = primary_expression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (MINUS | NOT_OP) unary_expression
  private static boolean unary_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "unary_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = unary_expression_0_0(b, l + 1);
    r = r && unary_expression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // MINUS | NOT_OP
  private static boolean unary_expression_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "unary_expression_0_0")) return false;
    boolean r;
    r = consumeToken(b, MINUS);
    if (!r) r = consumeToken(b, NOT_OP);
    return r;
  }

  /* ********************************************************** */
  // IDENTIFIER EQ expression
  public static boolean variable_assignment(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "variable_assignment")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, IDENTIFIER, EQ);
    r = r && expression(b, l + 1);
    exit_section_(b, m, VARIABLE_ASSIGNMENT, r);
    return r;
  }

  /* ********************************************************** */
  // VAR_KEYWORD IDENTIFIER (EQ expression)?
  public static boolean variable_declaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "variable_declaration")) return false;
    if (!nextTokenIs(b, VAR_KEYWORD)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, VAR_KEYWORD, IDENTIFIER);
    r = r && variable_declaration_2(b, l + 1);
    exit_section_(b, m, VARIABLE_DECLARATION, r);
    return r;
  }

  // (EQ expression)?
  private static boolean variable_declaration_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "variable_declaration_2")) return false;
    variable_declaration_2_0(b, l + 1);
    return true;
  }

  // EQ expression
  private static boolean variable_declaration_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "variable_declaration_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, EQ);
    r = r && expression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // path
  public static boolean variable_reference(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "variable_reference")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, VARIABLE_REFERENCE, "<variable reference>");
    r = path(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // WHILE_KEYWORD LPAREN expression RPAREN statement_block
  public static boolean while_statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "while_statement")) return false;
    if (!nextTokenIs(b, WHILE_KEYWORD)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, WHILE_KEYWORD, LPAREN);
    r = r && expression(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    r = r && statement_block(b, l + 1);
    exit_section_(b, m, WHILE_STATEMENT, r);
    return r;
  }

}
