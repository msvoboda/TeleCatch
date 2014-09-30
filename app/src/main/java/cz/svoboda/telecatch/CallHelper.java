package cz.svoboda.telecatch;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class CallHelper {

    ArrayList<CallItem> list = new ArrayList<CallItem>();
    ArrayList<CallItem> phone = new ArrayList<CallItem>();
    ArrayList<CallItem> sms = new ArrayList<CallItem>();
    static CallNotifyInterface _notify;

    public static void setNotify(CallNotifyInterface notif) {
        _notify = notif;
    }


    private String GetContactName(String phone_number) {
        String name = null;
        try {
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone_number));
            Cursor idCursor = ctx.getContentResolver().query(uri, null, null, null, null);
            while (idCursor.moveToNext()) {
                String id = idCursor.getString(idCursor.getColumnIndex(ContactsContract.Contacts._ID));
                String key = idCursor.getString(idCursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
                name = idCursor.getString(idCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                System.out.print("search: " + id + " key: " + key + " name: " + name);
            }
            idCursor.close();
        } catch (Exception e) {

        }
        return name;
    }


    /**
     * Listener to detect incoming calls.
     */
    private class CallStateListener extends PhoneStateListener {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    // called when someone is ringing to this phone
                    if (list != null) {
                        CallItem ci = new CallItem(incomingNumber);
                        ci.DateTime = new Date(System.currentTimeMillis());
                        ci.Type = "PHONE";
                        ci.Name = GetContactName(incomingNumber);
                        list.add(ci);
                        phone.add(ci);
                        if (_notify != null) {
                            _notify.OnNotify(ci);
                        }
                        System.out.print("Call:" + incomingNumber);
                    }
                    Toast.makeText(ctx,
                            "Incoming: " + incomingNumber,
                            Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    /**
     * Broadcast receiver to detect the outgoing calls.
     */
    public class OutgoingReceiver extends BroadcastReceiver {
        public OutgoingReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String number = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);

            Toast.makeText(ctx,
                    "Outgoing: " + number,
                    Toast.LENGTH_LONG).show();
        }
    }

    private Context ctx;
    private TelephonyManager telephone_manager;
    private CallStateListener callStateListener;
    private OutgoingReceiver outgoingReceiver;
    private SmsReceiver smsReceiver;
    BroadcastReceiver send_receiver;

    private Timer timer;
    private String phone_notify;
    private String phone_title;
    long last_time = -1;
    long interval = -1;
    boolean send_sms = true;
    boolean send_email = true;
    //
    String mail_port;
    String mail_host;
    String mail_user;
    String mail_pass;
    String fromMail;
    String toMail;
    // internal
    static int instances = 0;
    static boolean state = false;
    boolean error = false;


    public CallHelper(Context ctx) {
        this.ctx = ctx;
        ///
        callStateListener = new CallStateListener();
        outgoingReceiver = new OutgoingReceiver();
        smsReceiver = new SmsReceiver();
        ///
        MyTimerTask myTask = new MyTimerTask();
        timer = new Timer();
        timer.schedule(myTask, 30000, 30000);
        last_time = System.currentTimeMillis();
        ///
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
        phone_notify = preferences.getString("phone_text", null);
        String interval_string = preferences.getString("sync_frequency", "60000");
        interval = Long.parseLong(interval_string);
        phone_title = preferences.getString("message_title", "Call report:");
        send_sms = preferences.getBoolean("sms_enabled", true);
        send_email = preferences.getBoolean("email_enabled", true);
        mail_port = preferences.getString("port_text", "25");
        mail_host = preferences.getString("smtp_text", "");
        mail_user = preferences.getString("user_text", "");
        mail_pass = preferences.getString("pass_text", "");
        fromMail = preferences.getString("from_text", "");
        toMail = preferences.getString("email_text", "");
        ///
    }

    class MyTimerTask extends TimerTask {
        public void run() {
            long time = System.currentTimeMillis();
            if (time > last_time + interval) {
                SmsManager sms_manager = SmsManager.getDefault();

                String msg = phone_title;

                for (int i = 0; i < phone.size(); i++) {
                    CallItem c = phone.get(i);
                    if (i == 0)
                        msg += c.PhoneNumber + " [" + c.Name + "] - " + c.Message;
                    else
                        msg += ", " + c.PhoneNumber + " [" + c.Name + "] - " + c.Message;
                }

                String sms_msg = "";
                for (int i = 0; i < sms.size(); i++) {
                    CallItem c = sms.get(i);
                    if (i == 0) {
                        sms_msg += "SMS: "+ c.PhoneNumber + " [" + c.Name + "] - " + c.Message;
                    }
                    else
                        sms_msg += ", " + c.PhoneNumber + " [" + c.Name + "] - " + c.Message;
                }

                String to_send = "";
                String delimiter = "";
                if (phone.size()>0&& sms.size()>0)
                    delimiter = ";";

                to_send = msg+delimiter;
                if (sms.size() > 0)
                    to_send+=sms_msg;

                error = false;
                if (send_email == true && list.size() > 0)
                {
                    try
                    {
                        SendMail sender = new SendMail();
                        sender.setMailServerProperties(mail_port, mail_host, mail_user, mail_pass);
                        sender.createEmailMessage(fromMail, new String[]{toMail}, phone_title, to_send);
                        sender.sendEmail();
                    }
                    catch (Exception e)
                    {
                        CallItem ci = new CallItem("TeleCatch");
                        ci.DateTime = new Date(System.currentTimeMillis());
                        ci.Name = "ERROR MAIL";
                        ci.Message = e.getMessage();
                        ci.Type = "ERR";
                        list.add(ci);
                        if (_notify != null) {
                            _notify.OnNotify(ci);
                        }
                        error = true;
                        System.out.print(e.getMessage());
                    }
                }

                if (send_sms == true && list.size() > 0) try {
                    String SENT = "SMS_SENT";
                    PendingIntent sentPI = PendingIntent.getBroadcast(ctx, 0, new Intent(SENT), 0);
                    //PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,new Intent(DELIVERED), 0);
                    //---when the SMS has been sent---
                    if (send_receiver == null) {
                        send_receiver = new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context arg0, Intent arg1) {
                                switch (getResultCode()) {
                                    case Activity.RESULT_OK:
                                        Toast.makeText(ctx, "SMS was sent",
                                                Toast.LENGTH_SHORT).show();
                                        break;
                                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                                        CallItem ci = new CallItem("TeleCatch");
                                        ci.DateTime = new Date(System.currentTimeMillis());
                                        ci.Name = "ERROR SMS";
                                        ci.Message = "SMS wasn't sent.";
                                        ci.Type = "ERR";
                                        if (_notify != null) {
                                            _notify.OnNotify(ci);
                                        }
                                        error = true;
                                        break;
                                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                                        ci = new CallItem("TeleCatch");
                                        ci.DateTime = new Date(System.currentTimeMillis());
                                        ci.Name = "ERROR SMS";
                                        ci.Message = "SMS wasn't sent. No Service";
                                        ci.Type = "ERR";
                                        if (_notify != null) {
                                            _notify.OnNotify(ci);
                                        }
                                        error = true;
                                        break;
                                    case SmsManager.RESULT_ERROR_NULL_PDU:
                                        ci = new CallItem("TeleCatch");
                                        ci.DateTime = new Date(System.currentTimeMillis());
                                        ci.Name = "ERROR SMS";
                                        ci.Message = "SMS wasn't sent. Null PDU";
                                        ci.Type = "ERR";
                                        if (_notify != null) {
                                            _notify.OnNotify(ci);
                                        }
                                        error = true;
                                        break;
                                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                                        ci = new CallItem("TeleCatch");
                                        ci.DateTime = new Date(System.currentTimeMillis());
                                        ci.Name = "ERROR SMS";
                                        ci.Message = "SMS wasn't sent. Radio off";
                                        ci.Type = "ERR";
                                        if (_notify != null) {
                                            _notify.OnNotify(ci);
                                        }
                                        error = true;
                                        break;
                                }
                            }
                        };
                        ctx.registerReceiver(send_receiver, new IntentFilter(SENT));
                    }
                    sms_manager.sendTextMessage(phone_notify, null, to_send, sentPI, null);
                } catch (Exception e) {
                    CallItem ci = new CallItem("TeleCatch");
                    ci.DateTime = new Date(System.currentTimeMillis());
                    ci.Name = "ERROR SMS";
                    ci.Message = e.getMessage();
                    ci.Type = "ERR";
                    list.add(ci);
                    if (_notify != null) {
                        _notify.OnNotify(ci);
                    }
                    error = true;
                    System.out.print(e.getMessage());
                }

                if (error == false) {
                    list.clear();
                    phone.clear();
                    sms.clear();
                }
                last_time = System.currentTimeMillis();
            }

            System.out.println("timer");
        }
    }

    public class SmsReceiver extends BroadcastReceiver {

        private SharedPreferences preferences;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
                Bundle bundle = intent.getExtras();           //---get the SMS message passed in---
                SmsMessage[] msgs = null;
                String msg_from;
                if (bundle != null) {
                    //---retrieve the SMS message received---
                    try {
                        Object[] pdus = (Object[]) bundle.get("pdus");
                        msgs = new SmsMessage[pdus.length];
                        for (int i = 0; i < msgs.length; i++) {
                            msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                            msg_from = msgs[i].getOriginatingAddress();
                            String msgBody = msgs[i].getMessageBody();
                            CallItem ci = new CallItem(msg_from);
                            ci.DateTime = new Date(System.currentTimeMillis());
                            String name = GetContactName(msg_from);
                            ci.PhoneNumber = msg_from;
                            if (name != null)
                                ci.Name = name;
                            else
                                ci.Name = msg_from;

                            ci.Message = msgBody;
                            ci.Type = "SMS";
                            list.add(ci);
                            sms.add(ci);
                            if (_notify != null) {
                                _notify.OnNotify(ci);
                            }
                        }
                    } catch (Exception e) {
//                            Log.d("Exception caught",e.getMessage());
                    }
                }
            }
        }
    }



    /**
     * Start calls detection.
     */
    public void start() {

        if (state == true) {
            CallItem ci = new CallItem("");
            ci.DateTime = new Date(System.currentTimeMillis());
            ci.Name = "info";
            ci.Message = "Resume service";
            ci.Type = "INFO";
            if (_notify != null) {
                _notify.OnNotify(ci);
            }
            return;
        }

        telephone_manager = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
        telephone_manager.listen(callStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_NEW_OUTGOING_CALL);
        ctx.registerReceiver(outgoingReceiver, intentFilter);

        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
        ctx.registerReceiver(smsReceiver, mIntentFilter);


        CallItem ci = new CallItem("");
        ci.DateTime = new Date(System.currentTimeMillis());
        ci.Name = "info";
        ci.Message = "Start service " + String.valueOf(++instances);
        ci.Type = "INFO";
        if (_notify != null) {
            _notify.OnNotify(ci);
        }

        state = true;
    }

    /**
     * Stop calls detection.
     */
    public void stop() {
        telephone_manager.listen(callStateListener, PhoneStateListener.LISTEN_NONE);
        ctx.unregisterReceiver(outgoingReceiver);
        ctx.unregisterReceiver(smsReceiver);
        if (send_receiver != null)
            ctx.unregisterReceiver(send_receiver);

        timer.cancel();

        CallItem ci = new CallItem("");
        ci.DateTime = new Date(System.currentTimeMillis());
        ci.Name = "info";
        ci.Message = "Stop service ";
        ci.Type = "INFO";
        instances--;
        if (_notify != null) {
            _notify.OnNotify(ci);
        }

        telephone_manager = null;
        ctx = null;
        timer = null;

        state = false;
    }

    public boolean getState() {
        return state;
    }
}

