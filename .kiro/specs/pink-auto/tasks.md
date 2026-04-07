# Implementation Plan: Pink Auto

## Overview

Implement the Pink Auto ride-booking platform as a microservices backend (Node.js/TypeScript) with Android client apps (Kotlin) and a web Admin Dashboard. Tasks follow the service boundaries defined in the design: Auth → KYC → Location → Fare → Ride/Allocation → Payment → Notifications → Ratings → Admin. Each service is built incrementally and wired together at the end.

## Tasks

- [ ] 1. Project scaffolding and shared infrastructure
  - Create monorepo directory structure: `services/auth`, `services/ride`, `services/location`, `services/fare`, `services/payment`, `services/notification`, `services/kyc`, `services/driver-allocator`, `shared/`, `admin-dashboard/`
  - Define shared TypeScript types and error envelope (`{ error: { code, message, details } }`) in `shared/types.ts`
  - Set up PostgreSQL schema migrations (all `CREATE TABLE` statements from design) using a migration tool (e.g., node-pg-migrate)
  - Configure Redis connection helper in `shared/redis.ts`
  - Set up Jest + fast-check as the test framework for all backend services
  - _Requirements: 16.1_

- [ ] 2. Auth Service — OTP and JWT
  - [ ] 2.1 Implement OTP send endpoints (`POST /auth/rider/send-otp`, `POST /auth/driver/send-otp`)
    - Insert `otp_sessions` record with hashed OTP, 5-minute TTL, zero attempts
    - Trigger SMS via Notification Service stub
    - _Requirements: 1.1, 2.1_

  - [ ] 2.2 Write property test for OTP session creation (Property 1)
    - **Property 1: OTP send creates a session record**
    - **Validates: Requirements 1.1, 2.1**

  - [ ] 2.3 Write property test for SMS notification on OTP send (Property 30)
    - **Property 30: SMS created for OTP requests**
    - **Validates: Requirements 14.4**

  - [ ] 2.4 Implement OTP verify endpoints (`POST /auth/rider/verify-otp`, `POST /auth/driver/verify-otp`)
    - On correct OTP within TTL: issue JWT (1h) + refresh token (30d), return both
    - On incorrect OTP: increment attempts, return `OTP_INVALID`; lock session after 3 failures for 15 minutes
    - On expired OTP: return `OTP_EXPIRED`
    - _Requirements: 1.2, 1.3, 1.4, 2.2_

  - [ ] 2.5 Write property test for correct OTP producing JWT (Property 2)
    - **Property 2: Correct OTP produces a JWT**
    - **Validates: Requirements 1.2, 2.2**

  - [ ] 2.6 Write property test for wrong OTP rejection and lockout (Property 3)
    - **Property 3: Wrong OTP is rejected and attempt count increments**
    - **Validates: Requirements 1.3, 1.4**

  - [ ] 2.7 Implement email/password login (`POST /auth/rider/login`) and Google OAuth (`POST /auth/rider/google`)
    - Email/password: bcrypt compare, return JWT on match, `AUTH_FAILED` on mismatch
    - Google OAuth: verify Google token via Firebase Auth, upsert rider record, return JWT
    - _Requirements: 1.5, 1.6_

  - [ ] 2.8 Write property test for email/password login round-trip (Property 4)
    - **Property 4: Email/password login round-trip**
    - **Validates: Requirements 1.5**

  - [ ] 2.9 Write property test for Google OAuth login (Property 5)
    - **Property 5: Google OAuth login produces a JWT**
    - **Validates: Requirements 1.6**

  - [ ] 2.10 Implement JWT middleware and token refresh (`POST /auth/refresh`)
    - Middleware rejects expired/invalid tokens with HTTP 401 (`TOKEN_EXPIRED` / `TOKEN_INVALID`)
    - Refresh endpoint validates refresh token and issues new JWT
    - _Requirements: 1.7_

  - [ ] 2.11 Write property test for expired JWT rejection (Property 6)
    - **Property 6: Expired JWT is rejected**
    - **Validates: Requirements 1.7**

- [ ] 3. Auth Service — Profile management
  - [ ] 3.1 Implement profile endpoints (`PUT /auth/profile`, `GET /auth/profile`)
    - Rider: update name, photo_url, saved_addresses; Driver: update photo_url, contact details
    - Persist changes and return updated profile within 2 seconds
    - _Requirements: 3.1, 3.2, 3.3_

  - [ ] 3.2 Write property test for profile update round-trip (Property 10)
    - **Property 10: Profile update round-trip**
    - **Validates: Requirements 3.1, 3.2, 3.3**

- [ ] 4. Checkpoint — Auth Service
  - Ensure all Auth Service tests pass, ask the user if questions arise.

- [ ] 5. KYC Service
  - [ ] 5.1 Implement KYC document submission (`POST /kyc/submit`)
    - Upload documents to S3, store URLs in `driver_kyc`, set `kyc_status = 'under_review'`
    - Validate all required fields present (Aadhaar, License, RC, Insurance, PUC, bank, photo); PAN optional
    - Return HTTP 400 with missing field list if required docs absent
    - _Requirements: 2.4, 2.7_

  - [ ] 5.2 Write property test for KYC submission status transition (Property 8)
    - **Property 8: KYC submission transitions status to under_review**
    - **Validates: Requirements 2.4, 2.7**

  - [ ] 5.3 Implement KYC review endpoint (`PUT /kyc/:driver_id/review`) and status query (`GET /kyc/:driver_id/status`)
    - Approve: set `kyc_status = 'active'`, trigger activation notification
    - Reject: require non-null reason, set `kyc_status = 'rejected'`, store reason, trigger rejection notification
    - _Requirements: 2.5, 2.6, 15.3_

  - [ ] 5.4 Write property test for KYC approval and rejection transitions (Property 9)
    - **Property 9: KYC approval and rejection transitions**
    - **Validates: Requirements 2.5, 2.6, 15.3**

- [ ] 6. Location Service
  - [ ] 6.1 Implement driver GPS ingestion WebSocket (`WS /ws/driver/location`)
    - Accept `{ lat, lng, heading, timestamp }` from Driver App
    - Store in Redis using `GEOADD drivers` with 10-second TTL per key
    - Log coordinates to `ride_gps_log` when a ride is active
    - _Requirements: 10.1, 12.4_

  - [ ] 6.2 Implement nearby-drivers query (`GET /location/nearby-drivers`)
    - Use `GEORADIUS` to return only `is_online=true` drivers within specified radius
    - _Requirements: 4.4, 6.1_

  - [ ] 6.3 Write property test for nearby drivers query correctness (Property 11)
    - **Property 11: Nearby drivers query returns only online drivers within radius**
    - **Validates: Requirements 4.4, 6.1**

  - [ ] 6.4 Implement rider tracking WebSocket (`WS /ws/rider/track/:ride_id`)
    - Stream driver GPS updates to rider via Redis Pub/Sub at most every 3 seconds
    - Only emit updates when ride status is `DRIVER_EN_ROUTE` or `IN_PROGRESS`
    - On WebSocket disconnect: buffer last known position for 30 seconds, emit `reconnecting` event
    - _Requirements: 7.1, 7.2, 7.4_

  - [ ] 6.5 Write property test for location streaming only during active rides (Property 15)
    - **Property 15: Location streaming only during active rides**
    - **Validates: Requirements 7.1**

  - [ ] 6.6 Implement driver online/offline toggle in Auth/Driver service
    - Online: set `is_online=true`, register in Redis geo-index
    - Offline: set `is_online=false`, remove from Redis geo-index
    - Block toggle to online if `kyc_status != 'active'` or driver is blacklisted
    - _Requirements: 2.3, 10.1, 10.7_

  - [ ] 6.7 Write property test for driver online/offline state consistency (Property 22)
    - **Property 22: Driver online/offline state consistency**
    - **Validates: Requirements 10.1, 10.7**

  - [ ] 6.8 Write property test for non-active driver cannot go online (Property 7)
    - **Property 7: Non-active driver cannot go online**
    - **Validates: Requirements 2.3**

- [ ] 7. Checkpoint — Location and KYC Services
  - Ensure all Location and KYC Service tests pass, ask the user if questions arise.

- [ ] 8. Fare Engine
  - [ ] 8.1 Implement fare estimate endpoint (`POST /fare/estimate`)
    - Calculate `base_fare + (per_km_rate × distance_km) + (per_minute_rate × waiting_minutes)`
    - Apply surge multiplier when demand/supply ratio exceeds zone threshold
    - Return estimated fare, distance_km, and ETA seconds; all fields non-negative
    - Look up pricing zone by coordinates; fall back to default zone if unknown
    - _Requirements: 5.1, 8.1, 8.2_

  - [ ] 8.2 Write property test for fare formula correctness (Property 16)
    - **Property 16: Fare formula correctness**
    - **Validates: Requirements 8.1, 8.2**

  - [ ] 8.3 Write property test for fare estimate required fields (Property 12)
    - **Property 12: Fare estimate contains all required fields**
    - **Validates: Requirements 5.1**

  - [ ] 8.4 Implement coupon validation (`POST /fare/validate-coupon`) and final fare calculation (`POST /fare/final`)
    - Coupon: return discount amount or `COUPON_INVALID` / `COUPON_EXPIRED`
    - Final fare: produce `fare_breakdowns` record with all required fields (base_fare, distance_charge, waiting_charge, surge_multiplier, platform_fee, discount, total_fare)
    - _Requirements: 8.3, 8.4, 8.5_

  - [ ] 8.5 Write property test for discount and platform fee (Property 17)
    - **Property 17: Discount reduces fare and platform fee is always present**
    - **Validates: Requirements 8.3, 8.4, 8.5**

  - [ ] 8.6 Implement pricing zone configuration endpoints (via Admin API `PUT /admin/pricing/:zone_id`)
    - Persist zone config to `pricing_zones`; subsequent fare calculations use updated rates
    - _Requirements: 8.6, 15.5_

  - [ ] 8.7 Write property test for pricing config round-trip (Property 18)
    - **Property 18: Pricing config round-trip**
    - **Validates: Requirements 8.6, 15.5**

- [ ] 9. Driver Allocator
  - [ ] 9.1 Implement candidate selection logic
    - Query Redis `GEORADIUS` for online drivers within 5 km of pickup
    - Rank by `rating DESC, cancellation_rate ASC`; exclude drivers on active rides
    - _Requirements: 6.1, 6.2, 6.5_

  - [ ] 9.2 Implement sequential escalation with 30-second timeout
    - Send request to top-ranked driver; set 30-second Redis TTL key for the offer
    - On timeout or explicit rejection, escalate to next candidate
    - _Requirements: 6.3, 6.4, 10.3_

  - [ ] 9.3 Write property test for driver allocation escalation (Property 14)
    - **Property 14: Driver allocation escalation**
    - **Validates: Requirements 6.2, 6.3, 6.4, 6.5, 10.3**

- [ ] 10. Ride Service
  - [ ] 10.1 Implement ride request and cancellation endpoints (`POST /rides/request`, `POST /rides/:id/cancel`)
    - Create ride record in `REQUESTED` status; invoke Driver Allocator
    - Cancel before assignment: transition to `CANCELLED`, no fare/payment records created
    - If no driver accepts within 2 minutes: auto-cancel and notify rider
    - _Requirements: 5.2, 5.4, 5.6_

  - [ ] 10.2 Write property test for ride cancellation before assignment has no fare (Property 13)
    - **Property 13: Ride cancellation before assignment has no fare**
    - **Validates: Requirements 5.6**

  - [ ] 10.3 Implement ride state transitions (`POST /rides/:id/start`, `POST /rides/:id/end`, `GET /rides/:id`)
    - Start ride: `DRIVER_EN_ROUTE → IN_PROGRESS`, begin fare metering
    - End ride: `IN_PROGRESS → COMPLETED`, trigger Fare Engine final calculation and Payment Service
    - Reject invalid transitions with HTTP 409 `INVALID_STATE_TRANSITION`
    - _Requirements: 10.5, 10.6_

  - [ ] 10.4 Write property test for valid ride status transitions (Property 23)
    - **Property 23: Ride status transitions are valid**
    - **Validates: Requirements 10.5, 10.6**

  - [ ] 10.5 Implement ride history endpoint (`GET /rides/history`) and GPS log persistence
    - Return paginated ride history for rider or driver
    - Ensure `ride_gps_log` entries are written throughout active ride; retained ≥ 90 days
    - _Requirements: 3.4, 3.5, 12.4_

  - [ ] 10.6 Write property test for GPS log existence for every ride (Property 26)
    - **Property 26: GPS log exists for every ride**
    - **Validates: Requirements 12.4**

- [ ] 11. Checkpoint — Ride, Fare, and Allocator Services
  - Ensure all Ride, Fare, and Driver Allocator tests pass, ask the user if questions arise.

- [ ] 12. Payment Service
  - [ ] 12.1 Implement payment initiation and Razorpay webhook (`POST /payments/initiate`, `POST /payments/webhook`)
    - Create payment record with unique `gateway_txn_id`; handle idempotency key check
    - Webhook: update payment status to `success` or `failed`; on success trigger invoice generation and notification
    - On failure: store `failure_reason`, status=`failed`
    - _Requirements: 9.1, 9.2, 9.4, 9.6_

  - [ ] 12.2 Write property test for payment created on ride completion (Property 19)
    - **Property 19: Payment created on ride completion with unique transaction ID**
    - **Validates: Requirements 9.2, 9.4, 9.6**

  - [ ] 12.3 Implement invoice retrieval (`GET /payments/invoice/:ride_id`) and transaction history (`GET /payments/transactions`)
    - Invoice must exist for every payment with status=`success`
    - _Requirements: 9.3_

  - [ ] 12.4 Write property test for invoice generated on successful payment (Property 20)
    - **Property 20: Invoice generated on successful payment**
    - **Validates: Requirements 9.3**

  - [ ] 12.5 Implement refund endpoint (`POST /payments/refund`)
    - Create payment record with status=`refunded` referencing original ride
    - Reject refund on cash payments with HTTP 400 `REFUND_NOT_APPLICABLE`
    - _Requirements: 9.5_

  - [ ] 12.6 Write property test for refund record creation (Property 21)
    - **Property 21: Refund record created on approval**
    - **Validates: Requirements 9.5**

- [ ] 13. Notification Service
  - [ ] 13.1 Implement push and SMS dispatch (`POST /notify/push`, `POST /notify/sms`)
    - Send via FCM (push) and AWS SNS (SMS)
    - Log each attempt to `notification_log`; retry up to 3 times with exponential backoff (1s, 2s, 4s)
    - Mark stale FCM tokens; log final failure after 3 retries
    - _Requirements: 14.1, 14.2, 14.3, 14.4, 14.5, 14.6_

  - [ ] 13.2 Write property test for notification delivery and retry (Property 29)
    - **Property 29: Notification delivery on ride events**
    - **Validates: Requirements 14.1, 14.2, 14.3, 14.5, 14.6**

  - [ ] 13.3 Implement SOS notification endpoint (`POST /notify/sos`)
    - Send alert to all registered emergency contacts and platform support with rider name, GPS coordinates, and ride ID within 10 seconds
    - _Requirements: 12.2_

  - [ ] 13.4 Write property test for SOS notifications (Property 25)
    - **Property 25: SOS triggers notifications to all emergency contacts**
    - **Validates: Requirements 12.2**

- [ ] 14. Ratings Service
  - [ ] 14.1 Implement rating submission endpoints (rider rates driver, driver rates rider)
    - Insert into `ratings` table; recalculate driver's rolling average rating
    - If rolling average < 3.5: set admin-review flag on driver, notify Admin
    - _Requirements: 13.1, 13.2, 13.3, 13.4_

  - [ ] 14.2 Write property test for rating submission and rolling average (Property 28)
    - **Property 28: Rating submission and rolling average**
    - **Validates: Requirements 13.1, 13.2, 13.3, 13.4**

- [ ] 15. Driver Earnings Dashboard
  - [ ] 15.1 Implement earnings endpoints in Driver App API
    - Return total earnings for current day and current week (sum of `total_fare` from COMPLETED rides)
    - Return paginated completed ride list with date, pickup, destination, distance, fare
    - Update within 60 seconds of ride completion
    - _Requirements: 11.1, 11.2, 11.3_

  - [ ] 15.2 Write property test for driver earnings calculation (Property 24)
    - **Property 24: Driver earnings calculation correctness**
    - **Validates: Requirements 11.1, 11.2**

- [ ] 16. Checkpoint — Payment, Notifications, Ratings, Earnings
  - Ensure all tests pass for these services, ask the user if questions arise.

- [ ] 17. Admin Dashboard API
  - [ ] 17.1 Implement active rides list (`GET /admin/rides/active`)
    - Return all rides in non-terminal status (REQUESTED, DRIVER_ASSIGNED, DRIVER_EN_ROUTE, IN_PROGRESS) with driver location, rider details, and status
    - _Requirements: 15.1_

  - [ ] 17.2 Write property test for admin active rides completeness (Property 31)
    - **Property 31: Admin active rides list completeness**
    - **Validates: Requirements 15.1**

  - [ ] 17.3 Implement user/driver search and management (`GET /admin/users`, `PUT /admin/drivers/:id/blacklist`)
    - Search by name, phone, or email; return only matching accounts
    - Blacklist: record reason and timestamp; blocked driver cannot go online
    - _Requirements: 15.2, 12.5, 15.6_

  - [ ] 17.4 Write property test for admin user search (Property 32)
    - **Property 32: Admin user search returns matching accounts only**
    - **Validates: Requirements 15.2**

  - [ ] 17.5 Write property test for blacklisted driver cannot go online (Property 27)
    - **Property 27: Blacklisted driver cannot go online**
    - **Validates: Requirements 12.5, 15.6**

  - [ ] 17.6 Implement platform analytics endpoint (`GET /admin/analytics`)
    - Return total_rides (count of COMPLETED rides), total_revenue (sum of fares), active drivers, active riders for given date range
    - _Requirements: 15.4_

  - [ ] 17.7 Write property test for analytics correctness (Property 33)
    - **Property 33: Analytics reflect actual data for date range**
    - **Validates: Requirements 15.4**

- [ ] 18. Integration — Wire all services together
  - [ ] 18.1 Connect Ride Service → Driver Allocator → Location Service → Notification Service event chain
    - Ride request triggers allocation; assignment triggers rider notification and tracking link
    - _Requirements: 5.2, 5.3, 14.1_

  - [ ] 18.2 Connect Ride Service → Fare Engine → Payment Service → Notification Service on ride completion
    - End-ride triggers final fare calculation, payment initiation, invoice generation, and push notification
    - _Requirements: 9.2, 9.3, 14.3_

  - [ ] 18.3 Connect Ratings prompt on ride completion
    - Ride completion event triggers rating prompt in both Rider and Driver apps
    - _Requirements: 13.1, 13.2_

  - [ ] 18.4 Write integration test for end-to-end ride flow
    - Rider books → driver accepts → ride starts → ride ends → payment processed → ratings submitted
    - _Requirements: 5.1–5.6, 9.2, 13.1, 13.2_

  - [ ] 18.5 Write integration test for WebSocket lifecycle
    - Connect → receive location updates → disconnect → reconnect within 5 seconds
    - _Requirements: 7.4_

- [ ] 19. Final checkpoint — Ensure all tests pass
  - Ensure all tests pass across all services, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP
- Property tests use fast-check for Node.js/TypeScript services; Kotest for Kotlin Android services
- Each property test must run a minimum of 100 iterations and include the comment `// Feature: pink-auto, Property N: <property_text>`
- Checkpoints ensure incremental validation before moving to the next service group
