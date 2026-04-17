import express from "express";

const app = express();
app.use(express.json());

const driverLocations = new Map<string, { lat: number; lng: number; heading: number; ts: number }>();

app.post("/location/driver/update", (req, res) => {
  const driverId = String(req.body?.driver_id || "");
  if (!driverId) return res.status(400).json({ error: { code: "BAD_REQUEST", message: "driver_id required" } });
  driverLocations.set(driverId, {
    lat: Number(req.body?.lat || 0),
    lng: Number(req.body?.lng || 0),
    heading: Number(req.body?.heading || 0),
    ts: Date.now()
  });
  res.json({ ok: true });
});

app.get("/location/nearby-drivers", (req, res) => {
  const lat = Number(req.query.lat || 0);
  const lng = Number(req.query.lng || 0);
  const radiusKm = Number(req.query.radius_km || 5);
  const list = [...driverLocations.entries()].map(([driverId, p]) => ({
    driver_id: driverId,
    lat: p.lat,
    lng: p.lng,
    distance_km: Math.sqrt((lat - p.lat) ** 2 + (lng - p.lng) ** 2) * 111
  })).filter((d) => d.distance_km <= radiusKm);
  res.json(list);
});

app.get("/health", (_, res) => res.json({ ok: true, service: "location" }));

app.listen(4004, () => {
  console.log("Location service listening on :4004");
});
