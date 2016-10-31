package com.grb.insidious.tl1;

import com.grb.tl1.TL1InputMessage;

/**
 * Created by gbromfie on 10/30/16.
 */
public class InputElementNotFoundException extends Exception {
    private TL1InputMessage _input;
    private String _tid;
    private InputElementNotFoundReason _reason;

    public InputElementNotFoundException(TL1InputMessage input, String tid, InputElementNotFoundReason reason, String message) {
        super(message);
        _input = input;
        _tid = tid;
        _reason = reason;
    }

    public InputElementNotFoundException(TL1InputMessage input, String tid, InputElementNotFoundReason reason, String message,
                                         Throwable cause) {
        super(message, cause);
        _input = input;
        _tid = tid;
        _reason = reason;
    }

    public TL1InputMessage getInput() {
        return _input;
    }

    public String getTID() {
        return _tid;
    }

    public InputElementNotFoundReason getReason() {
        return _reason;
    }

    @Override
    public String toString() {
        String message = getMessage();
        if ((message == null) || (message.isEmpty())) {
            return String.format("%s for input: \"%s\"", _reason.toString(), _input.toString());
        } else {
            return String.format("%s (%s) for input: \"%s\"", _reason.toString(), message, _input.toString());
        }
    }
}
