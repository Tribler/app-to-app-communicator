package org.tribler.app_to_appcommunicator.PEX;

import org.tribler.app_to_appcommunicator.Peer;

import java.io.IOException;
import java.util.List;

/**
 * Created by jaap on 5/22/16.
 */
public class PexSender {
    final static int INTERVAL_MS = 5000;
    private List<Peer> peers;
    private boolean running;

    public PexSender(List<Peer> peers) {
        this.peers = peers;
        startPexThread();
    }

    private void startPexThread() {
        running = true;
        System.out.println("Starting PEX sender");
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (running) {
                    try {
                        Thread.sleep(INTERVAL_MS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    for (Peer peer : peers) {
                        try {
                            if (peer.hasReceivedData()) {
                                sendPexToPeer(peer);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    private void sendPexToPeer(Peer peer) throws IOException {
//        if (peer.hasConnection()) {
//            peer.getPeerConnection().sendPexMessage(peers);
//        }
    }

    public void stop() {
        running = false;
    }

}
