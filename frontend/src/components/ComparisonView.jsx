import React from 'react';
import { Database, Zap, AlertTriangle, ArrowRightLeft, Users, ShieldAlert, CheckCircle2 } from 'lucide-react';

export default function ComparisonView({ catalog, chaosStatus, telemetry }) {
  const isPostgresHealthy = chaosStatus?.postgresHealthy ?? true;
  const isRedisHealthy = chaosStatus?.redisHealthy ?? true;

  // Drift calculado pelo backend ou derivado
  const driftAmount = catalog?.driftAmount || 0;
  const driftDetected = catalog?.driftDetected || driftAmount > 0;
  const redisStock = catalog?.availableStock ?? 50;
  // Se o Postgres estiver particionado, o estoque Postgres não responde ou é desconhecido
  const postgresStock = isPostgresHealthy
    ? Math.max(0, redisStock - (driftDetected ? driftAmount : 0))
    : 'PARTITIONED';

  const blockedCount503 = telemetry?.total503 ?? 0;
  const recentBuyers = catalog?.recentBuyers || [];

  return (
    <section className="mb-8">
      {/* Alerta de Drift Pulsante */}
      {driftDetected && (
        <div className="mb-6 p-4 rounded-xl bg-amber-500/10 border-2 border-amber-500/40 text-amber-300 animate-pulse-fast flex items-center gap-3 shadow-lg shadow-amber-500/10">
          <AlertTriangle className="w-6 h-6 text-amber-400 shrink-0" />
          <div className="text-sm">
            <strong className="font-bold text-amber-200">
              Inconsistência Temporária Detectada (BASE):
            </strong>{' '}
            A vitrine Redis está defasada em{' '}
            <span className="font-mono font-black text-amber-100 underline decoration-amber-400 decoration-2">
              {driftAmount} ingresso{driftAmount > 1 ? 's' : ''}
            </span>{' '}
            em relação ao PostgreSQL real devido ao corte de rede. Ao restaurar a rede, os dados convergem!
          </div>
        </div>
      )}

      {/* Grid de Comparação Lado a Lado */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 relative">
        {/* Indicador Central de Drift Flutuante */}
        <div className="hidden lg:flex absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 z-10 flex-col items-center">
          <div
            className={`p-3 rounded-full border shadow-xl backdrop-blur-md transition-all ${
              driftDetected
                ? 'bg-amber-950 border-amber-500 text-amber-400 scale-110'
                : 'bg-slate-900 border-slate-700 text-slate-400'
            }`}
          >
            <ArrowRightLeft className="w-5 h-5" />
          </div>
          <div className="mt-1 text-center">
            <span
              className={`text-xs font-mono font-bold px-2 py-0.5 rounded-full border ${
                driftDetected
                  ? 'bg-amber-500/20 text-amber-300 border-amber-500/40'
                  : 'bg-slate-800 text-slate-400 border-slate-700'
              }`}
            >
              Δ Drift: {driftAmount}
            </span>
          </div>
        </div>

        {/* Coluna 1: PostgreSQL (Fonte da Verdade - ACID / CP) */}
        <div className="rounded-2xl border border-slate-800 bg-slate-900/80 p-6 flex flex-col justify-between shadow-xl relative overflow-hidden">
          <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-blue-600 to-indigo-500"></div>

          <div>
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2.5">
                <div className="p-2 bg-blue-500/10 text-blue-400 rounded-lg border border-blue-500/20">
                  <Database className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-bold text-white text-base flex items-center gap-2">
                    PostgreSQL
                    <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-blue-500/10 text-blue-400 border border-blue-500/20">
                      ACID / CP
                    </span>
                  </h3>
                  <span className="text-xs text-slate-400">Fonte da Verdade Oficial</span>
                </div>
              </div>

              {/* Status Badge */}
              <span
                className={`inline-flex items-center gap-1.5 text-xs font-mono px-2.5 py-1 rounded-full border ${
                  isPostgresHealthy
                    ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                    : 'bg-rose-500/10 text-rose-400 border-rose-500/30 font-bold'
                }`}
              >
                <span
                  className={`w-2 h-2 rounded-full ${
                    isPostgresHealthy ? 'bg-emerald-500' : 'bg-rose-500 animate-ping'
                  }`}
                ></span>
                {isPostgresHealthy ? 'ONLINE (TCP 25432)' : 'PARTITIONED (503)'}
              </span>
            </div>

            {/* Display do Estoque Real */}
            <div className="my-6 p-5 rounded-xl bg-slate-950/60 border border-slate-800/80 text-center">
              <span className="text-xs font-mono text-slate-400 uppercase tracking-wider block mb-1">
                Estoque Real Confirmado
              </span>
              <div className="text-5xl font-black font-mono tracking-tight text-white">
                {postgresStock === 'PARTITIONED' ? (
                  <span className="text-rose-400 text-3xl font-extrabold flex items-center justify-center gap-2">
                    <ShieldAlert className="w-7 h-7" /> BLOQUEADO
                  </span>
                ) : (
                  <span>{postgresStock}</span>
                )}
              </div>
              <span className="text-xs text-slate-500 mt-2 block font-mono">
                Lock pessimista: LockModeType.PESSIMISTIC_WRITE
              </span>
            </div>
          </div>

          {/* Métricas ACID */}
          <div className="pt-4 border-t border-slate-800/60 grid grid-cols-2 gap-3 text-xs font-mono">
            <div className="p-2.5 rounded-lg bg-slate-950/40 border border-slate-800/50">
              <span className="text-slate-400 block mb-1">Garantia CP</span>
              <span className="font-bold text-blue-400">Consistência Forte</span>
            </div>
            <div className="p-2.5 rounded-lg bg-slate-950/40 border border-slate-800/50">
              <span className="text-slate-400 block mb-1">Vendas Barradas (503)</span>
              <span className="font-bold text-rose-400">{blockedCount503}</span>
            </div>
          </div>
        </div>

        {/* Coluna 2: Redis (Vitrine Pública - BASE / AP) */}
        <div className="rounded-2xl border border-slate-800 bg-slate-900/80 p-6 flex flex-col justify-between shadow-xl relative overflow-hidden">
          <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-rose-500 to-amber-500"></div>

          <div>
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2.5">
                <div className="p-2 bg-rose-500/10 text-rose-400 rounded-lg border border-rose-500/20">
                  <Zap className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-bold text-white text-base flex items-center gap-2">
                    Redis Cache
                    <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-rose-500/10 text-rose-400 border border-rose-500/20">
                      BASE / AP
                    </span>
                  </h3>
                  <span className="text-xs text-slate-400">Vitrine de Alta Disponibilidade</span>
                </div>
              </div>

              {/* Status Badge */}
              <span
                className={`inline-flex items-center gap-1.5 text-xs font-mono px-2.5 py-1 rounded-full border ${
                  isRedisHealthy
                    ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                    : 'bg-orange-500/10 text-orange-400 border-orange-500/30'
                }`}
              >
                <span
                  className={`w-2 h-2 rounded-full ${
                    isRedisHealthy ? 'bg-emerald-500' : 'bg-orange-500'
                  }`}
                ></span>
                {isRedisHealthy ? 'ONLINE (TCP 26379)' : 'PARTITIONED (LAG)'}
              </span>
            </div>

            {/* Display do Estoque em Vitrine */}
            <div className="my-6 p-5 rounded-xl bg-slate-950/60 border border-slate-800/80 text-center">
              <span className="text-xs font-mono text-slate-400 uppercase tracking-wider block mb-1">
                Estoque Exibido na Vitrine
              </span>
              <div className="text-5xl font-black font-mono tracking-tight text-white flex items-center justify-center gap-2">
                <span>{redisStock >= 0 ? redisStock : 'N/A'}</span>
              </div>
              <span className="text-xs text-emerald-400 mt-2 block font-mono flex items-center justify-center gap-1">
                <CheckCircle2 className="w-3.5 h-3.5" />
                Latência de leitura: &lt; 15ms (Status 200 OK)
              </span>
            </div>
          </div>

          {/* Últimos Compradores no Feed BASE */}
          <div className="pt-4 border-t border-slate-800/60">
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs font-mono text-slate-400 flex items-center gap-1.5">
                <Users className="w-3.5 h-3.5 text-slate-400" />
                Últimos Compradores (Feed Redis):
              </span>
              <span className="text-[11px] font-mono text-slate-500">
                Data Source: {catalog?.dataSource || 'REDIS'}
              </span>
            </div>
            <div className="flex items-center gap-2 overflow-x-auto py-1">
              {recentBuyers.length > 0 ? (
                recentBuyers.slice(0, 5).map((buyer, idx) => (
                  <span
                    key={idx}
                    className="text-xs font-mono px-2.5 py-1 rounded bg-slate-800 text-slate-200 border border-slate-700 shrink-0"
                  >
                    👤 {buyer}
                  </span>
                ))
              ) : (
                <span className="text-xs text-slate-500 italic font-mono">Nenhuma compra recente</span>
              )}
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
