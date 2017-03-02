#Project-Fluffy#

Project-Fluffy is an effort towards developing a system that can store, retrieve, update and delete data serving across multiple nodes.The functionality is similar to storage on cloud or on a network of servers with the features of fault tolerance and file consistency. With the emphasis on device transparency, work balance across stacks and failures survival, we design and implement a new approach to storing, finding, deleting, and updated data in a massive, decentralized, heterogeneous platform. The following features are part of the scope: Search capabilities with real-time monitoring of work and data and constant leader election when timeout on distributed system handling data storage and massive registration processes to store, share and delete.


## Instructions to run ##

### Prerequisites : ###
    This project requires the following JAR files(They are present in the /lib folder of the project).

    commons-beanutils.jar
    commons-codec-1.3.jar
    commons-collections.jar
    commons-httpclient-3.1.jar
    commons-io-2.4.jar
    commons-lang.jar
    commons-logging-1.1.jar
    couchdb4j-0.1.2.jar
    ezmorph-1.0.3.jar
    http-core-4.1.jar
    httpclient-4.1.1.jar
    httpcore-4.0-beta2.jar
    jackson-all-1.8.5.jar
    jackson-core-2.8.4.jar
    jackson-databind-2.8.3.jar
    json-20160212.jar
    json-lib-2.2.3-MODIFIED-jdk15.jar
    netty-all-4.0.15.Final.jar
    protobuf-java-2.6.1.jar
    riak-client-2.0.7-jar-with-dependencies (1).jar
    slf4j-api-1.7.2.jar
    slf4j-simple-1.7.2.jar

In the terminal, navigate to the folder directory where the project files are located.
Run the shell script build_pb.sh to build the .proto files as follows:

    ./build_pb.sh

Run the ant build script build.xml to build the project.

    ant

### How to run a server ###
Run the shell script startServer.sh and provide the following arguments to run it.

    startServer.sh <routing-conf-filename> <global-routing-conf-filename>

<routing-conf-filename>
The file which contains the routing information for the node. Eg : route-1.conf.

<global-routing-conf-filename>
The file which contains the information for the Global Configuration of the node. Eg :route-globalconf-4.conf


Run the shell script as follows  :

    ./startServer.sh  route-2.conf global-1.conf

The server should be started at this point.

### How to run a client (Java) ###
For the java based client, run the gash.router.app.DemoApp.java file and follow the instructions on screen.

    ./runPing.sh <Leader Host IP> <Leader Host Port>

### How to run a client (Python) ###
Prerequisites: Google's Protobuf

    ./build_pb.sh

For the python based client, run the Python Client as follows :

    python DemoApp.py <Leader Host IP> <Leader Host Port>
