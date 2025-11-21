package com.example.snmpagent;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class NetworkUtils {
    
    /**
     * Get the local IP address of the device
     * @return IP address as string, or null if not found
     */
    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress()) {
                        // Return IPv4 address
                        if (inetAddress.getHostAddress().indexOf(':') == -1) {
                            return inetAddress.getHostAddress();
                        }
                    }
                }
            }
        } catch (SocketException ex) {
            android.util.Log.e("NetworkUtils", "Error getting IP address", ex);
        }
        return null;
    }
    
    /**
     * Get all available network interfaces and their IP addresses
     * @return String representation of network interfaces
     */
    public static String getNetworkInfo() {
        StringBuilder sb = new StringBuilder();
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                sb.append("Interface: ").append(intf.getName()).append("\n");
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    sb.append("  IP: ").append(inetAddress.getHostAddress()).append("\n");
                }
            }
        } catch (SocketException ex) {
            sb.append("Error: ").append(ex.getMessage());
        }
        return sb.toString();
    }
}