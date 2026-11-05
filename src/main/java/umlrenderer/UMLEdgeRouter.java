package umlrenderer;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

public final class UMLEdgeRouter {
    private UMLEdgeRouter() {}

    public static List<Point2D> route(Rectangle2D source, Rectangle2D target) {
        double verticalGap = Math.max(target.getMinY() - source.getMaxY(),
                source.getMinY() - target.getMaxY());
        double horizontalGap = Math.max(target.getMinX() - source.getMaxX(),
                source.getMinX() - target.getMaxX());

        if (verticalGap > 0 && (verticalGap >= horizontalGap || horizontalGap <= 0)) {
            boolean down = target.getCenterY() > source.getCenterY();
            double startY = down ? source.getMaxY() : source.getMinY();
            double endY = down ? target.getMinY() : target.getMaxY();
            double middleY = (startY + endY) / 2;
            return List.of(
                    point(source.getCenterX(), startY),
                    point(source.getCenterX(), middleY),
                    point(target.getCenterX(), middleY),
                    point(target.getCenterX(), endY)
            );
        }

        if (horizontalGap > 0) {
            boolean right = target.getCenterX() > source.getCenterX();
            double startX = right ? source.getMaxX() : source.getMinX();
            double endX = right ? target.getMinX() : target.getMaxX();
            double middleX = (startX + endX) / 2;
            return List.of(
                    point(startX, source.getCenterY()),
                    point(middleX, source.getCenterY()),
                    point(middleX, target.getCenterY()),
                    point(endX, target.getCenterY())
            );
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

    private static Point2D point(double x, double y) {
        return new Point2D.Double(x, y);
    }
}
