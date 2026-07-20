import { useQuery } from '@tanstack/react-query';

import type { HealthResponse } from '@/src/api/types';
import { config } from '@/src/config/env';

export function useHealth() {
  return useQuery({
    queryKey: ['health'],
    queryFn: async (): Promise<HealthResponse> => {
      const response = await fetch(`${config.apiBaseUrl}/actuator/health`);
      if (!response.ok) {
        throw new Error('Health check failed');
      }
      return response.json();
    },
    refetchInterval: 30_000,
  });
}
