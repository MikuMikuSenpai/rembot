package va.rembot.exceptions;

public class StrikeSpamNotFoundException extends RuntimeException {
    public StrikeSpamNotFoundException(String message) {
        super(message);
    }
}
