import express from "express";
import { v4 as uuid } from "uuid";
import { errorEnvelope, KycStatus, Role, User } from "@pink-auto/shared";

const app = express();
app.use(express.json());

const otpSessions = new Map<string, { otp: string; attempts: number; expiresAt: number; lockedUntil?: number }>();
const users = new Map<string, User>();

const now = () => Date.now();
const issueTokens = (userId: string) => ({
  accessToken: `access-${userId}-${uuid()}`,
  refreshToken: `refresh-${userId}-${uuid()}`
});

app.post("/auth/send-otp", (req, res) => {
  const phone = String(req.body?.phone || "");
  if (phone.length < 10) return res.status(400).json(errorEnvelope("PHONE_INVALID", "Phone must be valid"));
  otpSessions.set(phone, {
    otp: "123456",
    attempts: 0,
    expiresAt: now() + 5 * 60 * 1000
  });
  res.json({ success: true, ttlSeconds: 300 });
});

app.post("/auth/verify-otp", (req, res) => {
  const phone = String(req.body?.phone || "");
  const otp = String(req.body?.otp || "");
  const role = (req.body?.role || "rider") as Role;
  const session = otpSessions.get(phone);
  if (!session) return res.status(400).json(errorEnvelope("OTP_INVALID", "No OTP session"));
  if (session.lockedUntil && session.lockedUntil > now()) {
    return res.status(429).json(errorEnvelope("OTP_LOCKED", "OTP session is locked"));
  }
  if (session.expiresAt < now()) return res.status(400).json(errorEnvelope("OTP_EXPIRED", "OTP expired"));
  if (otp !== session.otp) {
    session.attempts += 1;
    if (session.attempts >= 3) session.lockedUntil = now() + 15 * 60 * 1000;
    otpSessions.set(phone, session);
    return res.status(400).json(errorEnvelope("OTP_INVALID", "OTP mismatch"));
  }
  const existing = [...users.values()].find((u) => u.phone === phone && u.role === role);
  const user = existing || {
    id: uuid(),
    phone,
    role,
    name: role === "driver" ? "Driver User" : "Rider User",
    kycStatus: role === "driver" ? ("pending" as KycStatus) : ("active" as KycStatus),
    rating: 5
  };
  users.set(user.id, user);
  res.json({ ...issueTokens(user.id), user });
});

app.post("/auth/rider/login", (req, res) => {
  const email = String(req.body?.email || "");
  const password = String(req.body?.password || "");
  if (!email || password.length < 6) return res.status(401).json(errorEnvelope("AUTH_FAILED", "Invalid credentials"));
  const user: User = { id: uuid(), phone: "9000000001", role: "rider", name: "Email Rider", kycStatus: "active", rating: 5 };
  users.set(user.id, user);
  res.json({ ...issueTokens(user.id), user });
});

app.post("/auth/rider/google", (req, res) => {
  const token = String(req.body?.google_token || "");
  if (!token) return res.status(400).json(errorEnvelope("GOOGLE_TOKEN_INVALID", "Invalid token"));
  const user: User = { id: uuid(), phone: "9000000004", role: "rider", name: "Google Rider", kycStatus: "active", rating: 5 };
  users.set(user.id, user);
  res.json({ ...issueTokens(user.id), user });
});

app.put("/auth/profile", (req, res) => {
  const userId = String(req.body?.userId || "");
  const user = users.get(userId);
  if (!user) return res.status(404).json(errorEnvelope("USER_NOT_FOUND", "Unknown user"));
  const updated = { ...user, name: req.body?.name ?? user.name };
  users.set(userId, updated);
  res.json(updated);
});

app.get("/health", (_, res) => res.json({ ok: true, service: "auth" }));

app.listen(4001, () => {
  console.log("Auth service listening on :4001");
});
