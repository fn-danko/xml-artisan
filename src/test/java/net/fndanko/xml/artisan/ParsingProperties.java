package net.fndanko.xml.artisan;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import static org.junit.jupiter.api.Assertions.*;

class ParsingProperties {

    // --- Arbitraries ---

    @Provide
    Arbitrary<String> tagNames() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(20);
    }

    @Provide
    Arbitrary<String> safeText() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .withChars(' ', '\t', '\n')
                .ofMaxLength(100);
    }

    @Provide
    Arbitrary<String> validXml() {
        return Combinators.combine(tagNames(), safeText(), childElements(0))
                .as((tag, text, children) ->
                        "<" + tag + ">" + escapeXml(text) + children + "</" + tag + ">");
    }

    @Provide
    Arbitrary<String> validXmlWithAttrs() {
        Arbitrary<String> attrName = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10);
        Arbitrary<String> attrVal = safeText().map(ParsingProperties::escapeXmlAttr);

        return Combinators.combine(tagNames(), attrName, attrVal, safeText())
                .as((tag, aName, aVal, text) ->
                        "<" + tag + " " + aName + "=\"" + aVal + "\">"
                                + escapeXml(text) + "</" + tag + ">");
    }

    @Provide
    Arbitrary<String> malformedXml() {
        return validXml().map(xml -> {
            // Apply a random mutation
            return Arbitraries.of(
                    xml.replaceFirst("</\\w+>$", ""),          // unclosed tag
                    xml.replaceFirst(">", ""),                  // missing >
                    "<123>" + xml + "</123>",                   // digit-starting tag
                    xml.replace("<", "<<"),                     // doubled <
                    "<>"                                        // empty tag name
            ).sample();
        });
    }

    private Arbitrary<String> childElements(int depth) {
        if (depth >= 2) {
            return Arbitraries.just("");
        }
        Arbitrary<String> child = Combinators.combine(tagNames(), safeText(), childElements(depth + 1))
                .as((tag, text, nested) ->
                        "<" + tag + ">" + escapeXml(text) + nested + "</" + tag + ">");

        return Arbitraries.integers().between(0, 3).flatMap(count -> {
            if (count == 0) return Arbitraries.just("");
            return child.list().ofSize(count).map(list -> String.join("", list));
        });
    }

    // --- Properties ---

    @Property
    void validXml_alwaysParses(@ForAll("validXml") String xmlStr) {
        assertDoesNotThrow(() -> XML.parse(xmlStr));
    }

    @Property
    void validXmlWithAttrs_alwaysParses(@ForAll("validXmlWithAttrs") String xmlStr) {
        assertDoesNotThrow(() -> XML.parse(xmlStr));
    }

    @Property
    void malformedXml_onlyThrowsRuntimeException(@ForAll("malformedXml") String xmlStr) {
        try {
            XML.parse(xmlStr);
            // Parsing succeeded — that's fine, some mutations are still valid
        } catch (RuntimeException e) {
            // Expected — wrapping SAXException or similar
        } catch (Exception e) {
            fail("XML.parse() threw unexpected checked exception: " + e.getClass().getName());
        }
    }

    @Property
    void parseThenToString_containsRootTag(@ForAll("validXml") String xmlStr) {
        XML xml = XML.parse(xmlStr);
        String serialized = xml.toString();
        // Extract root tag name from the input
        String rootTag = xmlStr.substring(1, xmlStr.indexOf('>'));
        assertTrue(serialized.contains(rootTag),
                "Serialized XML should contain root tag '" + rootTag + "'");
    }

    @Property
    void parsedXml_hasExactlyOneRootElement(@ForAll("validXml") String xmlStr) {
        XML xml = XML.parse(xmlStr);
        assertEquals(1, xml.sel("/*").size());
    }

    @Property
    void parsedXml_rootSelNeverEmpty(@ForAll("validXml") String xmlStr) {
        XML xml = XML.parse(xmlStr);
        assertFalse(xml.sel("/*").empty());
    }

    // --- Helpers ---

    private static String escapeXml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;");
    }

    private static String escapeXmlAttr(String text) {
        return escapeXml(text).replace("\"", "&quot;");
    }
}
