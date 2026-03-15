package net.fndanko.xml.artisan;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SelStructuralTest {

    // --- append(tag) ---

    @Test
    void append_tag_addsChildToEveryNodeInSelection() {
        XML xml = XML.parse("<root><item/><item/></root>");

        xml.sel("//item").append("child");

        assertEquals(2, xml.sel("//child").size());
        // each <item> should have one <child>
        for (Node item : xml.sel("//item")) {
            assertEquals(1, item.sel("child").size());
        }
    }

    @Test
    void append_tag_returnsOriginalSelectionNotNewNodes() {
        XML xml = XML.parse("<root><item/><item/></root>");
        Sel items = xml.sel("//item");

        Sel result = items.append("child");

        assertSame(items, result);
    }

    // --- append(XML) ---

    @Test
    void append_xml_addsFragmentToEveryNode() {
        XML xml = XML.parse("<root><item/><item/></root>");
        XML fragment = XML.parse("<frag><nested/></frag>");

        xml.sel("//item").append(fragment);

        assertEquals(2, xml.sel("//frag").size());
        assertEquals(2, xml.sel("//nested").size());
    }

    // --- prepend(tag) ---

    @Test
    void prepend_tag_addsChildAtBeginningOfEveryNode() {
        XML xml = XML.parse("<root><item><existing/></item><item><existing/></item></root>");

        xml.sel("//item").prepend("first");

        for (Node item : xml.sel("//item")) {
            assertEquals("first", item.children().first().name());
        }
    }

    // --- prepend(XML) ---

    @Test
    void prepend_xml_addsFragmentAtBeginningOfEveryNode() {
        XML xml = XML.parse("<root><item><existing/></item></root>");
        XML fragment = XML.parse("<first/>");

        xml.sel("//item").prepend(fragment);

        assertEquals("first", xml.sel("//item").first().children().first().name());
    }

    // --- before(tag) ---

    @Test
    void before_tag_insertsSiblingBeforeEveryNode() {
        XML xml = XML.parse("<root><item/><item/></root>");

        xml.sel("//item").before("prev");

        assertEquals(2, xml.sel("//prev").size());
        // root children should be: prev, item, prev, item
        Sel children = xml.sel("/root").first().children();
        assertEquals(4, children.size());
        assertEquals("prev", children.first().name());
    }

    @Test
    void before_tag_returnsOriginalSelection() {
        XML xml = XML.parse("<root><item/></root>");
        Sel items = xml.sel("//item");

        Sel result = items.before("prev");

        assertSame(items, result);
    }

    // --- before(XML) ---

    @Test
    void before_xml_insertsFragmentAsSiblingBefore() {
        XML xml = XML.parse("<root><item/></root>");
        XML fragment = XML.parse("<prev/>");

        xml.sel("//item").before(fragment);

        assertEquals("prev", xml.sel("/root").first().children().first().name());
    }

    // --- after(tag) ---

    @Test
    void after_tag_insertsSiblingAfterEveryNode() {
        XML xml = XML.parse("<root><item/><item/></root>");

        xml.sel("//item").after("next");

        assertEquals(2, xml.sel("//next").size());
    }

    @Test
    void after_tag_onMultipleSiblings_interleaves() {
        XML xml = XML.parse("<root><a/><b/><c/></root>");

        xml.sel("/root/*").after("x");

        // expect: a, x, b, x, c, x
        java.util.List<String> names = xml.sel("/root/*").stream()
                .map(Node::name)
                .collect(java.util.stream.Collectors.toList());
        assertEquals(java.util.List.of("a", "x", "b", "x", "c", "x"), names);
    }

    // --- after(XML) ---

    @Test
    void after_xml_insertsFragmentAsSiblingAfter() {
        XML xml = XML.parse("<root><item/></root>");
        XML fragment = XML.parse("<next/>");

        xml.sel("//item").after(fragment);

        assertEquals("next", xml.sel("/root").first().children().last().name());
    }

    // --- replace(XML) ---

    @Test
    void replace_xml_replacesEveryNode() {
        XML xml = XML.parse("<root><old/><old/></root>");
        XML fragment = XML.parse("<new/>");

        xml.sel("//old").replace(fragment);

        assertEquals(0, xml.sel("//old").size());
        assertEquals(2, xml.sel("//new").size());
    }

    @Test
    void replace_xml_returnsSelWithNewNodesNotOriginals() {
        XML xml = XML.parse("<root><old/><old/></root>");
        XML fragment = XML.parse("<new/>");

        Sel result = xml.sel("//old").replace(fragment);

        assertEquals(2, result.size());
        assertEquals("new", result.first().name());
        assertEquals("new", result.last().name());
    }

    // --- empty selection: all structural methods are no-op ---

    @Test
    void allStructuralMethods_onEmptySel_noOpAndChainingContinues() {
        XML xml = XML.parse("<root/>");
        Sel empty = xml.sel("//nonexistent");

        // each call should not throw and should return a Sel for chaining
        Sel result = empty
                .append("x")
                .append(XML.parse("<x/>"))
                .prepend("x")
                .prepend(XML.parse("<x/>"))
                .before("x")
                .before(XML.parse("<x/>"))
                .after("x")
                .after(XML.parse("<x/>"))
                .replace(XML.parse("<x/>"));

        assertNotNull(result);
        // the document should be unchanged
        assertEquals(0, xml.sel("/root").first().children().size());
    }
}
