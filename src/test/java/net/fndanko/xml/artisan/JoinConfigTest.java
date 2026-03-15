package net.fndanko.xml.artisan;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JoinConfigTest {

    @Test
    void defaults_setsEnterAppendUpdateIdentityExitRemove() {
        XML xml = XML.parse("<root><item>old</item></root>");

        JoinConfig<String> config = JoinConfig.<String>builder()
            .defaults("item")
            .build();

        xml.sel("//item").data(List.of("A", "B")).join(config);

        // 1 update + 1 enter = 2 nodes
        assertEquals(2, xml.sel("//item").size());
        // Update node retains text (identity), enter node is empty
        assertEquals("old", xml.sel("//item").first().text());
        assertEquals("", xml.sel("//item").last().text());
    }

    @Test
    void defaults_excessNodesRemoved() {
        XML xml = XML.parse("<root><item>A</item><item>B</item><item>C</item></root>");

        JoinConfig<String> config = JoinConfig.<String>builder()
            .defaults("item")
            .build();

        xml.sel("//item").data(List.of("X")).join(config);

        assertEquals(1, xml.sel("//item").size());
        assertEquals("A", xml.sel("//item").first().text());
    }

    @Test
    void defaults_withUpdateOverride() {
        XML xml = XML.parse("<root><item>old</item></root>");

        JoinConfig<String> config = JoinConfig.<String>builder()
            .defaults("item")
            .update((node, datum) -> {
                node.text(datum.toString());
                return node;
            })
            .build();

        xml.sel("//item").data(List.of("new")).join(config);

        assertEquals("new", xml.sel("//item").first().text());
    }

    @Test
    void exitNull_keepsOrphanNodes() {
        XML xml = XML.parse("<root><item>A</item><item>B</item><item>C</item></root>");

        JoinConfig<String> config = JoinConfig.<String>builder()
            .defaults("item")
            .exit(null)
            .build();

        xml.sel("//item").data(List.of("X")).join(config);

        // exit(null) overrides the default remove — all 3 nodes survive
        assertEquals(3, xml.sel("//item").size());
    }

    @Test
    void enterNull_keepsEnterSlotEmpty() {
        XML xml = XML.parse("<root><item>A</item></root>");

        JoinConfig<String> config = JoinConfig.<String>builder()
            .defaults("item")
            .enter(null)
            .build();

        JoinedSel<String> joined = xml.sel("//item")
            .data(List.of("X", "Y"))
            .join(config);

        // enter(null) overrides default append — only 1 update, no enter nodes
        assertEquals(1, joined.size());
        assertEquals(1, xml.sel("//item").size());
    }

    @Test
    void updateNull_identityBehavior() {
        XML xml = XML.parse("<root><item>original</item></root>");

        JoinConfig<String> config = JoinConfig.<String>builder()
            .defaults("item")
            .update(null)
            .build();

        xml.sel("//item").data(List.of("X")).join(config);

        // update(null) = identity, node unchanged
        assertEquals("original", xml.sel("//item").first().text());
    }

    @Test
    void handlerOrder_exitThenUpdateThenEnter() {
        XML xml = XML.parse("<root><item id='1'/><item id='2'/></root>");
        List<String> order = new ArrayList<>();

        JoinConfig<String> config = JoinConfig.<String>builder()
            .enter((parent, datum) -> {
                order.add("enter:" + datum);
                return parent.append("item");
            })
            .update((node, datum) -> {
                order.add("update:" + datum);
                return node;
            })
            .exit(node -> order.add("exit:" + node.attr("id")))
            .build();

        // Key matching: data ["1","3"] vs nodes [id=1, id=2]
        // → exit: id=2, update: id=1 with "1", enter: "3"
        xml.sel("//item")
            .data(List.of("1", "3"), s -> s, n -> n.attr("id"))
            .join(config);

        assertEquals(List.of("exit:2", "update:1", "enter:3"), order);
    }

    @Test
    void customEnterHandler_createsCustomNodes() {
        XML xml = XML.parse("<root></root>");

        JoinConfig<String> config = JoinConfig.<String>builder()
            .enter((parent, datum) -> {
                Node n = parent.append("div");
                n.attr("class", datum);
                return n;
            })
            .build();

        xml.sel("//div").data(List.of("alpha", "beta")).join(config);

        List<Node> divs = xml.sel("//div").list();
        assertEquals(2, divs.size());
        assertEquals("alpha", divs.get(0).attr("class"));
        assertEquals("beta", divs.get(1).attr("class"));
    }

    @Test
    void customExitHandler_modifiesInsteadOfRemoving() {
        XML xml = XML.parse("<root><item>A</item><item>B</item></root>");

        JoinConfig<String> config = JoinConfig.<String>builder()
            .exit(node -> node.attr("deleted", "true"))
            .build();

        xml.sel("//item").data(List.<String>of()).join(config);

        // Both nodes still exist but marked deleted
        List<Node> items = xml.sel("//item").list();
        assertEquals(2, items.size());
        assertEquals("true", items.get(0).attr("deleted"));
        assertEquals("true", items.get(1).attr("deleted"));
    }

    @Test
    void noDefaults_noHandlers_enterIgnored_exitIgnored() {
        XML xml = XML.parse("<root><item>A</item></root>");

        JoinConfig<String> config = JoinConfig.<String>builder().build();

        JoinedSel<String> joined = xml.sel("//item")
            .data(List.of("X", "Y"))
            .join(config);

        // No enter handler → enter slots skipped; only 1 update in merged
        assertEquals(1, joined.size());
        // Original node survives, no new nodes created
        assertEquals(1, xml.sel("//item").size());
        assertEquals("A", xml.sel("//item").first().text());
    }

    @Test
    void noDefaults_noHandlers_exitIgnored() {
        XML xml = XML.parse("<root><item>A</item><item>B</item></root>");

        JoinConfig<String> config = JoinConfig.<String>builder().build();

        xml.sel("//item").data(List.of("X")).join(config);

        // No exit handler → exit nodes not removed
        assertEquals(2, xml.sel("//item").size());
        assertEquals("A", xml.sel("//item").first().text());
        assertEquals("B", xml.sel("//item").last().text());
    }
}
