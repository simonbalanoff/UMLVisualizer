package umlrenderer;

import org.junit.jupiter.api.Test;

import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UMLLayoutTest {
    private final UMLLayout layout = new UMLLayout();
    private final UMLLayout.NodeSizer sizer = c -> 180;

    @Test
    void returnsEmptyLayoutForEmptyInput() {
        assertTrue(layout.layout(List.of(), sizer).isEmpty());
    }

    @Test
    void positionsChildBelowParent() {
        UMLClass parent = new UMLClass("Parent", UMLClass.Kind.CLASS);
        UMLClass child = new UMLClass("Child", UMLClass.Kind.CLASS);
        child.setParentClass(parent);

        Map<UMLClass, Rectangle2D> positions = layout.layout(List.of(parent, child), sizer);

        assertEquals(2, positions.size());
        assertTrue(positions.get(child).getMinY() > positions.get(parent).getMaxY());
        assertEquals(positions.get(parent).getCenterX(), positions.get(child).getCenterX(), 0.001);
    }

    @Test
    void placesIndependentRootsWithoutOverlap() {
        UMLClass first = new UMLClass("First", UMLClass.Kind.CLASS);
        UMLClass second = new UMLClass("Second", UMLClass.Kind.CLASS);

        Map<UMLClass, Rectangle2D> positions = layout.layout(List.of(first, second), sizer);

        assertFalse(positions.get(first).intersects(positions.get(second)));
    }
}
