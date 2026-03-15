package net.fndanko.xml.artisan;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SerializationTest {

    // --- toString produce XML con dichiarazione ---

    @Test
    void toString_anyDocument_startsWithXmlDeclaration() {
        // Arrange
        XML xml = XML.parse("<root><item/></root>");

        // Act
        String result = xml.toString();

        // Assert
        assertTrue(result.startsWith("<?xml"), "Expected XML declaration at start");
    }

    // --- toFragment produce XML senza dichiarazione ---

    @Test
    void toFragment_anyDocument_doesNotContainDeclaration() {
        // Arrange
        XML xml = XML.parse("<root><item/></root>");

        // Act
        String result = xml.toFragment();

        // Assert
        assertFalse(result.contains("<?xml"), "Fragment should not contain XML declaration");
        assertTrue(result.contains("<root"), "Fragment should contain the root element");
    }

    // --- writeTo(path) scrive su file ---

    @Test
    void writeTo_path_writesFileReadableByFrom(@TempDir Path tempDir) {
        // Arrange
        XML xml = XML.parse("<root><msg>hello</msg></root>");
        Path file = tempDir.resolve("output.xml");

        // Act
        xml.writeTo(file);

        // Assert
        XML reloaded = XML.from(file);
        assertEquals("hello", reloaded.get("//msg/text()"));
    }

    // --- writeTo(path, options) con indentazione ---

    @Test
    void writeTo_withIndentation_producesIndentedOutput(@TempDir Path tempDir) throws IOException {
        // Arrange
        XML xml = XML.parse("<root><child>text</child></root>");
        Path file = tempDir.resolve("indented.xml");
        OutputOptions options = OutputOptions.builder()
                .indent(true)
                .indentAmount(4)
                .build();

        // Act
        xml.writeTo(file, options);

        // Assert
        String content = Files.readString(file);
        assertTrue(content.contains("\n"), "Indented output should contain newlines");
    }

    // --- writeTo(path, options) con encoding specifico ---

    @Test
    void writeTo_withEncoding_usesSpecifiedEncoding(@TempDir Path tempDir) throws IOException {
        // Arrange
        XML xml = XML.parse("<root><msg>hello</msg></root>");
        Path file = tempDir.resolve("encoded.xml");
        OutputOptions options = OutputOptions.builder()
                .encoding("ISO-8859-1")
                .build();

        // Act
        xml.writeTo(file, options);

        // Assert
        String content = Files.readString(file);
        assertTrue(content.contains("ISO-8859-1"),
                "Output should reference the specified encoding");
    }

    // --- writeTo(path, options) con dichiarazione omessa ---

    @Test
    void writeTo_omitDeclaration_noXmlDeclarationInFile(@TempDir Path tempDir) throws IOException {
        // Arrange
        XML xml = XML.parse("<root><item/></root>");
        Path file = tempDir.resolve("no-decl.xml");
        OutputOptions options = OutputOptions.builder()
                .omitDeclaration(true)
                .build();

        // Act
        xml.writeTo(file, options);

        // Assert
        String content = Files.readString(file);
        assertFalse(content.contains("<?xml"),
                "Output should not contain XML declaration when omitted");
        assertTrue(content.contains("<root"), "Output should contain the root element");
    }

    // --- Documento modificato si serializza con le modifiche ---

    @Test
    void toString_afterModification_reflectsChanges() {
        // Arrange
        XML xml = XML.parse("<root><msg>original</msg></root>");

        // Act
        xml.set("//msg/text()", "modified");
        String result = xml.toString();

        // Assert
        assertTrue(result.contains("modified"), "Serialized output should contain the new value");
        assertFalse(result.contains("original"),
                "Serialized output should not contain the old value");
    }
}
