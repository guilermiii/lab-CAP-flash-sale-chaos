import React, { useState, useEffect, useCallback } from 'react';
import Header from './components/Header';
import ChaosControls from './components/ChaosControls';
import ComparisonView from './components/ComparisonView';
import TelemetryFeed from './components/TelemetryFeed';
import {
  fetchCatalog,
  fetchChaosStatus,
  fetchTelemetry,
  buyTicket,
  applyChaosScenario,
  resetEnvironment,
} from './api/client';

export default function App() {
  const [catalog, setCatalog] = useState(null);
  const [chaosStatus, setChaosStatus] = useState(null);
  const [telemetry, setTelemetry] = useState(null);

  const [isBuying, setIsBuying] = useState(false);
  const [isResetting, setIsResetting] = useState(false);
  const [isChangingScenario, setIsChangingScenario] = useState(false);
  const [bannerNotice, setBannerNotice] = useState(null);

  // Função principal de polling (500ms)
  const pollData = useCallback(async () => {
    try {
      const [catData, chaosData, telemData] = await Promise.allSettled([
        fetchCatalog(),
        fetchChaosStatus(),
        fetchTelemetry(),
      ]);

      if (catData.status === 'fulfilled') setCatalog(catData.value);
      if (chaosData.status === 'fulfilled') setChaosStatus(chaosData.value);
      if (telemData.status === 'fulfilled') setTelemetry(telemData.value);
    } catch (err) {
      console.error('Polling error:', err);
    }
  }, []);

  useEffect(() => {
    pollData();
    const interval = setInterval(pollData, 500);
    return () => clearInterval(interval);
  }, [pollData]);

  // Ação manual de compra
  const handleBuy = async () => {
    setIsBuying(true);
    try {
      const result = await buyTicket();
      if (result.status === 201) {
        showNotice('success', `Compra aprovada! Pedido #${result.data?.orderId || ''} garantido (ACID).`);
      } else if (result.status === 409) {
        showNotice('warning', 'Ingressos esgotados! (409 Conflict)');
      } else if (result.status === 503) {
        showNotice('error', 'Venda bloqueada por partição de rede no banco! (503 Service Unavailable - Garantia CP).');
      }
      await pollData();
    } catch (err) {
      showNotice('error', `Falha ao tentar comprar: ${err.message}`);
    } finally {
      setIsBuying(false);
    }
  };

  // Ação de reset
  const handleReset = async () => {
    setIsResetting(true);
    try {
      await resetEnvironment();
      showNotice('success', 'Ambiente resetado: 50 ingressos restaurados e rede HEALTHY.');
      await pollData();
    } catch (err) {
      showNotice('error', `Falha ao resetar: ${err.message}`);
    } finally {
      setIsResetting(false);
    }
  };

  // Ação de seleção de cenário Toxiproxy
  const handleSelectScenario = async (scenarioType) => {
    setIsChangingScenario(true);
    try {
      await applyChaosScenario(scenarioType);
      showNotice('info', `Topologia de rede alterada para: ${scenarioType}`);
      await pollData();
    } catch (err) {
      showNotice('error', `Falha ao aplicar cenário: ${err.message}`);
    } finally {
      setIsChangingScenario(false);
    }
  };

  const showNotice = (type, message) => {
    setBannerNotice({ type, message });
    setTimeout(() => {
      setBannerNotice(null);
    }, 4000);
  };

  return (
    <div className="min-h-screen flex flex-col bg-[#0B0F19]">
      <Header
        onBuy={handleBuy}
        onReset={handleReset}
        isBuying={isBuying}
        isResetting={isResetting}
      />

      {/* Floating Feedback Notification */}
      {bannerNotice && (
        <div className="max-w-7xl mx-auto px-6 w-full pt-4">
          <div
            className={`p-3 rounded-xl border text-sm font-medium flex items-center justify-between shadow-lg transition-all ${
              bannerNotice.type === 'success'
                ? 'bg-emerald-950/80 border-emerald-500/50 text-emerald-200'
                : bannerNotice.type === 'warning'
                ? 'bg-amber-950/80 border-amber-500/50 text-amber-200'
                : bannerNotice.type === 'error'
                ? 'bg-rose-950/80 border-rose-500/50 text-rose-200'
                : 'bg-blue-950/80 border-blue-500/50 text-blue-200'
            }`}
          >
            <span>{bannerNotice.message}</span>
            <button
              onClick={() => setBannerNotice(null)}
              className="text-xs opacity-75 hover:opacity-100 font-bold ml-4"
            >
              ✕
            </button>
          </div>
        </div>
      )}

      <main className="flex-1 max-w-7xl w-full mx-auto px-6 py-6">
        {/* Painel de Injeção de Caos (US05) */}
        <ChaosControls
          activeScenario={chaosStatus?.activeScenario || 'HEALTHY'}
          onSelectScenario={handleSelectScenario}
          isLoading={isChangingScenario}
        />

        {/* Comparador Lado a Lado ACID vs BASE (US06) */}
        <ComparisonView
          catalog={catalog}
          chaosStatus={chaosStatus}
          telemetry={telemetry}
        />

        {/* Stream de Telemetria ao Vivo (US07) */}
        <TelemetryFeed telemetry={telemetry} />
      </main>

      <footer className="border-t border-slate-800/80 bg-slate-950/40 py-4 px-6 text-center text-xs text-slate-500 font-mono">
        FlashSale Chaos Lab &bull; Demonstração Prática de Teorema CAP (CP vs AP) &bull; Java 25 &bull; Spring Boot 4 &bull; React &bull; Toxiproxy
      </footer>
    </div>
  );
}
