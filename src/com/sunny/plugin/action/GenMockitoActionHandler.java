package com.sunny.plugin.action;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.sunny.plugin.codegen.FieldsCodeInjector;
import com.sunny.plugin.codegen.ImportOrganizer;
import com.sunny.plugin.codegen.NestedInnerTestClassInjector;
import com.sunny.plugin.codegen.RunnerCodeInjector;
import com.sunny.plugin.codegen.StaticImportsInjector;
import com.sunny.plugin.codegen.utils.CommonCodeInjector;

public class GenMockitoActionHandler extends EditorWriteActionHandler {

  @Override
  public void executeWriteAction(Editor editor, Caret caret, DataContext dataContext) {
    PsiJavaFile psiJavaFile = (PsiJavaFile) dataContext.getData(CommonDataKeys.PSI_FILE.getName());
    Project project = psiJavaFile.getProject();
    JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
    ImportOrganizer importOrganizer = new ImportOrganizer(JavaPsiFacade.getInstance(project));
    JavaCodeStyleManager codeStyleManager = JavaCodeStyleManager.getInstance(project);
    CommonCodeInjector commonCodeInjector = new CommonCodeInjector(javaPsiFacade, psiJavaFile, importOrganizer, codeStyleManager);


    RunnerCodeInjector runnerCodeInjector = new RunnerCodeInjector(psiJavaFile, importOrganizer);
    runnerCodeInjector.inject();
    FieldsCodeInjector fieldsCodeInjector = new FieldsCodeInjector(psiJavaFile, importOrganizer, commonCodeInjector);
    fieldsCodeInjector.inject();
    NestedInnerTestClassInjector innerTestClassInjector = new NestedInnerTestClassInjector(psiJavaFile, importOrganizer, commonCodeInjector);
    innerTestClassInjector.inject();
    StaticImportsInjector staticImportsInjector = new StaticImportsInjector(psiJavaFile, importOrganizer);
    staticImportsInjector.inject();
  }
}
