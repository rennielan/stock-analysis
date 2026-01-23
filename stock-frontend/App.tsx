import React, { useState, useEffect } from 'react';
import { LayoutDashboard } from 'lucide-react';

import { StockData, StrategyType } from './types';
import { stockApi } from './services/stockApi';
import StockRow from './components/StockCard';
import AddStockForm from './components/AddStockForm';

const App: React.FC = () => {
  // --- State Management ---
  const [stocks, setStocks] = useState<StockData[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // --- Data Fetching ---
  useEffect(() => {
    fetchStocks();

    // 设置轮询，每30秒刷新一次数据，以获取ETL更新的最新价格
    const intervalId = setInterval(fetchStocks, 30000);
    return () => clearInterval(intervalId);
  }, []);

  const fetchStocks = async () => {
    try {
      // 仅在首次加载时显示全屏loading，后续静默更新
      if (stocks.length === 0) setLoading(true);

      const data = await stockApi.getAllStocks();
      setStocks(data);
      setError(null);
    } catch (err) {
      console.error("Failed to fetch stocks", err);
      if (stocks.length === 0) {
        setError("无法连接到服务器，请稍后重试");
      }
    } finally {
      setLoading(false);
    }
  };

  // --- Handlers ---
  const handleAddStock = async (symbol: string) => {
    const newStock: Partial<StockData> = {
      symbol,
      currentPrice: 0, // 初始价格为0，等待ETL更新
      changePercent: 0,
      strategy: StrategyType.WATCH,
      targetPrice: '',
      stopLoss: '',
      confidence: 3,
      notes: ''
    };

    try {
      const createdStock = await stockApi.createStock(newStock);
      setStocks(prev => [createdStock, ...prev]);
      // 添加成功后立即触发一次刷新，尝试获取最新状态
      setTimeout(fetchStocks, 1000);
    } catch (err) {
      console.error("Failed to add stock", err);
      alert("添加股票失败，请重试");
    }
  };

  const handleUpdateStock = async (id: string, updates: Partial<StockData>) => {
    // 乐观更新 UI
    setStocks(prev => prev.map(stock => 
      stock.id === id ? { ...stock, ...updates } : stock
    ));

    try {
      await stockApi.updateStock(id, updates);
    } catch (err) {
      console.error("Failed to update stock", err);
      fetchStocks(); // 回滚
    }
  };

  const handleRemoveStock = async (id: string) => {
    // 乐观更新 UI
    setStocks(prev => prev.filter(stock => stock.id !== id));

    try {
      await stockApi.deleteStock(id);
    } catch (err) {
      console.error("Failed to delete stock", err);
      fetchStocks(); // 回滚
    }
  };

  // --- Render ---
  return (
    <div className="min-h-screen bg-slate-950 text-slate-200 font-sans selection:bg-blue-500/30">
      
      {/* Navbar */}
      <header className="fixed top-0 left-0 right-0 z-10 bg-slate-950/80 backdrop-blur-md border-b border-slate-800">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-20 flex items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-blue-600 rounded-lg shadow-lg shadow-blue-900/20">
              <LayoutDashboard size={24} className="text-white" />
            </div>
            <h1 className="text-xl font-bold tracking-tight text-white hidden sm:block">
              StockWatch
            </h1>
          </div>

          <div className="flex-1 max-w-md flex justify-end sm:justify-center">
            <AddStockForm onAdd={handleAddStock} />
          </div>

          <div className="w-[100px] hidden sm:block text-right text-xs text-slate-500 font-mono">
            v1.2.0 Live
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-28 pb-12">
        
        {loading && stocks.length === 0 ? (
          <div className="flex justify-center items-center min-h-[50vh]">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500"></div>
          </div>
        ) : error ? (
          <div className="flex flex-col items-center justify-center min-h-[50vh] text-center">
            <div className="text-rose-500 mb-4">{error}</div>
            <button
              onClick={fetchStocks}
              className="px-4 py-2 bg-slate-800 hover:bg-slate-700 rounded-lg text-sm transition-colors"
            >
              重试
            </button>
          </div>
        ) : stocks.length === 0 ? (
          <div className="flex flex-col items-center justify-center min-h-[50vh] text-center border-2 border-dashed border-slate-800 rounded-2xl p-8">
            <div className="bg-slate-900 p-4 rounded-full mb-4">
              <LayoutDashboard size={32} className="text-slate-600" />
            </div>
            <h3 className="text-lg font-medium text-slate-300">您的关注列表为空</h3>
            <p className="text-slate-500 mt-2 max-w-sm">
              在上方输入股票代码，开始制定您的交易计划。
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto border border-slate-800 rounded-xl bg-slate-950/50 shadow-sm ring-1 ring-white/5">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-slate-800 bg-slate-900/50 text-[10px] uppercase tracking-wider text-slate-500 font-semibold font-mono">
                  <th className="p-4 w-32">代码</th>
                  <th className="p-4 w-28">价格</th>
                  <th className="p-4 w-32">策略</th>
                  <th className="p-4 w-40">交易计划</th>
                  <th className="p-4 w-24">空间 %</th>
                  <th className="p-4 w-24">盈亏比</th>
                  <th className="p-4 w-32">信心</th>
                  <th className="p-4">备注</th>
                  <th className="p-4 w-12"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/50">
                {stocks.map(stock => (
                  <StockRow 
                    key={stock.id} 
                    data={stock} 
                    onUpdate={handleUpdateStock} 
                    onRemove={handleRemoveStock} 
                  />
                ))}
              </tbody>
            </table>
          </div>
        )}
      </main>
    </div>
  );
};

export default App;