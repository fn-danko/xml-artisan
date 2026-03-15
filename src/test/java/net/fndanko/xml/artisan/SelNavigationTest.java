package net.fndanko.xml.artisan;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SelNavigationTest {

    // --- sel sub-selection ---

    @Test
    void sel_contextualSubSelection_findsOnlyDescendants() {
        // Arrange
        XML xml = XML.parse("<root><a><b>inside</b></a><b>outside</b></root>");

        // Act
        Sel result = xml.sel("//a").sel("//b");

        // Assert
        assertEquals(1, result.size());
        assertEquals("inside", result.text());
    }

    @Test
    void sel_emptySelection_returnsEmptySel() {
        // Arrange
        XML xml = XML.parse("<root><item/></root>");
        Sel empty = xml.sel("//missing");

        // Act
        Sel result = empty.sel("//item");

        // Assert
        assertTrue(result.empty());
    }

    @Test
    void sel_nestedMultipleLevels_works() {
        // Arrange
        XML xml = XML.parse("<root><a><b><c id=\"deep\"/></b></a></root>");

        // Act
        Sel result = xml.sel("//a").sel("//b").sel("//c");

        // Assert
        assertEquals(1, result.size());
        assertEquals("deep", result.attr("id"));
    }

    // --- end ---

    @Test
    void end_afterSubSelection_returnsParentSelection() {
        // Arrange
        XML xml = XML.parse("<root><a><b/></a></root>");
        Sel parent = xml.sel("//a");

        // Act
        Sel result = parent.sel("//b").end();

        // Assert
        assertEquals(1, result.size());
        assertEquals("a", result.first().name());
    }

    @Test
    void end_onRootSelection_returnsSelf() {
        // Arrange
        XML xml = XML.parse("<root><item/></root>");
        Sel rootSel = xml.sel("//item");

        // Act
        Sel result = rootSel.end();

        // Assert
        assertEquals(rootSel.size(), result.size());
        assertEquals("item", result.first().name());
    }

    @Test
    void end_multipleEndCalls_navigatesBackMultipleLevels() {
        // Arrange
        XML xml = XML.parse("<root><a><b><c/></b></a></root>");

        // Act
        Sel result = xml.sel("//a").sel("//b").sel("//c").end().end();

        // Assert
        assertEquals("a", result.first().name());
    }

    // --- contextual search ---

    @Test
    void sel_contextual_searchesOnlyWithinParentNodes() {
        // Arrange
        XML xml = XML.parse(
                "<root>"
                + "<group id=\"g1\"><item type=\"x\"/></group>"
                + "<group id=\"g2\"><item type=\"y\"/></group>"
                + "<item type=\"z\"/>"
                + "</root>");

        // Act
        Sel items = xml.sel("//group[@id='g1']").sel("//item");

        // Assert
        assertEquals(1, items.size());
        assertEquals("x", items.attr("type"));
    }

    // --- full chaining ---

    @Test
    void chaining_selAttrEndAttr_worksCorrectly() {
        // Arrange
        XML xml = XML.parse("<root><a><b/></a></root>");

        // Act
        xml.sel("//a")
                .sel("//b").attr("inner", "yes").end()
                .attr("outer", "yes");

        // Assert
        assertEquals("yes", xml.get("//b/@inner"));
        assertEquals("yes", xml.get("//a/@outer"));
    }
}
