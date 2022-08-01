package Client;// Papageorgiou Efthymios 4340
// csd4340@csd.uoc.gr

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        Scanner s = new Scanner(System.in);
        int toConnectPort = -1;

        do {
            System.out.print("-Enter the port[4000,15000] of the Web Server that you want to connect to: ");
            toConnectPort = s.nextInt();
        } while (toConnectPort < 4000 || toConnectPort > 15000);
        new Client(toConnectPort);
    }
}
