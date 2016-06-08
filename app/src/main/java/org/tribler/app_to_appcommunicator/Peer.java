package org.tribler.app_to_appcommunicator;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 * Created by jaap on 5/19/16.
 */
public class Peer {
    public final static boolean INCOMING = true;
    public final static boolean OUTGOING = false;
    private InetSocketAddress address;
    private String peerId;
    private boolean hasReceivedData = false;
    private boolean incoming;
    private int connectionType;

    final private static int TIMEOUT = 20000;
    private long lastSendTime;

    public Peer(String peerId, InetSocketAddress address, boolean incoming) {
        this.peerId = peerId;
        this.address = address;
        this.incoming = incoming;
        this.lastSendTime = System.currentTimeMillis();
    }

    public int getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(int connectionType) {
        this.connectionType = connectionType;
    }

    public boolean isIncoming() {
        return incoming;
    }

    public boolean isOutgoing() {
        return !incoming;
    }

    public String getPeerId() {
        return peerId;
    }

    public void setPeerId(String peerId) {
        this.peerId = peerId;
    }

    public boolean hasReceivedData() {
        return hasReceivedData;
    }

    public int getPort() {
        return address.getPort();
    }

    public InetAddress getExternalAddress() {
        return address.getAddress();
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public void sentData() {
        lastSendTime = System.currentTimeMillis();
    }

    public boolean isInactive() {
        return (!hasReceivedData && (System.currentTimeMillis() - lastSendTime) > TIMEOUT);
    }

    @Override
    public String toString() {
        return "Peer{" +
                "address=" + address +
                ", peerId='" + peerId + '\'' +
                ", hasReceivedData=" + hasReceivedData +
                ", incoming=" + incoming +
                ", connectionType=" + connectionType +
                '}';
    }

    public void received(ByteBuffer buffer) {
        hasReceivedData = true;
    }
}
