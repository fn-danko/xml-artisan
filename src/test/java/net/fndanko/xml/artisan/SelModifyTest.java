package net.fndanko.xml.artisan;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SelModifyTest {

    // --- attr set ---

    @Test
    void attr_setValue_appliedToAllNodes() {
        // Arrange
        XML xml = XML.parse("<root><item/><item/><item/></root>");
        Sel sel = xml.sel("//item");

        // Act
        sel.attr("color", "red");

        // Assert
        Sel items = xml.sel("//item[@color='red']");
        assertEquals(3, items.size());
    }

    @Test
    void attr_setValueOnEmptySelection_noOpChainingContinues() {
        // Arrange
        XML xml = XML.parse("<root/>");
        Sel sel = xml.sel("//missing");

        // Act
        Sel result = sel.attr("key", "val");

        // Assert
        assertNotNull(result);
        assertTrue(result.empty());
    }

    // --- text set ---

    @Test
    void text_setValue_appliedToAllNodes() {
        // Arrange
        XML xml = XML.parse("<root><p>a</p><p>b</p></root>");
        Sel sel = xml.sel("//p");

        // Act
        sel.text("same");

        // Assert
        xml.sel("//p").stream().forEach(node ->
                assertEquals("same", node.text()));
    }

    @Test
    void text_setValueOnEmptySelection_noOpChainingContinues() {
        // Arrange
        XML xml = XML.parse("<root/>");
        Sel sel = xml.sel("//missing");

        // Act
        Sel result = sel.text("value");

        // Assert
        assertNotNull(result);
        assertTrue(result.empty());
    }

    // --- remove ---

    @Test
    void remove_existingNodes_removedFromDom() {
        // Arrange
        XML xml = XML.parse("<root><a/><b/><a/></root>");

        // Act
        xml.sel("//a").remove();

        // Assert
        assertEquals(0, xml.sel("//a").size());
        assertEquals(1, xml.sel("//b").size());
    }

    @Test
    void remove_emptySelection_noOp() {
        // Arrange
        XML xml = XML.parse("<root><item/></root>");
        String before = xml.toString();

        // Act
        xml.sel("//missing").remove();

        // Assert
        assertEquals(before, xml.toString());
    }

    @Test
    void remove_existingNodes_returnsParentSelection() {
        // Arrange
        XML xml = XML.parse("<root><parent><child/></parent></root>");
        Sel parentSel = xml.sel("//parent");
        Sel children = parentSel.sel("//child");

        // Act
        Sel result = children.remove();

        // Assert
        assertFalse(result.empty());
        assertEquals("parent", result.first().name());
    }

    // --- chaining ---

    @Test
    void chaining_multipleModifications_allApplied() {
        // Arrange
        XML xml = XML.parse("<root><item/></root>");

        // Act
        xml.sel("//item")
                .attr("a", "1")
                .text("hello")
                .attr("b", "2");

        // Assert
        Sel item = xml.sel("//item");
        assertEquals("1", item.attr("a"));
        assertEquals("2", item.attr("b"));
        assertEquals("hello", item.text());
    }
}
