# App-to-app communicator
NATs and firewalls of ISPs on cellular network can prevent making direct connections between devices, app-to-app communicator connects to a swarm to map connectability in the wild.

App-to-app communicator is an Android app to test the connectability of mobile devices over cellular networks. Rendezvous through UDP puncturing is used to punch holes through NATs between peers.

When started, the app connects to a hardcoded connectable peer, through peer exchange a list of peers which are actively connected to the network is retrieved. Every 5 seconds, an introduction request to a random peer is sent. If received: the receiving peer sends back an introduction response containing actively connected PEX peers, and a puncture request is sent to another randomly chosen peer to connect with the initial peer through rendez-vous. The network stays alive, and statistics of the connected peers are displayed.

## Building
This app is made with Android studio, it can be imported, or compiled manually:

<code>./gradlew build</code>

The built APK can be found in <code>app/build/outputs/apk/</code>

## Messages
Several UDP messages are sent between peers. Every message includes the unique id of the sending peer, and the external IP address of the destination peer.

### Introduction request (peer A -> B)
An introduction request sent every 5 seconds to a random active peer.

### Introduction response (peer B -> A)
An introduction response is returned upon receiving an introduction request. A list of actively connected peers is returned, along with one random invitee. A puncture request is send to the invitee.

### Puncture Request (peer B -> C)
A request including a peer to puncture. Upon reception, a puncture is send to the given peer.

### Puncture (peer C -> A)
A puncture is send to a given peer to punch a hole in the NAT on the sending peer's side. This allows the receiving peer to connect to the sending peer.


The related Tribler issue can be found [here](https://github.com/Tribler/tribler/issues/2131).
