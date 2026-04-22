import { DarkTheme, DefaultTheme, ThemeProvider } from '@react-navigation/native';
import { useFonts } from 'expo-font';
import { Stack, useRouter, useSegments } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';
import { useEffect } from 'react';
import 'react-native-reanimated';
import './global.css';
import { useColorScheme } from '@/components/useColorScheme';
import { registerProximityTask } from '@/utils/proximityTask';
import { AuthProvider, useAuth } from '@/context/AuthContext';

export {
  // Catch any errors thrown by the Layout component.
  ErrorBoundary,
} from 'expo-router';

export const unstable_settings = {
  // Ensure that reloading on `/modal` keeps a back button present.
  initialRouteName: '(tabs)',
};

// Prevent the splash screen from auto-hiding before asset loading is complete.
SplashScreen.preventAutoHideAsync();

export default function RootLayout() {
  return (
    <AuthProvider>
      <RootLayoutNav />
    </AuthProvider>
  );
}

function RootLayoutNav() {
  const colorScheme = useColorScheme();
  const { anonymousId, token, isLoading } = useAuth();
  const segments = useSegments();
  const router = useRouter();
  
  const [loaded, error] = useFonts({
    SpaceMono: require('../assets/fonts/SpaceMono-Regular.ttf'),
  });

  // Expo Router uses Error Boundaries to catch errors in the navigation tree.
  useEffect(() => {
    if (error) throw error;
  }, [error]);

  useEffect(() => {
    if (loaded) {
      SplashScreen.hideAsync();
      registerProximityTask();
    }
  }, [loaded]);

  useEffect(() => {
    console.log('RootLayoutNav: Auth State Change', { anonymousId, token, isLoading });
    console.log('RootLayoutNav: Segments', segments);
    
    if (isLoading || !loaded) return;

    const inAuthGroup = segments[0] === '(tabs)';

    if (!anonymousId && !token && inAuthGroup) {
      console.log('RootLayoutNav: Redirecting to /login');
      router.replace('/login');
    } else if (anonymousId && token && !inAuthGroup && segments[0] !== 'visitor') {
      console.log('RootLayoutNav: Redirecting to /(tabs)');
      router.replace('/(tabs)');
    }
  }, [anonymousId, token, isLoading, segments, loaded]);

  if (!loaded) {
    return null;
  }

  return (
    <ThemeProvider value={colorScheme === 'dark' ? DarkTheme : DefaultTheme}>
      <Stack>
        <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
        <Stack.Screen name="enroll" options={{ headerShown: false, animation: 'fade' }} />
        <Stack.Screen name="login" options={{ headerShown: false, presentation: 'modal' }} />
        <Stack.Screen name="visitor" options={{ headerShown: false }} />
        <Stack.Screen name="modal" options={{ presentation: 'modal' }} />
      </Stack>
    </ThemeProvider>
  );
}
