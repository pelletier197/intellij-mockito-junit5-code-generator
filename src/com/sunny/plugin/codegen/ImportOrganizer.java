package com.sunny.plugin.codegen;

import com.intellij.openapi.editor.Editor;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiImportStaticStatement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import com.intellij.refactoring.util.CommonRefactoringUtil;

/**
 * Created by przemek on 8/9/15.
 */
public class ImportOrganizer {

  private final JavaPsiFacade javaPsiFacade;
  private final GlobalSearchScope projectSearchScope;

  public ImportOrganizer(JavaPsiFacade javaPsiFacade) {
    this.javaPsiFacade = javaPsiFacade;
    this.projectSearchScope = ProjectScope.getAllScope(javaPsiFacade.getProject());
  }

  public void addClassImport(PsiJavaFile psiJavaFile, String className) {
    PsiClass psiClass = javaPsiFacade.findClass(className, projectSearchScope);
    if (psiClass == null) {
      return;
    }

    psiJavaFile.importClass(psiClass);
  }

  protected void addStaticImportForAllMethods(PsiJavaFile psiJavaFile, String className) {
    PsiClass psiClass = javaPsiFacade.findClass(className, projectSearchScope);
    if (psiClass == null) {
      return;
    }
    PsiImportStaticStatement importStaticStatement = javaPsiFacade.getElementFactory().createImportStaticStatement(psiClass, "*");
    psiJavaFile.getImportList().add(importStaticStatement);
  }

  protected void addStaticImport(PsiJavaFile psiJavaFile, String importClassName, String importSubElementName) {
    PsiClass psiClass = javaPsiFacade.findClass(importClassName, projectSearchScope);
    if (psiClass == null) {
      return;
    }

    PsiImportStaticStatement importStaticStatement = javaPsiFacade.getElementFactory().createImportStaticStatement(psiClass, importSubElementName);
    psiJavaFile.getImportList().add(importStaticStatement);
  }
}
