package org.tribler.app_to_appcommunicator.connection.messages;

import org.tribler.app_to_appcommunicator.Peer;

import java.net.InetSocketAddress;
import java.util.Map;

/**
 * Created by jaap on 5/31/16.
 */
public class PunctureRequest extends Message {
    final private static String SOURCE = "source";
    final private static String PUNCTURE_PEER = "puncture_peer";

    public PunctureRequest(String peerId, InetSocketAddress destination, InetSocketAddress source, Peer puncturePeer) {
        super(PUNCTURE_REQUEST, peerId, destination);
        put(SOURCE, createAddressMap(source));
        put(PUNCTURE_PEER, createPeerMap(puncturePeer));
    }

    public static Message fromMap(Map map) throws MessageException {
        String peerId = (String) map.get(PEER_ID);
        InetSocketAddress destination = Message.createMapAddress((Map) map.get(DESTINATION));
        InetSocketAddress source = Message.createMapAddress((Map) map.get(SOURCE));
        Peer puncturePeer = Message.createMapPeer((Map) map.get(PUNCTURE_PEER));
        return new PunctureRequest(peerId, destination, source, puncturePeer);
    }

    public Peer getPuncturePeer() throws MessageException {
        return createMapPeer((Map) get(PUNCTURE_PEER));
    }

    @Override
    public String toString() {
        return "PunctureRequest{" + super.toString() + "}";
    }
}
