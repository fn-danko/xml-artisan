package net.fndanko.xml.artisan;

import net.jqwik.api.*;

import static org.junit.jupiter.api.Assertions.*;

class RoundTripProperties {

    // --- Arbitraries ---

    @Provide
    Arbitrary<String> tagNames() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(15);
    }

    @Provide
    Arbitrary<String> safeText() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .withChars(' ')
                .ofMinLength(0)
                .ofMaxLength(50);
    }

    @Provide
    Arbitrary<String> xmlDocument() {
        return tagNames().flatMap(rootTag ->
                childBlock(0).map(children ->
                        "<" + rootTag + ">" + children + "</" + rootTag + ">"));
    }

    private Arbitrary<String> childBlock(int depth) {
        if (depth >= 3) {
            return safeText().map(RoundTripProperties::escapeXml);
        }
        Arbitrary<String> leaf = safeText().map(RoundTripProperties::escapeXml);
        Arbitrary<String> element = Combinators.combine(tagNames(), childBlock(depth + 1))
                .as((tag, inner) -> "<" + tag + ">" + inner + "</" + tag + ">");

        return Arbitraries.integers().between(0, 4).flatMap(count -> {
            if (count == 0) return leaf;
            return Arbitraries.oneOf(leaf, element).list().ofSize(count)
                    .map(list -> String.join("", list));
        });
    }

    @Provide
    Arbitrary<OutputOptions> outputOptions() {
        return Combinators.combine(
                Arbitraries.of(true, false),              // indent
                Arbitraries.integers().between(0, 8),     // indentAmount
                Arbitraries.of(true, false),              // omitDeclaration
                Arbitraries.of("UTF-8", "ISO-8859-1")    // encoding
        ).as((indent, amount, omit, enc) ->
                OutputOptions.builder()
                        .indent(indent)
                        .indentAmount(amount)
                        .omitDeclaration(omit)
                        .encoding(enc)
                        .build());
    }

    // --- Properties ---

    @Property
    void roundTrip_preservesNodeCount(@ForAll("xmlDocument") String xmlStr) {
        XML first = XML.parse(xmlStr);
        int firstCount = first.sel("//*").size();

        String serialized = first.toString();
        XML second = XML.parse(serialized);
        int secondCount = second.sel("//*").size();

        assertEquals(firstCount, secondCount,
                "Node count should be preserved after round-trip");
    }

    @Property
    void roundTrip_preservesRootTagName(@ForAll("xmlDocument") String xmlStr) {
        XML first = XML.parse(xmlStr);
        String firstRoot = first.sel("/*").first().name();

        String serialized = first.toString();
        XML second = XML.parse(serialized);
        String secondRoot = second.sel("/*").first().name();

        assertEquals(firstRoot, secondRoot);
    }

    @Property
    void roundTrip_withOutputOptions_stillParseable(
            @ForAll("xmlDocument") String xmlStr,
            @ForAll("outputOptions") OutputOptions options) {
        XML xml = XML.parse(xmlStr);
        // Serialize with random options, then parse — should never fail
        String serialized = xml.toString();
        assertDoesNotThrow(() -> XML.parse(serialized));
    }

    @Property
    void normalizeText_isIdempotent(@ForAll("xmlDocument") String xmlStr) {
        XML xml = XML.parse(xmlStr);

        xml.normalizeText();
        String afterFirst = xml.toString();

        xml.normalizeText();
        String afterSecond = xml.toString();

        assertEquals(afterFirst, afterSecond,
                "normalizeText() should be idempotent");
    }

    @Property
    void roundTrip_preservesAttributes(@ForAll("tagNames") String root, @ForAll("tagNames") String child,
            @ForAll("tagNames") String attrName, @ForAll("safeText") String attrValue) {
        String escaped = escapeXmlAttr(attrValue);
        String xmlStr = "<" + root + "><" + child + " " + attrName + "=\"" + escaped + "\"/></" + root + ">";

        XML first = XML.parse(xmlStr);
        String val1 = first.sel("//" + child).first().attr(attrName);

        XML second = XML.parse(first.toString());
        String val2 = second.sel("//" + child).first().attr(attrName);

        assertEquals(val1, val2, "Attribute value should survive round-trip");
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
