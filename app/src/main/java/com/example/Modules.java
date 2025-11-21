package com.example;

import android.content.Context;
import org.snmp4j.agent.*;
import org.snmp4j.agent.mo.*;
import org.snmp4j.smi.OctetString;
import com.example.mib.SampleMib;
import com.example.mib.AndroidDeviceMib;

public class Modules implements MOGroup {

    private SampleMib sampleMib;
    private AndroidDeviceMib androidDeviceMib;
    private MOFactory factory;
    private Context context;

    public Modules() {
        sampleMib = new SampleMib(123456);
        // androidDeviceMib will be null without context
    }

    public Modules(MOFactory factory) {
        this.factory = factory;
        sampleMib = new SampleMib(123456);
        // androidDeviceMib will be null without context
    }
    
    public Modules(MOFactory factory, Context context) {
        this.factory = factory;
        this.context = context;
        sampleMib = new SampleMib(123456);
        androidDeviceMib = new AndroidDeviceMib(context);
    }

    public void registerMOs(MOServer server, OctetString context) 
        throws DuplicateRegistrationException {
        // Register the original sample MIB
        sampleMib.registerMOs(server, context);
        
        // Register the comprehensive Android device MIB if available
        if (androidDeviceMib != null) {
            androidDeviceMib.registerMOs(server, context);
        }
    }

    public void unregisterMOs(MOServer server, OctetString context) {
        sampleMib.unregisterMOs(server, context);
        
        if (androidDeviceMib != null) {
            androidDeviceMib.unregisterMOs(server, context);
        }
    }

    public SampleMib getSnmp4jDemoMib() {
        return sampleMib;
    }
    
    public AndroidDeviceMib getAndroidDeviceMib() {
        return androidDeviceMib;
    }
}

