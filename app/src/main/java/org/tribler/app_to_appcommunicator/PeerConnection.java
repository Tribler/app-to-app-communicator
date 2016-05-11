package org.tribler.app_to_appcommunicator;

import com.hypirion.bencode.BencodeReadException;

import org.tribler.app_to_appcommunicator.PEX.PexException;
import org.tribler.app_to_appcommunicator.PEX.UtPex;
import org.tribler.app_to_appcommunicator.PEX.UtPexHandshake;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Socket;
import java.util.Collection;
import java.util.Observable;

/**
 * Created by jaap on 5/3/16.
 */
public class PeerConnection extends Observable {
    private Socket socket;
    private boolean isIncoming;
    final static int PEX_MESSAGE_ID = 1;
    private boolean isConnected;
    private boolean isClosed;
    private UtPexHandshake pexHandshake;
    private UtPex pex;


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
                        onClosed();
                        isClosed = socket.isClosed();
                        break;
                    }

                    if (isConnected != socket.isConnected())
                        onConnected();
                    isConnected = socket.isConnected();

                    readInputStream();
                }
            }
        }).start();
    }

    private void onClosed() {
        System.out.println("Closed");
    }

    private void onConnected() {
        System.out.println("Connected");
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
            } else {
                System.out.println("Received pex handshake");
                pexHandshake = UtPexHandshake.createFromStream(socket.getInputStream());
                System.out.println("Received pex message ID: " + pexHandshake.getMessageId());
                while (socket.getInputStream().available() >= 0) {
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

    public void sendPexMessage(Collection<? extends Inet4Address> peers) throws IOException {
        System.out.println("Sending PEX");
        UtPex pex = new UtPex();
        pex.addAll(peers);
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

    public UtPex getPex() {
        return pex;
    }

    public boolean hasDoneHandshake() {
        return pexHandshake != null;
    }
}
