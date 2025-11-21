# Android Device MIB Reference

## Overview
The Android Device MIB provides comprehensive real-time statistics about your Android device through SNMP. This MIB exposes 28 different OIDs covering system information, memory, storage, battery, network, CPU, and application metrics.

## Base OID Structure
- **Base OID**: `1.3.6.1.4.1.5380.1.16`
- **Enterprise**: 5380 (Custom enterprise number)
- **Application**: 1.16 (Android SNMP Agent)

## Available OIDs

### 1. System Information (1.3.6.1.4.1.5380.1.16.10.x)
| OID | Name | Type | Description | Example Value |
|-----|------|------|-------------|---------------|
| `1.3.6.1.4.1.5380.1.16.10.1.0` | Device Model | STRING | Android device model | "Pixel 7" |
| `1.3.6.1.4.1.5380.1.16.10.2.0` | Device Manufacturer | STRING | Device manufacturer | "Google" |
| `1.3.6.1.4.1.5380.1.16.10.3.0` | Android Version | STRING | Android OS version | "13" |
| `1.3.6.1.4.1.5380.1.16.10.4.0` | API Level | INTEGER | Android API level | 33 |
| `1.3.6.1.4.1.5380.1.16.10.5.0` | Device Serial | STRING | Device serial number | "ABC123DEF456" |

### 2. Memory Information (1.3.6.1.4.1.5380.1.16.2.x)
| OID | Name | Type | Description | Example Value |
|-----|------|------|-------------|---------------|
| `1.3.6.1.4.1.5380.1.16.2.1.0` | Total Memory | COUNTER64 | Total RAM in bytes | 8589934592 |
| `1.3.6.1.4.1.5380.1.16.2.2.0` | Available Memory | COUNTER64 | Available RAM in bytes | 4294967296 |
| `1.3.6.1.4.1.5380.1.16.2.3.0` | Used Memory | COUNTER64 | Used RAM in bytes | 4294967296 |
| `1.3.6.1.4.1.5380.1.16.2.4.0` | Memory Usage % | INTEGER | Memory usage percentage | 50 |

### 3. Storage Information (1.3.6.1.4.1.5380.1.16.3.x)
| OID | Name | Type | Description | Example Value |
|-----|------|------|-------------|---------------|
| `1.3.6.1.4.1.5380.1.16.3.1.0` | Total Storage | COUNTER64 | Total storage in bytes | 128849018880 |
| `1.3.6.1.4.1.5380.1.16.3.2.0` | Available Storage | COUNTER64 | Available storage in bytes | 64424509440 |
| `1.3.6.1.4.1.5380.1.16.3.3.0` | Used Storage | COUNTER64 | Used storage in bytes | 64424509440 |
| `1.3.6.1.4.1.5380.1.16.3.4.0` | Storage Usage % | INTEGER | Storage usage percentage | 50 |

### 4. Battery Information (1.3.6.1.4.1.5380.1.16.4.x)
| OID | Name | Type | Description | Example Value |
|-----|------|------|-------------|---------------|
| `1.3.6.1.4.1.5380.1.16.4.1.0` | Battery Level | INTEGER | Battery percentage (0-100) | 85 |
| `1.3.6.1.4.1.5380.1.16.4.2.0` | Battery Temperature | INTEGER | Battery temperature (°C * 10) | 250 |
| `1.3.6.1.4.1.5380.1.16.4.3.0` | Battery Voltage | INTEGER | Battery voltage (mV) | 4200 |
| `1.3.6.1.4.1.5380.1.16.4.4.0` | Battery Status | STRING | Charging status | "CHARGING" |

### 5. Network Information (1.3.6.1.4.1.5380.1.16.5.x)
| OID | Name | Type | Description | Example Value |
|-----|------|------|-------------|---------------|
| `1.3.6.1.4.1.5380.1.16.5.1.0` | Network Type | STRING | Active network type | "WIFI" |
| `1.3.6.1.4.1.5380.1.16.5.2.0` | WiFi SSID | STRING | Connected WiFi network | "MyNetwork" |
| `1.3.6.1.4.1.5380.1.16.5.3.0` | WiFi Signal Strength | INTEGER | WiFi signal strength (dBm) | -45 |
| `1.3.6.1.4.1.5380.1.16.5.4.0` | IP Address | STRING | Device IP address | "192.168.1.100" |
| `1.3.6.1.4.1.5380.1.16.5.5.0` | MAC Address | STRING | WiFi MAC address | "AA:BB:CC:DD:EE:FF" |

### 6. CPU Information (1.3.6.1.4.1.5380.1.16.6.x)
| OID | Name | Type | Description | Example Value |
|-----|------|------|-------------|---------------|
| `1.3.6.1.4.1.5380.1.16.6.1.0` | CPU Cores | INTEGER | Number of CPU cores | 8 |
| `1.3.6.1.4.1.5380.1.16.6.2.0` | CPU Usage | INTEGER | CPU usage percentage | 25 |
| `1.3.6.1.4.1.5380.1.16.6.3.0` | CPU Frequency | INTEGER | CPU frequency (MHz) | 2400 |

### 7. Application Information (1.3.6.1.4.1.5380.1.16.7.x)
| OID | Name | Type | Description | Example Value |
|-----|------|------|-------------|---------------|
| `1.3.6.1.4.1.5380.1.16.7.1.0` | Running Processes | INTEGER | Number of running processes | 45 |
| `1.3.6.1.4.1.5380.1.16.7.2.0` | Uptime | COUNTER64 | System uptime in milliseconds | 3600000 |

### 8. Legacy Sample MIB (1.3.6.1.4.1.5380.1.16.1.1.0)
| OID | Name | Type | Description | Example Value |
|-----|------|------|-------------|---------------|
| `1.3.6.1.4.1.5380.1.16.1.1.0` | Sample Value | INTEGER | Static sample value | 123456 |

## SNMP Testing Commands

### Using snmpget (single OID)
```bash
# Get device model
snmpget -v2c -c blackjack 192.168.1.3:1161 1.3.6.1.4.1.5380.1.16.10.1.0

# Get battery level
snmpget -v2c -c blackjack 192.168.1.3:1161 1.3.6.1.4.1.5380.1.16.4.1.0

# Get memory usage percentage
snmpget -v2c -c blackjack 192.168.1.3:1161 1.3.6.1.4.1.5380.1.16.2.4.0

# Get legacy sample value
snmpget -v2c -c blackjack 192.168.1.3:1161 1.3.6.1.4.1.5380.1.16.1.1.0
```

### Using snmpwalk (multiple OIDs)
```bash
# Walk all Android device MIB OIDs
snmpwalk -v2c -c blackjack 192.168.1.3:1161 1.3.6.1.4.1.5380.1.16

# Walk system information only
snmpwalk -v2c -c blackjack 192.168.1.3:1161 1.3.6.1.4.1.5380.1.16.10

# Walk memory information only
snmpwalk -v2c -c blackjack 192.168.1.3:1161 1.3.6.1.4.1.5380.1.16.2

# Walk battery information only
snmpwalk -v2c -c blackjack 192.168.1.3:1161 1.3.6.1.4.1.5380.1.16.4
```

### Using snmpbulkget (efficient bulk retrieval)
```bash
# Get multiple OIDs efficiently
snmpbulkget -v2c -c blackjack 192.168.1.3:1161 1.3.6.1.4.1.5380.1.16.10.1.0 1.3.6.1.4.1.5380.1.16.2.4.0 1.3.6.1.4.1.5380.1.16.4.1.0
```

## Configuration
- **Device IP**: 192.168.1.3
- **Port**: 1161
- **GET Community**: blackjack
- **SET Community**: blackjack007
- **SNMP Version**: v2c

## Notes
- All values are updated in real-time when queried
- Some values may return -1 or "UNKNOWN" if the system cannot access the information
- Battery temperature is in Celsius * 10 (e.g., 250 = 25.0°C)
- Memory and storage values are in bytes
- WiFi signal strength is in dBm (negative values, closer to 0 = stronger signal)
- CPU frequency may not be available on all devices
- Uptime is in milliseconds since last boot

## Troubleshooting
1. **No response**: Check device IP, port, and community string
2. **Permission errors**: Some values require specific Android permissions
3. **Unknown values**: Normal for some metrics on certain Android versions/devices
4. **Negative values**: Indicates unavailable or error condition for that metric
