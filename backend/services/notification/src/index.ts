import express from "express";

const app = express();
app.use(express.json());

const logs: Array<{ channel: "push" | "sms"; to: string; title?: string; body: string; ts: number }> = [];

app.post("/notify/push", (req, res) => {
  logs.push({ channel: "push", to: String(req.body?.user_id || "unknown"), title: req.body?.title, body: String(req.body?.body || ""), ts: Date.now() });
  res.json({ sent: true, retries: 0 });
});

app.post("/notify/sms", (req, res) => {
  logs.push({ channel: "sms", to: String(req.body?.phone || "unknown"), body: String(req.body?.message || ""), ts: Date.now() });
  res.json({ sent: true, retries: 0 });
});

app.post("/notify/sos", (req, res) => {
  const riderId = String(req.body?.rider_id || "unknown");
  logs.push({ channel: "sms", to: "support", body: `SOS for rider ${riderId}`, ts: Date.now() });
  res.json({ sent: true });
});

app.get("/notify/logs", (_, res) => res.json(logs.slice(-100)));
app.get("/health", (_, res) => res.json({ ok: true, service: "notification" }));
app.listen(4008, () => console.log("Notification service listening on :4008"));
