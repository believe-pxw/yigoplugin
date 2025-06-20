package example.formatting;
import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.TokenType;
import com.intellij.psi.formatter.common.AbstractBlock;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import example.psi.MyLanguageTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MyLanguageBlock extends AbstractBlock {

    private final SpacingBuilder spacingBuilder;
    private final Indent myIndent;

    // 定義哪些元素內部的內容需要縮進
    private static final TokenSet INDENTED_BLOCKS = TokenSet.create(
            MyLanguageTypes.BLOCK_STATEMENT,
            MyLanguageTypes.CODE_BLOCK_LITERAL,
            MyLanguageTypes.OBJECT_LITERAL_CONTENT,
            MyLanguageTypes.CALLBACK_OBJECT
    );

    protected MyLanguageBlock(@NotNull ASTNode node, @Nullable Wrap wrap, @Nullable Alignment alignment,
                              SpacingBuilder spacingBuilder) {
        this(node, wrap, alignment, spacingBuilder, Indent.getNoneIndent());
    }

    protected MyLanguageBlock(@NotNull ASTNode node, @Nullable Wrap wrap, @Nullable Alignment alignment,
                              SpacingBuilder spacingBuilder, @NotNull Indent indent) {
        super(node, wrap, alignment);
        this.spacingBuilder = spacingBuilder;
        this.myIndent = indent;
    }

    @Override
    public Indent getIndent() {
        return myIndent;
    }

    @Nullable
    @Override
    public Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
        return spacingBuilder.getSpacing(this, child1, child2);
    }

    @Override
    public boolean isLeaf() {
        return myNode.getFirstChildNode() == null;
    }

    @Override
    public @NotNull ChildAttributes getChildAttributes(int newChildIndex) {
        // 當在一個塊中按下回車時，新行的縮進是什麼
        if (INDENTED_BLOCKS.contains(myNode.getElementType())) {
            return new ChildAttributes(Indent.getNormalIndent(), null);
        }
        // 對於 if(condition) single_statement 的情況
        if (myNode.getElementType() == MyLanguageTypes.IF_STATEMENT || myNode.getElementType() == MyLanguageTypes.WHILE_STATEMENT) {
            return new ChildAttributes(Indent.getNormalIndent(), null);
        }
        return new ChildAttributes(Indent.getNoneIndent(), null);
    }
    // 辅助方法：检查是否为 IIFS 函数调用
    private boolean isIIFSFunctionCall(ASTNode node) {
        if (node.getElementType() == MyLanguageTypes.REGULAR_FUNCTION_CALL) {
            ASTNode nameNode = node.findChildByType(MyLanguageTypes.IDENTIFIER);
            if (nameNode != null) {
                return "IIFS".equals(nameNode.getText());
            }
        }
        return false;
    }

    // 辅助方法：检查是否为 IIFS 函数调用
    private boolean isIIFFunctionCall(ASTNode node) {
        if (node.getElementType() == MyLanguageTypes.REGULAR_FUNCTION_CALL) {
            ASTNode nameNode = node.findChildByType(MyLanguageTypes.IIF_FUNCTION_CALL);
            if (nameNode != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * 核心修正：為子塊分配正確的縮進和換行
     */
    @Override
    protected List<Block> buildChildren() {
        List<Block> blocks = new ArrayList<>();
        ASTNode child = myNode.getFirstChildNode();

        // 檢查父節點是否是需要縮進其子節點的類型
        boolean isParentIndentedBlock = INDENTED_BLOCKS.contains(myNode.getElementType());

        while (child != null) {
            IElementType childType = child.getElementType();
            if (childType != TokenType.WHITE_SPACE) {
                Indent indent = Indent.getNoneIndent();
                Wrap wrap = Wrap.createWrap(WrapType.NONE, false);

                // --- 規則 1: 處理塊內部的語句 ---
                if (isIIFSFunctionCall(myNode)) {
                    return buildIIFSChildren();
                }

                if (isIIFFunctionCall(myNode)) {
                    return buildIIFChildren();
                }

                if (isParentIndentedBlock) {
                    // 忽略開頭的 { 和結尾的 }
                    if (childType != MyLanguageTypes.LBRACE && childType != MyLanguageTypes.RBRACE) {
                        indent = Indent.getNormalIndent();
                        // 只对完整语句的开始进行换行，而不是每个token
                        if (isStatementStart(child)) {
                            wrap = Wrap.createWrap(WrapType.ALWAYS, true);
                        } else {
                            wrap = Wrap.createWrap(WrapType.NONE, false);
                        }
                    }
                }

                // --- 規則 2: 處理 if/while 後沒有大括號的單個語句 ---
                IElementType parentType = myNode.getElementType();
                if ((parentType == MyLanguageTypes.IF_STATEMENT || parentType == MyLanguageTypes.WHILE_STATEMENT) &&
                        childType == MyLanguageTypes.STATEMENT_BLOCK) {
                    // 如果 statement_block 不包含 block_statement (即沒有 {}), 則其內容需要縮進
                    if(child.findChildByType(MyLanguageTypes.BLOCK_STATEMENT) == null) {
                        indent = Indent.getNormalIndent();
                        wrap = Wrap.createWrap(WrapType.ALWAYS, true);
                    }
                }

                blocks.add(new MyLanguageBlock(child, wrap, null, spacingBuilder, indent));
            }
            child = child.getTreeNext();
        }
        return blocks;
    }

    private boolean isStatementStart(ASTNode node) {
        // 检查这个节点是否是一个语句的开始
        // 而不是语句内部的token（如分号）
        IElementType type = node.getElementType();
        return type == MyLanguageTypes.VARIABLE_ASSIGNMENT ||
                type == MyLanguageTypes.IF_STATEMENT ||
                type == MyLanguageTypes.WHILE_STATEMENT ||
                type == MyLanguageTypes.FUNCTION_CALL;
    }

    private List<Block> buildIIFChildren() {
        List<Block> blocks = new ArrayList<>();
        ASTNode child = myNode.getFirstChildNode();

        while (child != null) {
            IElementType childType = child.getElementType();
            if (childType != TokenType.WHITE_SPACE) {
                if (childType == MyLanguageTypes.ARGUMENT_LIST) {
                    Indent indent = Indent.getNoneIndent();
                    ASTNode next = child.getFirstChildNode();
                    boolean first = true;
                    while (next != null) {
                        childType = next.getElementType();
                        if (childType != TokenType.WHITE_SPACE) {
                            Wrap wrap = Wrap.createWrap(WrapType.NONE, false);
                            if (childType == MyLanguageTypes.COMMA) {
                                wrap = Wrap.createWrap(WrapType.ALWAYS, true);
                            } else if (isArgument(next)) {
                                // 新行的参数需要缩进
                                indent = Indent.getContinuationIndent();
                            }
                            if (first) {
                                wrap = Wrap.createWrap(WrapType.ALWAYS, true);
                                first = false;
                            }
                            blocks.add(new MyLanguageBlock(next, wrap, null, spacingBuilder, indent));
                        }
                        next = next.getTreeNext();
                    }
                } else {
                    Indent indent = Indent.getNoneIndent();
                    Wrap wrap = Wrap.createWrap(WrapType.NONE, false);
                    if (childType == MyLanguageTypes.RPAREN) {
                        wrap = Wrap.createWrap(WrapType.ALWAYS, true);
                    }
                    blocks.add(new MyLanguageBlock(child, wrap, null, spacingBuilder, indent));
                }
            }
            child = child.getTreeNext();
        }
        return blocks;
    }

    private List<Block> buildIIFSChildren() {
        List<Block> blocks = new ArrayList<>();
        ASTNode child = myNode.getFirstChildNode();
        int argumentCount = 0;

        while (child != null) {
            IElementType childType = child.getElementType();
            if (childType != TokenType.WHITE_SPACE) {
                if (childType == MyLanguageTypes.ARGUMENT_LIST) {
                    Indent indent = Indent.getNoneIndent();
                    ASTNode next = child.getFirstChildNode();
                    boolean first = true;
                    while (next != null) {
                        childType = next.getElementType();
                        if (childType != TokenType.WHITE_SPACE) {
                            Wrap wrap = Wrap.createWrap(WrapType.NONE, false);
                            if (childType == MyLanguageTypes.COMMA) {
                                argumentCount++;
                                // 每两个参数后的逗号换行
                                if (argumentCount % 2 == 0) {
                                    wrap = Wrap.createWrap(WrapType.ALWAYS, true);
                                }
                            } else if (isArgument(next) && argumentCount % 2 == 0) {
                                // 新行的参数需要缩进
                                indent = Indent.getContinuationIndent();
                            }
                            if (first) {
                                wrap = Wrap.createWrap(WrapType.ALWAYS, true);
                                first = false;
                            }
                            blocks.add(new MyLanguageBlock(next, wrap, null, spacingBuilder, indent));
                        }
                        next = next.getTreeNext();
                    }
                }else {
                    Indent indent = Indent.getNoneIndent();
                    Wrap wrap = Wrap.createWrap(WrapType.NONE, false);
                    if (childType == MyLanguageTypes.RPAREN) {
                        wrap = Wrap.createWrap(WrapType.ALWAYS, true);
                    }
                    blocks.add(new MyLanguageBlock(child, wrap, null, spacingBuilder, indent));
                }
            }
            child = child.getTreeNext();
        }
        return blocks;
    }

    private boolean isArgument(ASTNode node) {
        // 判断节点是否为函数参数
        return node.getElementType() == MyLanguageTypes.EXPRESSION ||
                node.getElementType() == MyLanguageTypes.FUNCTION_CALL ||
                node.getElementType() == MyLanguageTypes.IDENTIFIER;
    }
}