package net.fndanko.xml.artisan;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SelReadTest {

    // --- attr read ---

    @Test
    void attr_multipleNodes_returnsValueFromFirstNode() {
        // Arrange
        XML xml = XML.parse("<root><item id=\"first\"/><item id=\"second\"/></root>");
        Sel sel = xml.sel("//item");

        // Act
        String value = sel.attr("id");

        // Assert
        assertEquals("first", value);
    }

    @Test
    void attr_nonexistentAttribute_returnsEmptyString() {
        // Arrange
        XML xml = XML.parse("<root><item/></root>");
        Sel sel = xml.sel("//item");

        // Act
        String value = sel.attr("missing");

        // Assert
        assertEquals("", value);
    }

    @Test
    void attr_emptySelection_returnsEmptyString() {
        // Arrange
        XML xml = XML.parse("<root/>");
        Sel sel = xml.sel("//nonexistent");

        // Act
        String value = sel.attr("id");

        // Assert
        assertEquals("", value);
    }

    // --- text read ---

    @Test
    void text_multipleNodes_returnsTextFromFirstNode() {
        // Arrange
        XML xml = XML.parse("<root><p>first</p><p>second</p></root>");
        Sel sel = xml.sel("//p");

        // Act
        String value = sel.text();

        // Assert
        assertEquals("first", value);
    }

    @Test
    void text_nodeWithoutText_returnsEmptyString() {
        // Arrange
        XML xml = XML.parse("<root><empty/></root>");
        Sel sel = xml.sel("//empty");

        // Act
        String value = sel.text();

        // Assert
        assertEquals("", value);
    }

    @Test
    void text_emptySelection_returnsEmptyString() {
        // Arrange
        XML xml = XML.parse("<root/>");
        Sel sel = xml.sel("//nonexistent");

        // Act
        String value = sel.text();

        // Assert
        assertEquals("", value);
    }

    // --- size ---

    @Test
    void size_multipleNodes_returnsCorrectCount() {
        // Arrange
        XML xml = XML.parse("<root><a/><a/><a/></root>");
        Sel sel = xml.sel("//a");

        // Act
        int count = sel.size();

        // Assert
        assertEquals(3, count);
    }

    @Test
    void size_emptySelection_returnsZero() {
        // Arrange
        XML xml = XML.parse("<root/>");
        Sel sel = xml.sel("//missing");

        // Act
        int count = sel.size();

        // Assert
        assertEquals(0, count);
    }

    // --- empty ---

    @Test
    void empty_selectionWithNodes_returnsFalse() {
        // Arrange
        XML xml = XML.parse("<root><item/></root>");
        Sel sel = xml.sel("//item");

        // Act
        boolean result = sel.empty();

        // Assert
        assertFalse(result);
    }

    @Test
    void empty_emptySelection_returnsTrue() {
        // Arrange
        XML xml = XML.parse("<root/>");
        Sel sel = xml.sel("//missing");

        // Act
        boolean result = sel.empty();

        // Assert
        assertTrue(result);
    }
}
