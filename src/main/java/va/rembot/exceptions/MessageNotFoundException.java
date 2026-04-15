package va.rembot.exceptions;

public class MessageNotFoundException extends RuntimeException {
    public MessageNotFoundException(String message) {
        super(message);
    }

    public MessageNotFoundException(String message, long messageId) {
        super(message + " " + messageId);
    }
}
