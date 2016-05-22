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
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class OverviewActivity extends AppCompatActivity implements PexListener {

    final static int DEFAULT_PORT = 1873;

    private List<ServerTask> serverTasks;

    private Button mOpenConnectButton;
    private Button mCloseConnectButton;
    private TextView mStatusText;
    private PeerListAdapter peerConnectionListAdapter;
    private PeerConnectionObserver peerConnectionObserver;
    private PexSender pexSender;
    private PexListener pexListener = this;

    private List<Peer> peers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overview);

        peers = new ArrayList<>();
        serverTasks = new ArrayList<>();

        mStatusText = (TextView) findViewById(R.id.status_text);
        mOpenConnectButton = (Button) findViewById(R.id.start_connection_button);
        mCloseConnectButton = (Button) findViewById(R.id.close_connection_button);

        mOpenConnectButton.setOnClickListener(new ConnectButtonListener());
        mCloseConnectButton.setOnClickListener(new CloseConnectionButtonListener());

        ListView peerConnectionListView = (ListView) findViewById(R.id.peer_connection_list_view);
        peerConnectionListAdapter = new PeerListAdapter(getApplicationContext(), R.layout.peer_connection_list_item, peers);
        peerConnectionListView.setAdapter(peerConnectionListAdapter);

        peerConnectionObserver = new PeerConnectionObserver();
        pexSender = new PexSender(peers);

        startServer();
        System.out.println("Showing local ip");
        showLocalIpAddress();
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

    private void addPeer(final Peer peer) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {

            @Override
            public void run() {
                peers.add(peer);
                peer.setConnectionObserver(peerConnectionObserver);
                peerConnectionListAdapter.notifyDataSetChanged();
                System.out.println("Added " + peer);
            }
        });
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

    private void startServer() {
        setStatus("Starting server socket");
        ServerTask serverTask = new ServerTask(DEFAULT_PORT, new ServerTask.ServerConnectionListener() {
            @Override
            public void onConnection(Peer peer) {
                addPeer(peer);
                showToast("Received connection from " + peer.getExternalAddress());
            }
        });
        serverTasks.add(serverTask);
        serverTask.start();
    }

    private void stopServer() {
        setStatus("Closing client socket");
        for (Peer peer : peers) {
            try {
                if (peer.hasConnection()) {
                    peer.getPeerConnection().close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onPex(PexMessage pex) {
        boolean has;
        for (Peer pexPeer : pex) {
            has = false;
            for (Peer localPeer : peers) {
                if (pexPeer.getExternalAddress().equals(localPeer.getExternalAddress())) {
                    has = true;
                    break;
                }
            }
            if (!has) {
                addPeer(pexPeer);
                pexPeer.connect();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
//        for (ServerTask serverTask : serverTasks) {
//            serverTask.stop();
//        }
//        for (Peer peer : peers) {
//            if (peer.hasConnection())
//                peer.getPeerConnection().stop();
//        }
//        pexSender.stop();
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
                Peer peer = new Peer(port, (Inet4Address) Inet4Address.getByName(ipText));
                addPeer(peer);
                peer.connect();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }

    }

    private class CloseConnectionButtonListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            setStatus("Closing client socket");
            for (Peer peer : peers) {
                try {
                    if (peer.hasConnection())
                        peer.getPeerConnection().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
