package utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class MainServerSocket {

    public static void main(String[] args) throws Exception {
        String request;
        String response;
        ServerSocket server = new ServerSocket(6789);
        System.out.println("IP: " + server.getInetAddress() + " port: " + server.getLocalPort());
        Socket client = server.accept();
        String clientAddress = client.getInetAddress().getHostAddress();
        System.out.println("\r\nNew connection from " + clientAddress);

        while (true) {
            Socket connectionSocket = server.accept();
            BufferedReader inFromClient =
                    new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
            DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
            request = inFromClient.readLine();

            if (request.equals("currentVolume")) {
                System.out.println("Requested current volume value");
                response = (Math.round(VolumeControl.getMasterOutputVolume() * 100)) + "" + '\n';
                System.out.println("current volume: " + response + " (it will be send as response)");
                outToClient.writeBytes(response);
            } else {
                System.out.println("Requested new value for volume: " + request);
                float requestedVolumeValue = Integer.parseInt(request) / 100f;
                System.out.println("Parsed value: " + Integer.parseInt(request));
                VolumeControl.setMasterOutputVolume(requestedVolumeValue);
                response = VolumeControl.getMasterOutputVolume().toString() + '\n';
                outToClient.writeBytes(response);
            }
        }
    }

}
