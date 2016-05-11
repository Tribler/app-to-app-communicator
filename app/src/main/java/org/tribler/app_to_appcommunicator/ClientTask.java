package org.tribler.app_to_appcommunicator;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * Created by jaap on 4/26/16.
 */
public class ClientTask {
    private String address;
    private int port;
    private ClientConnectionCallback clientConnectionListener = null;
    private Socket socket = null;
    private Thread clientThread;

    public ClientTask(String address, int port) {
        this.address = address;
        this.port = port;
    }

    public ClientTask(String address, int port, ClientConnectionCallback clientConnectionListener) {
        this.address = address;
        this.port = port;
        this.clientConnectionListener = clientConnectionListener;
    }

    public boolean start() {
        if (clientThread != null) {
            return false;
        }
        clientThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket(address, port);
                    if (clientConnectionListener != null) {
                        PeerConnection connection = new PeerConnection(socket, false);
                        if (clientConnectionListener != null)
                            clientConnectionListener.onConnection(connection);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        clientThread.start();

        return true;
    }

    public interface ClientConnectionCallback {
        void onConnection(PeerConnection connection);
    }
}
