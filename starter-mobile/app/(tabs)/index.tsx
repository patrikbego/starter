import { useCallback } from 'react';
import {
  ActivityIndicator,
  Pressable,
  RefreshControl,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';

import { useAuth } from '@/src/features/auth/useAuth';
import { useHealth } from '@/src/features/profile/useHealth';
import { useMe } from '@/src/features/profile/useMe';

export default function HomeScreen() {
  const { signOut } = useAuth();
  const meQuery = useMe();
  const healthQuery = useHealth();

  const onRefresh = useCallback(() => {
    void meQuery.refetch();
    void healthQuery.refetch();
  }, [meQuery, healthQuery]);

  const isRefreshing = meQuery.isRefetching || healthQuery.isRefetching;
  const isBackendUp = healthQuery.data?.status === 'UP';

  return (
    <ScrollView
      contentContainerStyle={styles.container}
      refreshControl={<RefreshControl refreshing={isRefreshing} onRefresh={onRefresh} />}>
      <Text style={styles.heading}>Welcome</Text>

      <View style={styles.card}>
        <Text style={styles.label}>Profile</Text>
        {meQuery.isLoading ? (
          <ActivityIndicator />
        ) : meQuery.isError ? (
          <Text style={styles.error}>Failed to load profile</Text>
        ) : (
          <>
            <Text style={styles.value}>{meQuery.data?.displayName || 'No name'}</Text>
            <Text style={styles.muted}>{meQuery.data?.email}</Text>
            <Text style={styles.muted}>ID: {meQuery.data?.id}</Text>
          </>
        )}
      </View>

      <View style={styles.card}>
        <Text style={styles.label}>Backend status</Text>
        {healthQuery.isLoading ? (
          <ActivityIndicator />
        ) : (
          <View style={styles.badgeRow}>
            <View
              style={[
                styles.badge,
                { backgroundColor: isBackendUp ? '#16a34a' : '#dc2626' },
              ]}
            />
            <Text style={styles.value}>
              {isBackendUp ? 'Connected' : 'Unreachable'}
            </Text>
          </View>
        )}
      </View>

      <Pressable style={styles.signOutButton} onPress={() => void signOut()}>
        <Text style={styles.signOutText}>Sign out</Text>
      </Pressable>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    padding: 24,
    gap: 16,
  },
  heading: {
    fontSize: 24,
    fontWeight: '700',
  },
  card: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    gap: 8,
    borderWidth: 1,
    borderColor: '#e5e7eb',
  },
  label: {
    fontSize: 12,
    fontWeight: '600',
    color: '#6b7280',
    textTransform: 'uppercase',
  },
  value: {
    fontSize: 18,
    fontWeight: '600',
  },
  muted: {
    fontSize: 14,
    color: '#6b7280',
  },
  badgeRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  badge: {
    width: 10,
    height: 10,
    borderRadius: 5,
  },
  error: {
    color: '#dc2626',
  },
  signOutButton: {
    marginTop: 8,
    paddingVertical: 12,
    alignItems: 'center',
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#dc2626',
  },
  signOutText: {
    color: '#dc2626',
    fontWeight: '600',
  },
});
