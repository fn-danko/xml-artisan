package net.fndanko.xml.artisan;

import net.jqwik.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SelInvariantProperties {

    // --- Arbitraries ---

    @Provide
    Arbitrary<String> tagNames() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(10);
    }

    @Provide
    Arbitrary<String> safeText() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .withChars(' ')
                .ofMinLength(1)
                .ofMaxLength(50);
    }

    @Provide
    Arbitrary<String> xmlWithItems() {
        return Arbitraries.integers().between(0, 20).flatMap(count -> {
            StringBuilder sb = new StringBuilder("<root>");
            for (int i = 0; i < count; i++) {
                sb.append("<item id=\"").append(i).append("\">text").append(i).append("</item>");
            }
            sb.append("</root>");
            return Arbitraries.just(sb.toString());
        });
    }

    // --- Properties ---

    @Property
    void emptySelIsNoOp(@ForAll("tagNames") String tag) {
        XML xml = XML.parse("<root/>");
        Sel empty = xml.sel("//nonexistent");

        String before = xml.toString();

        // All these should be no-ops on empty Sel
        empty.attr("x", "y");
        empty.text("hello");
        empty.append(tag);
        empty.prepend(tag);

        assertEquals(before, xml.toString(),
                "Operations on empty Sel should not modify document");
    }

    @Property
    void selSize_matchesList(@ForAll("xmlWithItems") String xmlStr) {
        XML xml = XML.parse(xmlStr);
        Sel items = xml.sel("//item");
        assertEquals(items.size(), items.list().size());
    }

    @Property
    void selEmpty_matchesSize(@ForAll("xmlWithItems") String xmlStr) {
        XML xml = XML.parse(xmlStr);
        Sel items = xml.sel("//item");
        assertEquals(items.empty(), items.size() == 0);
    }

    @Property
    void attrSetGet_roundTrips(@ForAll("tagNames") String attrName, @ForAll("safeText") String attrValue) {
        XML xml = XML.parse("<root><item/></root>");
        Node item = xml.sel("//item").first();
        item.attr(attrName, attrValue);
        assertEquals(attrValue, item.attr(attrName));
    }

    @Property
    void textSetGet_roundTrips(@ForAll("safeText") String textValue) {
        XML xml = XML.parse("<root><item/></root>");
        Node item = xml.sel("//item").first();
        item.text(textValue);
        assertEquals(textValue, item.text());
    }

    @Property
    void appendIncreasesChildCount(@ForAll("tagNames") String childTag) {
        XML xml = XML.parse("<root><parent/></root>");
        Node parent = xml.sel("//parent").first();

        int before = parent.children().size();
        parent.append(childTag);
        int after = parent.children().size();

        assertEquals(before + 1, after);
    }

    @Property
    void removeDecreasesCount(@ForAll("xmlWithItems") String xmlStr) {
        XML xml = XML.parse(xmlStr);
        int before = xml.sel("//item").size();
        Assume.that(before > 0);

        xml.sel("//item").first().remove();
        int after = xml.sel("//item").size();

        assertEquals(before - 1, after);
    }

    @Property
    void firstAndLast_consistentWithList(@ForAll("xmlWithItems") String xmlStr) {
        XML xml = XML.parse(xmlStr);
        Sel items = xml.sel("//item");
        Assume.that(!items.empty());

        List<Node> list = items.list();
        assertEquals(list.get(0).attr("id"), items.first().attr("id"));
        assertEquals(list.get(list.size() - 1).attr("id"), items.last().attr("id"));
    }

    @Property
    void streamSize_matchesSelSize(@ForAll("xmlWithItems") String xmlStr) {
        XML xml = XML.parse(xmlStr);
        Sel items = xml.sel("//item");
        assertEquals(items.size(), items.stream().count());
    }

    @Property
    void endReturnsParent(@ForAll("xmlWithItems") String xmlStr) {
        XML xml = XML.parse(xmlStr);
        Sel root = xml.sel("/*");
        Sel sub = root.sel("item");
        assertSame(root, sub.end());
    }
}
