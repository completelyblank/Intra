import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet, ActivityIndicator, Image, ImageBackground, ScrollView } from 'react-native';

const Statistics = () => {
  const [graph, setGraph] = useState<string | null>(null);
  const [loading, setLoading] = useState(true); // State for loading

  useEffect(() => {
    fetchGraph(); // Fetch graph when component mounts
  }, []);

  const fetchGraph = async () => {
    try {
      const response = await fetch('http://127.0.0.1:5000/generate-graph'); // Fetch graph
      const blob = await response.blob(); // Get response as blob
      const graphUrl = URL.createObjectURL(blob); // Create local URL for blob
      setGraph(graphUrl); // Update state with graph URL
    } catch (error) {
      console.error('Error fetching the graph:', error);
    } finally {
      setLoading(false); // Stop loading
    }
  };

  return (
    <ImageBackground
      source={require('@/assets/images/stats.jpg')} // Background image path
      style={styles.background}
      resizeMode="cover"
    >
      <ScrollView contentContainerStyle={styles.scrollViewContainer}>
        <View style={styles.container}>
          <Text style={styles.heading}>Statistics</Text>
          {loading ? (
            <ActivityIndicator size="large" color="#00aaaa" style={styles.spinner} /> // Show spinner while loading
          ) : (
            graph && <Image source={{ uri: graph }} style={styles.graph} /> // Display graph when available
          )}

          <Text style={styles.description}>
            This graph provides insights into the various statistics over time, showcasing
            the relationship between different variables. The analysis is based on real-time
            data and offers a clear visualization of trends and patterns, helping you understand
            the core metrics better.
          </Text>
        </View>
      </ScrollView>
    </ImageBackground>
  );
};

const styles = StyleSheet.create({
  scrollViewContainer: {
    flexGrow: 1, // Ensures the scroll view fills the entire screen
  },
  container: {
    flex: 2,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
    opacity: 0.85, // Add a slight transparency for the container
  },
  heading: {
    fontSize: 30,
    fontWeight: 'bold',
    color: '#00aaaa', // Neon teal color for the heading
    marginBottom: 20,
    textShadowColor: '#000', // Dark shadow for better readability
    textShadowOffset: { width: 1, height: 1 },
    textShadowRadius: 10,
  },
  graph: {
    width: '70%',
    height: '70%', // Set height to fit within container (adjust if necessary)
    marginBottom: 20, // Space below the graph
  },
  description: {
    fontSize: 16,
    color: '#ffffff', // Light text color for readability
    marginTop: 10,
    textAlign: 'center', // Center align the text
    paddingHorizontal: 10, // Padding on left and right for better readability
    lineHeight: 24, // Line height to make the paragraph easier to read
  },
  background: {
    flex: 1,
    width: '100%', // Ensures full width
    height: '100%', // Ensures full height
    justifyContent: 'center',
    alignItems: 'center', // Aligns children horizontally in the center
    backgroundColor: '#020024', // Fallback color in case the image doesn't load
},
  spinner: {
    marginTop: 20, // Space above the spinner
  },
});

export default Statistics;
