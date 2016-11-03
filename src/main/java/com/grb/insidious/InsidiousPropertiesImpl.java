package com.grb.insidious;

import com.ciena.logx.logfile.ra.insidious.InsidiousLogXPropertiesImpl;

/**
 * Created by gbromfie on 11/2/16.
 */
public class InsidiousPropertiesImpl extends InsidiousLogXPropertiesImpl implements InsidiousProperties {
    private Boolean _interactive;

    @Override
    public boolean getInteractive() {
        if (_interactive == null) {
            return false;
        }
        return _interactive;
    }

    @Override
    public void setInteractive(boolean interactive) {
        _interactive = interactive;
    }
}
