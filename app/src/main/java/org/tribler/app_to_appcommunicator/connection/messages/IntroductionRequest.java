package org.tribler.app_to_appcommunicator.connection.messages;

import java.net.InetSocketAddress;
import java.util.Map;

/**
 * Created by jaap on 5/31/16.
 */
public class IntroductionRequest extends Message {
    final private static String CONNECTION_TYPE = "connection_type";
    final private static String NETWORK_OPERATOR = "network_operator";

    public IntroductionRequest(String peerId, InetSocketAddress destination, long connectionType, String networkOperator) {
        super(INTRODUCTION_REQUEST, peerId, destination);
        put(CONNECTION_TYPE, connectionType);
        put(NETWORK_OPERATOR, networkOperator);

    }

    public static Message fromMap(Map map) throws MessageException {
        String peerId = (String) map.get(PEER_ID);
        InetSocketAddress destination = Message.createMapAddress((Map) map.get(DESTINATION));
        long connectionType = (long) map.get(CONNECTION_TYPE);
        String networkOperator = (String) map.get(NETWORK_OPERATOR);
        return new IntroductionRequest(peerId, destination, connectionType, networkOperator);
    }

    public String getNetworkOperator() {
        return (String) get(NETWORK_OPERATOR);
    }

    public long getConnectionType() {
        return (long) get(CONNECTION_TYPE);
    }

    @Override
    public String toString() {
        return "IntroductionRequest{" + super.toString() + "}";
    }
}
