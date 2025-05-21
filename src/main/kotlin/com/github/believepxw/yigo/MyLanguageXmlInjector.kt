package com.github.believepxw.yigo;

import com.github.believepxw.yigo.example.MyLanguage;
import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlText;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class MyLanguageXmlInjector implements MultiHostInjector {
    @Override
    public void getLanguagesToInject(@NotNull MultiHostRegistrar registrar, @NotNull PsiElement context) {
        if (!(context instanceof XmlText)) return;

        PsiElement parent = context.getParent();
        if (!(parent instanceof XmlTag)) return;

        XmlTag tag = (XmlTag) parent;

        // 你可以通过标签名或其他逻辑决定是否注入
        if ("script".equals(tag.getName())) {
            registrar
                    .startInjecting(MyLanguage.INSTANCE)
                    .addPlace(null, null, (PsiLanguageInjectionHost) context, TextRange.from(0, context.getTextLength()))
                    .doneInjecting();
        }
    }

    @NotNull
    @Override
    public List<Class<? extends PsiElement>> elementsToInjectIn() {
        // CDATA 的内容最终是 XmlText 或 XmlCDATA
        return Collections.singletonList(XmlText.class);
    }
}