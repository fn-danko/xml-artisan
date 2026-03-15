package net.fndanko.xml.artisan;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
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
        return join(JoinConfig.<T>builder().defaults(tagName).build());
    }

    public JoinedSel<T> join(JoinConfig<T> config) {
        BiFunction<Node, T, Node> enterHandler = resolveEnterHandler(config);
        BiFunction<Node, T, Node> updateHandler = resolveUpdateHandler(config);
        Consumer<Node> exitHandler = resolveExitHandler(config);

        // Group selection nodes by parent — each group joins independently (D3 semantics)
        Map<org.w3c.dom.Node, List<org.w3c.dom.Node>> groups = groupByParent(sel.nodes);

        // Empty selection: create one group under document element
        if (groups.isEmpty() && owner != null) {
            groups.put(owner.document().getDocumentElement(), new ArrayList<>());
        }

        List<org.w3c.dom.Node> allMerged = new ArrayList<>();
        Map<org.w3c.dom.Node, T> allDatumMap = new LinkedHashMap<>();

        for (var entry : groups.entrySet()) {
            org.w3c.dom.Node parentDom = entry.getKey();
            List<org.w3c.dom.Node> groupNodes = entry.getValue();

            GroupResult<T> result = joinGroup(
                parentDom, groupNodes, enterHandler, updateHandler, exitHandler
            );
            allMerged.addAll(result.mergedNodes);
            allDatumMap.putAll(result.datumMap);
        }

        return new JoinedSel<>(allMerged, sel.parent, owner, allDatumMap);
    }

    private GroupResult<T> joinGroup(
        org.w3c.dom.Node parentDom,
        List<org.w3c.dom.Node> groupNodes,
        BiFunction<Node, T, Node> enterHandler,
        BiFunction<Node, T, Node> updateHandler,
        Consumer<Node> exitHandler
    ) {
        // Index-based slots: slots[i] = DOM node for data[i], null = enter slot
        org.w3c.dom.Node[] slots = new org.w3c.dom.Node[data.size()];
        List<org.w3c.dom.Node> exitNodes = new ArrayList<>();
        Map<org.w3c.dom.Node, T> datumMap = new LinkedHashMap<>();

        if (dataKey != null && nodeKey != null) {
            classifyByKey(groupNodes, slots, exitNodes);
        } else {
            classifyPositional(groupNodes, slots, exitNodes);
        }

        // Execute EXIT (parent already captured as parentDom before any DOM mutations)
        if (exitHandler != null) {
            for (org.w3c.dom.Node n : exitNodes) {
                exitHandler.accept(new Node(n, sel, owner));
            }
        }

        // Execute UPDATE
        for (int i = 0; i < data.size(); i++) {
            if (slots[i] != null) {
                if (updateHandler != null) {
                    updateHandler.apply(new Node(slots[i], sel, owner), data.get(i));
                }
                datumMap.put(slots[i], data.get(i));
            }
        }

        // Execute ENTER
        if (enterHandler != null) {
            Node parentNode = new Node(parentDom, sel, owner);
            for (int i = 0; i < data.size(); i++) {
                if (slots[i] == null) {
                    Node created = enterHandler.apply(parentNode, data.get(i));
                    if (created != null && created.unwrap() != null) {
                        slots[i] = created.unwrap();
                        datumMap.put(created.unwrap(), data.get(i));
                    }
                }
            }
        }

        // Build merged list in data order
        List<org.w3c.dom.Node> merged = new ArrayList<>();
        for (org.w3c.dom.Node slot : slots) {
            if (slot != null) {
                merged.add(slot);
            }
        }

        return new GroupResult<>(merged, datumMap);
    }

    private void classifyPositional(
        List<org.w3c.dom.Node> groupNodes,
        org.w3c.dom.Node[] slots,
        List<org.w3c.dom.Node> exitNodes
    ) {
        int min = Math.min(groupNodes.size(), data.size());
        for (int i = 0; i < min; i++) {
            slots[i] = groupNodes.get(i);
        }
        for (int i = min; i < groupNodes.size(); i++) {
            exitNodes.add(groupNodes.get(i));
        }
    }

    private void classifyByKey(
        List<org.w3c.dom.Node> groupNodes,
        org.w3c.dom.Node[] slots,
        List<org.w3c.dom.Node> exitNodes
    ) {
        // Build node-by-key map (first occurrence wins, duplicates → exit)
        Map<Object, org.w3c.dom.Node> nodesByKey = new LinkedHashMap<>();
        for (org.w3c.dom.Node domNode : groupNodes) {
            Object key = nodeKey.apply(new Node(domNode, sel, owner));
            if (nodesByKey.containsKey(key)) {
                exitNodes.add(domNode);
            } else {
                nodesByKey.put(key, domNode);
            }
        }

        // Match data to nodes by key
        Set<Object> matchedKeys = new HashSet<>();
        for (int i = 0; i < data.size(); i++) {
            Object key = dataKey.apply(data.get(i));
            org.w3c.dom.Node match = nodesByKey.get(key);
            if (match != null && !matchedKeys.contains(key)) {
                slots[i] = match;
                matchedKeys.add(key);
            }
        }

        // Unmatched nodes → exit
        for (var entry : nodesByKey.entrySet()) {
            if (!matchedKeys.contains(entry.getKey())) {
                exitNodes.add(entry.getValue());
            }
        }
    }

    private static Map<org.w3c.dom.Node, List<org.w3c.dom.Node>> groupByParent(List<org.w3c.dom.Node> nodes) {
        Map<org.w3c.dom.Node, List<org.w3c.dom.Node>> groups = new LinkedHashMap<>();
        for (org.w3c.dom.Node node : nodes) {
            org.w3c.dom.Node parent = node.getParentNode();
            if (parent != null) {
                groups.computeIfAbsent(parent, k -> new ArrayList<>()).add(node);
            }
        }
        return groups;
    }

    private BiFunction<Node, T, Node> resolveEnterHandler(JoinConfig<T> config) {
        if (config.enterSet()) {
            return config.enterHandler();
        }
        if (config.defaultTag() != null) {
            String tag = config.defaultTag();
            return (parent, datum) -> parent.append(tag);
        }
        return null;
    }

    private BiFunction<Node, T, Node> resolveUpdateHandler(JoinConfig<T> config) {
        if (config.updateSet()) {
            return config.updateHandler();
        }
        return (node, datum) -> node;
    }

    private Consumer<Node> resolveExitHandler(JoinConfig<T> config) {
        if (config.exitSet()) {
            return config.exitHandler();
        }
        if (config.defaultTag() != null) {
            return node -> node.remove();
        }
        return null;
    }

    private record GroupResult<T>(List<org.w3c.dom.Node> mergedNodes, Map<org.w3c.dom.Node, T> datumMap) {}
}
