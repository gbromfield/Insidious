package com.grb.flirc2;

import com.ciena.logx.LogXProperties;
import com.ciena.logx.logfile.ra.flirc2.Flirc2LogXCommandLineProcessor;

/**
 * Created by gbromfie on 11/2/16.
 */
public class Flirc2CommandLineProcessor extends Flirc2LogXCommandLineProcessor {
    public Flirc2CommandLineProcessor() {
        super();
        syntax.add("-i (interactive mode)\n");
    }

    @Override
    protected LogXProperties createProperties() {
        return new Flirc2PropertiesImpl();
    }

    @Override
    protected int parseArgument(LogXProperties props, String[] args, int index) {
        if (args[index].equalsIgnoreCase("-i")) {
            processingInputFiles = false;
            ((Flirc2Properties)props).setInteractive(true);
            return index + 1;
        } else {
            return super.parseArgument(props, args, index);
        }
    }
}
