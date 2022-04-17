package com.tftp;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    //initialize socket and input stream
    private Socket socket = null;
    private ServerSocket server = null;
    private DataInputStream in = null;

    public Server(int port,String dbFolder) {
        // starts server and waits for a connection
        try {
            server = new ServerSocket(port);
            System.out.println(" Server started "+ server.getLocalSocketAddress());

            while (true) {
                try {
                    socket = server.accept();
                    System.out.println("Client accepted");
                } catch (IOException e) {
                    System.out.println("I/O error: " + e.getMessage());
                }
                // new thread for a client
                new ServerThread(socket,dbFolder).start();
            }

        } catch (IOException i) {
            System.out.println(i);
        }
    }

}
