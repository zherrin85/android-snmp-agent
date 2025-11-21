package com.example.snmpagent;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

public class MainActivity extends AppCompatActivity {
    
    private EditText portEditText;
    private EditText getCommunityEditText;
    private EditText setCommunityEditText;
    private EditText trapCommunityEditText;
    private EditText trapDestinationEditText;
    private Switch serviceSwitch;
    private Switch autoStartSwitch;
    private Button saveButton;
    
    private SharedPreferences preferences;
    private SnmpAgentService.ServiceBinder serviceBinder;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        
        // Initialize views
        portEditText = findViewById(R.id.portEditText);
        getCommunityEditText = findViewById(R.id.getCommunityEditText);
        setCommunityEditText = findViewById(R.id.setCommunityEditText);
        trapCommunityEditText = findViewById(R.id.trapCommunityEditText);
        trapDestinationEditText = findViewById(R.id.trapDestinationEditText);
        serviceSwitch = findViewById(R.id.serviceSwitch);
        autoStartSwitch = findViewById(R.id.autoStartSwitch);
        saveButton = findViewById(R.id.saveButton);
        
        // Load saved preferences
        loadPreferences();
        
        // Set up save button
        saveButton.setOnClickListener(v -> savePreferences());
        
        // Add long click to show network info
        saveButton.setOnLongClickListener(v -> {
            showNetworkInfo();
            return true;
        });
        
        // Set up service switch
        serviceSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                startService();
            } else {
                stopService();
            }
        });
        
        // Check if service is running
        updateServiceStatus();
    }
    
    private void loadPreferences() {
        portEditText.setText(String.valueOf(preferences.getInt("port", 1161)));
        portEditText.setHint("1161");
        getCommunityEditText.setText(preferences.getString("get_community", "blackjack"));
        getCommunityEditText.setHint("blackjack");
        setCommunityEditText.setText(preferences.getString("set_community", "blackjack007"));
        setCommunityEditText.setHint("blackjack007");
        trapCommunityEditText.setText(preferences.getString("trap_community", "blackjack"));
        trapCommunityEditText.setHint("blackjack");
        trapDestinationEditText.setText(preferences.getString("trap_destination", "192.168.1.100"));
        trapDestinationEditText.setHint("192.168.1.100");
        autoStartSwitch.setChecked(preferences.getBoolean("auto_start", true));
    }
    
    private void savePreferences() {
        try {
            int port = Integer.parseInt(portEditText.getText().toString());
            if (port < 1 || port > 65535) {
                Toast.makeText(this, "Port must be between 1 and 65535", Toast.LENGTH_SHORT).show();
                return;
            }
            
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt("port", port);
            editor.putString("get_community", getCommunityEditText.getText().toString());
            editor.putString("set_community", setCommunityEditText.getText().toString());
            editor.putString("trap_community", trapCommunityEditText.getText().toString());
            editor.putString("trap_destination", trapDestinationEditText.getText().toString());
            editor.putBoolean("auto_start", autoStartSwitch.isChecked());
            editor.apply();
            
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
            
            // Restart service if running
            if (serviceSwitch.isChecked()) {
                stopService();
                startService();
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid port number", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void startService() {
        Intent serviceIntent = new Intent(this, SnmpAgentService.class);
        startForegroundService(serviceIntent);
        serviceSwitch.setChecked(true);
        Toast.makeText(this, "SNMP Agent started", Toast.LENGTH_SHORT).show();
    }
    
    private void stopService() {
        Intent serviceIntent = new Intent(this, SnmpAgentService.class);
        stopService(serviceIntent);
        serviceSwitch.setChecked(false);
        Toast.makeText(this, "SNMP Agent stopped", Toast.LENGTH_SHORT).show();
    }
    
    private void updateServiceStatus() {
        // Check if service is running
        boolean isRunning = SnmpAgentService.isRunning();
        serviceSwitch.setChecked(isRunning);
    }
    
    private void showNetworkInfo() {
        String networkInfo = NetworkUtils.getNetworkInfo();
        String localIp = NetworkUtils.getLocalIpAddress();
        int port = preferences.getInt("port", 1161);
        
        String message = "Local IP: " + (localIp != null ? localIp : "Not found") + 
                        "\nPort: " + port +
                        "\n\nTest command:\nsnmpget -v2c -c blackjack " + 
                        (localIp != null ? localIp : "<device-ip>") + ":" + port + 
                        " 1.3.6.1.4.1.5380.1.16.1.1.0" +
                        "\n\nNetwork Interfaces:\n" + networkInfo;
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Network Information")
               .setMessage(message)
               .setPositiveButton("OK", null)
               .show();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateServiceStatus();
    }
}

