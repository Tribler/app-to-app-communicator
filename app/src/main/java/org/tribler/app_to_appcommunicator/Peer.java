package org.tribler.app_to_appcommunicator;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 * Created by jaap on 5/19/16.
 */
public class Peer implements Serializable {
    public final static boolean INCOMING = true;
    public final static boolean OUTGOING = false;

    final private static int TIMEOUT = 20000;
    private InetSocketAddress address;
    private String peerId;
    private boolean hasReceivedData = false;
    private boolean hasSentData = false;
    private boolean incoming;
    private int connectionType;
    private String networkOperator;
    private long lastSendTime;
    private long lastReceiveTime;
    private long creationTime;


    public Peer(String peerId, InetSocketAddress address, boolean incoming) {
        this.peerId = peerId;
        this.address = address;
        this.incoming = incoming;
        this.lastSendTime = System.currentTimeMillis();
        this.creationTime = System.currentTimeMillis();
    }

    public long getCreationTime() {
        return creationTime;
    }

    public String getNetworkOperator() {
        return networkOperator;
    }

    public void setNetworkOperator(String networkOperator) {
        this.networkOperator = networkOperator;
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

    public void setAddress(InetSocketAddress address) {
        this.address = address;
    }

    public void sentData() {
        hasSentData = true;
        lastSendTime = System.currentTimeMillis();
    }

    public void received(ByteBuffer buffer) {
        if (!hasSentData) {
            incoming = INCOMING;
        }
        hasReceivedData = true;
        lastReceiveTime = System.currentTimeMillis();
    }

    public boolean isAlive() {
        if (hasSentData) {
            if (System.currentTimeMillis() - lastSendTime < TIMEOUT) return true;
            return hasReceivedData && lastReceiveTime > lastSendTime;
        }
        return true;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Peer peer = (Peer) o;

        if (address != null ? !address.equals(peer.address) : peer.address != null) return false;
        return peerId != null ? peerId.equals(peer.peerId) : peer.peerId == null;

    }

    public boolean isHasSentData() {
        return hasSentData;
    }

    @Override
    public int hashCode() {
        int result = address != null ? address.hashCode() : 0;
        result = 31 * result + (peerId != null ? peerId.hashCode() : 0);
        return result;
    }

    public long getLastSendTime() {
        return lastSendTime;
    }

    public long getLastReceiveTime() {
        return lastReceiveTime;
    }
}
