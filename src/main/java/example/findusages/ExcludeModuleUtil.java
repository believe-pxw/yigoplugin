package example.findusages;

import com.intellij.find.findUsages.FindUsagesOptions;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExcludeModuleUtil {

    static List<String> modules = new ArrayList<>(Arrays.asList("erp-entity-biz-appconfig",
            "erp-entity-biz-archiveconfig",
            "erp-entity-biz-authorityconfig",
            "erp-entity-biz-basisconfig",
            "erp-entity-biz-bcconfig",
            "erp-entity-biz-bk_basic",
            "erp-entity-biz-bokedee_management_config",
            "erp-entity-biz-cmconfig",
            "erp-entity-biz-coconfig",
            "erp-entity-biz-common",
            "erp-entity-biz-copaconfig",
            "erp-entity-biz-dmconfig",
            "erp-entity-biz-ficonfig",
            "erp-entity-biz-fmconfig",
            "erp-entity-biz-hrconfig",
            "erp-entity-biz-imconfig",
            "erp-entity-biz-internal_sds",
            "erp-entity-biz-mmconfig",
            "erp-entity-biz-pmconfig",
            "erp-entity-biz-ppconfig",
            "erp-entity-biz-psconfig",
            "erp-entity-biz-qmconfig",
            "erp-entity-biz-sdconfig",
            "erp-entity-biz-solutions",
            "erp-entity-biz-srmconfig",
            "erp-entity-biz-tcmconfig",
            "erp-entity-biz-tmconfig",
            "erp-entity-biz-wmsconfig"
    ));

    public static GlobalSearchScope getSearchScope(@NotNull PsiElement element) {
        GlobalSearchScope projectSearchScope = GlobalSearchScope.projectScope(element.getProject());
        ModuleManager moduleManager = ModuleManager.getInstance(element.getProject());
        com.intellij.openapi.module.Module moduleByName = moduleManager.findModuleByName("erp-entity-core");
        if (moduleByName != null) {
            GlobalSearchScope projectScope = projectSearchScope.intersectWith(GlobalSearchScope.notScope(GlobalSearchScope.moduleScope(moduleByName)));
            return projectScope;
        }
        moduleByName = moduleManager.findModuleByName("erp-entity-business");
        if (moduleByName != null) {
            SearchScope searchScope = projectSearchScope;
            for (String module : modules) {
                Module moduleByName1 = moduleManager.findModuleByName(module);
                if (moduleByName1 != null) {
                    GlobalSearchScope moduleScope = GlobalSearchScope.notScope(GlobalSearchScope.moduleScope(moduleByName1));
                    searchScope = searchScope.intersectWith(moduleScope);
                }
            }
            return (GlobalSearchScope) searchScope;
        }
        return projectSearchScope;
    }

    public static void enhanceFindUsagesOptions(@NotNull PsiElement element, @NotNull FindUsagesOptions options) {
        ModuleManager moduleManager = ModuleManager.getInstance(element.getProject());
        com.intellij.openapi.module.Module moduleByName = moduleManager.findModuleByName("erp-entity-core");
        if (moduleByName != null) {
            GlobalSearchScope globalSearchScope = GlobalSearchScope.notScope(GlobalSearchScope.moduleScope(moduleByName));
            options.searchScope = options.searchScope.intersectWith(globalSearchScope);
            return;
        }
        moduleByName = moduleManager.findModuleByName("erp-entity-business");
        if (moduleByName != null) {
            SearchScope searchScope = options.searchScope;
            for (String module : modules) {
                Module moduleByName1 = moduleManager.findModuleByName(module);
                if (moduleByName1 != null) {
                    GlobalSearchScope moduleScope = GlobalSearchScope.notScope(GlobalSearchScope.moduleScope(moduleByName1));
                    searchScope = searchScope.intersectWith(moduleScope);
                }
            }
            options.searchScope = searchScope;
        }
    }
}
