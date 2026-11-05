package umlrenderer;

import java.util.Objects;

public record UMLRelationship(UMLClass source, UMLClass target, Type type) {
    public enum Type {
        EXTENDS,
        IMPLEMENTS,
        ASSOCIATION
    }

    public UMLRelationship {
        Objects.requireNonNull(source);
        Objects.requireNonNull(target);
        Objects.requireNonNull(type);
        if (source.equals(target)) {
            throw new IllegalArgumentException("Self relationships are not supported");
        }
    }

    public boolean touches(UMLClass type) {
        return source.equals(type) || target.equals(type);
    }
}
