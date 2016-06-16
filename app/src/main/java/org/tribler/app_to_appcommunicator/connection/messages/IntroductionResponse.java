package org.tribler.app_to_appcommunicator.connection.messages;

import org.tribler.app_to_appcommunicator.Peer;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by jaap on 5/31/16.
 */
public class IntroductionResponse extends Message {
    final private static String NETWORK_OPERATOR = "network_operator";
    final private static String CONNECTION_TYPE = "connection_type";
    final private static String INTERNAL_SOURCE = "internal_source";
    final private static String INVITEE = "invitee";
    final private static String PEX = "pex";

    private List<Peer> pex;

    public IntroductionResponse(String peerId, InetSocketAddress internalSource, InetSocketAddress destination, Peer invitee,
                                long connectionType, List<Peer> pex, String networkOperator) {
        super(INTRODUCTION_RESPONSE, peerId, destination);
        this.pex = pex;
        put(CONNECTION_TYPE, connectionType);
        put(INTERNAL_SOURCE, createAddressMap(internalSource));
        put(INVITEE, createPeerMap(invitee));
        put(NETWORK_OPERATOR, networkOperator);
        List<Map> pexMap = new ArrayList<>();
        for (Peer peer : pex) {
            pexMap.add(createPeerMap(peer));
        }
        put(PEX, pexMap);
    }

    public static Message fromMap(Map map) throws MessageException {
        String peerId = (String) map.get(PEER_ID);
        InetSocketAddress internalSource = Message.createMapAddress((Map) map.get(INTERNAL_SOURCE));
        InetSocketAddress destination = Message.createMapAddress((Map) map.get(DESTINATION));
        Peer invitee = Message.createMapPeer((Map) map.get(INVITEE));
        long connectionType = (long) map.get(CONNECTION_TYPE);
        String networkOperator = (String) map.get(NETWORK_OPERATOR);
        List<Map> pexMaps = (List<Map>) map.get(PEX);
        List<Peer> pex = new ArrayList<>();
        for (Map m : pexMaps) {
            pex.add(Message.createMapPeer(m));
        }
        return new IntroductionResponse(peerId, internalSource, destination, invitee, connectionType, pex, networkOperator);
    }

    public String getNetworkOperator() {
        return (String) get(NETWORK_OPERATOR);
    }

    public List<Peer> getPex() {
        return pex;
    }

    @Override
    public String toString() {
        return "IntroductionResponse{" + super.toString() + "}";
    }

    public long getConnectionType() {
        return (long) get(CONNECTION_TYPE);
    }
}
