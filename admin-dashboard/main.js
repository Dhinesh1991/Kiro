const api = "http://localhost:4000";

const analyticsEl = document.getElementById("analytics");
const blacklistResult = document.getElementById("blacklistResult");

document.getElementById("loadAnalytics").addEventListener("click", async () => {
  const data = await fetch(`${api}/admin/analytics`).then((r) => r.json());
  analyticsEl.innerHTML = `
    <div class="card"><b>Total Rides</b><div>${data.total_rides}</div></div>
    <div class="card"><b>Total Revenue</b><div>${data.total_revenue}</div></div>
    <div class="card"><b>Active Drivers</b><div>${data.active_drivers}</div></div>
    <div class="card"><b>Active Riders</b><div>${data.active_riders}</div></div>
  `;
});

document.getElementById("blacklist").addEventListener("click", async () => {
  const driverId = document.getElementById("driverId").value;
  const reason = document.getElementById("reason").value;
  const data = await fetch(`${api}/admin/drivers/${driverId}/blacklist`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ reason })
  }).then((r) => r.json());
  blacklistResult.textContent = JSON.stringify(data, null, 2);
});
