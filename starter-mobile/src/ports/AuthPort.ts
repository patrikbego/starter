export interface AuthUser {
  uid: string;
  email: string | null;
  displayName: string | null;
}

export interface AuthPort {
  signIn(email: string, password: string): Promise<void>;
  signUp(email: string, password: string): Promise<void>;
  signOut(): Promise<void>;
  getIdToken(forceRefresh?: boolean): Promise<string | null>;
  onAuthStateChanged(callback: (user: AuthUser | null) => void): () => void;
}
