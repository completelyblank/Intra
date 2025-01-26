import React, { useState } from 'react';
import { View, Text, StyleSheet, FlatList, TouchableOpacity, ImageBackground } from 'react-native';

interface Device {
  ip: string;
  mac: string;
  hostname?: string; // Optional in case it's not always present
  tag?: string;      // Optional tag
}

const Scan = () => {
  const [devices, setDevices] = useState<Device[]>([]);
  const [isScanning, setIsScanning] = useState(false);

  const triggerScan = async () => {
    setIsScanning(true);
    try {
      // Call the backend or trigger local scan
      const response = await fetch('http://127.0.0.1:5000/scan');
      const data: Device[] = await response.json();
      setDevices(data);
    } catch (error) {
      console.error('Error during scan:', error);
    } finally {
      setIsScanning(false);
    }
  };

  const tagDevice = (device: Device) => {
    // Handle tagging the device (e.g., open a modal to input custom name)
    console.log(`Tagging device: ${device.ip}`);
  };

  return (
    <ImageBackground
          source={require('@/assets/images/scan.jpg')} // Background image path
          style={styles.background}
          resizeMode="cover"
        >
    <View style={styles.container}>
      <Text style={styles.header}>Scanned Devices</Text>
      <TouchableOpacity
        onPress={triggerScan}
        disabled={isScanning}
        style={[
          styles.button,
          isScanning ? { opacity: 0.6 } : { opacity: 1 }, // Dim button during scanning
        ]}
      >
        <Text style={styles.buttonText}>
          {isScanning ? 'Scanning...' : 'Scan Network'}
        </Text>
      </TouchableOpacity>
      <FlatList
        data={devices}
        keyExtractor={(item) => item.ip}
        renderItem={({ item }) => (
          <View style={styles.deviceCard}>
            <Text style={styles.deviceText}>{item.hostname || item.ip}</Text>
            <TouchableOpacity onPress={() => tagDevice(item)} style={styles.tagButton}>
              <Text style={styles.tagText}>Tag Device</Text>
            </TouchableOpacity>
          </View>
        )}
      />
    </View>
    </ImageBackground>
  );
};

const styles = StyleSheet.create({
    background: {
        flex: 1,
        width: '100%', // Ensures full width
        height: '100%', // Ensures full height
        justifyContent: 'center',
        alignItems: 'center', // Aligns children horizontally in the center
        backgroundColor: '#020024', // Fallback color in case the image doesn't load
      },
    container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  button: {
    backgroundColor: 'rgb(12, 42, 42)', // Teal base color
    paddingVertical: 14,
    borderRadius: 14,
    elevation: 10,
    marginBottom: 20,
    width: '75%',
    alignItems: 'center',
    justifyContent: 'center',
    shadowColor: 'rgba(5, 255, 255, 0.7)',
    shadowOffset: { width: 0, height: 6 },
    shadowOpacity: 0.5,
    shadowRadius: 12,
  },
  buttonText: {
    color: '#ffffff',
    fontWeight: '600',
    fontSize: 16,
    textShadowColor: 'rgba(0, 0, 0, 0.3)',
    textShadowOffset: { width: 0, height: 2 },
    textShadowRadius: 4,
  },
  header: {
    fontSize: 24,
    color: '#ffffff',
    marginBottom: 20,
    fontWeight: 'bold',
  },
  deviceCard: {
    padding: 10,
    backgroundColor: '#1c1c1c',
    marginBottom: 10,
    borderRadius: 8,
    width: '100%',
    alignItems: 'center',
  },
  deviceText: {
    color: '#ffffff',
    fontSize: 16,
  },
  tagButton: {
    marginTop: 10,
    paddingVertical: 5,
    paddingHorizontal: 10,
    borderRadius: 5,
    backgroundColor: '#00aaaa',
  },
  tagText: {
    color: '#000000',
    fontWeight: 'bold',
  },
});

export default Scan;
