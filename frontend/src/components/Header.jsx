import React from 'react';
import { Activity, Zap, RefreshCw, ShoppingCart } from 'lucide-react';

export default function Header({ onBuy, onReset, isBuying, isResetting }) {
  return (
    <header className="border-b border-slate-800 bg-slate-900/60 backdrop-blur-md sticky top-0 z-40 px-6 py-4">
      <div className="max-w-7xl mx-auto flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-3">
            <div className="p-2 bg-gradient-to-tr from-emerald-500 to-cyan-500 rounded-xl shadow-lg shadow-emerald-500/20">
              <Zap className="w-6 h-6 text-slate-950 stroke-[2.5]" />
            </div>
            <div>
              <h1 className="text-xl md:text-2xl font-extrabold text-white tracking-tight flex items-center gap-2">
                FlashSale Chaos Lab
                <span className="text-xs px-2.5 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 font-mono font-medium">
                  v1.0 MVP
                </span>
              </h1>
              <p className="text-xs text-slate-400 font-medium">
                Sistemas Distribuídos &amp; Engenharia do Caos: <span className="text-emerald-400">ACID (CP)</span> vs <span className="text-amber-400">BASE (AP)</span>
              </p>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-3 flex-wrap">
          <button
            onClick={onBuy}
            disabled={isBuying}
            className="flex items-center gap-2 px-4 py-2 bg-emerald-600 hover:bg-emerald-500 active:scale-95 disabled:opacity-50 text-white font-semibold text-sm rounded-lg transition-all shadow-md shadow-emerald-600/25"
          >
            <ShoppingCart className={`w-4 h-4 ${isBuying ? 'animate-bounce' : ''}`} />
            {isBuying ? 'Processando...' : 'Comprar 1 Ingresso'}
          </button>

          <button
            onClick={onReset}
            disabled={isResetting}
            className="flex items-center gap-2 px-4 py-2 bg-slate-800 hover:bg-slate-700 active:scale-95 disabled:opacity-50 text-slate-200 font-semibold text-sm rounded-lg transition-all border border-slate-700"
          >
            <RefreshCw className={`w-4 h-4 ${isResetting ? 'animate-spin' : ''}`} />
            {isResetting ? 'Resetando...' : 'Resetar (50 Ingressos)'}
          </button>
        </div>
      </div>
    </header>
  );
}
