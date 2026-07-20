import { useQuery } from '@tanstack/react-query';

import type { MeResponse } from '@/src/api/types';
import { useAuth } from '@/src/features/auth/useAuth';

export function useMe() {
  const { apiClient } = useAuth();

  return useQuery({
    queryKey: ['me'],
    queryFn: () => apiClient.get<MeResponse>('/api/me'),
  });
}
