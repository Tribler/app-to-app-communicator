package org.tribler.app_to_appcommunicator.PEX;

import com.hypirion.bencode.BencodeReadException;

import junit.framework.Assert;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by jaap on 5/3/16.
 */
public class PexMessageHandshakeTest {

    @Test
    public void writeEqualsReadPex() throws Exception {
        PexHandshake handshake1 = new PexHandshake(1);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        handshake1.writeToStream(stream);
        PexHandshake handshake2 = PexHandshake.createFromStream(new ByteArrayInputStream(stream.toByteArray()));
        Assert.assertEquals(handshake1, handshake2);
    }

    @Test
    public void PexHandshakeIdCheck() throws IOException, BencodeReadException, PexException {
        int id = 34;
        PexHandshake handshake1 = new PexHandshake(id);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        handshake1.writeToStream(stream);
        PexHandshake handshake2 = PexHandshake.createFromStream(new ByteArrayInputStream(stream.toByteArray()));
        Assert.assertEquals(handshake1.getMessageId(), handshake2.getMessageId());
    }

    @Test
    public void PexHandshakeSupportsPex() {
        PexHandshake handshake = new PexHandshake(3);
        Assert.assertTrue(handshake.supportsPex());
    }

    @Test
    public void PexHandshakeDoesntSupportPex() {
        PexHandshake handshake = new PexHandshake(0);
        Assert.assertFalse(handshake.supportsPex());
    }
}
