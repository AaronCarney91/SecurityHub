import java.sql.Timestamp;



public class Image {

    private static Timestamp time;

    //returns current time and date
    public static Timestamp getTimeStamp(){
        time = new Timestamp(System.currentTimeMillis());
        return time;
    }





}
