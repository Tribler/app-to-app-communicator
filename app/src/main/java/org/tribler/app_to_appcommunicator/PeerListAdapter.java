package org.tribler.app_to_appcommunicator;

import android.content.Context;
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
        if (peer.hasReceivedData()) {
            holder.mStatusIndicator.setTextColor(context.getResources().getColor(R.color.colorStatusConnected));
            holder.mConnected.setText("Has received data");
            holder.mOpened.setText("");
            holder.mSourceAddress.setText("");
            holder.mDestinationAddress.setText(String.format("%s:%d", peer.getExternalAddress(), peer.getPort()));
            holder.mPexId.setText(peer.hasDoneHandshake() ? String.valueOf(peer.getPexHandshake().getMessageId()) : "None");
        } else {
            holder.mStatusIndicator.setTextColor(context.getResources().getColor(R.color.colorStatusCantConnect));
            holder.mConnected.setText("Not connected");
            holder.mOpened.setText("");
            holder.mSourceAddress.setText("");
            holder.mDestinationAddress.setText(String.format("%s:%d", peer.getExternalAddress(), peer.getPort()));
            holder.mPexId.setText("");
        }

        return convertView;
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
