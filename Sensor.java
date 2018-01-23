import SecuritySystem.*;

import org.omg.CORBA.*;
import org.omg.CosNaming.*;

import java.io.*;
import javax.swing.*;
import java.awt.event.*;


public class Sensor extends JFrame implements SensorOperations {

    private JPanel sensorPanel;
    private JTextArea sensorText;
    private JButton trigger;
    private JButton reset;
    private JButton toggle;

    private String sensorName = "";
    private String roomName = "";
    private String hubName = "";
    private boolean onOff = true;

    public Sensor(String[] args, String name, String room, String connect){
        try{
            //Assign Sensor Name
            sensorName = name;
            roomName = room;
            hubName = connect;

            setTitle(sensorName);
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
            sensorText = new JTextArea(20,25);
            sensorText.append("Sensor Active...\n");

            sensorPanel = new JPanel();
            sensorPanel.add(sensorText);

            //Button to trigger sensor alarm
            trigger = new JButton("Trigger Sensor");
            trigger.addActionListener (new ActionListener() {
                public void actionPerformed (ActionEvent evt) {
                    sensorText.append("Intruder Detected, Sensor Triggered...\n");
                    hub.sensorAlert(sensorName, roomName); //Trigger Sensor passing Hub the Sensors name and room
                    trigger.setEnabled(false);
                    toggle.setEnabled(false); //If Sensor is tripped, the sensor cannot be turned ON/OFF
                    reset.setEnabled(true);
                }
            });
            sensorPanel.add(trigger, "Center");

            //Button to reset the sensor
            reset = new JButton("Reset Sensor");
            reset.setEnabled(false);
            reset.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    sensorText.append("Sensor Reset, Alarm Deactivated...\n");
                    hub.sensorReset(sensorName, roomName);//Reverts Sensor back to normal state.
                    reset.setEnabled(false);
                    trigger.setEnabled(true);
                    toggle.setEnabled(true);
                }
            });
            sensorPanel.add(reset, "Center");

            //Toggle Sensor On or Off, Sensor cannot be triggered while OFF
            toggle = new JButton("Sensor: ON");
            toggle.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (onOff == true)
                    {
                        toggle.setText("Sensor: OFF");
                        onOff = false;
                        hub.sensorOnOff(sensorName, roomName, onOff); //Tell the Hub this Sensor has been turned OFF
                        trigger.setEnabled(false);
                    }
                    else if (onOff == false)
                    {
                        toggle.setText("Sensor: ON");
                        onOff = true;
                        hub.sensorOnOff(sensorName, roomName, onOff); //Tell the Hub this Sensor has been turned ON
                        trigger.setEnabled(true);
                    }

                }
            });
            sensorPanel.add(toggle, "Center");

            getContentPane().add(sensorPanel, "Center");
            addWindowListener (new java.awt.event.WindowAdapter () {
                public void windowClosing (java.awt.event.WindowEvent evt) {
                    System.exit(0);
                }
            } );


        } catch (Exception e){
            System.err.println("Cannot connect to Naming Service");
            System.err.println(e);
        }
    }


    public static void main(String args[])
    {
        final String[] arguments = args;
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Sensor(arguments, "Sensor A", "Room 1", "Hub Alpha").setVisible(true);
                new Sensor(arguments, "Sensor B", "Room 2", "Hub Alpha").setVisible(true);
                new Sensor(arguments, "Sensor C", "Room 1", "Hub Beta").setVisible(true);
                new Sensor(arguments, "Sensor D", "Room 2", "Hub Beta").setVisible(true);
            }
        });


    }

}
