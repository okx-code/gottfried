package sh.okx.gottfried.auth;

import com.github.steveice10.mc.auth.exception.request.RequestException;

public class XboxRequestException extends RequestException {
    public XboxRequestException(String message) {
        super(message);
    }
}