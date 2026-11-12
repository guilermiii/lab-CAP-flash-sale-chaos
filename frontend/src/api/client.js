const API_BASE = import.meta.env.VITE_API_URL || '';

export async function fetchCatalog() {
  const res = await fetch(`${API_BASE}/api/tickets/catalog`);
  if (!res.ok) throw new Error(`Catalog error: ${res.status}`);
  return res.json();
}

export async function buyTicket(buyerId = `user-${Math.floor(1000 + Math.random() * 9000)}`) {
  const res = await fetch(`${API_BASE}/api/tickets/buy`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ buyerId })
  });
  const data = await res.json().catch(() => ({}));
  return { status: res.status, ok: res.ok, data };
}

export async function fetchChaosStatus() {
  const res = await fetch(`${API_BASE}/api/chaos/status`);
  if (!res.ok) throw new Error(`Chaos status error: ${res.status}`);
  return res.json();
}

export async function applyChaosScenario(scenarioType) {
  const res = await fetch(`${API_BASE}/api/chaos/scenario/${scenarioType}`, {
    method: 'POST'
  });
  if (!res.ok) throw new Error(`Apply scenario error: ${res.status}`);
  return res.json();
}

export async function resetEnvironment() {
  const res = await fetch(`${API_BASE}/api/admin/reset`, {
    method: 'POST'
  });
  if (!res.ok) throw new Error(`Reset error: ${res.status}`);
  return res.json();
}

export async function fetchTelemetry() {
  const res = await fetch(`${API_BASE}/api/telemetry`);
  if (!res.ok) throw new Error(`Telemetry error: ${res.status}`);
  return res.json();
}
