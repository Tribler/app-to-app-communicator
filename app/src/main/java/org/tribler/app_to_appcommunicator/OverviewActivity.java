package org.tribler.app_to_appcommunicator;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.tribler.app_to_appcommunicator.PEX.PexListener;
import org.tribler.app_to_appcommunicator.PEX.PexMessage;
import org.tribler.app_to_appcommunicator.PEX.PexSender;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

public class OverviewActivity extends AppCompatActivity implements PexListener {

    final static int DEFAULT_PORT = 1873;
    private static final int BUFFER_SIZE = 1024;

    private List<ServerTask> serverTasks;

    private Button mOpenConnectButton;
    private TextView mStatusText;
    private PeerListAdapter peerConnectionListAdapter;
    private PexSender pexSender;
    private PexListener pexListener = this;
    private DatagramChannel channel;

    private Map<InetSocketAddress, Peer> peers;
    private List<Peer> peerList;
    private boolean running;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overview);

        peers = new HashMap<>();
        peerList = new ArrayList<>();
        serverTasks = new ArrayList<>();

        mStatusText = (TextView) findViewById(R.id.status_text);
        mOpenConnectButton = (Button) findViewById(R.id.start_connection_button);

        mOpenConnectButton.setOnClickListener(new ConnectButtonListener());

        ListView peerConnectionListView = (ListView) findViewById(R.id.peer_connection_list_view);
        peerConnectionListAdapter = new PeerListAdapter(getApplicationContext(), R.layout.peer_connection_list_item, peerList);
        peerConnectionListView.setAdapter(peerConnectionListAdapter);
//
//        peerConnectionObserver = new PeerConnectionObserver();
//        pexSender = new PexSender(peers);
//
//        startServer();
//        System.out.println("Showing local ip");
        running = true;
        showLocalIpAddress();
        startListenThread();
        startSendThread();
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
                    PexMessage pex = new PexMessage();
                    pex.addAll(peers.values());
                    for (Peer peer : peers.values()) {
                        pex.setDestinationAddress(peer.getAddress());
                        peer.sendPex(pex);
                    }
                }
            }
        }).start();
        System.out.println("Thread started");
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
//                        byte[] b = new byte[inputBuffer.remaining()];
//                        inputBuffer.get(b);
                        dataReceived(inputBuffer, (InetSocketAddress) address);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        System.out.println("Thread started");
    }

    private void dataReceived(ByteBuffer data, InetSocketAddress address) {
        if (peers.containsKey(address)) {
            peers.get(address).received(data);
        } else {
            Peer peer = addPeer(address);
            peer.received(data);
        }
        updatePeerList();
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
                System.out.println("Local ip: " + inetAddress);
                TextView localIp = (TextView) findViewById(R.id.local_ip_address_view);
                localIp.setText(inetAddress.toString());
            }
        }.execute();
    }

    private void setStatus(final String status) {
        new Handler(getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mStatusText.setText(status);
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

    private Peer addPeer(InetSocketAddress address) {
        final Peer peer = new Peer(address, channel);
        new Handler(Looper.getMainLooper()).post(new Runnable() {

            @Override
            public void run() {
                peers.put(peer.getAddress(), peer);
                peerList.add(peer);
                peer.connect();
                peerConnectionListAdapter.notifyDataSetChanged();
                System.out.println("Added " + peer);
            }
        });
        return peer;
    }

    private void updatePeerList() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                peerConnectionListAdapter.notifyDataSetChanged();
            }
        });
    }

    private boolean isValidIp(String s) {
        return Patterns.IP_ADDRESS.matcher(s).matches();
    }

    @Override
    public void onPex(PexMessage pex) {
        boolean has;
        for (Peer pexPeer : pex) {
            has = false;
            for (Peer localPeer : peers.values()) {
                if (pexPeer.getExternalAddress().equals(localPeer.getExternalAddress())) {
                    has = true;
                    break;
                }
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private class PeerConnectionObserver implements Observer {

        @Override
        public void update(Observable observable, Object data) {
            updatePeerList();
            PeerConnection connection = (PeerConnection) observable;
            connection.setPexListener(pexListener);
        }
    }

    private class ConnectButtonListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            setStatus("Connecting to server");
            String ipText = ((EditText) findViewById(R.id.ip_address_edit_text)).getText().toString();
            String portText = ((EditText) findViewById(R.id.port_edit_text)).getText().toString();
            int port = Integer.valueOf(portText);
            if (!isValidIp(ipText)) {
                Toast.makeText(getApplicationContext(), "Not a valid IP address", Toast.LENGTH_LONG).show();
                return;
            }

            try {
                InetSocketAddress address = new InetSocketAddress(InetAddress.getByName(ipText), port);
                if (peers.containsKey(address)) {
                    showToast("Peer with address " + address + " already added");
                    return;
                }
                addPeer(address);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }

    }

}
