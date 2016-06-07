package org.tribler.app_to_appcommunicator.PEX.messages;

import java.net.InetSocketAddress;
import java.util.Map;

/**
 * Created by jaap on 5/31/16.
 */
public class IntroductionRequest extends Message {
    final private static String CONNECTION_TYPE = "connection_type";

    public IntroductionRequest(String peerId, InetSocketAddress destination, long connectionType) {
        super(INTRODUCTION_REQUEST, peerId, destination);
        put(CONNECTION_TYPE, connectionType);
    }

    public static Message fromMap(Map map) throws MessageException {
        String peerId = (String) map.get(PEER_ID);
        InetSocketAddress destination = Message.createMapAddress((Map) map.get(DESTINATION));
        long connectionType = (long) map.get(CONNECTION_TYPE);
        return new IntroductionRequest(peerId, destination, connectionType);
    }

    public long getConnectionType() {
        return (long) get(CONNECTION_TYPE);
    }

    @Override
    public String toString() {
        return "IntroductionRequest{" + super.toString() + "}";
    }
}
