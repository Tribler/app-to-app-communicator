package org.tribler.app_to_appcommunicator;

import android.os.AsyncTask;

import com.hypirion.bencode.BencodeReadException;

import org.tribler.app_to_appcommunicator.PEX.PexException;
import org.tribler.app_to_appcommunicator.PEX.PexHandshake;
import org.tribler.app_to_appcommunicator.PEX.PexMessage;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Observer;
import java.util.Random;

/**
 * Created by jaap on 5/19/16.
 */
public class Peer {
    final static int DEFAULT_PORT = 1873;
    private static final int BUFFER_SIZE = 1024;
    private InetSocketAddress address;
    private PexHandshake pexHandshake;
    private Observer connectionObserver;
    private DatagramChannel channel;
    private boolean hasReceivedData = false;

    public Peer(InetSocketAddress address, DatagramChannel channel) {
        this.address = address;
        this.channel = channel;
    }

    public boolean hasReceivedData() {
        return hasReceivedData;
    }

    public PexHandshake getPexHandshake() {
        return pexHandshake;
    }

    public boolean hasDoneHandshake() {
        return pexHandshake != null;
    }

    public int getPort() {
        return address.getPort();
    }

    public InetAddress getExternalAddress() {
        return address.getAddress();
    }

    public void connect() {
//        new ConnectionTask().execute();
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    @Override
    public String toString() {
        return "Peer{" +
                "address=" + address +
                ", pexHandshake=" + pexHandshake +
                ", connectionObserver=" + connectionObserver +
                ", hasReceivedData=" + hasReceivedData +
                '}';
    }

    public void received(ByteBuffer buffer) {
        hasReceivedData = true;
        try {
            PexMessage message;
            message = PexMessage.createFromByteBuffer(buffer);
            System.out.println("Received PEX message: " + message);
        } catch (BencodeReadException | PexException | IOException e) {
            e.printStackTrace();
        }
    }

    public void sendPex(PexMessage pex) {
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        try {
            pex.writeToByteBuffer(buffer);
            buffer.flip();
            channel.send(buffer, address);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ConnectionTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Random random = new Random();
                ByteBuffer buf = ByteBuffer.allocate(BUFFER_SIZE);
                while (true) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    buf.clear();
                    for (int i = 0; i < random.nextInt(4); i++) {
                        buf.put((byte) (80 + random.nextInt(100)));
                    }
                    buf.flip();
                    int bytesSent = channel.send(buf, address);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
