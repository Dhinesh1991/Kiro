import express from "express";
import { v4 as uuid } from "uuid";
import { errorEnvelope, Ride } from "@pink-auto/shared";

const app = express();
app.use(express.json());

const rides = new Map<string, Ride>();
const drivers = [
  { id: "d1", name: "Driver One", rating: 4.9, cancellationRate: 0.02, online: true },
  { id: "d2", name: "Driver Two", rating: 4.7, cancellationRate: 0.05, online: true }
];

app.post("/rides/request", (req, res) => {
  const riderId = String(req.body?.rider_id || "r-default");
  const pickup = String(req.body?.pickup || "");
  const destination = String(req.body?.destination || "");
  if (!pickup || !destination) return res.status(400).json(errorEnvelope("BAD_REQUEST", "pickup and destination required"));

  const candidate = drivers
    .filter((d) => d.online)
    .sort((a, b) => b.rating - a.rating || a.cancellationRate - b.cancellationRate)[0];

  if (!candidate) return res.status(200).json({ no_drivers_available: true });

  const ride: Ride = {
    id: uuid(),
    riderId,
    driverId: candidate.id,
    pickup,
    destination,
    status: "DRIVER_ASSIGNED",
    fare: 115
  };
  rides.set(ride.id, ride);
  res.json(ride);
});

app.post("/rides/:id/cancel", (req, res) => {
  const ride = rides.get(req.params.id);
  if (!ride) return res.status(404).json(errorEnvelope("RIDE_NOT_FOUND", "Unknown ride"));
  if (ride.status === "COMPLETED") return res.status(409).json(errorEnvelope("INVALID_STATE_TRANSITION", "Already completed"));
  ride.status = "CANCELLED";
  rides.set(ride.id, ride);
  res.json(ride);
});

app.post("/rides/:id/start", (req, res) => {
  const ride = rides.get(req.params.id);
  if (!ride) return res.status(404).json(errorEnvelope("RIDE_NOT_FOUND", "Unknown ride"));
  if (!["DRIVER_ASSIGNED", "DRIVER_EN_ROUTE"].includes(ride.status)) {
    return res.status(409).json(errorEnvelope("INVALID_STATE_TRANSITION", "Cannot start ride in current status", { current: ride.status }));
  }
  ride.status = "IN_PROGRESS";
  rides.set(ride.id, ride);
  res.json(ride);
});

app.post("/rides/:id/end", (req, res) => {
  const ride = rides.get(req.params.id);
  if (!ride) return res.status(404).json(errorEnvelope("RIDE_NOT_FOUND", "Unknown ride"));
  if (ride.status !== "IN_PROGRESS") {
    return res.status(409).json(errorEnvelope("INVALID_STATE_TRANSITION", "Cannot end ride in current status", { current: ride.status }));
  }
  ride.status = "COMPLETED";
  rides.set(ride.id, ride);
  res.json(ride);
});

app.get("/rides/history", (_, res) => {
  res.json([...rides.values()]);
});

app.get("/health", (_, res) => res.json({ ok: true, service: "ride" }));

app.listen(4002, () => {
  console.log("Ride service listening on :4002");
});
