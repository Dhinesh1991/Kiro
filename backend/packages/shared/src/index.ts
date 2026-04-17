export type ErrorEnvelope = {
  error: {
    code: string;
    message: string;
    details?: Record<string, unknown>;
  };
};

export type Role = "rider" | "driver" | "admin";
export type KycStatus = "pending" | "under_review" | "active" | "rejected" | "blacklisted";
export type RideStatus = "REQUESTED" | "DRIVER_ASSIGNED" | "DRIVER_EN_ROUTE" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";

export type User = {
  id: string;
  phone: string;
  name: string;
  role: Role;
  kycStatus?: KycStatus;
  rating?: number;
};

export type Ride = {
  id: string;
  riderId: string;
  driverId?: string;
  pickup: string;
  destination: string;
  status: RideStatus;
  fare?: number;
};

export const errorEnvelope = (code: string, message: string, details?: Record<string, unknown>): ErrorEnvelope => ({
  error: { code, message, details }
});
