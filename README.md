# Reliable-Email-Transfer-Protocol-UDP
In this project, we implement Reliable Data Transfer over
UDP to ensure correct, loss-free, and ordered delivery of data. A 3-Way Handshake was added
for connection management, along with error-handling mechanisms like sequence numbers and
acknowledgments to prevent packet loss. Clients can send large attachments (up to 25MB)
by splitting them into 1 KB packets, and Inbox Synchronization was included to allow clients
to receive emails even when disconnected, with the server storing and forwarding emails upon
reconnection.
