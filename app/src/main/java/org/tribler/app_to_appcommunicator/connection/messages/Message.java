package org.tribler.app_to_appcommunicator.connection.messages;

import com.hypirion.bencode.BencodeReadException;
import com.hypirion.bencode.BencodeReader;
import com.hypirion.bencode.BencodeWriter;

import org.tribler.app_to_appcommunicator.Peer;
import org.tribler.app_to_appcommunicator.connection.ByteBufferOutputStream;
import org.tribler.app_to_appcommunicator.connection.ByteBufferinputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jaap on 5/31/16.
 */
public abstract class Message extends HashMap {
    public final static int INTRODUCTION_REQUEST = 1;
    public final static int INTRODUCTION_RESPONSE = 2;
    public final static int PUNCTURE_REQUEST = 3;
    public final static int PUNCTURE = 4;

    final protected static String TYPE = "type";
    final protected static String DESTINATION = "destination";

    final protected static String PORT = "port";
    final protected static String ADDRESS = "address";
    final protected static String PEER_ID = "peer_id";

    public Message(int type, String peerId, InetSocketAddress destination) {
        put(TYPE, type);
        put(PEER_ID, peerId);
        put(DESTINATION, createAddressMap(destination));
    }

    public static Message createFromStream(InputStream stream) throws IOException, BencodeReadException, MessageException {
        BencodeReader reader = new BencodeReader(stream);
        Map<String, Object> dict = reader.readDict();
        if (!dict.containsKey(TYPE)) {
            System.out.println("Dictionary " + dict + " doesn't contain type");
            throw new MessageException("Invalid message");
        }
        int messageType = (int) (long) dict.get(TYPE);
        switch (messageType) {
            case INTRODUCTION_REQUEST:
                return IntroductionRequest.fromMap(dict);
            case INTRODUCTION_RESPONSE:
                return IntroductionResponse.fromMap(dict);
            case PUNCTURE:
                return Puncture.fromMap(dict);
            case PUNCTURE_REQUEST:
                return PunctureRequest.fromMap(dict);
            default:
                throw new MessageException("Unknown message");
        }
    }

    public static Message createFromByteBuffer(ByteBuffer buffer) throws BencodeReadException, IOException, MessageException {
        return createFromStream(new ByteBufferinputStream(buffer));
    }

    public static Map createAddressMap(InetSocketAddress address) {
        Map map = new HashMap();
        map.put(PORT, (long) address.getPort());
        map.put(ADDRESS, address.getAddress().getHostAddress());
        return map;
    }

    public static Map createPeerMap(Peer peer) {
        Map map = new HashMap();
        InetSocketAddress address = peer.getAddress();
        map.put(PORT, (long) address.getPort());
        map.put(ADDRESS, address.getAddress().getHostAddress());
        if (peer.getPeerId() != null) map.put(PEER_ID, peer.getPeerId());
        return map;
    }

    public static InetSocketAddress createMapAddress(Map map) throws MessageException {
        if (!map.containsKey(PORT) || !map.containsKey(ADDRESS)) throw new MessageException("Invalid address map");

        int port = (int) (long) map.get(PORT);
        String address = (String) map.get(ADDRESS);
        return new InetSocketAddress(address, port);
    }

    public static Peer createMapPeer(Map map) throws MessageException {
        if (!map.containsKey(PORT) || !map.containsKey(ADDRESS)) throw new MessageException("Invalid address map");

        int port = (int) (long) map.get(PORT);
        String address = (String) map.get(ADDRESS);
        String peerId = null;
        if (map.containsKey(PEER_ID)) peerId = (String) map.get(PEER_ID);
        return new Peer(peerId, new InetSocketAddress(address, port), false);
    }

    public InetSocketAddress getDestination() throws MessageException {
        return createMapAddress((Map) get(DESTINATION));
    }

    public void writeToStream(OutputStream out) throws IOException {
        BencodeWriter writer = new BencodeWriter(out);
        writer.write(this);
    }

    public void writeToByteBuffer(ByteBuffer buffer) throws IOException {
        buffer.clear();
        writeToStream(new ByteBufferOutputStream(buffer));
    }

    public String getPeerId() {
        return (String) get(PEER_ID);
    }

    public int getType() {
        return (int) get(TYPE);
    }
}
