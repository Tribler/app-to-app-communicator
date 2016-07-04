package org.tribler.app_to_appcommunicator;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Patterns;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.hypirion.bencode.BencodeReadException;

import org.tribler.app_to_appcommunicator.connection.WanVote;
import org.tribler.app_to_appcommunicator.connection.messages.IntroductionRequest;
import org.tribler.app_to_appcommunicator.connection.messages.IntroductionResponse;
import org.tribler.app_to_appcommunicator.connection.messages.Message;
import org.tribler.app_to_appcommunicator.connection.messages.MessageException;
import org.tribler.app_to_appcommunicator.connection.messages.Puncture;
import org.tribler.app_to_appcommunicator.connection.messages.PunctureRequest;

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

    public final static String CONNECTABLE_ADDRESS = "130.161.211.254";
    //            public final static String CONNECTABLE_ADDRESS = "84.80.46.152";
    final static int UNKNOWN_PEER_LIMIT = 20;
    final static String HASH_ID = "hash_id";
    final static int DEFAULT_PORT = 1873;
    final static int KNOWN_PEER_LIMIT = 10;
    private static final int BUFFER_SIZE = 2048;
    private TextView mWanVote;
    private TextView mActivePeers;
    private TextView mConnectablePeers;
    private TextView mConnectableRatio;
    private PeerListAdapter incomingPeerAdapter;
    private PeerListAdapter outgoingPeerAdapter;
    private DatagramChannel channel;
    private boolean updatePending = false;

    private ArrayList<Peer> peerList;
    private List<Peer> incomingList;
    private List<Peer> outgoingList;
    private boolean running;
    private String hashId;
    private String networkOperator;
    private WanVote wanVote;
    private int connectionType;
    private ByteBuffer outBuffer;
    private InetSocketAddress internalSourceAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overview);

        TelephonyManager telephonyManager = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE));
        networkOperator = telephonyManager.getNetworkOperatorName();
        if (savedInstanceState != null) {
            peerList = (ArrayList<Peer>) savedInstanceState.getSerializable("peers");
            System.out.println("New peerlist: " + peerList);
        } else {
            peerList = new ArrayList<>();
        }
        incomingList = new ArrayList<>();
        outgoingList = new ArrayList<>();
        hashId = getId();
        ((TextView) findViewById(R.id.peer_id)).setText(hashId.toString().substring(0, 4));
        wanVote = new WanVote();
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            connectionType = cm.getActiveNetworkInfo().getType();
        } catch (Exception e) {
            showToast("Can't connect: no active network");
            return;
        }
        ((TextView) findViewById(R.id.connection_type)).setText(cm.getActiveNetworkInfo().getTypeName() + " " + cm.getActiveNetworkInfo().getSubtypeName());
        outBuffer = ByteBuffer.allocate(BUFFER_SIZE);

        mWanVote = (TextView) findViewById(R.id.wanvote);
        mActivePeers = (TextView) findViewById(R.id.active_peers);
        mConnectablePeers = (TextView) findViewById(R.id.connectable_peers);
        mConnectableRatio = (TextView) findViewById(R.id.connectable_ratio);

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
        if (savedInstanceState != null) {
            updatePeerLists();
        }
    }

    private String getId() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String id = preferences.getString(HASH_ID, null);
        if (id == null) {
            System.out.println("Generating new ID");
            id = generateHash();
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(HASH_ID, id);
            editor.apply();
        }
        return id;
    }

    private String generateHash() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private void startSendThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                do {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    try {
                        if (peerList.size() > 0) {
                            Peer peer = getRandomPeerExcluding(null);
                            if (peer != null) {
                                sendIntroductionRequest(peer);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } while (running);
                System.out.println("Send thread stopped");
            }
        }).start();
        System.out.println("Thread started");
    }

    private void sendIntroductionRequest(Peer peer) throws IOException {
        IntroductionRequest request = new IntroductionRequest(hashId, peer.getAddress(), connectionType, networkOperator);
        sendMesssage(request, peer);
    }

    private void sendPunctureRequest(Peer peer, Peer puncturePeer) throws IOException {
        PunctureRequest request = new PunctureRequest(hashId, peer.getAddress(), internalSourceAddress, puncturePeer);
        sendMesssage(request, peer);
    }

    private void sendPuncture(Peer peer) throws IOException {
        Puncture puncture = new Puncture(hashId, peer.getAddress(), internalSourceAddress);
        sendMesssage(puncture, peer);
    }

    private void sendIntroductionResponse(Peer p, Peer invitee) throws IOException {
        List<Peer> pexPeers = new ArrayList<>();
        for (Peer peer : peerList) {
            if (peer.hasReceivedData() && peer.getPeerId() != null && peer.isAlive())
                pexPeers.add(peer);
        }
        IntroductionResponse response = new IntroductionResponse(hashId, internalSourceAddress, p.getAddress(), invitee, connectionType, pexPeers, networkOperator);
        sendMesssage(response, p);
    }

    private synchronized void sendMesssage(Message message, Peer peer) throws IOException {
        System.out.println("Sending " + message);
        outBuffer.clear();
        message.writeToByteBuffer(outBuffer);
        outBuffer.flip();
        channel.send(outBuffer, peer.getAddress());
        peer.sentData();
    }

    private Peer getRandomPeerExcluding(Peer excludePeer) {
        List<Peer> eligiblePeers = new ArrayList<>();
        for (Peer p : peerList) {
            if (p.isAlive() && !p.equals(excludePeer)) {
                eligiblePeers.add(p);
            }
        }
        if (eligiblePeers.size() == 0) {
            System.out.println("No elegible peers!");
            return null;
        }
        Random random = new Random();
        return eligiblePeers.get(random.nextInt(eligiblePeers.size()));
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
                    System.out.println("Listen thread stopped");
                }
            }
        }).start();
        System.out.println("Listen thread started");
    }

    private Peer getOrMakePeer(String id, InetSocketAddress address, boolean incoming) {
        if (id != null) {
            for (Peer peer : peerList) {
                if (id.equals(peer.getPeerId())) {
                    if (!address.equals(peer.getAddress())) {
                        System.out.println("Peer address differs from known address");
                        peer.setAddress(address);
                        removeDuplicates();
                    }
                    return peer;
                }
            }
        }
        for (Peer peer : peerList) {
            if (peer.getAddress().equals(address)) {
                if (id != null) peer.setPeerId(id);
                return peer;
            }
        }
        return addPeer(id, address, incoming);
    }

    private void removeDuplicates() {
        for (int i = 0; i < peerList.size(); i++) {
            Peer p1 = peerList.get(i);
            for (int j = 0; j < peerList.size(); j++) {
                Peer p2 = peerList.get(j);
                if (j != i && p1.getPeerId() != null && p1.getPeerId().equals(p2.getPeerId())) {
                    peerList.remove(p2);
                }
            }
        }
    }

    private boolean peerExists(String id) {
        if (id == null) return false;
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
            Peer peer = getOrMakePeer(id, address, Peer.INCOMING);
            if (peer == null) return;
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
        } catch (BencodeReadException | IOException | MessageException e) {
            e.printStackTrace();
        }
    }

    private void handlIntroductionRequest(Peer peer, IntroductionRequest message) throws IOException {
        peer.setNetworkOperator(message.getNetworkOperator());
        peer.setConnectionType((int) message.getConnectionType());
        if (peerList.size() > 1) {
            Peer invitee = getRandomPeerExcluding(peer);
            if (invitee != null) {
                sendIntroductionResponse(peer, invitee);
                sendPunctureRequest(invitee, peer);
                System.out.println("Introducing " + invitee.getAddress() + " to " + peer.getAddress());
            }
        } else {
            System.out.println("Peerlist too small, can't handle introduction request");
        }
    }

    private void handleIntroductionResponse(Peer peer, IntroductionResponse message) {
        peer.setConnectionType((int) message.getConnectionType());
        peer.setNetworkOperator(message.getNetworkOperator());
        List<Peer> pex = message.getPex();
        for (Peer pexPeer : pex) {
            if (hashId.equals(pexPeer.getPeerId())) continue;
            getOrMakePeer(pexPeer.getPeerId(), pexPeer.getAddress(), Peer.OUTGOING);
        }
    }

    private void handlePuncture(Peer peer, Puncture message) throws IOException {
    }

    private void handlePunctureRequest(Peer peer, PunctureRequest message) throws IOException, MessageException {
        if (!peerExists(message.getPuncturePeer().getPeerId()))
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
        if (hashId.equals(peerId)) {
            System.out.println("Not adding self");
            return null;
        }
        if (wanVote.getAddress() != null && wanVote.getAddress().equals(address)) {
            System.out.println("Not adding peer with same address as wanVote");
            return null;
        }
        for (Peer peer : peerList) {
            if (peer.getPeerId() != null && peer.getPeerId().equals(peerId)) return peer;
            if (peer.getAddress().equals(address)) return peer;
        }
        final Peer peer = new Peer(peerId, address, incoming);
        if (incoming) {
            showToast("New incoming peer from " + peer.getAddress());
        }
        new Handler(Looper.getMainLooper()).post(new Runnable() {

            @Override
            public void run() {
                peerList.add(peer);
                trimPeers();
                splitPeerList();
                incomingPeerAdapter.notifyDataSetChanged();
                outgoingPeerAdapter.notifyDataSetChanged();
                System.out.println("Added " + peer);
            }
        });
        return peer;
    }

    private void trimPeers() {
        limitKnownPeers(KNOWN_PEER_LIMIT);
        limitUnknownPeers(UNKNOWN_PEER_LIMIT);
    }

    private void limitKnownPeers(int limit) {
        if (peerList.size() < limit) return;
        int knownPeers = 0;
        Peer oldestPeer = null;
        long oldestDate = System.currentTimeMillis();
        for (Peer peer : peerList) {
            if (peer.hasReceivedData()) {
                knownPeers++;
                if (peer.getCreationTime() < oldestDate) {
                    oldestDate = peer.getCreationTime();
                    oldestPeer = peer;
                }
            }
        }
        if (knownPeers > limit) {
            peerList.remove(oldestPeer);
        }
        if (knownPeers - 1 > limit) {
            limitKnownPeers(limit);
        }
    }

    private void limitUnknownPeers(int limit) {
        if (peerList.size() < limit) return;
        int unknownPeers = 0;
        Peer oldestPeer = null;
        long oldestDate = System.currentTimeMillis();
        for (Peer peer : peerList) {
            if (!peer.hasReceivedData()) {
                unknownPeers++;
                if (peer.getCreationTime() < oldestDate) {
                    oldestDate = peer.getCreationTime();
                    oldestPeer = peer;
                }
            }
        }
        if (unknownPeers > limit) {
            peerList.remove(oldestPeer);
        }
        if (unknownPeers - 1 > limit) {
            limitKnownPeers(limit);
        }
    }

    private void updatePeerLists() {
        if (updatePending) return;
        updatePending = true;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                splitPeerList();
                incomingPeerAdapter.notifyDataSetChanged();
                outgoingPeerAdapter.notifyDataSetChanged();
                updatePeerStats();
                updatePending = false;
            }
        });
    }

    private void updatePeerStats() {
        int activePeers = 0;
        int connectablePeers = 0;
        for (Peer peer : peerList) {
            if (peer.isHasSentData() || peer.hasReceivedData()) {
                activePeers++;
            }
            if (peer.hasReceivedData()) {
                connectablePeers++;
            }
        }
        float ratio = (float) connectablePeers / (float) activePeers;
        mConnectablePeers.setText(String.valueOf(connectablePeers));
        mActivePeers.setText(String.valueOf(activePeers));
        mConnectableRatio.setText(String.valueOf(ratio));
    }

    private void splitPeerList() {
        List<Peer> newIncoming = new ArrayList<>();
        List<Peer> newOutgoing = new ArrayList<>();
        for (Peer peer : peerList) {
            if (peer.hasReceivedData()) {
                newIncoming.add(peer);
            } else {
                newOutgoing.add(peer);
            }
        }
        if (!newIncoming.equals(incomingList)) {
            incomingList.clear();
            incomingList.addAll(newIncoming);
        }
        if (!newOutgoing.equals(outgoingList)) {
            outgoingList.clear();
            outgoingList.addAll(newOutgoing);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private boolean isValidIp(String s) {
        return Patterns.IP_ADDRESS.matcher(s).matches();
    }

    @Override
    protected void onStop() {
        running = false;
        channel.socket().close();
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("peers", peerList);

        super.onSaveInstanceState(outState);
    }
}
