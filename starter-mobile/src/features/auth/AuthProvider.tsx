import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';

import { firebaseAuthAdapter } from '@/src/adapters/FirebaseAuthAdapter';
import { createHttpApiClient } from '@/src/adapters/HttpApiClient';
import type { ApiPort } from '@/src/ports/ApiPort';
import type { AuthPort, AuthUser } from '@/src/ports/AuthPort';

interface AuthContextValue {
  user: AuthUser | null;
  loading: boolean;
  signIn: (email: string, password: string) => Promise<void>;
  signUp: (email: string, password: string) => Promise<void>;
  signOut: () => Promise<void>;
  apiClient: ApiPort;
}

const AuthContext = createContext<AuthContextValue | null>(null);

interface AuthProviderProps {
  children: React.ReactNode;
  authPort?: AuthPort;
}

export function AuthProvider({ children, authPort = firebaseAuthAdapter }: AuthProviderProps) {
  const queryClient = useQueryClient();
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);

  const apiClient = useMemo(
    () =>
      createHttpApiClient({
        authPort,
        onUnauthorized: async () => {
          await authPort.signOut();
          queryClient.clear();
        },
      }),
    [authPort, queryClient],
  );

  useEffect(() => {
    const unsubscribe = authPort.onAuthStateChanged((nextUser) => {
      setUser(nextUser);
      setLoading(false);
    });

    return unsubscribe;
  }, [authPort]);

  const signIn = useCallback(
    async (email: string, password: string) => {
      await authPort.signIn(email, password);
    },
    [authPort],
  );

  const signUp = useCallback(
    async (email: string, password: string) => {
      await authPort.signUp(email, password);
    },
    [authPort],
  );

  const signOut = useCallback(async () => {
    await authPort.signOut();
    queryClient.clear();
  }, [authPort, queryClient]);

  const value = useMemo(
    () => ({
      user,
      loading,
      signIn,
      signUp,
      signOut,
      apiClient,
    }),
    [user, loading, signIn, signUp, signOut, apiClient],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
}
