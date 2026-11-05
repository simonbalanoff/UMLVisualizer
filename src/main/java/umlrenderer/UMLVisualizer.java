package umlrenderer;

import java.awt.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.List;

/**
 * Call {@code UMLVisualizer.init()} from your application's {@code main} method
 * (or anywhere during startup) to open the diagram window.
 *
 * <pre>
 *     public static void main(String[] args) {
 *         UMLVisualizer.init();
 *         // ... rest of your app
 *     }
 * </pre>
 */

public class UMLVisualizer {

    private UMLVisualizer() {} // static API only


    public static void init() {
        try {
            Path root = findProjectRoot();
            System.out.println("[UMLVisualizer] Scanning: " + root);

            UMLParser parser = new UMLParser();
            List<UMLClass> classes = parser.parseDirectory(root);
            System.out.printf("[UMLVisualizer] Parsed %d types%n", classes.size());

            if (classes.isEmpty()) {
                System.err.println("[UMLVisualizer] No Java types found — nothing to display.");
                return;
            }

            UMLLayout layout = new UMLLayout();
            UMLLayout.NodeSizer sizer = defaultSizer();
            Map<UMLClass, java.awt.geom.Rectangle2D> bounds = layout.layout(classes, sizer);

            EventQueue.invokeLater(() -> new UMLRenderer(classes, bounds));

        } catch (IOException e) {
            throw new RuntimeException("[UMLVisualizer] Failed to scan project", e);
        }
    }

    public static void initFrom(Path directory) {
        try {
            UMLParser parser = new UMLParser();
            List<UMLClass> classes = parser.parseDirectory(directory);

            UMLLayout layout = new UMLLayout();
            Map<UMLClass, java.awt.geom.Rectangle2D> bounds =
                    layout.layout(classes, defaultSizer());

            EventQueue.invokeLater(() -> new UMLRenderer(classes, bounds));
        } catch (IOException e) {
            throw new RuntimeException("[UMLVisualizer] Failed to parse directory: " + directory, e);
        }
    }

    private static Path findProjectRoot() throws IOException {
        Path cwd = Paths.get("").toAbsolutePath();

        Optional<Path> mainDir = Files.walk(cwd)
                .parallel()
                .filter(p -> p.toString().endsWith(".java"))
                .filter(UMLVisualizer::hasMainMethod)
                .map(Path::getParent)
                .findFirst();

        if (mainDir.isPresent()) return mainDir.get();

        Optional<Path> anyJavaDir = Files.walk(cwd)
                .filter(p -> p.toString().endsWith(".java"))
                .map(Path::getParent)
                .findFirst();

        return anyJavaDir.orElse(cwd);
    }

    private static boolean hasMainMethod(Path path) {
        try {
            return Files.lines(path).anyMatch(l -> l.contains("public static void main"));
        } catch (IOException e) {
            return false;
        }
    }

    private static UMLLayout.NodeSizer defaultSizer() {
        final double charW = 7.5;
        final int PAD = 12;
        final int HEADER_H = 44;
        final int ROW_H = 18;

        return new UMLLayout.NodeSizer() {
            @Override
            public int width(UMLClass c) {
                int max = c.getClassName().length();
                for (UMLClass.Member f : c.getFields())
                    max = Math.max(max, f.displayLabel().length());
                for (UMLClass.Member m : c.getMethods())
                    max = Math.max(max, m.displayLabel().length());
                return (int)(max * charW) + PAD * 2 + 20;
            }

            @Override
            public int height(UMLClass c) {
                int rows = c.getFields().size() + c.getMethods().size();
                boolean hasBoth = !c.getFields().isEmpty() && !c.getMethods().isEmpty();
                return HEADER_H + rows * ROW_H + (hasBoth ? ROW_H : 0) + PAD;
            }
        };
    }
}