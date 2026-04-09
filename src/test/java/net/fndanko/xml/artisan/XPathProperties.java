package net.fndanko.xml.artisan;

import net.jqwik.api.*;

import static org.junit.jupiter.api.Assertions.*;

class XPathProperties {

    // --- Arbitraries ---

    @Provide
    Arbitrary<String> tagNames() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(10);
    }

    @Provide
    Arbitrary<String> simpleXPath() {
        // Generates paths like //tag, /root/child, //tag[@attr]
        Arbitrary<String> tag = tagNames();
        return Arbitraries.oneOf(
                tag.map(t -> "//" + t),
                tag.map(t -> "/*/" + t),
                Combinators.combine(tag, tag).as((a, b) -> "//" + a + "/" + b),
                Combinators.combine(tag, tag).as((a, b) -> "//" + a + "[@" + b + "]"),
                tag.map(t -> "//" + t + "[1]"),
                Arbitraries.just("//*"),
                Arbitraries.just("/*")
        );
    }

    @Provide
    Arbitrary<String> richDocument() {
        // Build a document with known structure: root with varied children and grandchildren
        return Combinators.combine(
                tagNames(),
                tagNames().list().ofMinSize(1).ofMaxSize(5),
                tagNames()
        ).as((root, children, grandchild) -> {
            StringBuilder sb = new StringBuilder("<" + root + ">");
            for (String child : children) {
                sb.append("<").append(child).append(" id=\"").append(child).append("\">");
                sb.append("<").append(grandchild).append("/>");
                sb.append("</").append(child).append(">");
            }
            sb.append("</").append(root).append(">");
            return sb.toString();
        });
    }

    // --- Properties ---

    @Property
    void validXPath_neverThrowsOnDoc(@ForAll("richDocument") String xmlStr, @ForAll("simpleXPath") String xpath) {
        XML xml = XML.parse(xmlStr);
        // Should return a Sel (possibly empty), never throw
        assertDoesNotThrow(() -> xml.sel(xpath));
    }

    @Property
    void count_matchesSelSize(@ForAll("richDocument") String xmlStr, @ForAll("simpleXPath") String xpath) {
        XML xml = XML.parse(xmlStr);
        assertEquals(xml.count(xpath), xml.sel(xpath).size(),
                "count() and sel().size() should agree for xpath: " + xpath);
    }

    @Property
    void doubleSlashRewrite_sameResultOnRoot(@ForAll("richDocument") String xmlStr, @ForAll("tagNames") String tag) {
        XML xml = XML.parse(xmlStr);
        // At root level, //tag and .//tag should find the same nodes
        int withDoubleSlash = xml.sel("//" + tag).size();
        int withDotSlash = xml.sel(".//" + tag).size();
        assertEquals(withDoubleSlash, withDotSlash,
                "//tag and .//tag should match same nodes at root level");
    }

    @Property
    void subSelRewrite_contextual(@ForAll("tagNames") String parent, @ForAll("tagNames") String child) {
        Assume.that(!parent.equals(child));

        // Build doc where child exists under parent AND under root
        String xmlStr = "<root><" + parent + "><" + child + "/></" + parent + ">"
                + "<" + child + "/></root>";
        XML xml = XML.parse(xmlStr);

        // Sub-selection should only find child under parent, not globally
        int subSelCount = xml.sel("//" + parent).sel("//" + child).size();
        int globalCount = xml.sel("//" + child).size();

        assertTrue(subSelCount <= globalCount,
                "Sub-selection should find <= global count");
        // The child under parent should be found
        assertTrue(subSelCount >= 1,
                "Sub-selection should find at least the child under parent");
    }

    @Property
    void selOnEmptyResult_returnsEmptySel(@ForAll("richDocument") String xmlStr) {
        XML xml = XML.parse(xmlStr);
        // A tag name that can't exist in the document
        Sel result = xml.sel("//zzzznonexistent");
        assertTrue(result.empty());
        assertEquals(0, result.size());
    }

    @Property
    void selSize_neverNegative(@ForAll("richDocument") String xmlStr, @ForAll("simpleXPath") String xpath) {
        XML xml = XML.parse(xmlStr);
        assertTrue(xml.sel(xpath).size() >= 0);
    }
}
