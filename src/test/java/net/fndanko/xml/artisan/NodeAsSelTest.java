package net.fndanko.xml.artisan;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class NodeAsSelTest {

    // --- Node ha tutti i metodi di Sel ---

    @Test
    void node_hasMethods_allSelMethodsAccessible() {
        // Arrange
        XML xml = XML.parse("<root><item id=\"1\">text</item></root>");
        Node node = xml.sel("//item").first();

        // Act / Assert — verify Sel methods are callable on Node
        assertDoesNotThrow(() -> node.attr("id"));
        assertDoesNotThrow(() -> node.text());
        assertDoesNotThrow(() -> node.size());
        assertDoesNotThrow(() -> node.empty());
        assertDoesNotThrow(() -> node.sel("*"));
        assertDoesNotThrow(() -> node.end());
        assertDoesNotThrow(() -> node.stream());
        assertDoesNotThrow(() -> node.list());
        assertDoesNotThrow(() -> node.first());
        assertDoesNotThrow(() -> node.last());
        assertDoesNotThrow(() -> node.iterator());
    }

    // --- node.sel(xpath) crea sotto-selezione dal nodo ---

    @Test
    void sel_fromNode_createsSubSelectionFromNode() {
        // Arrange
        XML xml = XML.parse(
                "<root><parent><child>found</child></parent><child>other</child></root>");
        Node parent = xml.sel("//parent").first();

        // Act
        Sel sub = parent.sel("child");

        // Assert
        assertEquals(1, sub.size());
        assertEquals("found", sub.text());
    }

    // --- node.attr(name, value) funziona come Sel di cardinalita' 1 ---

    @Test
    void attr_setOnNode_worksLikeSelOfOne() {
        // Arrange
        XML xml = XML.parse("<root><item id=\"old\"/></root>");
        Node node = xml.sel("//item").first();

        // Act
        Sel result = node.attr("id", "new");

        // Assert
        assertNotNull(result);
        assertEquals("new", xml.get("//item/@id"));
    }

    // --- Node ottenuto da iterazione e' utilizzabile come Sel ---

    @Test
    void iteration_nodeFromIterator_usableAsSel() {
        // Arrange
        XML xml = XML.parse("<root><a val=\"x\"/><a val=\"y\"/></root>");
        Sel sel = xml.sel("//a");

        // Act / Assert
        int count = 0;
        for (Node node : sel) {
            assertFalse(node.empty());
            assertFalse(node.attr("val").isEmpty());
            assertEquals(1, node.size());
            count++;
        }
        assertEquals(2, count);
    }
}
