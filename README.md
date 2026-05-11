# UMLVisualizer

**Zero-config UML class diagram generator for Java projects.** Drop two lines into your `main` method and UMLVisualizer will scan your source tree, parse every class, interface, abstract class, and enum it finds, and open an interactive diagram window - no plugins, no build tool integration, no external dependencies beyond the JDK.

---

## What it does

UMLVisualizer parses your `.java` source files directly (no compilation required) and produces a live, pannable, zoomable class diagram showing:

- **Classes, abstract classes, interfaces, and enums** - each visually distinct with its own color coding and stereotype label
- **Fields and methods** - with visibility symbols (`+` `-` `#` `~`), type annotations, and parameter lists; static members are underlined, abstract members italicized
- **Inheritance arrows** - solid lines with hollow triangles pointing to the parent class
- **Interface implementation arrows** - dashed lines with hollow triangles pointing to the interface
- **Automatic hierarchical layout** - classes are arranged by inheritance depth with parent–child relationships driving the tree structure

---

## Getting started

### 1. Add UMLVisualizer to your project

Copy the five source files into your project under the `umlrenderer` package:

```
src/
└── umlrenderer/
    ├── UMLVisualizer.java
    ├── UMLParser.java
    ├── UMLLayout.java
    ├── UMLRenderer.java
    └── UMLClass.java
```

### 2. Call `UMLVisualizer.init()` from your main method

```java
public static void main(String[] args) {
    UMLVisualizer.init();
    // ... rest of your application
}
```

That's it. `init()` will locate your source root automatically by finding the directory containing a `public static void main` method, scan all `.java` files beneath it, and open the diagram in a new window.

### 3. Point it at a specific directory (optional)

If you'd rather choose the directory yourself:

```java
UMLVisualizer.initFrom(Path.of("src/main/java/com/example"));
```

---

## Navigating the diagram

| Action | How |
|---|---|
| **Pan** | Click and drag on the canvas |
| **Zoom** | Scroll wheel (zooms toward cursor) |
| **Fit all** | Press `F` |
| **Reset zoom** | Press `R` |
| **Inspect a class** | Click any node to open it in the sidebar |
| **Deselect** | Press `Esc` |
| **Zoom in / out** | `+` / `-` keys |

The minimap in the bottom-right corner shows the full diagram extent and highlights the current viewport.

---

## The sidebar

Clicking a class node opens a detail panel on the right showing:

- Kind (`class`, `«abstract»`, `«interface»`, `«enum»`)
- Parent class (if any)
- Implemented interfaces (if any)
- Subclasses
- All fields with visibility, type, and modifiers
- All methods with visibility, return type, parameters, and modifiers

---

## How it works

### Parsing (`UMLParser`)

Source files are processed through a single-pass sanitizer that blanks out the content of all string literals, char literals, text blocks, block comments, and line comments before any parsing takes place. This means braces, slashes, and quotes inside comments or strings are invisible to the rest of the parser.

Type declarations are matched with a regex that captures the class/interface/enum keyword, modifiers, name, generic bounds, `extends` clause, and `implements` clause. The class body is extracted using a brace-depth counter on the sanitized source, then fields and methods are matched with separate patterns inside that body.

### Layout (`UMLLayout`)

Classes are arranged using a layered tree layout:

1. Each class is assigned a layer based on its depth in the inheritance hierarchy (roots at layer 0, subclasses one layer below their parent).
2. Within each layer, nodes are sorted by barycentre to minimize edge crossings.
3. Subtree widths are computed bottom-up so that parent nodes are centred over their children.
4. Nodes are placed top-down with configurable horizontal and vertical gaps.

### Rendering (`UMLRenderer`)

The diagram is drawn with Java2D with full antialiasing. The camera transform (pan + zoom) is applied as a `Graphics2D` scale+translate so all world-space coordinates stay stable - the grid, node positions, and edge endpoints are all defined in world space and projected to the screen on each paint. Zoom is animated with a frame-rate-independent exponential lerp; panning snaps immediately to the cursor with no lag.

---

## Project structure

| File | Responsibility |
|---|---|
| `UMLVisualizer.java` | Public entry point - `init()` and `initFrom()` |
| `UMLParser.java` | Source file parsing and relationship resolution |
| `UMLLayout.java` | Hierarchical layout algorithm |
| `UMLRenderer.java` | Swing window, canvas painting, sidebar, input handling |
| `UMLClass.java` | Data model - classes, members, relationships |

---

## Requirements

- Java 17 or later (uses records-style switch expressions and `Pattern.MULTILINE`)
- No external libraries - pure JDK + Swing

---

## Limitations

- Parses source files, not bytecode - classes without source (dependencies, JDK types) will not appear
- One top-level type per file is parsed (the first type declaration found)
- Generic type parameters are stripped from display labels for brevity
- Very large projects (500+ classes) may produce a dense diagram; use `initFrom()` to scope to a specific package