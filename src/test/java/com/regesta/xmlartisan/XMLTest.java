package net.fndanko.xmlartisan;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class XMLTest {
    private static XML xml;

    @BeforeAll
    public static void init() throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(XMLTest.class.getResourceAsStream("/test1.xml"));
        doc.getDocumentElement().normalize();

        xml = XML.from(doc);
    }

    @Test
    public void testGetSingleValue() {
        assertEquals("Laura", xml.getValue("/persons/person/text()"));
        assertEquals("Michele", xml.getValue("/persons/person[2]/text()"));
        assertEquals("Laura", xml.getValue("//person/text()"));
    }
}
