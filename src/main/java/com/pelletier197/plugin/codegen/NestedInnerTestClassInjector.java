package com.pelletier197.plugin.codegen;

import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.pelletier197.plugin.codegen.utils.CommonCodeInjector;
import com.pelletier197.plugin.codegen.utils.MockitoPluginUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NestedInnerTestClassInjector implements CodeInjector {
  public static final List<String> TEST_CLASS_NAME_SUFFIXES = List.of("Test", "IT", "Tests", "ITs", "E2E");
  public static final String TEST_NESTED_CLASS_PREFIX = "When";
  public static final String NESTED_ANNOTATION_NAME = "Nested";
  public static final String JUNIT_PACKAGE_PREFIX = "org.junit.jupiter.api.";
  public static final String NESTED_ANNOTATION_QUALIFIED_NAME = JUNIT_PACKAGE_PREFIX + NESTED_ANNOTATION_NAME;
  public static final String SETUP_METHOD_NAME = "setup";
  public static final String BEFORE_EACH_ANNOTATION_SHORT_NAME = "BeforeEach";
  public static final String BEFORE_EACH_ANNOTATION_QUALIFIED_NAME = JUNIT_PACKAGE_PREFIX + BEFORE_EACH_ANNOTATION_SHORT_NAME;
  public static final String[] NON_HANDLED_METHOD_PREFIX = new String[]{"set", "toString", "equals", "canEqual", "hashCode"};

  private final PsiJavaFile psiJavaFile;
  private final Project project;
  private final JavaPsiFacade javaPsiFacade;
  private final ImportOrganizer importOrganizer;
  private final CommonCodeInjector commonCodeInjector;

  public NestedInnerTestClassInjector(PsiJavaFile psiJavaFile, ImportOrganizer importOrganizer, CommonCodeInjector commonCodeInjector) {
    this.psiJavaFile = psiJavaFile;
    this.project = psiJavaFile.getProject();
    this.commonCodeInjector = commonCodeInjector;
    this.javaPsiFacade = JavaPsiFacade.getInstance(project);
    this.importOrganizer = importOrganizer;
  }

  @Override
  public void inject() {
    PsiClass testClass = MockitoPluginUtils.getUnitTestClass(psiJavaFile);
    Set<String> existingWhenMethods = getExistingTestInnerClasses(testClass);

    String underTestQualifiedClassName = getUnderTestQualifiedClassName(testClass);
    if (underTestQualifiedClassName == null) {
      return;
    }

    insertMissingWhenMethods(underTestQualifiedClassName, testClass, existingWhenMethods);
  }

  private void insertMissingWhenMethods(String underTestQualifiedClassName,
                                        PsiClass testClass,
                                        Set<String> existingWhenMethods) {
    PsiClass underTestPsiClass = javaPsiFacade.findClass(underTestQualifiedClassName, GlobalSearchScope.allScope(project));
    if (underTestPsiClass == null) {
      return;
    }

    boolean addedWhenMethods = false;

    for (PsiMethod method : underTestPsiClass.getMethods()) {
      if (isVisible(method) && !method.getHierarchicalMethodSignature().isConstructor() && isHandledGeneratedMethod(method)) {

        String innerClassTestName = createWhenMethodName(method);

        PsiClass innerClass;
        boolean addedClass = false;
        if (!existingWhenMethods.contains(innerClassTestName)) {
          innerClass = createTestNestedClass(innerClassTestName);
          addedWhenMethods = true;
          addedClass = true;
        } else {
          innerClass = testClass.findInnerClassByName(innerClassTestName, false);
        }

        addMissingTestedMethodParametersMocks(method, innerClass);

        if (addedClass) {
          testClass.add(innerClass);
        }
      }
    }

    if (addedWhenMethods) {
      importOrganizer.addClassImport(psiJavaFile, NESTED_ANNOTATION_QUALIFIED_NAME);
    }
  }

  private boolean isHandledGeneratedMethod(PsiMethod method) {
    return Stream.of(NON_HANDLED_METHOD_PREFIX).noneMatch(prefix -> method.getName().startsWith(prefix))
            && !method.getClass().getName().contains("Lombok");
  }

  private PsiClass createTestNestedClass(String innerClassTestName) {
    PsiClass innerClass = javaPsiFacade.getElementFactory().createClass(innerClassTestName);

    innerClass.getModifierList().addAnnotation(NESTED_ANNOTATION_NAME);
    innerClass.getModifierList().setModifierProperty(PsiModifier.PACKAGE_LOCAL, true);

    addBeforeEachMethod(innerClass);

    return innerClass;
  }

  private void addBeforeEachMethod(PsiClass innerClass) {
    PsiMethod beforeEachMethod = javaPsiFacade.getElementFactory().createMethod(SETUP_METHOD_NAME, PsiType.VOID);
    beforeEachMethod.getModifierList().addAnnotation(BEFORE_EACH_ANNOTATION_SHORT_NAME);
    beforeEachMethod.getModifierList().setModifierProperty(PsiModifier.PACKAGE_LOCAL, true);
    innerClass.add(beforeEachMethod);

    importOrganizer.addClassImport(psiJavaFile, BEFORE_EACH_ANNOTATION_QUALIFIED_NAME);
  }

  private void addMissingTestedMethodParametersMocks(PsiMethod testedMethod, PsiClass innerClass) {
    commonCodeInjector.insertAllMissingMocksForParametersOfMethodIntoTestClass(innerClass, testedMethod);
  }

  private String createWhenMethodName(PsiMethod method) {
    String methodName = method.getName();
    int indexOfFirstCapital = findIndexOfFirstUppercase(methodName);
    String verb = methodName.substring(0, indexOfFirstCapital);
    String restOfMethodName = methodName.substring(indexOfFirstCapital);
    return String.format("%s%s%s", TEST_NESTED_CLASS_PREFIX, toActionVerbCapitalized(verb), restOfMethodName);
  }

  private String toActionVerbCapitalized(String verb) {
    if (verb.length() == 1) {
      return Character.toUpperCase(verb.charAt(0)) + "ing";
    }

    if (Stream.of("is", "can", "has").anyMatch(verb::startsWith)) {
      return "CheckingIf" + Character.toUpperCase(verb.charAt(0)) + verb.substring(1);
    }

    String lastTwoLetters = verb.substring(verb.length() - 2);
    String lastLetter = lastTwoLetters.substring(1);

    if (Arrays.asList("ee", "ye", "oe").contains(lastTwoLetters)) {
      return Character.toUpperCase(verb.charAt(0)) + verb.substring(1) + "ing";
    }

    if (lastLetter.equals("e")) {
      return Character.toUpperCase(verb.charAt(0)) + verb.substring(1, verb.length() - 1) + "ing";
    }

    // Only hardcoded most recently used verbs where last letter is doubled
    if (Arrays.asList("get", "commit").contains(verb.toLowerCase())) {
      return Character.toUpperCase(verb.charAt(0)) + verb.substring(1) + verb.charAt(verb.length() - 1) + "ing";
    }

    return Character.toUpperCase(verb.charAt(0)) + verb.substring(1) + "ing";
  }

  private int findIndexOfFirstUppercase(String string) {
    for (int i = 0; i < string.length(); i++) {
      if (Character.isUpperCase(string.charAt(i))) {
        return i;
      }
    }
    return string.length();
  }

  private boolean isVisible(PsiMethod method) {
    return method.getModifierList().hasModifierProperty(PsiModifier.PUBLIC)
            || method.getModifierList().hasModifierProperty(PsiModifier.PROTECTED)
            || method.getModifierList().hasModifierProperty(PsiModifier.PACKAGE_LOCAL);
  }


  private Set<String> getExistingTestInnerClasses(PsiClass testClass) {
    return Arrays.stream(testClass.getInnerClasses()).map(NavigationItem::getName).collect(Collectors.toSet());
  }


  private String getUnderTestQualifiedClassName(PsiClass psiClass) {
    String testClassName = psiClass.getQualifiedName();
    final String match = TEST_CLASS_NAME_SUFFIXES.stream().filter((suffix) -> testClassName.endsWith(suffix)).findFirst().orElse(null);
    if (match != null) {
      return testClassName.substring(0, testClassName.length() - match.length());
    }
    return null;
  }
}
