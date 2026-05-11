package umlrenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UMLClass {
    public enum Kind {
        CLASS, ABSTRACT_CLASS, INTERFACE, ENUM
    }

    private final String className;
    private final Kind kind;
    private final List<Member> fields = new ArrayList<>();
    private final List<Member> methods = new ArrayList<>();

    private UMLClass parentClass;
    private String parentClassName = "";

    private final List<UMLClass> subclasses = new ArrayList<>();
    private final List<UMLClass> implementedIfaces = new ArrayList<>();
    private final List<String> unresolvedIfaces = new ArrayList<>();

    public static class Member {
        public enum Visibility {PUBLIC, PROTECTED, PACKAGE, PRIVATE}

        public final String name;
        public final String type;
        public final String params;
        public final Visibility visibility;
        public final boolean isStatic;
        public final boolean isAbstract;
        public final boolean isFinal;

        public Member(String name, String type, String params,
                      Visibility visibility,
                      boolean isStatic, boolean isAbstract, boolean isFinal) {
            this.name = name;
            this.type = type;
            this.params = params;
            this.visibility = visibility;
            this.isStatic = isStatic;
            this.isAbstract = isAbstract;
            this.isFinal = isFinal;
        }

        public String visibilitySymbol() {
            return switch (visibility) {
                case PUBLIC -> "+";
                case PROTECTED -> "#";
                case PACKAGE -> "~";
                case PRIVATE -> "-";
            };
        }

        public String label() {
            String base = visibilitySymbol() + " " + name;
            if (!params.isEmpty()) base += "(" + params + ") : " + type;
            else base += " : " + type;
            if (isStatic) base = "_" + base + "_";
            if (isAbstract) base = "/" + base + "/";
            return base;
        }

        public String displayLabel() {
            return label().replace("_", "").replace("/", "");
        }

        public boolean isMethod() {
            return !params.isEmpty() || type.equals("void");
        }
    }

    public UMLClass(String className, Kind kind) {
        this.className = className;
        this.kind = kind;
    }

    public void setParentClass(UMLClass parent) {
        this.parentClass = parent;
        this.parentClassName = parent.className;
        parent.subclasses.add(this);
    }

    public void setParentClassName(String name) {
        this.parentClassName = name;
    }

    public void addImplementedIface(UMLClass iface) {
        implementedIfaces.add(iface);
    }

    public void addUnresolvedIface(String name) {
        unresolvedIfaces.add(name);
    }

    public void addField(Member m) {
        fields.add(m);
    }

    public void addMethod(Member m) {
        methods.add(m);
    }

    public String getClassName() {
        return className;
    }

    public Kind getKind() {
        return kind;
    }

    public boolean isAbstract() {
        return kind == Kind.ABSTRACT_CLASS;
    }

    public boolean isInterface() {
        return kind == Kind.INTERFACE;
    }

    public boolean isEnum() {
        return kind == Kind.ENUM;
    }

    public List<Member> getFields() {
        return fields;
    }

    public List<Member> getMethods() {
        return methods;
    }

    public UMLClass getParentClass() {
        return parentClass;
    }

    public String getParentClassName() {
        return parentClassName;
    }

    public List<UMLClass> getSubclasses() {
        return subclasses;
    }

    public List<UMLClass> getImplementedIfaces() {
        return implementedIfaces;
    }

    public List<String> getUnresolvedIfaces() {
        return unresolvedIfaces;
    }

    public boolean isRoot() {
        return parentClass == null;
    }

    public String stereotype() {
        return switch (kind) {
            case INTERFACE -> "«interface»";
            case ABSTRACT_CLASS -> "«abstract»";
            case ENUM -> "«enum»";
            case CLASS -> "";
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UMLClass uc)) return false;
        return Objects.equals(className, uc.className);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(className);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(className).append("\n");
        fields.forEach(f -> sb.append("  ").append(f.displayLabel()).append("\n"));
        methods.forEach(m -> sb.append("  ").append(m.displayLabel()).append("\n"));
        return sb.toString();
    }
}