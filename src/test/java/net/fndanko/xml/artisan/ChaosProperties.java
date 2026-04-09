package net.fndanko.xml.artisan;

import net.jqwik.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Aggressive property tests that push the library with nasty, malformed,
 * and extreme inputs. The goal is to verify the library never crashes with
 * unexpected exceptions (NPE, StackOverflow, ClassCast, etc.) — only the
 * documented exception types: RuntimeException, UncheckedIOException,
 * IllegalArgumentException.
 */
class ChaosProperties {

    // ========================================================================
    // Arbitraries: nasty input generators
    // ========================================================================

    /** Completely random strings — garbage, unicode, control chars, null bytes */
    @Provide
    Arbitrary<String> garbage() {
        return Arbitraries.strings()
                .all()                     // full unicode range
                .ofMinLength(0)
                .ofMaxLength(500);
    }

    /** Strings that look XML-ish but are broken in subtle ways */
    @Provide
    Arbitrary<String> almostXml() {
        return Arbitraries.oneOf(
                // Unclosed tags
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10)
                        .map(tag -> "<" + tag + ">content"),
                // Mismatched close tag
                Combinators.combine(
                        Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10),
                        Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10)
                ).as((open, close) -> "<" + open + ">text</" + close + ">"),
                // Double close
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10)
                        .map(tag -> "<" + tag + "/></" + tag + ">"),
                // Nested unclosed
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10)
                        .map(tag -> "<" + tag + "><" + tag + "></" + tag + ">"),
                // Unescaped special chars in content
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10)
                        .map(tag -> "<" + tag + ">a < b & c</" + tag + ">"),
                // Invalid tag name (starts with number)
                Arbitraries.just("<123>text</123>"),
                // Empty document
                Arbitraries.just(""),
                // Just whitespace
                Arbitraries.just("   \n\t  "),
                // Processing instruction only
                Arbitraries.just("<?xml version=\"1.0\"?>"),
                // Comment only
                Arbitraries.just("<!-- just a comment -->"),
                // Multiple roots
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(5)
                        .map(tag -> "<" + tag + "/><" + tag + "/>"),
                // Entity bomb (small)
                Arbitraries.just("<!DOCTYPE foo [<!ENTITY x \"xxxx\">]><foo>&x;&x;&x;</foo>")
        );
    }

    /** Unicode-heavy tag names and attribute values */
    @Provide
    Arbitrary<String> unicodeXml() {
        Arbitrary<String> unicodeText = Arbitraries.strings()
                .withCharRange('\u00C0', '\u00FF')   // Latin Extended
                .withCharRange('\u0400', '\u04FF')   // Cyrillic
                .withCharRange('\u4E00', '\u4FFF')   // CJK subset
                .withChars(' ', '\t', '\n')
                .ofMinLength(0)
                .ofMaxLength(100);

        // XML tag names must start with letter/underscore, can contain unicode letters
        Arbitrary<String> unicodeTag = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(1)
                .ofMaxLength(10);

        return Combinators.combine(unicodeTag, unicodeText)
                .as((tag, text) -> "<" + tag + ">" + escapeXml(text) + "</" + tag + ">");
    }

    /** Deep nesting (stress DOM tree depth) */
    @Provide
    Arbitrary<String> deeplyNested() {
        return Arbitraries.integers().between(10, 200).map(depth -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < depth; i++) {
                sb.append("<d").append(i).append(">");
            }
            sb.append("leaf");
            for (int i = depth - 1; i >= 0; i--) {
                sb.append("</d").append(i).append(">");
            }
            return sb.toString();
        });
    }

    /** Wide documents (many siblings) */
    @Provide
    Arbitrary<String> wideDocument() {
        return Arbitraries.integers().between(50, 500).map(width -> {
            StringBuilder sb = new StringBuilder("<root>");
            for (int i = 0; i < width; i++) {
                sb.append("<item id=\"").append(i).append("\">v").append(i).append("</item>");
            }
            sb.append("</root>");
            return sb.toString();
        });
    }

    /** Random XPath expressions — some valid, some garbage */
    @Provide
    Arbitrary<String> chaosXPath() {
        Arbitrary<String> tag = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10);
        return Arbitraries.oneOf(
                // Valid-ish
                tag.map(t -> "//" + t),
                tag.map(t -> "/*/" + t),
                tag.map(t -> "//" + t + "[1]"),
                tag.map(t -> "//" + t + "[@id]"),
                Arbitraries.just("//*"),
                Arbitraries.just("/*"),
                Arbitraries.just("."),
                Arbitraries.just(".."),
                // Exotic axes
                tag.map(t -> "//ancestor::" + t),
                tag.map(t -> "//following-sibling::" + t),
                tag.map(t -> "//preceding::" + t),
                // Malformed
                Arbitraries.just("///"),
                Arbitraries.just("[invalid"),
                Arbitraries.just(""),
                garbage().map(g -> "//" + g)
        );
    }

    /** Random attribute names — valid and invalid */
    @Provide
    Arbitrary<String> chaosAttrNames() {
        return Arbitraries.oneOf(
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20),
                Arbitraries.just(""),
                Arbitraries.just("class"),
                Arbitraries.just("xml:lang"),
                Arbitraries.strings().all().ofMinLength(1).ofMaxLength(10)
        );
    }

    /** Random attribute values — everything goes */
    @Provide
    Arbitrary<String> chaosAttrValues() {
        return Arbitraries.oneOf(
                Arbitraries.strings().all().ofMaxLength(200),
                Arbitraries.just(""),
                Arbitraries.just("   "),
                Arbitraries.just("<script>alert(1)</script>"),
                Arbitraries.just("a\"b'c&d<e>f"),
                Arbitraries.just("\u0000\u0001\u0002")
        );
    }

    // ========================================================================
    // Properties: parsing chaos
    // ========================================================================

    @Property
    void parse_garbage_neverThrowsUnexpected(@ForAll("garbage") String input) {
        assertOnlyDocumentedExceptions(() -> XML.parse(input));
    }

    @Property
    void parse_almostXml_neverThrowsUnexpected(@ForAll("almostXml") String input) {
        assertOnlyDocumentedExceptions(() -> XML.parse(input));
    }

    @Property
    void parse_unicodeXml_alwaysParses(@ForAll("unicodeXml") String input) {
        XML xml = XML.parse(input);
        assertNotNull(xml);
        assertFalse(xml.sel("/*").empty());
    }

    @Property
    void parse_deeplyNested_parses(@ForAll("deeplyNested") String input) {
        XML xml = XML.parse(input);
        assertTrue(xml.sel("//*").size() >= 10);
    }

    @Property
    void parse_wideDocument_parses(@ForAll("wideDocument") String input) {
        XML xml = XML.parse(input);
        assertTrue(xml.sel("//item").size() >= 50);
    }

    // ========================================================================
    // Properties: XPath chaos
    // ========================================================================

    @Property
    void sel_chaosXPath_neverThrowsUnexpected(@ForAll("chaosXPath") String xpath) {
        XML xml = XML.parse("<root><a><b id='1'/></a><c/></root>");
        assertOnlyDocumentedExceptions(() -> xml.sel(xpath));
    }

    @Property
    void count_chaosXPath_neverThrowsUnexpected(@ForAll("chaosXPath") String xpath) {
        XML xml = XML.parse("<root><a><b/></a></root>");
        assertOnlyDocumentedExceptions(() -> xml.count(xpath));
    }

    @Property
    void get_chaosXPath_neverThrowsUnexpected(@ForAll("chaosXPath") String xpath) {
        XML xml = XML.parse("<root><a>text</a></root>");
        assertOnlyDocumentedExceptions(() -> xml.get(xpath));
    }

    // ========================================================================
    // Properties: attribute/text chaos on valid documents
    // ========================================================================

    @Property
    void attr_chaosValues_neverCorrupts(
            @ForAll("chaosAttrNames") String name,
            @ForAll("chaosAttrValues") String value) {
        XML xml = XML.parse("<root><item/></root>");
        Node item = xml.sel("//item").first();

        try {
            item.attr(name, value);
            // If set succeeded, get should return something (may differ due to XML normalization)
            String result = item.attr(name);
            assertNotNull(result);
        } catch (RuntimeException e) {
            // DOM may reject invalid attribute names — that's fine
        }
    }

    @Property
    void text_chaosValues_neverCorrupts(@ForAll("chaosAttrValues") String value) {
        XML xml = XML.parse("<root><item/></root>");
        Node item = xml.sel("//item").first();

        try {
            item.text(value);
            String result = item.text();
            assertNotNull(result);
        } catch (RuntimeException e) {
            // DOM may reject certain characters
        }
    }

    // ========================================================================
    // Properties: serialization chaos
    // ========================================================================

    @Property
    void deeplyNested_roundTrips(@ForAll("deeplyNested") String input) {
        XML xml = XML.parse(input);
        String serialized = xml.toString();
        assertDoesNotThrow(() -> XML.parse(serialized));
    }

    @Property
    void wideDocument_roundTrips(@ForAll("wideDocument") String input) {
        XML xml = XML.parse(input);
        String serialized = xml.toString();
        XML reparsed = XML.parse(serialized);
        assertEquals(xml.sel("//item").size(), reparsed.sel("//item").size());
    }

    @Property
    void unicodeXml_roundTrips(@ForAll("unicodeXml") String input) {
        XML xml = XML.parse(input);
        String serialized = xml.toString();
        XML reparsed = XML.parse(serialized);
        // Root tag name should survive
        assertEquals(
                xml.sel("/*").first().name(),
                reparsed.sel("/*").first().name());
    }

    // ========================================================================
    // Properties: operations on deep/wide documents
    // ========================================================================

    @Property
    void deeplyNested_selAndNavigate(@ForAll("deeplyNested") String input) {
        XML xml = XML.parse(input);
        // Navigate all the way down and back up
        Sel all = xml.sel("//*");
        for (Node node : all) {
            assertDoesNotThrow(() -> node.parent());
            assertDoesNotThrow(() -> node.children());
            assertDoesNotThrow(() -> node.name());
        }
    }

    @Property
    void wideDocument_joinOnSubset(@ForAll("wideDocument") String input) {
        XML xml = XML.parse(input);
        int before = xml.sel("//item").size();

        // Join with smaller data — should reduce count
        xml.sel("//item").data(java.util.List.of("A", "B", "C")).join("item");

        assertEquals(3, xml.sel("//item").size());
        assertTrue(before >= 50);
    }

    @Property
    void wideDocument_removeAll(@ForAll("wideDocument") String input) {
        XML xml = XML.parse(input);
        xml.sel("//item").remove();
        assertEquals(0, xml.sel("//item").size());
        // Root should still exist
        assertFalse(xml.sel("/*").empty());
    }

    // ========================================================================
    // Properties: create with chaos tag names
    // ========================================================================

    @Property
    void create_chaosTagName_neverThrowsUnexpected(@ForAll("chaosAttrNames") String tagName) {
        assertOnlyDocumentedExceptions(() -> XML.create(tagName));
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private static void assertOnlyDocumentedExceptions(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            if (isDocumentedException(e)) return;
            fail("Operation threw unexpected RuntimeException: " + e.getClass().getName() + ": " + e.getMessage());
        } catch (Error e) {
            fail("Operation threw an Error: " + e.getClass().getName() + ": " + e.getMessage());
        } catch (Throwable t) {
            fail("Operation threw unexpected exception: " + t.getClass().getName() + ": " + t.getMessage());
        }
    }

    private static boolean isDocumentedException(RuntimeException e) {
        // UncheckedIOException (missing file / IO errors)
        if (e instanceof java.io.UncheckedIOException) return true;
        // IllegalArgumentException (invalid tag names, etc.)
        if (e instanceof IllegalArgumentException) return true;
        // DOMException (invalid attribute names, DOM constraint violations)
        if (e instanceof org.w3c.dom.DOMException) return true;
        // RuntimeException wrapping SAXException (malformed XML)
        if (e.getCause() instanceof org.xml.sax.SAXException) return true;
        // RuntimeException wrapping XPathExpressionException (bad XPath)
        if (e.getCause() instanceof javax.xml.xpath.XPathExpressionException) return true;
        // RuntimeException wrapping DOMException (invalid chars in tag/attr names)
        if (e.getCause() instanceof org.w3c.dom.DOMException) return true;
        // RuntimeException with "Malformed XML" message (library's own wrapping)
        if (e.getMessage() != null && e.getMessage().startsWith("Malformed XML")) return true;
        return false;
    }

    private static String escapeXml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;");
    }
}
