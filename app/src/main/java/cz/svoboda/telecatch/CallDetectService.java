package cz.svoboda.telecatch;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class CallDetectService extends Service {
    private static CallHelper callHelper = null;

    public CallDetectService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (callHelper == null) {
            callHelper = new CallHelper(this);
        }

        int res = super.onStartCommand(intent, flags, startId);
        callHelper.start();
        return res;
    }

    public static boolean getState() {

        if (callHelper != null) {
            return callHelper.getState();
        }

        return false;
    }

	
    @Override
	public void onDestroy() {
		super.onDestroy();
		callHelper.stop();
        callHelper = null;
	}

	@Override
    public IBinder onBind(Intent intent) {
		// not supporting binding
    	return null;
    }
}
