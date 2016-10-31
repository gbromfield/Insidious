package com.grb.insidious.tl1;

import com.grb.tl1.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by gbromfie on 10/29/16.
 */
public class TIDElement {
    final Logger logger = LoggerFactory.getLogger(TIDElement.class);

    private String _tid;
    private HashMap<String, HashMap<String, InputElement>> _cmdCodeMap;
    private HashMap<String, InputElement> _outstandingCommandMap;
    private ArrayList<InputElement> _inputElements;
    private Object _lastElementAdded;
    private InputElement _lastInputElementAdded;
    private long _lastElementAddedTime;

    public TIDElement(String tid) {
        _tid = tid;
        _cmdCodeMap = new HashMap<String, HashMap<String, InputElement>>();
        _outstandingCommandMap = new HashMap<String, InputElement>();
        _inputElements = new ArrayList<InputElement>();
        _lastElementAdded = null;
        _lastInputElementAdded = null;
        _lastElementAddedTime = 0;
    }

    public String getTID() {
        return _tid;
    }

    public void reset() {
        _cmdCodeMap.clear();
        _outstandingCommandMap.clear();
        _inputElements.clear();
        _lastElementAdded = null;
        _lastInputElementAdded = null;
        _lastElementAddedTime = 0;
    }

    public void add(InputElement newElement) {
        TL1InputMessage input = newElement.tl1InputMsg;
        HashMap<String, InputElement> aidMap = _cmdCodeMap.get(input.getCmdCode().toUpperCase());
        if (aidMap == null) {
            aidMap = new HashMap<String, InputElement>();
            _cmdCodeMap.put(input.getCmdCode().toUpperCase(), aidMap);
        }
        InputElement element = aidMap.get(input.getAid());
        if (element == null) {
            aidMap.put(input.getAid(), newElement);
        } else {
            element.appendInput(newElement);
        }
        _lastElementAdded = newElement;
        _lastInputElementAdded = newElement;
        _lastElementAddedTime = System.currentTimeMillis();
        _outstandingCommandMap.put(input.getCTAG(), newElement);
        _inputElements.add(newElement);
    }

    public InputElement add(OutputElement newElement) {
        TL1OutputMessage output = newElement.tl1OutputMsg;
        if (output instanceof TL1AckMessage) {
            TL1AckMessage ack = (TL1AckMessage)output;
            // no tid so loop through the tids
            InputElement matchElement = _outstandingCommandMap.get(ack.getCTAG());
            if ((matchElement == null) || (matchElement.hasResponse())) {
                // missing request for ack
                return null;
            } else {
                matchElement.appendOutput(newElement);
                _lastElementAdded = newElement;
                _lastElementAddedTime = System.currentTimeMillis();
            }
            return matchElement;
        } else if (output instanceof TL1ResponseMessage) {
            TL1ResponseMessage resp = (TL1ResponseMessage)output;
            InputElement matchElement = _outstandingCommandMap.get(resp.getCTAG());
            if (matchElement == null) {
                // missing request for response
                return null;
            } else {
                matchElement.appendOutput(newElement);
                _lastElementAdded = newElement;
                _lastElementAddedTime = System.currentTimeMillis();
                if (resp.getResponseType().equals(TL1ResponseType.TERMINATION)) {
                    _outstandingCommandMap.remove(resp.getCTAG());
                }
            }
            return matchElement;
        } else if (output instanceof TL1AOMessage) {
            TL1AOMessage ao = (TL1AOMessage)output;
            if (_lastElementAdded == null) {
                // no request or response to tie to
                return null;
            } else {
                InputElement matchElement = null;
                if(_lastElementAdded instanceof InputElement) {
                    matchElement = (InputElement) _lastElementAdded;
                    matchElement.appendOutput(newElement);
                } else {
                    OutputElement element = (OutputElement) _lastElementAdded;
                    element.next = newElement;
                    matchElement = _lastInputElementAdded;
                }
                _lastElementAdded = newElement;
                _lastElementAddedTime = System.currentTimeMillis();
                return matchElement;
            }
        }
        return null;
    }

    public InputElement getInputElement(TL1InputMessage input) throws InputElementNotFoundException {
        HashMap<String, InputElement> aidMap = _cmdCodeMap.get(input.getCmdCode().toUpperCase());
        if(aidMap == null) {
            throw new InputElementNotFoundException(input, _tid, InputElementNotFoundReason.COMMAND_CODE_NOT_FOUND, input.getCmdCode());
        } else {
            InputElement element = aidMap.get(input.getAid());
            if (element == null) {
                throw new InputElementNotFoundException(input, _tid, InputElementNotFoundReason.AID_NOT_FOUND, input.getAid());
            } else {
                if (element.left > 0) {
                    element.left = element.left - 1;
                    return element;
                } else {
                    while(element.next != null) {
                        if (element.next.left > 0) {
                            element.next.left = element.next.left - 1;
                            return element.next;
                        }
                        element = element.next;
                    }
                }
                throw new InputElementNotFoundException(input, _tid, InputElementNotFoundReason.INPUTS_EXHAUSTED, null);
            }
        }
    }

    public TIDElement merge(TIDElement other) {
        if (_lastElementAddedTime > other._lastElementAddedTime) {
            for(InputElement element: _inputElements) {
                other.add(element);
            }
            return other;
        } else {
            for(InputElement element: other._inputElements) {
                add(element);
            }
            return this;
        }
    }

    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        for(InputElement inputElement: _inputElements) {
            bldr.append(inputElement);
            bldr.append("\n");
        }
        return bldr.toString();
    }
}
