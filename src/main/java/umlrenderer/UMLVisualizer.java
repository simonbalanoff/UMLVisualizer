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
        return new UMLLayout.NodeSizer() {
            @Override
            public int width(UMLClass type) {
                int max = type.getClassName().length();
                for (UMLClass.Member field : type.getFields()) {
                    max = Math.max(max, field.displayLabel().length());
                }
                for (UMLClass.Member method : type.getMethods()) {
                    max = Math.max(max, method.displayLabel().length());
                }
                return Math.max(215, Math.min(285, (int) (max * 7.3) + 36));
            }

            @Override
            public int height(UMLClass type) {
                int fields = Math.min(4, type.getFields().size());
                int methods = Math.min(4, type.getMethods().size());
                if (type.getFields().size() > fields) fields++;
                if (type.getMethods().size() > methods) methods++;
                boolean both = !type.getFields().isEmpty() && !type.getMethods().isEmpty();
                return 44 + (fields + methods) * 18 + (both ? 18 : 0) + 12;
            }
        };
    }
}
