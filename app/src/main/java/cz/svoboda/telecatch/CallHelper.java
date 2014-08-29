package cz.svoboda.telecatch;

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
    static CallNotifyInterface _notify;

    public static void setNotify(CallNotifyInterface notif)
    {
        _notify = notif;
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

                    try {
                        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(incomingNumber));
                        Cursor idCursor = ctx.getContentResolver().query(uri, null, null, null, null);
                        while (idCursor.moveToNext()) {
                            String id = idCursor.getString(idCursor.getColumnIndex(ContactsContract.Contacts._ID));
                            String key = idCursor.getString(idCursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
                            String name = idCursor.getString(idCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                            ci.Name = name;
                            System.out.print("search: " + id + " key: " + key + " name: " + name);
                        }
                        idCursor.close();
                    }
                    catch (Exception e) {

                    }

                    list.add(ci);
                    if (_notify != null) {
                        _notify.OnNotify(ci);
                    }
                    System.out.print("Call:"+incomingNumber);
                }
				Toast.makeText(ctx, 
						"Incoming: "+incomingNumber, 
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
	        		"Outgoing: "+number, 
	        		Toast.LENGTH_LONG).show();
	    }
	}

	private Context ctx;
	private TelephonyManager tm;
	private CallStateListener callStateListener;
	private OutgoingReceiver outgoingReceiver;
    private SmsReceiver smsReceiver;

    private Timer timer;
    private String phone_notify;
    private String phone_title;
    long last_time=-1;
    long interval=-1;

	public CallHelper(Context ctx) {
		this.ctx = ctx;
		
		callStateListener = new CallStateListener();
		outgoingReceiver = new OutgoingReceiver();
        smsReceiver = new SmsReceiver();

        MyTimerTask myTask = new MyTimerTask();
        timer = new Timer();
        timer.schedule(myTask,30000,30000);
        last_time = System.currentTimeMillis();

       SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
        phone_notify = preferences.getString("phone_text",null);
        String interval_string = preferences.getString("sync_frequency", "60000");
        interval = Long.parseLong(interval_string);
        phone_title = preferences.getString("message_title", "Call report:");
	}

    class MyTimerTask extends TimerTask {
        public void run() {
            long time = System.currentTimeMillis();
            if (time > last_time+interval) {
                SmsManager sms = SmsManager.getDefault();
                String msg =phone_title;

                for(int i =0; i < list.size();i++) {
                    CallItem c = list.get(i);
                    if (i == 0)
                        msg+=c.PhoneNumber;
                    else
                        msg+=", "+c.PhoneNumber;
                }

                if (list.size()>0) {
                    sms.sendTextMessage(phone_notify, null, msg, null, null);
                }

                list.clear();
                last_time = System.currentTimeMillis();
            }

            System.out.println("timer");
        }
    }

    public class SmsReceiver extends BroadcastReceiver{

        private SharedPreferences preferences;

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")){
                Bundle bundle = intent.getExtras();           //---get the SMS message passed in---
                SmsMessage[] msgs = null;
                String msg_from;
                if (bundle != null){
                    //---retrieve the SMS message received---
                    try{
                        Object[] pdus = (Object[]) bundle.get("pdus");
                        msgs = new SmsMessage[pdus.length];
                        for(int i=0; i<msgs.length; i++){
                            msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
                            msg_from = msgs[i].getOriginatingAddress();
                            String msgBody = msgs[i].getMessageBody();
                            CallItem ci = new CallItem(msg_from);
                            ci.DateTime = new Date(System.currentTimeMillis());
                            ci.Name = msg_from;
                            ci.Message = msgBody;
                            ci.Type = "SMS";
                            list.add(ci);
                            if (_notify != null) {
                                _notify.OnNotify(ci);
                            }
                        }
                    }catch(Exception e){
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
		tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
		tm.listen(callStateListener, PhoneStateListener.LISTEN_CALL_STATE);

		IntentFilter intentFilter = new IntentFilter(Intent.ACTION_NEW_OUTGOING_CALL);
		ctx.registerReceiver(outgoingReceiver, intentFilter);


        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
        ctx.registerReceiver(smsReceiver, mIntentFilter);

	}
	
	/**
	 * Stop calls detection.
	 */
	public void stop() {
		tm.listen(callStateListener, PhoneStateListener.LISTEN_NONE);
		ctx.unregisterReceiver(outgoingReceiver);
        ctx.unregisterReceiver(smsReceiver);
        timer.cancel();
	}

}
