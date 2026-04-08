package net.fndanko.xml.artisan;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JoinAdvancedTest {

    // --- Insertion position ---

    @Test
    void enterNodes_insertedInDataOrder_afterOrder() {
        XML xml = XML.parse("<root><item id='B'/></root>");

        xml.sel("//item")
            .data(List.of("A", "B", "C"),
                  s -> s,
                  n -> n.attr("id"))
            .join(JoinConfig.<String>builder()
                .defaults("item")
                .enter((parent, datum) -> {
                    Node n = parent.append("item");
                    n.attr("id", datum);
                    return n;
                })
                .build())
            .order();

        List<Node> items = xml.sel("//item").list();
        assertEquals(3, items.size());
        assertEquals("A", items.get(0).attr("id"));
        assertEquals("B", items.get(1).attr("id"));
        assertEquals("C", items.get(2).attr("id"));
    }

    // --- Multiple parent groups (D3 semantics: each group gets full data) ---

    @Test
    void multipleParents_eachGroupGetsFullData() {
        XML xml = XML.parse("<root><ul><li/></ul><ul><li/></ul></root>");

        // 2 nodes under different parents, data = ["A", "B", "C"]
        // Per-parent: each group (1 node) independently joins with full data (3 items)
        // Group 1: 1 node + 3 data → 1 update + 2 enter
        // Group 2: 1 node + 3 data → 1 update + 2 enter
        xml.sel("//li").data(List.of("A", "B", "C")).join("li")
            .textWith(d -> d);

        assertEquals(6, xml.sel("//li").size());
        assertEquals(3, xml.sel("//ul[1]/li").size());
        assertEquals(3, xml.sel("//ul[2]/li").size());
    }

    @Test
    void multipleParents_exitPerGroup() {
        XML xml = XML.parse(
            "<root><ul><li>1</li><li>2</li><li>3</li></ul><ul><li>4</li><li>5</li></ul></root>");

        // Group 1: 3 nodes, data ["X"] → 1 update + 2 exit
        // Group 2: 2 nodes, data ["X"] → 1 update + 1 exit
        xml.sel("//li").data(List.of("X")).join("li");

        assertEquals(1, xml.sel("//ul[1]/li").size());
        assertEquals(1, xml.sel("//ul[2]/li").size());
        assertEquals(2, xml.sel("//li").size());
    }

    @Test
    void multipleParents_enterCreatesUnderCorrectParent() {
        XML xml = XML.parse("<root><group id='a'/><group id='b'/></root>");

        // No items exist, two groups each get new children
        xml.sel("//group").sel("item").data(List.of("X")).join("item");

        // Each group gets its own enter — one item per group
        assertEquals(2, xml.sel("//item").size());
        assertEquals(1, xml.sel("//group[@id='a']/item").size());
        assertEquals(1, xml.sel("//group[@id='b']/item").size());
    }

    @Test
    void multipleParents_emptySubSelWithMultipleData() {
        XML xml = XML.parse("<root><group id='a'/><group id='b'/></root>");

        // Each group independently joins full data array
        xml.sel("//group").sel("item").data(List.of("X", "Y")).join("item")
            .textWith(d -> d);

        assertEquals(4, xml.sel("//item").size());
        assertEquals(2, xml.sel("//group[@id='a']/item").size());
        assertEquals(2, xml.sel("//group[@id='b']/item").size());
    }

    @Test
    void chainedEmptySelections_onlyImmediateParentUsed() {
        // D3 semantics: chaining selectAll on an empty result loses parent context
        // after the first empty level. Only the immediate parent Sel is consulted.
        XML xml = XML.parse("<root><group id='a'/><group id='b'/></root>");

        // sel("subgroup") is empty → parent is the groups Sel
        // sel("item") is also empty → parent is the empty subgroup Sel
        // Since subgroup Sel is empty, fallback to document root
        xml.sel("//group").sel("subgroup").sel("item").data(List.of("X")).join("item");

        // Items created under document root, not under groups (no intermediate synthesis)
        assertEquals(1, xml.sel("//item").size());
        assertEquals(0, xml.sel("//group/item").size());
    }

    // --- Exit then enter in same join — parent captured before exit ---

    @Test
    void exitThenEnter_parentCapturedBeforeExit() {
        XML xml = XML.parse("<root><container><item id='old'/></container></root>");

        // Key matching: data "new" doesn't match node id="old"
        // → exit: node id=old removed, enter: "new" created
        xml.sel("//item")
            .data(List.of("new"), s -> s, n -> n.attr("id"))
            .join(JoinConfig.<String>builder()
                .defaults("item")
                .enter((parent, datum) -> {
                    Node n = parent.append("item");
                    n.attr("id", datum);
                    return n;
                })
                .build());

        // New item created under container (parent captured before exit removed old node)
        assertEquals(1, xml.sel("//item").size());
        assertEquals(1, xml.sel("//container/item").size());
        assertEquals("new", xml.sel("//container/item").first().attr("id"));
    }

    // --- Repeated joins ---

    @Test
    void repeatedJoin_onSameDocument() {
        XML xml = XML.parse("<root></root>");

        // First join: create 3 items
        xml.sel("//item").data(List.of("A", "B", "C")).join("item")
            .textWith(d -> d);

        assertEquals(3, xml.sel("//item").size());
        assertEquals("A", xml.sel("//item").first().text());

        // Second join: reduce to 2 items, overwrite text
        xml.sel("//item").data(List.of("X", "Y")).join("item")
            .textWith(d -> d);

        List<Node> items = xml.sel("//item").list();
        assertEquals(2, items.size());
        assertEquals("X", items.get(0).text());
        assertEquals("Y", items.get(1).text());
    }

    // --- Different data types ---

    @Test
    void join_withStringData() {
        XML xml = XML.parse("<root></root>");

        xml.sel("//item").data(List.of("hello", "world")).join("item")
            .textWith(d -> d);

        List<Node> items = xml.sel("//item").list();
        assertEquals("hello", items.get(0).text());
        assertEquals("world", items.get(1).text());
    }

    @Test
    void join_withRecordData() {
        record Person(String name, int age) {}
        XML xml = XML.parse("<root></root>");

        xml.sel("//person").data(List.of(
            new Person("Alice", 30),
            new Person("Bob", 25)
        )).join("person")
            .attrWith("name", (c, p) -> p.name())
            .attrWith("age", (c, p) -> String.valueOf(p.age()));

        List<Node> people = xml.sel("//person").list();
        assertEquals(2, people.size());
        assertEquals("Alice", people.get(0).attr("name"));
        assertEquals("30", people.get(0).attr("age"));
        assertEquals("Bob", people.get(1).attr("name"));
        assertEquals("25", people.get(1).attr("age"));
    }

    // --- Full enter + update + exit together ---

    @Test
    void fullJoin_enterUpdateExitTogether() {
        XML xml = XML.parse("<root><item id='1'/><item id='2'/><item id='3'/></root>");

        xml.sel("//item")
            .data(List.of("2", "4"),
                  s -> s,
                  n -> n.attr("id"))
            .join(JoinConfig.<String>builder()
                .defaults("item")
                .enter((parent, datum) -> {
                    Node n = parent.append("item");
                    n.attr("id", datum);
                    return n;
                })
                .update((node, datum) -> {
                    node.attr("status", "kept");
                    return node;
                })
                .build());

        // id=1 and id=3 removed (exit), id=2 kept (update), id=4 created (enter)
        assertEquals(2, xml.sel("//item").size());
        assertEquals("kept", xml.sel("//item[@id='2']").first().attr("status"));
        assertFalse(xml.sel("//item[@id='4']").empty());
        assertTrue(xml.sel("//item[@id='1']").empty());
        assertTrue(xml.sel("//item[@id='3']").empty());
    }

    // --- order() after key-based join ---

    @Test
    void order_afterKeyJoin_reordersDom() {
        XML xml = XML.parse("<root><item id='A'/><item id='B'/><item id='C'/></root>");

        xml.sel("//item")
            .data(List.of("C", "A", "B"),
                  s -> s,
                  n -> n.attr("id"))
            .join("item")
            .order();

        List<Node> items = xml.sel("//item").list();
        assertEquals(3, items.size());
        assertEquals("C", items.get(0).attr("id"));
        assertEquals("A", items.get(1).attr("id"));
        assertEquals("B", items.get(2).attr("id"));
    }

    // --- Empty selection with data ---

    @Test
    void emptySelection_allDataCreatesEnterNodes() {
        XML xml = XML.parse("<root></root>");

        xml.sel("//item")
            .data(List.of("A", "B", "C"))
            .join("item")
            .textWith(d -> d);

        List<Node> items = xml.sel("//item").list();
        assertEquals(3, items.size());
        assertEquals("A", items.get(0).text());
        assertEquals("B", items.get(1).text());
        assertEquals("C", items.get(2).text());
    }

    // --- Join preserves non-selected siblings ---

    @Test
    void join_preservesNonSelectedSiblings() {
        XML xml = XML.parse("<root><header/><item>A</item><item>B</item><footer/></root>");

        xml.sel("//item").data(List.of("X")).join("item");

        assertEquals(1, xml.sel("//header").size());
        assertEquals(1, xml.sel("//footer").size());
        assertEquals(1, xml.sel("//item").size());
        // Surviving item retains original text
        assertEquals("A", xml.sel("//item").first().text());
    }

    // --- Merged list is in data order ---

    @Test
    void mergedList_inDataOrder_positional() {
        XML xml = XML.parse("<root><item>old</item></root>");

        JoinedSel<String> joined = xml.sel("//item")
            .data(List.of("first", "second", "third"))
            .join("item")
            .textWith(d -> d);

        // The merged list should be in data order
        List<Node> items = joined.list();
        assertEquals(3, items.size());
        assertEquals("first", items.get(0).text());
        assertEquals("second", items.get(1).text());
        assertEquals("third", items.get(2).text());
    }

    @Test
    void mergedList_inDataOrder_keyBased() {
        XML xml = XML.parse("<root><item id='C'/><item id='A'/></root>");

        JoinedSel<String> joined = xml.sel("//item")
            .data(List.of("A", "B", "C"), s -> s, n -> n.attr("id"))
            .join("item");

        // Data order: A, B, C — merged should follow this order
        // A=update (matches id=A), B=enter, C=update (matches id=C)
        List<Node> items = joined.list();
        assertEquals(3, items.size());
        assertEquals("A", items.get(0).attr("id"));
        // items.get(1) is the newly created enter node
        assertEquals("C", items.get(2).attr("id"));
    }

    // --- Edge cases: empty sub-selection parent recovery ---

    @Test
    void emptySubSel_singleParent_createsUnderCorrectParent() {
        XML xml = XML.parse("<root><container/></root>");

        xml.sel("//container").sel("item").data(List.of("A", "B")).join("item")
            .textWith(d -> d);

        assertEquals(2, xml.sel("//container/item").size());
        assertEquals("A", xml.sel("//container/item[1]").first().text());
        assertEquals("B", xml.sel("//container/item[2]").first().text());
    }

    @Test
    void emptySubSel_fromNode_createsUnderThatNode() {
        XML xml = XML.parse("<root><div id='target'/><div id='other'/></root>");

        Node target = xml.sel("//div[@id='target']").first();
        target.sel("item").data(List.of("X")).join("item");

        assertEquals(1, xml.sel("//div[@id='target']/item").size());
        assertEquals(0, xml.sel("//div[@id='other']/item").size());
    }

    @Test
    void emptySubSel_parentHasDuplicateDomNodes() {
        // XPath union can return the same node — sel deduplicates via LinkedHashSet,
        // but verify grouping doesn't create duplicate children
        XML xml = XML.parse("<root><g id='x'/></root>");

        // sel("//g | //g") would deduplicate to one node in Sel
        xml.sel("//g | //g").sel("item").data(List.of("A")).join("item");

        assertEquals(1, xml.sel("//item").size());
        assertEquals(1, xml.sel("//g/item").size());
    }

    @Test
    void emptySubSel_parentsAtDifferentDepths() {
        XML xml = XML.parse("<root><a><b/></a><c/></root>");

        // Select nodes at different depths, then empty sub-sel + join
        xml.sel("//b | //c").sel("item").data(List.of("X")).join("item");

        assertEquals(2, xml.sel("//item").size());
        assertEquals(1, xml.sel("//b/item").size());
        assertEquals(1, xml.sel("//c/item").size());
    }

    @Test
    void chainedEmptySelections_threeDeep() {
        XML xml = XML.parse("<root><a/></root>");

        // a exists, but b, c, item don't
        // sel("b") empty → parent = sel("//a") with nodes
        // sel("c") empty → parent = sel("b") which is empty
        // sel("item") empty → parent = sel("c") which is empty
        // Falls back to document root
        xml.sel("//a").sel("b").sel("c").sel("item").data(List.of("X")).join("item");

        assertEquals(1, xml.sel("//item").size());
        assertEquals(0, xml.sel("//a/item").size());
        // Created under document root
        assertEquals(1, xml.sel("/root/item").size());
    }

    // --- Edge case: mixed empty/non-empty groups ---

    @Test
    void mixedGroups_someParentsHaveChildren_othersEmpty() {
        XML xml = XML.parse("<root><g id='a'><item/></g><g id='b'/></root>");

        // Group a: 1 existing item, group b: 0 items
        // Both should independently join with data ["X"]
        // Group a: 1 update, group b: 1 enter
        xml.sel("//g").sel("item").data(List.of("X")).join("item");

        assertEquals(2, xml.sel("//item").size());
        assertEquals(1, xml.sel("//g[@id='a']/item").size());
        assertEquals(1, xml.sel("//g[@id='b']/item").size());
    }

    // --- Edge cases: data boundaries with empty selections ---

    @Test
    void emptyData_emptySelection_createsNothing() {
        XML xml = XML.parse("<root><g id='a'/><g id='b'/></root>");

        xml.sel("//g").sel("item").data(List.of()).join("item");

        assertEquals(0, xml.sel("//item").size());
    }

    @Test
    void keyBased_allEnter_emptySubSel() {
        XML xml = XML.parse("<root><g id='a'/><g id='b'/></root>");

        xml.sel("//g").sel("item")
            .data(List.of("X", "Y"), s -> s, n -> n.attr("id"))
            .join(JoinConfig.<String>builder()
                .defaults("item")
                .enter((parent, datum) -> {
                    Node n = parent.append("item");
                    n.attr("id", datum);
                    return n;
                })
                .build());

        // Each group creates 2 items (all enter, key matching finds nothing)
        assertEquals(4, xml.sel("//item").size());
        assertEquals(2, xml.sel("//g[@id='a']/item").size());
        assertEquals(2, xml.sel("//g[@id='b']/item").size());
    }

    @Test
    void keyBased_duplicateDataKeys_emptySubSel() {
        XML xml = XML.parse("<root><g/></root>");

        xml.sel("//g").sel("item")
            .data(List.of("same", "same", "different"), s -> s, n -> n.attr("id"))
            .join(JoinConfig.<String>builder()
                .defaults("item")
                .enter((parent, datum) -> {
                    Node n = parent.append("item");
                    n.attr("id", datum);
                    return n;
                })
                .build());

        // All 3 data items should create enter nodes (no existing nodes to match)
        assertEquals(3, xml.sel("//g/item").size());
    }

    // --- Edge cases: post-join operations after empty sub-sel ---

    @Test
    void emptySubSel_joinThenAttrWith() {
        XML xml = XML.parse("<root><g id='a'/><g id='b'/></root>");

        xml.sel("//g").sel("item").data(List.of("X", "Y")).join("item")
            .attrWith("val", (current, datum) -> datum);

        // All 4 items (2 per group) should have the attr set
        List<Node> items = xml.sel("//item").list();
        assertEquals(4, items.size());
        for (Node item : items) {
            assertFalse(item.attr("val").isEmpty());
        }
    }

    @Test
    void emptySubSel_joinThenOrder() {
        XML xml = XML.parse("<root><g id='a'/><g id='b'/></root>");

        xml.sel("//g").sel("item").data(List.of("B", "A")).join("item")
            .textWith(d -> d)
            .order();

        // order() should reorder within each parent, not mix across parents
        assertEquals(2, xml.sel("//g[@id='a']/item").size());
        assertEquals(2, xml.sel("//g[@id='b']/item").size());
    }

    @Test
    void emptySubSel_customEnterHandler_receivesCorrectParent() {
        XML xml = XML.parse("<root><g id='a'/><g id='b'/></root>");

        List<String> parentIds = new ArrayList<>();

        xml.sel("//g").sel("item")
            .data(List.of("X"))
            .join(JoinConfig.<String>builder()
                .defaults("item")
                .enter((parent, datum) -> {
                    parentIds.add(parent.attr("id"));
                    return parent.append("item");
                })
                .build());

        // Enter handler should be called once per group, with the actual group parent
        assertEquals(2, parentIds.size());
        assertTrue(parentIds.contains("a"));
        assertTrue(parentIds.contains("b"));
    }

    // --- Edge cases: repeated and nested joins ---

    @Test
    void emptySubSel_joinTwice_secondJoinSeesFirstResults() {
        XML xml = XML.parse("<root><g id='a'/><g id='b'/></root>");

        // First join: create items under each group
        xml.sel("//g").sel("item").data(List.of("X")).join("item");
        assertEquals(2, xml.sel("//item").size());

        // Second join: re-select and join with more data
        // Now items exist, so groupByParent finds them normally
        xml.sel("//g").sel("item").data(List.of("X", "Y")).join("item");
        assertEquals(4, xml.sel("//item").size());
        assertEquals(2, xml.sel("//g[@id='a']/item").size());
        assertEquals(2, xml.sel("//g[@id='b']/item").size());
    }

    @Test
    void reusedSelObject_joinedTwice() {
        XML xml = XML.parse("<root></root>");

        Sel items = xml.sel("//item");

        // First join creates 2 items
        items.data(List.of("A", "B")).join("item").textWith(d -> d);
        assertEquals(2, xml.sel("//item").size());

        // Second join on same (now stale) Sel object — still has empty nodes list
        // Parent recovery should still work (parent is root sel)
        items.data(List.of("X", "Y", "Z")).join("item").textWith(d -> d);

        // Creates 3 MORE items (Sel object is stale, doesn't see the 2 existing ones)
        assertEquals(5, xml.sel("//item").size());
    }
}
