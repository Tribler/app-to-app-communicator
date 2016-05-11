package org.tribler.app_to_appcommunicator;

import java.net.Socket;

/**
 * Created by jaap on 4/26/16.
 */
public class PrintUtil {

    public static void printSocket(Socket s) {
        System.out.println("src: " +  s.getLocalAddress() + ":" + s.getLocalPort());
        System.out.println("dst: " + s.getInetAddress() + ":" + s.getPort());
    }
}
