# Run — Local (macOS)

Run the mobile app on your Mac using Expo. This is how developers work on the app day to day.

**Time needed:** ~20 minutes first time  
**Difficulty:** Easy

---

## Before you start

### What you will have at the end

The app running on an iOS Simulator, Android Emulator, or your physical phone — showing login, home, and chat screens.

### What you need

| Requirement | How to check | How to install |
|-------------|--------------|----------------|
| Node.js 22+ | `node -version` | `brew install node@22` |
| npm | `npm -version` | Included with Node |
| Backend running | See below | [../starter-backend/run/run-local.md](../starter-backend/run/run-local.md) |
| Firebase DEV project | Ask team lead | [Firebase Console](https://console.firebase.google.com) |

**Optional (for simulators):**
- **iOS Simulator:** install Xcode from the Mac App Store
- **Android Emulator:** install [Android Studio](https://developer.android.com/studio)
- **Physical phone:** install **Expo Go** from App Store or Play Store

### Get the code

```bash
git clone <your-repo-url> starter
cd starter/starter-mobile
```

---

## Step 1 — Start the backend first

The mobile app needs the API server running. **Do this before starting the mobile app.**

Open a terminal and follow [../starter-backend/run/run-local.md](../starter-backend/run/run-local.md) **Option A** (easiest):

```bash
cd starter/starter-backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Leave this terminal open. Confirm it works:

```bash
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```

---

## Step 2 — Install mobile dependencies

Open a **new terminal**:

```bash
cd starter/starter-mobile
npm install
```

This downloads all JavaScript packages. It may take a few minutes the first time.

---

## Step 3 — Configure environment variables

Copy the example config file:

```bash
cp .env.example .env
```

Open `.env` in a text editor and fill in the values:

```bash
APP_ENV=development

# Backend on your Mac (use this for iOS Simulator)
API_BASE_URL_DEV=http://localhost:8080

# Firebase — get these from Firebase Console (see below)
EXPO_PUBLIC_FIREBASE_API_KEY=AIza...
EXPO_PUBLIC_FIREBASE_AUTH_DOMAIN=starter-dev.firebaseapp.com
EXPO_PUBLIC_FIREBASE_PROJECT_ID=starter-dev
```

### How to get Firebase keys (first time)

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Select project **`starter-dev`** (ask team lead if you don't have access)
3. Click the gear icon → **Project settings**
4. Scroll to **Your apps** → select or create a **Web** app
5. Copy `apiKey`, `authDomain`, and `projectId` into your `.env`

### Enable email/password login (first time)

1. In Firebase Console → **Authentication** → **Sign-in method**
2. Enable **Email/Password**
3. Save

---

## Step 4 — Start the app

```bash
npx expo start
```

You will see a QR code and a menu in the terminal.

### Choose how to run

| Target | What to do |
|--------|------------|
| **iOS Simulator** (easiest on Mac) | Press `i` in the terminal |
| **Android Emulator** | Start emulator in Android Studio, then press `a` |
| **Your physical phone** | Install Expo Go, scan the QR code |

**Using a physical phone?** Change `.env` first:

```bash
# Find your Mac IP: System Settings → Network → Wi‑Fi → IP Address
API_BASE_URL_DEV=http://192.168.1.10:8080
```

Then restart: `npx expo start --clear`

Phones cannot reach `localhost` — they need your Mac's network IP.

---

## Step 5 — Verify the app works

**You are done when** you can complete this flow:

1. App opens → you see the **login screen**
2. Tap **"Need an account? Sign up"** → create a test user with any email/password
3. After sign-in → you land on the **Home** tab
4. Home shows your email and a **green** "Connected" health badge
5. Go to **Chat** tab → type "hello" → you get a reply
6. Tap **Sign out** → you return to the login screen

### If health badge is red

The app cannot reach the backend. Check:
- Backend terminal is still running (Step 1)
- `API_BASE_URL_DEV` in `.env` is correct
- On a physical phone: using LAN IP, not `localhost`

### If login fails

- Firebase keys in `.env` are correct
- Email/Password is enabled in Firebase Console
- Restart with `npx expo start --clear` after changing `.env`

### If home shows an error after login

- Backend is running on port 8080
- For `local` backend profile, any Firebase user works
- Try signing out and back in

---

## Daily workflow (after first setup)

Once configured, your daily routine is:

```bash
# Terminal 1 — backend
cd starter/starter-backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Terminal 2 — mobile
cd starter/starter-mobile
npx expo start
```

---

## Run checks without starting the app

```bash
npm run lint          # code style
npx tsc --noEmit      # type errors
```

---

## Troubleshooting

| Problem | What to do |
|---------|------------|
| `Missing required config: firebase.apiKey` | Fill `.env`, run `npx expo start --clear` |
| `npm install` fails | Check Node version: `node -version` (need 22+) |
| Simulator won't open | Install Xcode; run `xcode-select --install` |
| Expo Go can't connect | Same Wi‑Fi as Mac; try `npx expo start --tunnel` |
| 401 error on home screen | Firebase project must be `starter-dev` |
| Config changes ignored | Always use `npx expo start --clear` after editing `.env` |

---

## What comes next?

| When you are ready to… | Read |
|------------------------|------|
| Install a test build on your phone (cloud backend) | [run-dev.md](./run-dev.md) |
| Submit to App Store / Play Store | [run-prod.md](./run-prod.md) |
| Run backend in the cloud | [../starter-backend/run/run-dev.md](../starter-backend/run/run-dev.md) |
