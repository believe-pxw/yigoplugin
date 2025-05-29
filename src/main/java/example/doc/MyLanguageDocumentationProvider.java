package example.doc;

import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.lang.documentation.DocumentationProvider;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import example.psi.MyLanguageTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * MyLanguage 文档提供器
 * 直接调用Java方法的现有文档，无需重新生成
 */
public class MyLanguageDocumentationProvider implements DocumentationProvider {

    @Override
    public @Nullable String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {
        // 查找对应的Java方法
        PsiMethod javaMethod = findJavaMethod(element);
        if (javaMethod != null) {
            // 直接使用Java文档提供器
            return getJavaDocumentationProvider().getQuickNavigateInfo(javaMethod, javaMethod);
        }
        return null;
    }

    @Override
    public @Nullable String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
        // 查找对应的Java方法
        PsiMethod javaMethod = findJavaMethod(element);
        if (javaMethod != null) {
            // 直接使用Java文档提供器
            return getJavaDocumentationProvider().generateDoc(javaMethod, javaMethod);
        }
        return null;
    }

    @Override
    public @Nullable PsiElement getDocumentationElementForLookupItem(PsiManager psiManager,
                                                                     Object object,
                                                                     PsiElement element) {
        // 如果是Java方法调用，返回对应的Java方法元素
        PsiMethod javaMethod = findJavaMethod(element);
        return javaMethod != null ? javaMethod : element;
    }

    @Override
    public @Nullable PsiElement getCustomDocumentationElement(@NotNull Editor editor,
                                                              @NotNull PsiFile file,
                                                              @Nullable PsiElement contextElement,
                                                              int targetOffset) {
        if (contextElement == null) {
            return null;
        }

        // 检查是否是Java路径标识符
        if (isJavaPathIdentifier(contextElement)) {
            // 查找对应的Java方法，如果找到就返回Java方法元素
            PsiMethod javaMethod = findJavaMethod(contextElement);
            return javaMethod != null ? javaMethod : contextElement;
        }

        return null;
    }

    /**
     * 检查元素是否是Java路径标识符
     */
    private boolean isJavaPathIdentifier(PsiElement element) {
        if (element == null || element.getNode() == null) {
            return false;
        }
        return element.getNode().getElementType() == MyLanguageTypes.JAVA_PATH_IDENTIFIER;
    }

    /**
     * 查找对应的Java方法
     */
    private @Nullable PsiMethod findJavaMethod(PsiElement element) {
        if (!isJavaPathIdentifier(element)) {
            return null;
        }

        String javaPath = element.getText();
        if (StringUtil.isEmpty(javaPath) || !javaPath.contains(".")) {
            return null;
        }

        // 解析Java路径
        int lastDotIndex = javaPath.lastIndexOf('.');
        String className = javaPath.substring(0, lastDotIndex);
        String methodName = javaPath.substring(lastDotIndex + 1);

        // 查找Java类
        Project project = element.getProject();
        JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
        PsiClass psiClass = javaPsiFacade.findClass(className, GlobalSearchScope.allScope(project));

        if (psiClass == null) {
            return null;
        }

        // 查找方法（返回第一个匹配的方法，如果有重载，用户可以通过导航看到所有重载）
        PsiMethod[] methods = psiClass.findMethodsByName(methodName, true);
        return methods.length > 0 ? methods[0] : null;
    }

    /**
     * 获取Java文档提供器实例
     */
    private com.intellij.lang.java.JavaDocumentationProvider getJavaDocumentationProvider() {
        return new com.intellij.lang.java.JavaDocumentationProvider();
    }

    @Override
    public @Nullable PsiElement getDocumentationElementForLink(PsiManager psiManager,
                                                               String link,
                                                               PsiElement context) {
        // 如果链接指向Java元素，让Java文档提供器处理
        PsiMethod javaMethod = findJavaMethod(context);
        if (javaMethod != null) {
            return getJavaDocumentationProvider().getDocumentationElementForLink(psiManager, link, javaMethod);
        }
        return null;
    }
/*
    @Override
    public @Nullable String fetchExternalDocumentation(Project project,
                                                       PsiElement element,
                                                       java.util.List<String> docUrls) {
        // 如果是Java方法，让Java文档提供器处理外部文档
        if (element instanceof PsiMethod) {
            return getJavaDocumentationProvider().fetchExternalDocumentation(project, element, docUrls);
        }
        return null;
    }

    @Override
    public boolean hasDocumentationFor(PsiElement element, PsiElement originalElement) {
        // 检查是否有对应的Java方法文档
        return findJavaMethod(element) != null;
    }

    @Override
    public boolean canPromptToConfigureDocumentation(PsiElement element) {
        // 如果找到了Java方法，可以配置文档
        return findJavaMethod(element) != null;
    }

    @Override
    public void promptToConfigureDocumentation(PsiElement element) {
        // 查找Java方法并让Java文档提供器处理配置
        PsiMethod javaMethod = findJavaMethod(element);
        if (javaMethod != null) {
            getJavaDocumentationProvider().promptToConfigureDocumentation(javaMethod);
        }
    }*/
}