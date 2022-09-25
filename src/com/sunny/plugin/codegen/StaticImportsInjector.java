package com.sunny.plugin.codegen;

import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiImportStaticStatement;
import com.intellij.psi.PsiJavaFile;

/**
 * Inserts code for static imports for Mockito.
 * <p>
 * Created by przemek on 8/10/15.
 */
public class StaticImportsInjector implements CodeInjector {

  public static final String MOCKITO_FULLY_QUALIFIED_CLASS_NAME = "org.mockito.Mockito";
  public static final String GROUPED_MOCKITO_STATIC_IMPORT = MOCKITO_FULLY_QUALIFIED_CLASS_NAME + ".*";

  public static final String ASSERTJ_ASSERTIONS_QUALIFIED_NAME = "org.assertj.core.api.Assertions";
  public static final String ASSERTJ_ASSERT_THAT_METHOD_NAME = "assertThat";
  public static final String GROUPED_ASSERTJ_STATIC_IMPORT = ASSERTJ_ASSERTIONS_QUALIFIED_NAME + "." + ASSERTJ_ASSERT_THAT_METHOD_NAME;

  private final PsiJavaFile psiJavaFile;
  private final ImportOrganizer importOrganizer;
  private final Project project;
  private final JavaPsiFacade javaPsiFacade;


  public StaticImportsInjector(PsiJavaFile psiJavaFile, ImportOrganizer importOrganizer) {
    this.psiJavaFile = psiJavaFile;
    this.importOrganizer = importOrganizer;
    this.project = psiJavaFile.getProject();
    this.javaPsiFacade = JavaPsiFacade.getInstance(project);
  }

  @Override
  public void inject() {
    addAssertJImport();
    addMockitoImport();
  }

  private void addMockitoImport() {
    for (PsiImportStaticStatement staticImport : psiJavaFile.getImportList().getImportStaticStatements()) {
      if (staticImport.getText().contains(GROUPED_MOCKITO_STATIC_IMPORT)) {
        return;
      }
    }
    importOrganizer.addStaticImportForAllMethods(psiJavaFile, MOCKITO_FULLY_QUALIFIED_CLASS_NAME);
  }

  private void addAssertJImport() {
    for (PsiImportStaticStatement staticImport : psiJavaFile.getImportList().getImportStaticStatements()) {
      if (staticImport.getText().contains(GROUPED_ASSERTJ_STATIC_IMPORT)) {
        return;
      }
    }
    importOrganizer.addStaticImport(psiJavaFile, ASSERTJ_ASSERTIONS_QUALIFIED_NAME, ASSERTJ_ASSERT_THAT_METHOD_NAME);
  }
}
