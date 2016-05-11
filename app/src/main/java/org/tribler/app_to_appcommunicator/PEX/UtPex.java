package org.tribler.app_to_appcommunicator.PEX;

import com.hypirion.bencode.BencodeReadException;
import com.hypirion.bencode.BencodeReader;
import com.hypirion.bencode.BencodeWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Created by jaap on 5/2/16.
 */
public class UtPex extends HashSet<Inet4Address> {

    public void writeToStream(OutputStream out) throws IOException {
        Map<String, Object> utPex = new HashMap<>();
        List<String> peers = new ArrayList<>();
        for (Inet4Address address : this) {
            peers.add(new String(address.getAddress()));
        }

        utPex.put("added", peers);

        BencodeWriter writer = new BencodeWriter(out);
        writer.write(utPex);
        writer.write("\n");
    }

    public static UtPex createFromStream(InputStream stream) throws IOException, BencodeReadException, PexException {
        BencodeReader reader = new BencodeReader(stream);
        Map<String, Object> dict = reader.readDict();

        List peers;
        if (dict.get("added") instanceof List) {
            peers = (List) dict.get("added");
        } else {
            throw new PexException("Added must be a List");
        }

        UtPex utPex = new UtPex();
        for (Object peer : peers) {
            utPex.add((Inet4Address) Inet4Address.getByAddress(((String) peer).getBytes()));
        }

        return utPex;
    }
}
