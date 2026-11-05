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

    @Test
    void alignedBoxesUseSingleStraightSegmentInEveryDirection() {
        var source = new Rectangle2D.Double(0, 0, 120, 60);
        for (var target : java.util.List.of(
                new Rectangle2D.Double(0, 200, 120, 80),
                new Rectangle2D.Double(0, -200, 120, 80),
                new Rectangle2D.Double(260, 0, 160, 60),
                new Rectangle2D.Double(-260, 0, 160, 60),
                new Rectangle2D.Double(120.01, 0, 120, 60))) {
            var path = UMLEdgeRouter.route(source, target);
            assertEquals(2, path.size());
            assertOrthogonal(path);
            assertNotEquals(path.get(0), path.get(1));
        }
    }

    @Test
    void touchingOverlappingAndSelfEdgesKeepVisibleSegments() {
        var source = new Rectangle2D.Double(0, 0, 120, 60);
        for (var target : java.util.List.of(
                source,
                new Rectangle2D.Double(120, 0, 120, 60),
                new Rectangle2D.Double(0, 60, 120, 60),
                new Rectangle2D.Double(30, 0, 120, 60),
                new Rectangle2D.Double(0, 20, 120, 60),
                new Rectangle2D.Double(-10, -10, 140, 80),
                new Rectangle2D.Double(120, 60, 120, 60))) {
            for (var path : java.util.List.of(UMLEdgeRouter.route(source, target),
                    UMLEdgeRouter.route(target, source))) {
                assertOrthogonal(path);
                assertTrue(path.size() >= 2);
                for (int i = 1; i < path.size(); i++) {
                    assertNotEquals(path.get(i - 1), path.get(i), "Zero-length segment");
                }
            }
        }
    }

    @Test
    void routesAllQuadrantsAndUnequalBoxSizes() {
        var source = new Rectangle2D.Double(-60, -30, 120, 60);
        for (double x : new double[]{-300, -80, 0, 80, 300}) {
            for (double y : new double[]{-300, -50, 0, 50, 300}) {
                var target = new Rectangle2D.Double(x, y, 160, 100);
                var path = UMLEdgeRouter.route(source, target);
                assertOrthogonal(path);
                for (int i = 1; i < path.size(); i++) {
                    assertNotEquals(path.get(i - 1), path.get(i));
                }
            }
        }
    }

    private static void assertOrthogonal(java.util.List<java.awt.geom.Point2D> points) {
        for (int i = 1; i < points.size(); i++) {
            var a = points.get(i - 1);
            var b = points.get(i);
            assertTrue(a.getX() == b.getX() || a.getY() == b.getY());
        }
    }
}
