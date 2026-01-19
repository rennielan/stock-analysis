import React, { useMemo } from 'react';
import { ExternalLink, Trash2, TrendingUp, TrendingDown, Target, ShieldAlert, ArrowRight } from 'lucide-react';
import { StockData, StrategyType, STRATEGY_CONFIG } from '../types';
import { calculateRiskReward } from '../utils';
import StarRating from './StarRating';

interface StockCardProps {
  data: StockData;
  onUpdate: (id: string, field: Partial<StockData>) => void;
  onRemove: (id: string) => void;
}

const StockRow: React.FC<StockCardProps> = ({ data, onUpdate, onRemove }) => {
  
  // Calculate R:R
  const rrRatio = useMemo(() => {
    return calculateRiskReward(data.currentPrice, data.targetPrice, data.stopLoss);
  }, [data.currentPrice, data.targetPrice, data.stopLoss]);

  // Calculate Distance (Upside/Downside to Target)
  const distanceToTarget = useMemo(() => {
    const target = parseFloat(data.targetPrice);
    if (!data.targetPrice || isNaN(target) || data.currentPrice === 0) return null;
    const diffPercent = ((target - data.currentPrice) / data.currentPrice) * 100;
    return diffPercent;
  }, [data.currentPrice, data.targetPrice]);

  const priceColor = data.changePercent >= 0 ? 'text-emerald-400' : 'text-rose-400';
  const StrategyIcon = data.changePercent >= 0 ? TrendingUp : TrendingDown;
  
  const handleInputChange = (field: keyof StockData, value: string | number | StrategyType) => {
    onUpdate(data.id, { [field]: value });
  };

  return (
    <tr className="group border-b border-slate-800/50 hover:bg-slate-900/40 transition-colors">
      
      {/* 1. SYMBOL & PRICE */}
      <td className="p-4 align-top">
        <div className="flex flex-col">
          <a 
            href={`https://finance.yahoo.com/quote/${data.symbol}`}
            target="_blank"
            rel="noreferrer"
            className="font-bold text-slate-100 hover:text-blue-400 transition-colors flex items-center gap-1 group/link"
          >
            {data.symbol}
            <ExternalLink size={10} className="opacity-0 group-hover/link:opacity-100 transition-opacity text-slate-500" />
          </a>
          <span className="text-xs text-slate-500 font-mono mt-0.5">{data.symbol}</span>
        </div>
      </td>

      <td className="p-4 align-top">
        <div className="font-mono text-slate-200">${data.currentPrice.toFixed(2)}</div>
        <div className={`text-xs flex items-center gap-1 ${priceColor}`}>
          {data.changePercent >= 0 ? '+' : ''}{data.changePercent}%
        </div>
      </td>

      {/* 3. STRATEGY */}
      <td className="p-4 align-top">
        <select
          value={data.strategy}
          onChange={(e) => handleInputChange('strategy', e.target.value as StrategyType)}
          className={`bg-transparent text-xs font-medium py-1 px-2 rounded border focus:outline-none cursor-pointer ${STRATEGY_CONFIG[data.strategy].color}`}
        >
          {Object.values(StrategyType).map((strat) => (
            <option key={strat} value={strat} className="bg-slate-900 text-slate-300">
              {STRATEGY_CONFIG[strat].label}
            </option>
          ))}
        </select>
      </td>

      {/* 4. PLAN (Target & Stop stacked) */}
      <td className="p-4 align-top">
        <div className="flex flex-col gap-2 w-32">
          {/* Target */}
          <div className="flex items-center group/input">
            <Target size={12} className="text-emerald-500/50 mr-2 shrink-0 group-focus-within/input:text-emerald-500" />
            <input 
              type="number"
              value={data.targetPrice}
              onChange={(e) => handleInputChange('targetPrice', e.target.value)}
              placeholder="目标价"
              className="w-full bg-transparent border-0 border-b border-slate-800 text-sm text-slate-200 font-mono focus:border-emerald-500 focus:ring-0 px-0 py-0.5 placeholder-slate-700 transition-colors"
            />
          </div>
          {/* Stop */}
          <div className="flex items-center group/input">
            <ShieldAlert size={12} className="text-rose-500/50 mr-2 shrink-0 group-focus-within/input:text-rose-500" />
            <input 
              type="number"
              value={data.stopLoss}
              onChange={(e) => handleInputChange('stopLoss', e.target.value)}
              placeholder="止损价"
              className="w-full bg-transparent border-0 border-b border-slate-800 text-sm text-slate-200 font-mono focus:border-rose-500 focus:ring-0 px-0 py-0.5 placeholder-slate-700 transition-colors"
            />
          </div>
        </div>
      </td>

      {/* 5. DISTANCE (Calculated) */}
      <td className="p-4 align-top">
        {distanceToTarget !== null ? (
          <div className={`font-mono text-sm font-medium flex items-center gap-1 ${distanceToTarget > 0 ? 'text-emerald-400' : distanceToTarget < 0 ? 'text-rose-400' : 'text-slate-400'}`}>
             {distanceToTarget > 0 ? '+' : ''}{distanceToTarget.toFixed(2)}%
          </div>
        ) : (
          <span className="text-slate-700 font-mono text-sm">-</span>
        )}
        <div className="text-[10px] text-slate-600 mt-0.5">至目标</div>
      </td>

      {/* 6. R:R */}
      <td className="p-4 align-top">
        {rrRatio ? (
          <span className="inline-block text-xs font-mono text-blue-400 bg-blue-900/10 border border-blue-900/30 px-2 py-1 rounded">
            {rrRatio}
          </span>
        ) : (
          <span className="text-slate-700 text-xs">-</span>
        )}
      </td>

      {/* 7. CONVICTION */}
      <td className="p-4 align-top">
        <StarRating value={data.confidence} onChange={(val) => handleInputChange('confidence', val)} />
      </td>

      {/* 8. NOTES */}
      <td className="p-4 align-top">
        <input 
          type="text"
          value={data.notes}
          onChange={(e) => handleInputChange('notes', e.target.value)}
          placeholder="添加逻辑/备注..."
          className="w-full min-w-[140px] bg-transparent text-sm text-slate-300 placeholder-slate-700 border-0 focus:ring-0 px-0 py-0 transition-all focus:placeholder-slate-600"
        />
      </td>

      {/* 9. ACTIONS */}
      <td className="p-4 align-top text-right">
        <button 
          onClick={() => onRemove(data.id)}
          className="text-slate-600 hover:text-rose-500 p-1.5 rounded-md hover:bg-rose-950/30 transition-all opacity-0 group-hover:opacity-100 focus:opacity-100"
          title="删除"
        >
          <Trash2 size={16} />
        </button>
      </td>
    </tr>
  );
};

export default StockRow;