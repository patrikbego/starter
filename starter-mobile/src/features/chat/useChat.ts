import { useMutation } from '@tanstack/react-query';

import type { ChatRequest, ChatResponse } from '@/src/api/types';
import { useAuth } from '@/src/features/auth/useAuth';

export function useSendChat() {
  const { apiClient } = useAuth();

  return useMutation({
    mutationFn: (body: ChatRequest) =>
      apiClient.post<ChatResponse>('/api/chat', body),
  });
}
