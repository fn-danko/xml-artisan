package net.fndanko.xml.artisan;

import java.util.List;
import java.util.function.Function;

public class BoundSel<T> {

    private final Sel sel;
    private final List<T> data;
    private final Function<T, ?> dataKey;
    private final Function<Node, ?> nodeKey;
    private final XML owner;

    BoundSel(Sel sel, List<T> data, Function<T, ?> dataKey, Function<Node, ?> nodeKey, XML owner) {
        this.sel = sel;
        this.data = data;
        this.dataKey = dataKey;
        this.nodeKey = nodeKey;
        this.owner = owner;
    }

    public JoinedSel<T> join(String tagName) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public JoinedSel<T> join(JoinConfig<T> config) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
