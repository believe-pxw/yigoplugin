// Generated by JFlex 1.9.2 http://jflex.de/  (tweaked for IntelliJ platform)
// source: _MyLanguageLexer.flex

package example.parser;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static example.psi.MyLanguageTypes.*;


public class _MyLanguageLexer implements FlexLexer {

  /** This character denotes the end of file */
  public static final int YYEOF = -1;

  /** initial size of the lookahead buffer */
  private static final int ZZ_BUFFERSIZE = 16384;

  /** lexical states */
  public static final int YYINITIAL = 0;

  /**
   * ZZ_LEXSTATE[l] is the state in the DFA for the lexical state l
   * ZZ_LEXSTATE[l+1] is the state in the DFA for the lexical state l
   *                  at the beginning of a line
   * l is of the form l = 2*k, k a non negative integer
   */
  private static final int ZZ_LEXSTATE[] = {
     0, 0
  };

  /**
   * Top-level table for translating characters to character classes
   */
  private static final int [] ZZ_CMAP_TOP = zzUnpackcmap_top();

  private static final String ZZ_CMAP_TOP_PACKED_0 =
    "\1\0\25\u0100\1\u0200\11\u0100\1\u0300\17\u0100\1\u0400\u10cf\u0100";

  private static int [] zzUnpackcmap_top() {
    int [] result = new int[4352];
    int offset = 0;
    offset = zzUnpackcmap_top(ZZ_CMAP_TOP_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackcmap_top(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }


  /**
   * Second-level tables for translating characters to character classes
   */
  private static final int [] ZZ_CMAP_BLOCKS = zzUnpackcmap_blocks();

  private static final String ZZ_CMAP_BLOCKS_PACKED_0 =
    "\11\0\5\1\22\0\1\1\1\2\1\3\3\0\1\4"+
    "\1\5\1\6\1\7\1\10\1\11\1\12\1\13\1\14"+
    "\1\15\12\16\1\17\1\20\1\21\1\22\1\23\2\0"+
    "\2\24\1\25\2\24\1\26\2\24\1\27\3\24\1\30"+
    "\2\24\1\31\12\24\4\0\1\32\1\0\1\33\1\24"+
    "\1\34\1\24\1\35\1\36\1\37\1\40\1\41\2\24"+
    "\1\42\1\43\1\44\1\45\1\46\1\24\1\47\1\50"+
    "\1\51\1\52\1\53\1\54\3\24\1\55\1\56\1\57"+
    "\7\0\1\1\32\0\1\1\u01df\0\1\1\177\0\13\1"+
    "\35\0\2\1\5\0\1\1\57\0\1\1\240\0\1\1"+
    "\377\0";

  private static int [] zzUnpackcmap_blocks() {
    int [] result = new int[1280];
    int offset = 0;
    offset = zzUnpackcmap_blocks(ZZ_CMAP_BLOCKS_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackcmap_blocks(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }

  /**
   * Translates DFA states to action switch labels.
   */
  private static final int [] ZZ_ACTION = zzUnpackAction();

  private static final String ZZ_ACTION_PACKED_0 =
    "\1\0\1\1\1\2\1\3\1\1\1\4\1\1\1\5"+
    "\1\6\1\7\1\10\1\11\1\12\1\13\1\14\1\15"+
    "\1\16\1\17\1\20\1\21\1\22\14\23\1\24\1\1"+
    "\1\25\1\26\1\0\1\27\1\30\4\0\1\31\1\32"+
    "\1\33\1\34\1\35\7\23\1\36\3\23\1\37\3\0"+
    "\1\23\1\40\2\23\1\0\5\23\1\41\1\23\1\0"+
    "\1\42\1\43\4\23\1\44\1\45\1\23\1\46\1\23"+
    "\1\47\4\23\1\50\1\51\1\0\3\23\1\52\1\0"+
    "\2\23\1\53\1\0\2\23\1\0\1\23\1\54\1\55"+
    "\1\56";

  private static int [] zzUnpackAction() {
    int [] result = new int[112];
    int offset = 0;
    offset = zzUnpackAction(ZZ_ACTION_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackAction(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }


  /**
   * Translates a state to a row index in the transition table
   */
  private static final int [] ZZ_ROWMAP = zzUnpackRowMap();

  private static final String ZZ_ROWMAP_PACKED_0 =
    "\0\0\0\60\0\140\0\220\0\300\0\360\0\u0120\0\60"+
    "\0\60\0\60\0\60\0\60\0\60\0\60\0\60\0\u0150"+
    "\0\60\0\60\0\u0180\0\u01b0\0\u01e0\0\u0210\0\u0240\0\u0270"+
    "\0\u02a0\0\u02d0\0\u0300\0\u0330\0\u0360\0\u0390\0\u03c0\0\u03f0"+
    "\0\u0420\0\60\0\u0450\0\60\0\60\0\300\0\60\0\60"+
    "\0\u0480\0\u04b0\0\u04e0\0\u0120\0\60\0\60\0\60\0\60"+
    "\0\60\0\u0510\0\u0540\0\u0570\0\u05a0\0\u05d0\0\u0600\0\u0630"+
    "\0\u0210\0\u0660\0\u0690\0\u06c0\0\60\0\u06f0\0\u0720\0\u0750"+
    "\0\u0780\0\u0210\0\u07b0\0\u07e0\0\u0810\0\u0840\0\u0870\0\u08a0"+
    "\0\u08d0\0\u0900\0\u0210\0\u0930\0\u0960\0\60\0\60\0\u0990"+
    "\0\u09c0\0\u09f0\0\u0a20\0\u0a50\0\u0210\0\u0a80\0\u0210\0\u0ab0"+
    "\0\u0ae0\0\u0b10\0\u0b40\0\u0b70\0\u0ba0\0\u0210\0\u0210\0\u0bd0"+
    "\0\u0c00\0\u0c30\0\u0c60\0\u0210\0\u0c90\0\u0cc0\0\u0cf0\0\u0d20"+
    "\0\u0d50\0\u0d80\0\u0db0\0\u0de0\0\u0e10\0\u0210\0\60\0\u0210";

  private static int [] zzUnpackRowMap() {
    int [] result = new int[112];
    int offset = 0;
    offset = zzUnpackRowMap(ZZ_ROWMAP_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackRowMap(String packed, int offset, int [] result) {
    int i = 0;  /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length() - 1;
    while (i < l) {
      int high = packed.charAt(i++) << 16;
      result[j++] = high | packed.charAt(i++);
    }
    return j;
  }

  /**
   * The transition table of the DFA
   */
  private static final int [] ZZ_TRANS = zzUnpacktrans();

  private static final String ZZ_TRANS_PACKED_0 =
    "\1\2\1\3\1\4\1\5\1\6\1\7\1\10\1\11"+
    "\1\12\1\13\1\14\1\15\1\16\1\17\1\20\1\21"+
    "\1\22\1\23\1\24\1\25\1\26\1\27\1\26\1\30"+
    "\1\31\1\32\2\26\1\33\1\34\1\35\2\26\1\36"+
    "\4\26\1\32\2\26\1\37\1\26\1\40\1\41\1\42"+
    "\1\43\1\44\61\0\1\3\100\0\1\45\35\0\3\46"+
    "\1\47\54\46\4\0\1\50\26\0\1\51\3\0\1\52"+
    "\2\0\1\53\15\0\5\54\1\55\52\54\16\0\1\20"+
    "\63\0\1\56\1\57\56\0\1\60\57\0\1\61\53\0"+
    "\1\26\5\0\31\26\21\0\1\26\5\0\21\26\1\62"+
    "\7\26\21\0\1\26\5\0\3\26\1\63\25\26\21\0"+
    "\1\26\5\0\7\26\1\64\21\26\21\0\1\26\5\0"+
    "\7\26\1\65\21\26\21\0\1\26\5\0\21\26\1\66"+
    "\7\26\21\0\1\26\5\0\16\26\1\67\12\26\21\0"+
    "\1\26\5\0\7\26\1\70\21\26\21\0\1\26\5\0"+
    "\12\26\1\71\16\26\21\0\1\26\5\0\23\26\1\72"+
    "\5\26\21\0\1\26\5\0\7\26\1\73\21\26\21\0"+
    "\1\26\5\0\14\26\1\74\14\26\61\0\1\75\44\0"+
    "\1\76\65\0\1\77\57\0\1\100\24\0\1\26\5\0"+
    "\20\26\1\101\10\26\21\0\1\26\5\0\2\26\1\102"+
    "\26\26\21\0\1\26\5\0\10\26\1\103\20\26\21\0"+
    "\1\26\5\0\23\26\1\104\5\26\17\0\1\105\1\0"+
    "\1\26\5\0\17\26\1\106\1\107\10\26\21\0\1\26"+
    "\5\0\24\26\1\110\4\26\21\0\1\26\5\0\16\26"+
    "\1\111\12\26\21\0\1\26\5\0\26\26\1\112\2\26"+
    "\21\0\1\26\5\0\23\26\1\113\5\26\21\0\1\26"+
    "\5\0\15\26\1\114\13\26\51\0\1\115\31\0\1\116"+
    "\57\0\1\117\55\0\1\26\5\0\12\26\1\120\12\26"+
    "\1\121\3\26\21\0\1\26\5\0\23\26\1\122\5\26"+
    "\21\0\1\26\5\0\11\26\1\123\17\26\27\0\31\124"+
    "\17\0\1\105\1\0\1\26\5\0\17\26\1\106\11\26"+
    "\21\0\1\26\5\0\25\26\1\121\3\26\21\0\1\26"+
    "\5\0\11\26\1\125\17\26\21\0\1\26\5\0\24\26"+
    "\1\126\4\26\21\0\1\26\5\0\11\26\1\127\17\26"+
    "\21\0\1\26\5\0\16\26\1\130\12\26\23\0\1\131"+
    "\55\0\1\26\5\0\15\26\1\132\13\26\21\0\1\26"+
    "\5\0\7\26\1\133\21\26\21\0\1\26\5\0\21\26"+
    "\1\134\7\26\21\0\1\26\5\0\20\26\1\135\10\26"+
    "\17\0\1\105\1\0\1\124\5\0\31\124\21\0\1\26"+
    "\5\0\11\26\1\136\17\26\21\0\1\26\5\0\11\26"+
    "\1\137\17\26\7\0\1\140\71\0\1\26\5\0\23\26"+
    "\1\141\5\26\21\0\1\26\5\0\15\26\1\142\13\26"+
    "\21\0\1\26\5\0\6\26\1\143\22\26\21\0\1\26"+
    "\5\0\25\26\1\144\3\26\36\0\1\145\42\0\1\26"+
    "\5\0\17\26\1\146\11\26\21\0\1\26\5\0\20\26"+
    "\1\147\10\26\21\0\1\26\5\0\31\150\46\0\1\151"+
    "\32\0\1\26\5\0\4\26\1\152\24\26\21\0\1\26"+
    "\5\0\11\26\1\153\17\26\21\0\1\150\5\0\31\150"+
    "\51\0\1\154\27\0\1\26\5\0\24\26\1\155\4\26"+
    "\21\0\1\26\5\0\23\26\1\156\5\26\23\0\1\157"+
    "\55\0\1\26\5\0\13\26\1\160\15\26\3\0";

  private static int [] zzUnpacktrans() {
    int [] result = new int[3648];
    int offset = 0;
    offset = zzUnpacktrans(ZZ_TRANS_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpacktrans(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      value--;
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }


  /* error codes */
  private static final int ZZ_UNKNOWN_ERROR = 0;
  private static final int ZZ_NO_MATCH = 1;
  private static final int ZZ_PUSHBACK_2BIG = 2;

  /* error messages for the codes above */
  private static final String[] ZZ_ERROR_MSG = {
    "Unknown internal scanner error",
    "Error: could not match input",
    "Error: pushback value was too large"
  };

  /**
   * ZZ_ATTRIBUTE[aState] contains the attributes of state {@code aState}
   */
  private static final int [] ZZ_ATTRIBUTE = zzUnpackAttribute();

  private static final String ZZ_ATTRIBUTE_PACKED_0 =
    "\1\0\1\11\5\1\10\11\1\1\2\11\17\1\1\11"+
    "\1\1\2\11\1\0\2\11\4\0\5\11\13\1\1\11"+
    "\3\0\4\1\1\0\7\1\1\0\2\11\20\1\1\0"+
    "\4\1\1\0\3\1\1\0\2\1\1\0\2\1\1\11"+
    "\1\1";

  private static int [] zzUnpackAttribute() {
    int [] result = new int[112];
    int offset = 0;
    offset = zzUnpackAttribute(ZZ_ATTRIBUTE_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackAttribute(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }

  /** the input device */
  private java.io.Reader zzReader;

  /** the current state of the DFA */
  private int zzState;

  /** the current lexical state */
  private int zzLexicalState = YYINITIAL;

  /** this buffer contains the current text to be matched and is
      the source of the yytext() string */
  private CharSequence zzBuffer = "";

  /** the textposition at the last accepting state */
  private int zzMarkedPos;

  /** the current text position in the buffer */
  private int zzCurrentPos;

  /** startRead marks the beginning of the yytext() string in the buffer */
  private int zzStartRead;

  /** endRead marks the last character in the buffer, that has been read
      from input */
  private int zzEndRead;

  /** zzAtEOF == true <=> the scanner is at the EOF */
  private boolean zzAtEOF;

  /** Number of newlines encountered up to the start of the matched text. */
  @SuppressWarnings("unused")
  private int yyline;

  /** Number of characters from the last newline up to the start of the matched text. */
  @SuppressWarnings("unused")
  protected int yycolumn;

  /** Number of characters up to the start of the matched text. */
  @SuppressWarnings("unused")
  private long yychar;

  /** Whether the scanner is currently at the beginning of a line. */
  @SuppressWarnings("unused")
  private boolean zzAtBOL = true;

  /** Whether the user-EOF-code has already been executed. */
  @SuppressWarnings("unused")
  private boolean zzEOFDone;

  /* user code: */
  public _MyLanguageLexer() {
    this((java.io.Reader)null);
  }


  /**
   * Creates a new scanner
   *
   * @param   in  the java.io.Reader to read input from.
   */
  public _MyLanguageLexer(java.io.Reader in) {
    this.zzReader = in;
  }


  /** Returns the maximum size of the scanner buffer, which limits the size of tokens. */
  private int zzMaxBufferLen() {
    return Integer.MAX_VALUE;
  }

  /**  Whether the scanner buffer can grow to accommodate a larger token. */
  private boolean zzCanGrow() {
    return true;
  }

  /**
   * Translates raw input code points to DFA table row
   */
  private static int zzCMap(int input) {
    int offset = input & 255;
    return offset == input ? ZZ_CMAP_BLOCKS[offset] : ZZ_CMAP_BLOCKS[ZZ_CMAP_TOP[input >> 8] | offset];
  }

  public final int getTokenStart() {
    return zzStartRead;
  }

  public final int getTokenEnd() {
    return getTokenStart() + yylength();
  }

  public void reset(CharSequence buffer, int start, int end, int initialState) {
    zzBuffer = buffer;
    zzCurrentPos = zzMarkedPos = zzStartRead = start;
    zzAtEOF  = false;
    zzAtBOL = true;
    zzEndRead = end;
    yybegin(initialState);
  }

  /**
   * Refills the input buffer.
   *
   * @return      {@code false}, iff there was new input.
   *
   * @exception   java.io.IOException  if any I/O-Error occurs
   */
  private boolean zzRefill() throws java.io.IOException {
    return true;
  }


  /**
   * Returns the current lexical state.
   */
  public final int yystate() {
    return zzLexicalState;
  }


  /**
   * Enters a new lexical state
   *
   * @param newState the new lexical state
   */
  public final void yybegin(int newState) {
    zzLexicalState = newState;
  }


  /**
   * Returns the text matched by the current regular expression.
   */
  public final CharSequence yytext() {
    return zzBuffer.subSequence(zzStartRead, zzMarkedPos);
  }


  /**
   * Returns the character at position {@code pos} from the
   * matched text.
   *
   * It is equivalent to yytext().charAt(pos), but faster
   *
   * @param pos the position of the character to fetch.
   *            A value from 0 to yylength()-1.
   *
   * @return the character at position pos
   */
  public final char yycharat(int pos) {
    return zzBuffer.charAt(zzStartRead+pos);
  }


  /**
   * Returns the length of the matched text region.
   */
  public final int yylength() {
    return zzMarkedPos-zzStartRead;
  }


  /**
   * Reports an error that occurred while scanning.
   *
   * In a wellformed scanner (no or only correct usage of
   * yypushback(int) and a match-all fallback rule) this method
   * will only be called with things that "Can't Possibly Happen".
   * If this method is called, something is seriously wrong
   * (e.g. a JFlex bug producing a faulty scanner etc.).
   *
   * Usual syntax/scanner level error handling should be done
   * in error fallback rules.
   *
   * @param   errorCode  the code of the errormessage to display
   */
  private void zzScanError(int errorCode) {
    String message;
    try {
      message = ZZ_ERROR_MSG[errorCode];
    }
    catch (ArrayIndexOutOfBoundsException e) {
      message = ZZ_ERROR_MSG[ZZ_UNKNOWN_ERROR];
    }

    throw new Error(message);
  }


  /**
   * Pushes the specified amount of characters back into the input stream.
   *
   * They will be read again by then next call of the scanning method
   *
   * @param number  the number of characters to be read again.
   *                This number must not be greater than yylength()!
   */
  public void yypushback(int number)  {
    if ( number > yylength() )
      zzScanError(ZZ_PUSHBACK_2BIG);

    zzMarkedPos -= number;
  }


  /**
   * Resumes scanning until the next regular expression is matched,
   * the end of input is encountered or an I/O-Error occurs.
   *
   * @return      the next token
   * @exception   java.io.IOException  if any I/O-Error occurs
   */
  public IElementType advance() throws java.io.IOException
  {
    int zzInput;
    int zzAction;

    // cached fields:
    int zzCurrentPosL;
    int zzMarkedPosL;
    int zzEndReadL = zzEndRead;
    CharSequence zzBufferL = zzBuffer;

    int [] zzTransL = ZZ_TRANS;
    int [] zzRowMapL = ZZ_ROWMAP;
    int [] zzAttrL = ZZ_ATTRIBUTE;

    while (true) {
      zzMarkedPosL = zzMarkedPos;

      zzAction = -1;

      zzCurrentPosL = zzCurrentPos = zzStartRead = zzMarkedPosL;

      zzState = ZZ_LEXSTATE[zzLexicalState];

      // set up zzAction for empty match case:
      int zzAttributes = zzAttrL[zzState];
      if ( (zzAttributes & 1) == 1 ) {
        zzAction = zzState;
      }


      zzForAction: {
        while (true) {

          if (zzCurrentPosL < zzEndReadL) {
            zzInput = Character.codePointAt(zzBufferL, zzCurrentPosL);
            zzCurrentPosL += Character.charCount(zzInput);
          }
          else if (zzAtEOF) {
            zzInput = YYEOF;
            break zzForAction;
          }
          else {
            // store back cached positions
            zzCurrentPos  = zzCurrentPosL;
            zzMarkedPos   = zzMarkedPosL;
            boolean eof = zzRefill();
            // get translated positions and possibly new buffer
            zzCurrentPosL  = zzCurrentPos;
            zzMarkedPosL   = zzMarkedPos;
            zzBufferL      = zzBuffer;
            zzEndReadL     = zzEndRead;
            if (eof) {
              zzInput = YYEOF;
              break zzForAction;
            }
            else {
              zzInput = Character.codePointAt(zzBufferL, zzCurrentPosL);
              zzCurrentPosL += Character.charCount(zzInput);
            }
          }
          int zzNext = zzTransL[ zzRowMapL[zzState] + zzCMap(zzInput) ];
          if (zzNext == -1) break zzForAction;
          zzState = zzNext;

          zzAttributes = zzAttrL[zzState];
          if ( (zzAttributes & 1) == 1 ) {
            zzAction = zzState;
            zzMarkedPosL = zzCurrentPosL;
            if ( (zzAttributes & 8) == 8 ) break zzForAction;
          }

        }
      }

      // store back cached position
      zzMarkedPos = zzMarkedPosL;

      if (zzInput == YYEOF && zzStartRead == zzCurrentPos) {
        zzAtEOF = true;
        return null;
      }
      else {
        switch (zzAction < 0 ? zzAction : ZZ_ACTION[zzAction]) {
          case 1:
            { return BAD_CHARACTER;
            }
          // fall through
          case 47: break;
          case 2:
            { return WHITE_SPACE;
            }
          // fall through
          case 48: break;
          case 3:
            { return NOT_OP;
            }
          // fall through
          case 49: break;
          case 4:
            { return AMPERSAND;
            }
          // fall through
          case 50: break;
          case 5:
            { return LPAREN;
            }
          // fall through
          case 51: break;
          case 6:
            { return RPAREN;
            }
          // fall through
          case 52: break;
          case 7:
            { return MUL;
            }
          // fall through
          case 53: break;
          case 8:
            { return PLUS;
            }
          // fall through
          case 54: break;
          case 9:
            { return COMMA;
            }
          // fall through
          case 55: break;
          case 10:
            { return MINUS;
            }
          // fall through
          case 56: break;
          case 11:
            { return DOT;
            }
          // fall through
          case 57: break;
          case 12:
            { return DIV;
            }
          // fall through
          case 58: break;
          case 13:
            { return NUMBER;
            }
          // fall through
          case 59: break;
          case 14:
            { return COLON;
            }
          // fall through
          case 60: break;
          case 15:
            { return SEMICOLON;
            }
          // fall through
          case 61: break;
          case 16:
            { return LESS;
            }
          // fall through
          case 62: break;
          case 17:
            { return EQ;
            }
          // fall through
          case 63: break;
          case 18:
            { return GREATER;
            }
          // fall through
          case 64: break;
          case 19:
            { return IDENTIFIER;
            }
          // fall through
          case 65: break;
          case 20:
            { return LBRACE;
            }
          // fall through
          case 66: break;
          case 21:
            { return RBRACE;
            }
          // fall through
          case 67: break;
          case 22:
            { return NOT_EQUAL;
            }
          // fall through
          case 68: break;
          case 23:
            { return DOUBLE_QUOTED_STRING;
            }
          // fall through
          case 69: break;
          case 24:
            { return AND_OP;
            }
          // fall through
          case 70: break;
          case 25:
            { return SINGLE_QUOTED_STRING;
            }
          // fall through
          case 71: break;
          case 26:
            { return LESS_EQUAL;
            }
          // fall through
          case 72: break;
          case 27:
            { return NOT_EQUAL_ALT;
            }
          // fall through
          case 73: break;
          case 28:
            { return EQUAL_EQUAL;
            }
          // fall through
          case 74: break;
          case 29:
            { return GREATER_EQUAL;
            }
          // fall through
          case 75: break;
          case 30:
            { return IF_KEYWORD;
            }
          // fall through
          case 76: break;
          case 31:
            { return OR_OP;
            }
          // fall through
          case 77: break;
          case 32:
            { return IIF_KEYWORD;
            }
          // fall through
          case 78: break;
          case 33:
            { return VAR_KEYWORD;
            }
          // fall through
          case 79: break;
          case 34:
            { return GT_ENTITY;
            }
          // fall through
          case 80: break;
          case 35:
            { return LT_ENTITY;
            }
          // fall through
          case 81: break;
          case 36:
            { return JAVA_PATH_IDENTIFIER;
            }
          // fall through
          case 82: break;
          case 37:
            { return ELSE_KEYWORD;
            }
          // fall through
          case 83: break;
          case 38:
            { return TRUE_KEYWORD;
            }
          // fall through
          case 84: break;
          case 39:
            { return AMP_ENTITY;
            }
          // fall through
          case 85: break;
          case 40:
            { return FALSE_KEYWORD;
            }
          // fall through
          case 86: break;
          case 41:
            { return WHILE_KEYWORD;
            }
          // fall through
          case 87: break;
          case 42:
            { return PARENT_KEYWORD;
            }
          // fall through
          case 88: break;
          case 43:
            { return MACRO_IDENTIFIER;
            }
          // fall through
          case 89: break;
          case 44:
            { return CONTAINER_KEYWORD;
            }
          // fall through
          case 90: break;
          case 45:
            { return AND_OP_ENTITY;
            }
          // fall through
          case 91: break;
          case 46:
            { return CONFIRM_MSG;
            }
          // fall through
          case 92: break;
          default:
            zzScanError(ZZ_NO_MATCH);
          }
      }
    }
  }


}
