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
public class UtPexHandshakeTest {

    @Test
    public void writeEqualsReadPex() throws Exception {
        UtPexHandshake handshake1 = new UtPexHandshake(1);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        handshake1.writeToStream(stream);
        UtPexHandshake handshake2 = UtPexHandshake.createFromStream(new ByteArrayInputStream(stream.toByteArray()));
        Assert.assertEquals(handshake1, handshake2);
    }

    @Test
    public void PexHandshakeIdCheck() throws IOException, BencodeReadException, PexException {
        int id = 34;
        UtPexHandshake handshake1 = new UtPexHandshake(id);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        handshake1.writeToStream(stream);
        UtPexHandshake handshake2 = UtPexHandshake.createFromStream(new ByteArrayInputStream(stream.toByteArray()));
        Assert.assertEquals(handshake1.getMessageId(), handshake2.getMessageId());
    }

    @Test
    public void PexHandshakeSupportsPex() {
        UtPexHandshake handshake = new UtPexHandshake(3);
        Assert.assertTrue(handshake.supportsPex());
    }

    @Test
    public void PexHandshakeDoesntSupportPex() {
        UtPexHandshake handshake = new UtPexHandshake(0);
        Assert.assertFalse(handshake.supportsPex());
    }
}
