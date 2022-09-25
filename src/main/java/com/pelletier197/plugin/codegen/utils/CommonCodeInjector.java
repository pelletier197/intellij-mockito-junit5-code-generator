package com.pelletier197.plugin.codegen.utils;

import com.intellij.navigation.NavigationItem;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiEnumConstant;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.codeStyle.SuggestedNameInfo;
import com.intellij.psi.codeStyle.VariableKind;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiUtil;
import com.pelletier197.plugin.codegen.ImportOrganizer;

import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommonCodeInjector {
  public static final String MOCK_ANNOTATION_QUALIFIED_NAME = "org.mockito.Mock";
  public static final String MOCK_ANNOTATION_SHORT_NAME = "Mock";

  private static final Random RANDOM = new Random();

  private final JavaPsiFacade javaPsiFacade;
  private final PsiJavaFile psiJavaFile;
  private final ImportOrganizer importOrganizer;
  private final JavaCodeStyleManager codeStyleManager;

  public CommonCodeInjector(JavaPsiFacade javaPsiFacade, PsiJavaFile psiJavaFile, ImportOrganizer importOrganizer, JavaCodeStyleManager codeStyleManager) {
    this.javaPsiFacade = javaPsiFacade;
    this.codeStyleManager = codeStyleManager;
    this.psiJavaFile = psiJavaFile;
    this.importOrganizer = importOrganizer;
  }

  private void insertPrimitiveOrFinalClassForAssociatedConstant(PsiClass psiClass, Set<String> existingFields, PsiVariable psiVariable) {
    String fieldName = suggestVariableConstantName(psiVariable);
    if (!existingFields.contains(fieldName)) {
      insertPrimitiveOrFinalClass(psiClass, psiVariable.getType(), fieldName);
    }
  }


  private void insertPrimitiveOrFinalClassForAssociatedField(PsiClass psiClass, Set<String> existingFieldNames, PsiType psiType) {
    String fieldName = suggestConstantName(psiType);

    if (!existingFieldNames.contains(fieldName)) {
      insertPrimitiveOrFinalClass(psiClass, psiType, fieldName);
    }
  }

  public void insertAllMissingMocksForFieldsOfClassIntoTestClass(PsiClass targetTestClass, PsiClass underTestPsiClass) {
    Set<String> existingFields = Stream.of(targetTestClass.getFields()).map(NavigationItem::getName).collect(Collectors.toSet());
    insertAllMissingNamedParameterIntoTestClass(targetTestClass, existingFields, underTestPsiClass.getAllFields());
  }

  public void insertAllMissingMocksForParametersOfMethodIntoTestClass(PsiClass targetTestClass, PsiMethod underTestPsiMethod) {
    Set<String> existingFields = Stream.of(targetTestClass.getFields()).map(NavigationItem::getName).collect(Collectors.toSet());
    insertAllMissingNamedParameterIntoTestClass(targetTestClass, existingFields, underTestPsiMethod.getParameterList().getParameters());
  }

  private void insertAllMissingNamedParameterIntoTestClass(PsiClass targetTestClass, Set<String> existingFields, PsiVariable[] elements) {
    boolean addedMocks = false;

    for (PsiVariable element : elements) {
      PsiType psiType = element.getType();

      if (isNotStatic(element)) {
        if (isPrimitiveOrSupportedFinalClass(psiType)) {
          insertPrimitiveOrFinalClassForAssociatedConstant(targetTestClass, existingFields, element);
        } else if (isCollection(psiType)) {
          addedMocks = insertSingleCollectionFieldValue(targetTestClass, existingFields, element) || addedMocks;
        } else if (isNotFinalClass(psiType)) {
          insertMockedField(targetTestClass, existingFields, element);
          addedMocks = true;
        }
      }
    }
    if (addedMocks) {
      importOrganizer.addClassImport(psiJavaFile, MOCK_ANNOTATION_QUALIFIED_NAME);
    }
  }

  private boolean insertSingleCollectionFieldValue(PsiClass psiClass, Set<String> existingFields, PsiVariable psiField) {
    PsiType type = PsiUtil.extractIterableTypeParameter(psiField.getType(), false);

    if (isPrimitiveOrSupportedFinalClass(type)) {
      insertPrimitiveOrFinalClassForAssociatedField(psiClass, existingFields, type);
    } else if (isNotFinalClass(type)) {
      insertMockedField(psiClass, existingFields, type);
      return true;
    }

    return false;
  }

  public void insertPrimitiveOrFinalClass(PsiClass psiClass, PsiType type, String newFieldName) {
    String typeName = type.getPresentableText(false);
    String assignment = "";
    String importToAdd = null;

    if (isEnum(type)) {
      PsiClass typeClass = javaPsiFacade.findClass(type.getCanonicalText(), GlobalSearchScope.allScope(psiJavaFile.getProject()));
      PsiField[] fields = typeClass.getFields();
      PsiEnumConstant firstEnumValue = Stream.of(fields).filter(f -> f instanceof PsiEnumConstant).map(f -> ((PsiEnumConstant) f)).findAny().orElse(null);

      if (firstEnumValue == null) {
        assignment = "null";
      } else {
        assignment = typeClass.getName() + "." + firstEnumValue.getName();
      }

      importToAdd = typeClass.getQualifiedName();
    } else {
      switch (typeName) {
        case "UUID":
          assignment = "UUID.randomUUID()";
          importToAdd = "java.util.UUID";
          break;
        case "Instant":
          assignment = "Instant.now()";
          importToAdd = "java.time.Instant";
          break;
        case "ZonedDateTime":
          assignment = "ZonedDateTime.now()";
          importToAdd = "java.time.ZonedDateTime";
          break;
        case "String":
          assignment = "\"" + newFieldName + "\"";
          importToAdd = "java.lang.String";
          break;
        case "int":
        case "long":
          assignment = String.valueOf(RANDOM.nextInt(100));
          break;
        case "Long":
          assignment = String.valueOf(RANDOM.nextInt(100));
          importToAdd = "java.lang.Long";
          break;
        case "Integer":
          assignment = String.valueOf(RANDOM.nextInt(100));
          importToAdd = "java.lang.Integer";
          break;
        case "double":
          assignment = String.format("%.1f", RANDOM.nextDouble() * 100);
          break;
        case "boolean":
          assignment = "true";
          break;
      }
    }

    String expString = "private" + (isInnerTestClass(psiClass) ? "" : " static") + " final " + typeName + " " + newFieldName + " = " + assignment + ";";
    PsiField field = javaPsiFacade.getElementFactory().createFieldFromText(expString, psiClass);

    psiClass.add(field);

    if (importToAdd != null) {
      importOrganizer.addClassImport(psiJavaFile, importToAdd);
    }
  }

  private boolean isInnerTestClass(PsiClass psiClass) {
    return psiClass.getName().startsWith("When");
  }

  private String suggestVariableConstantName(PsiVariable variable) {
    String variableOriginalName = variable.getName();

    StringBuilder constantFieldNameBuilder = new StringBuilder().append(Character.toUpperCase(variableOriginalName.charAt(0)));
    for (int i = 1; i < variableOriginalName.length(); i++) {
      char current = variableOriginalName.charAt(i);
      if (Character.isUpperCase(current)) {
        constantFieldNameBuilder.append("_").append(current);
      } else {
        constantFieldNameBuilder.append(Character.toUpperCase(current));
      }
    }
    return constantFieldNameBuilder.toString();
  }

  private String suggestConstantName(PsiType psiType) {
    SuggestedNameInfo info = codeStyleManager.suggestVariableName(VariableKind.STATIC_FINAL_FIELD, null, null, psiType);
    if (psiType.getPresentableText(false).equals("UUID")) {
      info.names[0] = "UUID_VALUE";
    }
    return info.names[0];
  }


  private String suggestFieldName(PsiVariable psiField) {
    return psiField.getName();
  }

  private String suggestFieldName(PsiType type) {
    return codeStyleManager.suggestVariableName(VariableKind.FIELD, null, null, type).names[0];
  }

  private boolean isNotFinalClass(PsiType psiType) {
    PsiClass psiClass = javaPsiFacade.findClass(psiType.getCanonicalText(), psiType.getResolveScope());
    if (psiClass == null) {
      return true;
    }
    return !psiClass.getModifierList().hasExplicitModifier("final");
  }

  private boolean isNotStatic(PsiVariable psiField) {
    return !psiField.getModifierList().getText().contains("static");
  }

  private boolean isPrimitiveOrSupportedFinalClass(PsiType type) {
    return (type instanceof PsiPrimitiveType)
            || isEnum(type)
            || equalsAnyOf(type.getCanonicalText(),
            "java.util.UUID",
            "java.lang.String",
            "java.time.Instant",
            "java.time.ZonedDateTime"
    );
  }

  private boolean isEnum(PsiType psiType) {
    return Stream.of(psiType.getSuperTypes()).anyMatch(type -> type.getCanonicalText().startsWith("java.lang.Enum"));
  }

  private boolean isCollection(PsiType type) {
    return Stream.of(type.getSuperTypes())
            .anyMatch(superType -> startsWithAnyOf(superType.getCanonicalText(), "java.util.List", "java.util.Set"))
            || startsWithAnyOf(type.getCanonicalText(), "java.util.List", "java.util.Set");
  }


  private boolean equalsAnyOf(String value, String... expected) {
    for (String ex : expected) {
      if (value.equals(ex))
        return true;
    }
    return false;
  }

  private boolean startsWithAnyOf(String value, String... expected) {
    for (String ex : expected) {
      if (value.startsWith(ex))
        return true;
    }
    return false;
  }

  private void insertMockedField(PsiClass psiClass, Set<String> existingFields, PsiType type) {
    String newFieldName = suggestFieldName(type);
    newFieldName = Character.toLowerCase(newFieldName.charAt(0)) + newFieldName.substring(1);
    if (!existingFields.contains(newFieldName)) {
      insertNewField(psiClass, type, newFieldName, MOCK_ANNOTATION_SHORT_NAME);
    }
  }

  private void insertMockedField(PsiClass psiClass, Set<String> existingFields, PsiVariable element) {
    String newFieldName = suggestFieldName(element);
    newFieldName = Character.toLowerCase(newFieldName.charAt(0)) + newFieldName.substring(1);
    if (!existingFields.contains(newFieldName)) {
      insertNewField(psiClass, element.getType(), newFieldName, MOCK_ANNOTATION_SHORT_NAME);
    }
  }

  public void insertNewField(PsiClass psiClass, PsiType newFieldType, String newFieldName, String annotationClassName) {
    PsiField underTestField = javaPsiFacade.getElementFactory().createField(newFieldName, newFieldType);
    underTestField.getModifierList().addAnnotation(annotationClassName);
    psiClass.add(underTestField);
  }

}
