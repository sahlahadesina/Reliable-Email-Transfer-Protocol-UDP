package netwroktrial;
import java.awt.Color;
import java.awt.Font;
import java.io.*;
import java.net.*; // this is for UDP networking
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import javax.swing.*;
import java.awt.*;
import java.io.OutputStream;
import java.io.PrintStream;
import javax.swing.JFrame;

//import javax.mail.*;
//import javax.mail.Authenticator;
//import javax.mail.PasswordAuthentication;
//import javax.mail.internet.*;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

//import com.mysql.cj.xdevapi.Session;
//import com.mysql.cj.xdevapi.Statement;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class server1{
    private static final int TIMEOUT_SECONDS = 5;
    private static JTextArea outputArea;

    private static Map<Integer, byte[]> packetMap = new TreeMap<>();
    private static HashMap<File, String> fileEmailMap = new HashMap<>();

    public static void main(String[] args) {
        //setupTerminalWindow();

        String serverhostname = "localhost";  // IP address of the server

        DatagramSocket serverSocket1 = null; // Datagram declaration for UDP communication

        // Socket Initialization
        try {
            serverSocket1 = new DatagramSocket(34); // Datagram on port 36 is created, we change port number every time we run the client-server process
            serverSocket1.setSoTimeout(TIMEOUT_SECONDS * 1000);
            byte[] receiveData = new byte[1024]; // array of size 1024 B to store the incoming data from the client's side

            // communication confirmation messages
            JOptionPane.showMessageDialog(null, "Mail Server Starting at host: " + serverhostname);
            JOptionPane.showMessageDialog(null, "Waiting to be contacted for transferring Mail.. " + serverhostname);



            // Receiving Packets, the server enters an infinite loop to listen for mail packets from client side
            // must forcely terminate it using control C or kill the terminal
            while (true) { // Keep the server running

                DatagramPacket connection_req = new DatagramPacket(receiveData, receiveData.length);

                try {
                    // Receive SYN
                    serverSocket1.receive(connection_req);
                    String req = new String(connection_req.getData(), 0, connection_req.getLength());
                    String[] reqpack = req.split("_");

                    InetAddress clientAddress = connection_req.getAddress();
                    int clientPort = connection_req.getPort();

                    if (reqpack[0].equals("SYN")) {
                        System.out.println("Received SYN from client at " + clientAddress + ":" + clientPort);

                        // Send ACK
                        String ack = "ACK";
                        byte[] ackBytes = ack.getBytes();
                        DatagramPacket ackPacket = new DatagramPacket(ackBytes, ackBytes.length, clientAddress, clientPort);
                        serverSocket1.send(ackPacket);
                        System.out.println("Sent ACK to client.");

                        // Wait for ACK ACK with timeout
                        try {
                            serverSocket1.receive(connection_req);
                            String ackAck = new String(connection_req.getData(), 0, connection_req.getLength());
                            if (ackAck.equals("ACK ACK")) {
                                System.out.println("Received ACK ACK from client. Handshake complete.");
             
                                serverSocket1.setSoTimeout(0);

                            } // innerif
                        } // try
                        catch (SocketTimeoutException e) {
                            System.out.println("Timeout waiting for ACK ACK from client. Resending ACK...");
                            serverSocket1.send(ackPacket);
                        } // catch
                    } // if
                    
                    if (reqpack[1].equals("sender"))
                        handleEmail(serverSocket1, clientAddress, clientPort);
                    else {
                        System.out.print("Reciver connected!");
                        String recieverName = "";
                        String recieverEmail = "";
						 recieverName =reqpack[2] ;
						 recieverEmail =reqpack[3] ;
						//System.out.println("from the main reHostname: " +recieverName );
						//System.out.println("from the main recEmail: " +recieverEmail );

                        // function to send email to receiver side
                        String fullmail = getFullEmailByHost("recivers_emails.txt",recieverName,recieverEmail);
                        deleteLine("recivers_emails.txt",recieverName,recieverEmail);
                        
                        System.out.print(fullmail);

                        if(fullmail!= null) {
                        String[] splitmail = fullmail.split("_");
                        
                        String to = splitmail[0];
                        String from =splitmail[1];
                        String subject = splitmail[2];
                        String body = splitmail[3];
                        
                        
                        // NEW CODE START
                        
                        // Get emails information as String to, form, subject, body and File file

                        File file = null;
                        file = getFileByEmail(to);
                        
                        // The following socket and InetAddress has to be changed to send to receiver
                        //DatagramSocket clientSocket = new DatagramSocket();
                        //InetAddress recieverAddress = InetAddress.getByName(recieverName);
                        // sending the mail content
                        String mail;
                        //System.out.println("from send mails in main" + file.toString());
                        // sending the file in chunks
                        if (file != null) {
                            String file_path = file.getAbsolutePath();

                            Path filePath = Paths.get(file_path);
                            String fname = file.getName();
                            System.out.println(file_path);
                            
                            System.out.print("im sending data");

                            mail = from + "_" + to + "_" + subject + "_" + body+"_yes";
                            byte[] sendData = mail.getBytes();

                            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
                            serverSocket1.send(sendPacket);

                            if (file_path != null) {

                                byte[][] file_byt = splitFile(filePath, fname);  // Split file into byte arrays and store arrays in file_byt with sequence numbers
                                for (byte[] bytes : file_byt){  // for each array in file_byte
                                    System.out.println(Arrays.toString(bytes));  // Print byte arrays

                                    // Send each packet to the server
                                    sendPacket = new DatagramPacket(bytes, bytes.length, clientAddress, clientPort);
                                    serverSocket1.send(sendPacket); // transmitting the packets
                                }

                                // Send a "FIN" message when packets are done transmitting
                                String msg = "FIN";
                                byte[] fin_msg= msg.getBytes();
                                sendPacket = new DatagramPacket(fin_msg, fin_msg.length, clientAddress, clientPort);
                                System.out.println("here");

                                serverSocket1.send(sendPacket); // transmitting the packets
                            }
                        }
                        else {
                            mail = from + "_" + to + "_" + subject + "_" + body+"_no";
                            byte[] sendData = mail.getBytes();
                            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
                            serverSocket1.send(sendPacket);
                        }
                        }//if the reciver actually has been sent an email
                        else {
                            String mail = "You have no new mail!";

                        	 byte[] sendData = mail.getBytes();
                             DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
                             serverSocket1.send(sendPacket);
                        }
                        
                        // NEW CODE END
                        
                        // this depends on button clicked
                    }

                } // try
                catch (SocketTimeoutException e) {
                    // System.out.println("No SYN received. Server is still waiting...");
                } // catch


            }//while(true)
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (serverSocket1 != null && !serverSocket1.isClosed()) {
                serverSocket1.close();
            }
        }
    }//main

//    private static void setupTerminalWindow() {
//        JFrame frame = new JFrame("Server Terminal");
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.setSize(800, 600);
//
//        outputArea = new JTextArea();
//        outputArea.setBackground(Color.BLACK);
//        outputArea.setForeground(Color.WHITE);
//        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
//        outputArea.setEditable(false);
//
//        JScrollPane scrollPane = new JScrollPane(outputArea);
//        frame.add(scrollPane);
//
//        frame.setVisible(true);
//
//        // Redirect System.out and System.err
//        PrintStream printStream = new PrintStream(new OutputStream() {
//            @Override
//            public void write(int b) {
//                outputArea.append(String.valueOf((char) b));
//                outputArea.setCaretPosition(outputArea.getDocument().getLength());
//            }
//        });
//        System.setOut(printStream);
//        System.setErr(printStream);
//    }

    
   
     
    private static void handleEmail(DatagramSocket serverSocket1, InetAddress clientAddress, int clientPort) throws IOException {
        byte[] receiveData = new byte[1024]; // array of size 1024 B to store the incoming data from the client's side

        //sending the mail stuff
        DatagramPacket mailPacket = new DatagramPacket(receiveData, receiveData.length); // creating a Datagram for the mail packets received (incoming data)
        serverSocket1.receive(mailPacket); // the receive method is called on the socket to wait for a packet from the client
        //*server replies with acknoledgment and it has sequence number*//

        // Message Processing
        String message = new String(mailPacket.getData(), 0, mailPacket.getLength()); // convert received packet into string : it's received as Binary
        String[] mail = message.split("_"); // split the string at _, resulting in an array
        //*mapping address to hostname*//
        int last_seq = 0;
        File file = null;
        long fileSizeInBytes = 0;
        if (mail[4].equals("yes")){ // If file is attached

            while(true){ //keep looping until all file packets are received
                // Receive file packets
                mailPacket = new DatagramPacket(receiveData , receiveData.length);
                serverSocket1.receive(mailPacket);

                // Create byte array with the received packet's size
                byte[] file_packet = new byte[mailPacket.getLength()];
                // Copy received packet in the file_packet byte array
                System.arraycopy(mailPacket.getData(), 0, file_packet, 0, mailPacket.getLength());

                // Check if the server sends a "FIN" message to stop receiving
                String fin_msg = new String(mailPacket.getData(), 0, 3); // Convert packet data to string
                if ("FIN".equals(fin_msg)) {
                    break; // Exit the loop
                } else{
                    // if the packet received is not "FIN", then store in map
                    last_seq = mapFile(file_packet); // store last sequence number received
                }
            }

            // Check for missing packets
            Integer[] arr_miss = missingPackets(packetMap);
            if (arr_miss.length == 0) {
                System.out.println("No missing packets");
                // Merge byte arrays to save file
                byte[][] merged_file = mergeFile(packetMap);
                // Save file in desktop
//                file = new File(
                String path=openFile(merged_file);
                file = new File(path);
                fileSizeInBytes = file.length();
                // Add file size to file's path for printing
//                String file_path = file.concat("\nFile size: " + Files.size(Paths.get(file)));
            } else {
                // Print sequence numbers for missing packets
                System.out.println("Missing packets: " + Arrays.toString(missingPackets(packetMap)));
            }
            // Clear map for new file
            packetMap.clear();
        }

        System.out.println("last seq number: " + last_seq); // print last sequence number for ack

        // Timestamp Generation
        Timestamp timestamp1 = Timestamp.valueOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        String timestampStr = timestamp1.toString();

        String to = mail[0];
        String from = mail[1];
        String subject = mail[2];
        String body = mail[3];
//        byte[] fileData = null;
//        File file = new File(mail[4]);
//        fileData = Files.readAllBytes(file.toPath());
        //the last element in the message is the client name
        JOptionPane.showMessageDialog(null, "Mail Received from " + from);
        JOptionPane.showMessageDialog(null, "Email Request Received at Time: "+timestamp1);
        JOptionPane.showMessageDialog(null, "\nFrom: "+from+"\nTo: "+to+"\nSubject: "+subject+"\nBody: "+body+"\nFile size in Bytes: "+fileSizeInBytes);

//        System.out.println("Mail Received from " + from);
//        System.out.println( "Email Request Received at Time: "+timestamp1);
//        System.out.println("\nFrom: "+from+"\nTo: "+to+"\nSubject: "+subject+"\nBody: "+body+"\nFile size in Bytes: "+fileSizeInBytes);
//

        // created a file "Email.txt" that has all the valid emails, for verification
        // Checking Email Validity
        if(file!=null)
        addFileEmailPair(file, to);

        if (validEmails(mail)) { // client's email is the 2md element of the mail array

            // Handling Valid Email
            JOptionPane.showMessageDialog(null, "The Header fields are verified.\n\nSending '250 OK'");

            // Sending response to client
            clientAddress = mailPacket.getAddress(); // what is InetAddress : to check the IP address the packet is coming from
            clientPort = mailPacket.getPort(); // returns the port number of which the data packet is sent to, client's
            byte[] sendData = timestampStr.getBytes();

            // server creates a response packet with the timestamp and sends it back to the client
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
            serverSocket1.send(sendPacket);

            // sending actual email
//            sendEmail(to, from,  subject,  body,  null);
            //System.out.println("ddddsssssssss");

            storeEmail(to, from, subject, body, file, timestamp1);
            
            storeEmailInfo(to, from, subject, body, timestamp1, "mapping.txt", "recivers_emails.txt");
            
            //add file and reciver mail pair 
            //used to send the file as chunks to the reciever

            
            //wait for anything regarding termination. //only do something if it is a termination 
            DatagramPacket termination = new DatagramPacket(receiveData , receiveData.length);
            serverSocket1.receive(termination);
            String term_msg = new String(termination.getData(), 0, termination.getLength()); // convert received packet into string : it's received as Binary
            if(term_msg.equals("TERMINATE")) {
                serverSocket1.setSoTimeout(TIMEOUT_SECONDS * 1000);

            	System.out.println("Received FIN from client at " + clientAddress + ":" + clientPort);

            	String ACK = "ACK";
                clientAddress = termination.getAddress();
                clientPort = termination.getPort();

                byte[] ACKB = ACK.getBytes();
                DatagramPacket sendACKPacket = new DatagramPacket(ACKB, ACKB.length, clientAddress, clientPort);
                serverSocket1.send(sendACKPacket);

                try {
                	serverSocket1.receive(termination);
                    String ackAck = new String(termination.getData(), 0, termination.getLength());
                    if (ackAck.equals("ACK ACK")) {
                        System.out.println("Received ACK ACK from client. Termination complete.");
                        serverSocket1.setSoTimeout(0);

                    } // innerif
                } // try
                catch (SocketTimeoutException e) {
                    System.out.println("Timeout waiting for ACK ACK from client. Resending ACK...");
                    serverSocket1.send(sendACKPacket);
                } // catch
                
                
                
                
            }//if (for termination sequence) 

            
            
            
        }//if valid emails. 

        // Handling Invalid Email
        else {
            //JOptionPane.showMessageDialog(null, "The Header fields are not valid.\n\nSending '501 ERROR'");
            System.out.println("The Header fields are not valid.\n\nSending '501 ERROR'");
            // no need right??
            String errormsg = "501 ERROR, Please try again.";
            clientAddress = mailPacket.getAddress();
            clientPort = mailPacket.getPort();

            byte[] sendData = errormsg.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
            serverSocket1.send(sendPacket);
//            //terminationn
//            DatagramPacket termination = new DatagramPacket(receiveData , receiveData.length);
//            serverSocket1.receive(termination);
//            String term_msg = new String(termination.getData(), 0, termination.getLength()); // convert received packet into string : it's received as Binary
//            
//            if(term_msg.equals("TERMINATE")) {
//                serverSocket1.setSoTimeout(TIMEOUT_SECONDS * 1000);
//
//            	System.out.println("Received FIN from client at " + clientAddress + ":" + clientPort);
//
//            	String ACK = "ACK";
//                clientAddress = termination.getAddress();
//                clientPort = termination.getPort();
//
//                byte[] ACKB = ACK.getBytes();
//                DatagramPacket sendACKPacket = new DatagramPacket(ACKB, ACKB.length, clientAddress, clientPort);
//                serverSocket1.send(sendACKPacket);
//
//                try {
//                
//					serverSocket1.receive(termination);
//                    String ackAck = new String(termination.getData(), 0, termination.getLength());
//                    if (ackAck.equals("ACK ACK")) {
//                        System.out.println("Received ACK ACK from client. Termination complete.");
//                        serverSocket1.setSoTimeout(0);
//
//                    } // innerif
//                } // try
//                catch (SocketTimeoutException e) {
//                    System.out.println("Timeout waiting for ACK ACK from client. Resending ACK...");
//                    serverSocket1.send(sendACKPacket);
//                } // catch
//                
//                
//                
//                
//            }//if (for termination sequence) 

            

        }//else

    }//handleEmail

    public static void deleteLine(String fileName, String localHostName, String emailName) {
        try {
            Path filePath = Paths.get(fileName);
            List<String> lines = Files.readAllLines(filePath); // Read all lines into a list
            List<String> updatedLines = new ArrayList<>();

            for (String line : lines) {
                // Split the line into its components
                String[] parts = line.split(",", 3); // Expect 3 parts: localHostName, to, fullEmail
                if (parts.length < 3) {
                    // Skip malformed lines
                    updatedLines.add(line);
                    continue;
                }

                // Check if the line matches the criteria
                if (!parts[0].equals(localHostName) || !parts[1].equals(emailName)) {
                    updatedLines.add(line); // Add non-matching lines to the updated list
                }
            }

            // Write the updated lines back to the file
            Files.write(filePath, updatedLines, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("File updated successfully.");

        } catch (IOException e) {
            System.err.println("Error updating file: " + e.getMessage());
        }
    }
    
    public static String getFullEmailByHost(String emailFilePath, String hostName, String receiveremail) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(emailFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Split the line into parts: receiver_host, receiver email, fullEmail
                String[] parts = line.split(",", 3); // Limit to 3 parts
                if (parts.length == 3) {
                    String receiverHost = parts[0].trim();
                    String receiverEmail = parts[1].trim();
                    String fullEmail = parts[2].trim();

                    //System.out.println(receiverHost + "." +receiverEmail );
                    // Check if the hostName matches
                    if (receiverHost.equalsIgnoreCase(hostName)&&receiverEmail.equalsIgnoreCase(receiveremail) ) {
                        return fullEmail;
                    }
                }
            }
        }
        return null; // Return null if the hostname is not found
    }//to get the email of the reciver 
    
    
    private static void storeEmailInfo(String to, String from, String subject, String body, Timestamp timestamp1, String mappingFilePath, String outputFilePath) throws IOException {
        Map<String, String> emailToHostMap = loadMapping(mappingFilePath);
        String receiverHost = emailToHostMap.getOrDefault(to, "unknown");

        // Full email content
        String fullEmail = from + "_" + to + "_" + subject + "_" + body + "_" + timestamp1 ;

        // Line to write to the output file
        String outputLine = String.format("%s,%s,%s", receiverHost, to, fullEmail);

        // Write the line to the output file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath, true))) {
            writer.write(outputLine);
            writer.newLine();
        }
    }
    
    //using the mapping file, this creates a Hash map of the reciver's email and its host name
    private static Map<String, String> loadMapping(String mappingFilePath) throws IOException {
        Map<String, String> emailToHostMap = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(mappingFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    emailToHostMap.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
        return emailToHostMap;
    }//to use to map the reciver's email to its host name. 
    

    //fileEmailMap was initialised in the start
    //creates a hash map that contains both the file and the email it was destined for
    private static void addFileEmailPair(File file, String email) {
        if (file == null) {
           //do nothing
        }
        fileEmailMap.put(file, email);
    }
    
    //this checks the hashmap id there is a file for a specific reciever email
    public static File getFileByEmail(String email) {
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty.");
        }

        for (File file : fileEmailMap.keySet()) {
            if (fileEmailMap.get(file).equals(email)) {
                return file;
            }
        }//for file
        return null; // Return null if no file is found for the given email
    }//returns file. 
    
    
    
    // Email Validation Method, whether the email address exists in the text file
    public static boolean validEmails(String[] email) {
        try {
            FileReader f = new FileReader("Email.txt"); //read file
            try (Scanner s = new Scanner(f)) {
                while (s.hasNextLine()) { //loop through file
                    String line = s.nextLine(); //store line
                    if (line.equals(email[1])) { //check if email exists
                        return true; //return true if email exists
                    }
                }
            }
            return false; //return false if email doesn't exist
        } catch (FileNotFoundException e) { //catch error if file not found
            return false; //return false if file not found
        }
    }


    // storing email received from client in the database
    public static void storeEmail(String to, String from, String subject, String body, File file, Timestamp timestamp1) throws FileNotFoundException {
        Connection Con = null;
        PreparedStatement pstmt = null;
        PreparedStatement pstmtSender = null;
        PreparedStatement pstmtReceiver = null;

        try {
            Con = DriverManager.getConnection("jdbc:mysql://localhost:3306/email" , "root", "");


            String sql= "INSERT INTO emails_new (send_from, send_to, subject, body, timestamp, file, file_name) " +
                    "VALUES ( ?, ?, ?, ?, ?, ?, ?)";

            // sender database
            String fileName = subject+"_"+timestamp1;

            pstmt = Con.prepareStatement(sql);
            pstmt.setString(1, from);
            pstmt.setString(2, to);
            pstmt.setString(3, subject);
            pstmt.setString(4, body);
            pstmt.setTimestamp(5, timestamp1);

            handleFileBinding(pstmt, file);


            pstmt.executeUpdate();

            // the following will check whether sender and receiver have directories - databases for them or not
            // if yes, append the new email to both
            // if no, create directory for the one with no directory

//            // store in the database
            String senderDb = from.replaceAll("[^a-zA-Z0-9]", "_"); // Sanitize email for DB name
            String receiverDb = to.replaceAll("[^a-zA-Z0-9]", "_");
//
//        	// Ensure databases for sender and receiver exist
            ensureUserDatabaseExists(from, true, senderDb);  // Create/check sender database
            ensureUserDatabaseExists(to, false, receiverDb);    // Create/check receiver database

//

            // Prepare SQL query
            String sql_sender = "INSERT INTO " + senderDb  + " ( email_to, email_from,subject, body, timestamp, attachment, attachment_name) "+
                    "VALUES ( ?, ?, ?, ?, ?, ?, ?)";

            pstmtSender = Con.prepareStatement(sql_sender);
            pstmtSender.setString(1, to);
            pstmtSender.setString(2, from);
            pstmtSender.setString(3, subject);
            pstmtSender.setString(4, body);
            handleFileBinding(pstmtSender, file);
            pstmtSender.executeUpdate();


            String sql_receiver = "INSERT INTO  " + receiverDb + " (email_from, email_to, subject, body, timestamp, attachment, attachment_name) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
            pstmtReceiver = Con.prepareStatement(sql_receiver);
            pstmtReceiver.setString(1, from);
            pstmtReceiver.setString(2, to);

            pstmtReceiver.setString(3, subject);
            pstmtReceiver.setString(4, body);
            pstmtReceiver.setTimestamp(5, timestamp1);
            handleFileBinding(pstmtReceiver, file);

            pstmtReceiver.executeUpdate();

        } catch (SQLException ex) {
            ex.printStackTrace();
            System.err.println("Error saving email: " + ex.getMessage());
        } finally {
            try {
                if (pstmtSender != null) pstmtSender.close();
                if (pstmtReceiver != null) pstmtReceiver.close();
                if (Con != null) Con.close();
            } catch (SQLException ex) {
                ex.printStackTrace();

            }
        }
    }

    public static void ensureUserDatabaseExists(String email, boolean isSender, String tableName) {
        Connection con = null;
        java.sql.Statement stmt = null;

        try {
//            String dbName = email.replaceAll("[^a-zA-Z0-9]", "_"); // Sanitize email for DB name
            con = DriverManager.getConnection("jdbc:mysql://localhost:3306/email", "root", "");
            stmt = con.createStatement();

            // Create database if it doesn't exist
//            stmt.executeUpdate("CREATE TAVBL IF NOT EXISTS " + dbName + "");
            // Use the database and create the emails table
//            con.setCatalog(dbName); // Switch to the user-specific database

            String createTableSql = isSender
                    ? "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "email_to VARCHAR(255), " +
                    "email_from VARCHAR(255), " +
                    "subject VARCHAR(255), " +
                    "body TEXT, " +
                    "timestamp DATETIME, " +
                    "attachment BLOB, " +
                    "attachment_name VARCHAR(255))"
                    : "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "email_from VARCHAR(255), " +
                    "email_to VARCHAR(255), " +
                    "subject VARCHAR(255), " +
                    "body TEXT, " +
                    "timestamp DATETIME, " +
                    "attachment BLOB, " +
                    "attachment_name VARCHAR(255))";

            stmt.executeUpdate(createTableSql);
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (con != null) con.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void handleFileBinding(PreparedStatement pstmt, File file) throws SQLException, FileNotFoundException {
        if (file != null) {
            FileInputStream fis = new FileInputStream(file); // A FileInputStream is created to read the contents of the file. This stream will be used to send the file data to the database.
            pstmt.setBinaryStream(6, fis, (int) file.length()); // fis is the input stream representing the file
            pstmt.setString(7, file.getName());
        } else {
            pstmt.setNull(6, Types.BLOB);
            pstmt.setNull(7, Types.VARCHAR);
        }
    }




    // Email Validation Method, whether the email address exists in the text file
    public static boolean validEmails(String email) {
        try {
            FileReader f = new FileReader("Email.txt"); //read file
            Scanner s = new Scanner(f); //scanner to read file
            while (s.hasNextLine()) { //loop through file
                String line = s.nextLine(); //store line
                if (line.equals(email)) { //check if email exists
                    return true; //return true if email exists
                }
            }
            return false; //return false if email doesn't exist
        } catch (FileNotFoundException e) { //catch error if file not found
            System.out.println("Mail Not Found");
            return false; //return false if file not found
        }
    }

    // Saving Emails Method
    public static void saveEmails(String[] info) {
        File file = new File("SavedEmails.txt"); //create file
        try{
            FileWriter fw = new FileWriter(file, true); //write in file, it appends
            //write email in file
            fw.write("To: " + info[0] + "\n");
            fw.write("From: " + info[1] + "\n");
            fw.write("Subject: " + info[2] + "\n");
            fw.write("File: " + info[4] + "\n");
            fw.write("Body: " + info[3] + "\n\n");
            // could add timestamp
            fw.close(); //close file writer
        } catch(IOException e){ //catch error
            System.out.println("Error writing to file");
        }
    }

    // Method to split the file into byte arrays
    public static byte[][] splitFile(Path filePath, String exten) throws IOException {
        int packet_size = 1024;  // Max packet size
        int packet_seq_size = packet_size - 2;  // Max packet size with sequence number

        // Get file path and convert file to byte array

        byte[] file_bytes = Files.readAllBytes(filePath);
        // Get file extension and save as byte array
//    String exten = file.substring(file.lastIndexOf("/") + 1);
        byte[] exten_b = exten.getBytes(StandardCharsets.UTF_8);

        // Calculate total number of packets
        int totalPackets = (int) (Math.ceil((double) file_bytes.length / packet_seq_size));

        // Create array to save file and extension byte arrays
        byte[][] split_arr = new byte[totalPackets + 1][];

        // Split file
        for (int i = 0; i < totalPackets; i++) {
            // Save index of the beginning and end of the file chunk
            int start = i * packet_seq_size;
            int end = Math.min(start + packet_seq_size, file_bytes.length);

            // Create substitute array
            byte[] sub_arr = new byte[2 + (end - start)];

            // Add sequence number to substitute array
            buffer(sub_arr, i);

            // Save file chunks into a substitute array with sequence numbers
            if (end - start >= 0) System.arraycopy(file_bytes, start, sub_arr, 2, end - start);

            // Add file bytes with sequence numbers to the final array
            split_arr[i] = sub_arr;
        }

        // Create array for file extension
        byte[] exten_pack = new byte[exten_b.length + 2];
        // Add sequence number to extension array
        buffer(exten_pack, totalPackets);
        // Add the extension to extension array
        System.arraycopy(exten_b, 0, exten_pack, 2, exten_b.length);
        // Add extension bytes with sequence numbers to the final array
        split_arr[split_arr.length - 1] = exten_pack;

        // Return final array for file and extension arrays with sequence numbers
        return split_arr;
    }

    // Method to add a sequence number to a byte array
    public static void buffer(byte[] sub_arr, int seq) {

        // Allocate space for 2 bytes
        ByteBuffer buffer = ByteBuffer.allocate(2);
        // Put sequence number as a short in buffer
        buffer.putShort((short) seq);

        // Add sequence number into the array
        buffer.flip();
        for (int buff_in = 0; buff_in < 2; buff_in++) {
            sub_arr[buff_in] = buffer.get();
        }
    }


    // Method to map byte arrays with sequence numbers
    public static int mapFile(byte[] arr) {
        // Get sequence numbers from the first two bytes making sure it's unsigned
        int seq_num = ((arr[0] & 255) * 256) + (arr[1] & 255);
        System.out.println("Cumulative Ack of all packets, Seq No= " + seq_num); // Print sequence number

        // Get file bytes without sequence numbers
        byte[] data = new byte[arr.length - 2];
        System.arraycopy(arr, 2, data, 0, arr.length - 2);

        // Save file bytes mapped to its sequence numbers
        packetMap.put(seq_num, data);

        return seq_num;
    }

    // Method to find missing packets
    public static Integer[] missingPackets(Map<Integer, byte[]> map) {
        // Get the last sequence number
        int last_key = map.size() - 1;

        // Get a set of sequence numbers in the map
        Set<Integer> existingKeys = map.keySet();

        // Create a list to store missing sequence numbers
        List<Integer> missingKeys = new ArrayList<>();

        // Check for missing keys
        for (int i = 0; i <= last_key; i++) {
            // Add sequence number to the list if missing
            if (!existingKeys.contains(i)) {
                missingKeys.add(i);
            }
        }

        // Return an array of missing sequence numbers
        return missingKeys.toArray(new Integer[0]);
    }

    // Method to merge file bytes
    public static byte[][] mergeFile(Map<Integer, byte[]> map) {
        int bytes_num = 0;  // To store the number of bytes
        byte[] extnArray = null; // To store extension bytes

        // Calculate the length of merged file without the extension
        for (byte[] byte_arr : map.values()) {
            bytes_num += byte_arr.length;
            extnArray = byte_arr;  // Store the extension bytes
        }

        // Make sure extension array is not null
        assert extnArray != null;
        // Create a byte array to store the file bytes without the extension
        byte[] mergedArray = new byte[bytes_num - extnArray.length];

        // Add file bytes to the merged array
        int curr = 0; // Store current index
        for (byte[] byteArray : map.values()) {
            if (byteArray != extnArray) {  // If it's file bytes
                // Copy file bytes into merged array
                System.arraycopy(byteArray, 0, mergedArray, curr, byteArray.length);
                curr += byteArray.length;  // Update current index
            }
        }

        // Return array of arrays where the first index is the file bytes and the second is the extension bytes
        return new byte[][] { mergedArray, extnArray };
    }

    // Method to save file to the Desktop
    public static String openFile(byte[][] file_b) throws IOException {
        // Get user's desktop path
        String userHome = System.getProperty("user.home");
        System.out.println();
        String desktopPath = userHome + File.separator + "Documents";
        // Change file extension from bytes to string
        String ext = new String(file_b[1], StandardCharsets.UTF_8);
        // Save file bytes
        byte[] file = file_b[0];

        // Get current time
        LocalDateTime time = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

        // Format the time
        String formattedTime = time.format(formatter);

        // Generate file path to desktop and add the time to the file name
        Path filePath = Paths.get(desktopPath, formattedTime + "_" + ext);

        // Save file to desktop
        Files.write(filePath, file, StandardOpenOption.CREATE_NEW);

        return ""+filePath;
    }

}//end of server


    

