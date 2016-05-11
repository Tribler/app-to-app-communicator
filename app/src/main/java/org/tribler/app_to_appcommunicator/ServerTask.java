package org.tribler.app_to_appcommunicator;

import java.io.IOException;
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
        serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    serverSocket = new ServerSocket(port);
                    while (true) {
                        Socket socket = serverSocket.accept();
                        PeerConnection connection = new PeerConnection(socket, true);
                        if (serverConnectionListener != null)
                            serverConnectionListener.onConnection(connection);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        serverThread.start();
        return true;
    }

    public void stop() throws IOException {
        if (serverSocket != null && !serverSocket.isClosed())
            serverSocket.close();
    }

    public interface ServerConnectionListener {
        void onConnection(PeerConnection connection);
    }

}
