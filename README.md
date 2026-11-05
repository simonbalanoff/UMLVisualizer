# UMLVisualizer

A Java-only UML class diagram explorer for source projects. Run it from the command line as a standalone JAR or invoke it inside another Java program.

Uses **JavaParser** for AST-based source analysis, **Java2D/Swing** for an interactive desktop viewer, and **Gradle Kotlin DSL** for builds.

## Requirements

JDK 17 or newer. Gradle is not required: use the checked-in wrapper.

## Build

```bash
./gradlew clean build
```

The standalone executable is `build/libs/umlvisualizer-all.jar`.

## Run

Analyze a source directory and open the desktop diagram:

```bash
java -jar build/libs/umlvisualizer-all.jar path/to/project/src/main/java
```

Analyze the current directory:

```bash
./gradlew run
```

Print a headless summary:

```bash
java -jar build/libs/umlvisualizer-all.jar --summary path/to/project
```

Show CLI options:

```bash
java -jar build/libs/umlvisualizer-all.jar --help
```

## Embed in a Java application

Include `umlvisualizer-all.jar` in your classpath and call:

```java
import java.nio.file.Path;
import umlrenderer.UMLVisualizer;

public class Main {
    public static void main(String[] args) {
        UMLVisualizer.initFrom(Path.of("src/main/java"));
    }
}
```

Closing the viewer will not terminate the host application's JVM.

## Viewer controls

| Action | Control |
| --- | --- |
| Pan | Drag canvas |
| Zoom | Mouse wheel |
| Fit all | F |
| Reset zoom | R |
| Select a class | Click its node |
| Deselect | Esc |
| Zoom in/out | + / - |

## Development commands

| Task | Command |
| --- | --- |
| Run tests | `./gradlew test` |
| Build and test | `./gradlew build` |
| Create standalone JAR | `./gradlew shadowJar` |
| Run CLI | `./gradlew run --args="--summary ."` |
| Clean | `./gradlew clean` |

## Architecture

- `src/main/java/umlrenderer/UMLParser.java`: AST-based source parsing
- `src/main/java/umlrenderer/UMLClass.java`: UML types, members, and relationships
- `src/main/java/umlrenderer/UMLLayout.java`: hierarchy-based layout algorithm
- `src/main/java/umlrenderer/UMLRenderer.java`: interactive Java2D/Swing visualization
- `src/main/java/umlrenderer/UMLVisualizer.java`: embeddable API
- `src/main/java/umlrenderer/UMLCli.java`: Picocli entry point

## Current limitations

Complete Java symbol resolution and import-aware relationship resolution are not implemented. Associations, aggregation, and composition are not yet inferred. The layout is optimized for inheritance relationships.
