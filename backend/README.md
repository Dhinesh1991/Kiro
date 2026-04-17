# Pink Auto Backend (Microservices Scaffold)

This backend folder provides a spec-aligned microservice scaffold for Pink Auto.

## Services

- `services/auth` - OTP, email, Google-style auth and profile
- `services/ride` - ride request, cancel, start, end, history
- `services/admin` - analytics, pricing updates, blacklist APIs
- `services/location` - driver location updates and nearby queries
- `services/fare` - fare estimate/final and pricing config
- `services/payment` - initiate, invoice, refund, transaction history
- `services/kyc` - submit/review/status
- `services/notification` - push, sms, sos logs
- `services/driver-allocator` - candidate selection and assignment
- `services/gateway` - single API entry point
- `packages/shared` - common types and error envelope

## Local Ports

- Auth: `http://localhost:4001`
- Ride: `http://localhost:4002`
- Admin: `http://localhost:4003`
- Location: `http://localhost:4004`
- Fare: `http://localhost:4005`
- Payment: `http://localhost:4006`
- KYC: `http://localhost:4007`
- Notification: `http://localhost:4008`
- Allocator: `http://localhost:4009`
- Gateway: `http://localhost:4000`

## Run

1. `cd backend`
2. Install Node.js + npm if not available on your machine.
3. `npm install`
4. Start each service:
   - `npm run dev -w @pink-auto/auth-service`
   - `npm run dev -w @pink-auto/ride-service`
   - `npm run dev -w @pink-auto/admin-service`
   - `npm run dev -w @pink-auto/gateway-service`

## Docker

If npm is unavailable locally, run all services via Docker:

1. Install Docker Desktop
2. `cd backend`
3. `docker compose up`

## Database

Use `database/schema.sql` as initial migration baseline for PostgreSQL.
