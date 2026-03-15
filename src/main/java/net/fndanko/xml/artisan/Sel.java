package net.fndanko.xml.artisan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class Sel implements Iterable<Node> {

    final List<org.w3c.dom.Node> nodes;
    final Sel parent;
    final XML owner;

    Sel(List<org.w3c.dom.Node> nodes, Sel parent, XML owner) {
        this.nodes = nodes != null ? nodes : Collections.emptyList();
        this.parent = parent != null ? parent : this;
        this.owner = owner;
    }

    // --- Lettura ---

    public String attr(String name) {
        if (nodes.isEmpty()) return "";
        org.w3c.dom.Node first = nodes.get(0);
        if (first instanceof Element) {
            String val = ((Element) first).getAttribute(name);
            return val != null ? val : "";
        }
        return "";
    }

    public String text() {
        if (nodes.isEmpty()) return "";
        return readDirectText(nodes.get(0));
    }

    public String deepText() {
        if (nodes.isEmpty()) return "";
        String tc = nodes.get(0).getTextContent();
        return tc != null ? tc : "";
    }

    public int size() {
        return nodes.size();
    }

    public boolean empty() {
        return nodes.isEmpty();
    }

    // --- Modifica ---

    public Sel attr(String name, String value) {
        for (org.w3c.dom.Node n : nodes) {
            if (n instanceof Element) {
                ((Element) n).setAttribute(name, value);
            }
        }
        return this;
    }

    public Sel attr(String name, Function<String, String> fn) {
        for (org.w3c.dom.Node n : nodes) {
            if (n instanceof Element) {
                Element el = (Element) n;
                String current = el.getAttribute(name);
                if (current == null) current = "";
                el.setAttribute(name, fn.apply(current));
            }
        }
        return this;
    }

    public Sel text(String value) {
        for (org.w3c.dom.Node n : nodes) {
            writeDirectText(n, value);
        }
        return this;
    }

    public Sel text(Function<String, String> fn) {
        for (org.w3c.dom.Node n : nodes) {
            writeDirectText(n, fn.apply(readDirectText(n)));
        }
        return this;
    }

    public Sel normalizeText() {
        for (org.w3c.dom.Node n : nodes) {
            normalizeTextNode(n);
        }
        return this;
    }

    public Sel coalesceText() {
        for (org.w3c.dom.Node n : nodes) {
            String tc = n.getTextContent();
            if (tc == null) tc = "";
            while (n.hasChildNodes()) {
                n.removeChild(n.getFirstChild());
            }
            n.appendChild(ownerDoc(n).createTextNode(tc));
        }
        return this;
    }

    static String readDirectText(org.w3c.dom.Node n) {
        StringBuilder sb = new StringBuilder();
        org.w3c.dom.NodeList children = n.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            org.w3c.dom.Node child = children.item(i);
            short type = child.getNodeType();
            if (type == org.w3c.dom.Node.TEXT_NODE || type == org.w3c.dom.Node.CDATA_SECTION_NODE) {
                String v = child.getNodeValue();
                if (v != null) sb.append(v);
            }
        }
        return sb.toString();
    }

    static void writeDirectText(org.w3c.dom.Node n, String value) {
        normalizeTextNode(n);
        List<org.w3c.dom.Node> toRemove = new ArrayList<>();
        org.w3c.dom.NodeList children = n.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            org.w3c.dom.Node child = children.item(i);
            short type = child.getNodeType();
            if (type == org.w3c.dom.Node.TEXT_NODE || type == org.w3c.dom.Node.CDATA_SECTION_NODE) {
                toRemove.add(child);
            }
        }
        for (org.w3c.dom.Node r : toRemove) {
            n.removeChild(r);
        }
        org.w3c.dom.Document doc = ownerDoc(n);
        n.insertBefore(doc.createTextNode(value), n.getFirstChild());
    }

    static void normalizeTextNode(org.w3c.dom.Node n) {
        org.w3c.dom.NodeList children = n.getChildNodes();
        StringBuilder sb = new StringBuilder();
        List<org.w3c.dom.Node> textNodes = new ArrayList<>();
        boolean hasCdata = false;
        for (int i = 0; i < children.getLength(); i++) {
            org.w3c.dom.Node child = children.item(i);
            short type = child.getNodeType();
            if (type == org.w3c.dom.Node.TEXT_NODE || type == org.w3c.dom.Node.CDATA_SECTION_NODE) {
                textNodes.add(child);
                String v = child.getNodeValue();
                if (v != null) sb.append(v);
                if (type == org.w3c.dom.Node.CDATA_SECTION_NODE) hasCdata = true;
            }
        }
        if (textNodes.size() <= 1) return;
        for (org.w3c.dom.Node tn : textNodes) {
            n.removeChild(tn);
        }
        org.w3c.dom.Document doc = ownerDoc(n);
        org.w3c.dom.Node unified = hasCdata
                ? doc.createCDATASection(sb.toString())
                : doc.createTextNode(sb.toString());
        n.insertBefore(unified, n.getFirstChild());
    }

    private static org.w3c.dom.Document ownerDoc(org.w3c.dom.Node n) {
        org.w3c.dom.Document doc = n.getOwnerDocument();
        if (doc == null && n instanceof org.w3c.dom.Document) {
            return (org.w3c.dom.Document) n;
        }
        return doc;
    }

    public Sel remove() {
        for (org.w3c.dom.Node n : nodes) {
            if (n.getParentNode() != null) {
                n.getParentNode().removeChild(n);
            }
        }
        return parent;
    }

    // --- Inserimento strutturale ---

    public Sel append(String tagName) {
        for (org.w3c.dom.Node n : nodes) {
            org.w3c.dom.Document doc = n.getOwnerDocument();
            n.appendChild(createElement(doc, tagName));
        }
        return this;
    }

    public Sel append(XML fragment) {
        for (org.w3c.dom.Node n : nodes) {
            org.w3c.dom.Document doc = n.getOwnerDocument();
            org.w3c.dom.Node imported = doc.importNode(fragment.document().getDocumentElement(), true);
            n.appendChild(imported);
        }
        return this;
    }

    public Sel prepend(String tagName) {
        for (org.w3c.dom.Node n : nodes) {
            org.w3c.dom.Document doc = n.getOwnerDocument();
            n.insertBefore(createElement(doc, tagName), n.getFirstChild());
        }
        return this;
    }

    public Sel prepend(XML fragment) {
        for (org.w3c.dom.Node n : nodes) {
            org.w3c.dom.Document doc = n.getOwnerDocument();
            org.w3c.dom.Node imported = doc.importNode(fragment.document().getDocumentElement(), true);
            n.insertBefore(imported, n.getFirstChild());
        }
        return this;
    }

    public Sel before(String tagName) {
        for (org.w3c.dom.Node n : nodes) {
            if (n.getParentNode() == null) continue;
            org.w3c.dom.Document doc = n.getOwnerDocument();
            n.getParentNode().insertBefore(createElement(doc, tagName), n);
        }
        return this;
    }

    public Sel before(XML fragment) {
        for (org.w3c.dom.Node n : nodes) {
            if (n.getParentNode() == null) continue;
            org.w3c.dom.Document doc = n.getOwnerDocument();
            org.w3c.dom.Node imported = doc.importNode(fragment.document().getDocumentElement(), true);
            n.getParentNode().insertBefore(imported, n);
        }
        return this;
    }

    public Sel after(String tagName) {
        for (org.w3c.dom.Node n : nodes) {
            if (n.getParentNode() == null) continue;
            org.w3c.dom.Document doc = n.getOwnerDocument();
            org.w3c.dom.Node newNode = createElement(doc, tagName);
            org.w3c.dom.Node nextSibling = n.getNextSibling();
            if (nextSibling != null) {
                n.getParentNode().insertBefore(newNode, nextSibling);
            } else {
                n.getParentNode().appendChild(newNode);
            }
        }
        return this;
    }

    public Sel after(XML fragment) {
        for (org.w3c.dom.Node n : nodes) {
            if (n.getParentNode() == null) continue;
            org.w3c.dom.Document doc = n.getOwnerDocument();
            org.w3c.dom.Node imported = doc.importNode(fragment.document().getDocumentElement(), true);
            org.w3c.dom.Node nextSibling = n.getNextSibling();
            if (nextSibling != null) {
                n.getParentNode().insertBefore(imported, nextSibling);
            } else {
                n.getParentNode().appendChild(imported);
            }
        }
        return this;
    }

    public Sel replace(XML fragment) {
        List<org.w3c.dom.Node> newNodes = new ArrayList<>();
        for (org.w3c.dom.Node n : nodes) {
            if (n.getParentNode() == null) continue;
            org.w3c.dom.Document doc = n.getOwnerDocument();
            org.w3c.dom.Node imported = doc.importNode(fragment.document().getDocumentElement(), true);
            n.getParentNode().replaceChild(imported, n);
            newNodes.add(imported);
        }
        return new Sel(newNodes, parent, owner);
    }

    // --- Navigazione ---

    public Sel sel(String xpathExpr) {
        if (nodes.isEmpty()) {
            return new Sel(Collections.emptyList(), this, owner);
        }
        try {
            String rewritten = rewriteXPath(xpathExpr);
            XPathExpression expr = owner.xpath().compile(rewritten);
            Set<org.w3c.dom.Node> seen = new LinkedHashSet<>();
            for (org.w3c.dom.Node contextNode : nodes) {
                NodeList nodeList = (NodeList) expr.evaluate(contextNode, XPathConstants.NODESET);
                for (int i = 0; i < nodeList.getLength(); i++) {
                    seen.add(nodeList.item(i));
                }
            }
            return new Sel(new ArrayList<>(seen), this, owner);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    public Sel end() {
        return parent;
    }

    // --- Data binding (stub per v1.1) ---

    public <T> BoundSel<T> data(List<T> data) {
        return new BoundSel<>(this, data, null, null, owner);
    }

    public <T, K> BoundSel<T> data(List<T> data, Function<T, K> dataKey, Function<Node, K> nodeKey) {
        return new BoundSel<>(this, data, dataKey, nodeKey, owner);
    }

    // --- Iterazione e conversione ---

    public Stream<Node> stream() {
        return list().stream();
    }

    public List<Node> list() {
        List<Node> result = new ArrayList<>();
        for (org.w3c.dom.Node n : nodes) {
            result.add(new Node(n, this, owner));
        }
        return result;
    }

    public Node first() {
        if (nodes.isEmpty()) return Node.EMPTY;
        return new Node(nodes.get(0), this, owner);
    }

    public Node last() {
        if (nodes.isEmpty()) return Node.EMPTY;
        return new Node(nodes.get(nodes.size() - 1), this, owner);
    }

    public Sel order() {
        for (int i = 0; i < nodes.size(); i++) {
            org.w3c.dom.Node n = nodes.get(i);
            org.w3c.dom.Node parentNode = n.getParentNode();
            if (parentNode != null) {
                parentNode.removeChild(n);
                parentNode.appendChild(n);
            }
        }
        return this;
    }

    @Override
    public Iterator<Node> iterator() {
        return list().iterator();
    }

    // --- Utility interna ---

    org.w3c.dom.Element createElement(org.w3c.dom.Document doc, String tagName) {
        if (owner != null) {
            return owner.createElement(doc, tagName);
        }
        return doc.createElement(tagName);
    }

    static String rewriteXPath(String xpathExpr) {
        if (xpathExpr.startsWith("//")) {
            return "." + xpathExpr;
        }
        return xpathExpr;
    }
}
