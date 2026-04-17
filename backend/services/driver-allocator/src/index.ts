import express from "express";

const app = express();
app.use(express.json());

const pool = [
  { id: "d1", rating: 4.9, cancellation_rate: 0.02, online: true, activeRide: false },
  { id: "d2", rating: 4.8, cancellation_rate: 0.05, online: true, activeRide: false },
  { id: "d3", rating: 4.5, cancellation_rate: 0.10, online: true, activeRide: true }
];

app.post("/allocator/assign", (req, res) => {
  const driver = pool
    .filter((d) => d.online && !d.activeRide)
    .sort((a, b) => b.rating - a.rating || a.cancellation_rate - b.cancellation_rate)[0];
  if (!driver) return res.status(200).json({ no_driver: true });
  res.json({ driver_id: driver.id, timeout_seconds: 30 });
});

app.get("/health", (_, res) => res.json({ ok: true, service: "driver-allocator" }));
app.listen(4009, () => console.log("Driver allocator listening on :4009"));
