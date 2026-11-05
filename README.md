# UMLVisualizer

Explore the structure of a Java codebase as an interactive UML class diagram. Point UMLVisualizer at a source directory to see its types, inheritance, interfaces, and field relationships, then select a type to inspect its members and connections.

UMLVisualizer is a Java desktop application built with JavaParser, Java2D/Swing, and Picocli. Run it as a standalone JAR, use the CLI for a text summary, or open the viewer from another Java application.

## Features

- Recursively scans `.java` files without compiling the project being analyzed.
- Parses classes, abstract classes, interfaces, enums, records, and nested type declarations. Records currently appear as classes.
- Displays declared fields, methods, and constructors with visibility and static/abstract styling.
- Groups related types in a two-dimensional layout.
- Connects boxes with straight segments and right-angle bends; aligned boxes use a single straight line.
- Highlights a selected type's direct relationships and dims unrelated types.
- Provides pan, zoom, fit-to-view, a minimap, and a member inspector. Boxes preview up to four fields and four methods; the inspector shows the full parsed member list.

## Quick start

Build with **JDK 21** using the included Gradle wrapper:

```bash
./gradlew build
```

Then open a diagram of this project's source:

```bash
java -jar build/libs/umlvisualizer-all.jar src/main/java
```

Replace `src/main/java` with the source directory you want to explore. The standalone JAR includes its dependencies and runs on **Java 17 or newer**. Opening the viewer requires a graphical desktop; `--summary` also works in headless environments.

On Windows, use `gradlew.bat` in place of `./gradlew`. A separate Gradle installation is not required. The first build needs network access to download Gradle and dependencies.

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

The CLI returns `0` on success, `1` when no Java types are found, and `2` for an invalid directory or an attempt to open the viewer in a headless environment. Parsing failures are reported as errors.

## Reading the diagram

| Relationship | Appearance |
| --- | --- |
| Class inheritance (`extends`) | Solid blue line with a hollow triangle pointing to the parent |
| Interface relationship (`implements` or interface `extends`) | Dashed green line with a hollow triangle pointing to the interface |
| Field association | Solid gray connector without an arrowhead |

Field associations connect types found in the scanned source tree, including generic arguments such as `List<Customer>`. Primitive types and unresolved references do not create connectors. Multiple fields referencing the same target produce one association per source/target pair.

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

The viewer opens on Swing's event dispatch thread. Closing it does not terminate the host application. `UMLVisualizer.init()` also offers automatic source-directory discovery from the working directory; use `initFrom(Path)` to control the scan explicitly.

## Development

The build uses a JDK 21 toolchain and compiles with `--release 17`. Tests use JUnit 5.

| Task | Command |
| --- | --- |
| Run tests | `./gradlew test` |
| Build, test, and package | `./gradlew build` |
| Create the standalone JAR | `./gradlew shadowJar` |
| Clean build outputs | `./gradlew clean` |

The main code lives in [`src/main/java/umlrenderer`](src/main/java/umlrenderer), with tests in [`src/test/java/umlrenderer`](src/test/java/umlrenderer).

| Component | Responsibility |
| --- | --- |
| `UMLParser` | Parse source declarations and resolve inheritance/interface references |
| `UMLClass` | Store type and member information |
| `UMLRelationships` / `UMLRelationship` | Infer and represent diagram connections |
| `UMLLayout` | Position and size groups of related types |
| `UMLEdgeRouter` | Route straight segments between box boundaries |
| `UMLRenderer` | Draw the diagram and handle desktop interaction |
| `UMLVisualizer` | Expose the embedded viewer API |
| `UMLCli` | Handle command-line arguments and headless summaries |

## Limitations

- Relationship resolution uses names and source context, without full Java symbol resolution or import-aware lookup. Ambiguous or unresolved names may leave connections out, and external library types are not added to the diagram.
- Records are displayed as classes; record components and compiler-generated members are not synthesized. Annotation declarations and local types inside method bodies are not modeled.
- Associations are inferred from declared field types. Method calls, parameter dependencies, aggregation, composition, and multiplicities are not inferred.
- Routing uses straight segments but does not avoid intervening boxes or guarantee separate lanes for overlapping connections. The layout does not guarantee zero crossings.
- The viewer does not currently provide diagram export or live updates when source files change.

## License

[MIT](LICENSE)
