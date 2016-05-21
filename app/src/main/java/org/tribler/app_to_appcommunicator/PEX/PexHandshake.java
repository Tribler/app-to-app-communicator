package org.tribler.app_to_appcommunicator.PEX;

import com.hypirion.bencode.BencodeReadException;
import com.hypirion.bencode.BencodeReader;
import com.hypirion.bencode.BencodeWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jaap on 5/2/16.
 */
public class PexHandshake {
    HashMap<String, Map<String, Long>> utPexHandshake;
    private long messageId;

    public PexHandshake(long messageId) {
        this.utPexHandshake = new HashMap<>();
        this.messageId = messageId;
        HashMap<String, Long> utPexDict = new HashMap<>();
        utPexDict.put("ut_pex", messageId);
        this.utPexHandshake.put("m", utPexDict);
    }

    public void writeToStream(OutputStream out) throws IOException {
        BencodeWriter writer = new BencodeWriter(out);
        writer.write(utPexHandshake);
    }

    public boolean supportsPex() {
        return messageId >= 1;
    }

    public static PexHandshake createFromStream(InputStream stream) throws IOException, BencodeReadException {
        BencodeReader reader = new BencodeReader(stream);
        Map<String, Object> dict = reader.readDict();
        Map<String, Long> message = (Map<String, Long>) dict.get("m");
        long messageId = message.get("ut_pex");
        String s = "";
        while (stream.available() > 0 ) {
            s += stream.read();
        }
        System.out.println("Leftover: " + s);
        System.out.println("Handshake read, available bytes: " + stream.available());
        return new PexHandshake(messageId);
    }

    public long getMessageId() {
        return messageId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PexHandshake handshake = (PexHandshake) o;

        if (messageId != handshake.messageId) return false;
        return utPexHandshake != null ? utPexHandshake.equals(handshake.utPexHandshake) : handshake.utPexHandshake == null;

    }
}
