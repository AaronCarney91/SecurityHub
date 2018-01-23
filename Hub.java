import SecuritySystem.*;

import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.PortableServer.*;
import org.omg.PortableServer.POA;
import org.omg.CORBA.*;

import java.io.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import javax.swing.*;

class HubServant extends HubPOA{

    public SecuritySystem.Office office;
    private Hub parent;
    private ArrayList<String> alarmNames = new ArrayList<>();
    private ArrayList<String> alarmRooms = new ArrayList<>();
    private ArrayList<Timestamp> alarmTimes = new ArrayList<>();

    public HubServant(Hub parentGUI){
        super();
        parent = parentGUI;
    }

    //Check if alarms came from same room within five seconds of each other
    public void raiseAlarm(String room){
        if(alarmRooms.size() >= 2)
        {
            for(int i = 0; i < alarmRooms.size()-1; i++){
                if(alarmRooms.get(i).compareTo(room) == 0)
                {
                    //Matching room found, check time difference
                    if((alarmTimes.get(alarmTimes.size()-1).getTime() - alarmTimes.get(i).getTime()) <= 5000)
                    {
                        office.notifyOffice(parent.hubName);
                        return;
                    }
                }
            }
        }
    }



    //Notifies Hub that a sensor was triggered
    @Override
    public void sensorAlert(String name, String room) {
        //Write String to GUI and Log
        String log = name + " At "  + room + " has been triggered. \n";
        parent.addMessage(log);
        logAlarm(log);

        //Add details to array list
        alarmNames.add(name);
        alarmRooms.add(room);
        alarmTimes.add(Image.getTimeStamp());

        //should the office be notified?
        raiseAlarm(room);
    }

    //Notifies Hub that a Camera or Sensor was turned on or off
    @Override
    public void sensorOnOff(String name, String room, boolean onOff) {
        if (onOff == false){
            String log = name + " in " + room + " was turned OFF.\n";
            parent.addMessage(log);
            logAlarm(log);
        }
        else if (onOff == true){
            String log = name + " in " + room + " was turned ON.\n";
            parent.addMessage(log);
            logAlarm(log);
        }
    }

    //Notifies Hub that a Camera was triggered, taking a picture(timestamp)
    @Override
    public void cameraAlert(String name, String room) {
        //Write string to GUI and Log
        String log = "Image taken from " + name + " in " + room + " at " + Image.getTimeStamp() + "\n";
        parent.addMessage(log);
        logAlarm(log);

        //Add details to array lists
        alarmNames.add(name);
        alarmRooms.add(room);
        alarmTimes.add(Image.getTimeStamp());

        //should the office be notified?
        raiseAlarm(room);
    }

    //Notifies Hub that the triggered sensor or camera has been reset to normal
    @Override
    public void sensorReset(String name, String room) {
        String log = name + " At " + room + " has been reset. \n";
        parent.addMessage(log);
        logAlarm(log);

        int removeAt = alarmNames.indexOf(name);
        alarmNames.remove(removeAt);
        alarmRooms.remove(removeAt);
        alarmTimes.remove(removeAt);
    }

    //Writes all Hub, Sensor and Camera activities to a file called Alarm Log
    @Override
    public void logAlarm(String log) {
        try(BufferedWriter bw = new BufferedWriter(new FileWriter("Alarm_Log.txt",true))){
            bw.write(log);
        }catch(Exception e){
            System.err.println("Cannot connect to Naming Service");
            System.err.println(e);
        }
    }


}


public class Hub extends JFrame {

    private JPanel panel;
    private JTextArea hubText;
    public String hubName = "";


    public Hub(String[] args, String name){
        try{
            hubName = name;

            //Create ORB
            ORB orb = ORB.init(args, null);

            //get root reference and activate POA
            POA root = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
            root.the_POAManager().activate();

            //Create servant and register it with ORB
            HubServant hubServant = new HubServant(this);

            //Get object reference from servant
            org.omg.CORBA.Object ref = root.servant_to_reference(hubServant);
            SecuritySystem.Hub hubRef = HubHelper.narrow(ref);

            //Get Naming service reference
            BufferedReader in = new BufferedReader(new FileReader("naming.ref"));
            String ior = in.readLine();
            org.omg.CORBA.Object nameServiceObj = orb.string_to_object(ior);

            if(nameServiceObj == null){
                System.out.println("nameServiceObj = null");
                return;
            }
            NamingContextExt nameService = NamingContextExtHelper.narrow(nameServiceObj);

            //Bind the Hub Object to the Naming Service
            String serviceName = hubName;
            NameComponent[] hubNameArray = nameService.to_name(serviceName);
            nameService.rebind(hubNameArray, hubRef);

            //All hubs connect to the one Regional Office
            hubServant.office = OfficeHelper.narrow(nameService.resolve_str("Office"));

            //GUI
            setTitle(hubName);
            panel = new JPanel();
            hubText = new JTextArea(20,25);
            panel.add(hubText);
            getContentPane().add(panel,"Center");
            setSize(400, 500);
            addWindowListener (new java.awt.event.WindowAdapter () {
                public void windowClosing (java.awt.event.WindowEvent evt) {
                    System.exit(0);;
                }
            } );

            hubText.append(hubName + " Active...\n");

        }catch(Exception e)
        {
            System.err.println("Cannot connect to Naming Service");
            System.err.println(e);
        }
    }

    //Adds messages to the Hubs GUI
    public void addMessage(String msg){
        hubText.append(msg);
    }


    public static void main(String args[]){
        final String[] arguments = args;
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new Hub(arguments, "Hub Alpha").setVisible(true);
                new Hub(arguments, "Hub Beta").setVisible(true);
            }
        });

    }



}
