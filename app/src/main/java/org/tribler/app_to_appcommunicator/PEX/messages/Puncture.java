package org.tribler.app_to_appcommunicator.PEX.messages;

import java.net.InetSocketAddress;
import java.util.Map;

/**
 * Created by jaap on 5/31/16.
 */
public class Puncture extends Message {
    final private static String SOURCE = "source";

    public Puncture(String peerId, InetSocketAddress destination, InetSocketAddress source) {
        super(PUNCTURE, peerId, destination);
        put(SOURCE, createAddressMap(source));
    }

    public static Message fromMap(Map map) throws MessageException {
        String peerId = (String) map.get(PEER_ID);
        InetSocketAddress source = Message.createMapAddress((Map) map.get(SOURCE));
        InetSocketAddress destination = Message.createMapAddress((Map) map.get(DESTINATION));
        return new Puncture(peerId, destination, source);
    }

    public String toString() {
        return "Puncture{" + super.toString() + "}";
    }
}
