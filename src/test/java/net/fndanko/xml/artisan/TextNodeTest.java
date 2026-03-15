package net.fndanko.xml.artisan;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TextNodeTest {

    // --- normalizeText ---

    @Test
    void normalizeText_adjacentTextNodes_mergedIntoOne() {
        // Arrange — manually create adjacent text nodes
        XML xml = XML.parse("<root><p>Hello</p></root>");
        org.w3c.dom.Node p = xml.sel("//p").first().unwrap();
        p.appendChild(p.getOwnerDocument().createTextNode(" World"));

        // Act
        xml.sel("//p").normalizeText();

        // Assert
        int textCount = countTextNodes(p);
        assertEquals(1, textCount);
        assertEquals("Hello World", xml.sel("//p").text());
    }

    @Test
    void normalizeText_textAroundElements_mergedAsFirstChild() {
        // Arrange — <p>A<b/>B</p>
        XML xml = XML.parse("<root><p>A<b/>B</p></root>");

        // Act
        xml.sel("//p").normalizeText();

        // Assert
        org.w3c.dom.Node p = xml.sel("//p").first().unwrap();
        assertEquals("AB", xml.sel("//p").text());
        // First child should be the merged text node
        short firstType = p.getFirstChild().getNodeType();
        assertTrue(firstType == org.w3c.dom.Node.TEXT_NODE || firstType == org.w3c.dom.Node.CDATA_SECTION_NODE);
        // <b> should still exist
        assertEquals(1, xml.sel("//b").size());
    }

    @Test
    void normalizeText_mixedTextAndCdata_produceCdata() {
        // Arrange
        XML xml = XML.parse("<root><p>Hello</p></root>");
        org.w3c.dom.Node p = xml.sel("//p").first().unwrap();
        p.appendChild(p.getOwnerDocument().createCDATASection(" World"));

        // Act
        xml.sel("//p").normalizeText();

        // Assert
        org.w3c.dom.Node first = p.getFirstChild();
        assertEquals(org.w3c.dom.Node.CDATA_SECTION_NODE, first.getNodeType());
        assertEquals("Hello World", first.getNodeValue());
    }

    @Test
    void normalizeText_cdataOnly_staysCdata() {
        // Arrange — two CDATA sections
        XML xml = XML.parse("<root><p/></root>");
        org.w3c.dom.Node p = xml.sel("//p").first().unwrap();
        p.appendChild(p.getOwnerDocument().createCDATASection("A"));
        p.appendChild(p.getOwnerDocument().createCDATASection("B"));

        // Act
        xml.sel("//p").normalizeText();

        // Assert
        org.w3c.dom.Node first = p.getFirstChild();
        assertEquals(org.w3c.dom.Node.CDATA_SECTION_NODE, first.getNodeType());
        assertEquals("AB", first.getNodeValue());
    }

    @Test
    void normalizeText_noTextNodes_noOp() {
        // Arrange
        XML xml = XML.parse("<root><p><b/></p></root>");
        String before = xml.toFragment();

        // Act
        xml.sel("//p").normalizeText();

        // Assert
        assertEquals(before, xml.toFragment());
    }

    @Test
    void normalizeText_emptyElement_noOp() {
        // Arrange
        XML xml = XML.parse("<root><p/></root>");
        String before = xml.toFragment();

        // Act
        xml.sel("//p").normalizeText();

        // Assert
        assertEquals(before, xml.toFragment());
    }

    @Test
    void normalizeText_preservesChildElements() {
        // Arrange — <p>A<b>inner</b>B</p>
        XML xml = XML.parse("<root><p>A<b>inner</b>B</p></root>");

        // Act
        xml.sel("//p").normalizeText();

        // Assert
        assertEquals(1, xml.sel("//b").size());
        assertEquals("inner", xml.sel("//b").text());
    }

    @Test
    void normalizeText_preservesAttributes() {
        // Arrange
        XML xml = XML.parse("<root><p class=\"x\">A<b/>B</p></root>");

        // Act
        xml.sel("//p").normalizeText();

        // Assert
        assertEquals("x", xml.sel("//p").attr("class"));
    }

    @Test
    void normalizeText_appliedToAllNodesInSelection() {
        // Arrange
        XML xml = XML.parse("<root><p>A<b/>B</p><p>C<i/>D</p></root>");

        // Act
        xml.sel("//p").normalizeText();

        // Assert
        var ps = xml.sel("//p").list();
        assertEquals("AB", ps.get(0).text());
        assertEquals("CD", ps.get(1).text());
    }

    @Test
    void normalizeText_chainable_returnsSelf() {
        // Arrange
        XML xml = XML.parse("<root><p>text</p></root>");
        Sel sel = xml.sel("//p");

        // Act
        Sel result = sel.normalizeText();

        // Assert
        assertSame(sel, result);
    }

    // --- text() read (new semantics) ---

    @Test
    void text_simpleElement_returnsDirectText() {
        // Arrange
        XML xml = XML.parse("<root><p>Hello</p></root>");

        // Act
        String result = xml.sel("//p").text();

        // Assert
        assertEquals("Hello", result);
    }

    @Test
    void text_mixedContent_returnsDirectTextOnly() {
        // Arrange
        XML xml = XML.parse("<root><p>Hello <b>world</b> today</p></root>");

        // Act
        String result = xml.sel("//p").text();

        // Assert
        assertEquals("Hello  today", result);
    }

    @Test
    void text_noDirectText_returnsEmpty() {
        // Arrange
        XML xml = XML.parse("<root><p><b>world</b></p></root>");

        // Act
        String result = xml.sel("//p").text();

        // Assert
        assertEquals("", result);
    }

    @Test
    void text_readDoesNotModifyDom() {
        // Arrange
        XML xml = XML.parse("<root><p>Hello <b>world</b> today</p></root>");
        String before = xml.toFragment();

        // Act
        xml.sel("//p").text();

        // Assert
        assertEquals(before, xml.toFragment());
    }

    @Test
    void text_fragmentedTextNodes_concatenatedInRead() {
        // Arrange — manually create fragmented text nodes
        XML xml = XML.parse("<root><p>A</p></root>");
        org.w3c.dom.Node p = xml.sel("//p").first().unwrap();
        p.appendChild(p.getOwnerDocument().createTextNode("B"));
        p.appendChild(p.getOwnerDocument().createTextNode("C"));

        // Act
        String result = xml.sel("//p").text();

        // Assert
        assertEquals("ABC", result);
    }

    @Test
    void text_emptySelection_returnsEmpty() {
        // Arrange
        XML xml = XML.parse("<root/>");

        // Act
        String result = xml.sel("//nonexistent").text();

        // Assert
        assertEquals("", result);
    }

    // --- text() write (new semantics) ---

    @Test
    void text_setValue_replacesDirectTextPreservesChildren() {
        // Arrange
        XML xml = XML.parse("<root><p>Hello <b>world</b> today</p></root>");

        // Act
        xml.sel("//p").text("New text");

        // Assert
        assertEquals("New text", xml.sel("//p").text());
        assertEquals(1, xml.sel("//b").size());
    }

    @Test
    void text_setEmpty_removesDirectTextOnly() {
        // Arrange
        XML xml = XML.parse("<root><p>Hello <b>world</b></p></root>");

        // Act
        xml.sel("//p").text("");

        // Assert
        assertEquals("", xml.sel("//p").text());
        assertEquals(1, xml.sel("//b").size());
    }

    @Test
    void text_onNodeWithoutText_addsTextNode() {
        // Arrange
        XML xml = XML.parse("<root><p><b>child</b></p></root>");

        // Act
        xml.sel("//p").text("added");

        // Assert
        assertEquals("added", xml.sel("//p").text());
        assertEquals(1, xml.sel("//b").size());
    }

    @Test
    void text_appliedToAllNodes() {
        // Arrange
        XML xml = XML.parse("<root><p>a</p><p>b</p></root>");

        // Act
        xml.sel("//p").text("same");

        // Assert
        xml.sel("//p").stream().forEach(node ->
                assertEquals("same", node.text()));
    }

    @Test
    void text_chainable() {
        // Arrange
        XML xml = XML.parse("<root><p>text</p></root>");
        Sel sel = xml.sel("//p");

        // Act
        Sel result = sel.text("new");

        // Assert
        assertSame(sel, result);
    }

    // --- text() transform ---

    @Test
    void text_transform_appliedToEachNode() {
        // Arrange
        XML xml = XML.parse("<root><p>hello</p><p>world</p></root>");

        // Act
        xml.sel("//p").text(t -> t + "!");

        // Assert
        assertEquals("hello!", xml.sel("//p[1]").text());
        assertEquals("world!", xml.sel("//p[2]").text());
    }

    @Test
    void text_transformOnMixedContent_preservesChildren() {
        // Arrange
        XML xml = XML.parse("<root><p>Hello <b>world</b> today</p></root>");

        // Act
        xml.sel("//p").text(t -> t.toUpperCase());

        // Assert
        assertEquals("HELLO  TODAY", xml.sel("//p").text());
        assertEquals(1, xml.sel("//b").size());
    }

    @Test
    void text_transformEmptyText_receivesEmptyString() {
        // Arrange
        XML xml = XML.parse("<root><empty/></root>");

        // Act
        xml.sel("//empty").text(old -> {
            assertEquals("", old);
            return "filled";
        });

        // Assert
        assertEquals("filled", xml.sel("//empty").text());
    }

    // --- deepText ---

    @Test
    void deepText_simpleElement_returnsText() {
        // Arrange
        XML xml = XML.parse("<root><p>Hello</p></root>");

        // Act
        String result = xml.sel("//p").deepText();

        // Assert
        assertEquals("Hello", result);
    }

    @Test
    void deepText_mixedContent_returnsAllDescendantText() {
        // Arrange
        XML xml = XML.parse("<root><p>Hello <b>world</b> today</p></root>");

        // Act
        String result = xml.sel("//p").deepText();

        // Assert
        assertEquals("Hello world today", result);
    }

    @Test
    void deepText_nested_concatenatesAll() {
        // Arrange
        XML xml = XML.parse("<root><p>A<b>B<i>C</i></b>D</p></root>");

        // Act
        String result = xml.sel("//p").deepText();

        // Assert
        assertEquals("ABCD", result);
    }

    @Test
    void deepText_emptySelection_returnsEmpty() {
        // Arrange
        XML xml = XML.parse("<root/>");

        // Act
        String result = xml.sel("//nonexistent").deepText();

        // Assert
        assertEquals("", result);
    }

    @Test
    void deepText_noSideEffects() {
        // Arrange
        XML xml = XML.parse("<root><p>Hello <b>world</b> today</p></root>");
        String before = xml.toFragment();

        // Act
        xml.sel("//p").deepText();

        // Assert
        assertEquals(before, xml.toFragment());
    }

    // --- coalesceText ---

    @Test
    void coalesceText_mixedContent_mergesIntoSingleText() {
        // Arrange
        XML xml = XML.parse("<root><p>Hello <b>world</b> today</p></root>");

        // Act
        xml.sel("//p").coalesceText();

        // Assert
        assertEquals("Hello world today", xml.sel("//p").text());
        assertEquals(0, xml.sel("//b").size());
    }

    @Test
    void coalesceText_nestedElements_flattensAll() {
        // Arrange
        XML xml = XML.parse("<root><p>A<b>B<i>C</i></b>D</p></root>");

        // Act
        xml.sel("//p").coalesceText();

        // Assert
        assertEquals("ABCD", xml.sel("//p").text());
        assertEquals(0, xml.sel("//b").size());
        assertEquals(0, xml.sel("//i").size());
    }

    @Test
    void coalesceText_noMixedContent_unchanged() {
        // Arrange
        XML xml = XML.parse("<root><p>simple text</p></root>");

        // Act
        xml.sel("//p").coalesceText();

        // Assert
        assertEquals("simple text", xml.sel("//p").text());
    }

    @Test
    void coalesceText_preservesAttributes() {
        // Arrange
        XML xml = XML.parse("<root><p class=\"x\">Hello <b>world</b></p></root>");

        // Act
        xml.sel("//p").coalesceText();

        // Assert
        assertEquals("x", xml.sel("//p").attr("class"));
    }

    @Test
    void coalesceText_appliedToAllNodes() {
        // Arrange
        XML xml = XML.parse("<root><p>A<b>B</b></p><p>C<i>D</i></p></root>");

        // Act
        xml.sel("//p").coalesceText();

        // Assert
        var ps = xml.sel("//p").list();
        assertEquals("AB", ps.get(0).text());
        assertEquals("CD", ps.get(1).text());
    }

    @Test
    void coalesceText_chainable() {
        // Arrange
        XML xml = XML.parse("<root><p>text</p></root>");
        Sel sel = xml.sel("//p");

        // Act
        Sel result = sel.coalesceText();

        // Assert
        assertSame(sel, result);
    }

    @Test
    void coalesceText_thenText_readsSingleString() {
        // Arrange
        XML xml = XML.parse("<root><p>Hello <b>world</b> today</p></root>");

        // Act
        xml.sel("//p").coalesceText();

        // Assert
        assertEquals("Hello world today", xml.sel("//p").text());
        assertEquals("Hello world today", xml.sel("//p").deepText());
    }

    // --- XML.normalizeText() ---

    @Test
    void xmlNormalizeText_recursiveOnAllLevels() {
        // Arrange — create fragmented text at multiple levels
        XML xml = XML.parse("<root><p>A<b>X</b>B</p></root>");
        // Add extra text nodes to <b> to fragment it
        org.w3c.dom.Node b = xml.sel("//b").first().unwrap();
        b.appendChild(b.getOwnerDocument().createTextNode("Y"));

        // Act
        xml.normalizeText();

        // Assert
        assertEquals("XY", xml.sel("//b").text());
        assertEquals("AB", xml.sel("//p").text());
    }

    @Test
    void xmlNormalizeText_preservesStructure() {
        // Arrange
        XML xml = XML.parse("<root><a>1<b>2<c>3</c>4</b>5</a></root>");

        // Act
        xml.normalizeText();

        // Assert
        assertEquals(1, xml.sel("//a").size());
        assertEquals(1, xml.sel("//b").size());
        assertEquals(1, xml.sel("//c").size());
    }

    @Test
    void xmlNormalizeText_chainable_returnsXml() {
        // Arrange
        XML xml = XML.parse("<root><p>text</p></root>");

        // Act
        XML result = xml.normalizeText();

        // Assert
        assertSame(xml, result);
    }

    // --- Utility ---

    private int countTextNodes(org.w3c.dom.Node parent) {
        int count = 0;
        org.w3c.dom.NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            short type = children.item(i).getNodeType();
            if (type == org.w3c.dom.Node.TEXT_NODE || type == org.w3c.dom.Node.CDATA_SECTION_NODE) {
                count++;
            }
        }
        return count;
    }
}
