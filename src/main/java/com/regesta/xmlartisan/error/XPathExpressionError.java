package net.fndanko.xmlartisan.error;

public class XPathExpressionError extends RuntimeException {
    public XPathExpressionError(String message, Throwable cause) {
        super(message, cause);
    }
}
