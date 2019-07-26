package in.dotworld.mqttserver;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.InputFilter;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Toast;

import org.apache.log4j.BasicConfigurator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import in.dotworld.mqttserver.broker.MQTTService;
import in.dotworld.mqttserver.broker.ServerInstance;
import in.dotworld.mqttserver.util.InputFilterMinMax;
import in.dotworld.mqttserver.util.Utils;
import io.moquette.BrokerConstants;

public class MainActivity extends AppCompatActivity {

    private MQTTService mService;
    private boolean mBound = false;
    Context context;

    EditText port, username, password, host;
    RadioButton auth, noAuth;
    LinearLayout authFields;
    File confFile, passwordFile;

    @Override
    protected void onStart() {
        super.onStart();
        this.bindService(new Intent(this, MQTTService.class), mConnection, BIND_IMPORTANT);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            MainActivity.this.mService = ((MQTTService.LocalBinder) service).getService();
            MainActivity.this.mBound = ((MQTTService.LocalBinder) service).getServerStatus();
            // MainActivity.this.updateStartedStatus();
        }

        public void onServiceDisconnected(ComponentName arg0) {
            MainActivity.this.mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.activity_main);
        BasicConfigurator.configure();
        host = findViewById(R.id.host);
        port = findViewById(R.id.port);
        port.setFilters(new InputFilter[]{new InputFilterMinMax(1, 65535)});
        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        auth = findViewById(R.id.auth);
        noAuth = findViewById(R.id.no_auth);
        authFields = findViewById(R.id.auth_fields);
        authFields.setVisibility(View.GONE);

        confFile = new File(getApplicationContext().getDir("media", 0).getAbsolutePath() + Utils.BROKER_CONFIG_FILE);
        passwordFile = new File(getApplicationContext().getDir("media", 0).getAbsolutePath() + Utils.PASSWORD_FILE);
        Log.i("MAIN", confFile.getAbsolutePath());
        loadConfig();
        if(mBound && ServerInstance.getServerInstance() == null){
            startService();
        }
        Log.i("MAIN", "JAVA : " + String.valueOf(Utils.getVersion()));
    }

    public void getSubs(View v) {
        try {
            Collection<String> clients = ServerInstance.getServerInstance().getConnectionsManager().getConnectedClientIds();

            clients.forEach(client -> {
                Log.i("Clients", String.valueOf(ServerInstance.getServerInstance().getConnectionsManager().isConnected(client)));
            });
        } catch (Exception e) {

        }
    }


    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch (view.getId()) {
            case R.id.no_auth:
                if (checked) {
                    authFields.setVisibility(View.GONE);
                }
                break;
            case R.id.auth:
                if (checked) {
                    authFields.setVisibility(View.VISIBLE);
                }
                break;
        }
    }

    private Properties defaultConfig() {
        Properties props = new Properties();
        props.setProperty(BrokerConstants.PERSISTENT_STORE_PROPERTY_NAME, context.getExternalFilesDir(null).getAbsolutePath() + File.separator + BrokerConstants.DEFAULT_MOQUETTE_STORE_MAP_DB_FILENAME);
        props.setProperty(BrokerConstants.PORT_PROPERTY_NAME, "1883");
        props.setProperty(BrokerConstants.NEED_CLIENT_AUTH, "false");
        props.setProperty(BrokerConstants.HOST_PROPERTY_NAME, Utils.getBrokerURL(this));
        props.setProperty(BrokerConstants.WEB_SOCKET_PORT_PROPERTY_NAME, String.valueOf(BrokerConstants.WEBSOCKET_PORT));
        return props;
    }

    private void writeToPasswordFile(File passwordFile) {
        try (FileOutputStream fileOutputStream = new FileOutputStream(passwordFile)) {
            fileOutputStream.write(username.getText().toString().getBytes());
            fileOutputStream.write(":".getBytes());
            fileOutputStream.write(Utils.getSHA(password.getText().toString()).getBytes());
            return;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Toast.makeText(this, "Unable to save password", Toast.LENGTH_SHORT).show();
    }

    private Properties saveAndGetConfig() {
        String vPort = port.getText().toString();
        Boolean vAuth = auth.isChecked() ? true : false;

        Properties props = new Properties();
        props.setProperty(BrokerConstants.PERSISTENT_STORE_PROPERTY_NAME, context.getExternalFilesDir(null).getAbsolutePath() + File.separator + BrokerConstants.DEFAULT_MOQUETTE_STORE_MAP_DB_FILENAME);
        props.setProperty(BrokerConstants.PORT_PROPERTY_NAME, vPort);
        props.setProperty(BrokerConstants.HOST_PROPERTY_NAME, Utils.getBrokerURL(this));
        props.setProperty(BrokerConstants.WEB_SOCKET_PORT_PROPERTY_NAME, String.valueOf(BrokerConstants.WEBSOCKET_PORT));
        props.setProperty(BrokerConstants.NEED_CLIENT_AUTH, String.valueOf(vAuth));
        if (vAuth) {
            Log.i("MAIN", "Setting password");
            writeToPasswordFile(passwordFile);
            props.setProperty(BrokerConstants.PASSWORD_FILE_PROPERTY_NAME, passwordFile.getAbsolutePath());
        }

        Log.i("MAIN", props.toString());

        try (OutputStream output = new FileOutputStream(confFile)) {
            props.store(output, "Last saved on " + new Date().toString());
            return props;
        } catch (IOException io) {
            Log.i("MAIN", "Unable to load broker config file. Using default config");
            return defaultConfig();
        }
    }

    private Properties loadConfig() {

        try (InputStream input = new FileInputStream(confFile)) {
            Properties props = new Properties();
            props.load(input);
            updateUI(props);
            return props;
        } catch (FileNotFoundException e) {
            Log.e("MAIN", "Config file not found. Using default config");
        } catch (IOException ex) {
            Log.e("MAIN", "IOException. Using default config");
        }
        Properties props = defaultConfig();
        updateUI(props);
        return props;
    }

    private void updateUI(Properties props) {
        username.setText("");
        password.setText("");

        port.setText(props.getProperty(BrokerConstants.PORT_PROPERTY_NAME));
        host.setText(props.getProperty(BrokerConstants.HOST_PROPERTY_NAME));
        props.setProperty(BrokerConstants.WEB_SOCKET_PORT_PROPERTY_NAME, String.valueOf(BrokerConstants.WEBSOCKET_PORT));
        if (props.getProperty(BrokerConstants.NEED_CLIENT_AUTH) != null && Boolean.valueOf(props.getProperty(BrokerConstants.NEED_CLIENT_AUTH))) {
            auth.setChecked(true);
            authFields.setVisibility(View.VISIBLE);
        } else {
            noAuth.setChecked(true);
        }

    }

    public void startService(View v) {
        startService();
    }

    public void startService() {
        if (mBound == true && mService != null) {
            Log.i("MainActivity", "Service already running");
            return;
        }
        Intent serviceIntent = new Intent(this, MQTTService.class);

        Bundle bundle = new Bundle();
        bundle.putSerializable("config", defaultConfig());
        serviceIntent.putExtras(bundle);

        startService(serviceIntent);
        this.bindService(new Intent(this, MQTTService.class), mConnection, BIND_IMPORTANT);
    }

    public void stopService(View v) {
        Intent serviceIntent = new Intent(this, MQTTService.class);
        stopService(serviceIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            this.unbindService(mConnection);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
