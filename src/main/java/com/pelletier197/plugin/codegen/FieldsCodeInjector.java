package com.pelletier197.plugin.codegen;

import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.pelletier197.plugin.codegen.utils.CommonCodeInjector;
import com.pelletier197.plugin.codegen.utils.MockitoPluginUtils;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Inserts code with declaration of fields that can be auto-generated in a Mockito test:
 * - mocked fields
 * - subject of the test
 * <p>
 * Mocked fields are inserted for each non-static object defined in the tested class. So far the fields
 * declared in parent of the tested class are ignored. Example of the code generated for an object of type ClassName:
 * <code>
 *
 * @Mock private ClassName className;
 * </code>
 * <p>
 * Field for a subject of the test has format:
 * <code>
 * @InjectMocks private SubjectClassName underTest;
 * </code>
 * <p>
 * Created by przemek on 8/9/15.
 */
public class FieldsCodeInjector implements CodeInjector {

  public static final String TEST_CLASS_NAME_SUFFIX = "Test";
  public static final String INJECT_MOCKS_CLASS_NAME = "InjectMocks";
  public static final String INJECT_MOCKS_ANNOTATION_QUALIFIED_NAME = "org.mockito.InjectMocks";

  public static final String UNDER_TEST_FIELD_NAME = "underTest";


  private final PsiJavaFile psiJavaFile;
  private final Project project;
  private final JavaPsiFacade javaPsiFacade;
  private final ImportOrganizer importOrganizer;
  private final CommonCodeInjector commonCodeInjector;

  public FieldsCodeInjector(PsiJavaFile psiJavaFile, ImportOrganizer importOrganizer, CommonCodeInjector commonCodeInjector) {
    this.psiJavaFile = psiJavaFile;
    this.project = psiJavaFile.getProject();
    this.commonCodeInjector = commonCodeInjector;
    this.javaPsiFacade = JavaPsiFacade.getInstance(project);
    this.importOrganizer = importOrganizer;
  }

  @Override
  public void inject() {
    PsiClass psiClass = MockitoPluginUtils.getUnitTestClass(psiJavaFile);

    String underTestQualifiedClassName = getUnderTestQualifiedClassName(psiClass);
    if (underTestQualifiedClassName == null) {
      return;
    }

    insertMockedFields(psiClass,underTestQualifiedClassName);
    insertUnderTestField(psiClass, underTestQualifiedClassName);
  }

  private void insertUnderTestField(PsiClass testClass, String underTestQualifiedClassName) {
    Set<String> existingFields = Stream.of(testClass.getFields()).map(NavigationItem::getName).collect(Collectors.toSet());
    if (!existingFields.contains(UNDER_TEST_FIELD_NAME)) {
      PsiClassType subjectClassType = PsiType.getTypeByName(underTestQualifiedClassName, project, GlobalSearchScope.projectScope(project));

      commonCodeInjector.insertNewField(testClass, subjectClassType, UNDER_TEST_FIELD_NAME, INJECT_MOCKS_CLASS_NAME);
      importOrganizer.addClassImport(psiJavaFile, INJECT_MOCKS_ANNOTATION_QUALIFIED_NAME);
    }
  }

  private void insertMockedFields(PsiClass testClass, String underTestQualifiedClassName) {
    PsiClass underTestPsiClass = javaPsiFacade.findClass(underTestQualifiedClassName, GlobalSearchScope.allScope(project));
    if (underTestPsiClass == null) {
      return;
    }

    commonCodeInjector.insertAllMissingMocksForFieldsOfClassIntoTestClass(testClass, underTestPsiClass);
  }

  private String getUnderTestQualifiedClassName(PsiClass testClass) {
    String testClassName = testClass.getQualifiedName();
    if (testClassName.endsWith(TEST_CLASS_NAME_SUFFIX)) {
      return testClassName.substring(0, testClassName.length() - TEST_CLASS_NAME_SUFFIX.length());
    }
    return null;
  }
}
