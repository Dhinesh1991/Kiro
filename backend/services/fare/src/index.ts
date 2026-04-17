import express from "express";

const app = express();
app.use(express.json());

const pricing = {
  base_fare: 30,
  per_km_rate: 12,
  per_minute_rate: 1,
  platform_fee: 8,
  surge_threshold: 1.5
};

app.post("/fare/estimate", (req, res) => {
  const distanceKm = Number(req.body?.distance_km || 5);
  const waitingMin = Number(req.body?.waiting_minutes || 0);
  const demandSupply = Number(req.body?.demand_supply_ratio || 1);
  const base = pricing.base_fare + (pricing.per_km_rate * distanceKm) + (pricing.per_minute_rate * waitingMin);
  const surgeMultiplier = demandSupply > pricing.surge_threshold ? 1.2 : 1.0;
  const total = (base * surgeMultiplier) + pricing.platform_fee;
  res.json({ estimated_fare: total, distance_km: distanceKm, eta_seconds: Math.round(distanceKm * 180), surge_multiplier: surgeMultiplier });
});

app.post("/fare/final", (req, res) => {
  const estimated = Number(req.body?.estimated_fare || 100);
  const discount = Number(req.body?.discount || 0);
  res.json({
    base_fare: estimated - pricing.platform_fee,
    distance_charge: estimated * 0.5,
    waiting_charge: estimated * 0.1,
    surge_multiplier: 1.0,
    platform_fee: pricing.platform_fee,
    discount,
    total_fare: Math.max(0, estimated - discount)
  });
});

app.put("/fare/config", (req, res) => {
  Object.assign(pricing, req.body || {});
  res.json(pricing);
});

app.get("/health", (_, res) => res.json({ ok: true, service: "fare" }));
app.listen(4005, () => console.log("Fare service listening on :4005"));
