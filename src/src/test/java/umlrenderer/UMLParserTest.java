package umlrenderer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UMLParserTest {
    private final UMLParser parser = new UMLParser();

    @Test
    void parsesClassWithFieldsAndMethods() {
        String source = """
                public class Person {
                    private String name;
                    public int age;
                    public String getName() { return name; }
                    public void setName(String name) { this.name = name; }
                }
                """;

        UMLClass result = parser.parseSource(source);

        assertNotNull(result);
        assertEquals("Person", result.getClassName());
        assertEquals(UMLClass.Kind.CLASS, result.getKind());
        assertEquals(2, result.getFields().size());
        assertEquals(2, result.getMethods().size());
    }

    @Test
    void ignoresDeclarationsInsideCommentsAndStrings() {
        String source = """
                public class Example {
                    private String fake = "public class NotAClass { }";
                    /* public void fakeMethod() {} */
                    public void realMethod() {}
                }
                """;

        UMLClass result = parser.parseSource(source);

        assertNotNull(result);
        assertEquals("Example", result.getClassName());
        assertEquals(1, result.getMethods().size());
        assertEquals("realMethod", result.getMethods().get(0).name);
    }

    @Test
    void capturesInheritanceAndImplementedInterfaces() {
        String source = """
                public abstract class Worker extends Person implements Runnable, AutoCloseable {
                    public abstract void work();
                }
                """;

        UMLClass result = parser.parseSource(source);

        assertNotNull(result);
        assertEquals(UMLClass.Kind.ABSTRACT_CLASS, result.getKind());
        assertEquals("Person", result.getParentClassName());
        assertEquals(2, result.getUnresolvedIfaces().size());
        assertTrue(result.getUnresolvedIfaces().contains("Runnable"));
        assertTrue(result.getUnresolvedIfaces().contains("AutoCloseable"));
    }

    @Test
    void parsesMultipleTypesAndNestedDeclarations() {
        String source = """
                package example;
                class First {
                    class Inner {}
                }
                interface Second {}
                """;

        var types = parser.parseSources(source);

        assertEquals(3, types.size());
        assertEquals("example.First", types.get(0).getQualifiedName());
        assertEquals("example.First.Inner", types.get(1).getQualifiedName());
        assertEquals("example.Second", types.get(2).getQualifiedName());
    }

    @Test
    void distinguishesFieldsFromZeroArgumentMethods() {
        UMLClass type = parser.parseSource("""
                class Example {
                    int value;
                    int getValue() { return value; }
                    Example() {}
                }
                """);

        assertNotNull(type);
        assertFalse(type.getFields().get(0).isMethod());
        assertEquals(2, type.getMethods().size());
        assertTrue(type.getMethods().stream().allMatch(UMLClass.Member::isMethod));
    }

    @Test
    void preservesGenericMemberTypesAndParameters() {
        UMLClass type = parser.parseSource("""
                import java.util.List;
                class Box<T> {
                    private List<T> values;
                    public List<T> addAll(List<T> incoming) { return incoming; }
                }
                """);

        assertNotNull(type);
        assertEquals("List<T>", type.getFields().get(0).type);
        assertEquals("List<T>", type.getMethods().get(0).params);
    }

}
