package umlrenderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UMLRelationships {
    private static final Pattern TYPE_NAME = Pattern.compile("[A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$]*)*");
    private static final Set<String> IGNORED = Set.of(
            "boolean", "byte", "short", "int", "long", "char", "float", "double",
            "void", "extends", "super", "var"
    );

    private UMLRelationships() {}

    public static List<UMLRelationship> analyze(List<UMLClass> classes) {
        Map<String, UMLClass> qualified = new HashMap<>();
        Map<String, List<UMLClass>> simple = new HashMap<>();

        for (UMLClass type : classes) {
            qualified.put(type.getQualifiedName(), type);
            simple.computeIfAbsent(type.getClassName(), ignored -> new ArrayList<>()).add(type);
        }

        Set<UMLRelationship> edges = new LinkedHashSet<>();
        for (UMLClass type : classes) {
            if (type.getParentClass() != null) {
                edges.add(new UMLRelationship(type, type.getParentClass(), UMLRelationship.Type.EXTENDS));
            }
            for (UMLClass iface : type.getImplementedIfaces()) {
                edges.add(new UMLRelationship(type, iface, UMLRelationship.Type.IMPLEMENTS));
            }
            for (UMLClass.Member field : type.getFields()) {
                Matcher tokens = TYPE_NAME.matcher(field.type);
                while (tokens.find()) {
                    String candidate = tokens.group();
                    if (IGNORED.contains(candidate)) continue;
                    UMLClass target = resolve(candidate, type, qualified, simple);
                    if (target != null && !type.equals(target)) {
                        edges.add(new UMLRelationship(type, target, UMLRelationship.Type.ASSOCIATION));
                    }
                }
            }
        }
        return List.copyOf(edges);
    }

    private static UMLClass resolve(String name, UMLClass context,
                                    Map<String, UMLClass> qualified,
                                    Map<String, List<UMLClass>> simple) {
        UMLClass exact = qualified.get(name);
        if (exact != null) return exact;

        String owner = context.getQualifiedName();
        int dot = owner.lastIndexOf('.');
        while (dot >= 0) {
            UMLClass local = qualified.get(owner.substring(0, dot + 1) + name);
            if (local != null) return local;
            owner = owner.substring(0, dot);
            dot = owner.lastIndexOf('.');
        }

        List<UMLClass> matches = simple.getOrDefault(name, List.of());
        return matches.size() == 1 ? matches.get(0) : null;
    }
}
