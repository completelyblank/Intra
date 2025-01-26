import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  Animated,
  TouchableOpacity,
  Image,
  ImageBackground,
} from 'react-native';

const Creators = () => {
  const [currentIndex, setCurrentIndex] = useState(0); // Track the current creator
  const creators = [
    { name: 'Muhammad Raza', role: 'Lead Developer', image: require('@/assets/images/hacker.png') },
    { name: 'Amna Shah', role: 'UI/UX Designer', image: require('@/assets/images/hacker.png') },
    { name: 'Falah Zainab', role: 'Backend Specialist', image: require('@/assets/images/hacker.png') },
  ];

  const animatedValue = new Animated.Value(0); // For animating the card flip

  const flipCard = (direction: string) => {
    const newIndex =
      direction === 'next'
        ? (currentIndex + 1) % creators.length
        : (currentIndex - 1 + creators.length) % creators.length;
    setCurrentIndex(newIndex);

    // Start animation to flip the card
    Animated.timing(animatedValue, {
      toValue: 180,
      duration: 500,
      useNativeDriver: true,
    }).start(() => {
      animatedValue.setValue(0); // Reset animation after flip
    });
  };

  const rotateInterpolate = animatedValue.interpolate({
    inputRange: [0, 180],
    outputRange: ['0deg', '180deg'],
  });

  const cardStyle = {
    transform: [{ rotateY: rotateInterpolate }],
  };

  const creator = creators[currentIndex];

  return (
    <ImageBackground
      source={require('@/assets/images/create.jpg')} // Hacker-style background image
      style={styles.background}
      resizeMode="cover"
    >
      <View style={styles.container}>
        <Text style={styles.heading}>Cʀᴇᴀᴛᴏʀs</Text>

        <Animated.View style={[styles.card, cardStyle]}>
          <Image source={creator.image} style={styles.image} />
          <Text style={styles.creatorName}>{creator.name}</Text>
          <Text style={styles.role}>{creator.role}</Text>
        </Animated.View>

        <View style={styles.navigation}>
          <TouchableOpacity onPress={() => flipCard('prev')}>
            <Text style={styles.arrow}>←</Text>
          </TouchableOpacity>
          <TouchableOpacity onPress={() => flipCard('next')}>
            <Text style={styles.arrow}>→</Text>
          </TouchableOpacity>
        </View>

        <Text style={styles.text}>
          Meet the minds behind the project.
        </Text>
      </View>
    </ImageBackground>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  heading: {
    fontSize: 28,
    fontWeight: 'bold',
    color: '#00ffee', // Neon teal
    fontFamily: 'Courier New', // Monospace font
    textShadowColor: '#00ffff',
    textShadowOffset: { width: 0, height: 0 },
    textShadowRadius: 15,
    marginBottom: 20,
  },
  card: {
    width: 250,
    height: 350,
    borderRadius: 10,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 20,
    shadowColor: '#00ffee',
    shadowOpacity: 0.9,
    shadowRadius: 20,
    shadowOffset: { width: 0, height: 5 },
    backgroundColor: 'rgba(0, 0, 0, 0.5)', // Transparent black for the card
  },
  background: {
    flex: 1,
    width: '100%',
    height: '100%',
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#000000', // Pure black fallback
  },
  image: {
    width: 100,
    height: 100,
    borderRadius: 50,
    marginBottom: 15,
    borderWidth: 2,
    borderColor: '#00ffee', // Neon teal border
  },
  creatorName: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#00ffee', // Neon teal
    fontFamily: 'Courier New',
    textShadowColor: '#00ffff',
    textShadowOffset: { width: 0, height: 0 },
    textShadowRadius: 10,
  },
  role: {
    fontSize: 16,
    color: '#ffffff', // White for contrast
    fontFamily: 'Courier New',
  },
  navigation: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    width: '60%',
    marginBottom: 20,
  },
  arrow: {
    fontSize: 30,
    color: '#ffffff',
    fontFamily: 'Courier New',
    textShadowColor: '#00ffee',
    textShadowOffset: { width: 0, height: 0 },
    textShadowRadius: 10,
  },
  text: {
    fontSize: 16,
    color: 'rgba(0, 255, 255, 0.8)', // Transparent neon teal
    fontFamily: 'Courier New',
    textAlign: 'center',
  },
});

export default Creators;
