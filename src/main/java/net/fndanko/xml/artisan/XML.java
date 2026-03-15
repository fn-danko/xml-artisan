package net.fndanko.xml.artisan;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class XML {

    private final Document document;
    private final XPath xpath;
    private final Map<String, String> namespaces = new HashMap<>();

    private final NamespaceContext nsContext = new NamespaceContext() {
        @Override
        public String getNamespaceURI(String prefix) {
            return namespaces.getOrDefault(prefix, javax.xml.XMLConstants.NULL_NS_URI);
        }
        @Override
        public String getPrefix(String namespaceURI) {
            for (Map.Entry<String, String> e : namespaces.entrySet()) {
                if (e.getValue().equals(namespaceURI)) return e.getKey();
            }
            return null;
        }
        @Override
        public Iterator<String> getPrefixes(String namespaceURI) {
            return Collections.emptyIterator();
        }
    };

    private XML(Document document) {
        this.document = document;
        this.xpath = XPathFactory.newInstance().newXPath();
    }

    // --- Factory methods ---

    public static XML from(Path path) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(path.toFile());
            doc.getDocumentElement().normalize();
            return new XML(doc);
        } catch (java.io.IOException e) {
            throw new UncheckedIOException(e);
        } catch (org.xml.sax.SAXException e) {
            throw new RuntimeException("Malformed XML: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static XML parse(String xmlString) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8)));
            doc.getDocumentElement().normalize();
            return new XML(doc);
        } catch (org.xml.sax.SAXException e) {
            throw new RuntimeException("Malformed XML: " + e.getMessage(), e);
        } catch (java.io.IOException e) {
            throw new UncheckedIOException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static XML create(String rootTagName) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();
            doc.appendChild(doc.createElement(rootTagName));
            return new XML(doc);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // --- Lettura e scrittura puntuale ---

    public String get(String xpathExpr) {
        try {
            XPathExpression expr = xpath.compile(xpathExpr);
            String result = (String) expr.evaluate(document, XPathConstants.STRING);
            return result != null ? result : "";
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    public void set(String xpathExpr, String value) {
        try {
            XPathExpression expr = xpath.compile(xpathExpr);
            org.w3c.dom.Node node = (org.w3c.dom.Node) expr.evaluate(document, XPathConstants.NODE);
            if (node != null) {
                node.setNodeValue(value);
            }
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    public int count(String xpathExpr) {
        try {
            XPathExpression expr = xpath.compile(xpathExpr);
            NodeList nodeList = (NodeList) expr.evaluate(document, XPathConstants.NODESET);
            return nodeList.getLength();
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    // --- Selezioni ---

    public Sel sel(String xpathExpr) {
        try {
            XPathExpression expr = xpath.compile(xpathExpr);
            NodeList nodeList = (NodeList) expr.evaluate(document, XPathConstants.NODESET);
            List<org.w3c.dom.Node> nodes = new ArrayList<>();
            for (int i = 0; i < nodeList.getLength(); i++) {
                nodes.add(nodeList.item(i));
            }
            return new Sel(nodes, null, this);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    // --- Text normalization ---

    public XML normalizeText() {
        normalizeTextRecursive(document.getDocumentElement());
        return this;
    }

    private void normalizeTextRecursive(org.w3c.dom.Node node) {
        // Depth-first post-order: normalize children first, then self
        org.w3c.dom.NodeList children = node.getChildNodes();
        // Collect element children first (NodeList is live)
        java.util.List<org.w3c.dom.Node> elements = new java.util.ArrayList<>();
        for (int i = 0; i < children.getLength(); i++) {
            org.w3c.dom.Node child = children.item(i);
            if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                elements.add(child);
            }
        }
        for (org.w3c.dom.Node el : elements) {
            normalizeTextRecursive(el);
        }
        Sel.normalizeTextNode(node);
    }

    // --- Namespace ---

    public XML namespace(String prefix, String uri) {
        namespaces.put(prefix, uri);
        xpath.setNamespaceContext(nsContext);
        return this;
    }

    // --- Serializzazione ---

    @Override
    public String toString() {
        return serialize(false, null);
    }

    public String toFragment() {
        return serialize(true, null);
    }

    public void writeTo(Path path) {
        writeTo(path, OutputOptions.builder().build());
    }

    public void writeTo(Path path, OutputOptions options) {
        try (OutputStream os = Files.newOutputStream(path)) {
            Transformer transformer = createTransformer(options);
            transformer.transform(new DOMSource(document), new StreamResult(os));
        } catch (java.io.IOException e) {
            throw new UncheckedIOException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String serialize(boolean omitDeclaration, OutputOptions options) {
        try {
            OutputOptions opts = options != null ? options : OutputOptions.builder().omitDeclaration(omitDeclaration).build();
            Transformer transformer = createTransformer(opts);
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Transformer createTransformer(OutputOptions options) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, options.encoding());
        if (options.omitDeclaration()) {
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        } else {
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        }
        if (options.indent()) {
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", String.valueOf(options.indentAmount()));
        }
        return transformer;
    }

    // --- Accesso interno ---

    Document document() {
        return document;
    }

    XPath xpath() {
        return xpath;
    }

    org.w3c.dom.Element createElement(Document doc, String tagName) {
        int colon = tagName.indexOf(':');
        if (colon > 0) {
            String prefix = tagName.substring(0, colon);
            String uri = namespaces.get(prefix);
            if (uri != null) {
                return doc.createElementNS(uri, tagName);
            }
            throw new IllegalArgumentException(
                "Namespace prefix '" + prefix + "' is not registered. "
                + "Call xml.namespace(\"" + prefix + "\", uri) before creating elements with this prefix.");
        }
        return doc.createElement(tagName);
    }
}
