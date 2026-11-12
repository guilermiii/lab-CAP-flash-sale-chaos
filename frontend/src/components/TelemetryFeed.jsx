import React from 'react';
import { Terminal, Activity, CheckCircle, XCircle, AlertOctagon, HelpCircle } from 'lucide-react';

export default function TelemetryFeed({ telemetry }) {
  const rps = telemetry?.rps ?? 0;
  const total201 = telemetry?.total201 ?? 0;
  const total409 = telemetry?.total409 ?? 0;
  const total503 = telemetry?.total503 ?? 0;
  const transactions = telemetry?.recentTransactions || [];

  return (
    <section className="rounded-2xl border border-slate-800 bg-slate-900/80 p-6 shadow-xl">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
        <div className="flex items-center gap-2.5">
          <div className="p-2 bg-indigo-500/10 text-indigo-400 rounded-lg border border-indigo-500/20">
            <Terminal className="w-5 h-5" />
          </div>
          <div>
            <h3 className="font-bold text-white text-base">Telemetria &amp; Stream de Requisições</h3>
            <span className="text-xs text-slate-400">Monitoramento ao vivo de carga (k6 e requisições HTTP)</span>
          </div>
        </div>

        {/* Indicador de Polling */}
        <div className="flex items-center gap-2 text-xs font-mono text-slate-400 bg-slate-950/60 px-3 py-1.5 rounded-lg border border-slate-800">
          <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
          Live Feed: Polling a cada 500ms
        </div>
      </div>

      {/* Cards de Métricas em Tempo Real */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800">
          <div className="flex items-center justify-between mb-1">
            <span className="text-xs font-mono text-slate-400">RPS Atual</span>
            <Activity className="w-4 h-4 text-cyan-400" />
          </div>
          <div className="text-2xl font-black font-mono text-cyan-400">{rps} req/s</div>
        </div>

        <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800">
          <div className="flex items-center justify-between mb-1">
            <span className="text-xs font-mono text-slate-400">201 Created</span>
            <CheckCircle className="w-4 h-4 text-emerald-400" />
          </div>
          <div className="text-2xl font-black font-mono text-emerald-400">{total201}</div>
          <span className="text-[10px] text-slate-500 font-mono">Vendas Aprovadas</span>
        </div>

        <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800">
          <div className="flex items-center justify-between mb-1">
            <span className="text-xs font-mono text-slate-400">409 Conflict</span>
            <AlertOctagon className="w-4 h-4 text-amber-400" />
          </div>
          <div className="text-2xl font-black font-mono text-amber-400">{total409}</div>
          <span className="text-[10px] text-slate-500 font-mono">Ingressos Esgotados</span>
        </div>

        <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800">
          <div className="flex items-center justify-between mb-1">
            <span className="text-xs font-mono text-slate-400">503 Unavailable</span>
            <XCircle className="w-4 h-4 text-rose-400" />
          </div>
          <div className="text-2xl font-black font-mono text-rose-400">{total503}</div>
          <span className="text-[10px] text-slate-500 font-mono">Bloqueios de Rede CP</span>
        </div>
      </div>

      {/* Tabela de Transações Recentes */}
      <div className="overflow-x-auto rounded-xl border border-slate-800/80 bg-slate-950/40">
        <table className="w-full text-left text-xs font-mono">
          <thead className="bg-slate-950 text-slate-400 uppercase tracking-wider text-[11px] border-b border-slate-800">
            <tr>
              <th className="py-2.5 px-4">Timestamp</th>
              <th className="py-2.5 px-4">Método</th>
              <th className="py-2.5 px-4">Endpoint</th>
              <th className="py-2.5 px-4">Duração</th>
              <th className="py-2.5 px-4 text-right">Status HTTP</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/60">
            {transactions.length > 0 ? (
              transactions.map((tx) => {
                let statusBadge = 'bg-slate-800 text-slate-300 border-slate-700';
                if (tx.statusCode === 201 || tx.statusCode === 200) {
                  statusBadge = 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30';
                } else if (tx.statusCode === 409) {
                  statusBadge = 'bg-amber-500/10 text-amber-400 border-amber-500/30';
                } else if (tx.statusCode === 503) {
                  statusBadge = 'bg-rose-500/10 text-rose-400 border-rose-500/30 font-bold';
                }

                return (
                  <tr key={tx.id} className="hover:bg-slate-900/60 transition-colors">
                    <td className="py-2 px-4 text-slate-400">
                      {tx.timestamp ? new Date(tx.timestamp).toLocaleTimeString() : '--'}
                    </td>
                    <td className="py-2 px-4">
                      <span className="px-1.5 py-0.5 rounded bg-slate-800 text-slate-300 text-[10px]">
                        {tx.method}
                      </span>
                    </td>
                    <td className="py-2 px-4 text-white font-medium">{tx.endpoint}</td>
                    <td className="py-2 px-4 text-slate-300">
                      <span className={tx.durationMs > 1000 ? 'text-amber-400 font-bold' : ''}>
                        {tx.durationMs}ms
                      </span>
                    </td>
                    <td className="py-2 px-4 text-right">
                      <span className={`inline-block px-2 py-0.5 rounded text-[11px] border ${statusBadge}`}>
                        {tx.statusCode}
                      </span>
                    </td>
                  </tr>
                );
              })
            ) : (
              <tr>
                <td colSpan={5} className="py-6 text-center text-slate-500 italic">
                  Aguardando tráfego de requisições (clique em "Comprar 1 Ingresso" ou inicie o k6)...
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
}
