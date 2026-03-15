package net.fndanko.xml.artisan;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ResilienceTest {

    // --- Selezione vuota: tutte le operazioni sono no-op ---

    @Test
    void emptySel_allOperations_areNoOp() {
        // Arrange
        XML xml = XML.parse("<root/>");
        Sel empty = xml.sel("//nonexistent");

        // Act / Assert
        assertDoesNotThrow(() -> {
            empty.attr("a");
            empty.attr("a", "v");
            empty.text();
            empty.text("v");
            empty.remove();
            empty.append("tag");
            empty.prepend("tag");
            empty.before("tag");
            empty.after("tag");
            empty.sel("x");
            empty.end();
            empty.size();
            empty.empty();
            empty.stream();
            empty.list();
            empty.first();
            empty.last();
            empty.iterator();
        });
    }

    // --- Selezione vuota: chaining lungo non lancia eccezioni ---

    @Test
    void emptySel_longChaining_noExceptions() {
        // Arrange
        XML xml = XML.parse("<root/>");

        // Act / Assert
        assertDoesNotThrow(() ->
                xml.sel("//nothing")
                        .attr("a", "1")
                        .text("x")
                        .append("child")
                        .attr("b", "2")
                        .sel("sub")
                        .end()
                        .remove());
    }

    // --- Node vuoto da .first() su Sel vuota: tutte le operazioni sono no-op ---

    @Test
    void emptyNode_fromFirstOnEmptySel_allOperationsNoOp() {
        // Arrange
        XML xml = XML.parse("<root/>");
        Node empty = xml.sel("//nothing").first();

        // Act / Assert
        assertDoesNotThrow(() -> {
            empty.attr("a");
            empty.attr("a", "v");
            empty.text();
            empty.text("v");
            empty.remove();
            empty.append("tag");
            empty.prepend("tag");
            empty.before("tag");
            empty.after("tag");
            empty.sel("x");
            empty.children();
            empty.parent();
            empty.name();
            empty.unwrap();
        });
    }

    // --- Node vuoto: .attr(name) ritorna "" ---

    @Test
    void emptyNode_attr_returnsEmptyString() {
        // Arrange
        Node empty = XML.parse("<root/>").sel("//x").first();

        // Act
        String result = empty.attr("anything");

        // Assert
        assertEquals("", result);
    }

    // --- Node vuoto: .text() ritorna "" ---

    @Test
    void emptyNode_text_returnsEmptyString() {
        // Arrange
        Node empty = XML.parse("<root/>").sel("//x").first();

        // Act
        String result = empty.text();

        // Assert
        assertEquals("", result);
    }

    // --- Node vuoto: .name() ritorna "" ---

    @Test
    void emptyNode_name_returnsEmptyString() {
        // Arrange
        Node empty = XML.parse("<root/>").sel("//x").first();

        // Act
        String result = empty.name();

        // Assert
        assertEquals("", result);
    }

    // --- Node vuoto: .children() ritorna Sel vuoto ---

    @Test
    void emptyNode_children_returnsEmptySel() {
        // Arrange
        Node empty = XML.parse("<root/>").sel("//x").first();

        // Act
        Sel children = empty.children();

        // Assert
        assertNotNull(children);
        assertTrue(children.empty());
        assertEquals(0, children.size());
    }

    // --- Node vuoto: .parent() ritorna Node vuoto ---

    @Test
    void emptyNode_parent_returnsEmptyNode() {
        // Arrange
        Node empty = XML.parse("<root/>").sel("//x").first();

        // Act
        Node parent = empty.parent();

        // Assert
        assertNotNull(parent);
        assertTrue(parent.empty());
        assertEquals("", parent.name());
    }

    // --- Node vuoto: .sel(xpath) ritorna Sel vuoto ---

    @Test
    void emptyNode_sel_returnsEmptySel() {
        // Arrange
        Node empty = XML.parse("<root/>").sel("//x").first();

        // Act
        Sel result = empty.sel("child");

        // Assert
        assertNotNull(result);
        assertTrue(result.empty());
    }

    // --- Node vuoto: .append(tag) ritorna Node vuoto ---

    @Test
    void emptyNode_append_returnsEmptyNode() {
        // Arrange
        Node empty = XML.parse("<root/>").sel("//x").first();

        // Act
        Node result = empty.append("tag");

        // Assert
        assertNotNull(result);
        assertTrue(result.empty());
    }

    // --- Node vuoto: .prepend(tag) ritorna Node vuoto ---

    @Test
    void emptyNode_prepend_returnsEmptyNode() {
        // Arrange
        Node empty = XML.parse("<root/>").sel("//x").first();

        // Act
        Node result = empty.prepend("tag");

        // Assert
        assertNotNull(result);
        assertTrue(result.empty());
    }

    // --- Node vuoto: .before(tag) ritorna Node vuoto ---

    @Test
    void emptyNode_before_returnsEmptyNode() {
        // Arrange
        Node empty = XML.parse("<root/>").sel("//x").first();

        // Act
        Node result = empty.before("tag");

        // Assert
        assertNotNull(result);
        assertTrue(result.empty());
    }

    // --- Node vuoto: .after(tag) ritorna Node vuoto ---

    @Test
    void emptyNode_after_returnsEmptyNode() {
        // Arrange
        Node empty = XML.parse("<root/>").sel("//x").first();

        // Act
        Node result = empty.after("tag");

        // Assert
        assertNotNull(result);
        assertTrue(result.empty());
    }

    // --- Node vuoto: .replace(xml) ritorna Node vuoto ---

    @Test
    void emptyNode_replace_returnsEmptyNode() {
        // Arrange
        Node empty = XML.parse("<root/>").sel("//x").first();
        XML fragment = XML.parse("<replacement/>");

        // Act
        Node result = empty.replace(fragment);

        // Assert
        assertNotNull(result);
        assertTrue(result.empty());
    }

    // --- Node vuoto: .remove() e' no-op ---

    @Test
    void emptyNode_remove_isNoOp() {
        // Arrange
        XML xml = XML.parse("<root><keep/></root>");
        Node empty = xml.sel("//nonexistent").first();

        // Act
        assertDoesNotThrow(() -> empty.remove());

        // Assert — original document unchanged
        assertEquals(1, xml.sel("//keep").size());
    }

    // --- Node vuoto: .unwrap() ritorna null ---

    @Test
    void emptyNode_unwrap_returnsNull() {
        // Arrange
        Node empty = XML.parse("<root/>").sel("//x").first();

        // Act
        org.w3c.dom.Node result = empty.unwrap();

        // Assert
        assertNull(result);
    }

    // --- Chaining misto con selezioni e nodi vuoti ---

    @Test
    void mixedChaining_emptySelectionsAndNodes_noExceptions() {
        // Arrange
        XML xml = XML.parse("<root><real/></root>");

        // Act / Assert
        assertDoesNotThrow(() ->
                xml.sel("//nothing").first().children().sel("//x").attr("a", "b"));
    }

    // --- .end() su selezione radice ritorna se stessa ---

    @Test
    void end_onRootSelection_returnsSelf() {
        // Arrange
        XML xml = XML.parse("<root><item/></root>");
        Sel root = xml.sel("//item");

        // Act
        Sel result = root.end();

        // Assert
        assertNotNull(result);
        assertDoesNotThrow(() -> result.size());
    }
}
