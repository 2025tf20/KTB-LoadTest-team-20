# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Development Commands

```bash
# Install dependencies
npm install

# Development server (http://localhost:3000)
npm run dev

# Production build
npm run build

# Production build with standalone deployment assets
npm run build:production

# Start production server
npm run start
```

### Deployment

```bash
# Build locally for production
make build-local

# Deploy to remote servers (builds + rsync + restart)
make deploy

# Deploy to specific servers
DEPLOY_SERVERS="ktb-fe01 ktb-fe02" make deploy

# Deploy to custom path
DEPLOY_PATH=/custom/path make deploy
```

## Environment Variables

Required environment variables (see `.env.example`):
- `NEXT_PUBLIC_API_URL` - Backend REST API server (default: http://localhost:5001)
- `NEXT_PUBLIC_SOCKET_URL` - Socket.IO server (default: http://localhost:5002)

## Architecture

### Real-time Communication

The app uses Socket.IO for real-time features. The `SocketService` singleton (`services/socket.js`) manages:
- WebSocket connection with automatic fallback to polling
- Heartbeat mechanism (25s interval)
- Message queuing for offline resilience
- Reconnection logic with exponential backoff
- Reaction event handling via `onReactionUpdate()` subscription pattern

### Authentication

`AuthContext` (`contexts/AuthContext.js`) provides global auth state:
- Token-based authentication with sessionId
- 2-hour session timeout with localStorage persistence
- HOC patterns: `withAuth` (protected routes), `withoutAuth` (login/register only)
- Token verification every 5 minutes with automatic refresh

### Custom Hooks

All hooks export from `hooks/index.js`:
- `useChatRoom` - Main chat room orchestrator
- `useSocketHandling` - Socket connection management
- `useMessageHandling` - Message send/receive logic
- `useFileHandling` - File upload/download
- `useReactionHandling` - Emoji reactions
- `useRoomHandling` - Room join/leave/create
- `useInfiniteScroll` - Paginated message loading
- `useScrollRestoration` - Scroll position preservation
- `useAutoScroll` - Auto-scroll on new messages

### Services Layer

- `services/socket.js` - SocketService singleton for WebSocket management
- `services/authService.js` - Authentication API calls
- `services/axios.js` - Configured Axios instance
- `services/fileService.js` - File upload/download operations

### UI Components

Uses Vapor UI design system (`@vapor-ui/core`, `@vapor-ui/icons`) with Tailwind CSS v4. Key components:
- `ChatMessages.js` + `UserMessage.js` + `SystemMessage.js` - Message rendering
- `ChatInput.js` - Message composition with mentions and file attachment
- `EmojiPicker.js` - Emoji selection (@emoji-mart/react)
- `MentionDropdown.js` - @mention autocomplete

### Routing (Next.js Pages Router)

- `/` - Login page
- `/register` - Registration
- `/profile` - User profile
- `/chat` - Chat room list
- `/chat/new` - Create room
- `/chat/[room]` - Dynamic chat room page

## Build Configuration

Next.js configured with:
- `output: 'standalone'` for Docker/deployment
- `transpilePackages` for Vapor UI monorepo dependencies
- `reactStrictMode: false` (temporarily disabled)
