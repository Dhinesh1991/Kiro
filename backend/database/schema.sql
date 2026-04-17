CREATE TABLE IF NOT EXISTS riders (
  id UUID PRIMARY KEY,
  phone VARCHAR(15) UNIQUE,
  email VARCHAR(255) UNIQUE,
  name VARCHAR(100),
  photo_url TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS drivers (
  id UUID PRIMARY KEY,
  phone VARCHAR(15) UNIQUE NOT NULL,
  name VARCHAR(100),
  kyc_status VARCHAR(20) DEFAULT 'pending',
  is_online BOOLEAN DEFAULT FALSE,
  rating DECIMAL(3,2) DEFAULT 5.0,
  cancellation_rate DECIMAL(5,4) DEFAULT 0.0,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS rides (
  id UUID PRIMARY KEY,
  rider_id UUID,
  driver_id UUID,
  status VARCHAR(30) NOT NULL,
  pickup_address TEXT,
  dest_address TEXT,
  payment_method VARCHAR(30),
  fare_total DECIMAL(10,2),
  requested_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS payments (
  id UUID PRIMARY KEY,
  ride_id UUID,
  amount DECIMAL(10,2),
  method VARCHAR(30),
  status VARCHAR(20),
  gateway_txn_id VARCHAR(255) UNIQUE,
  created_at TIMESTAMPTZ DEFAULT NOW()
);
