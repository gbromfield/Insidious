package com.grb.flirc2.tl1;

/**
 * Created by gbromfie on 10/30/16.
 */
public enum InputElementNotFoundReason {
    TID_NOT_FOUND(),
    COMMAND_CODE_NOT_FOUND,
    AID_NOT_FOUND,
    INPUTS_EXHAUSTED,
    NO_OUTPUTS;

    @Override
    public String toString() {
        if (this.equals(TID_NOT_FOUND)) {
            return "TID not found";
        } else if (this.equals(COMMAND_CODE_NOT_FOUND)) {
            return "Command Code not found";
        } else if (this.equals(AID_NOT_FOUND)) {
            return "AID not found";
        } else if (this.equals(INPUTS_EXHAUSTED)) {
            return "Inputs exhausted";
        } else if (this.equals(NO_OUTPUTS)) {
            return "No outputs";
        } else {
            return "Unknown reason";
        }
    }
}
