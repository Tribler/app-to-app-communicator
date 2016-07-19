# Towards indestructable apps

We envision a new Internet infrastructure built around people and their phones. This infrastructure should not rely on any server, require no infrastructure, and trust no entity except itself. The Tribler research group creates attack-resilient sharing software, including no-Internet-needed apps. We created a first pure P2P app.

## App-to-app communicator

This app makes a global connected network consisting exclusively of smartphones. Not a single server is needed.

The Internet has shifted from PC to mobile, our App-to-app communicator creates a decentralized network of mobile apps that can keep themselves connected without central authority or server. This is made hard by NATs and firewalls of ISPs on cellular networks which can prevent making direct connections between devices, app-to-app communicator tries to get around these barriers through NAT puncturing.

The Tribler group at the TU Delft has done research about and developed projects around the field of decentralisation on the Internet over the past 11 years. Projects include [Tribler](https://tribler.org/), our own torrents-only anon network with [Tor messages](https://github.com/Tribler/tribler/wiki/Anonymous-Downloading-and-Streaming-specifications), blockchain technologies, the Android [stealth app](https://github.com/droidstealth/droid-stealth), and the [self-compiling Android app](https://github.com/Tribler/self-compile-Android). This is another prototype which contributes towards this work. The coming years we will consolidate and merge all this work into a single app. This app would be resilient to attacks and have the ability to bypass Internet kill switches.

## [**Download Beta from Play Store**](https://play.google.com/apps/testing/org.tribler.app_to_appcommunicator)

![Screenshot] (https://raw.githubusercontent.com/Jaapp-/app-to-app-communicator/master/img/Screenshot.png)

## Peer discovery
1. When peer A starts App-to-app communicator, a connection request to peer B is made.
2. Upon connection peer B chooses another connected peer, peer C, and sends the address of peer C to peer A as introduction response message.
3. Peer B sends peer C a puncture request.
4. Peer C sends a puncture message to peer A to punch a hole in its own NAT.

![Peer walking](https://github.com/Jaapp-/app-to-app-communicator/blob/master/img/walk.png)

### UDP packet types
Several UDP messages are sent between peers. Every message includes the unique id of the sending peer, and the external IP address of the destination peer.

#### Introduction request (peer A -> B)
An introduction request is sent every 5 seconds to a random actively connected peer.

#### Introduction response (peer B -> A)
An introduction response is returned upon receiving an introduction request. A list of actively connected peers is returned, along with one random invitee. At the same time a puncture request is sent to the invitee.

#### Puncture Request (peer B -> C)
A request including a peer to puncture. Upon reception, a puncture message is sent to the given peer.

#### Puncture (peer C -> A)
A puncture message is sent to peer A to punch a hole peer C's NAT. This allows peer A to connect to peer C.

### Identification
The app generates a unique identifier on its first launch. This peer ID is sent with every outgoing message, and is used to identify peers so that the app can differentiate between UDP packets from different peers which use the same address.

## Local IP
The local IP address is obtained through a call to Android Network Info and displayed within the app.

## IPv4 voting
An Android device doesn't know what its own IPv4 is.
Each peer has its own WAN address, or external IP address, but a peer has no way of knowing its own WAN address without the help of others. 

Each message contains the WAN address of its destination. The app determines what the most likely external IP is based on the reported IPv4 addresses. When there's conflicting reports, and devices claim to see different external IPs, our app relies on the majority judgement.

As IPv4 addresses can change quickly, we only look at just the last 3 reports.

## Building
This app is made with Android studio, it can be imported, or compiled manually:

<code>./gradlew build</code>

The built APK can be found in <code>app/build/outputs/apk/</code>
