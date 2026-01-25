import React, { useState, useEffect, useRef } from 'react';
import { Plus, Search, Loader2 } from 'lucide-react';
import { stockApi } from '../services/stockApi';

interface AddStockFormProps {
  onAdd: (code: string) => void;
}

interface SearchResult {
  code: string;
  symbol: string;
  name: string;
}

const AddStockForm: React.FC<AddStockFormProps> = ({ onAdd }) => {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<SearchResult[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [showResults, setShowResults] = useState(false);
  const wrapperRef = useRef<HTMLDivElement>(null);

  // Debounce search
  useEffect(() => {
    const timer = setTimeout(async () => {
      if (query.trim().length >= 2) {
        setIsLoading(true);
        try {
          const data = await stockApi.searchStocks(query);
          setResults(data);
          setShowResults(true);
        } catch (error) {
          console.error("Search failed", error);
        } finally {
          setIsLoading(false);
        }
      } else {
        setResults([]);
        setShowResults(false);
      }
    }, 300);

    return () => clearTimeout(timer);
  }, [query]);

  // Click outside to close
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (wrapperRef.current && !wrapperRef.current.contains(event.target as Node)) {
        setShowResults(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleSelect = (stock: SearchResult) => {
    onAdd(stock.code);
    setQuery('');
    setResults([]);
    setShowResults(false);
  };

  return (
    <div ref={wrapperRef} className="w-full max-w-md relative">
      <div className="relative group">
        <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
          {isLoading ? (
            <Loader2 className="h-5 w-5 text-blue-500 animate-spin" />
          ) : (
            <Search className="h-5 w-5 text-slate-500 group-focus-within:text-blue-400 transition-colors" />
          )}
        </div>
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onFocus={() => { if (results.length > 0) setShowResults(true); }}
          className="block w-full pl-10 pr-4 py-3 bg-slate-900 border border-slate-800 rounded-xl leading-5 text-slate-200 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-blue-500 transition-all font-mono"
          placeholder="搜索股票代码或名称 (如 600000)"
        />
      </div>

      {/* Search Results Dropdown */}
      {showResults && results.length > 0 && (
        <div className="absolute z-50 w-full mt-1 bg-slate-900 border border-slate-800 rounded-xl shadow-xl max-h-60 overflow-y-auto">
          <ul className="py-1">
            {results.map((stock) => (
              <li key={stock.code}>
                <button
                  onClick={() => handleSelect(stock)}
                  className="w-full text-left px-4 py-2 hover:bg-slate-800 transition-colors flex items-center justify-between group"
                >
                  <div className="flex flex-col">
                    <span className="text-sm font-medium text-slate-200 group-hover:text-blue-400 transition-colors">
                      {stock.name}
                    </span>
                    <span className="text-xs text-slate-500 font-mono">
                      {stock.code}
                    </span>
                  </div>
                  <Plus size={16} className="text-slate-600 group-hover:text-blue-500 opacity-0 group-hover:opacity-100 transition-all" />
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}

      {showResults && query.length >= 2 && results.length === 0 && !isLoading && (
        <div className="absolute z-50 w-full mt-1 bg-slate-900 border border-slate-800 rounded-xl shadow-xl p-4 text-center text-slate-500 text-sm">
          未找到匹配的股票
        </div>
      )}
    </div>
  );
};

export default AddStockForm;