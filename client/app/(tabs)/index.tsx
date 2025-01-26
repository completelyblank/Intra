import { Link } from 'expo-router';
import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  Image,
  TouchableOpacity,
  ImageBackground,
} from 'react-native';

const Home = () => {
  const handleButtonPress = () => {
    console.log('Button Pressed!');
  };

  return (
    <ImageBackground
      source={require('@/assets/images/intra_home.jpg')} // Background image path
      style={styles.background}
      resizeMode="cover"
    >
      <View style={styles.container}>
        {/* Logo */}
        <Image
          source={require('@/assets/images/intra.png')} // Logo path
          style={styles.logo}
          resizeMode="contain"
        />

        {/* Buttons */}
        <TouchableOpacity
          style={styles.button}
          onPress={handleButtonPress}
        >
           <Link href="/" style={styles.link}>Scan</Link>
        </TouchableOpacity>
        <TouchableOpacity
          style={styles.button}
          onPress={handleButtonPress}
        >
           <Link href="/" style={styles.link}>Statistics</Link>
        </TouchableOpacity>
        <TouchableOpacity
          style={styles.button}
          onPress={handleButtonPress}
        >
           <Link href="/" style={styles.link}>Creators</Link>
        </TouchableOpacity>
      </View>
    </ImageBackground>
  );
};

const styles = StyleSheet.create({
  background: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'rgba(20, 20, 20, 0.8)', // Semi-transparent dark background for content
    width: '100%',
    padding: 20,
  },
  logo: {
    width: 200,
    height: 100,
    marginBottom: 30, // Increased spacing between logo and buttons
  },
  button: {
    backgroundColor: 'linear-gradient(90deg,rgb(12, 42, 42),rgb(26, 186, 186))', // Teal gradient
    shadowColor: 'rgba(5, 255, 255, 0.7)', // Softer teal shadow
    shadowOffset: { width: 0, height: 6 }, // More elevated shadow
    shadowOpacity: 0.5, // Subtle shadow opacity
    shadowRadius: 12, // Smoother shadow spread
    paddingVertical: 14, // Good padding for the button height
    borderRadius: 14, // Slightly more rounded corners
    elevation: 10, // Elevated for depth
    marginBottom: 20, // Better spacing between buttons
    width: '75%', // Balanced button width for consistency
    alignItems: 'center', // Centers text and content
    justifyContent: 'center', // Centers content vertically
  },
  buttonText: {
    color: '#ffffff', // White text for contrast
    fontWeight: '600', // Medium-bold text
    fontSize: 16, // Perfectly readable font size
    textShadowColor: 'rgba(0, 0, 0, 0.3)', // Subtle text shadow for emphasis
    textShadowOffset: { width: 0, height: 2 },
    textShadowRadius: 4,
  },
  link: {
    textDecorationLine: 'none', // Removes underline
    color: 'rgba(255, 255, 255, 0.75)', // Matches button text
    fontSize: 18,
    fontFamily: 'Poppins',
    textAlign: 'center',
  },
  buttonPressed: {
    transform: [{ scale: 0.96 }], // Slight press effect on interaction
    opacity: 0.9, // Dimmed on press
  },
});

export default Home;
