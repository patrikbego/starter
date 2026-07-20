import { initializeApp, getApps, getApp } from 'firebase/app';
import {
  getAuth,
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
  signOut as firebaseSignOut,
  onAuthStateChanged as firebaseOnAuthStateChanged,
} from 'firebase/auth';

import { config } from '@/src/config/env';
import type { AuthPort, AuthUser } from '@/src/ports/AuthPort';

function getFirebaseApp() {
  if (getApps().length > 0) {
    return getApp();
  }
  return initializeApp(config.firebase);
}

function mapUser(user: import('firebase/auth').User): AuthUser {
  return {
    uid: user.uid,
    email: user.email,
    displayName: user.displayName,
  };
}

export function createFirebaseAuthAdapter(): AuthPort {
  const auth = getAuth(getFirebaseApp());

  return {
    async signIn(email: string, password: string) {
      await signInWithEmailAndPassword(auth, email, password);
    },

    async signUp(email: string, password: string) {
      await createUserWithEmailAndPassword(auth, email, password);
    },

    async signOut() {
      await firebaseSignOut(auth);
    },

    async getIdToken(forceRefresh = false) {
      const user = auth.currentUser;
      if (!user) {
        return null;
      }
      return user.getIdToken(forceRefresh);
    },

    onAuthStateChanged(callback) {
      return firebaseOnAuthStateChanged(auth, (user) => {
        callback(user ? mapUser(user) : null);
      });
    },
  };
}

export const firebaseAuthAdapter = createFirebaseAuthAdapter();
