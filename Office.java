import SecuritySystem.*;

import org.omg.CORBA.*;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.PortableServer.*;
import org.omg.PortableServer.POA;

import java.io.*;
import javax.swing.*;

class OfficeServant extends OfficePOA {

    private Office parent;

    public OfficeServant(Office parentGUI){
        super();
        parent = parentGUI;
    }

    //Notifies Office that a Hub has gone into Alert, after multiple triggers
    @Override
    public void notifyOffice(String name) {
        parent.addMessage("Alarms Triggered at " + name + " \n");

    }
}

public class Office extends JFrame{
    private JPanel panel;
    private JTextArea officeText;

    public Office(String[] args){
        try{
            //Create ORB
            ORB orb = ORB.init(args, null);

            //get root reference and activate POA
            POA root = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
            root.the_POAManager().activate();

            //Create servant and register it with ORB
            OfficeServant officeServant = new OfficeServant(this);

            //Get object reference from servant
            org.omg.CORBA.Object ref = root.servant_to_reference(officeServant);
            SecuritySystem.Office officeRef = OfficeHelper.narrow(ref);

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
            String serviceName = "Office";
            NameComponent[] officeNameArray = nameService.to_name(serviceName);
            nameService.rebind(officeNameArray, officeRef);

            //GUI
            setTitle("Regional Office ");
            officeText = new JTextArea(20,25);
            panel = new JPanel();
            panel.add(officeText);
            getContentPane().add(panel, "Center");
            setSize(400,500);
            addWindowListener (new java.awt.event.WindowAdapter () {
                public void windowClosing (java.awt.event.WindowEvent evt) {
                    System.exit(0);;
                }
            } );

            officeText.append("Regional Office Monitoring...");

        }catch(Exception e){
            System.err.println("Cannot connect to Naming Service");
            System.err.println(e);
        }


    }

    //Adds messages to the office GUI
    public void addMessage(String msg){
        officeText.append(msg);
    }

    public static void main(String args[]){
        final String[] arguments = args;
        java.awt.EventQueue.invokeLater(new Runnable(){
            public void run(){ new Office(arguments).setVisible(true);}
        });
    }

}
