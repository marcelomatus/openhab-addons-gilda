package org.openhab.binding.insteonplm.internal.driver;

import java.util.List;

import org.openhab.binding.insteonplm.internal.driver.Port.ReplyType;
import org.openhab.binding.insteonplm.internal.message.ModemMessageType;
import org.openhab.binding.insteonplm.internal.message.modem.AllLinkRecordResponse;
import org.openhab.binding.insteonplm.internal.message.modem.BaseModemMessage;
import org.openhab.binding.insteonplm.internal.message.modem.GetFirstAllLinkingRecord;
import org.openhab.binding.insteonplm.internal.message.modem.PureNack;
import org.openhab.binding.insteonplm.internal.message.modem.SendInsteonMessage;
import org.openhab.binding.insteonplm.internal.message.modem.StandardMessageReceived;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

/**
 * Handles the incoming data and turns this into a useful message.
 *
 *
 * @author Bernd Pfrommer
 * @author Daniel Pfrommer
 * @since 1.5.0
 * @author David Bennett
 */
public class DataParser {
    private static final Logger logger = LoggerFactory.getLogger(Port.class);

    private List<BaseModemMessage> pendingMessages = Lists.newArrayList();
    private int currentIndex;
    private int lengthToLookFor = 0;
    private byte[] incomingData = new byte[40];
    private static final byte START_OF_MESSAGE = 0x2;

    enum ParsingState {
        LookingForStart,
        Command,
        Data
    }

    private ParsingState currentState = ParsingState.LookingForStart;
    private boolean lastMessageExtended = false;
    private ModemMessageType messageType = null;

    public void addData(byte[] data, int len) {
        for (int i = 0; i < len; i++) {
            addByte(data[i]);
        }
    }

    public List<BaseModemMessage> getPendingMessages() {
        return pendingMessages;
    }

    private void addByte(byte data) {
        switch (currentState) {
            case LookingForStart:
                if (data == START_OF_MESSAGE) {
                    // Yay! Next state.
                    currentState = ParsingState.Command;
                }
                break;
            case Command:
                messageType = ModemMessageType.fromCommand(data);
                if (messageType != null) {
                    if (messageType == ModemMessageType.SendInsteonMessage) {
                        if (lastMessageExtended) {
                            lengthToLookFor = 23;
                        } else {
                            lengthToLookFor = 9;
                        }
                    } else {
                        lengthToLookFor = messageType.getReceiveLength() - 2;
                    }
                    if (lengthToLookFor == 0) {
                        // Make a new message.
                        BaseModemMessage mess = createMessage(messageType, new byte[0]);
                        if (mess != null) {
                            pendingMessages.add(mess);
                        }
                        currentState = ParsingState.LookingForStart;
                    } else {
                        currentState = ParsingState.Data;
                        currentIndex = 0;
                    }
                } else {
                    currentState = ParsingState.LookingForStart;
                }
                break;
            case Data:
                incomingData[currentIndex++] = data;
                if (currentIndex >= lengthToLookFor) {
                    byte[] dataBytes = new byte[currentIndex];
                    System.arraycopy(incomingData, 0, dataBytes, 0, currentIndex);
                    BaseModemMessage mess = createMessage(messageType, dataBytes);
                    if (mess != null) {
                        pendingMessages.add(mess);
                    }
                    currentState = ParsingState.LookingForStart;
                }
                break;
        }
    }

    private BaseModemMessage createMessage(ModemMessageType messageType, byte[] bs) {
        switch (messageType) {
            case AllLinkRecordResponse:
                return new AllLinkRecordResponse(bs);
            case GetFirstAllLinkRecord:
                return new GetFirstAllLinkingRecord(bs);
            case GetNextAllLinkRecord:
                return new GetFirstAllLinkingRecord(bs);
            case PureNack:
                return new PureNack();
            case SendInsteonMessage:
                return new SendInsteonMessage(bs);
            case StandardMessageReceived:
                return new StandardMessageReceived(bs);
            case ExtendedMessageReceived:
                return new StandardMessageReceived(bs);
            default:
                logger.warn("Unsupported insteon message {}", messageType.toString());
                return null;
        }
    }

    /**
     * Blocking wait for ack or nack from modem.
     * Called by IOStreamWriter for flow control.
     *
     * @return true if retransmission is necessary
     */
    public boolean waitForReply(BaseModemMessage mess, byte data[]) {
        m_reply = ReplyType.WAITING_FOR_ACK;
        while (m_reply == ReplyType.WAITING_FOR_ACK) {
            try {
                logger.trace("writer waiting for ack.");
                // There have been cases observed, in particular for
                // the Hub, where we get no ack or nack back, causing the binding
                // to hang in the wait() below, because unsolicited messages
                // do not trigger a notify(). For this reason we request retransmission
                // if the wait() times out.
                getRequestReplyLock().wait(3000); // be patient for 30 sec
                if (m_reply == ReplyType.WAITING_FOR_ACK) { // timeout expired without getting ACK or NACK
                    logger.trace("writer timeout expired, asking for retransmit!");
                    m_reply = ReplyType.GOT_NACK;
                    break;
                } else {
                    logger.trace("writer got ack: {}", (m_reply == ReplyType.GOT_ACK));
                }
            } catch (InterruptedException e) {
                break; // done for the day...
            }
        }
        return (m_reply == ReplyType.GOT_NACK);
    }
}
