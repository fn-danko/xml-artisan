package net.fndanko.xml.artisan;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NamespaceTest {

    // =====================================================
    // Reading namespaced elements via XPath
    // =====================================================

    @Test
    void sel_prefixedNamespace_selectsCorrectNodes() {
        XML xml = XML.parse(
            "<root xmlns:ns='http://example.com'>"
            + "<ns:item>A</ns:item><ns:item>B</ns:item></root>");
        xml.namespace("ns", "http://example.com");

        Sel items = xml.sel("//ns:item");

        assertEquals(2, items.size());
        assertEquals("A", items.first().text());
        assertEquals("B", items.last().text());
    }

    @Test
    void sel_multipleNamespaces_eachResolvesIndependently() {
        XML xml = XML.parse(
            "<root xmlns:a='http://a.com' xmlns:b='http://b.com'>"
            + "<a:x>alpha</a:x><b:x>beta</b:x></root>");
        xml.namespace("a", "http://a.com")
           .namespace("b", "http://b.com");

        assertEquals(1, xml.sel("//a:x").size());
        assertEquals("alpha", xml.sel("//a:x").text());
        assertEquals(1, xml.sel("//b:x").size());
        assertEquals("beta", xml.sel("//b:x").text());
    }

    @Test
    void sel_defaultNamespace_requiresPrefixInXPath() {
        // XPath 1.0 has no concept of default namespace — unprefixed names match no-namespace.
        // To query default-NS elements, register a prefix and use it in XPath.
        XML xml = XML.parse(
            "<root xmlns='http://default.com'><item>found</item></root>");
        xml.namespace("d", "http://default.com");

        // Without prefix → finds nothing (XPath 1.0 semantics)
        assertEquals(0, xml.sel("//item").size());
        // With registered prefix → finds the element
        assertEquals(1, xml.sel("//d:item").size());
        assertEquals("found", xml.sel("//d:item").text());
    }

    @Test
    void sel_nestedNamespaces_innerOverridesOuter() {
        XML xml = XML.parse(
            "<root xmlns:ns='http://outer.com'>"
            + "<ns:item>outer</ns:item>"
            + "<wrapper xmlns:ns='http://inner.com'>"
            + "<ns:item>inner</ns:item>"
            + "</wrapper></root>");
        xml.namespace("outer", "http://outer.com")
           .namespace("inner", "http://inner.com");

        assertEquals(1, xml.sel("//outer:item").size());
        assertEquals("outer", xml.sel("//outer:item").text());
        assertEquals(1, xml.sel("//inner:item").size());
        assertEquals("inner", xml.sel("//inner:item").text());
    }

    @Test
    void sel_subSelection_namespacedXPathWorks() {
        XML xml = XML.parse(
            "<root xmlns:ns='http://example.com'>"
            + "<ns:group><ns:item>A</ns:item></ns:group></root>");
        xml.namespace("ns", "http://example.com");

        Sel group = xml.sel("//ns:group");
        Sel item = group.sel("ns:item");

        assertEquals(1, item.size());
        assertEquals("A", item.text());
    }

    // =====================================================
    // Reading namespaced attributes
    // =====================================================

    @Test
    void attr_prefixedAttribute_readByQualifiedName() {
        XML xml = XML.parse(
            "<root xmlns:custom='http://custom.com'>"
            + "<item custom:status='active'/></root>");

        // DOM getAttribute works with the literal prefixed name from the source
        Node item = xml.sel("//item").first();
        assertEquals("active", item.attr("custom:status"));
    }

    @Test
    void attr_xmlLang_readable() {
        XML xml = XML.parse(
            "<root xml:lang='en'><item xml:lang='it'/></root>");

        assertEquals("en", xml.sel("/root").first().attr("xml:lang"));
        assertEquals("it", xml.sel("//item").first().attr("xml:lang"));
    }

    // =====================================================
    // Writing namespaced attributes
    // =====================================================

    @Test
    void attr_setPrefixedAttribute_preservedInSerialization() {
        XML xml = XML.parse(
            "<root xmlns:custom='http://custom.com'><item/></root>");

        xml.sel("//item").attr("custom:type", "test");

        String output = xml.toString();
        assertTrue(output.contains("custom:type=\"test\""),
            "Serialized output should contain the prefixed attribute: " + output);
    }

    @Test
    void attr_setXmlLang_preservedInSerialization() {
        XML xml = XML.parse("<root><item/></root>");

        xml.sel("//item").attr("xml:lang", "de");

        String output = xml.toString();
        assertTrue(output.contains("xml:lang=\"de\""),
            "Serialized output should contain xml:lang: " + output);
    }

    // =====================================================
    // Serialization preserves namespace declarations
    // =====================================================

    @Test
    void toString_preservesNamespaceDeclarations() {
        String input = "<root xmlns:ns='http://example.com'><ns:item>A</ns:item></root>";
        XML xml = XML.parse(input);

        String output = xml.toString();

        assertTrue(output.contains("xmlns:ns=\"http://example.com\"")
                || output.contains("xmlns:ns='http://example.com'"),
            "Namespace declaration should be preserved: " + output);
        assertTrue(output.contains("ns:item"),
            "Prefixed element should be preserved: " + output);
    }

    @Test
    void toFragment_preservesNamespaceDeclarations() {
        String input = "<root xmlns:ns='http://example.com'><ns:item>A</ns:item></root>";
        XML xml = XML.parse(input);

        String output = xml.toFragment();

        assertTrue(output.contains("xmlns:ns"),
            "Namespace declaration should be preserved in fragment: " + output);
    }

    @Test
    void toString_preservesDefaultNamespace() {
        String input = "<root xmlns='http://default.com'><item>A</item></root>";
        XML xml = XML.parse(input);

        String output = xml.toString();

        assertTrue(output.contains("xmlns=\"http://default.com\"")
                || output.contains("xmlns='http://default.com'"),
            "Default namespace should be preserved: " + output);
    }

    @Test
    void toString_preservesMultipleNamespaceDeclarations() {
        String input = "<root xmlns:a='http://a.com' xmlns:b='http://b.com'>"
            + "<a:x/><b:y/></root>";
        XML xml = XML.parse(input);

        String output = xml.toString();

        assertTrue(output.contains("xmlns:a") && output.contains("xmlns:b"),
            "Both namespace declarations should be preserved: " + output);
    }

    @Test
    void toString_roundTrip_namespacedContentUnchanged() {
        XML xml = XML.parse(
            "<root xmlns:ns='http://example.com'>"
            + "<ns:item ns:id='1'>text</ns:item></root>");
        xml.namespace("ns", "http://example.com");

        // Modify text content, verify namespace structure survives
        xml.sel("//ns:item").text("modified");

        String output = xml.toString();
        assertTrue(output.contains("ns:item"), "Element prefix preserved: " + output);
        assertTrue(output.contains("ns:id"), "Attribute prefix preserved: " + output);
        assertTrue(output.contains("modified"), "Text was modified: " + output);
    }

    // =====================================================
    // Structural operations — namespace-aware element creation
    // =====================================================

    @Test
    void append_namespacedTag_createsNsAwareElement() {
        XML xml = XML.parse(
            "<root xmlns:ns='http://example.com'><ns:parent/></root>");
        xml.namespace("ns", "http://example.com");

        xml.sel("//ns:parent").first().append("ns:child");

        // Created element should be namespace-aware and findable via NS XPath
        assertEquals(1, xml.sel("//ns:child").size());
        assertEquals("ns:child", xml.sel("//ns:child").first().name());
    }

    @Test
    void append_namespacedTag_serializesWithPrefix() {
        XML xml = XML.parse(
            "<root xmlns:ns='http://example.com'><ns:parent/></root>");
        xml.namespace("ns", "http://example.com");

        xml.sel("//ns:parent").first().append("ns:item");

        String output = xml.toString();
        assertTrue(output.contains("ns:item"),
            "Appended NS element should serialize with prefix: " + output);
    }

    @Test
    void prepend_namespacedTag_createsNsAwareElement() {
        XML xml = XML.parse(
            "<root xmlns:ns='http://example.com'>"
            + "<ns:parent><ns:existing/></ns:parent></root>");
        xml.namespace("ns", "http://example.com");

        xml.sel("//ns:parent").first().prepend("ns:first");

        Sel children = xml.sel("//ns:parent").first().children();
        assertEquals(2, children.size());
        assertEquals("ns:first", children.first().name());
    }

    @Test
    void before_namespacedTag_createsNsAwareElement() {
        XML xml = XML.parse(
            "<root xmlns:ns='http://example.com'><ns:item>A</ns:item></root>");
        xml.namespace("ns", "http://example.com");

        xml.sel("//ns:item").first().before("ns:prev");

        Sel children = xml.sel("/root").first().children();
        assertEquals(2, children.size());
        assertEquals("ns:prev", children.first().name());
        assertEquals("ns:item", children.last().name());
    }

    @Test
    void after_namespacedTag_createsNsAwareElement() {
        XML xml = XML.parse(
            "<root xmlns:ns='http://example.com'><ns:item>A</ns:item></root>");
        xml.namespace("ns", "http://example.com");

        xml.sel("//ns:item").first().after("ns:next");

        Sel children = xml.sel("/root").first().children();
        assertEquals(2, children.size());
        assertEquals("ns:item", children.first().name());
        assertEquals("ns:next", children.last().name());
    }

    @Test
    void selAppend_namespacedTag_createsMultipleNsElements() {
        XML xml = XML.parse(
            "<root xmlns:ns='http://example.com'>"
            + "<ns:group/><ns:group/></root>");
        xml.namespace("ns", "http://example.com");

        xml.sel("//ns:group").append("ns:item");

        assertEquals(2, xml.sel("//ns:item").size());
    }

    @Test
    void selPrepend_namespacedTag_createsNsElements() {
        XML xml = XML.parse(
            "<root xmlns:ns='http://example.com'>"
            + "<ns:parent><ns:existing/></ns:parent></root>");
        xml.namespace("ns", "http://example.com");

        xml.sel("//ns:parent").prepend("ns:first");

        assertEquals("ns:first", xml.sel("//ns:parent").first().children().first().name());
    }

    @Test
    void selBefore_namespacedTag_createsNsElements() {
        XML xml = XML.parse(
            "<root xmlns:ns='http://example.com'><ns:item/></root>");
        xml.namespace("ns", "http://example.com");

        xml.sel("//ns:item").before("ns:prev");

        assertEquals(1, xml.sel("//ns:prev").size());
    }

    @Test
    void selAfter_namespacedTag_createsNsElements() {
        XML xml = XML.parse(
            "<root xmlns:ns='http://example.com'><ns:item/></root>");
        xml.namespace("ns", "http://example.com");

        xml.sel("//ns:item").after("ns:next");

        assertEquals(1, xml.sel("//ns:next").size());
    }

    @Test
    void append_underNamespacedParent_plainChild() {
        XML xml = XML.parse(
            "<root xmlns:ns='http://example.com'><ns:parent/></root>");
        xml.namespace("ns", "http://example.com");

        xml.sel("//ns:parent").append("child");

        assertEquals(1, xml.sel("//ns:parent/child").size());
    }

    @Test
    void append_plainTagUnderNamespacedParent_serializes() {
        XML xml = XML.parse(
            "<root xmlns:ns='http://example.com'><ns:parent/></root>");
        xml.namespace("ns", "http://example.com");

        xml.sel("//ns:parent").append("child");

        String output = xml.toString();
        assertTrue(output.contains("<child/>") || output.contains("<child></child>"),
            "Plain child element should appear in output: " + output);
    }

    @Test
    void remove_namespacedNode_removedFromDom() {
        XML xml = XML.parse(
            "<root xmlns:ns='http://example.com'>"
            + "<ns:item>A</ns:item><ns:item>B</ns:item></root>");
        xml.namespace("ns", "http://example.com");

        xml.sel("//ns:item[1]").remove();

        assertEquals(1, xml.sel("//ns:item").size());
        assertEquals("B", xml.sel("//ns:item").text());
    }

    @Test
    void text_setOnNamespacedElement_works() {
        XML xml = XML.parse(
            "<root xmlns:ns='http://example.com'><ns:item/></root>");
        xml.namespace("ns", "http://example.com");

        xml.sel("//ns:item").text("hello");

        assertEquals("hello", xml.sel("//ns:item").text());
    }

    // =====================================================
    // Unregistered prefix — throws IllegalArgumentException
    // =====================================================

    @Test
    void append_unregisteredPrefix_throwsIllegalArgument() {
        XML xml = XML.parse("<root/>");
        // "foo" prefix not registered

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> xml.sel("/root").first().append("foo:item"));

        assertTrue(ex.getMessage().contains("foo"),
            "Error message should mention the unregistered prefix: " + ex.getMessage());
    }

    @Test
    void prepend_unregisteredPrefix_throwsIllegalArgument() {
        XML xml = XML.parse("<root><child/></root>");

        assertThrows(IllegalArgumentException.class,
            () -> xml.sel("/root").first().prepend("bad:item"));
    }

    @Test
    void before_unregisteredPrefix_throwsIllegalArgument() {
        XML xml = XML.parse("<root><item/></root>");

        assertThrows(IllegalArgumentException.class,
            () -> xml.sel("//item").first().before("bad:prev"));
    }

    @Test
    void after_unregisteredPrefix_throwsIllegalArgument() {
        XML xml = XML.parse("<root><item/></root>");

        assertThrows(IllegalArgumentException.class,
            () -> xml.sel("//item").first().after("bad:next"));
    }

    @Test
    void join_unregisteredPrefix_throwsIllegalArgument() {
        XML xml = XML.parse("<root></root>");

        assertThrows(IllegalArgumentException.class,
            () -> xml.sel("//item").data(List.of("A")).join("bad:item"));
    }

    // =====================================================
    // XML.create() — namespace-aware factory
    // =====================================================

    @Test
    void create_plainTag_works() {
        XML xml = XML.create("root");

        assertEquals(1, xml.sel("/root").size());
    }

    @Test
    void create_thenNamespace_appendNsElement() {
        XML xml = XML.create("root");
        xml.namespace("ns", "http://example.com");

        xml.sel("/root").first().append("ns:item");

        assertEquals(1, xml.sel("//ns:item").size());
        assertEquals("ns:item", xml.sel("//ns:item").first().name());
    }

    @Test
    void create_thenNamespace_nsAwareXPathWorks() {
        XML xml = XML.create("root");
        xml.namespace("ns", "http://example.com");

        xml.sel("/root").first().append("ns:item").text("created");

        assertEquals("created", xml.sel("//ns:item").text());
        String output = xml.toString();
        assertTrue(output.contains("ns:item"),
            "NS element should serialize correctly from XML.create(): " + output);
    }

    // =====================================================
    // Node navigation with namespaces
    // =====================================================

    @Test
    void name_onNamespacedElement_returnsQualifiedName() {
        XML xml = XML.parse(
            "<root xmlns:ns='http://example.com'><ns:item/></root>");
        xml.namespace("ns", "http://example.com");

        Node item = xml.sel("//ns:item").first();

        assertEquals("ns:item", item.name());
    }

    @Test
    void children_ofNamespacedParent_returnsAllChildren() {
        XML xml = XML.parse(
            "<root xmlns:ns='http://example.com'>"
            + "<ns:parent><ns:a/><ns:b/><plain/></ns:parent></root>");
        xml.namespace("ns", "http://example.com");

        Node parent = xml.sel("//ns:parent").first();
        Sel children = parent.children();

        assertEquals(3, children.size());
    }

    @Test
    void parent_ofNamespacedElement_returnsCorrectParent() {
        XML xml = XML.parse(
            "<root xmlns:ns='http://example.com'>"
            + "<ns:parent><ns:child/></ns:parent></root>");
        xml.namespace("ns", "http://example.com");

        Node child = xml.sel("//ns:child").first();

        assertEquals("ns:parent", child.parent().name());
    }

    // =====================================================
    // Data binding with namespaced elements
    // =====================================================

    @Test
    void join_onNamespacedSelection_enterUpdateExitWork() {
        XML xml = XML.parse(
            "<root xmlns:ns='http://example.com'>"
            + "<ns:item>A</ns:item><ns:item>B</ns:item><ns:item>C</ns:item></root>");
        xml.namespace("ns", "http://example.com");

        // 3 nodes, 2 data → 2 update + 1 exit
        xml.sel("//ns:item").data(List.of("X", "Y")).join("ns:item");

        assertEquals(2, xml.sel("//ns:item").size());
    }

    @Test
    void join_namespacedSelection_textWithApplied() {
        XML xml = XML.parse(
            "<root xmlns:ns='http://example.com'><ns:item/></root>");
        xml.namespace("ns", "http://example.com");

        xml.sel("//ns:item")
            .data(List.of("hello", "world"))
            .join("ns:item")
            .textWith(d -> d);

        assertEquals(2, xml.sel("//ns:item").size());
        assertEquals("hello", xml.sel("//ns:item").first().text());
    }

    @Test
    void join_namespacedSelection_enterNodesAreNsAware() {
        XML xml = XML.parse(
            "<root xmlns:ns='http://example.com'></root>");
        xml.namespace("ns", "http://example.com");

        xml.sel("//ns:item")
            .data(List.of("A", "B"))
            .join("ns:item")
            .textWith(d -> d);

        // All nodes created via enter — they must be NS-aware
        List<Node> items = xml.sel("//ns:item").list();
        assertEquals(2, items.size());
        assertEquals("ns:item", items.get(0).name());
        assertEquals("ns:item", items.get(1).name());
        assertEquals("A", items.get(0).text());
        assertEquals("B", items.get(1).text());
    }

    @Test
    void join_namespacedSelection_enterNodeSerializes() {
        XML xml = XML.parse(
            "<root xmlns:ns='http://example.com'></root>");
        xml.namespace("ns", "http://example.com");

        xml.sel("//ns:item")
            .data(List.of("A"))
            .join("ns:item");

        String output = xml.toString();
        assertTrue(output.contains("ns:item"),
            "Enter node should serialize with namespace prefix: " + output);
    }

    // =====================================================
    // Edge cases
    // =====================================================

    @Test
    void namespace_unregisteredPrefix_xpathReturnsEmpty() {
        XML xml = XML.parse(
            "<root xmlns:ns='http://example.com'><ns:item>A</ns:item></root>");
        // Deliberately not registering the namespace
        // JAXP XPath silently returns empty for unresolved prefixes (implementation-specific)
        assertEquals(0, xml.sel("//ns:item").size());
    }

    @Test
    void sel_wildcardMatchesNamespacedElements() {
        XML xml = XML.parse(
            "<root xmlns:ns='http://example.com'>"
            + "<ns:item>A</ns:item><plain>B</plain></root>");

        // XPath wildcard * matches all elements regardless of namespace
        Sel all = xml.sel("/root/*");
        assertEquals(2, all.size());
    }

    @Test
    void attr_onNamespacedElement_plainAttributeWorks() {
        XML xml = XML.parse(
            "<root xmlns:ns='http://example.com'><ns:item/></root>");
        xml.namespace("ns", "http://example.com");

        xml.sel("//ns:item").attr("class", "highlight");

        assertEquals("highlight", xml.sel("//ns:item").first().attr("class"));
        assertTrue(xml.toString().contains("class=\"highlight\""));
    }

    @Test
    void append_plainTag_noColonNoException() {
        // Plain tags without colon should never trigger the prefix check
        XML xml = XML.parse("<root/>");
        xml.sel("/root").first().append("item");
        assertEquals(1, xml.sel("//item").size());
    }
}
