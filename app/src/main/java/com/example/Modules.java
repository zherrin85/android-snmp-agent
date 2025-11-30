package com.example;

import android.content.Context;
import org.snmp4j.agent.*;
import org.snmp4j.agent.mo.*;
import org.snmp4j.smi.OctetString;
import com.example.mib.AndroidDeviceMib;
import com.example.mib.SystemMibSimple;

public class Modules implements MOGroup {

    private AndroidDeviceMib androidDeviceMib;
    private SystemMibSimple systemMib;
    private MOFactory factory;
    private Context context;

    public Modules() {
        // androidDeviceMib will be null without context
    }

    public Modules(MOFactory factory) {
        this.factory = factory;
        // androidDeviceMib will be null without context
    }
    
    public Modules(MOFactory factory, Context context) {
        this.factory = factory;
        this.context = context;
        androidDeviceMib = new AndroidDeviceMib(context);
        systemMib = new SystemMibSimple(context);
    }

    public void registerMOs(MOServer server, OctetString context) 
        throws DuplicateRegistrationException {
        // Register the standard System MIB first (required for SolarWinds)
        if (systemMib != null) {
            systemMib.registerMOs(server, context);
        }
        
        // Register the comprehensive Android device MIB
        if (androidDeviceMib != null) {
            androidDeviceMib.registerMOs(server, context);
        }
    }

    public void unregisterMOs(MOServer server, OctetString context) {
        if (androidDeviceMib != null) {
            androidDeviceMib.unregisterMOs(server, context);
        }
        
        if (systemMib != null) {
            systemMib.unregisterMOs(server, context);
        }
    }
    
    public AndroidDeviceMib getAndroidDeviceMib() {
        return androidDeviceMib;
    }
    
    public SystemMibSimple getSystemMib() {
        return systemMib;
    }
}

