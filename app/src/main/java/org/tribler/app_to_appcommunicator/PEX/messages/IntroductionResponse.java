package org.tribler.app_to_appcommunicator.PEX.messages;

import java.net.InetSocketAddress;
import java.util.Map;

/**
 * Created by jaap on 5/31/16.
 */
public class IntroductionResponse extends Message {
    final private static String CONNECTION_TYPE = "connection_type";
    final private static String INTERNAL_SOURCE = "internal_source";
    final private static String INVITEE = "invitee";

    public IntroductionResponse(String peerId, InetSocketAddress internalSource, InetSocketAddress destination,
                                InetSocketAddress invitee, long connectionType) {
        super(INTRODUCTION_RESPONSE, peerId, destination);
        put(CONNECTION_TYPE, connectionType);
        put(INTERNAL_SOURCE, createAddressMap(internalSource));
        put(INVITEE, createAddressMap(invitee));
    }

    public static Message fromMap(Map map) throws MessageException {
        String peerId = (String) map.get(PEER_ID);
        InetSocketAddress internalSource = Message.createMapAddress((Map) map.get(INTERNAL_SOURCE));
        InetSocketAddress destination = Message.createMapAddress((Map) map.get(DESTINATION));
        InetSocketAddress invitee = Message.createMapAddress((Map) map.get(INVITEE));
        long connectionType = (long) map.get(CONNECTION_TYPE);
        return new IntroductionResponse(peerId, internalSource, destination, invitee, connectionType);
    }

    @Override
    public String toString() {
        return "IntroductionResponse{" + super.toString() + "}";
    }

    public long getConnectionType() {
        return (long) get(CONNECTION_TYPE);
    }
}
