package umlrenderer;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class UMLParser {
    private final JavaParser javaParser = new JavaParser(
            new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21));
    public List<UMLClass> parseDirectory(Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            throw new IOException("Source directory does not exist: " + root);
        }

        List<Path> files;
        try (Stream<Path> paths = Files.walk(root)) {
            files = paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .sorted()
                    .toList();
        }

        List<UMLClass> classes = new ArrayList<>();
        for (Path path : files) {
            try {
                classes.addAll(parseSources(Files.readString(path)));
            } catch (RuntimeException e) {
                throw new IOException("Could not parse Java source: " + path, e);
            }
        }

        resolveRelationships(classes);
        return classes;
    }

    UMLClass parseSource(String source) {
        List<UMLClass> result = parseSources(source);
        return result.isEmpty() ? null : result.get(0);
    }

    List<UMLClass> parseSources(String source) {
        ParseResult<CompilationUnit> parsed = javaParser.parse(source);
        CompilationUnit unit = parsed.getResult()
                .orElseThrow(() -> new ParseProblemException(parsed.getProblems()));
        String packageName = unit.getPackageDeclaration()
                .map(declaration -> declaration.getNameAsString())
                .orElse("");

        List<UMLClass> result = new ArrayList<>();
        for (TypeDeclaration<?> declaration : unit.getTypes()) {
            visitType(declaration, packageName, result);
        }
        return result;
    }

    private void visitType(TypeDeclaration<?> declaration, String owner, List<UMLClass> result) {
        if (!(declaration instanceof ClassOrInterfaceDeclaration)
                && !(declaration instanceof EnumDeclaration)
                && !(declaration instanceof RecordDeclaration)) {
            return;
        }

        String name = declaration.getNameAsString();
        String qualifiedName = owner.isEmpty() ? name : owner + "." + name;
        UMLClass.Kind kind;

        if (declaration instanceof ClassOrInterfaceDeclaration type) {
            kind = type.isInterface() ? UMLClass.Kind.INTERFACE
                    : type.isAbstract() ? UMLClass.Kind.ABSTRACT_CLASS
                    : UMLClass.Kind.CLASS;
        } else {
            kind = declaration instanceof EnumDeclaration ? UMLClass.Kind.ENUM : UMLClass.Kind.CLASS;
        }

        UMLClass model = new UMLClass(name, kind);
        model.setQualifiedName(qualifiedName);

        if (declaration instanceof ClassOrInterfaceDeclaration type) {
            if (!type.isInterface() && !type.getExtendedTypes().isEmpty()) {
                model.setParentClassName(type.getExtendedTypes().get(0).getNameWithScope());
            } else if (type.isInterface()) {
                for (ClassOrInterfaceType parent : type.getExtendedTypes()) {
                    model.addUnresolvedIface(parent.getNameWithScope());
                }
            }
            for (ClassOrInterfaceType iface : type.getImplementedTypes()) {
                model.addUnresolvedIface(iface.getNameWithScope());
            }
        } else if (declaration instanceof EnumDeclaration type) {
            for (ClassOrInterfaceType iface : type.getImplementedTypes()) {
                model.addUnresolvedIface(iface.getNameWithScope());
            }
        } else if (declaration instanceof RecordDeclaration type) {
            for (ClassOrInterfaceType iface : type.getImplementedTypes()) {
                model.addUnresolvedIface(iface.getNameWithScope());
            }
        }

        for (BodyDeclaration<?> member : declaration.getMembers()) {
            if (member instanceof FieldDeclaration field) {
                for (VariableDeclarator variable : field.getVariables()) {
                    model.addField(new UMLClass.Member(
                            variable.getNameAsString(),
                            variable.getType().asString(),
                            "",
                            visibility(field),
                            field.isStatic(),
                            false,
                            field.isFinal(),
                            false
                    ));
                }
            } else if (member instanceof MethodDeclaration method) {
                model.addMethod(new UMLClass.Member(
                        method.getNameAsString(),
                        method.getType().asString(),
                        method.getParameters().stream()
                                .map(param -> param.getType().asString() + (param.isVarArgs() ? "..." : ""))
                                .reduce((a, b) -> a + ", " + b).orElse(""),
                        visibility(method),
                        method.isStatic(),
                        method.isAbstract() || kind == UMLClass.Kind.INTERFACE && !method.isDefault() && !method.isStatic() && !method.isPrivate(),
                        method.isFinal(),
                        true
                ));
            } else if (member instanceof ConstructorDeclaration constructor) {
                model.addMethod(new UMLClass.Member(
                        constructor.getNameAsString(),
                        "",
                        constructor.getParameters().stream()
                                .map(param -> param.getType().asString() + (param.isVarArgs() ? "..." : ""))
                                .reduce((a, b) -> a + ", " + b).orElse(""),
                        visibility(constructor),
                        false,
                        false,
                        false,
                        true
                ));
            }
        }

        result.add(model);
        for (BodyDeclaration<?> member : declaration.getMembers()) {
            if (member instanceof TypeDeclaration<?> nested) {
                visitType(nested, qualifiedName, result);
            }
        }
    }

    private UMLClass.Member.Visibility visibility(BodyDeclaration<?> member) {
        if (member instanceof NodeWithModifiers<?> modifiers) {
            if (modifiers.hasModifier(Modifier.Keyword.PUBLIC)) return UMLClass.Member.Visibility.PUBLIC;
            if (modifiers.hasModifier(Modifier.Keyword.PROTECTED)) return UMLClass.Member.Visibility.PROTECTED;
            if (modifiers.hasModifier(Modifier.Keyword.PRIVATE)) return UMLClass.Member.Visibility.PRIVATE;
        }
        return UMLClass.Member.Visibility.PACKAGE;
    }

    private void resolveRelationships(List<UMLClass> classes) {
        Map<String, UMLClass> byQualifiedName = new HashMap<>();
        Map<String, List<UMLClass>> bySimpleName = new HashMap<>();
        for (UMLClass model : classes) {
            byQualifiedName.put(model.getQualifiedName(), model);
            bySimpleName.computeIfAbsent(model.getClassName(), ignored -> new ArrayList<>()).add(model);
        }

        for (UMLClass model : classes) {
            String parentName = model.getParentClassName();
            if (!parentName.isBlank()) {
                resolve(parentName, model, byQualifiedName, bySimpleName)
                        .filter(parent -> parent != model)
                        .ifPresent(model::setParentClass);
            }
            for (String ifaceName : model.getUnresolvedIfaces()) {
                resolve(ifaceName, model, byQualifiedName, bySimpleName)
                        .filter(iface -> iface != model)
                        .ifPresent(model::addImplementedIface);
            }
        }
    }

    private Optional<UMLClass> resolve(
            String name,
            UMLClass context,
            Map<String, UMLClass> byQualifiedName,
            Map<String, List<UMLClass>> bySimpleName) {
        UMLClass exact = byQualifiedName.get(name);
        if (exact != null) return Optional.of(exact);

        String qualified = context.getQualifiedName();
        int lastDot = qualified.lastIndexOf('.');
        if (lastDot >= 0) {
            UMLClass local = byQualifiedName.get(qualified.substring(0, lastDot + 1) + name);
            if (local != null) return Optional.of(local);
        }

        List<UMLClass> matches = bySimpleName.getOrDefault(name, List.of());
        if (matches.size() == 1) return Optional.of(matches.get(0));
        return Optional.empty();
    }
}
