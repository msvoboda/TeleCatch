package cz.svoboda.telecatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
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
                    list.add(ci);
                    if (_notify != null) {
                        _notify.OnNotify(ci);
                    }

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
    private Timer timer;
    private String phone_notify;
    private String phone_title;
    long last_time=-1;
    long interval=-1;

	public CallHelper(Context ctx) {
		this.ctx = ctx;
		
		callStateListener = new CallStateListener();
		outgoingReceiver = new OutgoingReceiver();

        MyTimerTask myTask = new MyTimerTask();
        timer = new Timer();
        timer.schedule(myTask,30000,30000);
        last_time = System.currentTimeMillis();

       SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
        phone_notify = preferences.getString("phone_text",null);
        String interval_string = preferences.getString("sync_frequency", "60000");
        interval = Long.parseLong(interval_string);
        phone_title = preferences.getString("pref_message_title", "60000");
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
            /*
            CallItem ci = new CallItem("+420777607978");
            ci.DateTime = new Date(System.currentTimeMillis());
            if (_notify != null) {
                _notify.OnNotify(ci);
            }
            list.add(ci);*/
            System.out.println("timer");
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
	}
	
	/**
	 * Stop calls detection.
	 */
	public void stop() {
		tm.listen(callStateListener, PhoneStateListener.LISTEN_NONE);
		ctx.unregisterReceiver(outgoingReceiver);
	}

}
