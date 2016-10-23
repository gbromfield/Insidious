package com.grb.insidious.tl1;

import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.*;

import com.grb.insidious.Protocol;
import com.grb.insidious.capture.Capture;
import com.grb.insidious.capture.CaptureElement;
import com.grb.tl1.TL1AckMessage;
import com.grb.tl1.TL1AgentDecoder;
import com.grb.tl1.TL1InputMessage;
import com.grb.tl1.TL1ManagerDecoder;
import com.grb.tl1.TL1MessageMaxSizeExceededException;
import com.grb.tl1.TL1OutputMessage;
import com.grb.tl1.TL1ResponseMessage;

/**
 * Created by gbromfie on 10/17/16.
 */
public class TL1CaptureManager {
    class TL1CaptureTimerTask extends TimerTask {
        public OutputElement _element;
        public String _ctag;
        
        public TL1CaptureTimerTask(OutputElement element, String ctag) {
            _element = element;
            _ctag = ctag;
        }

        public void updateTag(TL1OutputMessage outputMsg) {
        	if (outputMsg instanceof TL1ResponseMessage) {
        		TL1ResponseMessage resp = (TL1ResponseMessage)outputMsg;
        		resp.setCTAG(_ctag);
        	} else if (outputMsg instanceof TL1AckMessage) {
        		TL1AckMessage ack = (TL1AckMessage)outputMsg;
        		ack.setCTAG(_ctag);
        	} else {
        	}
        }
        
        @Override
        public void run() {
            // send _element.tl1OutputMsg
        	updateTag(_element.tl1OutputMsg);
        	_listener.onTL1Output(_element.tl1OutputMsg);
            Long currentTS = _element.timestamp;
            while(_element.next != null) {
                _element = _element.next;
                if((currentTS == null) || (_element.timestamp == null) ||
                        ((_element.timestamp - currentTS) == 0)) {
                    // send _element.tl1OutputMsg
                	updateTag(_element.tl1OutputMsg);
                	_listener.onTL1Output(_element.tl1OutputMsg);
                } else {
                    _timer.schedule(new TL1CaptureTimerTask(_element, _ctag), _element.timestamp - currentTS);
                    break;
                }
            }
        }
    }

    private String _sessionName;
    private HashMap<String, HashMap<String, HashMap<String, InputElement>>> _commandMap;
    private HashMap<String, HashMap<String, InputElement>> _outstandingCommandMap;
    private Object _lastElementAdded;
    private Timer _timer;
	private TL1AgentDecoder _agentDecoder;
	private TL1ManagerDecoder _managerDecoder;
	private TL1CaptureListener _listener;
	
    public TL1CaptureManager(String sessionName, TL1CaptureListener listener) {
    	_sessionName = sessionName;
    	_listener = listener;
    	_commandMap = new HashMap<String, HashMap<String, HashMap<String, InputElement>>>();
    	_outstandingCommandMap = new HashMap<String, HashMap<String, InputElement>>();
    	_lastElementAdded = null;
    	_timer = new Timer("TL1CaptureManager_" + _sessionName, true);
    	_agentDecoder = new TL1AgentDecoder();
    	_managerDecoder = new TL1ManagerDecoder();
    }
    
    public void setCapture(Capture capture) throws TL1MessageMaxSizeExceededException, ParseException {
    	_commandMap.clear();
    	_outstandingCommandMap.clear();
    	_lastElementAdded = null;
    	_timer.cancel();
    	_timer = new Timer("TL1CaptureManager_" + _sessionName, true);
		if (capture.elements != null) {
			for(int i = 0; i < capture.elements.length; i++) {
				CaptureElement element = capture.elements[i];
				if (element.protocol.equals(Protocol.TL1)) {
					if (element.input != null) {
						ByteBuffer buffer = ByteBuffer.wrap(element.input.getBytes());
						TL1InputMessage tl1Request = (TL1InputMessage) _managerDecoder.decodeTL1Message(buffer);
						addInput(tl1Request, element.timestamp.getTime());
					}
					if (element.output != null) {
						if (_agentDecoder == null) {
							_agentDecoder = new TL1AgentDecoder();
						}
						ByteBuffer buffer = ByteBuffer.wrap(element.output.getBytes());
						TL1OutputMessage tl1Output = (TL1OutputMessage)_agentDecoder.decodeTL1Message(buffer);
						int multitplicity = 1;
						if (element.multiplicity != null) {
							multitplicity = element.multiplicity; 
						}
						addOutput(tl1Output, element.timestamp.getTime());
					}
					if (element.tcpserver != null) {
						
					}
				}
			}
		}
    }
        
    /**
     * For going from json to internal structure
     */
    public void addInput(TL1InputMessage input, Long timestamp) {
        InputElement newElement = new InputElement();
        newElement.timestamp = timestamp;
        newElement.tl1InputMsg = input;
        newElement.processed = false;
        newElement.output = null;
        newElement.next = null;

        HashMap<String, HashMap<String, InputElement>> cmdCodeMap = _commandMap.get(input.getTid());
        if (cmdCodeMap == null) {
            cmdCodeMap = new HashMap<String, HashMap<String, InputElement>>();
            _commandMap.put(input.getTid(), cmdCodeMap);
        }
        HashMap<String, InputElement> aidMap = cmdCodeMap.get(input.getCmdCode());
        if (aidMap == null) {
            aidMap = new HashMap<String, InputElement>();
            cmdCodeMap.put(input.getCmdCode(), aidMap);
        }
        InputElement element = aidMap.get(input.getAid());
        if (element == null) {
            aidMap.put(input.getAid(), newElement);
        } else {
            element.appendInput(newElement);
        }
        _lastElementAdded = newElement;

        HashMap<String, InputElement> ctagMap = _outstandingCommandMap.get(input.getTid());
        if (ctagMap == null) {
            ctagMap = new HashMap<String, InputElement>();
            _outstandingCommandMap.put(input.getTid(), ctagMap);
        }
        ctagMap.put(input.getCTAG(), newElement);
    }

    /**
     * For going from json to internal structure
     */
    public void addOutput(TL1OutputMessage outputMsg, Long timestamp) {
        OutputElement newElement = new OutputElement();
        newElement.tl1OutputMsg = outputMsg;
        newElement.next = null;
        newElement.timestamp = timestamp;
        if (outputMsg instanceof TL1AckMessage) {
            TL1AckMessage ack = (TL1AckMessage)outputMsg;
            // no tid so loop through the tids
            InputElement matchElement = null;
            Iterator<HashMap<String, InputElement>> it = _outstandingCommandMap.values().iterator();
            while(it.hasNext()) {
                HashMap<String, InputElement> element = it.next();
                matchElement = element.get(ack.getCTAG());
                if (matchElement != null) {
                    if(!matchElement.hasResponse()) {
                        break;
                    }
                }
            }
            if (matchElement == null) {
                // missing request for ack
            } else {
                matchElement.appendOutput(newElement);
                _lastElementAdded = newElement;
            }
        } else if (outputMsg instanceof TL1ResponseMessage) {
            TL1ResponseMessage resp = (TL1ResponseMessage)outputMsg;
            HashMap<String, InputElement> ctagMap = _outstandingCommandMap.get(resp.getTid());
            if (ctagMap == null) {
                // missing request for response
            } else {
                InputElement element = ctagMap.get(resp.getCTAG());
                if (element == null) {
                    // missing request for response
                } else {
                    element.appendOutput(newElement);
                    _lastElementAdded = newElement;
                }
            }
        } else {
            if (_lastElementAdded == null) {
                // no request or response to tie to
            } else {
                if(_lastElementAdded instanceof InputElement) {
                    InputElement element = (InputElement)_lastElementAdded;
                    element.appendOutput(newElement);
                } else {
                    OutputElement element = (OutputElement)_lastElementAdded;
                    element.next = newElement;
                }
                _lastElementAdded = newElement;
            }
        }
    }

    public InputElement getUnprocessedInputElement(TL1InputMessage input) {
        HashMap<String, HashMap<String, InputElement>> cmdCodeMap = _commandMap.get(input.getTid());
        if(cmdCodeMap != null) {
            HashMap<String, InputElement> aidMap = cmdCodeMap.get(input.getCmdCode());
            if(aidMap != null) {
                InputElement element = aidMap.get(input.getAid());
                while((element != null) && (element.processed)) {
                    element = element.next;
                }
                return element;
            }
        }
        return null;
    }

    public void processInput(ByteBuffer readBuffer) throws TL1MessageMaxSizeExceededException, ParseException {
    	TL1InputMessage inputMsg = (TL1InputMessage)_managerDecoder.decodeTL1Message(readBuffer);
    	if (inputMsg != null) {
    		processInput(inputMsg);
    	}
    }
    
    public void processInput(TL1InputMessage input) {
        InputElement inElement = getUnprocessedInputElement(input);
        inElement.processed = true;
        OutputElement outElement = inElement.output;
        while (outElement != null) {
            if ((inElement.timestamp == null) || (outElement.timestamp == null) ||
                    ((outElement.timestamp - inElement.timestamp) == 0)) {
                // send immediately
                System.out.println(outElement.tl1OutputMsg);
                outElement = outElement.next;
            } else {
                // use timer
                _timer.schedule(new TL1CaptureTimerTask(outElement, input.getCTAG()), outElement.timestamp - inElement.timestamp);
                break;
            }
        }
    }

    // TODO: NEED TO UPDATE ATAG ON OUTGOING
    // TEST WITH BLANK TID
    
    public static void main(String[] args) {
        try {
            TL1CaptureManager test = new TL1CaptureManager("test", new TL1CaptureListener() {
				@Override
				public void onTL1Output(TL1OutputMessage outputMsg) {
					System.out.println(outputMsg);
				}
			});
            TL1ManagerDecoder mgrdecoder = new TL1ManagerDecoder();
            TL1AgentDecoder agtdecoder = new TL1AgentDecoder();
            byte[] actuser1 = "ACT-USER:GAGA:ADMIN:0001::ADMIN;".getBytes();
            byte[] actuserip1 = "IP 0001\r\n<".getBytes();
            byte[] actuserresp1 = "\r\n\n   GAGA 85-10-09 22:05:12\r\nM  0001 COMPLD\r\n;".getBytes();
            byte[] ao1 = "\r\n\n   BLUB 93-06-02 12:00:00\r\n** 1 REPT PM T1\r\n   \"AID-T1-1:CVL,50\"\r\n   \"AID-T1-2:CVL,10\"\r\n   \"AID-T1-n:CVL,22\"\r\n;".getBytes();
            TL1InputMessage input1 = (TL1InputMessage)mgrdecoder.decodeTL1Message(ByteBuffer.wrap(actuser1));
            long now = new Date().getTime();
            test.addInput(input1, now);
            TL1OutputMessage output1 = (TL1OutputMessage)agtdecoder.decodeTL1Message(ByteBuffer.wrap(actuserip1));
            test.addOutput(output1, now + 5000);
            output1 = (TL1OutputMessage)agtdecoder.decodeTL1Message(ByteBuffer.wrap(actuserresp1));
            test.addOutput(output1, now + 10000);
            output1 = (TL1OutputMessage)agtdecoder.decodeTL1Message(ByteBuffer.wrap(ao1));
            test.addOutput(output1, now + 12000);

            byte[] actuser2 = "ACT-USER:GAGA:ADMIN:0002::ADMIN;".getBytes();
            byte[] actuserip2 = "IP 0002\r\n<".getBytes();
            byte[] actuserresp2 = "\r\n\n   GAGA 85-10-09 22:05:12\r\nM  0002 DENY\r\n;".getBytes();
            TL1InputMessage input2 = (TL1InputMessage)mgrdecoder.decodeTL1Message(ByteBuffer.wrap(actuser2));
            test.addInput(input2, now);
            test.addOutput(output1, now + 2000);
            TL1OutputMessage output2 = (TL1OutputMessage)agtdecoder.decodeTL1Message(ByteBuffer.wrap(actuserip2));
            test.addOutput(output2, now + 5000);
            test.addOutput(output1, null);
            test.addOutput(output2, now + 10000);
            test.addOutput(output2, now + 15000);
            test.addOutput(output1, now + 17000);
            output2 = (TL1OutputMessage)agtdecoder.decodeTL1Message(ByteBuffer.wrap(actuserresp2));
            test.addOutput(output2, null);

            test.processInput(input1);
            for(int i = 0; i < 15; i++) {
                Thread.sleep(1000);
                System.out.println(String.valueOf((i+1)*1000));
            }
            byte[] actuser3 = "ACT-USER:GAGA:ADMIN:0002000::ADMIN;".getBytes();
            TL1InputMessage input3 = (TL1InputMessage)mgrdecoder.decodeTL1Message(ByteBuffer.wrap(actuser3));
            test.processInput(input3);
            for(int i = 0; i < 30; i++) {
                Thread.sleep(1000);
                System.out.println(String.valueOf((i+1)*1000));
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
