package WebServer;// Papageorgiou Efthymios 4340
// csd4340@csd.uoc.gr


import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

public class WebServer {

    private String host = "localhost";
    private ServerSocket welcomeSocket;
    private ArrayList<Integer> webServerTeamPorts;
    private int currentWebServerIndex = -1;
    private int listeningPort;
    private int webServerPort;
    private Socket clientSocketConnection;
    private HashMap<String, String> data;

    public WebServer(int listeningPort, int webServerPort) {

        this.listeningPort = listeningPort;
        this.webServerPort = webServerPort;

        webServerTeamPorts = new ArrayList<>();

        if(webServerPort == -1) {
            currentWebServerIndex = 0;
            webServerTeamPorts.add(listeningPort);
        }
        else sendJoinTeamRequest();

        data = new HashMap<>();
        data.put("red","apple\n");
        data.put("yellow","banana\n");
        data.put("green", "mellon\n");

        new Thread(new HealthCheckerHandler()).start();

        try {
            listen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void listen() throws IOException {
        Socket connectionSocket;

        welcomeSocket = new ServerSocket(listeningPort);
        System.out.println("::Server Started::");
        while(true) {
            System.out.println((char)27 + "[34m" + "Waiting for a connection.." + (char)27 + "[39m");
            connectionSocket = welcomeSocket.accept();
            System.out.println((char)27 + "[36m" + "Connection Established!" + (char)27 + "[39m");
            new Thread(new RequestHandler(connectionSocket)).start();
        }
    }

    private void sendJoinTeamRequest() {

        try {
            Socket socket                = new Socket(host, webServerPort);
            DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
            outToServer.writeBytes("WS JOIN " + listeningPort + "\n$\n");

            System.out.println((char)27 + "[36m" + "Successfully joined the team!" + (char)27 + "[39m");
        } catch (IOException e) {
            System.out.println("This Web Server doesn't exists");
            e.printStackTrace();
        }
    }

    private void notifyWebServerAboutListChanges(Integer senderPort) throws IOException {
        int numberOfMembers = webServerTeamPorts.size();
        Socket socket;

        if(numberOfMembers == 1) return;

        //if current web server is the last member of the team
        if(numberOfMembers-1 == currentWebServerIndex) {
            //then communicate with the first member of the team
            socket = new Socket(host, webServerTeamPorts.get(0));
        } else {
            //else communicate with the next member of the team
            socket = new Socket(host, webServerTeamPorts.get(currentWebServerIndex+1));
        }

        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        dataOutputStream.writeBytes("WS RECEIVE_LIST " + senderPort + "\n");
        for(Integer i : webServerTeamPorts) {
            dataOutputStream.writeBytes(i + " ");
        }
        dataOutputStream.writeBytes("\n$\n");
        socket.close();
    }

    private void updateMyCurrentWebServerIndex() {
        for(int i = 0; i < webServerTeamPorts.size(); i++) {
            if(webServerTeamPorts.get(i) == listeningPort) {
                currentWebServerIndex = i;
                return;
            }
        }
    }

    private void printWebServerTeamPorts() {
        for(Integer i : webServerTeamPorts) {
            System.out.print("[" + i + "]");
        }
        System.out.println();
    }

    private class RequestHandler implements Runnable {

        private Socket connectionSocket;

        public RequestHandler(Socket connectionSocket) {
            this.connectionSocket = connectionSocket;
        }

        @Override
        public void run() {
            BufferedReader inFromClient;

            try {
                inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
                processRequest(inFromClient);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println((char)27 + "[31m" + "Connection closed!" + (char)27 + "[39m");
        }

        private void processRequest(BufferedReader inFromClient) throws IOException {
            String key;
            String httpVersion;
            String wsOperation;
            String toConnectPort;

            String request                = inFromClient.readLine();
            StringTokenizer tokenizedLine = new StringTokenizer(request);
            String method                 = tokenizedLine.nextToken();

            StringBuilder value = new StringBuilder();
            String subValue;

            DataOutputStream dataOutputStream = new DataOutputStream(connectionSocket.getOutputStream());

            switch (method) {
                case "GET":
                    System.out.println("A client made a [GET] Request");
                    key = tokenizedLine.nextToken();
                    clientSocketConnection = connectionSocket;
                    handleGetRequest(key);
                    break;
                case "PUT":
                    System.out.println("A client made a [PUT] Request");
                    key         = tokenizedLine.nextToken();
                    httpVersion = tokenizedLine.nextToken();
                    subValue    = inFromClient.readLine();
                    while (!subValue.equals("$")) {
                        value.append(subValue).append("\n");
                        subValue = inFromClient.readLine();
                    }
                    handlePutRequest(httpVersion, key, value.toString());
                    break;
                case "WS":
                    wsOperation = tokenizedLine.nextToken();
                    switch (wsOperation) {
                        case "JOIN":
                            toConnectPort = tokenizedLine.nextToken();
                            System.out.println("New Web Server("+ toConnectPort +") wants to join");
                            linkWebServer(Integer.parseInt(toConnectPort));
                            printWebServerTeamPorts();
                            break;
                        case "RECEIVE_LIST":
                            String senderPort     = tokenizedLine.nextToken();
                            String ports          = inFromClient.readLine();
                            StringTokenizer token = new StringTokenizer(ports);
                            //Update my List
                            webServerTeamPorts.clear();
                            while (token.hasMoreTokens()) {
                                webServerTeamPorts.add(Integer.parseInt(token.nextToken()));
                            }
                            System.out.print("Received List: ");
                            printWebServerTeamPorts();
                            updateMyCurrentWebServerIndex();

                            //Redirect Request
                            System.out.println("My Position: " + currentWebServerIndex);
                            System.out.println("List   Size: " + webServerTeamPorts.size());
                            if (currentWebServerIndex == webServerTeamPorts.size()-1) {
                                if(webServerTeamPorts.get(0) != Integer.parseInt(senderPort)) {
                                    while(true)  {
                                        try {
                                            notifyWebServerAboutListChanges(Integer.parseInt(senderPort));
                                            break;
                                        } catch (ConnectException ignored) {}
                                    }
                                }
                            } else {
                                if(webServerTeamPorts.get(currentWebServerIndex+1) != Integer.parseInt(senderPort)) {
                                    while(true)  {
                                        try {
                                            notifyWebServerAboutListChanges(Integer.parseInt(senderPort));
                                            break;
                                        } catch (ConnectException ignored) {}
                                    }
                                }
                            }
                            break;
                        case "UPDATE_DATA":
                            key        = tokenizedLine.nextToken();
                            senderPort = tokenizedLine.nextToken();

                            subValue    = inFromClient.readLine();
                            while (!subValue.equals("$")) {
                                value.append(subValue).append("\n");
                                subValue = inFromClient.readLine();
                            }
                            data.put(key, value.toString());

                            Socket nextWsSocket;
                            if(listeningPort != Integer.parseInt(senderPort)) {
                                if(currentWebServerIndex == webServerTeamPorts.size()-1)
                                    nextWsSocket = new Socket(host, webServerTeamPorts.get(0));
                                else
                                    nextWsSocket = new Socket(host, webServerTeamPorts.get(currentWebServerIndex+1));

                                DataOutputStream nextWsOutputStream = new DataOutputStream(nextWsSocket.getOutputStream());
                                nextWsOutputStream.writeBytes("WS UPDATE_DATA " + key + " " + senderPort + "\n");
                                nextWsOutputStream.writeBytes(value.toString());
                                nextWsOutputStream.writeBytes("$\n");
                            }
                            break;
                        case "HEALTH_CHECK":
                            dataOutputStream.writeBytes("WS HEALTH_OK\n$\n");
                            break;
                        case "GET":
                            key        = tokenizedLine.nextToken();
                            senderPort = tokenizedLine.nextToken();
                            if(Integer.parseInt(senderPort) == listeningPort) {
                                DataOutputStream outToClient = new DataOutputStream(clientSocketConnection.getOutputStream());
                                subValue    = inFromClient.readLine();
                                while (!subValue.equals("$")) {
                                    value.append(subValue).append("\n");
                                    subValue = inFromClient.readLine();
                                }

                                Socket socket;
                                if(currentWebServerIndex == webServerTeamPorts.size()-1)
                                    socket = new Socket(host, webServerTeamPorts.get(0));
                                else
                                    socket = new Socket(host, webServerTeamPorts.get(currentWebServerIndex+1));

                                DataOutputStream nextWsDataOutputStream = new DataOutputStream(socket.getOutputStream());
                                nextWsDataOutputStream.writeBytes("WS UPDATE_DATA " + key + " " + senderPort + "\n");
                                nextWsDataOutputStream.writeBytes(value.toString());
                                nextWsDataOutputStream.writeBytes("$\n");

                                if(value.length() > 0) {
                                    outToClient.writeBytes(value.toString());
                                    outToClient.writeBytes("$\n");
                                } else {
                                    outToClient.writeBytes("HTTP 404 NOT_FOUND\n");
                                    outToClient.writeBytes("$\n");
                                }
                            } else {
                                Socket socket;
                                if(currentWebServerIndex == webServerTeamPorts.size()-1)
                                    socket = new Socket(host, webServerTeamPorts.get(0));
                                else
                                    socket = new Socket(host, webServerTeamPorts.get(currentWebServerIndex+1));

                                if(data.containsKey(key)) {
                                    DataOutputStream dataToNextWS = new DataOutputStream(socket.getOutputStream());
                                    dataToNextWS.writeBytes("WS GET " + key + " " + senderPort + "\n");
                                    dataToNextWS.writeBytes(data.get(key) + "\n");
                                    dataToNextWS.writeBytes("$\n");
                                } else {
                                    DataOutputStream dataToNextWS = new DataOutputStream(socket.getOutputStream());
                                    dataToNextWS.writeBytes("WS GET " + key + " " + senderPort + "\n");
                                    dataToNextWS.writeBytes("$\n");
                                }
                            }
                            break;
                    }
                    connectionSocket.close();
                    break;
                default:
                    feedbackToClient("HTTP 400 Bad_Request");
                    break;
            }
        }

        private void handleGetRequest(String key) {

            try {
                DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
                if(data.containsKey(key)) {
                    outToClient.writeBytes(data.get(key));
                    outToClient.writeBytes("$\n");
                } else {
                    System.out.println("Key " + key + " doesn't exists on Web Server " + listeningPort);
                    System.out.println("Redirecting request..");
                    Socket socket;
                    if(currentWebServerIndex == webServerTeamPorts.size()-1)
                        socket = new Socket(host, webServerTeamPorts.get(0));
                    else
                        socket = new Socket(host, webServerTeamPorts.get(currentWebServerIndex + 1));

                    DataOutputStream dataToNextServer = new DataOutputStream(socket.getOutputStream());
                    dataToNextServer.writeBytes("WS GET " + key + " " + listeningPort + "\n$\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void handlePutRequest(String httpVersion, String key, String value) {
            if(value.length() == 0) {
                feedbackToClient("Seriously? Empty Value? (>_<)");
                return;
            }

            Socket socket;
            try {
                if(currentWebServerIndex == webServerTeamPorts.size()-1)
                    socket = new Socket(host, webServerTeamPorts.get(0));
                else
                    socket = new Socket(host, webServerTeamPorts.get(currentWebServerIndex+1));

                DataOutputStream nextWsDataOutputStream = new DataOutputStream(socket.getOutputStream());
                nextWsDataOutputStream.writeBytes("WS UPDATE_DATA " + key + " " + listeningPort + "\n");
                nextWsDataOutputStream.writeBytes(value);
                nextWsDataOutputStream.writeBytes("$\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
            boolean keyExists = data.containsKey(key);
            data.put(key, value);

            if(keyExists) feedbackToClient(httpVersion + " 200 Updated");
            else          feedbackToClient(httpVersion + " 201 Created");
        }

        private void linkWebServer(int port) throws IOException {
            System.out.print("Updating list.. ");
            webServerTeamPorts.add(port);
            System.out.println("[OK]");
            System.out.print("Notify other members.. ");
            while(true)  {
                try {
                    notifyWebServerAboutListChanges(listeningPort);
                    break;
                } catch (ConnectException ignored) {

                }
            }
            System.out.println("[OK]");
            System.out.println("Web Server " + port + " joined the team");
        }

        private void feedbackToClient(String message) {
            try {
                DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
                outToClient.writeBytes(message);
                outToClient.writeBytes("\n$\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class HealthCheckerHandler implements Runnable {

        @Override
        public void run() {
            while(true) {
                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (webServerTeamPorts.size() > 1) {
                    if(currentWebServerIndex == webServerTeamPorts.size()-1) {
                        try {
                            Socket socket = new Socket(host, webServerTeamPorts.get(0));
                            DataOutputStream data = new DataOutputStream(socket.getOutputStream());
                            data.writeBytes("WS HEALTH_CHECK\n$\n");

                            BufferedReader inFromWebServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            String line = inFromWebServer.readLine();
                            StringTokenizer tokenizer = new StringTokenizer(line);
                            if(tokenizer.nextToken().equals("WS")) {
                                if(tokenizer.nextToken().equals("HEALTH_OK")) {
                                    System.out.println("Web Server " + webServerTeamPorts.get(0) + " is Healthy");
                                }
                            }
                        } catch (IOException e) {
                            System.out.println("Web Server " + webServerTeamPorts.get(0) + " is Dead");
                            System.out.print("Updating list.. ");
                            removeDeadWebServer(0);
                            System.out.println("[OK]");
                            updateMyCurrentWebServerIndex();
                            System.out.print("Notify team about the change.. ");
                            try {
                                while(true)  {
                                    try {
                                        notifyWebServerAboutListChanges(listeningPort);
                                        break;
                                    } catch (ConnectException ignored) {

                                    }
                                }
                                System.out.println("[OK]");
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                    else {
                        try {
                            Socket socket = new Socket(host, webServerTeamPorts.get(currentWebServerIndex+1));
                            DataOutputStream data = new DataOutputStream(socket.getOutputStream());
                            data.writeBytes("WS HEALTH_CHECK\n$\n");

                            BufferedReader inFromWebServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            String line = inFromWebServer.readLine();
                            StringTokenizer tokenizer = new StringTokenizer(line);
                            if(tokenizer.nextToken().equals("WS")) {
                                if(tokenizer.nextToken().equals("HEALTH_OK")) {
                                    System.out.println("Web Server " + webServerTeamPorts.get(currentWebServerIndex+1) + " is Healthy");
                                }
                            }
                        } catch (IOException e) {
                            System.out.println("Web Server " + webServerTeamPorts.get(currentWebServerIndex+1) + " is Dead");
                            System.out.print("Updating list..");
                            removeDeadWebServer(currentWebServerIndex+1);
                            System.out.println("[OK]");
                            updateMyCurrentWebServerIndex();
                            System.out.print("Notify team about the change.. ");
                            try {
                                while(true)  {
                                    try {
                                        notifyWebServerAboutListChanges(listeningPort);
                                        break;
                                    } catch (ConnectException ignored) {}
                                }
                                System.out.println("[OK]");
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            }
        }

        private void removeDeadWebServer(int index) {
            webServerTeamPorts.remove(index);
        }
    }
}