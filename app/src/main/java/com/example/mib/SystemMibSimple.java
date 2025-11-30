package com.example.mib;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.preference.PreferenceManager;

import org.snmp4j.agent.*;
import org.snmp4j.agent.mo.*;
import org.snmp4j.smi.*;

/**
 * Simplified Standard SNMP System MIB implementation for Android devices
 * Uses the same pattern as AndroidDeviceMib for compatibility
 */
public class SystemMibSimple implements MOGroup {

    // Standard System MIB OIDs (1.3.6.1.2.1.1.x.0)
    public static final OID SYS_DESCR = new OID("1.3.6.1.2.1.1.1.0");      
    public static final OID SYS_OBJECT_ID = new OID("1.3.6.1.2.1.1.2.0");  
    public static final OID SYS_UP_TIME = new OID("1.3.6.1.2.1.1.3.0");    
    public static final OID SYS_CONTACT = new OID("1.3.6.1.2.1.1.4.0");    
    public static final OID SYS_NAME = new OID("1.3.6.1.2.1.1.5.0");       
    public static final OID SYS_LOCATION = new OID("1.3.6.1.2.1.1.6.0");   
    public static final OID SYS_SERVICES = new OID("1.3.6.1.2.1.1.7.0");   

    // Our custom enterprise OID for sysObjectID
    private static final OID ANDROID_SNMP_AGENT_OID = new OID("1.3.6.1.4.1.5380.1.16.0.1");

    private Context context;

    // System MIB Managed Objects - using same pattern as AndroidDeviceMib
    private MOScalar sysDescr;
    private MOScalar sysObjectID;
    private MOScalar sysUpTime;
    private MOScalar sysContact;
    private MOScalar sysName;
    private MOScalar sysLocation;
    private MOScalar sysServices;

    public SystemMibSimple(Context context) {
        this.context = context;
        initializeSystemMOs();
    }

    private void initializeSystemMOs() {
        // System Description - dynamic device information that updates on each query
        sysDescr = new MOScalar<OctetString>(SYS_DESCR, MOAccessImpl.ACCESS_READ_ONLY, null) {
            @Override
            public OctetString getValue() {
                return new OctetString(getSystemDescription());
            }
        };
        
        // System Object ID
        sysObjectID = new MOScalar(SYS_OBJECT_ID, MOAccessImpl.ACCESS_READ_ONLY, ANDROID_SNMP_AGENT_OID);
        
        // System Uptime - dynamic uptime that updates on each query
        sysUpTime = new MOScalar<TimeTicks>(SYS_UP_TIME, MOAccessImpl.ACCESS_READ_ONLY, null) {
            @Override
            public TimeTicks getValue() {
                return new TimeTicks(getSystemUptime());
            }
        };
        
        // System Contact - configurable from SharedPreferences, updates on each query
        sysContact = new MOScalar<OctetString>(SYS_CONTACT, MOAccessImpl.ACCESS_READ_WRITE, null) {
            @Override
            public OctetString getValue() {
                return new OctetString(getSystemContact());
            }
        };
        
        // System Name - configurable from SharedPreferences, updates on each query
        sysName = new MOScalar<OctetString>(SYS_NAME, MOAccessImpl.ACCESS_READ_WRITE, null) {
            @Override
            public OctetString getValue() {
                return new OctetString(getSystemName());
            }
        };
        
        // System Location - configurable from SharedPreferences, updates on each query
        sysLocation = new MOScalar<OctetString>(SYS_LOCATION, MOAccessImpl.ACCESS_READ_WRITE, null) {
            @Override
            public OctetString getValue() {
                return new OctetString(getSystemLocation());
            }
        };
        
        // System Services
        sysServices = new MOScalar(SYS_SERVICES, MOAccessImpl.ACCESS_READ_ONLY, new Integer32(64));
    }

    private String getSystemDescription() {
        try {
            StringBuilder description = new StringBuilder();
            
            // Basic device info
            description.append(String.format("Android SNMP Agent - %s %s", 
                Build.MANUFACTURER, Build.MODEL));
            
            // Android version and security patch
            description.append(String.format(" - Android %s (API %d)", 
                Build.VERSION.RELEASE, Build.VERSION.SDK_INT));
            
            // Security patch level (Android 6.0+)
            try {
                if (Build.VERSION.SDK_INT >= 23) {
                    String securityPatch = Build.VERSION.SECURITY_PATCH;
                    if (securityPatch != null && !securityPatch.isEmpty()) {
                        description.append(" - Security: ").append(securityPatch);
                    }
                }
            } catch (Exception e) {
                // Security patch not available
            }
            
            // Network information
            String networkInfo = getNetworkInfo();
            if (!networkInfo.isEmpty()) {
                description.append(" - ").append(networkInfo);
            }
            
            // Battery level
            String batteryInfo = getBatteryInfo();
            if (!batteryInfo.isEmpty()) {
                description.append(" - ").append(batteryInfo);
            }
            
            return description.toString();
        } catch (Exception e) {
            return "Android SNMP Agent - Device Information Unavailable";
        }
    }
    
    private String getNetworkInfo() {
        try {
            android.net.ConnectivityManager connectivityManager = 
                (android.net.ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            
            if (connectivityManager == null) {
                return "";
            }
            
            StringBuilder networkInfo = new StringBuilder();
            
            // Get active network info
            android.net.NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            if (activeNetwork != null && activeNetwork.isConnected()) {
                String networkType = activeNetwork.getTypeName();
                
                if ("WIFI".equalsIgnoreCase(networkType)) {
                    // WiFi information
                    try {
                        android.net.wifi.WifiManager wifiManager = 
                            (android.net.wifi.WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                        
                        if (wifiManager != null) {
                            android.net.wifi.WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                            if (wifiInfo != null) {
                                String ssid = wifiInfo.getSSID();
                                
                                if (ssid != null && !ssid.equals("<unknown ssid>") && !ssid.equals("\"<unknown ssid>\"")) {
                                    ssid = ssid.replace("\"", ""); // Remove quotes
                                    networkInfo.append("WiFi: ").append(ssid);
                                    
                                    int rssi = wifiInfo.getRssi();
                                    int signalLevel = android.net.wifi.WifiManager.calculateSignalLevel(rssi, 5);
                                    networkInfo.append(" (Signal: ").append(signalLevel).append("/4)");
                                } else {
                                    networkInfo.append("WiFi: Connected (SSID hidden)");
                                }
                            } else {
                                networkInfo.append("WiFi: Connected (Info unavailable)");
                            }
                        } else {
                            networkInfo.append("WiFi: Connected (Manager unavailable)");
                        }
                    } catch (SecurityException e) {
                        networkInfo.append("WiFi: Connected (Permission denied)");
                    } catch (Exception e) {
                        networkInfo.append("WiFi: Connected (Error)");
                    }
                } else if ("MOBILE".equalsIgnoreCase(networkType)) {
                    // Cellular information
                    try {
                        android.telephony.TelephonyManager telephonyManager = 
                            (android.telephony.TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                        
                        if (telephonyManager != null) {
                            networkInfo.append("Cellular: ");
                            
                            // Network operator name
                            String operatorName = telephonyManager.getNetworkOperatorName();
                            if (operatorName != null && !operatorName.isEmpty()) {
                                networkInfo.append(operatorName);
                            } else {
                                networkInfo.append("Mobile");
                            }
                            
                            // Network type (LTE, 3G, etc.)
                            try {
                                if (Build.VERSION.SDK_INT >= 24) {
                                    int dataNetworkType = telephonyManager.getDataNetworkType();
                                    String networkTypeStr = getNetworkTypeString(dataNetworkType);
                                    if (!networkTypeStr.isEmpty()) {
                                        networkInfo.append(" (").append(networkTypeStr).append(")");
                                    }
                                }
                            } catch (Exception e) {
                                // Network type not available
                            }
                        } else {
                            networkInfo.append("Cellular: Service unavailable");
                        }
                    } catch (SecurityException e) {
                        networkInfo.append("Cellular: Permission denied");
                    } catch (Exception e) {
                        networkInfo.append("Cellular: Error");
                    }
                } else {
                    networkInfo.append("Network: ").append(networkType);
                }
            } else {
                return "";
            }
            
            return networkInfo.toString();
        } catch (Exception e) {
            return "";
        }
    }
    
    private String getNetworkTypeString(int networkType) {
        switch (networkType) {
            case android.telephony.TelephonyManager.NETWORK_TYPE_LTE:
                return "LTE";
            case android.telephony.TelephonyManager.NETWORK_TYPE_HSPAP:
            case android.telephony.TelephonyManager.NETWORK_TYPE_HSPA:
            case android.telephony.TelephonyManager.NETWORK_TYPE_HSDPA:
            case android.telephony.TelephonyManager.NETWORK_TYPE_HSUPA:
                return "HSPA";
            case android.telephony.TelephonyManager.NETWORK_TYPE_UMTS:
                return "3G";
            case android.telephony.TelephonyManager.NETWORK_TYPE_EDGE:
            case android.telephony.TelephonyManager.NETWORK_TYPE_GPRS:
                return "2G";
            case 20: // NETWORK_TYPE_NR (5G) - API 29+
                return "5G";
            default:
                return "";
        }
    }
    
    private String getBatteryInfo() {
        try {
            android.os.BatteryManager batteryManager = 
                (android.os.BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            
            if (batteryManager != null) {
                int batteryLevel = batteryManager.getIntProperty(
                    android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY);
                
                if (batteryLevel > 0) {
                    return "Battery: " + batteryLevel + "%";
                }
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    private long getSystemUptime() {
        try {
            // Android uptime in milliseconds, convert to hundredths of seconds for SNMP
            return android.os.SystemClock.elapsedRealtime() / 10;
        } catch (Exception e) {
            return 0;
        }
    }

    private String getSystemContact() {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            return prefs.getString("system_contact", "IT Support (contact@company.com)");
        } catch (Exception e) {
            return "IT Support";
        }
    }

    private String getSystemName() {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            
            // Create a default name with device model and partial serial
            String defaultName;
            try {
                String serial = Build.SERIAL;
                if (serial != null && !serial.equals("unknown") && serial.length() > 0) {
                    // Use last 6 characters of serial for uniqueness
                    String shortSerial = serial.length() > 6 ? 
                        serial.substring(serial.length() - 6) : serial;
                    defaultName = Build.MODEL.replaceAll("\\s+", "-") + "-" + shortSerial;
                } else {
                    // Fallback to model + Android ID partial
                    String androidId = android.provider.Settings.Secure.getString(
                        context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
                    if (androidId != null && androidId.length() > 6) {
                        defaultName = Build.MODEL.replaceAll("\\s+", "-") + "-" + androidId.substring(0, 6);
                    } else {
                        defaultName = Build.MODEL.replaceAll("\\s+", "-") + "-Device";
                    }
                }
            } catch (Exception e) {
                defaultName = "Android-Device";
            }
            
            return prefs.getString("system_name", defaultName);
        } catch (Exception e) {
            return "Android-Device";
        }
    }

    private String getSystemLocation() {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            return prefs.getString("system_location", "Location not configured");
        } catch (Exception e) {
            return "Unknown Location";
        }
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
}
