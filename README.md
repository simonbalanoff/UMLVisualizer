# UMLVisualizer

A Java-only UML class diagram visualizer for Java source projects. Run a standalone JAR against a directory, or open the diagram from another Java program.

UMLVisualizer uses JavaParser to analyze Java source code without compiling or executing it. The desktop viewer uses Java2D and Swing, with zooming, panning, a minimap, and a class inspector.

## Build

Requires JDK 17+ and Maven.

```bash
mvn -f src/pom.xml clean verify
```

The executable JAR is generated at:

```text
src/target/umlvisualizer-0.2.0-SNAPSHOT.jar
```

## Run from the command line

```bash
java -jar src/target/umlvisualizer-0.2.0-SNAPSHOT.jar path/to/project/src/main/java
```

To analyze the current directory:

```bash
java -jar src/target/umlvisualizer-0.2.0-SNAPSHOT.jar
```

To print a list of discovered types without opening the GUI:

```bash
java -jar src/target/umlvisualizer-0.2.0-SNAPSHOT.jar --summary path/to/project
```

Use `--help` for options.

## Run from another Java project

Add the built JAR to the application's classpath and call:

```java
import java.nio.file.Path;
import umlrenderer.UMLVisualizer;

public class Example {
    public static void main(String[] args) {
        UMLVisualizer.initFrom(Path.of("src/main/java"));
    }
}
```

Closing the diagram window does not terminate the host application.

## Viewer controls

| Action | Control |
| --- | --- |
| Pan | Drag on canvas |
| Zoom | Mouse wheel |
| Fit all | F |
| Reset zoom | R |
| Select class | Click node |
| Deselect | Esc |
| Zoom in/out | + / - |

## Architecture

- `UMLParser`: AST-based parsing using JavaParser
- `UMLClass`: diagram model for types and members
- `UMLLayout`: layered, inheritance-oriented layout
- `UMLRenderer`: interactive Java2D/Swing viewer
- `UMLVisualizer`: embeddable Java API
- `UMLCli`: standalone CLI

## Current scope

The parser handles multiple top-level and nested class/interface/enum declarations, record declarations as class-like types, fields, methods, and constructors. It resolves inheritance and interfaces when it can identify the target in the parsed source tree.

It does not yet provide complete symbol resolution, associations/composition, or reliable cross-package import resolution. Its layout is optimized for inheritance and may be crowded for complex projects.

## Development

Run tests:

```bash
mvn -f src/pom.xml test
```

The Java-only desktop rewrite is on `refactor/java-desktop`. The original application remains on `main`.
