package org.tribler.app_to_appcommunicator;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Patterns;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.hypirion.bencode.BencodeReadException;

import org.tribler.app_to_appcommunicator.PEX.PexException;
import org.tribler.app_to_appcommunicator.PEX.WanVote;
import org.tribler.app_to_appcommunicator.PEX.messages.IntroductionRequest;
import org.tribler.app_to_appcommunicator.PEX.messages.IntroductionResponse;
import org.tribler.app_to_appcommunicator.PEX.messages.Message;
import org.tribler.app_to_appcommunicator.PEX.messages.MessageException;
import org.tribler.app_to_appcommunicator.PEX.messages.Puncture;
import org.tribler.app_to_appcommunicator.PEX.messages.PunctureRequest;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class OverviewActivity extends AppCompatActivity {

    //    public final static String CONNECTABLE_ADDRESS = "130.161.211.254";
    public final static String CONNECTABLE_ADDRESS = "84.80.46.152";
    final static int DEFAULT_PORT = 1873;
    private static final int BUFFER_SIZE = 1024;
    private TextView mWanVote;
    private PeerListAdapter incomingPeerAdapter;
    private PeerListAdapter outgoingPeerAdapter;
    private DatagramChannel channel;

    private List<Peer> peerList;
    private List<Peer> incomingList;
    private List<Peer> outgoingList;
    private boolean running;
    private String hashId;
    private WanVote wanVote;
    private int connectionType;
    private ByteBuffer outBuffer;
    private InetSocketAddress internalSourceAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overview);

        peerList = new ArrayList<>();
        incomingList = new ArrayList<>();
        outgoingList = new ArrayList<>();
        hashId = generateHash();
        ((TextView) findViewById(R.id.peer_id)).setText(hashId.toString().substring(0, 4));
        wanVote = new WanVote();
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        connectionType = cm.getActiveNetworkInfo().getType();
        outBuffer = ByteBuffer.allocate(BUFFER_SIZE);

        mWanVote = (TextView) findViewById(R.id.wanvote);

        ListView incomingPeerConnectionListView = (ListView) findViewById(R.id.incoming_peer_connection_list_view);
        ListView outgoingPeerConnectionListView = (ListView) findViewById(R.id.outgoing_peer_connection_list_view);
        incomingPeerAdapter = new PeerListAdapter(getApplicationContext(), R.layout.peer_connection_list_item, incomingList, Peer.INCOMING);
        incomingPeerConnectionListView.setAdapter(incomingPeerAdapter);
        outgoingPeerAdapter = new PeerListAdapter(getApplicationContext(), R.layout.peer_connection_list_item, outgoingList, Peer.OUTGOING);
        outgoingPeerConnectionListView.setAdapter(outgoingPeerAdapter);

        try {
            addPeer(null, new InetSocketAddress(InetAddress.getByName(CONNECTABLE_ADDRESS), DEFAULT_PORT), Peer.OUTGOING);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        running = true;
        showLocalIpAddress();
        startListenThread();
        startSendThread();
    }

    private String generateHash() {
        return UUID.randomUUID().toString();
    }

    private void startSendThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (running) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    try {
                        if (peerList.size() > 0) {
                            Peer peer = getRandomPeerExcluding(null);
                            sendIntroductionRequest(peer.getAddress());
                            peer.sentData();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
        System.out.println("Thread started");
    }

    private void sendIntroductionRequest(InetSocketAddress address) throws IOException {
        IntroductionRequest request = new IntroductionRequest(hashId, address, connectionType);
        sendMesssage(request, address);
    }

    private void sendPunctureRequest(InetSocketAddress address, Peer puncturePeer) throws IOException {
        PunctureRequest request = new PunctureRequest(hashId, address, internalSourceAddress, puncturePeer.getAddress());
        sendMesssage(request, address);
    }

    private void sendPuncture(InetSocketAddress address) throws IOException {
        Puncture puncture = new Puncture(hashId, address, internalSourceAddress);
        sendMesssage(puncture, address);
    }

    private List<InetSocketAddress> getPexPeer() {
        List<InetSocketAddress> pexPeers = new ArrayList<>();
        for (Peer peer : peerList) {
            if (peer.hasReceivedData()) {
                pexPeers.add(peer.getAddress());
            }
        }
        return pexPeers;
    }

    private void sendIntroductionResponse(InetSocketAddress address, Peer invitee) throws IOException {
        IntroductionResponse response = new IntroductionResponse(hashId, internalSourceAddress, address,
                invitee.getAddress(), connectionType, getPexPeer());
        sendMesssage(response, address);
    }

    private void sendMesssage(Message message, InetSocketAddress address) throws IOException {
        System.out.println("Sending " + message);
        outBuffer.clear();
        message.writeToByteBuffer(outBuffer);
        outBuffer.flip();
        channel.send(outBuffer, address);
    }

    private Peer getRandomPeerExcluding(Peer peer) {
        Random random = new Random();
        int i;
        while (true) {
            i = random.nextInt(peerList.size());
            if (!peerList.get(i).equals(peer)) {
                return peerList.get(i);
            }
        }
    }

    private void startListenThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    channel = DatagramChannel.open();
                    channel.socket().bind(new InetSocketAddress(DEFAULT_PORT));
                    ByteBuffer inputBuffer = ByteBuffer.allocate(BUFFER_SIZE);
                    while (running) {
                        inputBuffer.clear();
                        SocketAddress address = channel.receive(inputBuffer);
                        inputBuffer.flip();
                        dataReceived(inputBuffer, (InetSocketAddress) address);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        System.out.println("Thread started");
    }

    private Peer getOrMakeIncomingPeer(String id, InetSocketAddress address) {
        if (id != null) {
            for (Peer peer : peerList) {
                if (id.equals(peer.getPeerId())) {
                    return peer;
                }
            }
        }
        for (Peer peer : peerList) {
            if (peer.getAddress().equals(address)) {
                peer.setPeerId(id);
                return peer;
            }
        }
        return addPeer(id, address, Peer.INCOMING);
    }

    private boolean peerExists(String id) {
        if (id == null)
            return false;
        for (Peer peer : peerList) {
            if (id.equals(peer.getPeerId())) {
                return true;
            }
        }
        return false;
    }


    private void dataReceived(ByteBuffer data, InetSocketAddress address) {
        try {
            Message message = Message.createFromByteBuffer(data);
            System.out.println("Received " + message);
            String id = message.getPeerId();
            if (!peerExists(id)) {
                wanVote.vote(message.getDestination());
                setWanvote(wanVote.getAddress().toString());
            }
            Peer peer = getOrMakeIncomingPeer(id, address);
            peer.received(data);
            switch (message.getType()) {
                case Message.INTRODUCTION_REQUEST:
                    handlIntroductionRequest(peer, (IntroductionRequest) message);
                    break;
                case Message.INTRODUCTION_RESPONSE:
                    handleIntroductionResponse(peer, (IntroductionResponse) message);
                    break;
                case Message.PUNCTURE:
                    handlePuncture(peer, (Puncture) message);
                    break;
                case Message.PUNCTURE_REQUEST:
                    handlePunctureRequest(peer, (PunctureRequest) message);
                    break;
            }
            updatePeerLists();
        } catch (BencodeReadException | PexException | IOException | MessageException e) {
            e.printStackTrace();
        }
    }

    private void handlIntroductionRequest(Peer peer, IntroductionRequest message) throws IOException {
        if (peerList.size() > 1) {
            Peer invitee = getRandomPeerExcluding(peer);
            sendIntroductionResponse(peer.getAddress(), invitee);
            sendPunctureRequest(invitee.getAddress(), peer);
            System.out.println("Introducing " + invitee.getAddress() + " to " + peer.getAddress());
        } else {
            System.out.println("Peerlist too small, can't handle introduction request");
        }
    }

    private void handleIntroductionResponse(Peer peer, IntroductionResponse message) {
        peer.setConnectionType((int) message.getConnectionType());
        List<InetSocketAddress> pex = message.getPex();
        for (InetSocketAddress address : pex) {
            addPeer(null, address, false);
        }
    }

    private void handlePuncture(Peer peer, Puncture message) throws IOException {
//        if (peer.isIncoming())
//            sendPuncture(peer.getAddress());
    }

    private void handlePunctureRequest(Peer peer, PunctureRequest message) throws IOException, MessageException {
        sendPuncture(message.getPuncturePeer());
    }

    private void showLocalIpAddress() {
        new AsyncTask<Void, Void, InetAddress>() {

            @Override
            protected InetAddress doInBackground(Void... params) {
                try {
                    for (Enumeration en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                        NetworkInterface intf = (NetworkInterface) en.nextElement();
                        for (Enumeration enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                            InetAddress inetAddress = (InetAddress) enumIpAddr.nextElement();
                            if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                                return inetAddress;
                            }
                        }
                    }
                } catch (SocketException ex) {
                    ex.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(InetAddress inetAddress) {
                super.onPostExecute(inetAddress);
                internalSourceAddress = new InetSocketAddress(inetAddress, DEFAULT_PORT);
                System.out.println("Local ip: " + inetAddress);
                TextView localIp = (TextView) findViewById(R.id.local_ip_address_view);
                localIp.setText(inetAddress.toString());
            }
        }.execute();
    }

    private void setWanvote(final String status) {
        new Handler(getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mWanVote.setText(status);
            }
        });
    }

    private void showToast(final String toast) {
        new Handler(getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), toast, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Peer addPeer(String peerId, InetSocketAddress address, boolean incoming) {
        for (Peer peer : peerList) {
            if (peer.getAddress().equals(address))
                return peer;
        }
        final Peer peer = new Peer(peerId, address, incoming);
        new Handler(Looper.getMainLooper()).post(new Runnable() {

            @Override
            public void run() {
                peerList.add(peer);
                incomingPeerAdapter.notifyDataSetChanged();
                outgoingPeerAdapter.notifyDataSetChanged();
                System.out.println("Added " + peer);
            }
        });
        return peer;
    }

    private void updatePeerLists() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                splitPeerList();
                incomingPeerAdapter.notifyDataSetChanged();
                outgoingPeerAdapter.notifyDataSetChanged();
            }
        });
    }

    private void splitPeerList() {
        incomingList.clear();
        outgoingList.clear();
        for (Peer peer : peerList) {
            if (peer.isIncoming()) {
                incomingList.add(peer);
            } else {
                outgoingList.add(peer);
            }
        }
    }

    private boolean isValidIp(String s) {
        return Patterns.IP_ADDRESS.matcher(s).matches();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }


}
