package net.fndanko.xml.artisan;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class DataBindingTest {

    // --- Positional matching ---

    @Test
    void data_positional_matchesByIndex() {
        XML xml = XML.parse("<root><item>A</item><item>B</item><item>C</item></root>");

        xml.sel("//item")
            .data(List.of("X", "Y", "Z"))
            .join("item")
            .textWith(d -> d);

        List<Node> items = xml.sel("//item").list();
        assertEquals(3, items.size());
        // Verify each datum was applied to the correct positional node
        assertEquals("X", items.get(0).text());
        assertEquals("Y", items.get(1).text());
        assertEquals("Z", items.get(2).text());
    }

    @Test
    void data_positional_moreDataThanNodes_createsEnterNodes() {
        XML xml = XML.parse("<root><item>A</item></root>");

        xml.sel("//item")
            .data(List.of("X", "Y", "Z"))
            .join("item")
            .textWith(d -> d);

        List<Node> items = xml.sel("//item").list();
        assertEquals(3, items.size());
        // First node updated with "X", two new nodes created
        assertEquals("X", items.get(0).text());
        assertEquals("Y", items.get(1).text());
        assertEquals("Z", items.get(2).text());
    }

    @Test
    void data_positional_moreNodesThanData_removesExitNodes() {
        XML xml = XML.parse("<root><item>A</item><item>B</item><item>C</item></root>");

        xml.sel("//item").data(List.of("X")).join("item");

        assertEquals(1, xml.sel("//item").size());
        // The surviving node is the first one (positional: index 0)
        assertEquals("A", xml.sel("//item").first().text());
    }

    @Test
    void data_positional_emptyData_removesAllNodes() {
        XML xml = XML.parse("<root><item>A</item><item>B</item></root>");

        JoinedSel<String> joined = xml.sel("//item").data(List.<String>of()).join("item");

        assertEquals(0, joined.size());
        assertEquals(0, xml.sel("//item").size());
    }

    @Test
    void data_positional_emptyNodes_createsAllNew() {
        XML xml = XML.parse("<root></root>");

        xml.sel("//item")
            .data(List.of("X", "Y"))
            .join("item")
            .textWith(d -> d);

        List<Node> items = xml.sel("//item").list();
        assertEquals(2, items.size());
        assertEquals("X", items.get(0).text());
        assertEquals("Y", items.get(1).text());
    }

    @Test
    void data_positional_duplicateEqualValues_eachGetsOwnNode() {
        XML xml = XML.parse("<root><item>A</item><item>B</item></root>");

        xml.sel("//item")
            .data(List.of("X", "X", "X"))
            .join("item")
            .attrWith("val", (c, d) -> d);

        List<Node> items = xml.sel("//item").list();
        assertEquals(3, items.size());
        // All three get the attribute (each datum applied independently)
        for (Node item : items) {
            assertEquals("X", item.attr("val"));
        }
        // First two retain original text (update = identity), third is new (enter)
        assertEquals("A", items.get(0).text());
        assertEquals("B", items.get(1).text());
        assertEquals("", items.get(2).text());
    }

    // --- Key function matching ---

    @Test
    void data_keyFunction_matchesByKey() {
        XML xml = XML.parse("<root><item id='1'>old1</item><item id='2'>old2</item></root>");

        xml.sel("//item")
            .data(List.of("1", "2"), s -> s, n -> n.attr("id"))
            .join("item")
            .attrWith("matched", (c, d) -> d);

        // Verify correct datum associated with correct node
        assertEquals("1", xml.sel("//item[@id='1']").first().attr("matched"));
        assertEquals("2", xml.sel("//item[@id='2']").first().attr("matched"));
        // Original text untouched (identity update)
        assertEquals("old1", xml.sel("//item[@id='1']").first().text());
        assertEquals("old2", xml.sel("//item[@id='2']").first().text());
    }

    @Test
    void data_keyFunction_reorderedData_matchesCorrectly() {
        XML xml = XML.parse("<root><item id='1'>A</item><item id='2'>B</item></root>");

        JoinedSel<String> joined = xml.sel("//item")
            .data(List.of("2", "1"), s -> s, n -> n.attr("id"))
            .join("item")
            .attrWith("matched", (c, d) -> d);

        assertEquals(2, joined.size());
        assertEquals(2, xml.sel("//item").size());
        // Each node got the correct datum regardless of data order
        assertEquals("1", xml.sel("//item[@id='1']").first().attr("matched"));
        assertEquals("2", xml.sel("//item[@id='2']").first().attr("matched"));
    }

    @Test
    void data_keyFunction_unmatchedData_createsEnterNodes() {
        XML xml = XML.parse("<root><item id='1'>A</item></root>");

        xml.sel("//item")
            .data(List.of("1", "3"), s -> s, n -> n.attr("id"))
            .join("item");

        assertEquals(2, xml.sel("//item").size());
        // id=1 survived (update), new node created (enter for "3")
        assertEquals("A", xml.sel("//item[@id='1']").first().text());
    }

    @Test
    void data_keyFunction_unmatchedNodes_removesExitNodes() {
        XML xml = XML.parse("<root><item id='1'>A</item><item id='2'>B</item></root>");

        xml.sel("//item")
            .data(List.of("1"), s -> s, n -> n.attr("id"))
            .join("item");

        assertEquals(1, xml.sel("//item").size());
        // id=1 survived, id=2 removed
        assertEquals("A", xml.sel("//item[@id='1']").first().text());
        assertTrue(xml.sel("//item[@id='2']").empty());
    }

    @Test
    void data_keyFunction_duplicateNodeKeys_extrasGoToExit() {
        XML xml = XML.parse("<root><item id='1'>first</item><item id='1'>second</item></root>");

        xml.sel("//item")
            .data(List.of("1"), s -> s, n -> n.attr("id"))
            .join("item");

        assertEquals(1, xml.sel("//item").size());
        // First node wins, second (duplicate) goes to exit
        assertEquals("first", xml.sel("//item").first().text());
    }

    @Test
    void data_keyFunction_duplicateDataKeys_extrasGoToEnter() {
        XML xml = XML.parse("<root><item id='1'>A</item></root>");

        xml.sel("//item")
            .data(List.of("1", "1"), s -> s, n -> n.attr("id"))
            .join("item");

        assertEquals(2, xml.sel("//item").size());
        // First datum matched the node (update), second datum is enter (new node)
        assertEquals("A", xml.sel("//item").first().text());
    }
}
