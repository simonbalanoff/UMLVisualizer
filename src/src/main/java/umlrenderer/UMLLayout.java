package umlrenderer;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;

public class UMLLayout {
    // Horizontal gap between sibling nodes on the same layer.
    public static final int H_GAP = 60;
    // Vertical gap between layers.
    public static final int V_GAP = 80;
    // Minimum node width.
    public static final int MIN_W = 160;
    // Left/top canvas margin.
    public static final int MARGIN = 40;

    public Map<UMLClass, Rectangle2D> layout(
            List<UMLClass> classes,
            NodeSizer sizer) {

        if (classes.isEmpty()) return Collections.emptyMap();

        Map<UMLClass, Integer> layer = assignLayers(classes);

        int maxLayer = layer.values().stream().mapToInt(i -> i).max().orElse(0);
        List<List<UMLClass>> layers = new ArrayList<>();
        for (int i = 0; i <= maxLayer; i++) layers.add(new ArrayList<>());
        for (UMLClass c : classes) layers.get(layer.get(c)).add(c);

        sortLayers(layers, layer);

        Map<UMLClass, Double> nodeH = new HashMap<>();
        for (UMLClass c : classes) nodeH.put(c, (double) sizer.height(c));

        Map<Integer, Double> layerY = new HashMap<>();
        double y = MARGIN;
        for (int i = 0; i <= maxLayer; i++) {
            layerY.put(i, y);
            double maxH = layers.get(i).stream()
                    .mapToDouble(c -> nodeH.get(c)).max().orElse(0);
            y += maxH + V_GAP;
        }

        Map<UMLClass, Double> nodeW = new HashMap<>();
        for (UMLClass c : classes) nodeW.put(c, (double) Math.max(MIN_W, sizer.width(c)));

        Map<UMLClass, Double> xPos = assignX(layers, nodeW);

        Map<UMLClass, Rectangle2D> bounds = new LinkedHashMap<>();
        for (UMLClass c : classes) {
            double cx = xPos.getOrDefault(c, (double) MARGIN);
            double cy = layerY.get(layer.get(c));
            double w = nodeW.get(c);
            double h = nodeH.get(c);
            bounds.put(c, new Rectangle2D.Double(cx, cy, w, h));
        }

        return bounds;
    }

    private Map<UMLClass, Integer> assignLayers(List<UMLClass> classes) {
        Map<UMLClass, Integer> layer = new HashMap<>();

        Queue<UMLClass> queue = new ArrayDeque<>();
        for (UMLClass c : classes) {
            if (c.isRoot()) {
                layer.put(c, 0);
                queue.add(c);
            }
        }

        for (UMLClass c : classes) {
            if (!layer.containsKey(c)) {
                layer.put(c, 0);
                queue.add(c);
            }
        }

        while (!queue.isEmpty()) {
            UMLClass cur = queue.poll();
            int childLayer = layer.get(cur) + 1;
            for (UMLClass sub : cur.getSubclasses()) {
                if (!layer.containsKey(sub) || layer.get(sub) < childLayer) {
                    layer.put(sub, childLayer);
                    queue.add(sub);
                }
            }
        }

        return layer;
    }

    private void sortLayers(List<List<UMLClass>> layers, Map<UMLClass, Integer> layerMap) {
        for (int i = 1; i < layers.size(); i++) {
            List<UMLClass> prev = layers.get(i - 1);
            Map<UMLClass, Integer> prevPos = new HashMap<>();
            for (int j = 0; j < prev.size(); j++) prevPos.put(prev.get(j), j);

            layers.get(i).sort((a, b) -> {
                double ba = barycentre(a, prevPos);
                double bb = barycentre(b, prevPos);
                return Double.compare(ba, bb);
            });
        }
    }

    private double barycentre(UMLClass node, Map<UMLClass, Integer> parentPositions) {
        UMLClass p = node.getParentClass();
        if (p == null) return Double.MAX_VALUE;
        Integer pos = parentPositions.get(p);
        return pos == null ? Double.MAX_VALUE : pos;
    }

    private Map<UMLClass, Double> assignX(
            List<List<UMLClass>> layers,
            Map<UMLClass, Double> nodeW) {

        Map<UMLClass, Double> x = new HashMap<>();

        Map<UMLClass, Double> subtreeW = new HashMap<>();
        for (int i = layers.size() - 1; i >= 0; i--) {
            for (UMLClass node : layers.get(i)) {
                List<UMLClass> children = node.getSubclasses();
                if (children.isEmpty()) {
                    subtreeW.put(node, nodeW.get(node));
                } else {
                    double span = children.stream()
                            .mapToDouble(c -> subtreeW.getOrDefault(c, nodeW.get(c)) + H_GAP)
                            .sum() - H_GAP;
                    subtreeW.put(node, Math.max(nodeW.get(node), span));
                }
            }
        }

        double cursor = MARGIN;
        for (UMLClass root : layers.get(0)) {
            placeSubtree(root, cursor, x, nodeW, subtreeW);
            cursor += subtreeW.get(root) + H_GAP;
        }

        return x;
    }

    private void placeSubtree(
            UMLClass node,
            double left,
            Map<UMLClass, Double> x,
            Map<UMLClass, Double> nodeW,
            Map<UMLClass, Double> subtreeW) {

        double span = subtreeW.get(node);
        double w = nodeW.get(node);
        x.put(node, left + (span - w) / 2.0);

        List<UMLClass> children = node.getSubclasses();
        if (children.isEmpty()) return;

        double childCursor = left;
        for (UMLClass child : children) {
            placeSubtree(child, childCursor, x, nodeW, subtreeW);
            childCursor += subtreeW.getOrDefault(child, nodeW.get(child)) + H_GAP;
        }
    }

    @FunctionalInterface
    public interface NodeSizer {
        int width(UMLClass c);

        default int height(UMLClass c) {
            int rows = 2 + c.getFields().size() + c.getMethods().size();
            return rows * 18 + 28; // 18px per row, 28px header
        }
    }

    public static Point2D[] inheritanceEndpoints(Rectangle2D parent, Rectangle2D child) {
        double px = parent.getCenterX();
        double py = parent.getMaxY();
        double cx = child.getCenterX();
        double cy = child.getMinY();
        return new Point2D[]{
                new Point2D.Double(px, py),
                new Point2D.Double(cx, cy)
        };
    }

    public static Point2D[] implementsEndpoints(Rectangle2D iface, Rectangle2D impl) {
        boolean ifaceLeft = iface.getCenterX() < impl.getCenterX();
        double ix = ifaceLeft ? iface.getMaxX() : iface.getMinX();
        double iy = iface.getCenterY();
        double cx = ifaceLeft ? impl.getMinX() : impl.getMaxX();
        double cy = impl.getCenterY();
        return new Point2D[]{
                new Point2D.Double(ix, iy),
                new Point2D.Double(cx, cy)
        };
    }
}