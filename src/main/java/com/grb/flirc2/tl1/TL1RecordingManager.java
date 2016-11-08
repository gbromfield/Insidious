package com.grb.flirc2.tl1;

import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import com.grb.flirc2.Protocol;
import com.grb.flirc2.recording.Recording;
import com.grb.flirc2.recording.RecordingElement;
import com.grb.tl1.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by gbromfie on 10/17/16.
 */
public class TL1RecordingManager {
    final Logger logger = LoggerFactory.getLogger(TL1RecordingManager.class);
    final static public SimpleDateFormat DateFormatter = new SimpleDateFormat("HH:mm:ss");

    class TL1RecordingTimerTask extends TimerTask {
        public OutputElement _element;
        public String _ctag;
        
        public TL1RecordingTimerTask(OutputElement element, String ctag) {
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
        	} else if ((outputMsg instanceof TL1AOMessage) && (_renumberAOs)) {
                TL1AOMessage ao = (TL1AOMessage)outputMsg;
                ao.setATAG(String.valueOf(_atag++));
        	}
        }

        public void sendOutput() {
            if (_element.tl1OutputMsg instanceof TL1AOMessage) {
                for(int i = 0; i < _element.multiplicity; i++) {
                    updateTag(_element.tl1OutputMsg);
                    _listener.onTL1Output(_element.tl1OutputMsg);
                }
            } else {
                updateTag(_element.tl1OutputMsg);
                for(int i = 0; i < _element.multiplicity; i++) {
                    _listener.onTL1Output(_element.tl1OutputMsg);
                }
            }
        }

        @Override
        public void run() {
            // send _element.tl1OutputMsg
            sendOutput();
            Long currentTS = _element.timestamp;
            while(_element.next != null) {
                _element = _element.next;
                if((currentTS == null) || (_element.timestamp == null) ||
                        ((_element.timestamp - currentTS) == 0)) {
                    // send _element.tl1OutputMsg
                    sendOutput();
                } else {
                    _timer.schedule(new TL1RecordingTimerTask(_element, _ctag), _element.timestamp - currentTS);
                    break;
                }
            }
        }
    }

    private String _sessionName;
    private CommandDB _commandDB;
    private Timer _timer;
	private TL1AgentDecoder _agentDecoder;
	private TL1ManagerDecoder _managerDecoder;
	private TL1RecordingListener _listener;
    private boolean _renumberAOs;
    private int _atag;

    public TL1RecordingManager(String sessionName, TL1RecordingListener listener) {
    	_sessionName = sessionName;
    	_listener = listener;
        _commandDB = new CommandDB();
    	_timer = null;
    	_agentDecoder = new TL1AgentDecoder();
    	_managerDecoder = new TL1ManagerDecoder();
        _renumberAOs = false;
        _atag = 1;
    }

    public void close() {
        _commandDB.reset();
        if (_timer != null) {
            _timer.cancel();
        }
    }

    public void setRecording(Recording recording) throws TL1MessageMaxSizeExceededException, ParseException {
        if (_timer != null) {
            _timer.cancel();
        }
    	_timer = new Timer("TL1RecordingManager_" + _sessionName, true);
        loadRecording(recording);
    }

    public void loadRecording(Recording recording) throws TL1MessageMaxSizeExceededException, ParseException {
        _commandDB.reset();
        List<RecordingElement> elements = recording.getRecordingElements();
        if (elements != null) {
            for(RecordingElement element : elements) {
                if (element.protocol.equals(Protocol.TL1)) {
                    if (element.input != null) {
                        ByteBuffer buffer = ByteBuffer.wrap(element.input.getBytes());
                        TL1InputMessage tl1Request = (TL1InputMessage) _managerDecoder.decodeTL1Message(buffer);
                        int multiplicity = 1;
                        if (element.multiplicity != null) {
                            multiplicity = element.multiplicity;
                        }
                        addInput(tl1Request, element.timestamp.getTime(), multiplicity);
                    }
                    if (element.output != null) {
                        if (_agentDecoder == null) {
                            _agentDecoder = new TL1AgentDecoder();
                        }
                        ByteBuffer buffer = ByteBuffer.wrap(element.output.getBytes());
                        while(buffer.hasRemaining()) {
                            TL1OutputMessage tl1Output = (TL1OutputMessage)_agentDecoder.decodeTL1Message(buffer);
                            if (tl1Output == null) {
                                // error
                            } else {
                                int multiplicity = 1;
                                if (element.multiplicity != null) {
                                    multiplicity = element.multiplicity;
                                    if ((multiplicity > 1) && (tl1Output instanceof TL1AOMessage)) {
                                        // Need to renumber AOs when AO multiplicity is greater than 1
                                        _renumberAOs = true;
                                    }
                                }
                                InputElement input = addOutput(tl1Output, element.timestamp.getTime(), multiplicity);
                                if ((input != null) && (tl1Output instanceof TL1AOMessage)) {
                                    if (input.multiplicity > 1) {
                                        // Need to renumber AOs when linked command multiplicity is greater than 1
                                        _renumberAOs = true;
                                    }
                                }
                            }
                        }
                    }
                    if (element.tcpserver != null) {

                    }
                }
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Session %s command database:\n %s", _sessionName, _commandDB.toJsonString()));
        }
    }

    /**
     * For going from json to internal structure
     */
    public void addInput(TL1InputMessage input, Long timestamp, int multiplicity) {
        InputElement newElement = new InputElement();
        newElement.timestamp = timestamp;
        newElement.tl1InputMsg = input;
        newElement.multiplicity = multiplicity;
        newElement.left = multiplicity;
        newElement.output = null;
        newElement.next = null;
        _commandDB.add(newElement);
    }

    /**
     * For going from json to internal structure
     */
    public InputElement addOutput(TL1OutputMessage outputMsg, Long timestamp, int multiplicity) {
        OutputElement newElement = new OutputElement();
        newElement.tl1OutputMsg = outputMsg;
        newElement.next = null;
        newElement.timestamp = timestamp;
        newElement.multiplicity = multiplicity;
        return _commandDB.add(newElement);
    }

    public void processInput(ByteBuffer readBuffer) throws TL1MessageMaxSizeExceededException, ParseException {
    	TL1InputMessage inputMsg = (TL1InputMessage)_managerDecoder.decodeTL1Message(readBuffer);
    	if (inputMsg != null) {
    		processInput(inputMsg);
    	}
    }

    public void processInput(TL1InputMessage input) {
        if (logger.isInfoEnabled()) {
            logger.info(String.format("Session %s - Received TL1 command: \"%s\"", _sessionName, input.toString()));
        }
        InputElement inElement;
        try {
            inElement = _commandDB.getInputElement(input);
            if (logger.isInfoEnabled()) {
                logger.info(String.format("Session %s - Matched command %s in database for TL1 command: \"%s\"",
                        _sessionName, inElement.toString().trim(), input.toString()));
            }
            OutputElement outElement = inElement.output;
            if (outElement == null) {
                if (logger.isInfoEnabled()) {
                    logger.info(String.format("Session %s - No output for TL1 command: \"%s\", this command will timeout",
                            _sessionName, input.toString()));
                }
            } else {
                if ((inElement.timestamp == null) || (outElement.timestamp == null) ||
                        ((outElement.timestamp - inElement.timestamp) <= 0)) {
                    _timer.schedule(new TL1RecordingTimerTask(outElement, input.getCTAG()), 0);
                } else {
                    _timer.schedule(new TL1RecordingTimerTask(outElement, input.getCTAG()), outElement.timestamp - inElement.timestamp);
                }
            }
        } catch (InputElementNotFoundException e) {
            if (logger.isErrorEnabled()) {
                logger.error(String.format("Session %s - Failed to get output for command -> %s", _sessionName, e.toString()));
            }
            String tl1Resp = null;
            try {
                tl1Resp = String.format(
                        "\r\n\n   \"%s\" 16-01-01 00:00:00\r\nM  %s DENY\r\n   ICNV\r\n   /* Session %s - %s */\r\n;",
                        e.getTID(), e.getInput().getCTAG(), _sessionName, e.getReason().toString());
                OutputElement outputElement = new OutputElement();
                outputElement.multiplicity = 1;
                outputElement.timestamp = null;
                outputElement.next = null;
                outputElement.tl1OutputMsg = (TL1OutputMessage)_agentDecoder.decodeTL1Message(ByteBuffer.wrap(tl1Resp.getBytes()));
                _timer.schedule(new TL1RecordingTimerTask(outputElement, e.getInput().getCTAG()), 0);
            } catch(Exception e1) {
                if (logger.isErrorEnabled()) {
                    logger.error(String.format("Session %s - Failed to send error response %s for command -> %s - %s",
                            _sessionName, tl1Resp, input.toString(), e1.getMessage()));
                }
            }
        }
    }

    public String toJsonString() {
        return _commandDB.toJsonString();
    }

    // TODO: NEED TO UPDATE ATAG ON OUTGOING
    // TEST WITH BLANK TID
    
    public static void main(String[] args) {
        try {
            TL1RecordingManager test = new TL1RecordingManager("test", new TL1RecordingListener() {
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
            test.addInput(input1, now, 1);
            TL1OutputMessage output1 = (TL1OutputMessage)agtdecoder.decodeTL1Message(ByteBuffer.wrap(actuserip1));
            test.addOutput(output1, now + 5000, 1);
            output1 = (TL1OutputMessage)agtdecoder.decodeTL1Message(ByteBuffer.wrap(actuserresp1));
            test.addOutput(output1, now + 10000, 1);
            output1 = (TL1OutputMessage)agtdecoder.decodeTL1Message(ByteBuffer.wrap(ao1));
            test.addOutput(output1, now + 12000, 1);

            byte[] actuser2 = "ACT-USER:GAGA:ADMIN:0002::ADMIN;".getBytes();
            byte[] actuserip2 = "IP 0002\r\n<".getBytes();
            byte[] actuserresp2 = "\r\n\n   GAGA 85-10-09 22:05:12\r\nM  0002 DENY\r\n;".getBytes();
            TL1InputMessage input2 = (TL1InputMessage)mgrdecoder.decodeTL1Message(ByteBuffer.wrap(actuser2));
            test.addInput(input2, now, 1);
            test.addOutput(output1, now + 2000, 1);
            TL1OutputMessage output2 = (TL1OutputMessage)agtdecoder.decodeTL1Message(ByteBuffer.wrap(actuserip2));
            test.addOutput(output2, now + 5000, 1);
            test.addOutput(output1, null, 1);
            test.addOutput(output2, now + 10000, 1);
            test.addOutput(output2, now + 15000, 1);
            test.addOutput(output1, now + 17000, 1);
            output2 = (TL1OutputMessage)agtdecoder.decodeTL1Message(ByteBuffer.wrap(actuserresp2));
            test.addOutput(output2, null, 1);

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
