package net.fndanko.xml.artisan;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JoinShorthandTest {

    @Test
    void joinShorthand_createsEnterNodesWithTag() {
        XML xml = XML.parse("<root></root>");

        xml.sel("//item").data(List.of("A", "B")).join("item");

        assertEquals(2, xml.sel("//item").size());
        // Created nodes are elements with the specified tag
        assertEquals("item", xml.sel("//item").first().name());
    }

    @Test
    void joinShorthand_updateNodesUnchanged() {
        XML xml = XML.parse("<root><item>A</item><item>B</item></root>");

        xml.sel("//item").data(List.of("X", "Y")).join("item");

        assertEquals(2, xml.sel("//item").size());
        // Default update is identity — original text preserved on both nodes
        assertEquals("A", xml.sel("//item").first().text());
        assertEquals("B", xml.sel("//item").last().text());
    }

    @Test
    void joinShorthand_removesExitNodes() {
        XML xml = XML.parse("<root><item>A</item><item>B</item><item>C</item></root>");

        xml.sel("//item").data(List.of("X")).join("item");

        assertEquals(1, xml.sel("//item").size());
        // First node survives (positional), others removed
        assertEquals("A", xml.sel("//item").first().text());
    }

    @Test
    void joinShorthand_returnsMergedEnterAndUpdate() {
        XML xml = XML.parse("<root><item>existing</item></root>");

        JoinedSel<String> joined = xml.sel("//item")
            .data(List.of("X", "Y", "Z"))
            .join("item");

        // 1 update + 2 enter = 3 in merged
        assertEquals(3, joined.size());
        // The update node retains its text
        assertEquals("existing", xml.sel("//item").first().text());
    }

    @Test
    void joinShorthand_postJoinAttrWithWorks() {
        XML xml = XML.parse("<root><item>A</item></root>");

        xml.sel("//item")
            .data(List.of("first", "second"))
            .join("item")
            .attrWith("class", (current, datum) -> datum);

        List<Node> items = xml.sel("//item").list();
        assertEquals("first", items.get(0).attr("class"));
        assertEquals("second", items.get(1).attr("class"));
    }

    @Test
    void joinShorthand_postJoinTextWithWorks() {
        XML xml = XML.parse("<root></root>");

        xml.sel("//item")
            .data(List.of("hello", "world"))
            .join("item")
            .textWith(datum -> datum);

        List<Node> items = xml.sel("//item").list();
        assertEquals("hello", items.get(0).text());
        assertEquals("world", items.get(1).text());
    }

    @Test
    void joinShorthand_returnsJoinedSel() {
        XML xml = XML.parse("<root></root>");

        var result = xml.sel("//item").data(List.of("A")).join("item");

        assertInstanceOf(JoinedSel.class, result);
    }
}
