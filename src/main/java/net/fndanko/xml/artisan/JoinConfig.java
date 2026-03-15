package net.fndanko.xml.artisan;

import java.util.function.BiFunction;
import java.util.function.Consumer;

public class JoinConfig<T> {

    private final String defaultTag;
    private final BiFunction<Node, T, Node> enterHandler;
    private final BiFunction<Node, T, Node> updateHandler;
    private final Consumer<Node> exitHandler;

    private JoinConfig(Builder<T> builder) {
        this.defaultTag = builder.defaultTag;
        this.enterHandler = builder.enterHandler;
        this.updateHandler = builder.updateHandler;
        this.exitHandler = builder.exitHandler;
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    String defaultTag() { return defaultTag; }
    BiFunction<Node, T, Node> enterHandler() { return enterHandler; }
    BiFunction<Node, T, Node> updateHandler() { return updateHandler; }
    Consumer<Node> exitHandler() { return exitHandler; }

    public static class Builder<T> {
        private String defaultTag;
        private BiFunction<Node, T, Node> enterHandler;
        private BiFunction<Node, T, Node> updateHandler;
        private Consumer<Node> exitHandler;

        public Builder<T> defaults(String tagName) {
            this.defaultTag = tagName;
            return this;
        }

        public Builder<T> enter(BiFunction<Node, T, Node> fn) {
            this.enterHandler = fn;
            return this;
        }

        public Builder<T> update(BiFunction<Node, T, Node> fn) {
            this.updateHandler = fn;
            return this;
        }

        public Builder<T> exit(Consumer<Node> fn) {
            this.exitHandler = fn;
            return this;
        }

        public JoinConfig<T> build() {
            return new JoinConfig<>(this);
        }
    }
}
