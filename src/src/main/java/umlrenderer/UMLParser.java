package umlrenderer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.*;

public class UMLParser {
    // [modifiers] (class|interface|enum) ClassName[<Generics>] [extends X] [implements A,B]
    private static final Pattern TYPE_DECL = Pattern.compile(
            "(?:^|\\s)(?<mods>(?:(?:public|protected|private|abstract|final|static)\\s+)*)" +
                    "(?<kind>class|interface|enum)\\s+(?<name>\\w+)(?:<[^>]*>)?" +
                    "(?:\\s+extends\\s+(?<ext>[\\w<>, ]+?))?" +
                    "(?:\\s+implements\\s+(?<impl>[\\w<>, ]+?))?" +
                    "\\s*\\{",
            Pattern.MULTILINE
    );

    // [modifiers] Type name ;
    private static final Pattern FIELD = Pattern.compile(
            "(?<mods>(?:(?:public|protected|private|static|final|volatile|transient)\\s+)+)" +
                    "(?<type>[\\w<>\\[\\]?,\\s]+?)\\s+(?<name>\\w+)\\s*(?:=[^;]*)?;"
    );

    // [modifiers] ReturnType name ( params )
    private static final Pattern METHOD = Pattern.compile(
            "(?<mods>(?:(?:public|protected|private|static|final|abstract|synchronized|default|native)\\s+)+)" +
                    "(?<ret>[\\w<>\\[\\]?,\\s]+?)\\s+(?<name>\\w+)\\s*\\((?<params>[^)]*)\\)"
    );

    public List<UMLClass> parseDirectory(Path root) throws IOException {
        List<Path> javaFiles = new ArrayList<>();
        Files.walk(root)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(javaFiles::add);

        List<UMLClass> classes = new ArrayList<>();
        for (Path file : javaFiles) {
            String source = Files.readString(file);
            UMLClass parsed = parseSource(source);
            if (parsed != null) classes.add(parsed);
        }

        resolveRelationships(classes);
        return classes;
    }

    UMLClass parseSource(String source) {
        source = sanitize(source);

        Matcher m = TYPE_DECL.matcher(source);
        if (!m.find()) return null;

        String mods = m.group("mods") == null ? "" : m.group("mods");
        String kindStr = m.group("kind");
        String name = m.group("name");
        String extStr = m.group("ext");
        String implStr = m.group("impl");

        UMLClass.Kind kind = switch (kindStr) {
            case "interface" -> UMLClass.Kind.INTERFACE;
            case "enum" -> UMLClass.Kind.ENUM;
            default -> mods.contains("abstract")
                    ? UMLClass.Kind.ABSTRACT_CLASS
                    : UMLClass.Kind.CLASS;
        };

        UMLClass umlClass = new UMLClass(name, kind);

        if (extStr != null) {
            String parentName = firstToken(extStr); // strip generics
            umlClass.setParentClassName(parentName);
        }

        if (implStr != null) {
            for (String iface : implStr.split(",")) {
                umlClass.addUnresolvedIface(firstToken(iface.trim()));
            }
        }

        int bodyStart = m.end() - 1;
        if (bodyStart < 0 || bodyStart >= source.length() || source.charAt(bodyStart) != '{') {
            bodyStart = source.indexOf('{', m.end() - 1);
        }
        if (bodyStart == -1) return umlClass;
        String body = extractBody(source, bodyStart);

        parseFields(body, umlClass, name);
        parseMethods(body, umlClass, name);

        return umlClass;
    }

    private void parseFields(String body, UMLClass umlClass, String className) {
        Matcher m = FIELD.matcher(body);
        while (m.find()) {
            String mods = m.group("mods");
            String type = m.group("type").trim();
            String name = m.group("name");

            if (isKeyword(name) || isKeyword(type)) continue;

            UMLClass.Member member = new UMLClass.Member(
                    name, type, "",
                    parseVisibility(mods),
                    mods.contains("static"),
                    false,
                    mods.contains("final")
            );
            umlClass.addField(member);
        }
    }

    private void parseMethods(String body, UMLClass umlClass, String className) {
        Matcher m = METHOD.matcher(body);
        while (m.find()) {
            String mods = m.group("mods");
            String ret = m.group("ret").trim();
            String name = m.group("name");
            String params = m.group("params").trim();

            if (name.equals(className)) continue;
            if (isKeyword(name) || isKeyword(ret)) continue;
            if (ret.matches("if|for|while|switch|catch|return|new|throw")) continue;

            String paramTypes = simplifyParams(params);

            UMLClass.Member member = new UMLClass.Member(
                    name, ret, paramTypes,
                    parseVisibility(mods),
                    mods.contains("static"),
                    mods.contains("abstract"),
                    mods.contains("final")
            );
            umlClass.addMethod(member);
        }
    }

    private void resolveRelationships(List<UMLClass> classes) {
        Map<String, UMLClass> byName = new HashMap<>();
        for (UMLClass c : classes) byName.put(c.getClassName(), c);

        for (UMLClass c : classes) {
            String parentName = c.getParentClassName();
            if (!parentName.isEmpty()) {
                UMLClass parent = byName.get(parentName);
                if (parent != null) c.setParentClass(parent);
            }

            for (String ifaceName : c.getUnresolvedIfaces()) {
                UMLClass iface = byName.get(ifaceName);
                if (iface != null) c.addImplementedIface(iface);
            }
        }
    }

    private String sanitize(String src) {
        int n = src.length();
        StringBuilder sb = new StringBuilder(n);
        int i = 0;
        while (i < n) {
            if (i + 2 < n && src.charAt(i) == '"' && src.charAt(i + 1) == '"' && src.charAt(i + 2) == '"') {
                sb.append("\"\"\"");
                i += 3;
                while (i < n) {
                    if (i + 2 < n && src.charAt(i) == '"' && src.charAt(i + 1) == '"' && src.charAt(i + 2) == '"') {
                        sb.append("\"\"\"");
                        i += 3;
                        break;
                    }
                    sb.append(src.charAt(i) == '\n' ? '\n' : ' ');
                    i++;
                }
                continue;
            }
            if (i + 1 < n && src.charAt(i) == '/' && src.charAt(i + 1) == '*') {
                sb.append("/*");
                i += 2;
                while (i < n) {
                    if (i + 1 < n && src.charAt(i) == '*' && src.charAt(i + 1) == '/') {
                        sb.append("*/");
                        i += 2;
                        break;
                    }
                    sb.append(src.charAt(i) == '\n' ? '\n' : ' ');
                    i++;
                }
                continue;
            }
            if (i + 1 < n && src.charAt(i) == '/' && src.charAt(i + 1) == '/') {
                sb.append("//");
                i += 2;
                while (i < n && src.charAt(i) != '\n') {
                    sb.append(' ');
                    i++;
                }
                continue;
            }
            if (src.charAt(i) == '"') {
                sb.append('"');
                i++;
                while (i < n) {
                    char ch = src.charAt(i);
                    if (ch == '\\' && i + 1 < n) {
                        sb.append("  ");
                        i += 2;
                    } else if (ch == '"') {
                        sb.append('"');
                        i++;
                        break;
                    } else {
                        sb.append(ch == '\n' ? '\n' : ' ');
                        i++;
                    }
                }
                continue;
            }
            if (src.charAt(i) == '\'') {
                int j = i + 1;
                if (j < n && src.charAt(j) == '\\') j++;
                if (j < n) j++;
                if (j < n && src.charAt(j) == '\'') {
                    sb.append('\'');
                    i++;
                    while (i < n) {
                        char ch = src.charAt(i);
                        if (ch == '\\' && i + 1 < n) {
                            sb.append("  ");
                            i += 2;
                        } else if (ch == '\'') {
                            sb.append('\'');
                            i++;
                            break;
                        } else {
                            sb.append(' ');
                            i++;
                        }
                    }
                    continue;
                }
            }
            sb.append(src.charAt(i));
            i++;
        }
        return sb.toString();
    }

    private String extractBody(String src, int start) {
        if (start < 0 || start >= src.length() || src.charAt(start) != '{') {
            throw new IllegalArgumentException(
                    "extractBody: expected '{' at " + start +
                            ", got: " + (start < src.length() ? src.charAt(start) : "EOF"));
        }
        int depth = 0;
        for (int i = start; i < src.length(); i++) {
            char c = src.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                if (--depth == 0) return src.substring(start + 1, i);
            }
        }
        throw new RuntimeException("No matching closing brace found");
    }

    private String firstToken(String s) {
        return s.trim().split("[\\s<,]")[0];
    }

    private UMLClass.Member.Visibility parseVisibility(String mods) {
        if (mods.contains("private")) return UMLClass.Member.Visibility.PRIVATE;
        if (mods.contains("protected")) return UMLClass.Member.Visibility.PROTECTED;
        if (mods.contains("public")) return UMLClass.Member.Visibility.PUBLIC;
        return UMLClass.Member.Visibility.PACKAGE;
    }

    private String simplifyParams(String params) {
        if (params.isBlank()) return "";
        StringBuilder sb = new StringBuilder();
        for (String p : params.split(",")) {
            p = p.trim();
            if (p.isEmpty()) continue;
            String[] parts = p.split("\\s+");
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(parts[0]);
        }
        return sb.toString();
    }

    private static final Set<String> KEYWORDS = Set.of(
            "if", "else", "for", "while", "do", "switch", "case", "break", "continue",
            "return", "new", "throw", "try", "catch", "finally", "import", "package",
            "this", "super", "null", "true", "false", "instanceof", "class", "interface",
            "enum", "void", "int", "long", "double", "float", "boolean", "char", "byte",
            "short", "static", "final", "abstract", "public", "private", "protected",
            "synchronized", "volatile", "transient", "native", "default", "extends",
            "implements", "throws"
    );

    private boolean isKeyword(String s) {
        return KEYWORDS.contains(s);
    }
}