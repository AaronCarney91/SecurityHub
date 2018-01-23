import SecuritySystem.*;

import org.omg.CORBA.*;
import org.omg.CosNaming.*;

import java.io.*;
import javax.swing.*;
import java.awt.event.*;

public class Camera extends JFrame implements CameraOperations{

    private JPanel cameraPanel;
    private JTextArea cameraText;
    private JButton imageButton;
    private JButton toggle;
    private JButton reset;

    private String cameraName = "";
    private String roomName = "";
    private String hubName = "";
    private boolean onOff = true;

    public Camera(String[] args, String name, String room, String connect){

        try{
            cameraName = name;
            roomName = room;
            hubName = connect;

            setTitle(cameraName);
            setSize(400,500);

            //create ORB
            ORB orb = ORB.init(args, null);

            //Get Naming service reference
            BufferedReader in = new BufferedReader(new FileReader("naming.ref"));
            String ior = in.readLine();
            org.omg.CORBA.Object nameServiceObj = orb.string_to_object(ior);

            if(nameServiceObj == null)
            {
                System.out.println("namingServiceObj is null");
                return;
            }

            //Connect to Hub with matching hubName
            NamingContextExt nameService = NamingContextExtHelper.narrow(nameServiceObj);
            SecuritySystem.Hub hub = HubHelper.narrow(nameService.resolve_str(hubName));

            //GUI
            cameraText = new JTextArea(20,25);
            cameraText.append("Sensor Active...\n");

            cameraPanel = new JPanel();
            cameraPanel.add(cameraText);

            //Button to take picture, Time and Date.
            imageButton = new JButton("Take Picture");
            imageButton.addActionListener (new ActionListener() {
                public void actionPerformed (ActionEvent evt) {
                    cameraText.append("Picture Taken. \n");
                    hub.cameraAlert(cameraName, roomName); //Trigger Camera and take a picture(Timestamp)
                    imageButton.setEnabled(false);
                    toggle.setEnabled(false); //Camera cannot be turned off while triggered
                    reset.setEnabled(true);
                }
            });
            cameraPanel.add(imageButton, "Center");

            //Button to reset the camera
            reset = new JButton("Reset Sensor");
            reset.setEnabled(false);
            reset.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    cameraText.append("Sensor Reset, Alarm Deactivated...\n");
                    hub.sensorReset(cameraName, roomName); //Revert Camera to normal state
                    reset.setEnabled(false);
                    imageButton.setEnabled(true);
                    toggle.setEnabled(true);
                }
            });
            cameraPanel.add(reset, "Center");

            //Toggle Camera On or Off, Cannot take picture while OFF
            toggle = new JButton("Sensor: ON");
            toggle.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (onOff == true)
                    {
                        toggle.setText("Camera: OFF");
                        onOff = false;
                        hub.sensorOnOff(cameraName, roomName, onOff); //Tell Hub that Camera was turned OFF
                        imageButton.setEnabled(false);
                    }
                    else if (onOff == false)
                    {
                        toggle.setText("Camera: ON");
                        onOff = true;
                        hub.sensorOnOff(cameraName, roomName, onOff); //Tell Hub that Camera was turned ON
                        imageButton.setEnabled(true);
                    }

                }
            });
            cameraPanel.add(toggle, "Center");

            getContentPane().add(cameraPanel, "Center");
            addWindowListener (new java.awt.event.WindowAdapter () {
                public void windowClosing (java.awt.event.WindowEvent evt) {
                    System.exit(0);
                }
            } );

        }catch(Exception e)
        {
            System.err.println("Cannot connect to Naming Service");
            System.err.println(e);
        }

    }

    public static void main(String args[])
    {
        final String[] arguments = args;
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Camera(arguments, "Camera A", "Room 1", "Hub Alpha").setVisible(true);
                new Camera(arguments, "Camera B", "Room 2", "Hub Alpha").setVisible(true);
                new Camera(arguments, "Camera C", "Room 1", "Hub Beta").setVisible(true);
                new Camera(arguments, "Camera D", "Room 2", "Hub Beta").setVisible(true);
            }
        });


    }


}
