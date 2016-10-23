package com.grb.insidious.tl1;

import com.grb.tl1.TL1OutputMessage;

/**
 * Created by gbromfie on 10/17/16.
 */
public class OutputElement {
    public Long timestamp;
    public TL1OutputMessage tl1OutputMsg;
    public OutputElement next;
}
