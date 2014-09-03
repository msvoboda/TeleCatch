package cz.svoboda.telecatch;

import java.util.Date;

/**
 * Created by svoboda on 28.8.2014.
 */
public class CallItem {
    public CallItem(String phone) {
        PhoneNumber = phone;
        Name = "";
        Type = "";
    }

    public String PhoneNumber;
    public Date DateTime;
    public String Name;
    public String Message;
    public String Type;
}
