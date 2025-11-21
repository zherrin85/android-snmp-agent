#!/bin/bash

# Android Device SNMP Agent - OID Test Script
# This script tests all available OIDs in the Android Device MIB

DEVICE_IP="192.168.1.100"
PORT="10161"
COMMUNITY="blackjack"

echo "=========================================="
echo "Android Device SNMP Agent - OID Test"
echo "Device: $DEVICE_IP:$PORT"
echo "Community: $COMMUNITY"
echo "=========================================="

# Test Legacy Sample MIB
echo ""
echo "=== Legacy Sample MIB ==="
echo "Testing Sample Value (1.3.6.1.4.1.5380.1.16.1.1.0):"
snmpget -v2c -c $COMMUNITY $DEVICE_IP:$PORT 1.3.6.1.4.1.5380.1.16.1.1.0

# Test System Information
echo ""
echo "=== System Information (10.x) ==="
echo "Testing Device Model (1.3.6.1.4.1.5380.1.16.10.1.0):"
snmpget -v2c -c $COMMUNITY $DEVICE_IP:$PORT 1.3.6.1.4.1.5380.1.16.10.1.0

echo "Testing Device Manufacturer (1.3.6.1.4.1.5380.1.16.10.2.0):"
snmpget -v2c -c $COMMUNITY $DEVICE_IP:$PORT 1.3.6.1.4.1.5380.1.16.10.2.0

echo "Testing Android Version (1.3.6.1.4.1.5380.1.16.10.3.0):"
snmpget -v2c -c $COMMUNITY $DEVICE_IP:$PORT 1.3.6.1.4.1.5380.1.16.10.3.0

echo "Testing API Level (1.3.6.1.4.1.5380.1.16.10.4.0):"
snmpget -v2c -c $COMMUNITY $DEVICE_IP:$PORT 1.3.6.1.4.1.5380.1.16.10.4.0

echo "Testing Device Serial (1.3.6.1.4.1.5380.1.16.10.5.0):"
snmpget -v2c -c $COMMUNITY $DEVICE_IP:$PORT 1.3.6.1.4.1.5380.1.16.10.5.0

# Test Memory Information
echo ""
echo "=== Memory Information (2.x) ==="
echo "Testing Total Memory (1.3.6.1.4.1.5380.1.16.2.1.0):"
snmpget -v2c -c $COMMUNITY $DEVICE_IP:$PORT 1.3.6.1.4.1.5380.1.16.2.1.0

echo "Testing Available Memory (1.3.6.1.4.1.5380.1.16.2.2.0):"
snmpget -v2c -c $COMMUNITY $DEVICE_IP:$PORT 1.3.6.1.4.1.5380.1.16.2.2.0

echo "Testing Used Memory (1.3.6.1.4.1.5380.1.16.2.3.0):"
snmpget -v2c -c $COMMUNITY $DEVICE_IP:$PORT 1.3.6.1.4.1.5380.1.16.2.3.0

echo "Testing Memory Usage % (1.3.6.1.4.1.5380.1.16.2.4.0):"
snmpget -v2c -c $COMMUNITY $DEVICE_IP:$PORT 1.3.6.1.4.1.5380.1.16.2.4.0

# Test Storage Information
echo ""
echo "=== Storage Information (3.x) ==="
echo "Testing Total Storage (1.3.6.1.4.1.5380.1.16.3.1.0):"
snmpget -v2c -c $COMMUNITY $DEVICE_IP:$PORT 1.3.6.1.4.1.5380.1.16.3.1.0

echo "Testing Available Storage (1.3.6.1.4.1.5380.1.16.3.2.0):"
snmpget -v2c -c $COMMUNITY $DEVICE_IP:$PORT 1.3.6.1.4.1.5380.1.16.3.2.0

echo "Testing Used Storage (1.3.6.1.4.1.5380.1.16.3.3.0):"
snmpget -v2c -c $COMMUNITY $DEVICE_IP:$PORT 1.3.6.1.4.1.5380.1.16.3.3.0

echo "Testing Storage Usage % (1.3.6.1.4.1.5380.1.16.3.4.0):"
snmpget -v2c -c $COMMUNITY $DEVICE_IP:$PORT 1.3.6.1.4.1.5380.1.16.3.4.0

# Test Battery Information
echo ""
echo "=== Battery Information (4.x) ==="
echo "Testing Battery Level (1.3.6.1.4.1.5380.1.16.4.1.0):"
snmpget -v2c -c $COMMUNITY $DEVICE_IP:$PORT 1.3.6.1.4.1.5380.1.16.4.1.0

echo "Testing Battery Temperature (1.3.6.1.4.1.5380.1.16.4.2.0):"
snmpget -v2c -c $COMMUNITY $DEVICE_IP:$PORT 1.3.6.1.4.1.5380.1.16.4.2.0

echo "Testing Battery Voltage (1.3.6.1.4.1.5380.1.16.4.3.0):"
snmpget -v2c -c $COMMUNITY $DEVICE_IP:$PORT 1.3.6.1.4.1.5380.1.16.4.3.0

echo "Testing Battery Status (1.3.6.1.4.1.5380.1.16.4.4.0):"
snmpget -v2c -c $COMMUNITY $DEVICE_IP:$PORT 1.3.6.1.4.1.5380.1.16.4.4.0

# Test Network Information
echo ""
echo "=== Network Information (5.x) ==="
echo "Testing Network Type (1.3.6.1.4.1.5380.1.16.5.1.0):"
snmpget -v2c -c $COMMUNITY $DEVICE_IP:$PORT 1.3.6.1.4.1.5380.1.16.5.1.0

echo "Testing WiFi SSID (1.3.6.1.4.1.5380.1.16.5.2.0):"
snmpget -v2c -c $COMMUNITY $DEVICE_IP:$PORT 1.3.6.1.4.1.5380.1.16.5.2.0

echo "Testing WiFi Signal Strength (1.3.6.1.4.1.5380.1.16.5.3.0):"
snmpget -v2c -c $COMMUNITY $DEVICE_IP:$PORT 1.3.6.1.4.1.5380.1.16.5.3.0

echo "Testing IP Address (1.3.6.1.4.1.5380.1.16.5.4.0):"
snmpget -v2c -c $COMMUNITY $DEVICE_IP:$PORT 1.3.6.1.4.1.5380.1.16.5.4.0

echo "Testing MAC Address (1.3.6.1.4.1.5380.1.16.5.5.0):"
snmpget -v2c -c $COMMUNITY $DEVICE_IP:$PORT 1.3.6.1.4.1.5380.1.16.5.5.0

# Test CPU Information
echo ""
echo "=== CPU Information (6.x) ==="
echo "Testing CPU Cores (1.3.6.1.4.1.5380.1.16.6.1.0):"
snmpget -v2c -c $COMMUNITY $DEVICE_IP:$PORT 1.3.6.1.4.1.5380.1.16.6.1.0

echo "Testing CPU Usage (1.3.6.1.4.1.5380.1.16.6.2.0):"
snmpget -v2c -c $COMMUNITY $DEVICE_IP:$PORT 1.3.6.1.4.1.5380.1.16.6.2.0

echo "Testing CPU Frequency (1.3.6.1.4.1.5380.1.16.6.3.0):"
snmpget -v2c -c $COMMUNITY $DEVICE_IP:$PORT 1.3.6.1.4.1.5380.1.16.6.3.0

# Test Application Information
echo ""
echo "=== Application Information (7.x) ==="
echo "Testing Running Processes (1.3.6.1.4.1.5380.1.16.7.1.0):"
snmpget -v2c -c $COMMUNITY $DEVICE_IP:$PORT 1.3.6.1.4.1.5380.1.16.7.1.0

echo "Testing Uptime (1.3.6.1.4.1.5380.1.16.7.2.0):"
snmpget -v2c -c $COMMUNITY $DEVICE_IP:$PORT 1.3.6.1.4.1.5380.1.16.7.2.0

# Test SNMP Walk
echo ""
echo "=== SNMP Walk Test ==="
echo "Walking all Android Device MIB OIDs:"
snmpwalk -v2c -c $COMMUNITY $DEVICE_IP:$PORT 1.3.6.1.4.1.5380.1.16

echo ""
echo "=========================================="
echo "Test completed!"
echo "=========================================="
