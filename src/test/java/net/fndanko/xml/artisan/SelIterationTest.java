package net.fndanko.xml.artisan;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class SelIterationTest {

    // --- for-each ---

    @Test
    void forEach_onSel_iteratesAllNodes() {
        XML xml = XML.parse("<root><a/><b/><c/></root>");
        Sel children = xml.sel("/root/*");

        List<String> names = new ArrayList<>();
        for (Node node : children) {
            names.add(node.name());
        }

        assertEquals(List.of("a", "b", "c"), names);
    }

    @Test
    void forEach_onEmptySel_noIteration() {
        XML xml = XML.parse("<root/>");
        Sel empty = xml.sel("//nonexistent");

        List<Node> visited = new ArrayList<>();
        for (Node node : empty) {
            visited.add(node);
        }

        assertTrue(visited.isEmpty());
    }

    // --- stream() ---

    @Test
    void stream_returnStreamWithAllNodes() {
        XML xml = XML.parse("<root><a/><b/><c/></root>");

        List<String> names = xml.sel("/root/*").stream()
                .map(Node::name)
                .collect(Collectors.toList());

        assertEquals(List.of("a", "b", "c"), names);
    }

    @Test
    void stream_onEmptySel_returnsEmptyStream() {
        XML xml = XML.parse("<root/>");

        long count = xml.sel("//nonexistent").stream().count();

        assertEquals(0, count);
    }

    // --- list() ---

    @Test
    void list_returnsListWithAllNodes() {
        XML xml = XML.parse("<root><a/><b/></root>");

        List<Node> nodes = xml.sel("/root/*").list();

        assertEquals(2, nodes.size());
        assertEquals("a", nodes.get(0).name());
        assertEquals("b", nodes.get(1).name());
    }

    @Test
    void list_onEmptySel_returnsEmptyList() {
        XML xml = XML.parse("<root/>");

        List<Node> nodes = xml.sel("//nonexistent").list();

        assertTrue(nodes.isEmpty());
    }

    // --- first() ---

    @Test
    void first_returnsFirstNode() {
        XML xml = XML.parse("<root><a/><b/><c/></root>");

        Node first = xml.sel("/root/*").first();

        assertEquals("a", first.name());
    }

    @Test
    void first_onEmptySel_returnsEmptyNode() {
        XML xml = XML.parse("<root/>");

        Node first = xml.sel("//nonexistent").first();

        assertTrue(first.empty());
        assertEquals("", first.name());
    }

    // --- last() ---

    @Test
    void last_returnsLastNode() {
        XML xml = XML.parse("<root><a/><b/><c/></root>");

        Node last = xml.sel("/root/*").last();

        assertEquals("c", last.name());
    }

    @Test
    void last_onEmptySel_returnsEmptyNode() {
        XML xml = XML.parse("<root/>");

        Node last = xml.sel("//nonexistent").last();

        assertTrue(last.empty());
        assertEquals("", last.name());
    }

    // --- order() ---

    @Test
    void order_reordersNodesInDomAccordingToSelectionOrder() {
        XML xml = XML.parse("<root><c/><a/><b/></root>");

        // Build a selection in a specific order: b, a, c
        // XPath union returns document order, so we build manually
        Node b = xml.sel("/root/b").first();
        Node a = xml.sel("/root/a").first();
        Node c = xml.sel("/root/c").first();
        Sel sel = new Sel(
                List.of(b.unwrap(), a.unwrap(), c.unwrap()),
                null, null);
        sel.order();

        // after order(), the DOM should reflect the selection order
        List<String> names = xml.sel("/root/*").stream()
                .map(Node::name)
                .collect(Collectors.toList());

        assertEquals(List.of("b", "a", "c"), names);
    }
}
