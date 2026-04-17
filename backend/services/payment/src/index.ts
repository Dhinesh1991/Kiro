import express from "express";
import { v4 as uuid } from "uuid";

const app = express();
app.use(express.json());

const txns = new Map<string, any>();

app.post("/payments/initiate", (req, res) => {
  const rideId = String(req.body?.ride_id || "");
  const method = String(req.body?.method || "UPI");
  if (!rideId) return res.status(400).json({ error: { code: "BAD_REQUEST", message: "ride_id required" } });
  const txn = { id: uuid(), ride_id: rideId, method, status: "success", gateway_txn_id: `txn_${uuid()}` };
  txns.set(rideId, txn);
  res.json(txn);
});

app.get("/payments/invoice/:ride_id", (req, res) => {
  const txn = txns.get(req.params.ride_id);
  if (!txn) return res.status(404).json({ error: { code: "NOT_FOUND", message: "Invoice not found" } });
  res.json({ invoice_id: `inv_${txn.id}`, ride_id: txn.ride_id, amount: 120, status: txn.status });
});

app.post("/payments/refund", (req, res) => {
  const rideId = String(req.body?.ride_id || "");
  const txn = txns.get(rideId);
  if (!txn) return res.status(404).json({ error: { code: "NOT_FOUND", message: "Payment not found" } });
  txn.status = "refunded";
  txns.set(rideId, txn);
  res.json(txn);
});

app.get("/payments/transactions", (_, res) => res.json([...txns.values()]));
app.get("/health", (_, res) => res.json({ ok: true, service: "payment" }));
app.listen(4006, () => console.log("Payment service listening on :4006"));
