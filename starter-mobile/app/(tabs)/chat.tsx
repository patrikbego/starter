import { useState } from 'react';
import {
  ActivityIndicator,
  FlatList,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';

import { ApiError, NetworkError } from '@/src/ports/ApiPort';
import { useSendChat } from '@/src/features/chat/useChat';

interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  text: string;
}

function getChatErrorMessage(error: unknown): string {
  if (error instanceof NetworkError) {
    return 'No connection. Check your network and try again.';
  }
  if (error instanceof ApiError) {
    if (error.status === 502) {
      return 'AI service unavailable. Try again later.';
    }
    if (error.status === 429) {
      return 'Too many requests. Please wait and try again.';
    }
    return 'Something went wrong. Please try again.';
  }
  return 'Something went wrong. Please try again.';
}

export default function ChatScreen() {
  const sendChat = useSendChat();
  const [input, setInput] = useState('');
  const [sessionId, setSessionId] = useState<string | undefined>();
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [error, setError] = useState<string | null>(null);

  async function handleSend() {
    const message = input.trim();
    if (!message || sendChat.isPending) {
      return;
    }

    setError(null);
    setInput('');

    const userMessage: ChatMessage = {
      id: `${Date.now()}-user`,
      role: 'user',
      text: message,
    };
    setMessages((current) => [...current, userMessage]);

    try {
      const response = await sendChat.mutateAsync({
        message,
        sessionId,
      });

      setSessionId(response.sessionId);
      setMessages((current) => [
        ...current,
        {
          id: `${Date.now()}-assistant`,
          role: 'assistant',
          text: response.reply,
        },
      ]);
    } catch (err) {
      setError(getChatErrorMessage(err));
    }
  }

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      keyboardVerticalOffset={Platform.OS === 'ios' ? 90 : 0}>
      <FlatList
        data={messages}
        keyExtractor={(item) => item.id}
        contentContainerStyle={styles.messageList}
        ListEmptyComponent={
          <Text style={styles.empty}>Send a message to start chatting with AI.</Text>
        }
        renderItem={({ item }) => (
          <View
            style={[
              styles.messageBubble,
              item.role === 'user' ? styles.userBubble : styles.assistantBubble,
            ]}>
            <Text
              style={[
                styles.messageText,
                item.role === 'user' ? styles.userText : styles.assistantText,
              ]}>
              {item.text}
            </Text>
          </View>
        )}
      />

      {error ? <Text style={styles.error}>{error}</Text> : null}

      <View style={styles.inputRow}>
        <TextInput
          style={styles.input}
          placeholder="Type a message..."
          value={input}
          onChangeText={setInput}
          editable={!sendChat.isPending}
          onSubmitEditing={() => void handleSend()}
        />
        <Pressable
          style={[styles.sendButton, sendChat.isPending && styles.sendButtonDisabled]}
          onPress={() => void handleSend()}
          disabled={sendChat.isPending}>
          {sendChat.isPending ? (
            <ActivityIndicator color="#fff" size="small" />
          ) : (
            <Text style={styles.sendButtonText}>Send</Text>
          )}
        </Pressable>
      </View>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f9fafb',
  },
  messageList: {
    padding: 16,
    gap: 8,
    flexGrow: 1,
  },
  empty: {
    textAlign: 'center',
    color: '#6b7280',
    marginTop: 48,
  },
  messageBubble: {
    maxWidth: '80%',
    borderRadius: 12,
    padding: 12,
    marginBottom: 8,
  },
  userBubble: {
    alignSelf: 'flex-end',
    backgroundColor: '#2563eb',
  },
  assistantBubble: {
    alignSelf: 'flex-start',
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: '#e5e7eb',
  },
  messageText: {
    fontSize: 16,
  },
  userText: {
    color: '#fff',
  },
  assistantText: {
    color: '#111827',
  },
  error: {
    color: '#dc2626',
    paddingHorizontal: 16,
    paddingBottom: 8,
  },
  inputRow: {
    flexDirection: 'row',
    gap: 8,
    padding: 16,
    borderTopWidth: 1,
    borderTopColor: '#e5e7eb',
    backgroundColor: '#fff',
  },
  input: {
    flex: 1,
    borderWidth: 1,
    borderColor: '#d1d5db',
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 16,
  },
  sendButton: {
    backgroundColor: '#2563eb',
    borderRadius: 8,
    paddingHorizontal: 16,
    justifyContent: 'center',
    alignItems: 'center',
  },
  sendButtonDisabled: {
    opacity: 0.7,
  },
  sendButtonText: {
    color: '#fff',
    fontWeight: '600',
  },
});
