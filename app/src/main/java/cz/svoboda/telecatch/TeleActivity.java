package cz.svoboda.telecatch;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;


public class TeleActivity extends Activity {

    private boolean detectEnabled;

    private TextView textViewDetectState;
    private Button buttonToggleDetect;
   // private Button buttonExit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tele);

        textViewDetectState = (TextView) findViewById(R.id.textViewDetectState);

        buttonToggleDetect = (ToggleButton) findViewById(R.id.buttonDetectToggle);
        buttonToggleDetect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setDetectEnabled(!detectEnabled);
            }
        });

        /*
        buttonExit = (Button) findViewById(R.id.buttonExit);
        buttonExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setDetectEnabled(false);
                TeleActivity.this.finish();
            }
        });*/
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.tele, menu);
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

        return super.onOptionsItemSelected(item);
    }

    private void setDetectEnabled(boolean enable) {
        detectEnabled = enable;

        Intent intent = new Intent(this, CallDetectService.class);
        if (enable) {
            //CallDetectService component = (CallDetectService)intent.getComponent();
            // start detect service
            startService(intent);

            buttonToggleDetect.setText("off");
            textViewDetectState.setText("Detecting");
            textViewDetectState.setTextColor(getResources().getColor(R.color.green));
        }
        else {
            // stop detect service
            stopService(intent);

            buttonToggleDetect.setText("on");
            textViewDetectState.setText("Not detecting");
            textViewDetectState.setTextColor(getResources().getColor(R.color.red));
        }
    }
}
