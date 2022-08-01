package WebServer;// Papageorgiou Efthymios 4340
// csd4340@csd.uoc.gr

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        System.out.print("-Enter your Welcome Port: ");
        Scanner s = new Scanner(System.in);
        int myPort = s.nextInt();
        int toConnectPort = -1;
        String answer;

        do {
            System.out.print("-Do you want to join the team: [Y/n]: ");
            answer = s.next();
            if (answer.equals("Y") || answer.equals("y")) {
                do {
                    System.out.print("-Enter the port[4000,15000] of the Web Server that you want to connect to: ");
                    toConnectPort = s.nextInt();
                } while (toConnectPort < 4000 || toConnectPort > 15000 || toConnectPort == myPort);
            }
        }while (!answer.equals("Y") && !answer.equals("y") && !answer.equals("N") && !answer.equals("n"));

        new WebServer(myPort, toConnectPort);

    }
}
