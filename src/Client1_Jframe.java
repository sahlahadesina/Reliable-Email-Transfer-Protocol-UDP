package netwroktrial;

//package networksproject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Scanner;

import javax.swing.JOptionPane;

public class Client1_Jframe extends JFrame {
    private JTextField fromField;       // New field for sender's email
    private JTextField toField;         // Recipient's email field
    private JTextField subjectField;    // Subject field
    private JTextArea bodyArea;         // Email body area
    private JLabel attachmentLabel;     // Label to show attached file
    private File attachment;             // File for attachment
    private String serverName; 
    private static final int TIMEOUT_SECONDS = 5;
    int portNumber = 34;

    public Client1_Jframe() {
    	getContentPane().setBackground(new Color(147, 147, 147));
    	
    	String ClientHostName = "Client 1";
    	 String mode = "sender";
    	JOptionPane.showMessageDialog(null, "Mail Client starting on host: " + ClientHostName); // client begun execution
        DatagramSocket clientSocket = null;
    	// window for options of quit, send email, sync
        
        
        
    	  while (true) {
  	    	serverName = JOptionPane.showInputDialog(null,"Enter server host name: ");
//  	    	serverName = "192.168.14.33";
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
            	//String ClientHostName = "ClientHost";
                  //String mode = "sender";
                 // int portNumber = 36;
                  //DatagramSocket clientSocket = null;

                  try {
                      clientSocket = new DatagramSocket();
                      InetAddress serverAddress = InetAddress.getByName(serverName);
                      clientSocket.setSoTimeout(TIMEOUT_SECONDS * 1000);

                      // Send SYN with retransmission
                      String syn = "SYN_" + mode;
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
                      } finally {
                          if (clientSocket != null && !clientSocket.isClosed()) {
                              clientSocket.close();
                          }
                      }
            	  //valid input exit loop
                  break;
              }//else
          }//true

    	  EmailFrame();

    }//private
    
    private void EmailFrame() {
  	  
        setTitle("Email Form");
        setSize(500, 450); // Increased height to accommodate the new field
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setLayout(new BorderLayout(10, 10));

        // Create panel for fields
        JPanel panel = new JPanel(new GridLayout(5, 2, 5, 5)); // Adjusted for 5 rows
        panel.setBackground(new Color(53, 53, 53));

        // From field
        JLabel label = new JLabel("To:");
        label.setForeground(new Color(255, 255, 255));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setBackground(new Color(236, 242, 230));
        label.setFont(new Font("Tahoma", Font.BOLD | Font.ITALIC, 20));
        panel.add(label);
        fromField = new JTextField();
        panel.add(fromField);

        // To field
        JLabel label_1 = new JLabel("From:");
        label_1.setForeground(new Color(255, 255, 255));
        label_1.setHorizontalAlignment(SwingConstants.CENTER);
        label_1.setFont(new Font("Tahoma", Font.BOLD | Font.ITALIC, 19));
        label_1.setBackground(new Color(236, 242, 230));
        panel.add(label_1);
        toField = new JTextField();
        panel.add(toField);

        // Subject field
        JLabel label_2 = new JLabel("Subject:");
        label_2.setForeground(new Color(255, 255, 255));
        label_2.setBackground(new Color(236, 242, 230));
        label_2.setHorizontalAlignment(SwingConstants.CENTER);
        label_2.setFont(new Font("Tahoma", Font.BOLD | Font.ITALIC, 19));
        panel.add(label_2);
        subjectField = new JTextField();
        panel.add(subjectField);

        // Email body field
        JLabel label_3 = new JLabel("Body:");
        label_3.setForeground(new Color(255, 255, 255));
        label_3.setBackground(new Color(236, 242, 230));
        label_3.setHorizontalAlignment(SwingConstants.CENTER);
        label_3.setFont(new Font("Tahoma", Font.BOLD | Font.ITALIC, 19));
        panel.add(label_3);
        bodyArea = new JTextArea(5, 20);
        JScrollPane scrollPane = new JScrollPane(bodyArea);
        panel.add(scrollPane);

        // Attach button
        JButton attachButton = new JButton("Attach File");
        attachButton.setBackground(new Color(28, 28, 28));
//        attachButton.sendBounds(263, 291, 336, 70);
        attachButton.setFont(new Font("Tahoma", Font.BOLD | Font.ITALIC, 19));
        attachButton.addActionListener(new AttachButtonListener());
        panel.add(attachButton);

        // Attachment label
        attachmentLabel = new JLabel("No file attached");
        attachmentLabel.setFont(new Font("Tahoma", Font.PLAIN, 16));
        attachmentLabel.setBackground(new Color(255, 255, 255));
        attachmentLabel.setForeground(new Color(255, 255, 255));
        panel.add(attachmentLabel);

        getContentPane().add(panel, BorderLayout.CENTER);

        // Send button
        JButton sendButton = new JButton("Send Email");
        sendButton.setForeground(new Color(255, 255, 255));
        sendButton.setBackground(new Color(28, 28, 28));
        sendButton.addActionListener(new SendButtonListener());
        getContentPane().add(sendButton, BorderLayout.SOUTH);
        
        setVisible(true);
    }
    
    private class AttachButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(Client1_Jframe.this);
            if (result == JFileChooser.APPROVE_OPTION) {
                attachment = fileChooser.getSelectedFile();
                attachmentLabel.setText("Attached: " + attachment.getName());
                String filePath = attachment.getAbsolutePath();
                System.out.println("File path: " + filePath); 
                // Get file size
                long fileSize = attachment.length(); // Get file size in bytes
                System.out.println("File size in Bytes: " + fileSize + " bytes"); 
               
                
                double fileSizeInKB = fileSize / 1024.0; // Convert to KB
                double fileSizeInMB = fileSize / (1024.0 * 1024.0); // Convert to MB
                System.out.printf("File size in KB: %.2f KB\n", fileSizeInKB); // Print file size in KB
                System.out.printf("File size in MB: %.2f MB\n", fileSizeInMB); // Print file size in 
            }
        }
    }
    
    public File getSelectedFile() {
        return attachment; // Return the selected file
    }

    public String getSelectedFilePath() {
        return (attachment != null) ? attachment.getAbsolutePath() : null; // Return the path or null if no file is attached
    }
    
    public long getSelectedFileSize() {
        return (attachment != null) ? attachment.length() : 0; // Return file size or 0 if no file is attached
    }
    
    private class SendButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String from = fromField.getText();
            String to = toField.getText();
            String subject = subjectField.getText();
            String body = bodyArea.getText();
            File file = getSelectedFile(); // should i change to path?
            
            if (from.isEmpty() || to.isEmpty() || subject.isEmpty() || body.isEmpty()) {
                JOptionPane.showMessageDialog(Client1_Jframe.this, "Please fill all fields.", "Error", JOptionPane.ERROR_MESSAGE);
            }
            
            // check email validity
            if (!validEmail(from) || !validEmail(to)) { 
                JOptionPane.showMessageDialog(Client1_Jframe.this, "Invalid Email, please re-enter: ", "Error", JOptionPane.ERROR_MESSAGE);            	
            }
            
            // everything is correct and valid, then send email to server
            else {
            	 try {
                     JOptionPane.showMessageDialog(Client1_Jframe.this, "Email sent sent successfully to server!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    
                                  sendEmail(from, to, subject, body, file);
                                  String[] Savemail = {to, from, subject, body, file.toString()};
                                  saveEmails(Savemail);
                                                      
                         
            	 } catch (Exception ex) {
            		    JOptionPane.showMessageDialog(Client1_Jframe.this, 
            		            "Unexpected error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            		}
//  go to browser and type: http://localhost/phpmyadmin/
            }
        }
    }
//    private void clearFields() {
//        // Reset text fields
//        fromField.setText("");
//        toField.setText("");
//        subjectField.setText("");
//        bodyArea.setText("");
//        attachment = null;
//        fromField.requestFocus();
//    }
    
    
    private void sendEmail(String from, String to, String subject, String body, File file) throws IOException {
    	System.out.println("\n\nEmail Details");
    	System.out.println("Email Sent from: "+from);
        System.out.println("Email Sent to: "+to);
        System.out.println("Email Sent Subject: "+subject);
        System.out.println("Email Sent body: "+body);

    	DatagramSocket clientSocket = new DatagramSocket();
        InetAddress serverAddress = InetAddress.getByName(serverName);
        String mail;
        // sending the mail content
      
        // sending the file in chunks
        if (file != null) { 
        	String file_path = file.getAbsolutePath();
        
	        Path filePath = Paths.get(file_path); 
	        String fname = file.getName();
	        System.out.println(file_path);
	        
	        mail = from + "_" + to + "_" + subject + "_" + body+"_yes";
	        byte[] sendData = mail.getBytes();
	        
	        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, 34); 
	        clientSocket.send(sendPacket);
	        
	        if (file_path != null) {

	            byte[][] file_byt = splitFile(filePath, fname);  // Split file into byte arrays and store arrays in file_byt with sequence numbers
	            for (byte[] bytes : file_byt){  // for each array in file_byte
	            	System.out.println(Arrays.toString(bytes));  // Print byte arrays
	                
	            	// Send each packet to the server
	                sendPacket = new DatagramPacket(bytes, bytes.length, serverAddress, 34);
	                clientSocket.send(sendPacket); // transmitting the packets
            }
	            
            // Send a "FIN" message when packets are done transmitting
            String msg = "FIN";
            byte[] fin_msg= msg.getBytes();
            sendPacket = new DatagramPacket(fin_msg, fin_msg.length, serverAddress, 34);
            System.out.println("here");

            clientSocket.send(sendPacket); // transmitting the packets
        }

  
        }
        else {

	        mail = from + "_" + to + "_" + subject + "_" + body+"_no";
	        byte[] sendData = mail.getBytes();
	        
	        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, 34); 
	        clientSocket.send(sendPacket);
        }
        
        // how to know when he is not conencted then connected that now you can send it? ig there is something that keeps checking the bit in the database
        // if he is not conencted, just place the email in receiver directory but do not send it
        
        
        // create directory for every client at the server side, one for the sender and receiver
        // the receiver should have another column of bit 0 or 1 whether he received or not
        
//        DatagramPacket filePacket = new DatagramPacket(chunk, length, serverAddress, 33);
//        clientSocket.send(filePacket);
        
        // Receiving response from server (optional, implement if needed)
        byte[] receiveData = new byte[1024]; 
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        clientSocket.receive(receivePacket);
        String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
        JOptionPane.showMessageDialog(null, "Server Response: " +response);
        
        //add termination seq here 
        
        int TERMINATION_QS = JOptionPane.showConfirmDialog(
                null,
                "Would you like to Terminate the Connection?",
                "Confirmation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        
        // Add ActionListeners for the options
        if (TERMINATION_QS == JOptionPane.YES_OPTION) {
             System.out.println("You clicked Yes.");
             ///terminate with timeout
            String termination = "TERMINATE";
 	        byte[] sendData = termination.getBytes();
 	        
 	        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, portNumber); 
 	        clientSocket.send(sendPacket);
            clientSocket.setSoTimeout(TIMEOUT_SECONDS * 1000);

 	                     // Wait for ACK ACK with timeout
                   try {
                	   clientSocket.receive(receivePacket);
                       String ackAck = new String(receivePacket.getData(), 0, receivePacket.getLength());
                       if (ackAck.equals("ACK")) {
                           System.out.println("Received ACK from Server, Sending ACK- ACK");
                           
                           String ack = "ACK ACK";
                           byte[] ackBytes = ack.getBytes();
                           DatagramPacket ackPacket = new DatagramPacket(ackBytes, ackBytes.length, serverAddress, portNumber);
                           clientSocket.send(ackPacket);
                           System.out.println("Sent ACK ACK to server.");
                           
                           setVisible(false);

                           clientSocket.close();
                           System.exit(0);
                       } // innerif
                   } // try
                   catch (SocketTimeoutException e) {
                       System.out.println("Timeout waiting for ACK from Sever. Resending TERMINATE...");
                       clientSocket.send(sendPacket);
                     
                   } // catch
        
                   finally {
                       if (clientSocket != null && !clientSocket.isClosed()) {
                               clientSocket.close();
                               System.out.println("Connection terminated gracefully.");
                            
                       }
                   }
 	        
                   clientSocket.close();
                      
        } else if (TERMINATION_QS == JOptionPane.NO_OPTION) {
            System.out.println("You clicked No, you can resend an email");
            	//EmailFrame();
        } else {
            // Handle the case when the dialog is closed without selecting Yes or No
            System.out.println("Dialog closed without selection.");
        }

        
        //clientSocket.close();
        
    }//sendEmail


    public static boolean validEmail(String email) {
        //will keep looping until user enters a valid email
        return email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

      }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client1_Jframe::new);
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


}//clinet1_jframe end

