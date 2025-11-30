package com.example.mib;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.preference.PreferenceManager;

import org.snmp4j.agent.*;
import org.snmp4j.agent.mo.*;
import org.snmp4j.smi.*;

import java.io.BufferedReader;
import java.io.FileReader;

/**
 * Standard SNMP System MIB implementation for Android devices
 * Implements the standard system MIB entries that SolarWinds and other SNMP managers expect
 */
public class SystemMib implements MOGroup {
    
    // Standard System MIB OIDs (1.3.6.1.2.1.1.x.0)
    public static final OID SYS_DESCR = new OID("1.3.6.1.2.1.1.1.0");      // System Description
    public static final OID SYS_OBJECT_ID = new OID("1.3.6.1.2.1.1.2.0");  // System Object ID
    public static final OID SYS_UP_TIME = new OID("1.3.6.1.2.1.1.3.0");    // System Uptime
    public static final OID SYS_CONTACT = new OID("1.3.6.1.2.1.1.4.0");    // System Contact
    public static final OID SYS_NAME = new OID("1.3.6.1.2.1.1.5.0");       // System Name
    public static final OID SYS_LOCATION = new OID("1.3.6.1.2.1.1.6.0");   // System Location
    public static final OID SYS_SERVICES = new OID("1.3.6.1.2.1.1.7.0");   // System Services
    
    // Our custom enterprise OID for sysObjectID
    private static final OID ANDROID_SNMP_AGENT_OID = new OID("1.3.6.1.4.1.5380.1.16.0.1");
    
    private Context context;
    
    // System MIB Managed Objects
    private MOScalar<OctetString> sysDescr;
    private MOScalar<OID> sysObjectID;
    private MOScalar<TimeTicks> sysUpTime;
    private MOScalar<OctetString> sysContact;
    private MOScalar<OctetString> sysName;
    private MOScalar<OctetString> sysLocation;
    private MOScalar<Integer32> sysServices;
    
    public SystemMib(Context context) {
        this.context = context;
        initializeSystemMOs();
    }
    
    private void initializeSystemMOs() {
        // System Description - detailed device information
        sysDescr = new MOScalar<OctetString>(SYS_DESCR, MOAccessImpl.ACCESS_READ_ONLY, 
            new OctetString(getSystemDescription())) {
            @Override
            public OctetString getValue() {
                return new OctetString(getSystemDescription());
            }
        };
        
        // System Object ID - identifies this as our Android SNMP agent
        sysObjectID = new MOScalar<OID>(SYS_OBJECT_ID, MOAccessImpl.ACCESS_READ_ONLY, 
            ANDROID_SNMP_AGENT_OID) {
            @Override
            public OID getValue() {
                return ANDROID_SNMP_AGENT_OID;
            }
        };
        
        // System Uptime - time since last boot in hundredths of seconds
        sysUpTime = new MOScalar<TimeTicks>(SYS_UP_TIME, MOAccessImpl.ACCESS_READ_ONLY, 
            new TimeTicks(getSystemUptime())) {
            @Override
            public TimeTicks getValue() {
                return new TimeTicks(getSystemUptime());
            }
        };
        
        // System Contact - configurable contact information
        sysContact = new MOScalar<OctetString>(SYS_CONTACT, MOAccessImpl.ACCESS_READ_WRITE, 
            new OctetString(getSystemContact())) {
            @Override
            public OctetString getValue() {
                return new OctetString(getSystemContact());
            }
            
            @Override
            public int setValue(OctetString value) {
                setSystemContact(value.toString());
                return super.setValue(value);
            }
        };
        
        // System Name - configurable device name
        sysName = new MOScalar<OctetString>(SYS_NAME, MOAccessImpl.ACCESS_READ_WRITE, 
            new OctetString(getSystemName())) {
            @Override
            public OctetString getValue() {
                return new OctetString(getSystemName());
            }
            
            @Override
            public int setValue(OctetString value) {
                setSystemName(value.toString());
                return super.setValue(value);
            }
        };
        
        // System Location - configurable location information
        sysLocation = new MOScalar<OctetString>(SYS_LOCATION, MOAccessImpl.ACCESS_READ_WRITE, 
            new OctetString(getSystemLocation())) {
            @Override
            public OctetString getValue() {
                return new OctetString(getSystemLocation());
            }
            
            @Override
            public int setValue(OctetString value) {
                setSystemLocation(value.toString());
                return super.setValue(value);
            }
        };
        
        // System Services - indicates what services this device provides
        // Bit 0: Physical layer (repeaters, hubs)
        // Bit 1: Datalink/subnetwork layer (bridges, switches) 
        // Bit 2: Internet layer (routers)
        // Bit 3: End-to-end layer (hosts)
        // Bit 6: Application layer (application gateways)
        // For an Android device, we set bit 6 (64) for application layer services
        sysServices = new MOScalar<Integer32>(SYS_SERVICES, MOAccessImpl.ACCESS_READ_ONLY, 
            new Integer32(64)) {
            @Override
            public Integer32 getValue() {
                return new Integer32(64); // Application layer services
            }
        };
    }
    
    @Override
    public void registerMOs(MOServer server, OctetString context) throws DuplicateRegistrationException {
        server.register(sysDescr, context);
        server.register(sysObjectID, context);
        server.register(sysUpTime, context);
        server.register(sysContact, context);
        server.register(sysName, context);
        server.register(sysLocation, context);
        server.register(sysServices, context);
    }
    
    @Override
    public void unregisterMOs(MOServer server, OctetString context) {
        server.unregister(sysDescr, context);
        server.unregister(sysObjectID, context);
        server.unregister(sysUpTime, context);
        server.unregister(sysContact, context);
        server.unregister(sysName, context);
        server.unregister(sysLocation, context);
        server.unregister(sysServices, context);
    }
    
    // Helper methods to get system information
    
    private String getSystemDescription() {
        try {
            // Get battery level for inclusion in description
            String batteryInfo = "";
            try {
                android.os.BatteryManager batteryManager = (android.os.BatteryManager) 
                    context.getSystemService(Context.BATTERY_SERVICE);
                int batteryLevel = batteryManager.getIntProperty(
                    android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY);
                if (batteryLevel > 0) {
                    batteryInfo = " - Battery: " + batteryLevel + "%";
                }
            } catch (Exception e) {
                // Battery info not available
            }
            
            // Get network type
            String networkInfo = "";
            try {
                android.net.ConnectivityManager connectivityManager = 
                    (android.net.ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                android.net.NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
                if (activeNetwork != null) {
                    networkInfo = " - Network: " + activeNetwork.getTypeName();
                }
            } catch (Exception e) {
                // Network info not available
            }
            
            return String.format("Android SNMP Agent - %s %s - Android %s (API %d)%s%s",
                Build.MANUFACTURER,
                Build.MODEL,
                Build.VERSION.RELEASE,
                Build.VERSION.SDK_INT,
                batteryInfo,
                networkInfo
            );
        } catch (Exception e) {
            return "Android SNMP Agent - Device Information Unavailable";
        }
    }
    
    private long getSystemUptime() {
        try {
            // Read uptime from /proc/uptime and convert to hundredths of seconds (TimeTicks)
            BufferedReader reader = new BufferedReader(new FileReader("/proc/uptime"));
            String line = reader.readLine();
            reader.close();
            if (line != null) {
                String[] tokens = line.split("\\s+");
                double uptimeSeconds = Double.parseDouble(tokens[0]);
                return (long) (uptimeSeconds * 100); // Convert to hundredths of seconds
            }
        } catch (Exception e) {
            // Fallback to system uptime
            return System.currentTimeMillis() / 10; // Rough approximation
        }
        return 0;
    }
    
    private String getSystemContact() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("system_contact", "IT Support (support@company.com)");
    }
    
    private void setSystemContact(String contact) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString("system_contact", contact).apply();
    }
    
    private String getSystemName() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String defaultName = Build.MODEL.replaceAll("\\s+", "-") + "-" + 
                           android.provider.Settings.Secure.getString(context.getContentResolver(), 
                           android.provider.Settings.Secure.ANDROID_ID).substring(0, 8).toUpperCase();
        return prefs.getString("system_name", defaultName);
    }
    
    private void setSystemName(String name) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString("system_name", name).apply();
    }
    
    private String getSystemLocation() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("system_location", "Unknown Location");
    }
    
    private void setSystemLocation(String location) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString("system_location", location).apply();
    }
}
