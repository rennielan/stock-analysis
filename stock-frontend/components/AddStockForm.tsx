import React, { useState } from 'react';
import { Plus, Search } from 'lucide-react';

interface AddStockFormProps {
  onAdd: (symbol: string) => void;
}

const AddStockForm: React.FC<AddStockFormProps> = ({ onAdd }) => {
  const [symbol, setSymbol] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (symbol.trim()) {
      onAdd(symbol.toUpperCase().trim());
      setSymbol('');
    }
  };

  return (
    <form onSubmit={handleSubmit} className="w-full max-w-md relative">
      <div className="relative group">
        <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
          <Search className="h-5 w-5 text-slate-500 group-focus-within:text-blue-400 transition-colors" />
        </div>
        <input
          type="text"
          value={symbol}
          onChange={(e) => setSymbol(e.target.value)}
          className="block w-full pl-10 pr-20 py-3 bg-slate-900 border border-slate-800 rounded-xl leading-5 text-slate-200 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-blue-500 transition-all font-mono"
          placeholder="输入股票代码 (如 NVDA)"
        />
        <button
          type="submit"
          disabled={!symbol}
          className="absolute inset-y-1 right-1 px-4 flex items-center bg-blue-600 hover:bg-blue-500 text-white text-sm font-medium rounded-lg disabled:opacity-50 disabled:bg-slate-800 disabled:text-slate-500 transition-all"
        >
          <Plus size={16} className="mr-1" />
          添加
        </button>
      </div>
    </form>
  );
};

export default AddStockForm;