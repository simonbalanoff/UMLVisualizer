package umlrenderer;

import org.junit.jupiter.api.Test;

import java.awt.geom.Rectangle2D;

import static org.junit.jupiter.api.Assertions.*;

class UMLEdgeRouterTest {
    @Test
    void routesVerticalRelationshipsWithRightAngleBends() {
        var source = new Rectangle2D.Double(0, 200, 120, 60);
        var target = new Rectangle2D.Double(180, 0, 120, 60);
        var path = UMLEdgeRouter.route(source, target);

        assertEquals(source.getMinY(), path.get(0).getY());
        assertEquals(target.getMaxY(), path.get(path.size() - 1).getY());
        assertOrthogonal(path);
    }

    @Test
    void routesHorizontalRelationshipsFromBoxBoundaries() {
        var source = new Rectangle2D.Double(0, 0, 120, 60);
        var target = new Rectangle2D.Double(260, 100, 120, 60);
        var path = UMLEdgeRouter.route(source, target);

        assertEquals(source.getMaxX(), path.get(0).getX());
        assertEquals(target.getMinX(), path.get(path.size() - 1).getX());
        assertOrthogonal(path);
    }

    @Test
    void handlesOverlappingProjectionsWithoutDiagonalSegments() {
        var source = new Rectangle2D.Double(0, 0, 200, 100);
        var target = new Rectangle2D.Double(50, 40, 200, 100);

        assertOrthogonal(UMLEdgeRouter.route(source, target));
    }

    private static void assertOrthogonal(java.util.List<java.awt.geom.Point2D> points) {
        for (int i = 1; i < points.size(); i++) {
            var a = points.get(i - 1);
            var b = points.get(i);
            assertTrue(a.getX() == b.getX() || a.getY() == b.getY());
        }
    }
}
