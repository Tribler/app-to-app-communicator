package org.tribler.app_to_appcommunicator;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
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
    final static int UNKNOWN_PEER_LIMIT = 20;
    final static String HASH_ID = "hash_id";
    final static int DEFAULT_PORT = 1873;
    final static int KNOWN_PEER_LIMIT = 10;
    private static final int BUFFER_SIZE = 2048;
    private TextView mWanVote;
    private TextView mActivePeers;
    private TextView mConnectablePeers;
    private TextView mConnectableRatio;
    private Button mExitButton;
    private PeerListAdapter incomingPeerAdapter;
    private PeerListAdapter outgoingPeerAdapter;
    private DatagramChannel channel;

    private ArrayList<Peer> peerList;
    private List<Peer> incomingList;
    private List<Peer> outgoingList;
    private String hashId;
    private String networkOperator;
    private WanVote wanVote;
    private int connectionType;
    private ByteBuffer outBuffer;
    private InetSocketAddress internalSourceAddress;

    private Thread sendThread;
    private Thread listenThread;

    private boolean willExit = false;

    /**
     * Initialize views, start send and receive threads if necessary.
     *
     * @param savedInstanceState saved instance state
     */
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
        if(hashId.toString().length() > 4){
            ((TextView) findViewById(R.id.peer_id)).setText(hashId.toString().substring(0, 4));
        } else {
            ((TextView) findViewById(R.id.peer_id)).setText(hashId.toString());
        }
        wanVote = new WanVote();
        outBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        mWanVote = (TextView) findViewById(R.id.wanvote);
        mActivePeers = (TextView) findViewById(R.id.active_peers);
        mConnectablePeers = (TextView) findViewById(R.id.connectable_peers);
        mConnectableRatio = (TextView) findViewById(R.id.connectable_ratio);
        initExitButton();

        try {
            channel = DatagramChannel.open();
            channel.socket().bind(new InetSocketAddress(DEFAULT_PORT));
        } catch (IOException e) {
            e.printStackTrace();
        }
        updateConnectionType();
        addInitialPeer();
        startListenThread();
        startSendThread();
        showLocalIpAddress();
        initPeerLists();
        if (savedInstanceState != null) {
            updatePeerLists();
        }
    }

    private void initExitButton() {
        mExitButton = (Button) findViewById(R.id.exit_button);
        mExitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                willExit = true;
                finish();
            }
        });
    }

    /**
     * Initialize the peer lists.
     */
    private void initPeerLists() {
        ListView incomingPeerConnectionListView = (ListView) findViewById(R.id.incoming_peer_connection_list_view);
        ListView outgoingPeerConnectionListView = (ListView) findViewById(R.id.outgoing_peer_connection_list_view);
        incomingPeerAdapter = new PeerListAdapter(getApplicationContext(), R.layout.peer_connection_list_item, incomingList, Peer.INCOMING);
        incomingPeerConnectionListView.setAdapter(incomingPeerAdapter);
        outgoingPeerAdapter = new PeerListAdapter(getApplicationContext(), R.layout.peer_connection_list_item, outgoingList, Peer.OUTGOING);
        outgoingPeerConnectionListView.setAdapter(outgoingPeerAdapter);
    }

    /**
     * Add the intial hard-coded connectable peer to the peer list.
     */
    private void addInitialPeer() {
        try {
            addPeer(null, new InetSocketAddress(InetAddress.getByName(CONNECTABLE_ADDRESS), DEFAULT_PORT), Peer.OUTGOING);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }


    /**
     * Request and display the current connection type.
     */
    private void updateConnectionType() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            connectionType = cm.getActiveNetworkInfo().getType();
        } catch (Exception e) {
            showToast("Can't connect: no active network");
            return;
        }
        ((TextView) findViewById(R.id.connection_type))
                .setText(cm.getActiveNetworkInfo().getTypeName() + " " + cm.getActiveNetworkInfo().getSubtypeName());
    }

    /**
     * Retrieve the local peer id from storage.
     *
     * @return the peer id.
     */
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

    /**
     * Generate a new hash to be used as peer id.
     *
     * @return the generated hash.
     */
    private String generateHash() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Start the thread send thread responsible for sending a {@link IntroductionRequest} to a random peer every 5 seconds.
     */
    private void startSendThread() {
        sendThread = new Thread(new Runnable() {
            @Override
            public void run() {
                do {
                    try {
                        if (peerList.size() > 0) {
                            Peer peer = getEligiblePeer(null);
                            if (peer != null) {
                                sendIntroductionRequest(peer);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        break;
                    }
                } while (!Thread.interrupted());
                System.out.println("Send thread stopped");
            }
        });
        sendThread.start();
        System.out.println("Send thread started");
    }

    /**
     * Send an introduction request.
     *
     * @param peer the destination.
     * @throws IOException
     */
    private void sendIntroductionRequest(Peer peer) throws IOException {
        IntroductionRequest request = new IntroductionRequest(hashId, peer.getAddress(), connectionType, networkOperator);
        sendMesssage(request, peer);
    }

    /**
     * Send a puncture request.
     *
     * @param peer         the destination.
     * @param puncturePeer the peer to puncture.
     * @throws IOException
     */
    private void sendPunctureRequest(Peer peer, Peer puncturePeer) throws IOException {
        PunctureRequest request = new PunctureRequest(hashId, peer.getAddress(), internalSourceAddress, puncturePeer);
        sendMesssage(request, peer);
    }

    /**
     * Send a puncture.
     *
     * @param peer the destination.
     * @throws IOException
     */
    private void sendPuncture(Peer peer) throws IOException {
        Puncture puncture = new Puncture(hashId, peer.getAddress(), internalSourceAddress);
        sendMesssage(puncture, peer);
    }

    /**
     * Send an introduction response.
     *
     * @param peer    the destination.
     * @param invitee the invitee to which the destination peer will send a puncture request.
     * @throws IOException
     */
    private void sendIntroductionResponse(Peer peer, Peer invitee) throws IOException {
        List<Peer> pexPeers = new ArrayList<>();
        for (Peer p : peerList) {
            if (p.hasReceivedData() && p.getPeerId() != null && p.isAlive())
                pexPeers.add(p);
        }
        IntroductionResponse response = new IntroductionResponse(hashId, internalSourceAddress, peer
                .getAddress(), invitee, connectionType, pexPeers, networkOperator);
        sendMesssage(response, peer);
    }

    /**
     * Send a message to given peer.
     *
     * @param message the message to send.
     * @param peer    the destination peer.
     * @throws IOException
     */
    private synchronized void sendMesssage(Message message, Peer peer) throws IOException {
        System.out.println("Sending " + message);
        outBuffer.clear();
        message.writeToByteBuffer(outBuffer);
        outBuffer.flip();
        channel.send(outBuffer, peer.getAddress());
        peer.sentData();
        updatePeerLists();
    }

    /**
     * Pick a random eligible peer/invitee for sending an introduction request to.
     *
     * @param excludePeer peer to which the invitee is sent.
     * @return the eligible peer if any, else null.
     */
    private Peer getEligiblePeer(Peer excludePeer) {
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

    /**
     * Start the listen thread. The thread opens a new {@link DatagramChannel} and calls {@link OverviewActivity#dataReceived(ByteBuffer,
     * InetSocketAddress)} for each incoming datagram.
     */
    private void startListenThread() {
        listenThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ByteBuffer inputBuffer = ByteBuffer.allocate(BUFFER_SIZE);
                    while (!Thread.interrupted()) {
                        inputBuffer.clear();
                        SocketAddress address = channel.receive(inputBuffer);
                        inputBuffer.flip();
                        dataReceived(inputBuffer, (InetSocketAddress) address);
                    }
                } catch (IOException e) {
                    System.out.println("Listen thread stopped");
                }
            }
        });
        listenThread.start();
        System.out.println("Listen thread started");
    }

    /**
     * Resolve a peer id or address to a peer, else create a new one.
     *
     * @param id       the peer's unique id.
     * @param address  the peer's address.
     * @param incoming boolean indicator whether the peer is incoming.
     * @return the resolved or create peer.
     */
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

    /**
     * Remove duplicate peers from the peerlist.
     */
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

    /**
     * Check whether a peer exists.
     *
     * @param id the peer's id.
     * @return the result.
     */
    private boolean peerExists(String id) {
        if (id == null) return false;
        for (Peer peer : peerList) {
            if (id.equals(peer.getPeerId())) {
                return true;
            }
        }
        return false;
    }


    /**
     * Handle incoming data.
     *
     * @param data    the data {@link ByteBuffer}.
     * @param address the incoming address.
     */
    private void dataReceived(ByteBuffer data, InetSocketAddress address) {
        try {
            Message message = Message.createFromByteBuffer(data);
            System.out.println("Received " + message);
            String id = message.getPeerId();
            if (wanVote.vote(message.getDestination())) {
                System.out.println("Address changed to " + wanVote.getAddress());
                showLocalIpAddress();
            }
            setWanvote(wanVote.getAddress().toString());
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

    /**
     * Handle an introduction request. Send a puncture request to the included invitee.
     *
     * @param peer    the origin peer.
     * @param message the message.
     * @throws IOException
     */
    private void handlIntroductionRequest(Peer peer, IntroductionRequest message) throws IOException {
        peer.setNetworkOperator(message.getNetworkOperator());
        peer.setConnectionType((int) message.getConnectionType());
        if (peerList.size() > 1) {
            Peer invitee = getEligiblePeer(peer);
            if (invitee != null) {
                sendIntroductionResponse(peer, invitee);
                sendPunctureRequest(invitee, peer);
                System.out.println("Introducing " + invitee.getAddress() + " to " + peer.getAddress());
            }
        } else {
            System.out.println("Peerlist too small, can't handle introduction request");
            sendIntroductionResponse(peer, null);
        }
    }

    /**
     * Handle an introduction response. Parse incoming PEX peers.
     *
     * @param peer    the origin peer.
     * @param message the message.
     */
    private void handleIntroductionResponse(Peer peer, IntroductionResponse message) {
        peer.setConnectionType((int) message.getConnectionType());
        peer.setNetworkOperator(message.getNetworkOperator());
        List<Peer> pex = message.getPex();
        for (Peer pexPeer : pex) {
            if (hashId.equals(pexPeer.getPeerId())) continue;
            getOrMakePeer(pexPeer.getPeerId(), pexPeer.getAddress(), Peer.OUTGOING);
        }
    }

    /**
     * Handle a puncture. Does nothing because the only purpose of a puncture is to punch a hole in the NAT.
     *
     * @param peer    the origin peer.
     * @param message the message.
     * @throws IOException
     */
    private void handlePuncture(Peer peer, Puncture message) throws IOException {
    }

    /**
     * Handle a puncture request. Sends a puncture to the puncture peer included in the message.
     *
     * @param peer    the origin peer.
     * @param message the message.
     * @throws IOException
     * @throws MessageException
     */
    private void handlePunctureRequest(Peer peer, PunctureRequest message) throws IOException, MessageException {
        if (!peerExists(message.getPuncturePeer().getPeerId()))
            sendPuncture(message.getPuncturePeer());
    }

    /**
     * Show the local IP address.
     */
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

    /**
     * Set the external ip field based on the WAN vote.
     *
     * @param ip the ip address.
     */
    private void setWanvote(final String ip) {
        new Handler(getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mWanVote.setText(ip);
            }
        });
    }

    /**
     * Show a toast.
     *
     * @param toast the text to show.
     */
    private void showToast(final String toast) {
        new Handler(getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), toast, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Add a peer to the peer list.
     *
     * @param peerId   the peer's id.
     * @param address  the peer's address.
     * @param incoming whether the peer is an incoming peer.
     * @return the added peer.
     */
    private synchronized Peer addPeer(String peerId, InetSocketAddress address, boolean incoming) {
        if (hashId.equals(peerId)) {
            System.out.println("Not adding self");
            Peer self = null;
            for (Peer p : peerList) {
                if (p.getAddress().equals(wanVote.getAddress()))
                    self = p;
            }
            if (self != null) {
                peerList.remove(self);
                System.out.println("Removed self");
            }
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
        final Peer peer = new Peer(peerId, address);
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

    /**
     * Deletes the oldest peers based on constant limits {@value KNOWN_PEER_LIMIT} and {@value UNKNOWN_PEER_LIMIT}.
     */
    private void trimPeers() {
        limitKnownPeers(KNOWN_PEER_LIMIT);
        limitUnknownPeers(UNKNOWN_PEER_LIMIT);
    }

    /**
     * Limit the amount of known peers by deleting the oldest peers.
     *
     * @param limit the limit.
     */
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

    /**
     * Limit the amount of known peers by deleting the oldest peers.
     *
     * @param limit the limit.
     */
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

    /**
     * Update the showed peer lists.
     */
    private void updatePeerLists() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                splitPeerList();
                incomingPeerAdapter.notifyDataSetChanged();
                outgoingPeerAdapter.notifyDataSetChanged();
                updatePeerStats();
            }
        });
    }

    /**
     * Update the connection stats of thee peers.
     */
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

    /**
     * Split the peer list between incoming and outgoing peers.
     */
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
        boolean changed = false;
        if (!newIncoming.equals(incomingList)) {
            changed = true;
            incomingList.clear();
            incomingList.addAll(newIncoming);
        }
        if (!newOutgoing.equals(outgoingList)) {
            changed = true;
            outgoingList.clear();
            outgoingList.addAll(newOutgoing);
        }
        if (changed) updatePeerStats();
    }

    /**
     * Check whether an ip address is valid.
     *
     * @param s the text to check for validity.
     * @return the validity.
     */
    private boolean isValidIp(String s) {
        return Patterns.IP_ADDRESS.matcher(s).matches();
    }

    @Override
    protected void onDestroy() {
        listenThread.interrupt();
        sendThread.interrupt();
        channel.socket().close();
        try {
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        if (!willExit)
            showToast("App will continue in background.");
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("peers", peerList);

        super.onSaveInstanceState(outState);
    }
}
