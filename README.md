# UMLVisualizer

Explore a Java codebase's structure as an interactive UML class diagram. Point UMLVisualizer at a source directory to see its types, inheritance, interfaces, and field relationships, then select a type to inspect its members and connections.

UMLVisualizer is a Java desktop application built with JavaParser, Java2D/Swing, and Picocli. Run it as a standalone JAR, use the CLI for a text summary, or open the viewer from another Java application.

## Features

- Recursively scans `.java` files without compiling the project being analyzed.
- Parses classes, abstract classes, interfaces, enums, records, and nested type declarations. Records currently appear as classes.
- Displays declared fields, methods, and constructors with visibility and static/abstract styling.
- Groups related types in a two-dimensional layout.
- Connects boxes with straight segments and right-angle bends; aligned boxes use a single straight line.
- Highlights a selected type's direct relationships and dims unrelated types.
- Provides pan, zoom, fit-to-view, a minimap, and a member inspector. Boxes preview up to four fields and four methods; the inspector shows the full parsed member list.

## Command-line usage

```text
java -jar build/libs/umlvisualizer-all.jar [OPTIONS] [SOURCE_DIRECTORY]
```

The source directory defaults to the current working directory. All `.java` files below it are scanned, so choose a source folder to narrow the diagram to the code you care about.

| Option | Behavior |
| --- | --- |
| `--summary` | Print the number of parsed types, then each type's kind and qualified name, without opening a window |
| `-h`, `--help` | Show usage and available options |
| `-V`, `--version` | Print the application version |

Open another project's diagram:

```bash
java -jar build/libs/umlvisualizer-all.jar /path/to/project/src/main/java
```

Inspect a project without a desktop:

```bash
java -jar build/libs/umlvisualizer-all.jar --summary /path/to/project/src/main/java
```

Run directly through Gradle:

```bash
./gradlew run --args="src/main/java"
./gradlew run --args="--summary src/main/java"
```

## Viewer controls

| Action | Control |
| --- | --- |
| Pan | Drag the canvas |
| Zoom | Mouse wheel or `+` / `-` |
| Fit the diagram | `F` |
| Reset zoom to 100% | `R` |
| Inspect a type and highlight its connections | Click its box |
| Clear selection | `Esc` or click empty canvas |

## Embed in a Java application

Add `build/libs/umlvisualizer-all.jar` to your application's classpath and call `initFrom` with the source directory:

```java
import java.nio.file.Path;
import umlrenderer.UMLVisualizer;

public class Main {
    public static void main(String[] args) {
        UMLVisualizer.initFrom(Path.of("src/main/java"));
    }
}
```

## Demo

<img width="600" alt="image" src="https://github.com/user-attachments/assets/52d9c961-9988-4891-948f-d4838c9a2ea7" />

