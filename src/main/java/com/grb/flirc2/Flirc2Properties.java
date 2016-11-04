package com.grb.flirc2;

import com.ciena.logx.logfile.ra.flirc2.Flirc2LogXProperties;

/**
 * Created by gbromfie on 11/2/16.
 */
public interface Flirc2Properties extends Flirc2LogXProperties {
    public boolean getInteractive();
    public void setInteractive(boolean interactive);
}
