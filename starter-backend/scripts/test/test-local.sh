#!/bin/bash
#
# Test script for 'local' profile
# This profile uses in-memory mocks - no emulators or real cloud services needed.
#
# Usage: ./scripts/test/test-local.sh
#

set -e

BASE_URL="${BASE_URL:-http://localhost:8080}"
FAKE_TOKEN="${FAKE_TOKEN:-my-local-test-token}"

echo "=========================================="
echo "Testing Starter API - Local Profile"
echo "Base URL: $BASE_URL"
echo "=========================================="
echo ""

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

print_success() { echo -e "${GREEN}✓${NC} $1"; }
print_error() { echo -e "${RED}✗${NC} $1"; }
print_info() { echo -e "${YELLOW}ℹ${NC} $1"; }

echo "1. Testing Health Endpoint (no auth required)..."
if curl -sf "$BASE_URL/actuator/health" > /dev/null 2>&1; then
    print_success "Health check passed"
    curl -s "$BASE_URL/actuator/health" | jq . 2>/dev/null || curl -s "$BASE_URL/actuator/health"
else
    print_error "Health check failed - is the server running?"
    exit 1
fi
echo ""

echo "2. Testing Authentication and /api/me (mock token)..."
USER_RESPONSE=$(curl -sf -H "Authorization: Bearer $FAKE_TOKEN" "$BASE_URL/api/me" 2>/dev/null)
if [ $? -eq 0 ]; then
    print_success "Authentication successful"
    echo "$USER_RESPONSE" | jq . 2>/dev/null || echo "$USER_RESPONSE"
    USER_ID=$(echo "$USER_RESPONSE" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
    print_info "User: $USER_ID"
else
    print_error "Authentication failed"
    exit 1
fi
echo ""

echo "3. Testing AI Chat..."
CHAT_RESPONSE=$(curl -sf -X POST \
  -H "Authorization: Bearer $FAKE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"message":"hello"}' \
  "$BASE_URL/api/chat" 2>/dev/null)

if [ $? -eq 0 ]; then
    print_success "Chat endpoint working"
    echo "$CHAT_RESPONSE" | jq . 2>/dev/null || echo "$CHAT_RESPONSE"
else
    print_error "Chat endpoint failed"
    exit 1
fi
echo ""

echo "4. Testing Multiple Users (isolation)..."
USER1_RESPONSE=$(curl -sf -H "Authorization: Bearer user1-token" "$BASE_URL/api/me" 2>/dev/null)
USER2_RESPONSE=$(curl -sf -H "Authorization: Bearer user2-token" "$BASE_URL/api/me" 2>/dev/null)

USER1_ID=$(echo "$USER1_RESPONSE" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
USER2_ID=$(echo "$USER2_RESPONSE" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)

if [ "$USER1_ID" != "$USER2_ID" ]; then
    print_success "User isolation working - different tokens = different users"
    print_info "User 1: $USER1_ID"
    print_info "User 2: $USER2_ID"
else
    print_error "User isolation failed - same user for different tokens"
fi
echo ""

echo "=========================================="
echo "Local Profile Tests Complete!"
echo "=========================================="
