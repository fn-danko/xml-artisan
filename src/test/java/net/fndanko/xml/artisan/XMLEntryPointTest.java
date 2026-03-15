package net.fndanko.xml.artisan;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
    void parse_malformedXml_throwsRuntimeException() {
        assertThrows(RuntimeException.class, () -> XML.parse("this is not xml at all <><>"));
    }
}
