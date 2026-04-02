package va.rembot.exceptions;

public class MessageSpamNotFoundException extends RuntimeException {
    public MessageSpamNotFoundException(String message) {
        super(message);
    }
}
