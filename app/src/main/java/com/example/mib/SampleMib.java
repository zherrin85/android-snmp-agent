package com.example.mib;

import org.snmp4j.agent.*;
import org.snmp4j.agent.mo.*;
import org.snmp4j.smi.*;

public class SampleMib implements MOGroup {

    public static final OID SAMPLE_OID = new OID(new int[]{1, 3, 6, 1, 4, 1, 5380, 1, 16, 1, 1, 0});

    private MOScalar<Integer32> sampleValue;

    public SampleMib(int value) {
        sampleValue = new MOScalar<>(SAMPLE_OID, MOAccessImpl.ACCESS_READ_WRITE, new Integer32(value));
        sampleValue.setVolatile(true);
    }

    public void registerMOs(MOServer server, OctetString context) throws DuplicateRegistrationException {
        server.register(sampleValue, context);
    }

    public void unregisterMOs(MOServer server, OctetString context) {
        server.unregister(sampleValue, context);
    }

    public Integer32 getSampleValue() {
        return sampleValue.getValue();
    }

    public void setSampleValue(Integer32 value) {
        sampleValue.setValue(value);
    }
}

