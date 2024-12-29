package netwroktrial;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class receiver extends JFrame {
    private static Map<Integer, byte[]> packetMap = new TreeMap<>();
    private JTextArea emailDisplayArea;
    private JButton refreshButton;
    private DatagramSocket clientSocket;
    private static final int TIMEOUT_SECONDS = 5;
    private String serverName;
    private String recieverEmail; 
    public receiver() {

        String ClientHostName = "localhost";
        JOptionPane.showMessageDialog(null, "Mail Client starting on host: " + ClientHostName); // client begun execution
        recieverEmail = serverName = JOptionPane.showInputDialog(null,"Please Enter your Email: ");
        //recieverEmail = "shamsa@gmail.com";

        // window for options of quit, send email, sync
        while (true) {
            //serverName = JOptionPane.showInputDialog(null,"Enter server host name: ");
            serverName = "localhost";
            // Check if the user canceled the dialog
            if (serverName == null) {
                System.exit(0); // Exit if server name is not provided
            }

            // Check if the server name is empty
            if (serverName.isEmpty()) {
                // Prompt the user again
                JOptionPane.showMessageDialog(null, "Server name cannot be empty. Please try again.", "Input Error", JOptionPane.WARNING_MESSAGE);
            }
            else {
                String mode = "reciever";
                int portNumber = 34;
                //DatagramSocket clientSocket = null;

                try {
                    clientSocket = new DatagramSocket();
                    InetAddress serverAddress = InetAddress.getByName(serverName);
                    clientSocket.setSoTimeout(TIMEOUT_SECONDS * 1000);

                    // Send SYN with retransmission
                    String syn = "SYN_" +mode + "_"+ ClientHostName+ "_" +recieverEmail ;
                    byte[] SYNpacket = syn.getBytes();
                    DatagramPacket synPacket = new DatagramPacket(SYNpacket, SYNpacket.length, serverAddress, portNumber);

                    boolean handshakeComplete = false;
                    while (!handshakeComplete) {
                        try {
                            clientSocket.send(synPacket);
                            System.out.println("SYN sent to server. Waiting for ACK...");

                            // Receive ACK
                            byte[] receiveData = new byte[1024];
                            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                            clientSocket.receive(receivePacket);
                            String ack = new String(receivePacket.getData(), 0, receivePacket.getLength());

                            if (ack.equals("ACK")) {
                                System.out.println("ACK received from server.");

                                // Send ACK ACK
                                String ackAck = "ACK ACK";
                                byte[] ackAckBytes = ackAck.getBytes();
                                DatagramPacket ackAckPacket = new DatagramPacket(ackAckBytes, ackAckBytes.length, serverAddress, portNumber);
                                clientSocket.send(ackAckPacket);
                                System.out.println("ACK ACK sent to server. Handshake complete.");
                                handshakeComplete = true;

                                clientSocket.setSoTimeout(0);
                            }
                        } catch (SocketTimeoutException e) {
                            System.out.println("Timeout waiting for ACK. Resending SYN...");
                        }//catch
                    }}//while handshake

                catch (Exception e) {
                    e.printStackTrace();
                } 

                break;
            }//else
        }//while(true)


        setTitle("Email Receiver");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        emailDisplayArea = new JTextArea();
        emailDisplayArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(emailDisplayArea);

        refreshButton = new JButton("Sync the Inbox");
        refreshButton.addActionListener(new RefreshButtonListener());

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(refreshButton, BorderLayout.SOUTH);

        add(panel);
        setVisible(true);

        // automatically refresh on start
        //refreshEmails(clientSocket);

    }//recievr


    private void refreshEmails(DatagramSocket clientSocket) {
        try {
            // send request to server
//            String request = "GET_EMAILS";
//            byte[] sendData = request.getBytes();
//            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
//            clientSocket.send(sendPacket);

         //System.out.println("im here!");

            // receive response
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            clientSocket.receive(receivePacket);

            String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
            System.out.println("im here!");

            // Assuming the format of response message is to_from_subject_body_file, where file is either "yes" or "no"
            String[] mail = response.split("_"); // split the string at _, resulting in an array
            int last_seq = 0;
            
            if(response.equals("You have no new mail!")) {
                JOptionPane.showMessageDialog(null, "You have no new mails!");

            }
            else if (mail[4].equals("yes")){ // If file is attached
                while(true){ //keep looping until all file packets are received
                    // Receive file packets
                    receivePacket = new DatagramPacket(receiveData , receiveData.length);
                    clientSocket.receive(receivePacket);

                    // Create byte array with the received packet's size
                    byte[] file_packet = new byte[receivePacket.getLength()];
                    // Copy received packet in the file_packet byte array
                    System.arraycopy(receivePacket.getData(), 0, file_packet, 0, receivePacket.getLength());

                    // Check if the server sends a "FIN" message to stop receiving
                    String fin_msg = new String(receivePacket.getData(), 0, 3); // Convert packet data to string
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
                    String path = openFile(merged_file);
                    // Save file in desktop
                    mail[4] = openFile(merged_file);
                    // Add file size to file's path for printing
                    mail[4] = mail[4].concat("\nFile size: " + Files.size(Paths.get(mail[4])));
                } else {
                    // Print sequence numbers for missing packets
                    System.out.println("Missing packets: " + Arrays.toString(missingPackets(packetMap)));
                }
                // Clear map for new file
                packetMap.clear();
            }

            System.out.println("last seq number: " + last_seq); // print last sequence number for ack

            // THE EMAIL CONTENTS ARE NOT SAVED PROPERLY, BUT THE FILE IS SUPPOSED TO BE SAVE IN THE USER'S DOCUMENTS

//            if (response.isEmpty()) {
//                emailDisplayArea.setText("No new emails.");
//                receivedEmails.clear();
//            } else {
//                String[] emails = response.split("\n\n");
//                for (String email : emails) {
//                    receivedEmails.add(email.trim());
//                }
//                //displayEmails();
//            }//else 
//            
            String to = mail[0];
            String from = mail[1];
            String subject = mail[2];
            String body = mail[3];
            String filepath = mail[4];
//            byte[] fileData = null;
//            File file = new File(mail[4]);
//            fileData = Files.readAllBytes(file.toPath());
            //the last element in the message is the client name
            JOptionPane.showMessageDialog(null, "ACK: Mail Received from server, Mail was sent from " + from);
            //JOptionPane.showMessageDialog(null, "Email Request Received at Time: "+timestamp1);
            JOptionPane.showMessageDialog(null, "\nFrom: "+from+"\nTo: "+to+"\nSubject: "+subject+"\nBody: "+body);

            String emailContent = String.format(
                    "------------------------------------\n" +
                    "From: %s\nTo: %s\nSubject: %s\nBody:\n%s\n" +
                    "------------------------------------\n",
                    from, to, subject, body
            );
            emailDisplayArea.append(emailContent);

            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error receiving emails: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//refresh emails

    // Method to map byte arrays with sequence numbers
    public static int mapFile(byte[] arr) {
        // Get sequence numbers from the first two bytes making sure it's unsigned
        int seq_num = ((arr[0] & 255) * 256) + (arr[1] & 255);
        System.out.println("Sequence number of packet Received: " + seq_num); // Print sequence number

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


//    private void displayEmails() {
//        emailDisplayArea.setText("");
//        if (receivedEmails.isEmpty()) {
//            emailDisplayArea.setText("No new emails.");
//        } else {
//            for (int i = 0; i < receivedEmails.size(); i++) {
//                emailDisplayArea.append((i + 1) + ". " + receivedEmails.get(i).split("\n")[0] + "\n");
//            }
//
//            emailDisplayArea.append("\nSelect an email to view its details.");
//            chooseEmailToView();
//        }
//    }

//    private void chooseEmailToView() {
//        if (!receivedEmails.isEmpty()) {
//            String input = JOptionPane.showInputDialog(this, "Enter the email number to view (1-" + receivedEmails.size() + "):");
//            try {
//                int choice = Integer.parseInt(input);
//                if (choice >= 1 && choice <= receivedEmails.size()) {
//                    displayEmailDetails(receivedEmails.get(choice - 1));
//                } else {
//                    JOptionPane.showMessageDialog(this, "Invalid selection.", "Error", JOptionPane.ERROR_MESSAGE);
//                }
//            } catch (NumberFormatException e) {
//                JOptionPane.showMessageDialog(this, "Invalid input.", "Error", JOptionPane.ERROR_MESSAGE);
//            }
//        }
//    }

//    private void displayEmailDetails(String email) {
//        emailDisplayArea.setText(email);
//
//        int choice = JOptionPane.showConfirmDialog(this, "Do you want to go back to the email list?", "View Emails", JOptionPane.YES_NO_OPTION);
//        if (choice == JOptionPane.YES_OPTION) {
//            displayEmails();
//        }
//    }

    private class RefreshButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            refreshEmails(clientSocket);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(receiver::new);
    }
}//class end

