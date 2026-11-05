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

    @Test
    void distributesManyUnrelatedTypesAcrossRowsAndColumns() {
        List<UMLClass> types = java.util.stream.IntStream.range(0, 16)
                .mapToObj(index -> new UMLClass("Type" + index, UMLClass.Kind.CLASS))
                .toList();

        Map<UMLClass, Rectangle2D> positions = layout.layout(types, sizer);
        long distinctRows = positions.values().stream()
                .map(Rectangle2D::getMinY).distinct().count();
        long distinctColumns = positions.values().stream()
                .map(Rectangle2D::getMinX).distinct().count();

        assertTrue(distinctRows >= 3);
        assertTrue(distinctColumns >= 3);
        for (int i = 0; i < types.size(); i++) {
            for (int j = i + 1; j < types.size(); j++) {
                assertFalse(positions.get(types.get(i)).intersects(positions.get(types.get(j))));
            }
        }
    }

    @Test
    void keepsConnectedTypesNearEachOther() {
        UMLClass hub = new UMLClass("Hub", UMLClass.Kind.CLASS);
        UMLClass a = new UMLClass("A", UMLClass.Kind.CLASS);
        UMLClass b = new UMLClass("B", UMLClass.Kind.CLASS);
        a.setParentClass(hub);
        b.setParentClass(hub);

        var positions = layout.layout(List.of(a, b, hub), sizer);

        assertEquals(3, positions.size());
        assertTrue(positions.get(a).getMinY() >= 0);
        assertTrue(positions.get(b).getMinY() >= 0);
    }

}
