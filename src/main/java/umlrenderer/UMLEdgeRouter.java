package umlrenderer;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.ArrayList;

public final class UMLEdgeRouter {
    private UMLEdgeRouter() {}

    public static List<Point2D> route(Rectangle2D source, Rectangle2D target) {
        // Concentric boxes need a visible loop with a nonzero final arrow segment.
        if (source.getCenterX() == target.getCenterX()
                && source.getCenterY() == target.getCenterY()) {
            return List.of(
                    point(source.getMaxX(), source.getCenterY()),
                    point(Math.max(source.getMaxX(), target.getMaxX()) + 36, source.getCenterY()),
                    point(Math.max(source.getMaxX(), target.getMaxX()) + 36, Math.min(source.getMinY(), target.getMinY()) - 36),
                    point(source.getCenterX(), Math.min(source.getMinY(), target.getMinY()) - 36),
                    point(target.getCenterX(), target.getMinY()));
        }

        double verticalGap = Math.max(target.getMinY() - source.getMaxY(),
                source.getMinY() - target.getMaxY());
        double horizontalGap = Math.max(target.getMinX() - source.getMaxX(),
                source.getMinX() - target.getMaxX());

        if (verticalGap > 0 && (verticalGap >= horizontalGap || horizontalGap <= 0)) {
            boolean down = target.getCenterY() > source.getCenterY();
            double startY = down ? source.getMaxY() : source.getMinY();
            double endY = down ? target.getMinY() : target.getMaxY();
            double middleY = (startY + endY) / 2;
            return simplify(List.of(
                    point(source.getCenterX(), startY),
                    point(source.getCenterX(), middleY),
                    point(target.getCenterX(), middleY),
                    point(target.getCenterX(), endY)
            ));
        }

        if (horizontalGap > 0) {
            boolean right = target.getCenterX() > source.getCenterX();
            double startX = right ? source.getMaxX() : source.getMinX();
            double endX = right ? target.getMinX() : target.getMaxX();
            double middleX = (startX + endX) / 2;
            return simplify(List.of(
                    point(startX, source.getCenterY()),
                    point(middleX, source.getCenterY()),
                    point(middleX, target.getCenterY()),
                    point(endX, target.getCenterY())
            ));
        }

        if (source.getCenterY() == target.getCenterY()) {
            double outsideY = Math.min(source.getMinY(), target.getMinY()) - 36;
            return List.of(point(source.getCenterX(), source.getMinY()),
                    point(source.getCenterX(), outsideY),
                    point(target.getCenterX(), outsideY),
                    point(target.getCenterX(), target.getMinY()));
        }

        boolean right = target.getCenterX() >= source.getCenterX();
        double startX = right ? source.getMaxX() : source.getMinX();
        double endX = right ? target.getMaxX() : target.getMinX();
        double outsideX = right
                ? Math.max(source.getMaxX(), target.getMaxX()) + 36
                : Math.min(source.getMinX(), target.getMinX()) - 36;
        return List.of(
                point(startX, source.getCenterY()),
                point(outsideX, source.getCenterY()),
                point(outsideX, target.getCenterY()),
                point(endX, target.getCenterY())
        );
    }

    private static List<Point2D> simplify(List<Point2D> points) {
        List<Point2D> result = new ArrayList<>();
        for (Point2D next : points) {
            if (!result.isEmpty() && result.get(result.size() - 1).equals(next)) continue;
            while (result.size() >= 2) {
                Point2D a = result.get(result.size() - 2);
                Point2D b = result.get(result.size() - 1);
                if ((a.getX() == b.getX() && b.getX() == next.getX())
                        || (a.getY() == b.getY() && b.getY() == next.getY())) {
                    result.remove(result.size() - 1);
                } else break;
            }
            result.add(next);
        }
        return List.copyOf(result);
    }

    private static Point2D point(double x, double y) {
        return new Point2D.Double(x, y);
    }
}
