package example.index;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.indexing.DataIndexer;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileBasedIndexExtension;
import com.intellij.util.indexing.FileContent;
import com.intellij.util.indexing.ID;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.intellij.util.io.VoidDataExternalizer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static example.index.AttrConstant.*;
import static example.index.AttrConstant.CAPTION_ATTRIBUTE;
import static example.index.AttrConstant.KEY_ATTRIBUTE;

// 宏名称索引
public class DataElementIndex extends FileBasedIndexExtension<String, Void> {

    // 唯一的索引ID
    public static final ID<String, Void> KEY = ID.create("DataElementIndex");

    @Override
    public @NotNull ID<String, Void> getName() {
        return KEY;
    }

    @Override
    public @NotNull DataIndexer<String, Void, FileContent> getIndexer() {
        // DataIndexer 定义了如何从文件中提取键值对
        return new DataIndexer<String, Void, FileContent>() {
            @Override
            public @NotNull Map<String, Void> map(@NotNull FileContent inputData) {
                PsiFile psiFile = inputData.getPsiFile();
                if (!(psiFile instanceof XmlFile)) {
                    return Collections.emptyMap();
                }
                Map<String, Void> map = new HashMap<>();
                XmlFile dataElementFile = (XmlFile) psiFile;
                XmlTag rootTag = dataElementFile.getRootTag();
                if (rootTag != null && DATA_ELEMENT_DEF_TAG.equals(rootTag.getName())) {
                    XmlTag collectionTag = rootTag.findFirstSubTag(DATA_ELEMENT_COLLECTION_TAG);
                    if (collectionTag != null) {
                        for (XmlTag dataElementTag : collectionTag.findSubTags(DATA_ELEMENT_TAG)) {
                            String key = dataElementTag.getAttributeValue(KEY_ATTRIBUTE);
                            if (key != null) {
                                map.put(key, null);
                            }
                        }
                    }
                }
                return map;
            }
        };
    }

    @Override
    public @NotNull KeyDescriptor<String> getKeyDescriptor() {
        // 定义键的序列化/反序列化器
        return EnumeratorStringDescriptor.INSTANCE;
    }

    @Override
    public @NotNull DataExternalizer<Void> getValueExternalizer() {
        return VoidDataExternalizer.INSTANCE;
    }

    @Override
    public @NotNull FileBasedIndex.InputFilter getInputFilter() {
        // 告诉索引器只处理 XML 文件
        return new FileBasedIndex.InputFilter() {
            @Override
            public boolean acceptInput(@NotNull VirtualFile file) {
                return file.getFileType() == XmlFileType.INSTANCE && file.getPath().contains("/DataElement/");
            }
        };
    }

    @Override
    public boolean dependsOnFileContent() {
        // 索引是否依赖于文件内容？是，因为它需要解析XML来提取宏名称。
        return true;
    }

    @Override
    public int getVersion() {
        // 索引的版本号。如果索引逻辑或结构改变，需要增加版本号以触发索引重建。
        return 1;
    }

    // --- Helper Method to Query the Index ---
    /**
     * 在项目中查找指定宏名称的定义。
     * @param project 当前项目
     * @param dataElementKey 要查找的宏名称
     * @return 匹配的 XmlTag（宏定义）如果找到，否则返回null。
     */
    @Nullable
    public static XmlTag findDEDefinition(Project project, String dataElementKey) {
        // 使用 GlobalSearchScope.allScope(project) 在整个项目范围内查找
        // FileBasedIndex.getInstance().getContainingFiles(KEY, macroName, scope) 返回包含指定键的所有文件
        Collection<VirtualFile> files = FileBasedIndex.getInstance().getContainingFiles(
                KEY, dataElementKey, GlobalSearchScope.projectScope(project));

        for (VirtualFile file : files) {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            XmlFile dataElementFile = (XmlFile) psiFile;
            XmlTag rootTag = dataElementFile.getRootTag();
            if (rootTag != null && DATA_ELEMENT_DEF_TAG.equals(rootTag.getName())) {
                XmlTag collectionTag = rootTag.findFirstSubTag(DATA_ELEMENT_COLLECTION_TAG);
                if (collectionTag != null) {
                    for (XmlTag dataElementTag : collectionTag.findSubTags(DATA_ELEMENT_TAG)) {
                        String key = dataElementTag.getAttributeValue(KEY_ATTRIBUTE);
                        if (key != null && key.equals(dataElementKey)) {
                            return dataElementTag;
                        }
                    }
                }
            }
        }
        return null; // 未找到
    }
}