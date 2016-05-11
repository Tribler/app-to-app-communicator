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

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class OverviewActivity extends AppCompatActivity {

    final static int PORT = 1873;

    private List<ServerTask> serverTasks;
    private List<ClientTask> clientTasks;

    private Button mOpenConnectButton;
    private Button mCloseConnectButton;
    private TextView mStatusText;
    private PeerConnectionListAdapter peerConnectionListAdapter;
    private PeerConnectionObserver peerConnectionObserver;

    private List<PeerConnection> peers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overview);

        peers = new ArrayList<>();
        clientTasks = new ArrayList<>();
        serverTasks = new ArrayList<>();

        mStatusText = (TextView) findViewById(R.id.status_text);
        mOpenConnectButton = (Button) findViewById(R.id.start_connection_button);
        mCloseConnectButton = (Button) findViewById(R.id.close_connection_button);

        mOpenConnectButton.setOnClickListener(new ConnectButtonListener());
        mCloseConnectButton.setOnClickListener(new CloseConnectionButtonListener());

        ListView peerConnectionListView = (ListView) findViewById(R.id.peer_connection_list_view);
        peerConnectionListAdapter = new PeerConnectionListAdapter(getApplicationContext(), R.layout.peer_connection_list_item, peers);
        peerConnectionListView.setAdapter(peerConnectionListAdapter);

        peerConnectionObserver = new PeerConnectionObserver();

        startServer();
        System.out.println("Showing local ip");
        showLocalIpAddress();
    }

    private class PeerConnectionObserver implements Observer {

        @Override
        public void update(Observable observable, Object data) {
            peerConnectionListAdapter.notifyDataSetChanged();
        }
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

    private class ConnectButtonListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            setStatus("Connecting to server");
            String ipText = ((EditText) findViewById(R.id.ip_address_edit_text)).getText().toString();
            InetAddress destinationAddress;
            if (!isValidIp(ipText)) {
                Toast.makeText(getApplicationContext(), "Not a valid IP address", Toast.LENGTH_LONG).show();
                return;
            }

            ClientTask clientTask = new ClientTask(ipText, PORT, new ClientTask.ClientConnectionCallback() {

                @Override
                public void onConnection(PeerConnection connection) {
                    addPeerConnection(connection);
                    connection.addObserver(peerConnectionObserver);
                    System.out.println("Added " + connection);
                }
            });
            clientTasks.add(clientTask);
            clientTask.start();
        }
    }

    private void addPeerConnection(final PeerConnection connection) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                peers.add(connection);
                peerConnectionListAdapter.notifyDataSetChanged();
            }
        });
    }

    private void updateStatus() {
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
        ServerTask serverTask = new ServerTask(PORT, new ServerTask.ServerConnectionListener() {
            @Override
            public void onConnection(PeerConnection connection) {
                addPeerConnection(connection);
            }
        });
        serverTasks.add(serverTask);
        serverTask.start();
    }


    private void stopServer() {
        setStatus("Closing client socket");
        for (PeerConnection connection : peers) {
            try {
                connection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class CloseConnectionButtonListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            setStatus("Closing client socket");
            for (PeerConnection connection : peers) {
                try {
                    connection.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
