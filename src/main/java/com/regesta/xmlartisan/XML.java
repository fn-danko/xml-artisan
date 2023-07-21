package net.fndanko.xmlartisan;

import net.fndanko.xmlartisan.error.XPathExpressionError;
import org.w3c.dom.Document;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

public class XML {
    private final Document document;
    private final XPath xPath = XPathFactory.newInstance().newXPath();

    private XML(Document document) {
        this.document = document;
    }

    public static XML from(Document document) {
        return new XML(document);
    }

    public static XML newDocument() {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    public String getValue(String xpathExpr) {
        try {
            return (String) xPath.evaluate(xpathExpr, document, XPathConstants.STRING);
        } catch (XPathExpressionException e) {
            throw new XPathExpressionError(String.format("Error evaluating xpath \"%s\"", xpathExpr), e);
        }
    }
}
