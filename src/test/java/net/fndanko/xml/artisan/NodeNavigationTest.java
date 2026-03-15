package net.fndanko.xml.artisan;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class NodeNavigationTest {

    // --- children() ---

    @Test
    void children_returnsSelWithChildNodes() {
        XML xml = XML.parse("<root><parent><a/><b/><c/></parent></root>");

        Sel children = xml.sel("//parent").first().children();

        assertEquals(3, children.size());
    }

    @Test
    void children_onLeafNode_returnsEmptySel() {
        XML xml = XML.parse("<root><leaf/></root>");

        Sel children = xml.sel("//leaf").first().children();

        assertTrue(children.empty());
        assertEquals(0, children.size());
    }

    @Test
    void children_chainableWithAttr() {
        XML xml = XML.parse("<root><parent><child name=\"x\"/><child name=\"y\"/></parent></root>");

        xml.sel("//parent").first().children().attr("name", "z");

        assertEquals("z", xml.get("//child[1]/@name"));
        assertEquals("z", xml.get("//child[2]/@name"));
    }

    @Test
    void children_list_convertsToList() {
        XML xml = XML.parse("<root><parent><a/><b/></parent></root>");

        List<Node> list = xml.sel("//parent").first().children().list();

        assertEquals(2, list.size());
        assertEquals("a", list.get(0).name());
        assertEquals("b", list.get(1).name());
    }

    // --- parent() ---

    @Test
    void parent_returnsParentNode() {
        XML xml = XML.parse("<root><parent><child/></parent></root>");

        Node parent = xml.sel("//child").first().parent();

        assertEquals("parent", parent.name());
    }

    @Test
    void parent_onRootNode_returnsEmptyNode() {
        XML xml = XML.parse("<root/>");

        Node parent = xml.sel("/root").first().parent();

        assertTrue(parent.empty());
        assertEquals("", parent.name());
    }

    // --- name() ---

    @Test
    void name_returnsTagName() {
        XML xml = XML.parse("<root><myElement/></root>");

        String name = xml.sel("//myElement").first().name();

        assertEquals("myElement", name);
    }

    @Test
    void name_onEmptyNode_returnsEmptyString() {
        String name = Node.EMPTY.name();

        assertEquals("", name);
    }
}
