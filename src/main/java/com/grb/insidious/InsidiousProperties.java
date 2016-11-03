package com.grb.insidious;

import com.ciena.logx.logfile.ra.insidious.InsidiousLogXProperties;

/**
 * Created by gbromfie on 11/2/16.
 */
public interface InsidiousProperties  extends InsidiousLogXProperties {
    public boolean getInteractive();
    public void setInteractive(boolean interactive);
}
