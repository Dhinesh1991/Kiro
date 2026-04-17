# Requirements Document

## Introduction

Pink Auto is a ride-booking platform that connects passengers with nearby auto-rickshaw drivers. The platform consists of three applications: a Rider app, a Driver app, and an Admin dashboard. It supports real-time ride booking, live driver tracking, secure authentication, smart fare calculation, seamless payments, driver onboarding with KYC verification, and safety-first features.

## Glossary

- **Rider**: A registered customer who books rides through the Rider App
- **Driver**: A registered and KYC-verified auto driver who accepts ride requests via the Driver App
- **Admin**: A platform operator who manages users, drivers, rides, and configuration via the Admin Dashboard
- **Ride_Request**: A request initiated by a Rider specifying a pickup location and destination
- **Ride**: A confirmed trip assigned to a Driver after accepting a Ride_Request
- **Auth_Service**: The service responsible for authentication and session management
- **Ride_Service**: The service responsible for managing ride lifecycle from booking to completion
- **Location_Service**: The service responsible for real-time GPS tracking and driver proximity calculations
- **Fare_Engine**: The component that calculates fares based on distance, time, demand, and pricing rules
- **Payment_Service**: The service responsible for processing payments, refunds, and invoices
- **Notification_Service**: The service responsible for sending push notifications and SMS alerts
- **KYC_Service**: The service responsible for managing driver document submission and verification
- **Driver_Allocator**: The component that finds and assigns available drivers to ride requests
- **OTP**: One-Time Password used for mobile number verification
- **KYC**: Know Your Customer — the document verification process for drivers
- **Surge_Multiplier**: A dynamic pricing factor applied during peak demand periods
- **ETA**: Estimated Time of Arrival
- **SOS**: Emergency alert feature that notifies emergency contacts and platform support

---

## Requirements

### Requirement 1: Rider Authentication

**User Story:** As a Rider, I want to register and log in using my mobile number, email, or Google account, so that I can securely access the platform.

#### Acceptance Criteria

1. WHEN a Rider submits a valid mobile number, THE Auth_Service SHALL send an OTP to that number within 10 seconds
2. WHEN a Rider submits the correct OTP within 5 minutes of generation, THE Auth_Service SHALL create an authenticated session and return a JWT token
3. IF a Rider submits an incorrect OTP, THEN THE Auth_Service SHALL return an error message and allow up to 3 retry attempts
4. IF a Rider exceeds 3 failed OTP attempts, THEN THE Auth_Service SHALL lock the OTP session for 15 minutes
5. WHEN a Rider submits valid email and password credentials, THE Auth_Service SHALL authenticate the Rider and return a JWT token
6. WHERE Google Sign-In is enabled, THE Auth_Service SHALL authenticate the Rider using a valid Google OAuth token and return a JWT token
7. WHEN an authenticated session token expires, THE Auth_Service SHALL reject subsequent requests and require re-authentication

---

### Requirement 2: Driver Authentication and KYC Verification

**User Story:** As a Driver, I want to register with my mobile number and complete KYC verification, so that I can be activated on the platform and accept rides.

#### Acceptance Criteria

1. WHEN a Driver submits a valid mobile number, THE Auth_Service SHALL send an OTP to that number within 10 seconds
2. WHEN a Driver submits the correct OTP, THE Auth_Service SHALL create a pending account and prompt the Driver to complete KYC
3. WHILE a Driver's KYC status is pending or rejected, THE Driver_App SHALL restrict the Driver from going online or accepting rides
4. WHEN a Driver submits KYC documents (Aadhaar Card, Driving License, RC, Vehicle Insurance, PUC, bank account details, and profile photo), THE KYC_Service SHALL store the documents and set the Driver's status to "under review"
5. WHEN an Admin approves a Driver's KYC submission, THE KYC_Service SHALL set the Driver's status to "active" and THE Notification_Service SHALL send an activation notification to the Driver
6. IF an Admin rejects a Driver's KYC submission, THEN THE KYC_Service SHALL set the Driver's status to "rejected" and THE Notification_Service SHALL send a rejection notification with the reason to the Driver
7. THE KYC_Service SHALL accept PAN Card as an optional document during Driver registration

---

### Requirement 3: User Profile Management

**User Story:** As a Rider or Driver, I want to manage my profile information, so that my account details remain accurate and up to date.

#### Acceptance Criteria

1. THE Auth_Service SHALL allow a Rider to update their name, profile picture, and saved addresses
2. THE Auth_Service SHALL allow a Driver to update their profile picture and contact details
3. WHEN a Rider or Driver updates their profile, THE Auth_Service SHALL persist the changes and return the updated profile within 2 seconds
4. THE Rider_App SHALL display the Rider's ride history accessible from the profile screen
5. THE Driver_App SHALL display the Driver's ride history and earnings accessible from the profile screen

---

### Requirement 4: Map Loading and Location Detection

**User Story:** As a Rider, I want the app to load a map with my current location when I open it, so that I can quickly set my pickup point.

#### Acceptance Criteria

1. WHEN a Rider opens the home screen, THE Rider_App SHALL request device location permission and load the map centered on the Rider's current GPS coordinates within 3 seconds
2. WHEN location permission is granted, THE Location_Service SHALL continuously update the Rider's position on the map
3. IF location permission is denied, THEN THE Rider_App SHALL prompt the Rider to enter a pickup address manually
4. THE Rider_App SHALL display nearby available Drivers on the map as real-time markers updated at most every 5 seconds

---

### Requirement 5: Ride Booking

**User Story:** As a Rider, I want to enter a pickup and destination, see fare and ETA estimates, and confirm a booking, so that I can request a ride.

#### Acceptance Criteria

1. WHEN a Rider enters a valid pickup location and destination, THE Fare_Engine SHALL calculate and display the estimated fare, distance, and ETA before the Rider confirms the booking
2. WHEN a Rider confirms a Ride_Request, THE Ride_Service SHALL broadcast the request to available Drivers within a 3–5 km radius
3. WHEN a Driver accepts a Ride_Request, THE Ride_Service SHALL assign the Ride to that Driver and notify the Rider with the Driver's name, vehicle details, and live tracking link within 5 seconds
4. IF no Driver accepts the Ride_Request within 2 minutes, THEN THE Ride_Service SHALL notify the Rider that no drivers are available and cancel the request
5. WHEN a Ride is assigned, THE Rider_App SHALL display the Driver's real-time location on the map updated at most every 3 seconds
6. THE Ride_Service SHALL allow a Rider to cancel a Ride_Request before a Driver is assigned without penalty

---

### Requirement 6: Driver Allocation

**User Story:** As a platform operator, I want the system to intelligently allocate the nearest available driver to a ride request, so that wait times are minimized and driver utilization is optimized.

#### Acceptance Criteria

1. WHEN a Ride_Request is created, THE Driver_Allocator SHALL identify all online Drivers within a 5 km radius of the pickup location
2. THE Driver_Allocator SHALL prioritize Drivers with higher ratings and lower cancellation rates when multiple Drivers are within range
3. WHEN a Ride_Request is broadcast, THE Driver_Allocator SHALL send the request to the highest-priority available Driver first and escalate to the next Driver if the request is not accepted within 30 seconds
4. IF a Driver rejects a Ride_Request, THEN THE Driver_Allocator SHALL immediately escalate the request to the next available Driver
5. WHILE a Driver is on an active Ride, THE Driver_Allocator SHALL exclude that Driver from receiving new Ride_Requests

---

### Requirement 7: Live Driver Tracking

**User Story:** As a Rider, I want to track my driver's location in real time during the ride, so that I know where the driver is and when to expect arrival.

#### Acceptance Criteria

1. WHILE a Ride is in "driver en route" or "in progress" status, THE Location_Service SHALL stream the Driver's GPS coordinates to the Rider_App via WebSocket at most every 3 seconds
2. WHEN the Driver's location updates, THE Rider_App SHALL re-render the Driver's map marker at the new coordinates without a full page reload
3. WHILE a Ride is active, THE Rider_App SHALL display the remaining ETA to the pickup point or destination, recalculated at most every 10 seconds
4. IF the WebSocket connection is interrupted, THEN THE Rider_App SHALL attempt to reconnect within 5 seconds and display a "reconnecting" indicator to the Rider

---

### Requirement 8: Fare Calculation

**User Story:** As a Rider, I want the fare to be calculated transparently based on distance, time, and demand, so that I understand what I am being charged.

#### Acceptance Criteria

1. THE Fare_Engine SHALL calculate the base fare as: base_fare + (per_km_rate × distance_km) + (per_minute_rate × waiting_minutes)
2. WHEN demand exceeds supply in a geographic zone, THE Fare_Engine SHALL apply a Surge_Multiplier to the base fare and display the multiplier value to the Rider before booking confirmation
3. THE Fare_Engine SHALL apply valid discount codes or coupons to reduce the final fare before payment
4. THE Fare_Engine SHALL add a platform fee to every completed Ride fare
5. WHEN a Ride is completed, THE Fare_Engine SHALL produce a final fare breakdown itemizing base fare, distance charge, waiting charge, surge multiplier, platform fee, and any discounts applied
6. THE Admin Dashboard SHALL allow an Admin to configure base fare, per_km_rate, per_minute_rate, platform fee, and surge thresholds per geographic zone

---

### Requirement 9: Payment Processing

**User Story:** As a Rider, I want to pay for my ride using my preferred payment method, so that the transaction is seamless and secure.

#### Acceptance Criteria

1. THE Payment_Service SHALL support the following payment methods: UPI (Google Pay, PhonePe, Paytm), credit/debit cards, net banking, in-app wallet, and cash
2. WHEN a Ride is completed, THE Payment_Service SHALL automatically initiate payment collection using the Rider's selected payment method
3. WHEN a payment is successfully processed, THE Payment_Service SHALL generate an invoice and deliver it to the Rider via push notification and in-app receipt within 30 seconds
4. IF a payment fails, THEN THE Payment_Service SHALL notify the Rider with the failure reason and allow the Rider to retry with an alternative payment method
5. WHEN a refund is approved, THE Payment_Service SHALL initiate the refund to the Rider's original payment method within 5 business days
6. THE Payment_Service SHALL store all transaction records with a unique transaction ID for audit purposes

---

### Requirement 10: Driver App — Ride Management

**User Story:** As a Driver, I want to go online, receive ride requests, and navigate to pickup and drop-off locations, so that I can complete rides efficiently.

#### Acceptance Criteria

1. WHEN a Driver toggles to "online" status, THE Driver_App SHALL register the Driver as available with the Location_Service and begin broadcasting the Driver's GPS location at most every 3 seconds
2. WHEN a Ride_Request is assigned to a Driver, THE Driver_App SHALL display a popup with the Rider's pickup location, estimated distance, and estimated fare, and allow the Driver to accept or reject within 30 seconds
3. IF a Driver does not respond to a Ride_Request within 30 seconds, THEN THE Ride_Service SHALL treat the request as rejected and escalate to the next Driver
4. WHEN a Driver accepts a Ride_Request, THE Driver_App SHALL launch turn-by-turn navigation to the pickup location using Google Maps
5. WHEN a Driver taps "Start Ride", THE Ride_Service SHALL transition the Ride status to "in progress" and begin fare metering
6. WHEN a Driver taps "End Ride", THE Ride_Service SHALL transition the Ride status to "completed" and trigger payment processing
7. WHEN a Driver toggles to "offline" status, THE Location_Service SHALL stop broadcasting the Driver's location and THE Driver_Allocator SHALL exclude the Driver from new Ride_Requests

---

### Requirement 11: Driver Earnings Dashboard

**User Story:** As a Driver, I want to view my daily and weekly earnings and ride history, so that I can track my income.

#### Acceptance Criteria

1. THE Driver_App SHALL display a dashboard showing total earnings for the current day and current week
2. THE Driver_App SHALL display a list of completed rides with date, pickup, destination, distance, and fare for each ride
3. WHEN a Ride is completed, THE Driver_App SHALL update the earnings dashboard within 60 seconds

---

### Requirement 12: Safety Features

**User Story:** As a Rider, I want access to safety tools during a ride, so that I can get help quickly in an emergency.

#### Acceptance Criteria

1. WHILE a Ride is in progress, THE Rider_App SHALL display an SOS button accessible within 2 taps from the active ride screen
2. WHEN a Rider activates the SOS button, THE Notification_Service SHALL send an emergency alert containing the Rider's name, current GPS coordinates, and Ride details to the Rider's registered emergency contacts and platform support within 10 seconds
3. WHILE a Ride is in progress, THE Rider_App SHALL allow the Rider to share a live tracking link with any contact via SMS or messaging apps
4. THE Ride_Service SHALL log all GPS coordinates, timestamps, and status transitions for every Ride for a minimum of 90 days for safety audit purposes
5. THE Admin Dashboard SHALL allow an Admin to blacklist a Driver, which immediately prevents the Driver from going online or accepting new rides

---

### Requirement 13: Ratings and Reviews

**User Story:** As a Rider or Driver, I want to rate and review the other party after a ride, so that platform quality and safety are maintained.

#### Acceptance Criteria

1. WHEN a Ride is completed, THE Rider_App SHALL prompt the Rider to rate the Driver on a scale of 1 to 5 stars and optionally submit a text comment
2. WHEN a Ride is completed, THE Driver_App SHALL prompt the Driver to rate the Rider on a scale of 1 to 5 stars
3. THE Ride_Service SHALL calculate and store a rolling average rating for each Driver based on all submitted Rider ratings
4. WHEN a Driver's rolling average rating falls below 3.5 stars, THE Ride_Service SHALL flag the Driver's account for Admin review and THE Notification_Service SHALL notify the Admin
5. THE Rider_App SHALL display the Driver's average rating on the ride confirmation screen before the Rider confirms a booking

---

### Requirement 14: Push Notifications and SMS Alerts

**User Story:** As a Rider or Driver, I want to receive timely notifications about ride status and account events, so that I stay informed without having to check the app manually.

#### Acceptance Criteria

1. THE Notification_Service SHALL send a push notification to the Rider when a Driver is assigned to their Ride_Request
2. THE Notification_Service SHALL send a push notification to the Rider when the Driver arrives at the pickup location
3. THE Notification_Service SHALL send a push notification to the Rider when a Ride is completed and payment is processed
4. THE Notification_Service SHALL send an SMS alert to a Rider's mobile number for OTP delivery and critical ride status updates
5. THE Notification_Service SHALL send a push notification to the Driver when a new Ride_Request is assigned
6. IF a push notification fails to deliver, THEN THE Notification_Service SHALL retry delivery up to 3 times with exponential backoff before logging the failure

---

### Requirement 15: Admin Dashboard — User and Driver Management

**User Story:** As an Admin, I want to manage Riders and Drivers and monitor platform activity, so that I can ensure safe and compliant operations.

#### Acceptance Criteria

1. THE Admin_Dashboard SHALL display a real-time list of all active Rides with Driver location, Rider details, and Ride status
2. THE Admin_Dashboard SHALL allow an Admin to search, view, and update Rider and Driver accounts
3. THE Admin_Dashboard SHALL allow an Admin to approve or reject Driver KYC submissions with a mandatory reason for rejection
4. THE Admin_Dashboard SHALL display platform analytics including total rides, total revenue, active drivers, and active riders for configurable date ranges
5. THE Admin_Dashboard SHALL allow an Admin to configure pricing rules including base fare, per_km_rate, per_minute_rate, platform fee, and surge multiplier thresholds per zone
6. WHEN an Admin blacklists a Driver, THE Admin_Dashboard SHALL record the reason and timestamp of the blacklist action

---

### Requirement 16: System Availability and Performance

**User Story:** As a platform operator, I want the system to be highly available and responsive, so that Riders and Drivers have a reliable experience.

#### Acceptance Criteria

1. THE Ride_Service SHALL maintain 99.9% uptime measured on a rolling 30-day basis
2. WHEN a Rider submits a Ride_Request, THE Ride_Service SHALL acknowledge the request within 2 seconds under normal load
3. THE Location_Service SHALL handle concurrent GPS location updates from at least 10,000 active Drivers without exceeding 500ms processing latency per update
4. THE Auth_Service SHALL respond to authentication requests within 1 second under normal load
5. THE Payment_Service SHALL process payment transactions within 10 seconds of ride completion under normal load
