package net.fndanko.xml.artisan;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class JoinedSel<T> extends Sel {

    JoinedSel(List<org.w3c.dom.Node> nodes, Sel parent, XML owner) {
        super(nodes, parent, owner);
    }

    public JoinedSel<T> attrWith(String name, BiFunction<String, T, String> fn) {
        return this;
    }

    public JoinedSel<T> textWith(Function<T, String> fn) {
        return this;
    }

    public JoinedSel<T> eachWith(BiConsumer<Node, T> fn) {
        return this;
    }

    public Sel toSel() {
        return new Sel(nodes, parent, owner);
    }

    @Override
    public JoinedSel<T> order() {
        return this;
    }
}
