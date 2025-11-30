package com.example.snmpagent;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.example.mib.SampleMib;
import com.example.Modules;

import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.MessageDispatcher;
import org.snmp4j.MessageDispatcherImpl;
import org.snmp4j.PDU;
import org.snmp4j.SNMP4JSettings;
import org.snmp4j.Snmp;
import org.snmp4j.agent.AgentConfigManager;
import org.snmp4j.agent.DefaultMOServer;
import org.snmp4j.agent.MOServer;
import org.snmp4j.agent.mo.DefaultMOFactory;
import org.snmp4j.agent.mo.MOFactory;
import org.snmp4j.agent.mo.snmp.StorageType;
import org.snmp4j.agent.mo.snmp.VacmMIB;
import org.snmp4j.agent.security.MutableVACM;
import org.snmp4j.cfg.EngineBootsCounterFile;
import org.snmp4j.cfg.EngineBootsProvider;
import org.snmp4j.mp.CounterSupport;
import org.snmp4j.mp.MPv1;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.StatusInformation;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModel;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.TransportMapping;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.TransportMappings;
import org.snmp4j.util.ThreadPool;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SnmpAgentService extends Service {
    
    private static final String CHANNEL_ID = "SNMPAgentChannel";
    private static final int NOTIFICATION_ID = 1;
    private static boolean isServiceRunning = false;
    private static final Object agentLock = new Object();
    private volatile boolean isStarting = false;
    
    private AgentConfigManager agent;
    private DefaultMOServer server;
    private Modules modules;
    private File bootCounterFile;
    private OctetString context = new OctetString("");
    private MessageDispatcher messageDispatcher;
    
    private final IBinder binder = new ServiceBinder();
    
    public class ServiceBinder extends Binder {
        SnmpAgentService getService() {
            return SnmpAgentService.this;
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        isServiceRunning = true;
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, createNotification());
        
        synchronized (agentLock) {
            if (isStarting) {
                android.util.Log.w("SnmpAgentService", "Agent is already starting, ignoring duplicate request");
                return START_STICKY;
            }
            isStarting = true;
        }
        
        // Stop existing agent if running
        stopSnmpAgent();
        
        // Start SNMP agent in a separate thread
        new Thread(() -> {
            try {
                synchronized (agentLock) {
                    // Wait a bit for cleanup
                    Thread.sleep(1000);
                    startSnmpAgent();
                }
            } catch (Exception e) {
                android.util.Log.e("SnmpAgentService", "Error starting SNMP agent", e);
                stopSelf();
            } finally {
                synchronized (agentLock) {
                    isStarting = false;
                }
            }
        }).start();
        
        return START_STICKY; // Restart if killed
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        isServiceRunning = false;
        stopSnmpAgent();
    }
    
    public static boolean isRunning() {
        return isServiceRunning;
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "SNMP Agent Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("SNMP Agent is running");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        );
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int port = prefs.getInt("port", 1161);
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SNMP Agent Running")
            .setContentText("Listening on port " + port)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();
    }
    
    private void startSnmpAgent() {
        // Make sure old agent is stopped
        if (agent != null) {
            android.util.Log.w("SnmpAgentService", "Agent already running, stopping first");
            stopSnmpAgent();
            try {
                Thread.sleep(1000); // Wait for cleanup
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            int port = prefs.getInt("port", 1161);
            String getCommunity = prefs.getString("get_community", "blackjack");
            String setCommunity = prefs.getString("set_community", "blackjack007");
            String trapCommunity = prefs.getString("trap_community", "blackjack");
            String trapDestination = prefs.getString("trap_destination", "10.11.139.83");
            
            // Get device IP address - but listen on all interfaces for better connectivity
            String deviceIp = NetworkUtils.getLocalIpAddress();
            android.util.Log.i("SnmpAgentService", "Detected device IP: " + deviceIp);
            
            // Force IPv4 binding to ensure compatibility - try specific device IP first
            String primaryAddress = deviceIp != null ? "udp:" + deviceIp + "/" + port : "udp:0.0.0.0/" + port;
            String backupAddress = "udp:0.0.0.0/" + port;
            android.util.Log.i("SnmpAgentService", "Primary binding to address: " + primaryAddress);
            android.util.Log.i("SnmpAgentService", "Backup address: " + backupAddress);
            
            // Initialize server
            server = new DefaultMOServer();
            MOServer[] moServers = new MOServer[]{server};
            
            bootCounterFile = new File(getFilesDir(), "bootCounter.txt");
            EngineBootsCounterFile engineBootsCounterFile = 
                new EngineBootsCounterFile(bootCounterFile);
            OctetString ownEngineId = engineBootsCounterFile.getEngineId(
                new OctetString(MPv3.createLocalEngineID())
            );
            
            // Force IPv4 preference
            System.setProperty("java.net.preferIPv4Stack", "true");
            
            // Setup agent with primary address only (specific IP works better than 0.0.0.0)
            List<Object> addresses = new ArrayList<>();
            addresses.add(primaryAddress);
            // Don't add backup address to avoid port conflicts
            android.util.Log.i("SnmpAgentService", "About to setup agent with " + addresses.size() + " addresses: " + addresses);
            setupAgent(moServers, engineBootsCounterFile, ownEngineId, 
                      addresses, getCommunity, setCommunity, trapCommunity);
            android.util.Log.i("SnmpAgentService", "Agent setup completed");
            
            // Register MIBs
            registerMIBs();
            
            // Start agent
            server.addContext(context);
            android.util.Log.d("SnmpAgentService", "Server context added");
            
            agent.initialize();
            android.util.Log.d("SnmpAgentService", "Agent initialized");
            
            agent.setupProxyForwarder();
            android.util.Log.d("SnmpAgentService", "Proxy forwarder setup");
            
            agent.registerShutdownHook();
            android.util.Log.d("SnmpAgentService", "Shutdown hook registered");
            
            // Log success info BEFORE calling run() since run() blocks
            android.util.Log.i("SnmpAgentService", "SNMP Agent ready to start on " + primaryAddress);
            android.util.Log.i("SnmpAgentService", "Communities - Get: " + getCommunity + ", Set: " + setCommunity + ", Trap: " + trapCommunity);
            android.util.Log.i("SnmpAgentService", "Network info: " + NetworkUtils.getNetworkInfo());
            android.util.Log.i("SnmpAgentService", "Registered OID: " + com.example.mib.SampleMib.SAMPLE_OID);
            
            // Add debugging to see if packets are being received
            android.util.Log.i("SnmpAgentService", "MessageDispatcher transport mappings: " + messageDispatcher.getTransportMappings().size());
            for (org.snmp4j.TransportMapping tm : messageDispatcher.getTransportMappings()) {
                android.util.Log.i("SnmpAgentService", "Transport mapping: " + tm.getClass().getSimpleName() + " listening on: " + tm.getListenAddress());
                // Ensure transport mapping is properly listening
                if (!tm.isListening()) {
                    android.util.Log.w("SnmpAgentService", "Transport mapping is NOT listening - trying to start it");
                    try {
                        tm.listen();
                        android.util.Log.i("SnmpAgentService", "Transport mapping started listening");
                    } catch (Exception e) {
                        android.util.Log.e("SnmpAgentService", "Failed to start transport mapping", e);
                    }
                }
            }
            
            // Start the MessageDispatcher explicitly to process incoming messages
            android.util.Log.i("SnmpAgentService", "Starting MessageDispatcher to process incoming SNMP messages");
            try {
                // Add message processing capabilities to the MessageDispatcher
                messageDispatcher.addMessageProcessingModel(new MPv1());
                messageDispatcher.addMessageProcessingModel(new MPv2c());
                android.util.Log.i("SnmpAgentService", "Added message processing models (v1, v2c)");
                
                // Try starting MessageDispatcher explicitly
                android.util.Log.i("SnmpAgentService", "Starting MessageDispatcher explicitly");
                for (org.snmp4j.TransportMapping tm : messageDispatcher.getTransportMappings()) {
                    if (!tm.isListening()) {
                        tm.listen();
                        android.util.Log.i("SnmpAgentService", "Started transport mapping: " + tm.getListenAddress());
                    }
                }
                
                // Use direct SNMP responder approach (AgentConfigManager.run() was the problem)
                android.util.Log.i("SnmpAgentService", "Using direct SNMP responder to handle MIB requests");
                
                // Create a direct SNMP responder using Snmp class
                org.snmp4j.Snmp snmp = new org.snmp4j.Snmp(messageDispatcher, 
                    messageDispatcher.getTransportMappings().iterator().next());
                
                // Add command responder to handle incoming requests through the MO Server
                snmp.addCommandResponder(new org.snmp4j.CommandResponder() {
                    @Override
                    public void processPdu(org.snmp4j.CommandResponderEvent event) {
                        android.util.Log.i("SnmpAgentService", "*** SNMP REQUEST RECEIVED! ***");
                        android.util.Log.i("SnmpAgentService", "PDU: " + event.getPDU());
                        
                        try {
                            // Use the MO Server to process the request through registered MIBs
                            org.snmp4j.PDU requestPDU = event.getPDU();
                            org.snmp4j.PDU responsePDU = new org.snmp4j.PDU();
                            responsePDU.setType(org.snmp4j.PDU.RESPONSE);
                            responsePDU.setRequestID(requestPDU.getRequestID());
                            
                            int pduType = requestPDU.getType();
                            android.util.Log.i("SnmpAgentService", "PDU Type: " + pduType + 
                                (pduType == org.snmp4j.PDU.GET ? " (GET)" : 
                                 pduType == org.snmp4j.PDU.GETNEXT ? " (GETNEXT)" : " (OTHER)"));
                            
                            // Process each variable binding in the request
                            for (int i = 0; i < requestPDU.size(); i++) {
                                org.snmp4j.smi.VariableBinding vb = requestPDU.get(i);
                                org.snmp4j.smi.OID oid = vb.getOid();
                                android.util.Log.i("SnmpAgentService", "Processing OID: " + oid);
                                
                                if (pduType == org.snmp4j.PDU.GETNEXT) {
                                    // For GETNEXT, find the next OID in the tree
                                    org.snmp4j.smi.OID nextOid = findNextOid(oid);
                                    if (nextOid != null) {
                                        android.util.Log.i("SnmpAgentService", "Next OID found: " + nextOid);
                                        // Get the value for the next OID
                                        org.snmp4j.agent.MOQuery query = new org.snmp4j.agent.DefaultMOQuery(
                                            new org.snmp4j.agent.DefaultMOContextScope(context, nextOid, true, nextOid, true));
                                        org.snmp4j.agent.ManagedObject mo = server.lookup(query);
                                        
                                        if (mo != null && mo instanceof org.snmp4j.agent.mo.MOScalar) {
                                            org.snmp4j.smi.Variable value = ((org.snmp4j.agent.mo.MOScalar) mo).getValue();
                                            responsePDU.add(new org.snmp4j.smi.VariableBinding(nextOid, value));
                                        } else {
                                            responsePDU.add(new org.snmp4j.smi.VariableBinding(nextOid, 
                                                new org.snmp4j.smi.Null()));
                                        }
                                    } else {
                                        android.util.Log.i("SnmpAgentService", "No next OID found - end of MIB");
                                        // End of MIB
                                        responsePDU.add(new org.snmp4j.smi.VariableBinding(oid, 
                                            new org.snmp4j.smi.Null()));
                                    }
                                } else {
                                    // Handle GET request
                                    org.snmp4j.agent.MOQuery query = new org.snmp4j.agent.DefaultMOQuery(
                                        new org.snmp4j.agent.DefaultMOContextScope(context, oid, true, oid, true));
                                    
                                    // Query the MO Server for this OID
                                    org.snmp4j.agent.ManagedObject mo = server.lookup(query);
                                    
                                    if (mo != null) {
                                        android.util.Log.i("SnmpAgentService", "Found MO for OID: " + oid);
                                        // Create a variable binding with the value from the MO
                                        org.snmp4j.smi.Variable value = null;
                                        if (mo instanceof org.snmp4j.agent.mo.MOScalar) {
                                            value = ((org.snmp4j.agent.mo.MOScalar) mo).getValue();
                                            android.util.Log.i("SnmpAgentService", "Retrieved value: " + value);
                                        }
                                        
                                        if (value != null) {
                                            responsePDU.add(new org.snmp4j.smi.VariableBinding(oid, value));
                                        } else {
                                            responsePDU.add(new org.snmp4j.smi.VariableBinding(oid, 
                                                new org.snmp4j.smi.Null()));
                                        }
                                    } else {
                                        android.util.Log.w("SnmpAgentService", "No MO found for OID: " + oid);
                                        // Return noSuchObject
                                        responsePDU.add(new org.snmp4j.smi.VariableBinding(oid, 
                                            new org.snmp4j.smi.Null()));
                                    }
                                }
                            }
                            
                            // Send response
                            event.getMessageDispatcher().returnResponsePdu(
                                event.getMessageProcessingModel(),
                                event.getSecurityModel(),
                                event.getSecurityName(),
                                event.getSecurityLevel(),
                                responsePDU,
                                event.getMaxSizeResponsePDU(),
                                event.getStateReference(),
                                new org.snmp4j.mp.StatusInformation());
                            android.util.Log.i("SnmpAgentService", "Response sent successfully!");
                            
                        } catch (Exception e) {
                            android.util.Log.e("SnmpAgentService", "Error processing SNMP request", e);
                        }
                    }
                });
                
                snmp.listen();
                android.util.Log.i("SnmpAgentService", "Direct SNMP responder is now listening and ready!");
                
                // Give the responder time to start
                Thread.sleep(200);
                android.util.Log.i("SnmpAgentService", "SNMP Agent configured - ready to process MIB requests");
                
            } catch (Exception e) {
                android.util.Log.e("SnmpAgentService", "Error configuring MessageDispatcher", e);
            }
            
            // Verify all components are properly connected
            android.util.Log.i("SnmpAgentService", "Agent state: initialized=" + (agent != null));
            android.util.Log.i("SnmpAgentService", "MessageDispatcher state: " + (messageDispatcher != null ? "active" : "null"));
            android.util.Log.i("SnmpAgentService", "MO Server state: " + (server != null ? "active" : "null"));
            
            android.util.Log.i("SnmpAgentService", "SNMP Agent started and ready to receive requests");
            android.util.Log.i("SnmpAgentService", "Transport mappings are active and listening for SNMP requests");
            
        } catch (Exception e) {
            android.util.Log.e("SnmpAgentService", "Failed to start SNMP agent", e);
            throw new RuntimeException(e);
        }
    }
    
    private void setupAgent(MOServer[] moServers, EngineBootsProvider engineBootsProvider,
                           OctetString engineID, List<Object> listenAddress,
                           String getCommunity, String setCommunity, String trapCommunity) {
        try {
            android.util.Log.d("SnmpAgentService", "setupAgent called with " + listenAddress.size() + " addresses");
            // Clean up old dispatcher if exists (will be handled in stopSnmpAgent)
            // Just reset reference here
            messageDispatcher = null;
            
            messageDispatcher = new MessageDispatcherImpl();
            android.util.Log.d("SnmpAgentService", "MessageDispatcher created, about to add listen addresses");
            addListenAddresses(messageDispatcher, listenAddress);
            android.util.Log.d("SnmpAgentService", "Listen addresses added");
            
            VacmMIB vacm = getCustomViews(moServers, getCommunity, setCommunity, trapCommunity);
            
            agent = new AgentConfigManager(
                engineID, messageDispatcher, vacm, moServers,
                ThreadPool.create("AndroidSNMPAgent", 4),
                null, null, engineBootsProvider, null, Collections.emptyList()
            );
            
            android.util.Log.d("SnmpAgentService", "AgentConfigManager created with MessageDispatcher");
            
            agent.setContext(
                new SecurityModels(),
                new SecurityProtocols(SecurityProtocols.SecurityProtocolSet.maxCompatibility),
                new CounterSupport()
            );
            
        } catch (Exception e) {
            android.util.Log.e("SnmpAgentService", "Error setting up agent", e);
            throw new RuntimeException(e);
        }
    }
    
    private VacmMIB getCustomViews(MOServer[] moServers, String getCommunity, 
                                   String setCommunity, String trapCommunity) {
        VacmMIB vacm = new VacmMIB(moServers);
        
        // SNMPv2c groups - community string is used as security name
        // Get community group
        vacm.addGroup(SecurityModel.SECURITY_MODEL_SNMPv2c, new OctetString(getCommunity), 
                     new OctetString(getCommunity), StorageType.nonVolatile);
        
        // Set community group (if different from get)
        if (!setCommunity.equals(getCommunity)) {
            vacm.addGroup(SecurityModel.SECURITY_MODEL_SNMPv2c, new OctetString(setCommunity), 
                         new OctetString(setCommunity), StorageType.nonVolatile);
        }
        
        // Trap community group (if different and not null)
        if (trapCommunity != null && !trapCommunity.equals(getCommunity) && !trapCommunity.equals(setCommunity)) {
            vacm.addGroup(SecurityModel.SECURITY_MODEL_SNMPv2c, new OctetString(trapCommunity), 
                         new OctetString(trapCommunity), StorageType.nonVolatile);
        }
        
        // Access for GET community (read operations)
        vacm.addAccess(new OctetString(getCommunity), new OctetString(""),
                      SecurityModel.SECURITY_MODEL_ANY, SecurityLevel.NOAUTH_NOPRIV,
                      MutableVACM.VACM_MATCH_EXACT,
                      new OctetString("readView"), new OctetString("readView"),
                      new OctetString("notifyView"), StorageType.nonVolatile);
        
        // Access for SET community (write operations)
        if (!setCommunity.equals(getCommunity)) {
            vacm.addAccess(new OctetString(setCommunity), new OctetString(""),
                          SecurityModel.SECURITY_MODEL_ANY, SecurityLevel.NOAUTH_NOPRIV,
                          MutableVACM.VACM_MATCH_EXACT,
                          new OctetString("readView"), new OctetString("writeView"),
                          new OctetString("notifyView"), StorageType.nonVolatile);
        } else {
            // If same community, allow both read and write
            vacm.addAccess(new OctetString(getCommunity), new OctetString(""),
                          SecurityModel.SECURITY_MODEL_ANY, SecurityLevel.NOAUTH_NOPRIV,
                          MutableVACM.VACM_MATCH_EXACT,
                          new OctetString("readView"), new OctetString("writeView"),
                          new OctetString("notifyView"), StorageType.nonVolatile);
        }
        
        // Read view - allow access to all OIDs under 1 (includes system MIBs)
        vacm.addViewTreeFamily(new OctetString("readView"), new OID("1"),
                              new OctetString(), VacmMIB.vacmViewIncluded,
                              StorageType.nonVolatile);
        
        // Write view - allow access to all OIDs under 1
        vacm.addViewTreeFamily(new OctetString("writeView"), new OID("1"),
                              new OctetString(), VacmMIB.vacmViewIncluded,
                              StorageType.nonVolatile);
        
        // Notify view for traps
        vacm.addViewTreeFamily(new OctetString("notifyView"), new OID("1"),
                              new OctetString(), VacmMIB.vacmViewIncluded,
                              StorageType.nonVolatile);
        
        return vacm;
    }
    
    private void addListenAddresses(MessageDispatcher md, List<Object> addresses) {
        android.util.Log.i("SnmpAgentService", "Setting up transport mappings for " + addresses.size() + " addresses");
        int successfulMappings = 0;
        for (Object addressString : addresses) {
            try {
                android.util.Log.d("SnmpAgentService", "Processing address: " + addressString);
                Address address = GenericAddress.parse(addressString.toString());
                if (address == null) {
                    android.util.Log.e("SnmpAgentService", 
                        "Could not parse address: " + addressString);
                    continue;
                }
                
                android.util.Log.d("SnmpAgentService", "Parsed address: " + address);
                TransportMapping<? extends Address> tm =
                    TransportMappings.getInstance().createTransportMapping(address);
                if (tm != null) {
                    md.addTransportMapping(tm);
                    android.util.Log.i("SnmpAgentService", 
                        "Successfully added transport mapping for: " + address);
                    
                    // Try to start listening
                    try {
                        tm.listen();
                        successfulMappings++;
                        android.util.Log.i("SnmpAgentService", 
                            "Transport mapping is now listening on: " + address);
                        android.util.Log.i("SnmpAgentService", 
                            "Listening address details: " + tm.getListenAddress());
                    } catch (Exception listenEx) {
                        android.util.Log.e("SnmpAgentService", 
                            "Failed to start listening on: " + address, listenEx);
                        android.util.Log.e("SnmpAgentService", 
                            "Listen error details: " + listenEx.getMessage());
                    }
                } else {
                    android.util.Log.w("SnmpAgentService", 
                        "No transport mapping created for: " + address);
                }
            } catch (Exception e) {
                android.util.Log.e("SnmpAgentService", 
                    "Error adding address: " + addressString, e);
            }
        }
        android.util.Log.i("SnmpAgentService", "Transport mapping setup complete. Successfully bound to " + successfulMappings + " addresses");
        if (successfulMappings == 0) {
            throw new RuntimeException("No transport mappings could be established - agent cannot start");
        }
    }
    
    private void registerMIBs() {
        android.util.Log.d("SnmpAgentService", "Starting MIB registration");
        
        // Unregister MIBs first if they exist
        if (modules != null && server != null) {
            try {
                android.util.Log.d("SnmpAgentService", "Unregistering existing MIBs");
                modules.unregisterMOs(server, context);
            } catch (Exception e) {
                android.util.Log.w("SnmpAgentService", "Error unregistering MIBs (may not be registered)", e);
            }
        }
        
        // Create new modules instance with context for real device stats
        MOFactory factory = DefaultMOFactory.getInstance();
        modules = new Modules(factory, this);
        android.util.Log.d("SnmpAgentService", "Created new Modules instance with Android context");
        
        try {
            android.util.Log.d("SnmpAgentService", "Registering MIBs with server and context");
            modules.registerMOs(server, context);
            android.util.Log.i("SnmpAgentService", "MIBs registered successfully");
            android.util.Log.i("SnmpAgentService", "Sample MIB OID: " + com.example.mib.SampleMib.SAMPLE_OID);
            android.util.Log.i("SnmpAgentService", "Android Device MIB registered with " + getAndroidDeviceMibOidCount() + " OIDs");
            android.util.Log.i("SnmpAgentService", "Device Model OID: " + com.example.mib.AndroidDeviceMib.DEVICE_MODEL);
            android.util.Log.i("SnmpAgentService", "Memory Usage OID: " + com.example.mib.AndroidDeviceMib.MEMORY_USAGE_PERCENT);
            android.util.Log.i("SnmpAgentService", "Battery Level OID: " + com.example.mib.AndroidDeviceMib.BATTERY_LEVEL);
            android.util.Log.i("SnmpAgentService", "Network Type OID: " + com.example.mib.AndroidDeviceMib.NETWORK_TYPE);
            android.util.Log.i("SnmpAgentService", "Original Sample OID: " + com.example.mib.SampleMib.SAMPLE_OID + " (legacy compatibility)");
        } catch (Exception e) {
            android.util.Log.e("SnmpAgentService", "Error registering MIBs", e);
            throw new RuntimeException("Failed to register MIBs", e);
        }
    }
    
    private int getAndroidDeviceMibOidCount() {
        // Count the number of OIDs in AndroidDeviceMib
        // 5 System + 4 Memory + 4 Storage + 4 Battery + 5 Network + 3 CPU + 2 Application = 27 OIDs
        return 27;
    }
    
    private org.snmp4j.smi.OID findNextOid(org.snmp4j.smi.OID currentOid) {
        // Define all available OIDs in sorted order for GETNEXT traversal
        java.util.List<org.snmp4j.smi.OID> allOids = new java.util.ArrayList<>();
        
        // Standard System MIB (1.3.6.1.2.1.1.x.0) - CRITICAL for SolarWinds
        allOids.add(com.example.mib.SystemMibSimple.SYS_DESCR);
        allOids.add(com.example.mib.SystemMibSimple.SYS_OBJECT_ID);
        allOids.add(com.example.mib.SystemMibSimple.SYS_UP_TIME);
        allOids.add(com.example.mib.SystemMibSimple.SYS_CONTACT);
        allOids.add(com.example.mib.SystemMibSimple.SYS_NAME);
        allOids.add(com.example.mib.SystemMibSimple.SYS_LOCATION);
        allOids.add(com.example.mib.SystemMibSimple.SYS_SERVICES);
        
        // Memory Information (2.x)
        allOids.add(com.example.mib.AndroidDeviceMib.TOTAL_MEMORY);
        allOids.add(com.example.mib.AndroidDeviceMib.AVAILABLE_MEMORY);
        allOids.add(com.example.mib.AndroidDeviceMib.USED_MEMORY);
        allOids.add(com.example.mib.AndroidDeviceMib.MEMORY_USAGE_PERCENT);
        
        // Storage Information (3.x)
        allOids.add(com.example.mib.AndroidDeviceMib.TOTAL_STORAGE);
        allOids.add(com.example.mib.AndroidDeviceMib.AVAILABLE_STORAGE);
        allOids.add(com.example.mib.AndroidDeviceMib.USED_STORAGE);
        allOids.add(com.example.mib.AndroidDeviceMib.STORAGE_USAGE_PERCENT);
        
        // Battery Information (4.x)
        allOids.add(com.example.mib.AndroidDeviceMib.BATTERY_LEVEL);
        allOids.add(com.example.mib.AndroidDeviceMib.BATTERY_TEMPERATURE);
        allOids.add(com.example.mib.AndroidDeviceMib.BATTERY_VOLTAGE);
        allOids.add(com.example.mib.AndroidDeviceMib.BATTERY_STATUS);
        
        // Network Information (5.x)
        allOids.add(com.example.mib.AndroidDeviceMib.NETWORK_TYPE);
        allOids.add(com.example.mib.AndroidDeviceMib.WIFI_SSID);
        allOids.add(com.example.mib.AndroidDeviceMib.WIFI_SIGNAL_STRENGTH);
        allOids.add(com.example.mib.AndroidDeviceMib.IP_ADDRESS);
        allOids.add(com.example.mib.AndroidDeviceMib.MAC_ADDRESS);
        
        // CPU Information (6.x)
        allOids.add(com.example.mib.AndroidDeviceMib.CPU_CORES);
        allOids.add(com.example.mib.AndroidDeviceMib.CPU_USAGE);
        allOids.add(com.example.mib.AndroidDeviceMib.CPU_FREQUENCY);
        
        // Application Information (7.x)
        allOids.add(com.example.mib.AndroidDeviceMib.RUNNING_PROCESSES);
        allOids.add(com.example.mib.AndroidDeviceMib.UPTIME);
        
        // System Information (10.x)
        allOids.add(com.example.mib.AndroidDeviceMib.DEVICE_MODEL);
        allOids.add(com.example.mib.AndroidDeviceMib.DEVICE_MANUFACTURER);
        allOids.add(com.example.mib.AndroidDeviceMib.ANDROID_VERSION);
        allOids.add(com.example.mib.AndroidDeviceMib.API_LEVEL);
        allOids.add(com.example.mib.AndroidDeviceMib.DEVICE_SERIAL);
        
        // Sort the OIDs
        java.util.Collections.sort(allOids);
        
        // Find the next OID after the current one
        for (org.snmp4j.smi.OID oid : allOids) {
            if (oid.compareTo(currentOid) > 0) {
                return oid;
            }
        }
        
        // No next OID found (end of MIB)
        return null;
    }
    
    private void stopSnmpAgent() {
        // Unregister MIBs first
        if (modules != null && server != null) {
            try {
                modules.unregisterMOs(server, context);
                android.util.Log.d("SnmpAgentService", "MIBs unregistered");
            } catch (Exception e) {
                android.util.Log.w("SnmpAgentService", "Error unregistering MIBs", e);
            }
        }
        
        // Shutdown agent
        if (agent != null) {
            try {
                agent.shutdown();
                android.util.Log.i("SnmpAgentService", "SNMP Agent stopped");
            } catch (Exception e) {
                android.util.Log.e("SnmpAgentService", "Error stopping agent", e);
            }
            agent = null;
        }
        
        // Close message dispatcher and transport mappings
        // The agent.shutdown() should handle closing transport mappings
        // Just reset reference
        messageDispatcher = null;
        
        // Reset server
        server = null;
        modules = null;
    }
}

