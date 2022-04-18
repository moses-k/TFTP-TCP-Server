package com.tftp;

import java.io.*;
import java.math.BigDecimal;
import java.net.Socket;
import java.util.Optional;

public class ServerThread extends Thread {
    protected Socket serverSocket;
    private String  dbFolder;
    private DataInputStream in = null;
    private DataOutputStream out = null;
    //
    // private DataOutputStream out = null;
    static boolean success = false;

    /**
     * The default buffer size
     */
    public static final int DEFAULT_BUFFER_SIZE = 8192;

    public ServerThread(Socket clientSocket,String dbFolder) {
        this.serverSocket = clientSocket;
        this.dbFolder = dbFolder;
    }

    public void run() {
        try {

            out = new DataOutputStream(serverSocket.getOutputStream());

            String line = "";
            //Welcome message and command menu.
            String welcomeMessage = "Connected to TCP Server  \n" +
                    "Menu : \n" +
                    "1: Store file \n" +
                    "2: Retrieve file";
            out.writeUTF(welcomeMessage);
            out.flush();

            // takes input from the client socket
            in = new DataInputStream(new BufferedInputStream(serverSocket.getInputStream()));

            int i = 1;
            // reads message from client until "Over" is sent
            while (i > 0) {
                String response = "";
                line = in.readUTF();
                System.out.println(line);
                //out.writeUTF("reply: "+ java.time.LocalTime.now());

                switch (line.toLowerCase()) {
                    case "help":
                        System.out.println("in help");
                        response = "Menu : \n" +
                                "1: Store file \n" +
                                "2: Retrieve file";

                        break;
                    case "1":
                        String input = "Input file name";
                        out.writeUTF(input);
                        String fileName = in.readUTF();
                        //tell the client to send the file
                        out.writeUTF("sending...");
                        Thread.sleep(0);

                        //save the file
                        File file = new File(dbFolder + fileName);
                        System.out.println("saving file  " + line);
                        copyInputStreamToFile(in, file);
                        if (success) {
                            System.out.println("File save..");
                            response = "File save successfully";
                            //out.write((output + "\r\n").getBytes());
                        } else {
                            response = "File not save";
                            //out.write((output + "\r\n").getBytes());
                        }
                        out.flush();

                        break;
                    case "2":
                        String outr = "Enter file name";
                        out.writeUTF(outr);
                        Thread.sleep(00);

                        String fName = in.readUTF();
                        System.out.println("fName "+ fName);

                        File fileRetrieved = new File(dbFolder+fName);
                        if (!fileRetrieved.exists()) {
                            System.out.println("File does not exist");
                            out.writeUTF("File does not exit");

                        } else {
                            System.out.println("File present");
                           out.writeUTF("downloading...");
                            sendFile(dbFolder+fName, out);
                        }

                        break;
                    case "quit":
                        i = 0;
                        break;

                    default:
                        response = "Command not understood. Type help";
                        //System.out.println("Command not understood. Type help");
                }
                System.out.println("<<<Writing buffer>>>");
                out.writeUTF(response);
                out.flush();

            }

            System.out.println("Client quiting...");
            // close connection
            serverSocket.close();
            in.close();

        } catch (IOException e) {
            System.out.println(e.getMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void sendFile(String fileName, DataOutputStream os ) {
        //track the time taken and the number of bytes sent to print at the end if all goes well
        long startTime = System.currentTimeMillis();
        int bytesSent = 0;

        //allocate a buffer for sending data - might as well make this 512 bytes, like the data packets in TFTP
        byte[] buffer = new byte[1026];

        //open an input stream to the file
        try (FileInputStream reader = new FileInputStream(fileName)) {
            int num;
            try {
                //keep on writing to the output stream until the end of the file is reached
                while ((num = reader.read(buffer)) != -1) {
                    System.out.println("writing");
                    os.write(buffer, 0, num);
                    bytesSent += num;
                }
            } catch (IOException e) {
                System.out.println("error sending file: " + e.getMessage());
                return;
            }
        } catch (IOException e) {
            System.out.println("error reading from file: " + e.getMessage());
            return;
        }

        //print information about the transfer, and finish
        long time = System.currentTimeMillis() - startTime;
        double seconds = (double) time / 1000.0;
        BigDecimal bigDecimal = new BigDecimal(seconds);
        bigDecimal = bigDecimal.setScale(1, BigDecimal.ROUND_UP);
        System.out.printf("sent %d bytes in %s seconds%n", bytesSent, bigDecimal.toPlainString());
    }

    public static String readToEnd(DataInputStream in) throws IOException {
//        logger.trace("Starting to Read Request");

        StringBuilder sb = new StringBuilder();

        int size = 0;
        int read = 0;

        char ch = (char) in.read();
        while (ch != -1) {
            sb.append(ch);
            read = sb.length();
            if (read == 4) {
                size = Integer.parseInt(sb.toString());
            }
            if (size > 0 && read == (size + 4)) {
                break;
            }
            //read next
            ch = (char) in.read();
        }
        return sb.toString();
    }

    private int readNext(StringBuilder stringBuilder, Buffer buffer) throws IOException {
        // Attempt to read up to the buffers size
        int read = in.read(buffer.array());
        // If EOF is reached (-1 read)
        // we disconnect, because the
        // other end disconnected.
        if (read == -1) {
            //disconnect();
            return -1;
        }
        // Add the read byte[] as
        // a String to the stringBuilder.
        stringBuilder.append(new String(buffer.array()).trim());
        buffer.clear();

        return read;
    }

    private Optional<String> readBlocking() throws IOException {
        final Buffer buffer = new Buffer(512);
        final StringBuilder stringBuilder = new StringBuilder();
        // This call blocks. Therefore
        // if we continue past this point
        // we WILL have some sort of
        // result. This might be -1, which
        // means, EOF (disconnect.)
        if (readNext(stringBuilder, buffer) == -1) {
            return Optional.empty();
        }
        while (in.available() > 0) {
            buffer.reallocate(in.available());
            if (readNext(stringBuilder, buffer) == -1) {
                return Optional.empty();
            }
        }

        buffer.teardown();

        return Optional.of(stringBuilder.toString());
    }

    public String readAll() throws IOException {
        Optional<String> response = readBlocking();
        return response.get();
    }

    private static void copyInputStreamToFile(DataInputStream inputStream, File file) throws IOException {
        try {
            // append = false
            try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
                int read;
                byte[] bytes = new byte[DEFAULT_BUFFER_SIZE];
                //byte[] bytes = new byte[510];
               // while ((read = inputStream.read(bytes)) != -1) {
                while ((read = inputStream.read(bytes)) != -1) {
                    outputStream.write(bytes, 0, read);
                    success = false;
                    System.out.println("file saved!");
                    if(read==-1){
                        System.out.println("breaking "+ read);
                        break;
                    }else{
                        System.out.println(" stream "+ read);

                    }
                }
                System.out.println("file released!");

                outputStream.flush();
            }
        } catch (Exception e) {
            success = false;
            System.out.println("Error occured while writing to file " + e.getMessage());
        }

    }
}