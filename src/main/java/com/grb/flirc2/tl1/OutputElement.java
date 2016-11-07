package com.grb.flirc2.tl1;

import com.grb.tl1.*;

import java.util.Date;

/**
 * Created by gbromfie on 10/17/16.
 */
public class OutputElement {
    public Long timestamp;
    public TL1OutputMessage tl1OutputMsg;
    public int multiplicity;
    public OutputElement next;

    public String toJsonString() {
        String tl1Abbrev = null;
        if (tl1OutputMsg instanceof TL1AOMessage) {
            TL1AOMessage ao = (TL1AOMessage)tl1OutputMsg;
            tl1Abbrev = String.format("%s:%s...", ao.getCmdCode(), ao.getTid());
        } else if (tl1OutputMsg instanceof TL1AckMessage) {
            TL1AckMessage ack = (TL1AckMessage)tl1OutputMsg;
            tl1Abbrev = String.format("%s...", ack.getAckCode());
        } else {
            TL1ResponseMessage resp = (TL1ResponseMessage)tl1OutputMsg;
            if (resp.getResponseType().equals(TL1ResponseType.CONTINUATION)) {
                tl1Abbrev = String.format("%s:%s>...", resp.getTid(), resp.getComplCode());
            } else {
                tl1Abbrev = String.format("%s:%s...", resp.getTid(), resp.getComplCode());
            }
        }
        return String.format("\"%s %s mult=%d\"", tl1Abbrev,
                TL1RecordingManager.DateFormatter.format(new Date(timestamp)),
                multiplicity);
    }

    @Override
    public String toString() {
        String tl1Abbrev = null;
        if (tl1OutputMsg instanceof TL1AOMessage) {
            TL1AOMessage ao = (TL1AOMessage)tl1OutputMsg;
            tl1Abbrev = String.format("%s:%s...", ao.getCmdCode(), ao.getTid());
        } else if (tl1OutputMsg instanceof TL1AckMessage) {
            TL1AckMessage ack = (TL1AckMessage)tl1OutputMsg;
            tl1Abbrev = String.format("%s...", ack.getAckCode());
        } else {
            TL1ResponseMessage resp = (TL1ResponseMessage)tl1OutputMsg;
            if (resp.getResponseType().equals(TL1ResponseType.CONTINUATION)) {
                tl1Abbrev = String.format("%s:%s>...", resp.getTid(), resp.getComplCode());
            } else {
                tl1Abbrev = String.format("%s:%s...", resp.getTid(), resp.getComplCode());
            }
        }
        return String.format("\"%s\" %s mult=%d", tl1Abbrev,
                TL1RecordingManager.DateFormatter.format(new Date(timestamp)),
                multiplicity);
    }
}
