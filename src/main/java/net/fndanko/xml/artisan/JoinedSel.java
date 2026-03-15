package net.fndanko.xml.artisan;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.w3c.dom.Element;

public class JoinedSel<T> extends Sel {

    private final Map<org.w3c.dom.Node, T> datumMap;

    JoinedSel(List<org.w3c.dom.Node> nodes, Sel parent, XML owner, Map<org.w3c.dom.Node, T> datumMap) {
        super(nodes, parent, owner);
        this.datumMap = datumMap != null ? datumMap : Collections.emptyMap();
    }

    JoinedSel(List<org.w3c.dom.Node> nodes, Sel parent, XML owner) {
        this(nodes, parent, owner, Collections.emptyMap());
    }

    public JoinedSel<T> attrWith(String name, BiFunction<String, T, String> fn) {
        for (org.w3c.dom.Node n : nodes) {
            T datum = datumMap.get(n);
            if (n instanceof Element) {
                Element el = (Element) n;
                String current = el.getAttribute(name);
                if (current == null) current = "";
                el.setAttribute(name, fn.apply(current, datum));
            }
        }
        return this;
    }

    public JoinedSel<T> textWith(Function<T, String> fn) {
        for (org.w3c.dom.Node n : nodes) {
            T datum = datumMap.get(n);
            n.setTextContent(fn.apply(datum));
        }
        return this;
    }

    public JoinedSel<T> eachWith(BiConsumer<Node, T> fn) {
        for (org.w3c.dom.Node n : nodes) {
            T datum = datumMap.get(n);
            fn.accept(new Node(n, this, owner), datum);
        }
        return this;
    }

    public Sel toSel() {
        return new Sel(nodes, parent, owner);
    }

    @Override
    public JoinedSel<T> order() {
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
}
