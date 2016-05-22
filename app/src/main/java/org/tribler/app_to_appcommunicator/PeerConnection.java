package org.tribler.app_to_appcommunicator;

import com.hypirion.bencode.BencodeReadException;

import org.tribler.app_to_appcommunicator.PEX.PexException;
import org.tribler.app_to_appcommunicator.PEX.PexHandshake;
import org.tribler.app_to_appcommunicator.PEX.PexListener;
import org.tribler.app_to_appcommunicator.PEX.PexMessage;

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
    private PexHandshake pexHandshake;
    private PexMessage pex;
    private PexListener pexListener;
    private boolean running;

    public PeerConnection(Socket socket, boolean isIncoming) {
        this.socket = socket;
        this.isIncoming = isIncoming;
        this.isConnected = false;
        this.isClosed = false;
        startUpdateThread();
    }

    public void setPexListener(PexListener pexListener) {
        this.pexListener = pexListener;
    }

    private void startUpdateThread() {
        running = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (running) {
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

    private void notifyPexListener() {
        if (pexListener != null) {
            pexListener.onPex(pex);
        }
    }

    private void readInputStream() {
        try {
            if (socket.getInputStream().available() <= 0)
                return;

            if (hasDoneHandshake()) {
                pex = PexMessage.createFromStream(socket.getInputStream());
                notifyPexListener();
                setChanged();
                while (socket.getInputStream().available() > 0) {
                    socket.getInputStream().read();
                }
            } else {
                pexHandshake = PexHandshake.createFromStream(socket.getInputStream());
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
        PexHandshake pexHandshake = new PexHandshake(PEX_MESSAGE_ID);
        pexHandshake.writeToStream(socket.getOutputStream());
    }

    public void sendPexMessage(List<Peer> peers) throws IOException {
        if (peers == null) {
            System.out.println("Peers null, can't send");
            return;
        }
        PexMessage pex = new PexMessage();
        for (Peer peer : peers) {
            pex.add(peer);
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

    public PexHandshake getPexHandshake() {
        return pexHandshake;
    }

    public PexMessage getPex() {
        return pex;
    }

    public boolean hasDoneHandshake() {
        return pexHandshake != null;
    }

    public void stop() {
        running = false;
        try {
            close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
