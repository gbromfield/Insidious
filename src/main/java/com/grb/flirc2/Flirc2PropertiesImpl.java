package com.grb.flirc2;

import com.ciena.logx.logfile.ra.flirc2.Flirc2LogXPropertiesImpl;

/**
 * Created by gbromfie on 11/2/16.
 */
public class Flirc2PropertiesImpl extends Flirc2LogXPropertiesImpl implements Flirc2Properties {
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
