# Intellij Mockito JUnit 5 Code Generator
Generates boilerplate code for testing using `Mockito` and `AssertJ` (if present) under JUnit 5.

## Usage
On your test class, hit `alt + insert` and select `Add Mockito JUnit 5 code`.

Here is a sample of the generated test code.
Given the following sample class: 
```Java
public class MyClass {
    private final MyEnum myEnum;
    private final String myString;
    private final List<UUID> myUUIDList;
    private final Object myObjectValueOfCourse;

  public MyClass(MyEnum myEnum, String myString, List<UUID> myUUIDList, Object myObjectValueOfCourse) {
    this.myEnum = myEnum;
      this.myString = myString;
      this.myUUIDList = myUUIDList;
      this.myObjectValueOfCourse = myObjectValueOfCourse;
  }

  public void testMyClass(List<String> myStrings, List<UUID> uuids, Object myObjectCustom, Object ySecondObject, MyEnum otherEnum) {

  }
}
```

The output code for the tests is:
```Java
@ExtendWith(MockitoExtension.class)
class MyClassTest {

    private static final MyEnum MY_ENUM = MyEnum.ENUM_1;
    private static final String MY_STRING = "MY_STRING";
    private static final UUID UUID_VALUE = UUID.randomUUID();
    @Mock
    private Object myObjectValueOfCourse;
    @InjectMocks
    private MyClass underTest;

    @Nested
    class WhenTestingMyClass {
        private final String STRING = "STRING";
        private final UUID UUID_VALUE = UUID.randomUUID();
        private final MyEnum OTHER_ENUM = MyEnum.ENUM_1;
        @Mock
        private Object myObjectCustom;
        @Mock
        private Object mySecondObject;

        @BeforeEach
        void setup() {
        }

        @Test
        void then() {
        }
    }
}
```
## More information
Read the description of the plugin, either in the `resources/META-INF/plugin.xml`, or in Intellij directly.

## How to import
You can import jar situated at the root (`mockito-code-generator-junit5.jar`) in `Intellij` when loading a plugin from disk. 

## Feature request
Feel free to propose feature requests via [The issues system](https://gitlab.com/pelletier197/intellij-mockito-junit-5-code-generator/issues) or to open your own merge requests.
