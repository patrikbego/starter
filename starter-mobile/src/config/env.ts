import Constants from 'expo-constants';

export interface FirebaseConfig {
  apiKey: string;
  authDomain: string;
  projectId: string;
}

export interface AppConfig {
  appEnv: string;
  apiBaseUrl: string;
  firebase: FirebaseConfig;
}

function requireConfig<T>(value: T | undefined, name: string): T {
  if (value === undefined || value === null || value === '') {
    throw new Error(
      `Missing required config: ${name}. Set it via app.config.ts / EAS env vars.`,
    );
  }
  return value;
}

const extra = Constants.expoConfig?.extra ?? {};

export const config: AppConfig = {
  appEnv: (extra.appEnv as string) ?? 'development',
  apiBaseUrl: requireConfig(extra.apiBaseUrl as string | undefined, 'apiBaseUrl'),
  firebase: {
    apiKey: requireConfig(
      (extra.firebase as FirebaseConfig | undefined)?.apiKey,
      'firebase.apiKey',
    ),
    authDomain: requireConfig(
      (extra.firebase as FirebaseConfig | undefined)?.authDomain,
      'firebase.authDomain',
    ),
    projectId: requireConfig(
      (extra.firebase as FirebaseConfig | undefined)?.projectId,
      'firebase.projectId',
    ),
  },
};
