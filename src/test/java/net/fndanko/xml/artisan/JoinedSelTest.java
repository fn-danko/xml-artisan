package net.fndanko.xml.artisan;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JoinedSelTest {

    @Test
    void attrWith_setAttributeFromDatum() {
        XML xml = XML.parse("<root><item>A</item><item>B</item></root>");

        xml.sel("//item")
            .data(List.of("x", "y"))
            .join("item")
            .attrWith("data-val", (current, datum) -> datum);

        List<Node> items = xml.sel("//item").list();
        assertEquals("x", items.get(0).attr("data-val"));
        assertEquals("y", items.get(1).attr("data-val"));
    }

    @Test
    void attrWith_accessesCurrentValue() {
        XML xml = XML.parse("<root><item class='old'>A</item></root>");

        xml.sel("//item")
            .data(List.of("new"))
            .join("item")
            .attrWith("class", (current, datum) -> current + "-" + datum);

        assertEquals("old-new", xml.sel("//item").first().attr("class"));
    }

    @Test
    void attrWith_onEnterAndUpdateNodes() {
        XML xml = XML.parse("<root><item>existing</item></root>");

        xml.sel("//item")
            .data(List.of("A", "B"))
            .join("item")
            .attrWith("source", (c, d) -> d);

        List<Node> items = xml.sel("//item").list();
        assertEquals(2, items.size());
        assertEquals("A", items.get(0).attr("source"));
        assertEquals("B", items.get(1).attr("source"));
        // First is update (retains text), second is enter (empty)
        assertEquals("existing", items.get(0).text());
        assertEquals("", items.get(1).text());
    }

    @Test
    void textWith_setsTextFromDatum() {
        XML xml = XML.parse("<root><item/><item/></root>");

        xml.sel("//item")
            .data(List.of("hello", "world"))
            .join("item")
            .textWith(datum -> datum);

        List<Node> items = xml.sel("//item").list();
        assertEquals("hello", items.get(0).text());
        assertEquals("world", items.get(1).text());
    }

    @Test
    void eachWith_executesOnEachNode() {
        XML xml = XML.parse("<root><item/><item/></root>");
        List<String> collected = new ArrayList<>();

        xml.sel("//item")
            .data(List.of("A", "B"))
            .join("item")
            .eachWith((node, datum) -> collected.add(datum));

        assertEquals(List.of("A", "B"), collected);
    }

    @Test
    void eachWith_nodeIsUsable() {
        XML xml = XML.parse("<root><item/><item/></root>");

        xml.sel("//item")
            .data(List.of("A", "B"))
            .join("item")
            .eachWith((node, datum) -> node.attr("id", datum));

        List<Node> items = xml.sel("//item").list();
        assertEquals("A", items.get(0).attr("id"));
        assertEquals("B", items.get(1).attr("id"));
    }

    @Test
    void toSel_abandonsBinding() {
        XML xml = XML.parse("<root><item/></root>");

        Sel sel = xml.sel("//item")
            .data(List.of("A"))
            .join("item")
            .toSel();

        assertInstanceOf(Sel.class, sel);
        assertFalse(sel instanceof JoinedSel);
        assertEquals(1, sel.size());
    }

    @Test
    void sel_returnsPlainSel() {
        XML xml = XML.parse("<root><item><child/></item></root>");

        Sel children = xml.sel("//item")
            .data(List.of("A"))
            .join("item")
            .sel("child");

        assertInstanceOf(Sel.class, children);
        assertFalse(children instanceof JoinedSel);
        assertEquals(1, children.size());
    }

    @Test
    void order_reordersDomToMatchDataOrder() {
        XML xml = XML.parse("<root><item id='1'/><item id='2'/><item id='3'/></root>");

        xml.sel("//item")
            .data(List.of("3", "1", "2"),
                  s -> s,
                  n -> n.attr("id"))
            .join("item")
            .order();

        List<Node> items = xml.sel("//item").list();
        assertEquals(3, items.size());
        assertEquals("3", items.get(0).attr("id"));
        assertEquals("1", items.get(1).attr("id"));
        assertEquals("2", items.get(2).attr("id"));
    }

    @Test
    void chaining_attrWithTextWithEachWith() {
        XML xml = XML.parse("<root></root>");
        List<String> visited = new ArrayList<>();

        xml.sel("//item")
            .data(List.of("hello"))
            .join("item")
            .attrWith("class", (c, d) -> d)
            .textWith(d -> d.toUpperCase())
            .eachWith((n, d) -> visited.add(d));

        Node item = xml.sel("//item").first();
        assertEquals("hello", item.attr("class"));
        assertEquals("HELLO", item.text());
        assertEquals(List.of("hello"), visited);
    }

    @Test
    void order_preservesDatumAssociation() {
        XML xml = XML.parse("<root><item id='A'/><item id='B'/></root>");

        xml.sel("//item")
            .data(List.of("B", "A"), s -> s, n -> n.attr("id"))
            .join("item")
            .order()
            .attrWith("pos", (c, d) -> d);

        // After order(), DOM order matches data order: B first, A second
        List<Node> items = xml.sel("//item").list();
        assertEquals("B", items.get(0).attr("id"));
        assertEquals("A", items.get(1).attr("id"));
        // attrWith still has correct datum association
        assertEquals("B", items.get(0).attr("pos"));
        assertEquals("A", items.get(1).attr("pos"));
    }
}
