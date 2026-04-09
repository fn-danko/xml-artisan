package net.fndanko.xml.artisan;

public class InvalidNameException extends XmlArtisanException {

    public InvalidNameException(String message) {
        super(message);
    }

    public InvalidNameException(String message, Throwable cause) {
        super(message, cause);
    }
}
