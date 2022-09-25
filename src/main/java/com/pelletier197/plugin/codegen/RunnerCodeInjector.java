package com.pelletier197.plugin.codegen;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiModifierList;
import com.pelletier197.plugin.codegen.utils.MockitoPluginUtils;

/**
 * Inserts annotation MockitoJUnitRunner.class annotation for the test.
 *
 * Created by przemek on 8/8/15.
 */
public class RunnerCodeInjector implements CodeInjector {

    public static final String MOCKITO_EXTENSION_QUALIFIED_CLASS_NAME = "org.mockito.junit.jupiter.MockitoExtension";
    public static final String MOCKITO_EXTENSION_SHORT_CLASS_NAME = "MockitoExtension";
    public static final String EXTEND_WITH_SHORT_CLASS_NAME = "ExtendWith";
    public static final String EXTEND_WITH_QUALIFIED_CLASS_NAME = "org.junit.jupiter.api.extension." + EXTEND_WITH_SHORT_CLASS_NAME;

    private final PsiJavaFile psiJavaFile;
    private final ImportOrganizer importOrganizer;

    public RunnerCodeInjector(PsiJavaFile psiJavaFile, ImportOrganizer importOrganizer) {
        this.psiJavaFile = psiJavaFile;
        this.importOrganizer = importOrganizer;
    }

    public void inject() {
        PsiClass psiClass = MockitoPluginUtils.getUnitTestClass(psiJavaFile);
        PsiModifierList modifierList = psiClass.getModifierList();
        if (!containsRunnerAnnotation(modifierList)) {
            modifierList.addAnnotation(String.format("%s(%s.class)", EXTEND_WITH_SHORT_CLASS_NAME, MOCKITO_EXTENSION_SHORT_CLASS_NAME));
            importOrganizer.addClassImport(psiJavaFile, MOCKITO_EXTENSION_QUALIFIED_CLASS_NAME);
            importOrganizer.addClassImport(psiJavaFile, EXTEND_WITH_QUALIFIED_CLASS_NAME);
        }
    }

    private boolean containsRunnerAnnotation(PsiModifierList modifierList) {
        for (PsiAnnotation psiAnnotation : modifierList.getAnnotations()) {
            if (psiAnnotation.getQualifiedName().endsWith(EXTEND_WITH_SHORT_CLASS_NAME)) {
                return true;
            }
        }
        return false;
    }

}
