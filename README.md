## Dependencies
Flirc2 needs:

1. Java 8
2. Maven
3. A set of libraries in GitHub

To install the set of libraries from GitHub:

    git clone https://github.com/gbromfield/GrbLibs2.git
    cd Grblibs2
    mvn install
    cd ..

## Source Files
Checkout and build the source from GitHub.

    git clone https://github.com/gbromfield/Insidious.git
    cd Insidious
    mvn clean compile assembly:single

## Executable jar
Under the "target" directory a standalone executable jar should exist: 
target/flirc2-0.0.1-jar-with-dependencies.jar

## Command Line Help

    java -jar flirc2-0.0.1-jar-with-dependencies.jar -?

## Running
There are three modes of running. 

1. Conversion mode that converts RA log files to recordings, and 
2. Simulator mode that plays back recordings, and
3. Interactive mode that converts TL1 raw output to an escaped single line suitable for a recording file.
    
### Log file Conversion
To convert log files:

    java -jar flirc2-0.0.1-jar-with-dependencies.jar -f {RA log file or directory} -rec {recording file}

### Starting the Simulator
To start the simulator:

    java -jar flirc2-0.0.1-jar-with-dependencies.jar

When the simulator is started it receives instructions through the rest interface (port 4567) to load recordings 
and open up ports.

### Interactive Mode
To start interactive mode:

    java -jar flirc2-0.0.1-jar-with-dependencies.jar -i



## REST Interface

### Creating a TL1 Server on port 12349 and loading a capture from a file
POST http://localhost:4567/servers with payload below to load a recording from a file:

    {
      "protocol": "tl1",
      "port": "12349",
      "recordingURLs": [
          "file:///Users/gbromfie/Development/Insidious/samples/sample2.json"
      ]
    }

If the port parameter is onitted or "0", a port will be picked and the value will be returned in the response.

A sample response:

    {
      "protocol": "tl1",
      "port": "12349",
      "sessions": []
    }

"Protocol" and "port" are copied back.

### Getting a Server
GET http://localhost:4567/server/{port}

### Deleting a Server
DELETE http://localhost:4567/server/{port}

### Getting all Servers
GET http://localhost:4567/servers

### Deleting all Servers
DELETE http://localhost:4567/servers

When a server is deleted, all sessions are terminated and you will no longer be able to connect to that port.

### Getting a session on a server
GET http://localhost:4567/server/{port}/session/{session}

The session id can be retrieved from the GET http://localhost:4567/server/{port} api.

### Creating a TL1 Server on port 12349 and providing an inline recording
POST http://localhost:4567/servers with payload below to load an inline recording:

    {
      "protocol": "tl1",
      "port": "12349",
      "recording": [{
        "protocol": "tl1",
        "timestamp": "2016-10-19 14:17:29,676",
        "input": "ACT-USER:\"PV0414E\":ADMIN:10001::ADMIN;"
      },
      {
        "protocol": "tl1",
        "timestamp": "2016-10-19 14:17:29,676",
        "output": "\r\n\n \"PV0414E\" 16-10-19 14:19:37\r\nM 10001 COMPLD\r\n;"
      },
      {
        "protocol": "tl1",
        "timestamp": "2016-10-19 14:17:32,135",
        "input": "RTRV-NETYPE:PV0414E::2;"
      },
      {
        "protocol": "tl1",
        "timestamp": "2016-10-19 14:17:35,372",
        "output": "\r\n\n \"PV0414E\" 15-12-09 06:01:41\r\n*C 1 REPT ALM EQPT\r\n \"40GMUX-1-10:CL,EQPT_MISSING,SA,11-26,14-14-33,NEND,NA:\\\"Circuit Pack Missing\\\",NONE:0100000045-0062-0035,:YEAR=2015,MODE=NONE\"\r\n;"
      },
      { 
        "protocol": "tl1",
        "timestamp": "2016-10-19 14:17:38,372",
        "output": "\r\n\n \"PV0414E\" 16-10-19 14:19:36\r\nM 2 COMPLD\r\n \"CIENA,\\\"6500 OPTICAL\\\",CNE,\\\"REL1150Z.WM\\\"\"\r\n;"
      }]
    }

To turn on debugging:
-Dorg.slf4j.simpleLogger.defaultLogLevel=debug
-Dorg.slf4j.simpleLogger.log.com.grb.flirc2=DEBUG