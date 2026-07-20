import { ExpoConfig, ConfigContext } from 'expo/config';

const appEnv = process.env.APP_ENV ?? 'development';
const apiBaseUrl =
  appEnv === 'production'
    ? process.env.API_BASE_URL_PROD
    : (process.env.API_BASE_URL_DEV ?? 'http://localhost:8080');

export default ({ config }: ConfigContext): ExpoConfig => ({
  ...config,
  name: 'starter-mobile',
  slug: 'starter-mobile',
  version: '1.0.0',
  orientation: 'portrait',
  icon: './assets/images/icon.png',
  scheme: 'startermobile',
  userInterfaceStyle: 'automatic',
  newArchEnabled: true,
  ios: {
    supportsTablet: true,
    bundleIdentifier: 'com.starter.mobile',
  },
  android: {
    package: 'com.starter.mobile',
    adaptiveIcon: {
      backgroundColor: '#E6F4FE',
      foregroundImage: './assets/images/android-icon-foreground.png',
      backgroundImage: './assets/images/android-icon-background.png',
      monochromeImage: './assets/images/android-icon-monochrome.png',
    },
    edgeToEdgeEnabled: true,
  },
  web: {
    output: 'static',
    favicon: './assets/images/favicon.png',
  },
  plugins: [
    'expo-router',
    [
      'expo-splash-screen',
      {
        image: './assets/images/splash-icon.png',
        imageWidth: 200,
        resizeMode: 'contain',
        backgroundColor: '#ffffff',
        dark: {
          backgroundColor: '#000000',
        },
      },
    ],
  ],
  experiments: {
    typedRoutes: true,
    reactCompiler: true,
  },
  extra: {
    appEnv,
    apiBaseUrl,
    firebase: {
      apiKey: process.env.EXPO_PUBLIC_FIREBASE_API_KEY,
      authDomain: process.env.EXPO_PUBLIC_FIREBASE_AUTH_DOMAIN,
      projectId: process.env.EXPO_PUBLIC_FIREBASE_PROJECT_ID,
    },
    router: {},
    eas: {
      projectId: process.env.EAS_PROJECT_ID,
    },
  },
});
