package umlrenderer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UMLRelationshipsTest {
    @TempDir
    Path directory;

    @Test
    void detectsInheritanceRealizationAndFieldAssociations() throws Exception {
        Files.writeString(directory.resolve("Example.java"), """
                package demo;
                interface Payable {}
                class Person {}
                class Department {}
                class Employee extends Person implements Payable {
                    private Department department;
                    private java.util.List<Department> teams;
                }
                """);

        List<UMLClass> classes = new UMLParser().parseDirectory(directory);
        var edges = UMLRelationships.analyze(classes);

        assertTrue(edges.stream().anyMatch(edge -> edge.type() == UMLRelationship.Type.EXTENDS
                && edge.source().getClassName().equals("Employee")
                && edge.target().getClassName().equals("Person")));
        assertTrue(edges.stream().anyMatch(edge -> edge.type() == UMLRelationship.Type.IMPLEMENTS
                && edge.source().getClassName().equals("Employee")
                && edge.target().getClassName().equals("Payable")));
        assertEquals(1, edges.stream().filter(edge -> edge.type() == UMLRelationship.Type.ASSOCIATION
                && edge.source().getClassName().equals("Employee")
                && edge.target().getClassName().equals("Department")).count());
    }

    @Test
    void doesNotConfuseSameSimpleNamesAcrossPackages() throws Exception {
        Path a = Files.createDirectories(directory.resolve("a"));
        Path b = Files.createDirectories(directory.resolve("b"));
        Files.writeString(a.resolve("Node.java"), "package a; public class Node {}");
        Files.writeString(b.resolve("Node.java"), "package b; public class Node {}");
        Files.writeString(a.resolve("Owner.java"), "package a; public class Owner { Node node; }");

        var edges = UMLRelationships.analyze(new UMLParser().parseDirectory(directory));

        assertEquals(1, edges.size());
        assertEquals("a.Node", edges.get(0).target().getQualifiedName());
    }

    @Test
    void ignoresSelfReferencesAndPrimitiveFields() throws Exception {
        Files.writeString(directory.resolve("Node.java"), """
                class Node { Node next; int count; String label; }
                """);

        var edges = UMLRelationships.analyze(new UMLParser().parseDirectory(directory));

        assertTrue(edges.isEmpty());
    }
}
