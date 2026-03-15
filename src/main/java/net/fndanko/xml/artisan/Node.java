package net.fndanko.xml.artisan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Node extends Sel {

    static final Node EMPTY = new Node(null, null, null);

    private final org.w3c.dom.Node domNode;

    Node(org.w3c.dom.Node domNode, Sel parent, XML owner) {
        super(domNode != null ? Collections.singletonList(domNode) : Collections.emptyList(), parent, owner);
        this.domNode = domNode;
    }

    private boolean isNullObject() {
        return domNode == null;
    }

    // --- Navigazione DOM ---

    public Sel children() {
        if (isNullObject()) return new Sel(Collections.emptyList(), this, owner);
        List<org.w3c.dom.Node> children = new ArrayList<>();
        org.w3c.dom.NodeList childNodes = domNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            org.w3c.dom.Node child = childNodes.item(i);
            if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                children.add(child);
            }
        }
        return new Sel(children, this, owner);
    }

    public Node parent() {
        if (isNullObject()) return EMPTY;
        org.w3c.dom.Node parentNode = domNode.getParentNode();
        if (parentNode == null || parentNode.getNodeType() == org.w3c.dom.Node.DOCUMENT_NODE) {
            return EMPTY;
        }
        return new Node(parentNode, this, owner);
    }

    public String name() {
        if (isNullObject()) return "";
        return domNode.getNodeName();
    }

    // --- Inserimento strutturale (ritorna il NUOVO nodo) ---

    @Override
    public Node append(String tagName) {
        if (isNullObject()) return EMPTY;
        org.w3c.dom.Document doc = domNode.getOwnerDocument();
        org.w3c.dom.Node newChild = createElement(doc, tagName);
        domNode.appendChild(newChild);
        return new Node(newChild, this, owner);
    }

    @Override
    public Node append(XML fragment) {
        if (isNullObject()) return EMPTY;
        org.w3c.dom.Document doc = domNode.getOwnerDocument();
        org.w3c.dom.Node imported = doc.importNode(fragment.document().getDocumentElement(), true);
        domNode.appendChild(imported);
        return new Node(imported, this, owner);
    }

    @Override
    public Node prepend(String tagName) {
        if (isNullObject()) return EMPTY;
        org.w3c.dom.Document doc = domNode.getOwnerDocument();
        org.w3c.dom.Node newChild = createElement(doc, tagName);
        domNode.insertBefore(newChild, domNode.getFirstChild());
        return new Node(newChild, this, owner);
    }

    @Override
    public Node prepend(XML fragment) {
        if (isNullObject()) return EMPTY;
        org.w3c.dom.Document doc = domNode.getOwnerDocument();
        org.w3c.dom.Node imported = doc.importNode(fragment.document().getDocumentElement(), true);
        domNode.insertBefore(imported, domNode.getFirstChild());
        return new Node(imported, this, owner);
    }

    public Node insert(String tagName, Node before) {
        if (isNullObject()) return EMPTY;
        org.w3c.dom.Document doc = domNode.getOwnerDocument();
        org.w3c.dom.Node newChild = createElement(doc, tagName);
        domNode.insertBefore(newChild, before.unwrap());
        return new Node(newChild, this, owner);
    }

    @Override
    public Node before(String tagName) {
        if (isNullObject()) return EMPTY;
        org.w3c.dom.Document doc = domNode.getOwnerDocument();
        org.w3c.dom.Node newNode = createElement(doc, tagName);
        domNode.getParentNode().insertBefore(newNode, domNode);
        return new Node(newNode, this, owner);
    }

    @Override
    public Node before(XML fragment) {
        if (isNullObject()) return EMPTY;
        org.w3c.dom.Document doc = domNode.getOwnerDocument();
        org.w3c.dom.Node imported = doc.importNode(fragment.document().getDocumentElement(), true);
        domNode.getParentNode().insertBefore(imported, domNode);
        return new Node(imported, this, owner);
    }

    @Override
    public Node after(String tagName) {
        if (isNullObject()) return EMPTY;
        org.w3c.dom.Document doc = domNode.getOwnerDocument();
        org.w3c.dom.Node newNode = createElement(doc, tagName);
        org.w3c.dom.Node nextSibling = domNode.getNextSibling();
        if (nextSibling != null) {
            domNode.getParentNode().insertBefore(newNode, nextSibling);
        } else {
            domNode.getParentNode().appendChild(newNode);
        }
        return new Node(newNode, this, owner);
    }

    @Override
    public Node after(XML fragment) {
        if (isNullObject()) return EMPTY;
        org.w3c.dom.Document doc = domNode.getOwnerDocument();
        org.w3c.dom.Node imported = doc.importNode(fragment.document().getDocumentElement(), true);
        org.w3c.dom.Node nextSibling = domNode.getNextSibling();
        if (nextSibling != null) {
            domNode.getParentNode().insertBefore(imported, nextSibling);
        } else {
            domNode.getParentNode().appendChild(imported);
        }
        return new Node(imported, this, owner);
    }

    @Override
    public Node replace(XML fragment) {
        if (isNullObject()) return EMPTY;
        org.w3c.dom.Document doc = domNode.getOwnerDocument();
        org.w3c.dom.Node imported = doc.importNode(fragment.document().getDocumentElement(), true);
        domNode.getParentNode().replaceChild(imported, domNode);
        return new Node(imported, this, owner);
    }

    // --- Accesso DOM ---

    public org.w3c.dom.Node unwrap() {
        return domNode;
    }
}
