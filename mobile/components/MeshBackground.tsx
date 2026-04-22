import React, { useMemo, useEffect } from 'react';
import { StyleSheet, View, Dimensions, Platform } from 'react-native';
import Svg, { Line, Circle, Defs, RadialGradient, Stop, Rect } from 'react-native-svg';
import Animated, { 
  useAnimatedProps, 
  useSharedValue, 
  withRepeat, 
  withTiming, 
  Easing,
  interpolate
} from 'react-native-reanimated';

const { width, height } = Dimensions.get('window');

const AnimatedCircle = Animated.createAnimatedComponent(Circle);
const AnimatedLine = Animated.createAnimatedComponent(Line);

interface Node {
  id: number;
  x: number;
  y: number;
  targetX: number;
  targetY: number;
}

interface MeshBackgroundProps {
  confirmedCount: number;
  unconfirmedCount: number;
}

const isWeb = Platform.OS === 'web';
const NODE_COUNT = isWeb ? 5 : 15; // Throttled on web for performance
const NOISE_COUNT = isWeb ? 10 : 40;

export const MeshBackground: React.FC<MeshBackgroundProps> = ({ confirmedCount, unconfirmedCount }) => {
  // Generate random base nodes for the mesh
  const nodes = useMemo(() => {
    return Array.from({ length: NODE_COUNT }).map((_, i) => ({
      id: i,
      x: Math.random() * width,
      y: Math.random() * height,
      targetX: Math.random() * width,
      targetY: Math.random() * height,
    }));
  }, []);

  // Shared values for animation
  const progress = useSharedValue(0);

  useEffect(() => {
    progress.value = withRepeat(
      withTiming(1, { duration: 15000, easing: Easing.inOut(Easing.sin) }),
      -1,
      true
    );
  }, []);

  // Map unconfirmed count to "noise" intensity (opacity of small dots)
  const noiseOpacity = Math.min(unconfirmedCount * 0.05, 0.3);
  
  // Map confirmed count to line visibility
  const lineOpacity = Math.min(confirmedCount * 0.1, 0.8);

  return (
    <View style={styles.container}>
      <Svg width="100%" height="100%" style={StyleSheet.absoluteFill}>
        <Defs>
          <RadialGradient id="grad" cx="50%" cy="50%" rx="50%" ry="50%">
            <Stop offset="0%" stopColor="#0891B2" stopOpacity="0.1" />
            <Stop offset="100%" stopColor="#000000" stopOpacity="0" />
          </RadialGradient>
        </Defs>

        {/* Pure Black Background */}
        <Rect width="100%" height="100%" fill="#000000" />

        {/* Subtle Background Glow */}
        <Circle cx={width / 2} cy={height / 2} r={width * 0.8} fill="url(#grad)" />

        {/* Unconfirmed "Noise" Particles - Random Static Dots */}
        {unconfirmedCount > 0 && Array.from({ length: NOISE_COUNT }).map((_, i) => (
          <Circle
            key={`noise-${i}`}
            cx={Math.random() * width}
            cy={Math.random() * height}
            r={1}
            fill="#0891B2"
            opacity={Math.random() * noiseOpacity}
          />
        ))}

        {/* Confirmed Geometric Mesh */}
        {nodes.map((node, i) => {
          // Find 2 closest neighbors to draw lines to, if confirmedCount permits
          const neighbors = nodes
            .filter(n => n.id !== node.id)
            .sort((a, b) => {
              const d1 = Math.pow(a.x - node.x, 2) + Math.pow(a.y - node.y, 2);
              const d2 = Math.pow(b.x - node.x, 2) + Math.pow(b.y - node.y, 2);
              return d1 - d2;
            })
            .slice(0, 2);

          return (
            <React.Fragment key={`node-group-${i}`}>
              {/* Lines between nodes */}
              {confirmedCount > 0 && neighbors.map((neighbor, ni) => (
                <Line
                  key={`line-${i}-${ni}`}
                  x1={node.x}
                  y1={node.y}
                  x2={neighbor.x}
                  y2={neighbor.y}
                  stroke="#0891B2"
                  strokeWidth="0.5"
                  opacity={lineOpacity * 0.4}
                />
              ))}

              {/* Node itself */}
              <Circle
                cx={node.x}
                cy={node.y}
                r="1.5"
                fill="#0891B2"
                opacity={lineOpacity + 0.1}
              />
            </React.Fragment>
          );
        })}
      </Svg>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: '#000000',
    zIndex: -1,
    // Force GPU acceleration on web for smoother animations
    ...Platform.select({
      web: {
        transform: [{ translateZ: 0 }] as any
      },
      default: {}
    })
  },
});
