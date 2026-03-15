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

    // --- cdata() write ---

    @Test
    void cdata_simpleValue_createsCdataSection() {
        // Arrange
        XML xml = XML.parse("<root><p/></root>");

        // Act
        xml.sel("//p").cdata("hello");

        // Assert
        org.w3c.dom.Node p = xml.sel("//p").first().unwrap();
        org.w3c.dom.Node first = p.getFirstChild();
        assertEquals(org.w3c.dom.Node.CDATA_SECTION_NODE, first.getNodeType());
        assertEquals("hello", first.getNodeValue());
    }

    @Test
    void cdata_preservesChildElements() {
        // Arrange
        XML xml = XML.parse("<root><p><b>child</b></p></root>");

        // Act
        xml.sel("//p").cdata("new");

        // Assert
        assertEquals(1, xml.sel("//b").size());
        assertEquals("child", xml.sel("//b").text());
    }

    @Test
    void cdata_replaceExistingText() {
        // Arrange
        XML xml = XML.parse("<root><p>old text</p></root>");

        // Act
        xml.sel("//p").cdata("new cdata");

        // Assert
        org.w3c.dom.Node p = xml.sel("//p").first().unwrap();
        org.w3c.dom.Node first = p.getFirstChild();
        assertEquals(org.w3c.dom.Node.CDATA_SECTION_NODE, first.getNodeType());
        assertEquals("new cdata", first.getNodeValue());
    }

    @Test
    void cdata_replaceExistingCdata() {
        // Arrange
        XML xml = XML.parse("<root><p/></root>");
        xml.sel("//p").cdata("old");

        // Act
        xml.sel("//p").cdata("new");

        // Assert
        org.w3c.dom.Node p = xml.sel("//p").first().unwrap();
        org.w3c.dom.Node first = p.getFirstChild();
        assertEquals(org.w3c.dom.Node.CDATA_SECTION_NODE, first.getNodeType());
        assertEquals("new", first.getNodeValue());
    }

    @Test
    void cdata_appliedToAllNodes() {
        // Arrange
        XML xml = XML.parse("<root><p>a</p><p>b</p></root>");

        // Act
        xml.sel("//p").cdata("same");

        // Assert
        for (Node node : xml.sel("//p")) {
            org.w3c.dom.Node first = node.unwrap().getFirstChild();
            assertEquals(org.w3c.dom.Node.CDATA_SECTION_NODE, first.getNodeType());
            assertEquals("same", first.getNodeValue());
        }
    }

    @Test
    void cdata_chainable() {
        // Arrange
        XML xml = XML.parse("<root><p/></root>");
        Sel sel = xml.sel("//p");

        // Act
        Sel result = sel.cdata("x");

        // Assert
        assertSame(sel, result);
    }

    @Test
    void cdata_emptySelection_noOp() {
        // Arrange
        XML xml = XML.parse("<root/>");

        // Act / Assert
        assertDoesNotThrow(() -> xml.sel("//nonexistent").cdata("value"));
    }

    @Test
    void cdata_thenText_readsCdataContent() {
        // Arrange
        XML xml = XML.parse("<root><p/></root>");

        // Act
        xml.sel("//p").cdata("cdata content");

        // Assert
        assertEquals("cdata content", xml.sel("//p").text());
    }

    @Test
    void cdata_withSpecialChars_preserved() {
        // Arrange
        XML xml = XML.parse("<root><p/></root>");
        String special = "<script>alert('xss')</script> & ]]";

        // Act
        xml.sel("//p").cdata(special);

        // Assert
        assertEquals(special, xml.sel("//p").text());
    }

    // --- content(XML) ---

    @Test
    void content_xml_replacesAllChildren() {
        // Arrange
        XML xml = XML.parse("<root><p>old text<b>bold</b></p></root>");
        XML fragment = XML.parse("<div>new content</div>");

        // Act
        xml.sel("//p").content(fragment);

        // Assert
        assertEquals(0, xml.sel("//b").size());
        assertEquals(1, xml.sel("//p/div").size());
        assertEquals("new content", xml.sel("//p/div").text());
    }

    @Test
    void content_xml_preservesNodeAttributes() {
        // Arrange
        XML xml = XML.parse("<root><p class=\"x\" id=\"1\">old</p></root>");
        XML fragment = XML.parse("<span>new</span>");

        // Act
        xml.sel("//p").content(fragment);

        // Assert
        assertEquals("x", xml.sel("//p").attr("class"));
        assertEquals("1", xml.sel("//p").attr("id"));
    }

    @Test
    void content_xml_preservesNodePosition() {
        // Arrange
        XML xml = XML.parse("<root><a/><p>old</p><z/></root>");
        XML fragment = XML.parse("<span>new</span>");

        // Act
        xml.sel("//p").content(fragment);

        // Assert
        var children = xml.sel("/root/*").list();
        assertEquals(3, children.size());
        assertEquals("a", children.get(0).name());
        assertEquals("p", children.get(1).name());
        assertEquals("z", children.get(2).name());
    }

    @Test
    void content_xml_importsRootElement() {
        // Arrange
        XML xml = XML.parse("<root><p>old</p></root>");
        XML fragment = XML.parse("<div><b>nested</b></div>");

        // Act
        xml.sel("//p").content(fragment);

        // Assert
        assertEquals(1, xml.sel("//p/div").size());
        assertEquals(1, xml.sel("//p/div/b").size());
        assertEquals("nested", xml.sel("//p/div/b").text());
    }

    @Test
    void content_xml_appliedToAllNodes() {
        // Arrange
        XML xml = XML.parse("<root><p>a</p><p>b</p></root>");
        XML fragment = XML.parse("<span>replaced</span>");

        // Act
        xml.sel("//p").content(fragment);

        // Assert
        assertEquals(2, xml.sel("//span").size());
        for (Node p : xml.sel("//p")) {
            assertEquals("replaced", p.sel("span").text());
        }
    }

    @Test
    void content_xml_chainable() {
        // Arrange
        XML xml = XML.parse("<root><p>old</p></root>");
        Sel sel = xml.sel("//p");
        XML fragment = XML.parse("<span/>");

        // Act
        Sel result = sel.content(fragment);

        // Assert
        assertSame(sel, result);
    }

    @Test
    void content_xml_emptySelection_noOp() {
        // Arrange
        XML xml = XML.parse("<root/>");
        XML fragment = XML.parse("<span/>");

        // Act / Assert
        assertDoesNotThrow(() -> xml.sel("//nonexistent").content(fragment));
    }

    // --- content(String) ---

    @Test
    void content_string_parsesAndReplaces() {
        // Arrange
        XML xml = XML.parse("<root><p>old text</p></root>");

        // Act
        xml.sel("//p").content("<span>new</span>");

        // Assert
        assertEquals(1, xml.sel("//p/span").size());
        assertEquals("new", xml.sel("//p/span").text());
    }

    @Test
    void content_string_withMixedContent() {
        // Arrange
        XML xml = XML.parse("<root><p>old</p></root>");

        // Act
        xml.sel("//p").content("<b>bold</b> text");

        // Assert
        assertEquals(1, xml.sel("//p/b").size());
        assertEquals("bold", xml.sel("//b").text());
        assertEquals(" text", xml.sel("//p").text());
    }

    @Test
    void content_string_singleElement() {
        // Arrange
        XML xml = XML.parse("<root><p>old</p></root>");

        // Act
        xml.sel("//p").content("<div>x</div>");

        // Assert
        assertEquals(1, xml.sel("//p/div").size());
        assertEquals("x", xml.sel("//p/div").text());
    }

    @Test
    void content_string_emptyString_removesAllChildren() {
        // Arrange
        XML xml = XML.parse("<root><p>text<b>bold</b></p></root>");

        // Act
        xml.sel("//p").content("");

        // Assert
        assertEquals("", xml.sel("//p").text());
        assertEquals("", xml.sel("//p").deepText());
        assertEquals(0, xml.sel("//b").size());
    }

    @Test
    void content_string_chainable() {
        // Arrange
        XML xml = XML.parse("<root><p>old</p></root>");
        Sel sel = xml.sel("//p");

        // Act
        Sel result = sel.content("<span/>");

        // Assert
        assertSame(sel, result);
    }

    @Test
    void content_xml_replacesTextOnlyNode() {
        // Arrange
        XML xml = XML.parse("<root><p>only text here</p></root>");
        XML fragment = XML.parse("<span>replaced</span>");

        // Act
        xml.sel("//p").content(fragment);

        // Assert
        assertEquals("", xml.sel("//p").text());
        assertEquals(1, xml.sel("//p/span").size());
        assertEquals("replaced", xml.sel("//p/span").text());
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
