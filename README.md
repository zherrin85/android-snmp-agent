# Android SNMP Agent

A comprehensive Android SNMP agent that provides real device statistics for network monitoring systems like SolarWinds.

## 🚀 Features

### Core SNMP Functionality
- **SNMP v2c support** with configurable communities
- **Standard System MIB** (RFC 1213) for SolarWinds compatibility
- **35+ real device statistics** (memory, battery, CPU, network, storage, system info)
- **SNMP walk support** (GETNEXT operations)
- **Configurable port** (default 1161, enterprise-friendly 10161)
- **Auto-start on boot** with user toggle

### Device Statistics Provided
- **System Information**: Model, manufacturer, Android version, API level, serial
- **Memory Information**: Total, available, used memory, usage percentage
- **Storage Information**: Total, available, used storage, usage percentage  
- **Battery Information**: Level, temperature, voltage, charging status
- **Network Information**: Connection type, WiFi SSID, signal strength, IP/MAC addresses
- **CPU Information**: Core count, usage, frequency
- **Application Information**: Running processes, uptime

### Enterprise Features
- **Foreground service** with persistent notification
- **Network auto-detection** (WiFi and LTE cellular compatible)
- **Cross-network communication** support
- **SolarWinds integration** tested and verified
- **Enterprise firewall bypass** (configurable ports)

## 📱 Installation

### Option 1: Build from Source
1. **Clone repository**: `git clone <repository-url>`
2. **Open in Android Studio**: Import the project
3. **Build APK**: `Build → Build Bundle(s)/APK(s) → Build APK(s)`
4. **Install**: Transfer APK to device and install

### Option 2: Command Line Build
```bash
# Debug APK (for testing)
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# Release APK (for production)
./gradlew assembleRelease  
# Output: app/build/outputs/apk/release/app-release-unsigned.apk
```

### Setup
1. **Install APK** on Android device
2. **Grant permissions** (location for WiFi SSID detection)
3. **Configure**: Set port (recommend 10161 for enterprise networks)
4. **Start Service**: Toggle the SNMP service switch in the app

## 🔧 Configuration

### Default Settings
- **Port**: 1161 (configurable to 10161 for enterprise)
- **Read Community**: `blackjack`
- **Write Community**: `blackjack007` 
- **SNMP Version**: v2c
- **Auto-start**: Enabled

### Enterprise Network Setup
- **Recommended Port**: 10161 (bypasses common firewall blocks)
- **Network Types**: WiFi, LTE cellular (no internet required)
- **Monitoring Integration**: SolarWinds, PRTG, Nagios compatible

## 🧪 Testing

### Quick Test
```bash
# Replace with your device's IP address
snmpget -v2c -c blackjack 192.168.1.100:10161 1.3.6.1.4.1.5380.1.16.10.1.0
```

### Comprehensive Test
```bash
# Run the included test script
./test_all_oids.sh
```

### Available OID Ranges
- **1.3.6.1.2.1.1.x** - Standard System MIB (RFC 1213)
- **1.3.6.1.4.1.5380.1.16.2.x** - Memory information
- **1.3.6.1.4.1.5380.1.16.3.x** - Storage information
- **1.3.6.1.4.1.5380.1.16.4.x** - Battery information
- **1.3.6.1.4.1.5380.1.16.5.x** - Network information
- **1.3.6.1.4.1.5380.1.16.6.x** - CPU information
- **1.3.6.1.4.1.5380.1.16.7.x** - Application information
- **1.3.6.1.4.1.5380.1.16.10.x** - System information

## 📊 SolarWinds Integration

### Node Discovery
1. **IP Address**: `[device-ip]:10161`
2. **SNMP Version**: `v2c`
3. **Community String**: `blackjack`
4. **Timeout**: `10+ seconds`

### Monitoring Setup
- **Read Community**: `blackjack` (for monitoring)
- **Write Community**: Not required (agent is read-only)
- **Polling Interval**: 5-15 minutes recommended

## 🏗️ Architecture

### Components
- **SnmpAgentService**: Core SNMP service with direct responder
- **AndroidDeviceMib**: Real device statistics provider
- **MainActivity**: Configuration interface
- **BootReceiver**: Auto-start functionality
- **NetworkUtils**: IP address detection

### Technical Details
- **SNMP4J Library**: v3.7.8 (SNMP) + v3.6.8 (Agent)
- **Direct Responder**: Bypasses AgentConfigManager for reliability
- **Foreground Service**: Ensures continuous operation
- **Dynamic IP Detection**: Works on any network type

## 📋 Requirements

- **Android**: API 26+ (Android 8.0+)
- **Permissions**: Network access, boot receiver, foreground service
- **Network**: WiFi or LTE cellular (no internet required)
- **Monitoring System**: Any SNMP v2c compatible system

## 🔒 Security

- **Read-only agent**: No SET operations supported
- **Configurable communities**: Change default passwords
- **Local network only**: No internet connectivity required
- **Minimal permissions**: Only essential Android permissions

## 📚 Documentation

- **ANDROID_DEVICE_MIB_REFERENCE.md**: Complete OID reference
- **test_all_oids.sh**: Automated testing script
- **Source code**: Fully commented and documented

## 🎯 Use Cases

- **Enterprise device monitoring**: Track Android tablets/phones in corporate environments
- **IoT device management**: Monitor Android-based IoT devices
- **Network infrastructure**: Integrate Android devices into existing SNMP monitoring
- **Remote device tracking**: Monitor devices on cellular networks

## 🏆 Production Ready

✅ **SolarWinds integration tested**  
✅ **Standard System MIB compliance**  
✅ **Enterprise firewall compatible**  
✅ **Cross-network communication verified**  
✅ **35+ real device statistics**  
✅ **No test data - production ready**  
✅ **Auto-start functionality**  
✅ **Professional custom icon**  
✅ **Comprehensive documentation**

---

**Built for enterprise network monitoring with real device statistics and SolarWinds integration.**

