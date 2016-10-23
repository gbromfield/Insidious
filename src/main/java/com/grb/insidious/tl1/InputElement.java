package com.grb.insidious.tl1;

import com.grb.tl1.TL1InputMessage;
import com.grb.tl1.TL1ResponseMessage;

/**
 * Created by gbromfie on 10/17/16.
 */
public class InputElement {
    public Long timestamp;
    public TL1InputMessage tl1InputMsg;
    public OutputElement output;
    public boolean processed;
    public InputElement next;

    public boolean hasResponse() {
        OutputElement outputElem = output;
        while(outputElem != null) {
            if (outputElem.tl1OutputMsg instanceof TL1ResponseMessage) {
                return true;
            }
            outputElem = outputElem.next;
        }
        return false;
    }

    public void appendInput(InputElement element) {
        if (next == null) {
            next = element;
        } else {
            InputElement nextElem = next;
            while(nextElem.next != null) {
                nextElem = nextElem.next;
            }
            nextElem.next = element;
        }
    }

    public void appendOutput(OutputElement element) {
        if (output == null) {
            output = element;
        } else {
            OutputElement outputElem = output;
            while(outputElem.next != null) {
                outputElem = outputElem.next;
            }
            outputElem.next = element;
        }
    }
}
