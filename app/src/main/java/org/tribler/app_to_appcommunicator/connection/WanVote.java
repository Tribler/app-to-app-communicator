package org.tribler.app_to_appcommunicator.connection;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jaap on 6/1/16.
 */
public class WanVote {
    Map<InetSocketAddress, Integer> countMap = new HashMap<>();
    private InetSocketAddress majorityAddress;
    private int max;

    public WanVote() {
        countMap = new HashMap<>();
        majorityAddress = null;
        max = 0;
    }

    public void vote(InetSocketAddress address) {
        if (countMap.containsKey(address)) {
            countMap.put(address, countMap.get(address) + 1);
        } else {
            countMap.put(address, 1);
        }
        if (countMap.get(address) > max) {
            max = countMap.get(address);
            majorityAddress = address;
        }
    }

    public InetSocketAddress getAddress() {
        return majorityAddress;
    }

}
