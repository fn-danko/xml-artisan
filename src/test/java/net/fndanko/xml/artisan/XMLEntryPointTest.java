package net.fndanko.xml.artisan;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;

class XMLEntryPointTest {

    // --- parse ---

    @Test
    void parse_validXmlString_createsDocument() {
        // Arrange
        String input = "<root><child>hello</child></root>";

        // Act
        XML xml = XML.parse(input);

        // Assert
        assertNotNull(xml);
        assertEquals("hello", xml.get("//child/text()"));
    }

    // --- from ---

    @Test
    void from_validFile_loadsDocument(@TempDir Path tempDir) throws IOException {
        // Arrange
        Path file = tempDir.resolve("test.xml");
        Files.writeString(file, "<root><item id=\"1\">value</item></root>");

        // Act
        XML xml = XML.from(file);

        // Assert
        assertEquals("value", xml.get("//item/text()"));
        assertEquals("1", xml.get("//item/@id"));
    }

    // --- create ---

    @Test
    void create_rootTag_createsEmptyDocument() {
        // Arrange / Act
        XML xml = XML.create("root");

        // Assert
        assertNotNull(xml);
        assertTrue(xml.toString().contains("<root"));
    }

    // --- wrap ---

    @Test
    void wrap_existingDocument_preservesContent() throws Exception {
        // Arrange
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new java.io.ByteArrayInputStream(
                "<root><child>hello</child></root>".getBytes(StandardCharsets.UTF_8)));

        // Act
        XML xml = XML.wrap(doc);

        // Assert
        assertNotNull(xml);
        assertEquals("hello", xml.get("//child/text()"));
    }

    @Test
    void wrap_existingDocument_selectionsWork() throws Exception {
        // Arrange
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new java.io.ByteArrayInputStream(
                "<root><item id=\"1\"/><item id=\"2\"/></root>".getBytes(StandardCharsets.UTF_8)));

        // Act
        XML xml = XML.wrap(doc);

        // Assert
        assertEquals(2, xml.sel("//item").size());
        assertEquals("1", xml.sel("//item").first().attr("id"));
    }

    @Test
    void wrap_nullDocument_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> XML.wrap(null));
    }

    // --- get attribute ---

    @Test
    void get_existingAttribute_returnsValue() {
        // Arrange
        XML xml = XML.parse("<root><item lang=\"it\"/></root>");

        // Act
        String value = xml.get("//item/@lang");

        // Assert
        assertEquals("it", value);
    }

    // --- get text ---

    @Test
    void get_existingText_returnsValue() {
        // Arrange
        XML xml = XML.parse("<root><msg>ciao</msg></root>");

        // Act
        String value = xml.get("//msg/text()");

        // Assert
        assertEquals("ciao", value);
    }

    // --- set attribute ---

    @Test
    void set_attribute_updatesValue() {
        // Arrange
        XML xml = XML.parse("<root><item lang=\"en\"/></root>");

        // Act
        xml.set("//item/@lang", "it");

        // Assert
        assertEquals("it", xml.get("//item/@lang"));
    }

    // --- set text ---

    @Test
    void set_text_updatesValue() {
        // Arrange
        XML xml = XML.parse("<root><msg>old</msg></root>");

        // Act
        xml.set("//msg/text()", "new");

        // Assert
        assertEquals("new", xml.get("//msg/text()"));
    }

    // --- get no match ---

    @Test
    void get_noMatch_returnsEmptyString() {
        // Arrange
        XML xml = XML.parse("<root/>");

        // Act
        String value = xml.get("//missing/@attr");

        // Assert
        assertEquals("", value);
    }

    // --- set no match ---

    @Test
    void set_noMatch_isNoOp() {
        // Arrange
        XML xml = XML.parse("<root><item>original</item></root>");
        String before = xml.toString();

        // Act
        xml.set("//nonexistent/@attr", "value");

        // Assert
        assertEquals(before, xml.toString());
    }

    // --- count ---

    @Test
    void count_multipleMatches_returnsCorrectCount() {
        // Arrange
        XML xml = XML.parse("<root><item/><item/><item/></root>");

        // Act
        int result = xml.count("//item");

        // Assert
        assertEquals(3, result);
    }

    @Test
    void count_noMatch_returnsZero() {
        // Arrange
        XML xml = XML.parse("<root/>");

        // Act
        int result = xml.count("//nonexistent");

        // Assert
        assertEquals(0, result);
    }

    @Test
    void count_singleMatch_returnsOne() {
        // Arrange
        XML xml = XML.parse("<root><only/></root>");

        // Act
        int result = xml.count("//only");

        // Assert
        assertEquals(1, result);
    }

    @Test
    void count_withPredicate_countsFilteredNodes() {
        // Arrange
        XML xml = XML.parse("<root><item lang=\"it\"/><item lang=\"en\"/><item lang=\"it\"/></root>");

        // Act
        int result = xml.count("//item[@lang='it']");

        // Assert
        assertEquals(2, result);
    }

    // --- namespace ---

    @Test
    void namespace_registered_usedInQuery() {
        // Arrange
        String input = "<root xmlns:ns=\"http://example.com\">"
                + "<ns:item>found</ns:item></root>";
        XML xml = XML.parse(input);

        // Act
        xml.namespace("ns", "http://example.com");
        String value = xml.get("//ns:item/text()");

        // Assert
        assertEquals("found", value);
    }

    @Test
    void namespace_multiple_allUsableInQuery() {
        // Arrange
        String input = "<root xmlns:a=\"http://a.com\" xmlns:b=\"http://b.com\">"
                + "<a:x>alpha</a:x><b:y>beta</b:y></root>";
        XML xml = XML.parse(input)
                .namespace("a", "http://a.com")
                .namespace("b", "http://b.com");

        // Act / Assert
        assertEquals("alpha", xml.get("//a:x/text()"));
        assertEquals("beta", xml.get("//b:y/text()"));
    }

    // --- from nonexistent file ---

    @Test
    void from_nonexistentFile_throwsUncheckedIOException() {
        assertThrows(UncheckedIOException.class, () -> XML.from(Path.of("/nonexistent/path.xml")));
    }

    // --- parse malformed XML ---

    @Test
    void parse_malformedXml_throwsParseException() {
        assertThrows(ParseException.class, () -> XML.parse("this is not xml at all <><>"));
    }

    @Test
    void parse_malformedXml_messageContainsDetail() {
        ParseException ex = assertThrows(ParseException.class, () -> XML.parse("<unclosed>"));
        assertTrue(ex.getMessage().startsWith("Failed to parse XML:"));
    }

    @Test
    void create_invalidTagName_throwsInvalidNameException() {
        assertThrows(InvalidNameException.class, () -> XML.create("123invalid"));
    }

    @Test
    void create_invalidTagName_messageContainsName() {
        InvalidNameException ex = assertThrows(InvalidNameException.class, () -> XML.create("123bad"));
        assertTrue(ex.getMessage().contains("123bad"));
    }

    @Test
    void allCustomExceptions_extendXmlArtisanException() {
        // ParseException
        assertThrows(XmlArtisanException.class, () -> XML.parse("<bad"));
        // XPathException
        assertThrows(XmlArtisanException.class, () -> XML.parse("<r/>").sel("[invalid"));
        // InvalidNameException
        assertThrows(XmlArtisanException.class, () -> XML.create("123bad"));
    }
}
