import express from "express";
import { createProxyMiddleware } from "http-proxy-middleware";

const app = express();

app.use("/auth", createProxyMiddleware({ target: "http://localhost:4001", changeOrigin: true }));
app.use("/rides", createProxyMiddleware({ target: "http://localhost:4002", changeOrigin: true }));
app.use("/admin", createProxyMiddleware({ target: "http://localhost:4003", changeOrigin: true }));
app.use("/location", createProxyMiddleware({ target: "http://localhost:4004", changeOrigin: true }));
app.use("/fare", createProxyMiddleware({ target: "http://localhost:4005", changeOrigin: true }));
app.use("/payments", createProxyMiddleware({ target: "http://localhost:4006", changeOrigin: true }));
app.use("/kyc", createProxyMiddleware({ target: "http://localhost:4007", changeOrigin: true }));
app.use("/notify", createProxyMiddleware({ target: "http://localhost:4008", changeOrigin: true }));
app.use("/allocator", createProxyMiddleware({ target: "http://localhost:4009", changeOrigin: true }));

app.get("/health", (_, res) => res.json({ ok: true, service: "gateway" }));

app.listen(4000, () => {
  console.log("Gateway listening on :4000");
});
