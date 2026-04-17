# Pink Auto Product Workspace

This repository now contains:

- Android Rider/Driver client (`app/`)
- Backend microservices scaffold (`backend/`)
- Admin dashboard web starter (`admin-dashboard/`)

## Android Production Hardening Included

- Token-based Retrofit auth interceptor
- BuildConfig-driven API and WebSocket base URLs
- Room persistence for rides and DataStore for session
- WebSocket driver tracking client with fallback stream
- Role-based Rider/Driver/Admin feature surfaces
- Safety actions (SOS + tracking share), ratings, earnings, analytics
- Unit test scaffold for fare engine formula (`FareCalculatorTest`)

## Implemented Scope

- Production-style app layering: `presentation -> repository -> local/remote data`
- Remote networking scaffold with Retrofit + Moshi (`PinkAutoApi`)
- Local persistence with Room (`RideEntity`, `RideDao`, `AppDatabase`)
- Session persistence with DataStore (`SessionStore`)
- Rider and Driver login flow (OTP-driven with offline fallback behavior)
- Driver KYC submission flow with status transitions (`PENDING -> UNDER_REVIEW`)
- Rider booking flow with fare breakdown and ride lifecycle (`ASSIGNED -> IN_PROGRESS -> COMPLETED`)
- Driver dashboard for active rides and ride actions
- Live tracking integration point (`LocationSocketClient`) for WebSocket-based GPS updates

## Project Structure

- `app/src/main/java/com/pinkauto/app/MainActivity.kt` - app entry point
- `app/src/main/java/com/pinkauto/app/PinkAutoApp.kt` - Compose screens and UI flow
- `app/src/main/java/com/pinkauto/app/PinkAutoViewModel.kt` - presentation state and actions
- `app/src/main/java/com/pinkauto/app/data/PinkAutoRepository.kt` - in-memory service simulation
- `app/src/main/java/com/pinkauto/app/domain/Models.kt` - domain models and enums

## Build And Run

1. Open the folder in Android Studio.
2. Let Gradle sync.
3. Run on emulator/device (API 24+).

## Local Integration

- Android API base URL is configured for emulator: `http://10.0.2.2:4000/`.
- Start backend gateway and services (`backend/README.md`).
- Open `admin-dashboard/index.html` in browser after backend is running.

## Demo Credentials

- Phone: any 10+ digit value
- OTP: `123456`