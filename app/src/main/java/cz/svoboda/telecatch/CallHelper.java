package cz.svoboda.telecatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
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
	}

    class MyTimerTask extends TimerTask {
        public void run() {
            long time = System.currentTimeMillis();
            if (time > last_time+interval) {
                SmsManager sms = SmsManager.getDefault();
                String msg ="Calls report:";

                for(int i =0; i < list.size();i++) {
                    CallItem c = list.get(i);
                    msg+=c.PhoneNumber+", ";
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
