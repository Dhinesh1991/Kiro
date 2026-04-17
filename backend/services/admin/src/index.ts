import express from "express";
import { errorEnvelope } from "@pink-auto/shared";

const app = express();
app.use(express.json());

const blacklistedDrivers = new Map<string, { reason: string; timestamp: string }>();
const pricing = new Map<string, { base_fare: number; per_km: number; per_minute: number; platform_fee: number; surge_threshold: number }>();

app.get("/admin/rides/active", (_, res) => {
  res.json([]);
});

app.get("/admin/users", (req, res) => {
  const q = String(req.query.search || "");
  res.json({ items: [], search: q });
});

app.put("/admin/drivers/:id/blacklist", (req, res) => {
  const reason = String(req.body?.reason || "");
  if (!reason) return res.status(400).json(errorEnvelope("BAD_REQUEST", "reason is required"));
  const record = { reason, timestamp: new Date().toISOString() };
  blacklistedDrivers.set(req.params.id, record);
  res.json({ driverId: req.params.id, ...record });
});

app.get("/admin/analytics", (_, res) => {
  res.json({
    total_rides: 0,
    total_revenue: 0,
    active_drivers: 0,
    active_riders: 0
  });
});

app.put("/admin/pricing/:zone_id", (req, res) => {
  const payload = {
    base_fare: Number(req.body?.base_fare || 30),
    per_km: Number(req.body?.per_km || 12),
    per_minute: Number(req.body?.per_minute || 1),
    platform_fee: Number(req.body?.platform_fee || 8),
    surge_threshold: Number(req.body?.surge_threshold || 1.5)
  };
  pricing.set(req.params.zone_id, payload);
  res.json({ zone_id: req.params.zone_id, ...payload });
});

app.get("/health", (_, res) => res.json({ ok: true, service: "admin" }));

app.listen(4003, () => {
  console.log("Admin service listening on :4003");
});
