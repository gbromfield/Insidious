package com.grb.insidious;

import com.ciena.logx.CommandLineProcessor;
import com.ciena.logx.LogXProperties;
import com.ciena.logx.logfile.ra.insidious.InsidiousLogXCommandLineProcessor;

/**
 * Created by gbromfie on 11/2/16.
 */
public class InsidiousCommandLineProcessor extends InsidiousLogXCommandLineProcessor {
    public InsidiousCommandLineProcessor() {
        super();
        syntax.add("-i (interactive mode)\n");
    }

    @Override
    protected LogXProperties createProperties() {
        return new InsidiousPropertiesImpl();
    }

    @Override
    protected int parseArgument(LogXProperties props, String[] args, int index) {
        if (args[index].equalsIgnoreCase("-i")) {
            processingInputFiles = false;
            ((InsidiousProperties)props).setInteractive(true);
            return index + 1;
        } else {
            return super.parseArgument(props, args, index);
        }
    }
}
