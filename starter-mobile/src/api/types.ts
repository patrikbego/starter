export interface MeResponse {
  id: string;
  email: string;
  displayName: string;
  createdAt: string;
}

export interface ChatRequest {
  message: string;
  sessionId?: string;
}

export interface ChatResponse {
  reply: string;
  sessionId: string;
}

export interface HealthResponse {
  status: string;
}
