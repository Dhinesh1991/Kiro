import express from "express";

const app = express();
app.use(express.json());

const kyc = new Map<string, { status: "pending" | "under_review" | "active" | "rejected"; reason?: string }>();

app.post("/kyc/submit", (req, res) => {
  const driverId = String(req.body?.driver_id || "");
  if (!driverId) return res.status(400).json({ error: { code: "BAD_REQUEST", message: "driver_id required" } });
  kyc.set(driverId, { status: "under_review" });
  res.json({ driver_id: driverId, status: "under_review" });
});

app.get("/kyc/:driver_id/status", (req, res) => {
  res.json(kyc.get(req.params.driver_id) || { status: "pending" });
});

app.put("/kyc/:driver_id/review", (req, res) => {
  const action = String(req.body?.action || "");
  if (!["approve", "reject"].includes(action)) return res.status(400).json({ error: { code: "BAD_REQUEST", message: "action must be approve|reject" } });
  if (action === "reject" && !req.body?.reason) return res.status(400).json({ error: { code: "BAD_REQUEST", message: "reason required for rejection" } });
  const next = action === "approve" ? { status: "active" as const } : { status: "rejected" as const, reason: String(req.body?.reason) };
  kyc.set(req.params.driver_id, next);
  res.json({ driver_id: req.params.driver_id, ...next });
});

app.get("/health", (_, res) => res.json({ ok: true, service: "kyc" }));
app.listen(4007, () => console.log("KYC service listening on :4007"));
