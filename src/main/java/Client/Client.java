package Client;// Papageorgiou Efthymios 4340
// csd4340@csd.uoc.gr

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class Client {

    private Socket clientSocket;
    private DataOutputStream outToServer;
    private BufferedReader inFromServer;

    public Client(int toConnectPort) {
        try {
            clientSocket = new Socket("localhost", toConnectPort);
            outToServer  = new DataOutputStream(clientSocket.getOutputStream());
            System.out.println("Connection with Web Server " + toConnectPort + " Established!");
        } catch (IOException e) {
            e.printStackTrace();
        }

        StringBuilder requestMessage = new StringBuilder();
        String subMessage;
        BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Enter a message:");
        try {
            do {
                subMessage = userInput.readLine();
                requestMessage.append(subMessage).append("\n");
            } while (!subMessage.endsWith("$"));
            outToServer.writeBytes(requestMessage +"\r\n");
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("::From Server::");
        String serverReply;
        try {
            inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            serverReply  = inFromServer.readLine();
            while(!serverReply.equals("$")) {
                System.out.println(serverReply);
                serverReply = inFromServer.readLine();
            }
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}