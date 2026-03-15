package net.fndanko.xml.artisan;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SelTransformTest {

    // --- attr transform ---

    @Test
    void attr_transformFunction_appliedToEachNode() {
        // Arrange
        XML xml = XML.parse("<root><item name=\"alice\"/><item name=\"bob\"/></root>");

        // Act
        xml.sel("//item").attr("name", String::toUpperCase);

        // Assert
        assertEquals("ALICE", xml.sel("//item[1]").attr("name"));
        assertEquals("BOB", xml.sel("//item[2]").attr("name"));
    }

    @Test
    void attr_transformNonexistentAttribute_functionReceivesEmptyString() {
        // Arrange
        XML xml = XML.parse("<root><item/></root>");

        // Act
        xml.sel("//item").attr("missing", old -> {
            assertEquals("", old);
            return "created";
        });

        // Assert
        assertEquals("created", xml.sel("//item").attr("missing"));
    }

    // --- text transform ---

    @Test
    void text_transformFunction_appliedToEachNode() {
        // Arrange
        XML xml = XML.parse("<root><p>hello</p><p>world</p></root>");

        // Act
        xml.sel("//p").text(t -> t + "!");

        // Assert
        assertEquals("hello!", xml.sel("//p[1]").text());
        assertEquals("world!", xml.sel("//p[2]").text());
    }

    @Test
    void text_transformEmptyText_functionReceivesEmptyString() {
        // Arrange
        XML xml = XML.parse("<root><empty/></root>");

        // Act
        xml.sel("//empty").text(old -> {
            assertEquals("", old);
            return "filled";
        });

        // Assert
        assertEquals("filled", xml.sel("//empty").text());
    }

    // --- transform runs on every node ---

    @Test
    void attr_transformFunction_executedOnEveryNode() {
        // Arrange
        XML xml = XML.parse("<root><n v=\"1\"/><n v=\"2\"/><n v=\"3\"/></root>");
        int[] callCount = {0};

        // Act
        xml.sel("//n").attr("v", old -> {
            callCount[0]++;
            return String.valueOf(Integer.parseInt(old) * 10);
        });

        // Assert
        assertEquals(3, callCount[0]);
        assertEquals("10", xml.sel("//n[1]").attr("v"));
        assertEquals("20", xml.sel("//n[2]").attr("v"));
        assertEquals("30", xml.sel("//n[3]").attr("v"));
    }
}
