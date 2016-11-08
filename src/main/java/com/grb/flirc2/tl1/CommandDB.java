package com.grb.flirc2.tl1;

import com.grb.tl1.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by gbromfie on 10/29/16.
 */
public class CommandDB {
    final Logger logger = LoggerFactory.getLogger(CommandDB.class);

    private HashMap<String, TIDElement> _tidElementMap;

    public CommandDB() {
        _tidElementMap = new HashMap<String, TIDElement>();
    }

    public String resolveTID(String tid) {
        if (tid.isEmpty()) {
            return _tidElementMap.get(tid).getTID();
        } else {
            return tid;
        }
    }

    public void reset() {
        for(TIDElement element : _tidElementMap.values()) {
            element.reset();
        }
        _tidElementMap.clear();
    }

    public void add(InputElement newElement) {
        TL1InputMessage input = newElement.tl1InputMsg;
        TIDElement tidElement = _tidElementMap.get(input.getTid());
        if (tidElement == null) {
            tidElement = new TIDElement(input.getTid());
            _tidElementMap.put(input.getTid(), tidElement);
        }
        tidElement.add(newElement);
    }

    public InputElement add(OutputElement newElement) {
        TL1OutputMessage output = newElement.tl1OutputMsg;
        if (output instanceof TL1AckMessage) {
            TL1AckMessage ack = (TL1AckMessage)output;
            // no tid so loop through the tids
            for (TIDElement element : _tidElementMap.values()) {
                if (element.add(newElement) != null) {
                    // found
                    return null;
                }
            }
            // error not found
        } else if (output instanceof TL1ResponseMessage) {
            TL1ResponseMessage resp = (TL1ResponseMessage)output;
            return add(resp.getTid(), newElement);
        } else if (output instanceof TL1AOMessage) {
            TL1AOMessage ao = (TL1AOMessage)output;
            return add(ao.getTid(), newElement);
        }
        return null;
    }

    private InputElement add(String tid, OutputElement newElement) {
        TIDElement tidElement = _tidElementMap.get(tid);
            if (tidElement == null) {
            tidElement = new TIDElement(tid);
            _tidElementMap.put(tid, tidElement);
        }
        InputElement input = tidElement.add(newElement);
        if (input == null) {
            // check for blank tid
            TIDElement blankTidElement = _tidElementMap.get("");
            if (blankTidElement != null) {
                input = blankTidElement.add(newElement);
                if (input != null) {
                    // merge the two
                    TIDElement merged = tidElement.merge(blankTidElement);
                    _tidElementMap.put("", merged);
                    _tidElementMap.put(tid, merged);
                    return input;
                }
            }
            // error not found
        }
        return input;
    }

    public InputElement getInputElement(TL1InputMessage input) throws InputElementNotFoundException {
        TIDElement tidElement = _tidElementMap.get(input.getTid());
        if (tidElement == null) {
            throw new InputElementNotFoundException(input, input.getTid(), InputElementNotFoundReason.TID_NOT_FOUND, input.getTid());
        } else {
            return tidElement.getInputElement(input);
        }
    }

    public String toJsonString() {
        HashSet<String> tidSet = new HashSet<String>();
        StringBuilder bldr = new StringBuilder("{");
        int index = 0;
        for(TIDElement tidElement: _tidElementMap.values()) {
            if (!tidSet.contains(tidElement.getTID())) {
                if (index > 0) {
                    bldr.append(",");
                }
                tidSet.add(tidElement.getTID());
                bldr.append(String.format("\"%s\": {", tidElement.getTID()));
                bldr.append(tidElement.toJsonString());
                bldr.append("}");
                index++;
            }
        }
        bldr.append("}");
        return bldr.toString();
    }

    @Override
    public String toString() {
        HashSet<String> tidSet = new HashSet<String>();
        StringBuilder bldr = new StringBuilder();
        for(TIDElement tidElement: _tidElementMap.values()) {
            if (!tidSet.contains(tidElement.getTID())) {
                tidSet.add(tidElement.getTID());
                bldr.append(String.format("\"%s\" {\n", tidElement.getTID()));
                bldr.append(tidElement);
                bldr.append("}\n");
            }
        }
        return bldr.toString();
    }
}