import React from 'react';
import { ShieldCheck, Clock, Database, Radio, Flame } from 'lucide-react';

export default function ChaosControls({ activeScenario, onSelectScenario, isLoading }) {
  const scenarios = [
    {
      id: 'HEALTHY',
      name: 'Rede Saudável',
      badge: 'ACID + BASE em Harmonia',
      desc: 'Rede 100% operacional. Zero latência, proxies ativos.',
      icon: ShieldCheck,
      color: 'emerald',
      activeBorder: 'border-emerald-500 shadow-emerald-500/20 bg-emerald-950/30',
      badgeClass: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30',
      btnActive: 'bg-emerald-600 text-white',
    },
    {
      id: 'HIGH_LATENCY',
      name: 'Latência Extrema (+3s DB)',
      badge: 'Virtual Threads & Timeout',
      desc: 'Injeta 3000ms de latência no PostgreSQL via Toxiproxy.',
      icon: Clock,
      color: 'amber',
      activeBorder: 'border-amber-500 shadow-amber-500/20 bg-amber-950/30',
      badgeClass: 'bg-amber-500/10 text-amber-400 border-amber-500/30',
      btnActive: 'bg-amber-600 text-white',
    },
    {
      id: 'PARTITION_CP',
      name: 'Cortar PostgreSQL',
      badge: 'CAP: Consistência Forte (CP)',
      desc: 'Desabilita proxy do Postgres. Compras falham (503), vitrine viva.',
      icon: Database,
      color: 'rose',
      activeBorder: 'border-rose-500 shadow-rose-500/20 bg-rose-950/30',
      badgeClass: 'bg-rose-500/10 text-rose-400 border-rose-500/30',
      btnActive: 'bg-rose-600 text-white',
    },
    {
      id: 'PARTITION_AP_LAG',
      name: 'Cortar Redis Sync',
      badge: 'CAP: Disponibilidade Eventual (AP)',
      desc: 'Corta sincronização do cache. Compras passam, vitrine defasada.',
      icon: Radio,
      color: 'orange',
      activeBorder: 'border-orange-500 shadow-orange-500/20 bg-orange-950/30',
      badgeClass: 'bg-orange-500/10 text-orange-400 border-orange-500/30',
      btnActive: 'bg-orange-600 text-white',
    },
  ];

  return (
    <section className="mb-8">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <Flame className="w-5 h-5 text-rose-500" />
          <h2 className="text-base font-bold text-white tracking-wide uppercase font-mono">
            Painel de Injeção de Caos (Toxiproxy Controls)
          </h2>
        </div>
        <span className="text-xs font-mono text-slate-400">
          Cenário Atual: <strong className="text-emerald-400 uppercase">{activeScenario || 'HEALTHY'}</strong>
        </span>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {scenarios.map((s) => {
          const Icon = s.icon;
          const isActive = activeScenario === s.id;

          return (
            <button
              key={s.id}
              onClick={() => onSelectScenario(s.id)}
              disabled={isLoading}
              className={`p-4 rounded-xl border text-left transition-all duration-200 relative overflow-hidden group ${
                isActive
                  ? `${s.activeBorder} shadow-lg scale-[1.01]`
                  : 'border-slate-800 bg-slate-900/60 hover:border-slate-700 hover:bg-slate-900'
              }`}
            >
              <div className="flex items-start justify-between mb-3">
                <div
                  className={`p-2.5 rounded-lg ${
                    isActive ? s.btnActive : 'bg-slate-800 text-slate-400 group-hover:text-slate-200'
                  }`}
                >
                  <Icon className="w-5 h-5" />
                </div>
                {isActive && (
                  <span className="flex h-2.5 w-2.5 relative">
                    <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                    <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-emerald-500"></span>
                  </span>
                )}
              </div>

              <h3 className="font-bold text-white text-sm mb-1">{s.name}</h3>

              <div className="mb-2">
                <span className={`inline-block text-[11px] font-mono px-2 py-0.5 rounded border ${s.badgeClass}`}>
                  {s.badge}
                </span>
              </div>

              <p className="text-xs text-slate-400 leading-relaxed">{s.desc}</p>
            </button>
          );
        })}
      </div>
    </section>
  );
}
