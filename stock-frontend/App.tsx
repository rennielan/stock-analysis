import React, { useState, useEffect } from 'react';
import { LayoutDashboard } from 'lucide-react';
import { v4 as uuidv4 } from 'uuid';

import { StockData, StrategyType } from './types';
import { generateMockStockData } from './utils';
import StockRow from './components/StockCard'; // Importing the Row component (formerly Card)
import AddStockForm from './components/AddStockForm';

const LOCAL_STORAGE_KEY = 'stockwatch_data';

const App: React.FC = () => {
  // --- State Management ---
  const [stocks, setStocks] = useState<StockData[]>(() => {
    // Initialize from LocalStorage
    try {
      const saved = localStorage.getItem(LOCAL_STORAGE_KEY);
      return saved ? JSON.parse(saved) : [];
    } catch (e) {
      console.error("Failed to load stocks", e);
      return [];
    }
  });

  // --- Persistence ---
  useEffect(() => {
    localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(stocks));
  }, [stocks]);

  // --- Handlers ---
  const handleAddStock = (symbol: string) => {
    const mockMarketData = generateMockStockData(symbol);
    
    const newStock: StockData = {
      id: uuidv4(),
      symbol,
      ...mockMarketData,
      strategy: StrategyType.WATCH,
      targetPrice: '',
      stopLoss: '',
      confidence: 3,
      notes: ''
    };

    setStocks(prev => [newStock, ...prev]);
  };

  const handleUpdateStock = (id: string, updates: Partial<StockData>) => {
    setStocks(prev => prev.map(stock => 
      stock.id === id ? { ...stock, ...updates } : stock
    ));
  };

  const handleRemoveStock = (id: string) => {
    setStocks(prev => prev.filter(stock => stock.id !== id));
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
            v1.1.0 CN
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-28 pb-12">
        
        {stocks.length === 0 ? (
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