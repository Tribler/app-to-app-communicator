package org.tribler.app_to_appcommunicator;

import android.content.Context;
import android.net.ConnectivityManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by jaap on 5/4/16.
 */
public class PeerListAdapter extends ArrayAdapter<Peer> {
    private final Context context;

    public PeerListAdapter(Context context, int resource, List<Peer> peerConnectionList) {
        super(context, resource, peerConnectionList);
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.peer_connection_list_item, parent, false);

            holder = new ViewHolder();
            holder.mStatusIndicator = (TextView) convertView.findViewById(R.id.status_indicator);
            holder.mConnected = (TextView) convertView.findViewById(R.id.connected);
            holder.mOpened = (TextView) convertView.findViewById(R.id.closed);
            holder.mSourceAddress = (TextView) convertView.findViewById(R.id.source_address);
            holder.mDestinationAddress = (TextView) convertView.findViewById(R.id.destination_address);
            holder.mPexId = (TextView) convertView.findViewById(R.id.pex_handshake_id);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }


        Peer peer = getItem(position);

        holder.mOpened.setText(String.format("%s", peer.isIncoming() ? "incoming" : "outgoing"));
        if (peer.getPeerId() != null)
            holder.mSourceAddress.setText(peer.getPeerId().substring(0, 8));
        if (peer.hasReceivedData()) {
            holder.mStatusIndicator.setTextColor(context.getResources().getColor(R.color.colorStatusConnected));
            holder.mConnected.setText("Has received data");
            holder.mDestinationAddress.setText(String.format("%s:%d", peer.getExternalAddress(), peer.getPort()));
            holder.mPexId.setText(connectionTypeString(peer.getConnectionType()));
        } else {
            if (peer.isOutgoing()) {
                holder.mStatusIndicator.setTextColor(context.getResources().getColor(R.color.colorStatusConnecting));
            } else {
                holder.mStatusIndicator.setTextColor(context.getResources().getColor(R.color.colorStatusCantConnect));
            }
            holder.mConnected.setText("Not connected");
            holder.mDestinationAddress.setText(String.format("%s:%d", peer.getExternalAddress(), peer.getPort()));
            holder.mPexId.setText("");
        }

        return convertView;
    }

    private String connectionTypeString(int connectionType) {
        switch (connectionType) {
            case ConnectivityManager.TYPE_WIFI:
                return "Wifi";
            case ConnectivityManager.TYPE_BLUETOOTH:
                return "Bluetooth";
            case ConnectivityManager.TYPE_ETHERNET:
                return "Ethernet";
            case ConnectivityManager.TYPE_MOBILE:
                return "Mobile";
            case ConnectivityManager.TYPE_MOBILE_DUN:
                return "Mobile dun";
            case ConnectivityManager.TYPE_VPN:
                return "VPN";
            default:
                return "Unknwon";
        }
    }

    static class ViewHolder {
        TextView mStatusIndicator;
        TextView mConnected;
        TextView mOpened;
        TextView mSourceAddress;
        TextView mDestinationAddress;
        TextView mPexId;
    }

}
