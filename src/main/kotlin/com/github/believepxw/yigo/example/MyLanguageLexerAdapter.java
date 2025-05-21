package com.github.believepxw.yigo.example;

import com.github.believepxw.yigo.example.parser._MyLanguageLexer;
import com.intellij.lexer.FlexAdapter;

public class MyLanguageLexerAdapter extends FlexAdapter {
    public MyLanguageLexerAdapter() {
        super(new _MyLanguageLexer(null)); // _MyLanguageLexer 可能需要一个 Reader 参数，通常传 null
    }
}
