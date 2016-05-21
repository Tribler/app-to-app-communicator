package org.tribler.app_to_appcommunicator;

import android.os.AsyncTask;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Socket;
import java.util.Observer;

/**
 * Created by jaap on 5/19/16.
 */
public class Peer {
    private PeerConnection peerConnection;
    private Inet4Address externalAddress;
    private Inet4Address internalAddress;
    private int port;
    private Observer connectionObserver;

    public Peer(int port, Inet4Address externalAddress) {
        this.port = port;
        this.externalAddress = externalAddress;
    }

    public Peer(PeerConnection peerConnection) {
        this.peerConnection = peerConnection;
        this.port = peerConnection.getSocket().getPort();
        this.externalAddress = (Inet4Address) peerConnection.getSocket().getInetAddress();
    }

    public void setConnectionObserver(Observer connectionObserver) {
        this.connectionObserver = connectionObserver;
        if (peerConnection != null)
            peerConnection.addObserver(connectionObserver);
    }

    public int getPort() {
        return port;
    }

    public PeerConnection getPeerConnection() {
        return peerConnection;
    }

    public void setPeerConnection(PeerConnection peerConnection) {
        this.peerConnection = peerConnection;
    }

    public Inet4Address getExternalAddress() {
        return externalAddress;
    }

    public Inet4Address getInternalAddress() {
        return internalAddress;
    }

    public void setInternalAddress(Inet4Address internalAddress) {
        this.internalAddress = internalAddress;
    }

    public boolean hasConnection() {
        return peerConnection != null;
    }

    public void connect() {
        new ConnectionTask().execute();
    }

    @Override
    public String toString() {
        return "Peer{" +
                "peerConnection=" + peerConnection +
                ", externalAddress=" + externalAddress +
                ", internalAddress=" + internalAddress +
                ", port=" + port +
                '}';
    }

    private class ConnectionTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                peerConnection = new PeerConnection(new Socket(externalAddress, port), false);
                if (connectionObserver != null)
                    peerConnection.addObserver(connectionObserver);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
