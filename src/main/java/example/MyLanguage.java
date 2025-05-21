package example;

import com.intellij.lang.Language;

public class MyLanguage extends Language {
    public static final MyLanguage INSTANCE = new MyLanguage();

    protected MyLanguage() {
        super("MyLanguage");
    }
}
