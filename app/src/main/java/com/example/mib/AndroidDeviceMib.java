package com.example.mib;

import android.app.ActivityManager;
import android.content.Context;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.telephony.TelephonyManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;

import org.snmp4j.agent.*;
import org.snmp4j.agent.mo.*;
import org.snmp4j.smi.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class AndroidDeviceMib implements MOGroup {
    
    // Base OID for Android Device MIB: 1.3.6.1.4.1.5380.1.16
    private static final OID BASE_OID = new OID(new int[]{1, 3, 6, 1, 4, 1, 5380, 1, 16});
    
    // System Information OIDs (1.3.6.1.4.1.5380.1.16.10.x) - Changed from 1.x to 10.x to avoid conflict
    public static final OID DEVICE_MODEL = new OID(BASE_OID).append("10.1.0");
    public static final OID DEVICE_MANUFACTURER = new OID(BASE_OID).append("10.2.0");
    public static final OID ANDROID_VERSION = new OID(BASE_OID).append("10.3.0");
    public static final OID API_LEVEL = new OID(BASE_OID).append("10.4.0");
    public static final OID DEVICE_SERIAL = new OID(BASE_OID).append("10.5.0");
    
    // Memory Information OIDs (1.3.6.1.4.1.5380.1.16.2.x)
    public static final OID TOTAL_MEMORY = new OID(BASE_OID).append("2.1.0");
    public static final OID AVAILABLE_MEMORY = new OID(BASE_OID).append("2.2.0");
    public static final OID USED_MEMORY = new OID(BASE_OID).append("2.3.0");
    public static final OID MEMORY_USAGE_PERCENT = new OID(BASE_OID).append("2.4.0");
    
    // Storage Information OIDs (1.3.6.1.4.1.5380.1.16.3.x)
    public static final OID TOTAL_STORAGE = new OID(BASE_OID).append("3.1.0");
    public static final OID AVAILABLE_STORAGE = new OID(BASE_OID).append("3.2.0");
    public static final OID USED_STORAGE = new OID(BASE_OID).append("3.3.0");
    public static final OID STORAGE_USAGE_PERCENT = new OID(BASE_OID).append("3.4.0");
    
    // Battery Information OIDs (1.3.6.1.4.1.5380.1.16.4.x)
    public static final OID BATTERY_LEVEL = new OID(BASE_OID).append("4.1.0");
    public static final OID BATTERY_TEMPERATURE = new OID(BASE_OID).append("4.2.0");
    public static final OID BATTERY_VOLTAGE = new OID(BASE_OID).append("4.3.0");
    public static final OID BATTERY_STATUS = new OID(BASE_OID).append("4.4.0");
    
    // Network Information OIDs (1.3.6.1.4.1.5380.1.16.5.x)
    public static final OID NETWORK_TYPE = new OID(BASE_OID).append("5.1.0");
    public static final OID WIFI_SSID = new OID(BASE_OID).append("5.2.0");
    public static final OID WIFI_SIGNAL_STRENGTH = new OID(BASE_OID).append("5.3.0");
    public static final OID IP_ADDRESS = new OID(BASE_OID).append("5.4.0");
    public static final OID MAC_ADDRESS = new OID(BASE_OID).append("5.5.0");
    
    // CPU Information OIDs (1.3.6.1.4.1.5380.1.16.6.x)
    public static final OID CPU_CORES = new OID(BASE_OID).append("6.1.0");
    public static final OID CPU_USAGE = new OID(BASE_OID).append("6.2.0");
    public static final OID CPU_FREQUENCY = new OID(BASE_OID).append("6.3.0");
    
    // Application Information OIDs (1.3.6.1.4.1.5380.1.16.7.x)
    public static final OID RUNNING_PROCESSES = new OID(BASE_OID).append("7.1.0");
    public static final OID UPTIME = new OID(BASE_OID).append("7.2.0");
    
    private Context context;
    
    // System Information MOs
    private MOScalar<OctetString> deviceModel;
    private MOScalar<OctetString> deviceManufacturer;
    private MOScalar<OctetString> androidVersion;
    private MOScalar<Integer32> apiLevel;
    private MOScalar<OctetString> deviceSerial;
    
    // Memory Information MOs
    private MOScalar<Counter64> totalMemory;
    private MOScalar<Counter64> availableMemory;
    private MOScalar<Counter64> usedMemory;
    private MOScalar<Integer32> memoryUsagePercent;
    
    // Storage Information MOs
    private MOScalar<Counter64> totalStorage;
    private MOScalar<Counter64> availableStorage;
    private MOScalar<Counter64> usedStorage;
    private MOScalar<Integer32> storageUsagePercent;
    
    // Battery Information MOs
    private MOScalar<Integer32> batteryLevel;
    private MOScalar<Integer32> batteryTemperature;
    private MOScalar<Integer32> batteryVoltage;
    private MOScalar<OctetString> batteryStatus;
    
    // Network Information MOs
    private MOScalar<OctetString> networkType;
    private MOScalar<OctetString> wifiSsid;
    private MOScalar<Integer32> wifiSignalStrength;
    private MOScalar<OctetString> ipAddress;
    private MOScalar<OctetString> macAddress;
    
    // CPU Information MOs
    private MOScalar<Integer32> cpuCores;
    private MOScalar<Integer32> cpuUsage;
    private MOScalar<Integer32> cpuFrequency;
    
    // Application Information MOs
    private MOScalar<Integer32> runningProcesses;
    private MOScalar<Counter64> uptime;

    public AndroidDeviceMib(Context context) {
        this.context = context;
        initializeMOs();
    }

    private void initializeMOs() {
        // System Information MOs
        deviceModel = new MOScalar<OctetString>(DEVICE_MODEL, MOAccessImpl.ACCESS_READ_ONLY, 
            new OctetString(Build.MODEL)) {
            @Override
            public OctetString getValue() {
                return new OctetString(Build.MODEL);
            }
        };
        
        deviceManufacturer = new MOScalar<OctetString>(DEVICE_MANUFACTURER, MOAccessImpl.ACCESS_READ_ONLY, 
            new OctetString(Build.MANUFACTURER)) {
            @Override
            public OctetString getValue() {
                return new OctetString(Build.MANUFACTURER);
            }
        };
        
        androidVersion = new MOScalar<OctetString>(ANDROID_VERSION, MOAccessImpl.ACCESS_READ_ONLY, 
            new OctetString(Build.VERSION.RELEASE)) {
            @Override
            public OctetString getValue() {
                return new OctetString(Build.VERSION.RELEASE);
            }
        };
        
        apiLevel = new MOScalar<Integer32>(API_LEVEL, MOAccessImpl.ACCESS_READ_ONLY, 
            new Integer32(Build.VERSION.SDK_INT)) {
            @Override
            public Integer32 getValue() {
                return new Integer32(Build.VERSION.SDK_INT);
            }
        };
        
        deviceSerial = new MOScalar<OctetString>(DEVICE_SERIAL, MOAccessImpl.ACCESS_READ_ONLY, 
            new OctetString(getDeviceSerial())) {
            @Override
            public OctetString getValue() {
                return new OctetString(getDeviceSerial());
            }
        };
        
        // Memory Information MOs
        totalMemory = new MOScalar<Counter64>(TOTAL_MEMORY, MOAccessImpl.ACCESS_READ_ONLY, 
            new Counter64(getTotalMemory())) {
            @Override
            public Counter64 getValue() {
                return new Counter64(getTotalMemory());
            }
        };
        
        availableMemory = new MOScalar<Counter64>(AVAILABLE_MEMORY, MOAccessImpl.ACCESS_READ_ONLY, 
            new Counter64(getAvailableMemory())) {
            @Override
            public Counter64 getValue() {
                return new Counter64(getAvailableMemory());
            }
        };
        
        usedMemory = new MOScalar<Counter64>(USED_MEMORY, MOAccessImpl.ACCESS_READ_ONLY, 
            new Counter64(getUsedMemory())) {
            @Override
            public Counter64 getValue() {
                return new Counter64(getUsedMemory());
            }
        };
        
        memoryUsagePercent = new MOScalar<Integer32>(MEMORY_USAGE_PERCENT, MOAccessImpl.ACCESS_READ_ONLY, 
            new Integer32(getMemoryUsagePercent())) {
            @Override
            public Integer32 getValue() {
                return new Integer32(getMemoryUsagePercent());
            }
        };
        
        // Storage Information MOs
        totalStorage = new MOScalar<Counter64>(TOTAL_STORAGE, MOAccessImpl.ACCESS_READ_ONLY, 
            new Counter64(getTotalStorage())) {
            @Override
            public Counter64 getValue() {
                return new Counter64(getTotalStorage());
            }
        };
        
        availableStorage = new MOScalar<Counter64>(AVAILABLE_STORAGE, MOAccessImpl.ACCESS_READ_ONLY, 
            new Counter64(getAvailableStorage())) {
            @Override
            public Counter64 getValue() {
                return new Counter64(getAvailableStorage());
            }
        };
        
        usedStorage = new MOScalar<Counter64>(USED_STORAGE, MOAccessImpl.ACCESS_READ_ONLY, 
            new Counter64(getUsedStorage())) {
            @Override
            public Counter64 getValue() {
                return new Counter64(getUsedStorage());
            }
        };
        
        storageUsagePercent = new MOScalar<Integer32>(STORAGE_USAGE_PERCENT, MOAccessImpl.ACCESS_READ_ONLY, 
            new Integer32(getStorageUsagePercent())) {
            @Override
            public Integer32 getValue() {
                return new Integer32(getStorageUsagePercent());
            }
        };
        
        // Battery Information MOs
        batteryLevel = new MOScalar<Integer32>(BATTERY_LEVEL, MOAccessImpl.ACCESS_READ_ONLY, 
            new Integer32(getBatteryLevel())) {
            @Override
            public Integer32 getValue() {
                return new Integer32(getBatteryLevel());
            }
        };
        
        batteryTemperature = new MOScalar<Integer32>(BATTERY_TEMPERATURE, MOAccessImpl.ACCESS_READ_ONLY, 
            new Integer32(getBatteryTemperature())) {
            @Override
            public Integer32 getValue() {
                return new Integer32(getBatteryTemperature());
            }
        };
        
        batteryVoltage = new MOScalar<Integer32>(BATTERY_VOLTAGE, MOAccessImpl.ACCESS_READ_ONLY, 
            new Integer32(getBatteryVoltage())) {
            @Override
            public Integer32 getValue() {
                return new Integer32(getBatteryVoltage());
            }
        };
        
        batteryStatus = new MOScalar<OctetString>(BATTERY_STATUS, MOAccessImpl.ACCESS_READ_ONLY, 
            new OctetString(getBatteryStatus())) {
            @Override
            public OctetString getValue() {
                return new OctetString(getBatteryStatus());
            }
        };
        
        // Network Information MOs
        networkType = new MOScalar<OctetString>(NETWORK_TYPE, MOAccessImpl.ACCESS_READ_ONLY, 
            new OctetString(getNetworkType())) {
            @Override
            public OctetString getValue() {
                return new OctetString(getNetworkType());
            }
        };
        
        wifiSsid = new MOScalar<OctetString>(WIFI_SSID, MOAccessImpl.ACCESS_READ_ONLY, 
            new OctetString(getWifiSsid())) {
            @Override
            public OctetString getValue() {
                return new OctetString(getWifiSsid());
            }
        };
        
        wifiSignalStrength = new MOScalar<Integer32>(WIFI_SIGNAL_STRENGTH, MOAccessImpl.ACCESS_READ_ONLY, 
            new Integer32(getWifiSignalStrength())) {
            @Override
            public Integer32 getValue() {
                return new Integer32(getWifiSignalStrength());
            }
        };
        
        ipAddress = new MOScalar<OctetString>(IP_ADDRESS, MOAccessImpl.ACCESS_READ_ONLY, 
            new OctetString(getIpAddress())) {
            @Override
            public OctetString getValue() {
                return new OctetString(getIpAddress());
            }
        };
        
        macAddress = new MOScalar<OctetString>(MAC_ADDRESS, MOAccessImpl.ACCESS_READ_ONLY, 
            new OctetString(getMacAddress())) {
            @Override
            public OctetString getValue() {
                return new OctetString(getMacAddress());
            }
        };
        
        // CPU Information MOs
        cpuCores = new MOScalar<Integer32>(CPU_CORES, MOAccessImpl.ACCESS_READ_ONLY, 
            new Integer32(getCpuCores())) {
            @Override
            public Integer32 getValue() {
                return new Integer32(getCpuCores());
            }
        };
        
        cpuUsage = new MOScalar<Integer32>(CPU_USAGE, MOAccessImpl.ACCESS_READ_ONLY, 
            new Integer32(getCpuUsage())) {
            @Override
            public Integer32 getValue() {
                return new Integer32(getCpuUsage());
            }
        };
        
        cpuFrequency = new MOScalar<Integer32>(CPU_FREQUENCY, MOAccessImpl.ACCESS_READ_ONLY, 
            new Integer32(getCpuFrequency())) {
            @Override
            public Integer32 getValue() {
                return new Integer32(getCpuFrequency());
            }
        };
        
        // Application Information MOs
        runningProcesses = new MOScalar<Integer32>(RUNNING_PROCESSES, MOAccessImpl.ACCESS_READ_ONLY, 
            new Integer32(getRunningProcesses())) {
            @Override
            public Integer32 getValue() {
                return new Integer32(getRunningProcesses());
            }
        };
        
        uptime = new MOScalar<Counter64>(UPTIME, MOAccessImpl.ACCESS_READ_ONLY, 
            new Counter64(getUptime())) {
            @Override
            public Counter64 getValue() {
                return new Counter64(getUptime());
            }
        };
    }

    @Override
    public void registerMOs(MOServer server, OctetString context) throws DuplicateRegistrationException {
        // System Information
        server.register(deviceModel, context);
        server.register(deviceManufacturer, context);
        server.register(androidVersion, context);
        server.register(apiLevel, context);
        server.register(deviceSerial, context);
        
        // Memory Information
        server.register(totalMemory, context);
        server.register(availableMemory, context);
        server.register(usedMemory, context);
        server.register(memoryUsagePercent, context);
        
        // Storage Information
        server.register(totalStorage, context);
        server.register(availableStorage, context);
        server.register(usedStorage, context);
        server.register(storageUsagePercent, context);
        
        // Battery Information
        server.register(batteryLevel, context);
        server.register(batteryTemperature, context);
        server.register(batteryVoltage, context);
        server.register(batteryStatus, context);
        
        // Network Information
        server.register(networkType, context);
        server.register(wifiSsid, context);
        server.register(wifiSignalStrength, context);
        server.register(ipAddress, context);
        server.register(macAddress, context);
        
        // CPU Information
        server.register(cpuCores, context);
        server.register(cpuUsage, context);
        server.register(cpuFrequency, context);
        
        // Application Information
        server.register(runningProcesses, context);
        server.register(uptime, context);
    }

    @Override
    public void unregisterMOs(MOServer server, OctetString context) {
        // System Information
        server.unregister(deviceModel, context);
        server.unregister(deviceManufacturer, context);
        server.unregister(androidVersion, context);
        server.unregister(apiLevel, context);
        server.unregister(deviceSerial, context);
        
        // Memory Information
        server.unregister(totalMemory, context);
        server.unregister(availableMemory, context);
        server.unregister(usedMemory, context);
        server.unregister(memoryUsagePercent, context);
        
        // Storage Information
        server.unregister(totalStorage, context);
        server.unregister(availableStorage, context);
        server.unregister(usedStorage, context);
        server.unregister(storageUsagePercent, context);
        
        // Battery Information
        server.unregister(batteryLevel, context);
        server.unregister(batteryTemperature, context);
        server.unregister(batteryVoltage, context);
        server.unregister(batteryStatus, context);
        
        // Network Information
        server.unregister(networkType, context);
        server.unregister(wifiSsid, context);
        server.unregister(wifiSignalStrength, context);
        server.unregister(ipAddress, context);
        server.unregister(macAddress, context);
        
        // CPU Information
        server.unregister(cpuCores, context);
        server.unregister(cpuUsage, context);
        server.unregister(cpuFrequency, context);
        
        // Application Information
        server.unregister(runningProcesses, context);
        server.unregister(uptime, context);
    }

    // Helper methods to get real device statistics
    
    private String getDeviceSerial() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return Build.getSerial();
            } else {
                return Build.SERIAL;
            }
        } catch (SecurityException e) {
            return "UNKNOWN";
        }
    }
    
    private long getTotalMemory() {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        return memoryInfo.totalMem;
    }
    
    private long getAvailableMemory() {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        return memoryInfo.availMem;
    }
    
    private long getUsedMemory() {
        return getTotalMemory() - getAvailableMemory();
    }
    
    private int getMemoryUsagePercent() {
        long total = getTotalMemory();
        if (total == 0) return 0;
        return (int) ((getUsedMemory() * 100) / total);
    }
    
    private long getTotalStorage() {
        StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
        return stat.getBlockCountLong() * stat.getBlockSizeLong();
    }
    
    private long getAvailableStorage() {
        StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
        return stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
    }
    
    private long getUsedStorage() {
        return getTotalStorage() - getAvailableStorage();
    }
    
    private int getStorageUsagePercent() {
        long total = getTotalStorage();
        if (total == 0) return 0;
        return (int) ((getUsedStorage() * 100) / total);
    }
    
    private int getBatteryLevel() {
        try {
            BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        } catch (Exception e) {
            return -1;
        }
    }
    
    private int getBatteryTemperature() {
        try {
            BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
        } catch (Exception e) {
            return -1;
        }
    }
    
    private int getBatteryVoltage() {
        try {
            BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE);
        } catch (Exception e) {
            return -1;
        }
    }
    
    private String getBatteryStatus() {
        try {
            BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            int status = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS);
            switch (status) {
                case BatteryManager.BATTERY_STATUS_CHARGING: return "CHARGING";
                case BatteryManager.BATTERY_STATUS_DISCHARGING: return "DISCHARGING";
                case BatteryManager.BATTERY_STATUS_FULL: return "FULL";
                case BatteryManager.BATTERY_STATUS_NOT_CHARGING: return "NOT_CHARGING";
                default: return "UNKNOWN";
            }
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
    
    private String getNetworkType() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            if (activeNetwork != null) {
                return activeNetwork.getTypeName();
            }
            return "NONE";
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
    
    private String getWifiSsid() {
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            String ssid = wifiInfo.getSSID();
            return ssid != null ? ssid.replace("\"", "") : "NONE";
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
    
    private int getWifiSignalStrength() {
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            return wifiInfo.getRssi();
        } catch (Exception e) {
            return -999;
        }
    }
    
    private String getIpAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress() && !addr.isLinkLocalAddress()) {
                        String sAddr = addr.getHostAddress();
                        if (sAddr.indexOf(':') < 0) { // IPv4
                            return sAddr;
                        }
                    }
                }
            }
            return "UNKNOWN";
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
    
    private String getMacAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (intf.getName().equalsIgnoreCase("wlan0")) {
                    byte[] mac = intf.getHardwareAddress();
                    if (mac != null) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < mac.length; i++) {
                            sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? ":" : ""));
                        }
                        return sb.toString();
                    }
                }
            }
            return "UNKNOWN";
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
    
    private int getCpuCores() {
        return Runtime.getRuntime().availableProcessors();
    }
    
    private int getCpuUsage() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/stat"));
            String line = reader.readLine();
            reader.close();
            if (line != null && line.startsWith("cpu ")) {
                String[] tokens = line.split("\\s+");
                long idle = Long.parseLong(tokens[4]);
                long total = 0;
                for (int i = 1; i < tokens.length; i++) {
                    total += Long.parseLong(tokens[i]);
                }
                return (int) (((total - idle) * 100) / total);
            }
            return -1;
        } catch (Exception e) {
            return -1;
        }
    }
    
    private int getCpuFrequency() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq"));
            String line = reader.readLine();
            reader.close();
            if (line != null) {
                return Integer.parseInt(line) / 1000; // Convert from kHz to MHz
            }
            return -1;
        } catch (Exception e) {
            return -1;
        }
    }
    
    private int getRunningProcesses() {
        try {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> processes = activityManager.getRunningAppProcesses();
            return processes != null ? processes.size() : 0;
        } catch (Exception e) {
            return -1;
        }
    }
    
    private long getUptime() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/uptime"));
            String line = reader.readLine();
            reader.close();
            if (line != null) {
                String[] tokens = line.split("\\s+");
                return (long) (Double.parseDouble(tokens[0]) * 1000); // Convert to milliseconds
            }
            return -1;
        } catch (Exception e) {
            return -1;
        }
    }
}
