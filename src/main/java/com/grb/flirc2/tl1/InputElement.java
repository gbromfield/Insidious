package com.grb.flirc2.tl1;

import com.grb.tl1.TL1InputMessage;
import com.grb.tl1.TL1ResponseMessage;

import java.util.Date;

/**
 * Created by gbromfie on 10/17/16.
 */
public class InputElement {
    public Long timestamp;
    public TL1InputMessage tl1InputMsg;
    public OutputElement output;
    public int multiplicity;
    public int left;
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

    public String toJsonString() {
        int numLinked = 0;
        InputElement linked = next;
        while(linked != null) {
            numLinked++;
            linked = linked.next;
        }
        StringBuilder bldr = new StringBuilder();
        bldr.append(String.format("\"%s:%s:%s... %s mult=%d left=%d linked=%d\": [", tl1InputMsg.getCmdCode(),
                tl1InputMsg.getTid(), tl1InputMsg.getAid(),
                TL1RecordingManager.DateFormatter.format(new Date(timestamp)),
                multiplicity, left, numLinked));
        OutputElement outputElement = output;
        int index = 0;
        while(outputElement != null) {
            if (index > 0) {
                bldr.append(",");
            }
            bldr.append(String.format("%s", outputElement.toJsonString()));
            outputElement = outputElement.next;
            index++;
        }
        bldr.append("]");
        return bldr.toString();
    }

    @Override
    public String toString() {
        int numLinked = 0;
        InputElement linked = next;
        while(linked != null) {
            numLinked++;
            linked = linked.next;
        }
        StringBuilder bldr = new StringBuilder();
        bldr.append(String.format("    \"%s:%s:%s...\" %s mult=%d left=%d linked=%d\n", tl1InputMsg.getCmdCode(),
                tl1InputMsg.getTid(), tl1InputMsg.getAid(),
                TL1RecordingManager.DateFormatter.format(new Date(timestamp)),
                multiplicity, left, numLinked));
        OutputElement outputElement = output;
        while(outputElement != null) {
            bldr.append(String.format("        %s\n", outputElement));
            outputElement = outputElement.next;
        }
        return bldr.toString();
    }
}
