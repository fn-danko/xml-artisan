package net.fndanko.xml.artisan;

import java.util.function.BiFunction;
import java.util.function.Consumer;

public class JoinConfig<T> {

    private final String defaultTag;
    private final BiFunction<Node, T, Node> enterHandler;
    private final BiFunction<Node, T, Node> updateHandler;
    private final Consumer<Node> exitHandler;
    private final boolean enterSet;
    private final boolean updateSet;
    private final boolean exitSet;

    private JoinConfig(Builder<T> builder) {
        this.defaultTag = builder.defaultTag;
        this.enterHandler = builder.enterHandler;
        this.updateHandler = builder.updateHandler;
        this.exitHandler = builder.exitHandler;
        this.enterSet = builder.enterSet;
        this.updateSet = builder.updateSet;
        this.exitSet = builder.exitSet;
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    String defaultTag() { return defaultTag; }
    BiFunction<Node, T, Node> enterHandler() { return enterHandler; }
    BiFunction<Node, T, Node> updateHandler() { return updateHandler; }
    Consumer<Node> exitHandler() { return exitHandler; }
    boolean enterSet() { return enterSet; }
    boolean updateSet() { return updateSet; }
    boolean exitSet() { return exitSet; }

    public static class Builder<T> {
        private String defaultTag;
        private BiFunction<Node, T, Node> enterHandler;
        private BiFunction<Node, T, Node> updateHandler;
        private Consumer<Node> exitHandler;
        private boolean enterSet;
        private boolean updateSet;
        private boolean exitSet;

        public Builder<T> defaults(String tagName) {
            this.defaultTag = tagName;
            return this;
        }

        public Builder<T> enter(BiFunction<Node, T, Node> fn) {
            this.enterHandler = fn;
            this.enterSet = true;
            return this;
        }

        public Builder<T> update(BiFunction<Node, T, Node> fn) {
            this.updateHandler = fn;
            this.updateSet = true;
            return this;
        }

        public Builder<T> exit(Consumer<Node> fn) {
            this.exitHandler = fn;
            this.exitSet = true;
            return this;
        }

        public JoinConfig<T> build() {
            return new JoinConfig<>(this);
        }
    }
}
