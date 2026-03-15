package net.fndanko.xml.artisan;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class NodeStructuralTest {

    // --- append(tag) ---

    @Test
    void append_tag_returnsNewNodeNotOriginal() {
        XML xml = XML.parse("<root><parent/></root>");
        Node parent = xml.sel("//parent").first();

        Node child = parent.append("child");

        assertEquals("child", child.name());
        assertNotSame(parent, child);
    }

    @Test
    void append_tag_allowsDeepConstruction() {
        XML xml = XML.parse("<root/>");
        Node root = xml.sel("/root").first();

        root.append("a").append("b");

        assertEquals("b", xml.sel("//a/b").first().name());
    }

    // --- append(XML) ---

    @Test
    void append_xml_returnsRootOfInsertedFragment() {
        XML xml = XML.parse("<root><parent/></root>");
        XML fragment = XML.parse("<frag><nested/></frag>");
        Node parent = xml.sel("//parent").first();

        Node inserted = parent.append(fragment);

        assertEquals("frag", inserted.name());
    }

    // --- prepend(tag) ---

    @Test
    void prepend_tag_returnsNewNode() {
        XML xml = XML.parse("<root><parent><existing/></parent></root>");
        Node parent = xml.sel("//parent").first();

        Node first = parent.prepend("first");

        assertEquals("first", first.name());
        assertEquals("first", parent.children().first().name());
    }

    // --- prepend(XML) ---

    @Test
    void prepend_xml_returnsNewNodeAtBeginning() {
        XML xml = XML.parse("<root><parent><existing/></parent></root>");
        XML fragment = XML.parse("<first/>");
        Node parent = xml.sel("//parent").first();

        Node inserted = parent.prepend(fragment);

        assertEquals("first", inserted.name());
        assertEquals("first", parent.children().first().name());
    }

    // --- insert(tag, before) ---

    @Test
    void insert_tag_insertsAtCorrectPosition() {
        XML xml = XML.parse("<root><a/><c/></root>");
        Node root = xml.sel("/root").first();
        Node c = xml.sel("//c").first();

        Node b = root.insert("b", c);

        assertEquals("b", b.name());
        // order should be a, b, c
        java.util.List<String> names = root.children().stream()
                .map(Node::name)
                .collect(java.util.stream.Collectors.toList());
        assertEquals(java.util.List.of("a", "b", "c"), names);
    }

    // --- before(tag) ---

    @Test
    void before_tag_returnsNewSibling() {
        XML xml = XML.parse("<root><target/></root>");
        Node target = xml.sel("//target").first();

        Node prev = target.before("prev");

        assertEquals("prev", prev.name());
        assertEquals("prev", xml.sel("/root").first().children().first().name());
    }

    // --- before(XML) ---

    @Test
    void before_xml_returnsNewNode() {
        XML xml = XML.parse("<root><target/></root>");
        XML fragment = XML.parse("<prev/>");
        Node target = xml.sel("//target").first();

        Node prev = target.before(fragment);

        assertEquals("prev", prev.name());
    }

    // --- after(tag) ---

    @Test
    void after_tag_returnsNewSibling() {
        XML xml = XML.parse("<root><target/></root>");
        Node target = xml.sel("//target").first();

        Node next = target.after("next");

        assertEquals("next", next.name());
        assertEquals("next", xml.sel("/root").first().children().last().name());
    }

    // --- after(XML) ---

    @Test
    void after_xml_returnsNewNode() {
        XML xml = XML.parse("<root><target/></root>");
        XML fragment = XML.parse("<next/>");
        Node target = xml.sel("//target").first();

        Node next = target.after(fragment);

        assertEquals("next", next.name());
    }

    // --- replace(XML) ---

    @Test
    void replace_xml_returnsNewSubstituteNode() {
        XML xml = XML.parse("<root><old/></root>");
        XML fragment = XML.parse("<new/>");
        Node old = xml.sel("//old").first();

        Node replacement = old.replace(fragment);

        assertEquals("new", replacement.name());
    }

    @Test
    void replace_xml_originalNodeNoLongerInDom() {
        XML xml = XML.parse("<root><old/></root>");
        XML fragment = XML.parse("<new/>");

        xml.sel("//old").first().replace(fragment);

        assertEquals(0, xml.sel("//old").size());
        assertEquals(1, xml.sel("//new").size());
    }
}
