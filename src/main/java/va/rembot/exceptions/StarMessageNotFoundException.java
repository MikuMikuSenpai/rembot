package va.rembot.exceptions;

public class StarMessageNotFoundException extends RuntimeException {
    public StarMessageNotFoundException(String message) {
        super(message);
    }

    public StarMessageNotFoundException(String message, long messageId) {
        super(message + " " + messageId);
    }
}
