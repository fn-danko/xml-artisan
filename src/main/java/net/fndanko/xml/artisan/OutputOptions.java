package net.fndanko.xml.artisan;

public class OutputOptions {

    private final boolean indent;
    private final int indentAmount;
    private final boolean omitDeclaration;
    private final String encoding;

    private OutputOptions(Builder builder) {
        this.indent = builder.indent;
        this.indentAmount = builder.indentAmount;
        this.omitDeclaration = builder.omitDeclaration;
        this.encoding = builder.encoding;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean indent() {
        return indent;
    }

    public int indentAmount() {
        return indentAmount;
    }

    public boolean omitDeclaration() {
        return omitDeclaration;
    }

    public String encoding() {
        return encoding;
    }

    public static class Builder {
        private boolean indent = false;
        private int indentAmount = 2;
        private boolean omitDeclaration = false;
        private String encoding = "UTF-8";

        public Builder indent(boolean indent) {
            this.indent = indent;
            return this;
        }

        public Builder indentAmount(int indentAmount) {
            this.indentAmount = indentAmount;
            return this;
        }

        public Builder omitDeclaration(boolean omitDeclaration) {
            this.omitDeclaration = omitDeclaration;
            return this;
        }

        public Builder encoding(String encoding) {
            this.encoding = encoding;
            return this;
        }

        public OutputOptions build() {
            return new OutputOptions(this);
        }
    }
}
