package umlrenderer;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UMLLayout {
    public static final int H_GAP = 120;
    public static final int V_GAP = 130;
    public static final int MIN_W = 205;
    public static final int MARGIN = 70;

    public Map<UMLClass, Rectangle2D> layout(List<UMLClass> classes, NodeSizer sizer) {
        if (classes.isEmpty()) return Map.of();

        Map<UMLClass, Set<UMLClass>> adjacency = new LinkedHashMap<>();
        for (UMLClass type : classes) adjacency.put(type, new HashSet<>());
        for (UMLRelationship relationship : UMLRelationships.analyze(classes)) {
            adjacency.get(relationship.source()).add(relationship.target());
            adjacency.get(relationship.target()).add(relationship.source());
        }

        List<List<UMLClass>> groups = connectedComponents(classes, adjacency);
        groups.sort(Comparator.<List<UMLClass>>comparingInt(List::size).reversed()
                .thenComparing(group -> group.get(0).getQualifiedName()));

        List<UMLClass> isolated = new ArrayList<>();
        groups.removeIf(group -> {
            if (group.size() == 1) {
                isolated.add(group.get(0));
                return true;
            }
            return false;
        });
        if (!isolated.isEmpty()) {
            isolated.sort(Comparator.comparing(UMLClass::getQualifiedName));
            groups.add(isolated);
        }

        Map<UMLClass, Rectangle2D> bounds = new LinkedHashMap<>();
        double cursorX = MARGIN;
        double cursorY = MARGIN;
        double bandHeight = 0;
        double maxRowWidth = 1550;

        for (List<UMLClass> group : groups) {
            List<UMLClass> ordered = orderByConnections(group, adjacency);
            int columns = Math.min(4, Math.max(1, (int) Math.ceil(Math.sqrt(ordered.size() * 1.25))));
            int rows = (ordered.size() + columns - 1) / columns;
            double[] widths = new double[columns];
            double[] heights = new double[rows];

            for (int index = 0; index < ordered.size(); index++) {
                UMLClass type = ordered.get(index);
                int column = index % columns;
                int row = index / columns;
                widths[column] = Math.max(widths[column], Math.max(MIN_W, sizer.width(type)));
                heights[row] = Math.max(heights[row], sizer.height(type));
            }

            double groupWidth = (columns - 1) * H_GAP;
            for (double width : widths) groupWidth += width;
            double groupHeight = (rows - 1) * V_GAP;
            for (double height : heights) groupHeight += height;

            if (cursorX > MARGIN && cursorX + groupWidth > maxRowWidth) {
                cursorX = MARGIN;
                cursorY += bandHeight + V_GAP * 1.2;
                bandHeight = 0;
            }

            double[] lefts = new double[columns];
            double[] tops = new double[rows];
            double x = cursorX;
            for (int i = 0; i < columns; i++) {
                lefts[i] = x;
                x += widths[i] + H_GAP;
            }
            double y = cursorY;
            for (int i = 0; i < rows; i++) {
                tops[i] = y;
                y += heights[i] + V_GAP;
            }

            for (int index = 0; index < ordered.size(); index++) {
                UMLClass type = ordered.get(index);
                int column = index % columns;
                int row = index / columns;
                double width = Math.max(MIN_W, sizer.width(type));
                bounds.put(type, new Rectangle2D.Double(
                        lefts[column] + (widths[column] - width) / 2,
                        tops[row], width, sizer.height(type)));
            }

            cursorX += groupWidth + H_GAP * 1.3;
            bandHeight = Math.max(bandHeight, groupHeight);
        }

        return bounds;
    }

    private List<List<UMLClass>> connectedComponents(
            List<UMLClass> classes, Map<UMLClass, Set<UMLClass>> adjacency) {
        List<List<UMLClass>> components = new ArrayList<>();
        Set<UMLClass> seen = new HashSet<>();
        for (UMLClass start : classes) {
            if (!seen.add(start)) continue;
            List<UMLClass> component = new ArrayList<>();
            ArrayDeque<UMLClass> queue = new ArrayDeque<>();
            queue.add(start);
            while (!queue.isEmpty()) {
                UMLClass current = queue.removeFirst();
                component.add(current);
                List<UMLClass> neighbors = new ArrayList<>(adjacency.get(current));
                neighbors.sort(Comparator.comparing(UMLClass::getQualifiedName));
                for (UMLClass neighbor : neighbors) {
                    if (seen.add(neighbor)) queue.addLast(neighbor);
                }
            }
            components.add(component);
        }
        return components;
    }

    private List<UMLClass> orderByConnections(
            List<UMLClass> group, Map<UMLClass, Set<UMLClass>> adjacency) {
        UMLClass root = group.stream().min(Comparator
                .<UMLClass>comparingInt(type -> -adjacency.get(type).size())
                .thenComparing(UMLClass::getQualifiedName)).orElseThrow();
        List<UMLClass> ordered = new ArrayList<>();
        Set<UMLClass> seen = new HashSet<>();
        ArrayDeque<UMLClass> queue = new ArrayDeque<>();
        queue.add(root);
        seen.add(root);
        while (!queue.isEmpty()) {
            UMLClass current = queue.removeFirst();
            ordered.add(current);
            List<UMLClass> neighbors = new ArrayList<>(adjacency.get(current));
            neighbors.sort(Comparator.<UMLClass>comparingInt(type -> -adjacency.get(type).size())
                    .thenComparing(UMLClass::getQualifiedName));
            for (UMLClass neighbor : neighbors) {
                if (seen.add(neighbor)) queue.addLast(neighbor);
            }
        }
        return ordered;
    }

    @FunctionalInterface
    public interface NodeSizer {
        int width(UMLClass type);

        default int height(UMLClass type) {
            return 56 + Math.min(type.getFields().size(), 4) * 18
                    + Math.min(type.getMethods().size(), 4) * 18 + 22;
        }
    }

    public static Point2D[] inheritanceEndpoints(Rectangle2D parent, Rectangle2D child) {
        return new Point2D[]{
                new Point2D.Double(parent.getCenterX(), parent.getMaxY()),
                new Point2D.Double(child.getCenterX(), child.getMinY())
        };
    }

    public static Point2D[] implementsEndpoints(Rectangle2D iface, Rectangle2D impl) {
        boolean left = iface.getCenterX() < impl.getCenterX();
        return new Point2D[]{
                new Point2D.Double(left ? iface.getMaxX() : iface.getMinX(), iface.getCenterY()),
                new Point2D.Double(left ? impl.getMinX() : impl.getMaxX(), impl.getCenterY())
        };
    }
}
