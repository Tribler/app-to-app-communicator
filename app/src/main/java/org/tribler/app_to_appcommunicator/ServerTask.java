package org.tribler.app_to_appcommunicator;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by jaap on 4/26/16.
 */
public class ServerTask {

    private int port;
    private ServerSocket serverSocket;
    private ServerConnectionListener serverConnectionListener;
    private Thread serverThread;
    private boolean running;

    public ServerTask(int port, ServerConnectionListener serverConnectionListener) {
        this.port = port;
        this.serverConnectionListener = serverConnectionListener;
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public boolean start() {
        if (serverThread != null) {
            return false;
        }
        running = true;
        serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    serverSocket = new ServerSocket(port);
                    while (running) {
                        Socket socket = serverSocket.accept();
                        PeerConnection connection = new PeerConnection(socket, true);
//                        Peer peer = new Peer(connection);
//                        if (serverConnectionListener != null)
//                            serverConnectionListener.onConnection(peer);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        serverThread.start();
        return true;
    }

    public void stop() {
        running = false;
    }

    public interface ServerConnectionListener {
        void onConnection(Peer peer);
    }

}
