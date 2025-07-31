package example.completion.func;

import com.intellij.json.psi.JsonFile;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonMethodParser {
    public static Map<String, MethodDetails> parseMethods(Project project) {
        // 这里放入之前 JsonFileReader 中解析 JSON 的逻辑
        // 示例：从 "frontFuns.json" 读取并解析
        @NotNull PsiFile[] psiFiles = FilenameIndex.getFilesByName(project, "frontFuns.json", GlobalSearchScope.projectScope(project));

        for (PsiFile file : psiFiles) {
            if (file instanceof JsonFile) {
                JsonFile jsonFile = (JsonFile) file;
                // 这里是之前 processJsonFile 方法中的逻辑，需要适配为返回 MethodDetails 列表
                // 为简化示例，假设你已经将解析逻辑封装好并返回 MethodDetails 列表
                // 实际生产代码需要将 JsonFileReader 的 processJsonFile 方法适配成返回值的形式
                return extractMethodsFromJsonFile(jsonFile);
            }
        }
        return new HashMap<>();
    }

    // 适配 JsonFileReader 中的解析逻辑以返回 List<MethodDetails>
    private static Map<String, MethodDetails> extractMethodsFromJsonFile(@NotNull JsonFile jsonFile) {
        Map<String, MethodDetails> methods = new HashMap<>();
        if (jsonFile.getTopLevelValue() instanceof com.intellij.json.psi.JsonObject) {
            com.intellij.json.psi.JsonObject rootObject = (com.intellij.json.psi.JsonObject) jsonFile.getTopLevelValue();
            for (com.intellij.json.psi.JsonProperty methodProperty : rootObject.getPropertyList()) {
                String methodName = methodProperty.getName();
                com.intellij.json.psi.JsonValue methodDetailsValue = methodProperty.getValue();

                if (methodDetailsValue instanceof com.intellij.json.psi.JsonObject) {
                    com.intellij.json.psi.JsonObject methodDetailsObject = (com.intellij.json.psi.JsonObject) methodDetailsValue;
                    MethodDetails details = new MethodDetails();
                    details.setTemplate(methodName); // 默认使用外部方法签名作为key

                    for (com.intellij.json.psi.JsonProperty detailProperty : methodDetailsObject.getPropertyList()) {
                        String detailName = detailProperty.getName();
                        com.intellij.json.psi.JsonValue detailValue = detailProperty.getValue();

                        if (detailValue instanceof com.intellij.json.psi.JsonStringLiteral) {
                            String value = ((com.intellij.json.psi.JsonStringLiteral) detailValue).getValue();
                            switch (detailName) {
                                case "desc":
                                    details.setDesc(value);
                                    break;
                                case "descDetail":
                                    details.setDescDetail(value);
                                    break;
                                case "key":
                                    details.setKey(value);
                                    break; // 更新为内部的key
                                case "returnValueDesc":
                                    details.setReturnValueDesc(value);
                                    break;
                                case "returnValueType":
                                    details.setReturnValueType(value);
                                    break;
                            }
                        } else if (detailValue instanceof com.intellij.json.psi.JsonArray && detailName.equals("paras")) {
                            com.intellij.json.psi.JsonArray parasArray = (com.intellij.json.psi.JsonArray) detailValue;
                            List<ParameterInfo> parameterList = new ArrayList<>();
                            for (com.intellij.json.psi.JsonValue paraValue : parasArray.getValueList()) {
                                if (paraValue instanceof com.intellij.json.psi.JsonObject) {
                                    com.intellij.json.psi.JsonObject paraObject = (com.intellij.json.psi.JsonObject) paraValue;
                                    ParameterInfo param = new ParameterInfo();
                                    for (com.intellij.json.psi.JsonProperty paramProperty : paraObject.getPropertyList()) {
                                        String paramName = paramProperty.getName();
                                        com.intellij.json.psi.JsonValue paramVal = paramProperty.getValue();
                                        if (paramVal instanceof com.intellij.json.psi.JsonStringLiteral) {
                                            String val = ((com.intellij.json.psi.JsonStringLiteral) paramVal).getValue();
                                            switch (paramName) {
                                                case "desc":
                                                    param.setDesc(val);
                                                    break;
                                                case "key":
                                                    param.setKey(val);
                                                    break;
                                                case "optional":
                                                    param.setOptional(val);
                                                    break;
                                                case "type":
                                                    param.setType(val);
                                                    break;
                                            }
                                        }
                                    }
                                    parameterList.add(param);
                                }
                            }
                            details.setParas(parameterList);
                        }
                    }
                    if (!methods.containsKey(details.getTemplate())) {
                        if (!isChinese(details.getTemplate().charAt(0))) {
                            methods.put(details.getTemplate(), details);
                        }
                    }
                }
            }
        }
        return methods;
    }
    public static boolean isChinese(char ch) {
        return ch >= '\u4e00' && ch <= '\u9fa5';
    }
}