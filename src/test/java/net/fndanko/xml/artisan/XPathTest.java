package net.fndanko.xml.artisan;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class XPathTest {

    // --- XPath assoluto da xml.sel() cerca dalla radice ---

    @Test
    void sel_absoluteXPath_searchesFromRoot() {
        // Arrange
        XML xml = XML.parse("<root><a><b>deep</b></a></root>");

        // Act
        Sel sel = xml.sel("/root/a/b");

        // Assert
        assertEquals(1, sel.size());
        assertEquals("deep", sel.text());
    }

    // --- XPath relativo in sotto-selezione cerca dal contesto ---

    @Test
    void sel_relativeXPathInSubSelection_searchesFromContext() {
        // Arrange
        XML xml = XML.parse(
                "<root><parent><child>inner</child></parent><child>outer</child></root>");
        Sel parent = xml.sel("//parent");

        // Act
        Sel child = parent.sel("child");

        // Assert
        assertEquals(1, child.size());
        assertEquals("inner", child.text());
    }

    // --- "//" in sotto-selezione trattato come ".//" (relativo) ---

    @Test
    void sel_doubleSlashInSubSelection_treatedAsRelative() {
        // Arrange
        XML xml = XML.parse(
                "<root><a><x>inside-a</x></a><b><x>inside-b</x></b></root>");
        Sel a = xml.sel("//a");

        // Act
        Sel result = a.sel("//x");

        // Assert
        assertEquals(1, result.size());
        assertEquals("inside-a", result.text());
    }

    // --- ".//" esplicito funziona in sotto-selezione ---

    @Test
    void sel_explicitDotDoubleSlash_worksInSubSelection() {
        // Arrange
        XML xml = XML.parse(
                "<root><a><deep><x>found</x></deep></a><x>other</x></root>");
        Sel a = xml.sel("//a");

        // Act
        Sel result = a.sel(".//x");

        // Assert
        assertEquals(1, result.size());
        assertEquals("found", result.text());
    }

    // --- XPath con predicati funziona ---

    @Test
    void sel_xpathWithPredicate_filtersCorrectly() {
        // Arrange
        XML xml = XML.parse(
                "<root><item id=\"1\">first</item><item id=\"2\">second</item></root>");

        // Act
        Sel sel = xml.sel("//item[@id='2']");

        // Assert
        assertEquals(1, sel.size());
        assertEquals("second", sel.text());
    }

    // --- XPath per attributi (@attr) funziona ---

    @Test
    void sel_xpathForAttribute_returnsAttributeValue() {
        // Arrange
        XML xml = XML.parse("<root><item lang=\"it\" type=\"doc\"/></root>");

        // Act
        String lang = xml.get("//item/@lang");
        String type = xml.get("//item/@type");

        // Assert
        assertEquals("it", lang);
        assertEquals("doc", type);
    }

    // --- XPath con namespace funziona ---

    @Test
    void sel_xpathWithNamespace_resolvesCorrectly() {
        // Arrange
        String input = "<root xmlns:ns=\"http://example.com\">"
                + "<ns:item>namespaced</ns:item></root>";
        XML xml = XML.parse(input).namespace("ns", "http://example.com");

        // Act
        Sel sel = xml.sel("//ns:item");

        // Assert
        assertEquals(1, sel.size());
        assertEquals("namespaced", sel.text());
    }

    // --- XPath malformato produce eccezione unchecked ---

    @Test
    void sel_malformedXPath_throwsUncheckedException() {
        // Arrange
        XML xml = XML.parse("<root><item/></root>");

        // Act / Assert
        assertThrows(RuntimeException.class, () -> xml.sel("///[invalid!!xpath"));
    }
}
