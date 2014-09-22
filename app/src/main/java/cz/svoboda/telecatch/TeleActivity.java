package cz.svoboda.telecatch;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.List;


public class TeleActivity extends Activity implements CallNotifyInterface {

    private boolean detectEnabled;

    //private TextView textViewDetectState;
    MenuItem play;
    //private Button buttonToggleDetect;
   // private Button buttonExit;
   ArrayList<CallItem> call_list;
    Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tele);


        /*
        textViewDetectState = (TextView) findViewById(R.id.textViewDetectState);
        buttonToggleDetect = (ToggleButton) findViewById(R.id.buttonDetectToggle);
        buttonToggleDetect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setDetectEnabled(!detectEnabled);
            }
        });*/

        CallHelper.setNotify(this);


        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String phone = preferences.getString("phone_text",null);

        call_list = new ArrayList<CallItem>();
        CallAdapter adapter = new CallAdapter(getApplicationContext(),call_list);
        ListView lv = (ListView)findViewById(R.id.callList);
        lv.setAdapter(adapter);
        //call_list.add(new CallItem("nobody"));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.tele, menu);

        play = (MenuItem)menu.findItem(R.id.action_start);
        setDetectEnabled(CallDetectService.getState());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent i = new Intent(this, SettingsActivity.class);
            startActivityForResult(i, 1);
            return true;
        }
        else if (id == R.id.action_exit)
        {
            setDetectEnabled(false);
            TeleActivity.this.finish();
        }
        else if (id == R.id.action_clear)
        {
            call_list.clear();
            CallAdapter adapter = new CallAdapter(getApplicationContext(),call_list);
            ListView lv = (ListView)findViewById(R.id.callList);
            lv.setAdapter(adapter);
        }
        else if (id == R.id.action_start)
        {
            setDetectEnabled(!detectEnabled);
        }
        else if (id == R.id.action_about) {
            showAbout();

        }

        return super.onOptionsItemSelected(item);
    }

    private void setDetectEnabled(boolean enable) {
        detectEnabled = enable;

        Intent intent = new Intent(this, CallDetectService.class);
        if (enable) {
            //CallDetectService component = (CallDetectService)intent.getComponent();
            // start detect service
            startService(intent);

            //buttonToggleDetect.setText("off");
            //textViewDetectState.setText("Detecting");
            play.setIcon(R.drawable.stop);
            //textViewDetectState.setTextColor(getResources().getColor(R.color.green));
        }
        else {
            // stop detect service
            stopService(intent);

            //buttonToggleDetect.setText("on");
            //textViewDetectState.setText("Not detecting");
            play.setIcon(R.drawable.play);
            //textViewDetectState.setTextColor(getResources().getColor(R.color.red));
        }
    }

    @Override
    public void OnNotify(final CallItem call)
    {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                {
                    //call_list.add(new CallItem("nobody"));
                    call_list.add(call);
                    CallAdapter adapter = new CallAdapter(getApplicationContext(),call_list);
                    ListView lv = (ListView)findViewById(R.id.callList);
                    lv.setAdapter(adapter);
                }
            }
        };
        mHandler.post(runnable);
    }

    protected void showAbout() {
        // Inflate the about message contents
        View messageView = getLayoutInflater().inflate(R.layout.about, null, false);

        PackageInfo pInfo = null;
        try {
            pInfo = getBaseContext().getPackageManager().getPackageInfo(getBaseContext().getPackageName(), 0);
            TextView text = (TextView)messageView.findViewById(R.id.textVersion);
            text.setText(pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.icon);
        builder.setTitle(R.string.app_name);
        builder.setView(messageView);
        builder.create();
        builder.show();
    }
}
