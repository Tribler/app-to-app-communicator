package org.tribler.app_to_appcommunicator;

import com.hypirion.bencode.BencodeReadException;

import org.tribler.app_to_appcommunicator.PEX.PexException;
import org.tribler.app_to_appcommunicator.PEX.UtPex;
import org.tribler.app_to_appcommunicator.PEX.UtPexHandshake;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Observable;

/**
 * Created by jaap on 5/3/16.
 */
public class PeerConnection extends Observable {
    final static int PEX_MESSAGE_ID = 1;
    private Socket socket;
    private boolean isIncoming;
    private boolean isConnected;
    private boolean isClosed;
    private UtPexHandshake pexHandshake;
    private UtPex pex;
    private List<Peer> peers;

    public PeerConnection(Socket socket, boolean isIncoming) {
        this.socket = socket;
        this.isIncoming = isIncoming;
        this.isConnected = false;
        this.isClosed = false;
        startUpdateThread();
    }

    private void startUpdateThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (isClosed != socket.isClosed()) {
                        isClosed = socket.isClosed();
                        onClosed();
                        break;
                    }

                    if (isConnected != socket.isConnected())
                        onConnected();
                    isConnected = socket.isConnected();

                    readInputStream();
                    notifyObservers();

                    if (hasDoneHandshake()) {
                        try {
                            System.out.println("Sending pex message");
                            sendPexMessage();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    private void onClosed() {
        setChanged();
        notifyObservers();
    }

    private void onConnected() {
        setChanged();
        try {
            sendPexHandshake();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void readInputStream() {
        try {
            if (socket.getInputStream().available() <= 0)
                return;

            if (hasDoneHandshake()) {
                System.out.println("Received pex");
                pex = UtPex.createFromStream(socket.getInputStream());
                System.out.println(pex);
                setChanged();
            } else {
                pexHandshake = UtPexHandshake.createFromStream(socket.getInputStream());
                System.out.println("Received pex message ID: " + pexHandshake.getMessageId());
                setChanged();
                while (socket.getInputStream().available() > 0) {
                    socket.getInputStream().read();
                }
            }
        } catch (IOException | PexException | BencodeReadException e) {
            e.printStackTrace();
        }

    }

    public boolean isClosed() {
        return isClosed;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void close() throws IOException {
        socket.close();
    }

    public Socket getSocket() {
        return socket;
    }

    public void sendPexHandshake() throws IOException {
        System.out.println("Sending PEX handshake");
        UtPexHandshake pexHandshake = new UtPexHandshake(PEX_MESSAGE_ID);
        pexHandshake.writeToStream(socket.getOutputStream());
    }

    public void sendPexMessage() throws IOException {
        if (peers == null) {
            System.out.println("Peers null, can't send");
            return;
        }
        System.out.println("Sending PEX");
        UtPex pex = new UtPex();
        for (Peer peer : peers) {
            pex.add(peer.getExternalAddress());
        }
        pex.writeToStream(socket.getOutputStream());
    }

    @Override
    public String toString() {
        return "PeerConnection{" +
                "socket=" + socket +
                ", isIncoming=" + isIncoming +
                ", isConnected=" + isConnected +
                ", isClosed=" + isClosed +
                '}';
    }

    public boolean isIncoming() {
        return isIncoming;
    }

    public UtPexHandshake getPexHandshake() {
        return pexHandshake;
    }


    public List<Peer> getPeers() {
        return peers;
    }

    public void setPeers(List<Peer> peers) {
        this.peers = peers;
    }

    public UtPex getPex() {
        return pex;
    }

    public boolean hasDoneHandshake() {
        return pexHandshake != null;
    }
}
