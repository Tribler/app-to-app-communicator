package org.tribler.app_to_appcommunicator.PEX;

import com.hypirion.bencode.BencodeReadException;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;

/**
 * Created by jaap on 5/3/16.
 */
public class PexMessageTest {
    Inet4Address address1;
    Inet4Address address2;
    Inet4Address address3;

    @Before
    public void initialize() throws UnknownHostException {
        address1 = (Inet4Address) Inet4Address.getByName("95.85.21.63");
        address2 = (Inet4Address) Inet4Address.getByName("127.0.0.1");
        address3 = (Inet4Address) Inet4Address.getByName("0.0.0.0");
    }

    @Test
    public void writeEqualsReadPex() throws Exception {
        PexMessage pex = new PexMessage();
        pex.add(address1);
        pex.add(address2);
        pex.add(address3);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        pex.writeToStream(stream);
        PexMessage pex2 = PexMessage.createFromStream(new ByteArrayInputStream(stream.toByteArray()));
        Assert.assertEquals(pex, pex2);
    }

    @Test
    public void PexContainsAddedAddress() throws IOException, BencodeReadException, PexException {
        PexMessage pex = new PexMessage();
        pex.add(address1);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        pex.writeToStream(stream);
        PexMessage pex2 = PexMessage.createFromStream(new ByteArrayInputStream(stream.toByteArray()));
        Assert.assertTrue(pex2.contains(address1));
    }

    @Test
    public void PexDoesntContainNotAddedAddress() throws IOException, BencodeReadException, PexException {
        PexMessage pex = new PexMessage();
        pex.add(address1);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        pex.writeToStream(stream);
        PexMessage pex2 = PexMessage.createFromStream(new ByteArrayInputStream(stream.toByteArray()));
        Assert.assertFalse(pex2.contains(address2));
    }
}
