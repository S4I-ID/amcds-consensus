Project completed as part of my master’s degree coursework (Algorithms, Models and Concepts in Distributed Systems - AMCDS). 
Consists of a Java implementation for the client of a given distributed system. 
Implements features such as message broadcasting, NN atomic registry, failure detection, leader detection and uniform consensus.
Used Protobuf library for message encoding/decoding and the log4j2 library for logging.<br>

# Starting the app
1. Compile the clients (uses Protobuf version 4.33.5)
```shell
mkdir -p target/generated-sources/protobuf
protoc --proto_path="src/main/resources" --java_out="target/generated-sources/protobuf" p.proto
mvn package
```
2. Start the server with the bundled reference clients (will start on ports 5001-5003)
```shell
.\dalgs-windows-amd64-new 127.0.0.1 5000 127.0.0.1 5001 5002 5003
```
3. Start the clients from step 1 (will start on ports 5004-5006)
```shell
java -jar .\target\amcds-client-1.1.jar 127.0.0.1 5000 127.0.0.1 5004 abc 1
java -jar .\target\amcds-client-1.1.jar 127.0.0.1 5000 127.0.0.1 5005 abc 2
java -jar .\target\amcds-client-1.1.jar 127.0.0.1 5000 127.0.0.1 5006 abc 3
```
Or you can just download and run the pre-compiled .jars from the [releases](https://github.com/S4I-ID/amcds-consensus/releases/) 
section. Or you can clone the project in your IDE of choice and run the client from there.<br>
4. Send commands in the server terminal ([see server specifications](#server-specifications))<br>
5. Read [FAQ](#faq--known-bugs)

# Server specifications
```
app   Custom-made application

uc    UniformConsensus                  Leader-Driven                         5.7   225
ep    EpochConsensus                    Read/Write Epoch                      5.6   223
ec    EpochChange                       Leader-Based                          5.5   219

nnar  (N,N) Atomic Register             Read-Impose Write-Consult-Majority    4.10  168
                                                                              4.11  169

beb   BestEffortBroadcast               Basic                                 3.1    76
eld   EventualLeaderDetector            Monarchical Eventual Leader Detector  2.8    57
epfd  EventuallyPerfectFailureDetector  Increasing Timeout                    2.7    55

pl    PerfectLink (use TCP)
                                                                                                  AbstractionIds
   +--+-------+------ app -------+                            +--+-------+------ app -------+            = app
   |  |       |                  |                            |  |       |                  |
   |  |       |             +- uc,uc -+                       |  |       |             +- uc,uc -+       = app.uc[topic]
   |  |   nnar,nnar         |         |                       |  |   nnar,nnar         |         |       = app.nnar[register]
   |  |      | |      +---- ec --+    |                       |  |      | |      +---- ec --+    |       = app.uc[topic].ec
   |  |      | |      |     |    | ep,ep,ep                   |  |      | |      |     |    | ep,ep,ep   = app.uc[topic].ep[0], app.uc[topic].ep[1],
   |  |      | |      |     |    |    |  |                    |  |      | |      |     |    |    |  |            app.uc[topic].ep[2], ...
   |  |      | |     eld    |    |    |  |                    |  |      | |     eld    |    |    |  |    = app.uc[topic].ec.eld
   |  |      | |     epfd   |    |    |  |                    |  |      | |     epfd   |    |    |  |    = app.uc[topic].ec.eld.epfd
   | beb   beb |      |    beb   |   beb |                    | beb   beb |      |    beb   |   beb |    = app.beb, app.nnar[register].beb, uc[topic].ec.beb
  pl  pl    pl pl     pl    pl   pl   pl pl                  pl  pl    pl pl     pl    pl   pl   pl pl    = app.pl, app.beb.pl, app.nnar[register].beb.pl, app.nnar[register].pl,
   |  |      | |      |     |    |    |  |                    |  |      | |      |     |    |    |  |         app.uc[topic].ec.eld.epfd.pl, app.uc[topic].ec.beb.pl,
   |  |      | |      |     |    |    |  |  osproc    osproc  |  |      | |      |     |    |    |  |         app.uc[topic].ec.pl, app.uc[topic].ep[0].pl,
   |  |      | |      |     |    |    |  |       |    |       |  |      | |      |     |    |    |  |         app.uc[topic].ep[1].pl, app.uc[topic].ep[2].pl, ...
   +--+----- +-+------+-----+----+----+--+----- NETWORK ------+--+------+-+------+-----+----+----+--+
                                                | |
                                                | pl
                                                | |
                                                hub

1. The communication is done using the Google Protobuffer 3.x messages defined below, over TCP. The exchange will be
   asynchronous. When sending a request/response, open the TCP connection, send the message, then close the connection.
   When listening for requests, get the request, then close the socket.

2. The system consists of several processes and one hub. The processes are implementation of what the textbook refers
   to as a client, with the extension that they can participate in multiple systems. They should be able to route
   messages and events separately for each system that the client is involved into.

3. The hub is responsible of informing the processes of the system(s) they belong to, trigger algorithms, and receive
   notifications that it can use to validate the functionality.

4. Your job is to implement a client that can run the algorithms shown in the evaluation flow below. Use the reference
   binaries provided by your instructor to verify your implementation

5. Process referencing: Upon starting, a client will connect to the hub and register sending: owner alias, client
   index, client host, client listening port (see ProcRegistration). The hub address and port will be configured manually.

6. The evaluation will be done as follows:
       - Share your screen with the instructor
       - Start the reference hub and processes along with 3 processes of your implementation
           #            HUB HOST + PORT  PROCESSES HOST + PORTS
           > dalgs.exe  127.0.0.1  5000  127.0.0.1  5001 5002 5003
             May 18 12:17:01.474 INF Hub listening on 127.0.0.1:5000
             May 18 12:17:01.475 INF ref-2: listening on 127.0.0.1:5002
             May 18 12:17:01.475 INF ref-3: listening on 127.0.0.1:5003
             May 18 12:17:01.475 INF ref-1: listening on 127.0.0.1:5001
             May 18 12:17:11.475 INF abc-2: listening on 127.0.0.1:5005
             May 18 12:17:11.475 INF abc-3: listening on 127.0.0.1:5006
             May 18 12:17:11.475 INF abc-1: listening on 127.0.0.1:5004
           - Process-level messages exchanged in this phase
               - Every client sends Message(NetworkMessage(Message(ProcRegistration))) to the Hub
       - Assuming your client owner is "abc", here is a walk-through what you can do at the command prompt
           dalgs> help
               Commands:
                   log [info|debug|trace] - set logging level
                   quit                   - quit the program
                   help                   - show usage
                   list                   - list the nodes (hub only)
                   init owner1 owner2 ... - initialize system with owners nodes (hub only)
                   consensus topic        - test consensus on topic (hub only)
                   wait N                 - wait N seconds (hub only)             dalgs> log info
           dalgs> list
               +---+-------+-----------+--------+--------+--------+
               | # | OWNER | HOST      | PORT 1 | PORT 2 | PORT 3 |
               +---+-------+-----------+--------+--------+--------+
               | 1 | ref   | 127.0.0.1 | 5001   | 5002   | 5003   |
               +---+-------+-----------+--------+--------+--------+
               | 2 | abc   | 127.0.0.1 | 5004   | 5005   | 5006   |
               +---+-------+-----------+--------+--------+--------+
           dalgs> log info
       - Create a system
           dalgs> system ref abc
               09:40:39.910 INF Starting system sys-1 of client ref-1 ...
               09:40:39.911 INF Starting system sys-1 of client ref-2 ...
               09:40:39.913 INF Starting system sys-1 of client ref-3 ...
               09:40:39.913 INF Starting system sys-1 of client ref-3 ...
               09:40:39.913 INF Starting system sys-1 of client abc-1 ...
               09:40:39.913 INF Starting system sys-1 of client abc-2 ...
               09:40:39.913 INF Starting system sys-1 of client abc-3 ...
           - Process-level messages exchanged in this phase
               - Hub sends Message(NetworkMessage(Message(ProcDestroySystem))) to all the processes of the existing system (if there is an existing system)
               - Hub sends Message(NetworkMessage(Message(ProcInitializeSystem))) to ref-1, ref-2, ref-3, abc-1, abc-2, abc-3
       - Launch BEB
           dalgs> broadcast abc-2 52
               09:41:50.854 INF sys-1/abc-3 delivered 52
               09:41:50.855 INF sys-1/abc-1 delivered 52
               09:41:50.886 INF sys-1/ref-3 delivered 52
               09:41:50.886 INF sys-1/abc-2 delivered 52
               09:41:50.886 INF sys-1/ref-2 delivered 52
               09:41:50.922 INF sys-1/ref-1 delivered 52
           - App-level messages exchanged in this phase
               - Hub sends Message(NetworkMessage(Message(AppBroadcast(52)))) to abc-2
               - Hub expects Message(NetworkMessage(Message(AppValue(52)))) from all processes
       - Register write read
           dalgs> write x 89 abc-2
               14:50:26.465 INF                     sys-1/ref-2: Registering unknown abstraction app.nnar[x].beb.pl
               14:50:26.465 INF sys-1/ref-1: Registering unknown abstraction app.nnar[x].beb.pl
               14:50:26.467 INF                                         sys-1/ref-3: Registering unknown abstraction app.nnar[x].beb.pl
               14:50:26.566 INF hub: sys-1/abc-2 finished writing x
           - App-level messages exchanged in this phase
               - Hub sends Message(NetworkMessage(Message(AppWrite(x, 89)))) to abc-2
               - Hub expects Message(NetworkMessage(Message(AppWriteReturn(x)))) from all processes
           dalgs> read x
               INFO: No client name(s) provided. Triggering all processes
               14:50:35.678 INF hub: sys-1/abc-2 read x=89
               14:50:35.678 INF hub: sys-1/abc-1 read x=89
               14:50:35.679 INF hub: sys-1/ref-1 read x=89
               14:50:35.679 INF hub: sys-1/abc-3 read x=89
               14:50:35.707 INF hub: sys-1/ref-2 read x=89
               14:50:35.707 INF hub: sys-1/ref-3 read x=89
           - App-level messages exchanged in this phase
               - Hub sends Message(NetworkMessage(Message(AppRead(x)))) to all processes
               - Hub expects Message(NetworkMessage(Message(AppReadReturn(x, 89)))) from all processes
       - Register read/write storm
           dalgs> storm w       # a lot of reads and writes involving all processes
               15:00:21.023 INF                                         sys-1/ref-3: Registering unknown abstraction app.nnar[w].beb.pl
               15:00:21.025 INF                     sys-1/ref-2: Registering unknown abstraction app.nnar[w].beb.pl
               15:00:21.057 INF hub: sys-1/abc-1 finished writing w
               15:00:21.062 INF hub: sys-1/abc-2 finished writing w
               15:00:21.070 INF hub: sys-1/ref-1 finished writing w
               15:00:21.073 INF hub: sys-1/abc-3 finished writing w
               15:00:21.077 INF hub: sys-1/ref-2 finished writing w
               15:00:21.080 INF hub: sys-1/ref-3 finished writing w
               15:00:21.097 INF hub: sys-1/abc-1 read w=44
               15:00:21.114 INF hub: sys-1/abc-2 read w=44
               15:00:21.115 INF hub: sys-1/ref-1 finished writing w
               15:00:21.123 INF hub: sys-1/abc-3 read w=44
               15:00:21.125 INF hub: sys-1/ref-3 finished writing w
               15:00:21.127 INF hub: sys-1/ref-2 finished writing w
               15:00:21.154 INF hub: sys-1/abc-1 finished writing w
               15:00:21.164 INF hub: sys-1/ref-1 read w=77
               15:00:21.169 INF hub: sys-1/abc-2 finished writing w
               15:00:21.174 INF hub: sys-1/ref-2 read w=85
               15:00:21.174 INF hub: sys-1/abc-3 finished writing w
               15:00:21.176 INF hub: sys-1/ref-3 read w=85
               15:00:21.203 INF hub: sys-1/abc-1 read w=55
               15:00:21.211 INF hub: sys-1/ref-1 read w=55
               15:00:21.218 INF hub: sys-1/abc-2 read w=55
               15:00:21.240 INF hub: sys-1/ref-2 read w=55
               15:00:21.252 INF hub: sys-1/ref-3 read w=55
               15:00:21.246 INF hub: sys-1/abc-3 read w=55
       - Try to linearize the register operations. You will not be able to always do this due to the differences between
         the actual operation start/end moments, and those recorded by the hub upon notification
           dalgs> lin w
                                         1 1 1 1 1 1 1 1 1 1 2 2 2 2 2 2 2 2 2 2 3 3 3 3 3 3 3 3 3 3 4 4 4 4 4 4 4 4 4 4 5 5 5 5 5 5 5 5 5 5 6 6 6 6 6 6 6 6 6 6 7 7 7
                       1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2
               abc-1: wwwwww44wwwwww  rrrrrrrrrrrrrrrr44rrrrrrrrrrrrrrrr  wwwwwwwwwwwwwwww85wwwwwwwwwwwwwwww  rrrrrrrrrrrrrrrr55rrrrrrrrrrrrrrrr
               abc-2:     wwwwwwww61wwwwww  rrrrrrrrrrrrrrrr44rrrrrrrrrrrrrrrr  wwwwwwwwwwwwwwwwwwww42wwwwwwwwwwwwwwwwww  rrrrrrrrrrrrrr55rrrrrrrrrrrrrr
               abc-3:         wwwwwwwwwwww78wwwwwwwwww  rrrrrrrrrrrrrrrr44rrrrrrrrrrrrrrrr  wwwwwwwwwwwwwwwwwwww55wwwwwwwwwwwwwwwwww  rrrrrrrrrrrrrr55rrrrrrrrrrrrrr
               ref-1:   wwwwwwwwwwww15wwwwwwwwww  wwwwwwwwwwwwwwww77wwwwwwwwwwwwwwww  rrrrrrrrrrrrrr77rrrrrrrrrrrr  rrrrrrrrrrrrrrrr55rrrrrrrrrrrrrr
               ref-2:       wwwwwwwwwwwwwwww92wwwwwwwwwwwwww  wwwwwwwwwwwwwwwwwwww28wwwwwwwwwwwwwwwwww  rrrrrrrrrr85rrrrrrrrrr  rrrrrrrrrrrrrr55rrrrrrrrrrrr
               ref-3:           wwwwwwwwwwwwwwww76wwwwwwwwwwwwwwww  wwwwwwwwwwwwww72wwwwwwwwwwww  rrrrrrrrrrrrrrrrrrrr85rrrrrrrrrrrrrrrrrr  rrrrrrrrrr55rrrrrrrr
               The execution is not atomic
           dalgs.linearize> write 55 after 51        # Collapse all the reads with value 55 on or after moment 51
                                         1 1 1 1 1 1 1 1 1 1 2 2 2 2 2 2 2 2 2 2 3 3 3 3 3 3 3 3 3 3 4 4 4 4 4 4 4 4 4 4 5 5 5 5 5 5 5 5 5 5 6 6 6 6 6 6 6 6 6 6 7 7 7
                       1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2
               abc-1: wwwwww44wwwwww  rrrrrrrrrrrrrrrr44rrrrrrrrrrrrrrrr  wwwwwwwwwwwwwwww85wwwwwwwwwwwwwwww  rrrrrrrrrrrrrrrr55rrrrrrrrrrrrrrrr
               abc-2:     wwwwwwww61wwwwww  rrrrrrrrrrrrrrrr44rrrrrrrrrrrrrrrr  wwwwwwwwwwwwwwwwwwww42wwwwwwwwwwwwwwwwww  rrrrrrrrrrrrrr55rrrrrrrrrrrrrr
               abc-3:         wwwwwwwwwwww78wwwwwwwwww  rrrrrrrrrrrrrrrr44rrrrrrrrrrrrrrrr  wwwwwwwwwwwwwwwwwwwwwwwwwwwwww55wwwwwwww  rrrrrrrrrrrrrr55rrrrrrrrrrrrrr
               ref-1:   wwwwwwwwwwww15wwwwwwwwww  wwwwwwwwwwwwwwww77wwwwwwwwwwwwwwww  rrrrrrrrrrrrrr77rrrrrrrrrrrr  rrrrrrrrrrrrrrrr55rrrrrrrrrrrrrr
               ref-2:       wwwwwwwwwwwwwwww92wwwwwwwwwwwwww  wwwwwwwwwwwwwwwwwwww28wwwwwwwwwwwwwwwwww  rrrrrrrrrr85rrrrrrrrrr  rrrrrrrrrrrrrr55rrrrrrrrrrrr
               ref-3:           wwwwwwwwwwwwwwww76wwwwwwwwwwwwwwww  wwwwwwwwwwwwww72wwwwwwwwwwww  rrrrrrrrrrrrrrrrrrrr85rrrrrrrrrrrrrrrrrr  rrrrrrrrrr55rrrrrrrr
               The execution is not atomic
           dalgs.linearize> write 85 after 42
                                         1 1 1 1 1 1 1 1 1 1 2 2 2 2 2 2 2 2 2 2 3 3 3 3 3 3 3 3 3 3 4 4 4 4 4 4 4 4 4 4 5 5 5 5 5 5 5 5 5 5 6 6 6 6 6 6 6 6 6 6 7 7 7
                       1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2
               abc-1: wwwwww44wwwwww  rrrrrrrrrrrrrrrr44rrrrrrrrrrrrrrrr  wwwwwwwwwwwwwwwwwwwwwwwwwwwwww85ww  rrrrrrrrrrrrrrrr55rrrrrrrrrrrrrrrr
               abc-2:     wwwwwwww61wwwwww  rrrrrrrrrrrrrrrr44rrrrrrrrrrrrrrrr  wwwwwwwwwwwwwwwwwwwwwwww42wwwwwwwwwwwwww  rrrrrrrrrrrrrr55rrrrrrrrrrrrrr
               abc-3:         wwwwwwwwwwww78wwwwwwwwww  rrrrrrrrrrrrrrrr44rrrrrrrrrrrrrrrr  wwwwwwwwwwwwwwwwwwwwwwwwwwwwww55wwwwwwww  rrrrrrrrrrrrrr55rrrrrrrrrrrrrr
               ref-1:   wwwwwwwwwwww15wwwwwwwwww  wwwwwwwwwwwwwwww77wwwwwwwwwwwwwwww  rrrrrrrrrrrrrr77rrrrrrrrrrrr  rrrrrrrrrrrrrrrr55rrrrrrrrrrrrrr
               ref-2:       wwwwwwwwwwwwwwww92wwwwwwwwwwwwww  wwwwwwwwwwwwwwwwwwww28wwwwwwwwwwwwwwwwww  rrrrrrrrrr85rrrrrrrrrr  rrrrrrrrrrrrrr55rrrrrrrrrrrr
               ref-3:           wwwwwwwwwwwwwwww76wwwwwwwwwwwwwwww  wwwwwwwwwwwwww72wwwwwwwwwwww  rrrrrrrrrrrrrrrrrrrr85rrrrrrrrrrrrrrrrrr  rrrrrrrrrr55rrrrrrrr
               The execution is not atomic
           dalgs.linearize> write 42 28 72 before 30
                                         1 1 1 1 1 1 1 1 1 1 2 2 2 2 2 2 2 2 2 2 3 3 3 3 3 3 3 3 3 3 4 4 4 4 4 4 4 4 4 4 5 5 5 5 5 5 5 5 5 5 6 6 6 6 6 6 6 6 6 6 7 7 7
                       1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2
               abc-1: wwwwww44wwwwww  rrrrrrrrrrrrrrrr44rrrrrrrrrrrrrrrr  wwwwwwwwwwwwwwwwwwwwwwwwwwwwww85ww  rrrrrrrrrrrrrrrr55rrrrrrrrrrrrrrrr
               abc-2:     wwwwwwww61wwwwww  rrrrrrrrrrrrrrrr44rrrrrrrrrrrrrrrr  42wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww  rrrrrrrrrrrrrr55rrrrrrrrrrrrrr
               abc-3:         wwwwwwwwwwww78wwwwwwwwww  rrrrrrrrrrrrrrrr44rrrrrrrrrrrrrrrr  wwwwwwwwwwwwwwwwwwwwwwwwwwwwww55wwwwwwww  rrrrrrrrrrrrrr55rrrrrrrrrrrrrr
               ref-1:   wwwwwwwwwwww15wwwwwwwwww  wwwwwwwwwwwwwwww77wwwwwwwwwwwwwwww  rrrrrrrrrrrrrr77rrrrrrrrrrrr  rrrrrrrrrrrrrrrr55rrrrrrrrrrrrrr
               ref-2:       wwwwwwwwwwwwwwww92wwwwwwwwwwwwww  wwwwwwwwwwwwwwwwww28wwwwwwwwwwwwwwwwwwww  rrrrrrrrrr85rrrrrrrrrr  rrrrrrrrrrrrrr55rrrrrrrrrrrr
               ref-3:           wwwwwwwwwwwwwwww76wwwwwwwwwwwwwwww  wwwwwwwwwwww72wwwwwwwwwwwwww  rrrrrrrrrrrrrrrrrrrr85rrrrrrrrrrrrrrrrrr  rrrrrrrrrr55rrrrrrrr
               The execution is not atomic
           dalgs.linearize> write 77 after 31
                                         1 1 1 1 1 1 1 1 1 1 2 2 2 2 2 2 2 2 2 2 3 3 3 3 3 3 3 3 3 3 4 4 4 4 4 4 4 4 4 4 5 5 5 5 5 5 5 5 5 5 6 6 6 6 6 6 6 6 6 6 7 7 7
                       1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2
               abc-1: wwwwww44wwwwww  rrrrrrrrrrrrrrrr44rrrrrrrrrrrrrrrr  wwwwwwwwwwwwwwwwwwwwwwwwwwwwww85ww  rrrrrrrrrrrrrrrr55rrrrrrrrrrrrrrrr
               abc-2:     wwwwwwww61wwwwww  rrrrrrrrrrrrrrrr44rrrrrrrrrrrrrrrr  42wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww  rrrrrrrrrrrrrr55rrrrrrrrrrrrrr
               abc-3:         wwwwwwwwwwww78wwwwwwwwww  rrrrrrrrrrrrrrrr44rrrrrrrrrrrrrrrr  wwwwwwwwwwwwwwwwwwwwwwwwwwwwww55wwwwwwww  rrrrrrrrrrrrrr55rrrrrrrrrrrrrr
               ref-1:   wwwwwwwwwwww15wwwwwwwwww  wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww77  rrrrrrrrrrrrrr77rrrrrrrrrrrr  rrrrrrrrrrrrrrrr55rrrrrrrrrrrrrr
               ref-2:       wwwwwwwwwwwwwwww92wwwwwwwwwwwwww  wwwwwwwwwwwwwwwwww28wwwwwwwwwwwwwwwwwwww  rrrrrrrrrr85rrrrrrrrrr  rrrrrrrrrrrrrr55rrrrrrrrrrrr
               ref-3:           wwwwwwwwwwwwwwww76wwwwwwwwwwwwwwww  wwwwwwwwwwww72wwwwwwwwwwwwww  rrrrrrrrrrrrrrrrrrrr85rrrrrrrrrrrrrrrrrr  rrrrrrrrrr55rrrrrrrr
               The execution is not atomic
           dalgs.linearize> write 61 78 15 92 76 before 6
                                         1 1 1 1 1 1 1 1 1 1 2 2 2 2 2 2 2 2 2 2 3 3 3 3 3 3 3 3 3 3 4 4 4 4 4 4 4 4 4 4 5 5 5 5 5 5 5 5 5 5 6 6 6 6 6 6 6 6 6 6 7 7 7
                       1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2
               abc-1: wwwwww44wwwwww  rrrrrrrrrrrrrrrr44rrrrrrrrrrrrrrrr  wwwwwwwwwwwwwwwwwwwwwwwwwwwwww85ww  rrrrrrrrrrrrrrrr55rrrrrrrrrrrrrrrr
               abc-2:     wwwwww61wwwwwwww  rrrrrrrrrrrrrrrr44rrrrrrrrrrrrrrrr  42wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww  rrrrrrrrrrrrrr55rrrrrrrrrrrrrr
               abc-3:         ww78wwwwwwwwwwwwwwwwwwww  rrrrrrrrrrrrrrrr44rrrrrrrrrrrrrrrr  wwwwwwwwwwwwwwwwwwwwwwwwwwwwww55wwwwwwww  rrrrrrrrrrrrrr55rrrrrrrrrrrrrr
               ref-1:   wwwwwwww15wwwwwwwwwwwwww  wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww77  rrrrrrrrrrrrrr77rrrrrrrrrrrr  rrrrrrrrrrrrrrrr55rrrrrrrrrrrrrr
               ref-2:       wwww92wwwwwwwwwwwwwwwwwwwwwwwwww  wwwwwwwwwwwwwwwwww28wwwwwwwwwwwwwwwwwwww  rrrrrrrrrr85rrrrrrrrrr  rrrrrrrrrrrrrr55rrrrrrrrrrrr
               ref-3:           76wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww  wwwwwwwwwwww72wwwwwwwwwwwwww  rrrrrrrrrrrrrrrrrrrr85rrrrrrrrrrrrrrrrrr  rrrrrrrrrr55rrrrrrrr
               The execution is not atomic
           dalgs.linearize> write 44 after 7
                                         1 1 1 1 1 1 1 1 1 1 2 2 2 2 2 2 2 2 2 2 3 3 3 3 3 3 3 3 3 3 4 4 4 4 4 4 4 4 4 4 5 5 5 5 5 5 5 5 5 5 6 6 6 6 6 6 6 6 6 6 7 7 7
                       1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2
               abc-1: wwwwwwwwwwww44  rrrrrrrrrrrrrrrr44rrrrrrrrrrrrrrrr  wwwwwwwwwwwwwwwwwwwwwwwwwwwwww85ww  rrrrrrrrrrrrrrrr55rrrrrrrrrrrrrrrr
               abc-2:     wwwwww61wwwwwwww  rrrrrrrrrrrrrrrr44rrrrrrrrrrrrrrrr  42wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww  rrrrrrrrrrrrrr55rrrrrrrrrrrrrr
               abc-3:         ww78wwwwwwwwwwwwwwwwwwww  rrrrrrrrrrrrrrrr44rrrrrrrrrrrrrrrr  wwwwwwwwwwwwwwwwwwwwwwwwwwwwww55wwwwwwww  rrrrrrrrrrrrrr55rrrrrrrrrrrrrr
               ref-1:   wwwwwwww15wwwwwwwwwwwwww  wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww77  rrrrrrrrrrrrrr77rrrrrrrrrrrr  rrrrrrrrrrrrrrrr55rrrrrrrrrrrrrr
               ref-2:       wwww92wwwwwwwwwwwwwwwwwwwwwwwwww  wwwwwwwwwwwwwwwwww28wwwwwwwwwwwwwwwwwwww  rrrrrrrrrr85rrrrrrrrrr  rrrrrrrrrrrrrr55rrrrrrrrrrrr
               ref-3:           76wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww  wwwwwwwwwwww72wwwwwwwwwwwwww  rrrrrrrrrrrrrrrrrrrr85rrrrrrrrrrrrrrrrrr  rrrrrrrrrr55rrrrrrrr
               The execution is atomic
           dalgs.linearize> done
       - Launch consensus on a topic
           dalgs> consensus mytopic
               INFO: sys-1/abc-1 will propose 89
               INFO: sys-1/ref-1 will propose 85
               INFO: sys-1/abc-2 will propose 85
               INFO: sys-1/ref-2 will propose 47
               INFO: sys-1/abc-3 will propose 52
               INFO: sys-1/ref-3 will propose 37
               09:42:45.310 INF sys-1/abc-2 decided 37
               09:42:45.342 INF sys-1/ref-2 decided 37
               09:42:45.343 INF sys-1/ref-3 decided 37
               09:42:45.344 INF sys-1/abc-3 decided 37
               09:42:45.353 INF sys-1/ref-1 decided 37
               09:42:45.356 INF sys-1/abc-1 decided 37
           - App-level messages exchanged in this phase
               - Hub sends Message(NetworkMessage(Message(AppPropose(mytopic, randomValue)))) to all processes
               - Hub expects Message(NetworkMessage(Message(AppDecided(mytopic, decidedValue)))) from all processes
           dalgs> quit
               09:43:49.028 INF Stopping client ref-1 ...
               09:43:49.030 WRN Unexpected message type EPFD_INTERNAL_HEARTBEAT_REQUEST for unknown system sys-1
               09:43:49.031 INF Stopping client ref-2 ...
               09:43:49.032 WRN Unexpected message type EPFD_INTERNAL_HEARTBEAT_REQUEST for unknown system sys-1
               09:43:49.037 WRN Unexpected message type EPFD_INTERNAL_HEARTBEAT_REQUEST for unknown system sys-1
               09:43:50.031 INF Stopping client ref-3 ...
               09:43:50.042 INF Stopping hub ...
               INFO: Stopped
       - A few comments on how this works
         - Log level debug shows all messages except for those related to heartbeat
         - Log level trace will show everything
         - The commands log messages are written over the command prompt, but you can always type "blindly" and
           hit ENTER. This may become necessary if the trace logging is too much; just type "log debug" and hit ENTER.
         - When the algorithm is over, it will seem stuck, but in fact is just waiting for another command. Hit ENTER
           and you will see the prompt again.
         - Look for INF log entries showing what each client has delivered/read/decided
         - Everything you see in the console is also logged in file dalgs-ref.log
         - The errors after quit are caused by the stopping heartbeat exchange, and can be ignored
```

# FAQ / Known bugs
- <b>Port exhaustion when using consensus command</b> - There is mostly no way of skipping (without breaking everything) 
the few minutes in which the TCP connection stays in the TIME_WAIT state after closing (at least on Windows), 
so the heartbeat message connections will exhaust all available ports on your machine sooner or later.
- <b>Some messages are missing UUID</b> - Server-side issue.
- <b>Doing a storm operation leaves the registry in an unfinished read state</b> - Even the server-bundled clients fail
in that regard, so I'm inclined to say it's a server-side issue.

## Bibliography
* _Introduction to Reliable and Secure Distributed Programming by C. Cachin, R. Guerraoui, L. Rodrigues_
