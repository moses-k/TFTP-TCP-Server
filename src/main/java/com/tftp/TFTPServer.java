package com.tftp;

import java.io.File;

public class TFTPServer {
    private static String dbFolder = "server_DB/";

    public static void main(String[] args) {
        //check for the folder
        File file = new File("server_DB");
        if (file.exists()) {
            //System.out.println("server_DB  exist");
        }else {
            //System.out.println(" creating new folder server_DB ...");
            (new File(dbFolder)).mkdir();
        }
        Server server = new Server(5000, dbFolder);

    }
}
