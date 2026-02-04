import React, { useMemo, useState, useEffect } from 'react';
import { ExternalLink, Trash2, TrendingUp, TrendingDown, Target, ShieldAlert, DollarSign } from 'lucide-react';
import { StockData, StrategyType, STRATEGY_CONFIG } from '../types';
import { calculateRiskReward } from '../utils';
import StarRating from './StarRating';

interface StockCardProps {
  data: StockData;
  onUpdate: (id: string, field: Partial<StockData>) => void;
  onRemove: (id: string) => void;
}

const StockRow: React.FC<StockCardProps> = ({ data, onUpdate, onRemove }) => {
  const [localData, setLocalData] = useState(data);

  // Sync local state when prop data changes (e.g. from ETL update)
  useEffect(() => {
    setLocalData(data);
  }, [data]);

  // Calculate R:R
  const rrRatio = useMemo(() => {
    const entryPrice = localData.buyPrice ? parseFloat(localData.buyPrice) : localData.currentPrice;
    return calculateRiskReward(entryPrice, localData.targetPrice, localData.stopLoss);
  }, [localData.currentPrice, localData.buyPrice, localData.targetPrice, localData.stopLoss]);

  // Calculate Distance (Upside/Downside to Target)
  const distanceToTarget = useMemo(() => {
    const target = parseFloat(localData.targetPrice);
    if (!localData.targetPrice || isNaN(target) || localData.currentPrice === 0) return null;
    const diffPercent = ((target - localData.currentPrice) / localData.currentPrice) * 100;
    return diffPercent;
  }, [localData.currentPrice, localData.targetPrice]);

  const priceColor = localData.changePercent >= 0 ? 'text-emerald-400' : 'text-rose-400';
  
  // 立即更新（用于下拉框和评分）
  const handleImmediateChange = (field: keyof StockData, value: string | number | StrategyType) => {
    const newData = { ...localData, [field]: value };
    setLocalData(newData);
    // 发送全量数据
    onUpdate(data.id, newData);
  };

  // 失去焦点或回车时更新（用于文本框）
  const handleBlurOrEnter = (field: keyof StockData, value: string) => {
    if (value !== data[field]) {
      // 发送全量数据
      const newData = { ...localData, [field]: value };
      onUpdate(data.id, newData);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent, field: keyof StockData, value: string) => {
    if (e.key === 'Enter') {
      (e.target as HTMLInputElement).blur(); // 触发 blur 事件
    }
  };

  return (
    <tr className="group border-b border-slate-800/50 hover:bg-slate-900/40 transition-colors">
      
      {/* 1. SYMBOL & PRICE */}
      <td className="p-4 align-top">
        <div className="flex flex-col">
          <div className="flex items-center gap-2">
             <a
              href={`https://finance.yahoo.com/quote/${data.symbol}`}
              target="_blank"
              rel="noreferrer"
              className="font-bold text-slate-100 hover:text-blue-400 transition-colors flex items-center gap-1 group/link"
            >
              {data.name || data.symbol}
              <ExternalLink size={10} className="opacity-0 group-hover/link:opacity-100 transition-opacity text-slate-500" />
            </a>
          </div>
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
          value={localData.strategy}
          onChange={(e) => handleImmediateChange('strategy', e.target.value as StrategyType)}
          className={`bg-transparent text-xs font-medium py-1 px-2 rounded border focus:outline-none cursor-pointer ${STRATEGY_CONFIG[localData.strategy].color}`}
        >
          {Object.values(StrategyType).map((strat) => (
            <option key={strat} value={strat} className="bg-slate-900 text-slate-300">
              {STRATEGY_CONFIG[strat].label}
            </option>
          ))}
        </select>
      </td>

      {/* 4. PLAN (Buy, Target & Stop stacked) */}
      <td className="p-4 align-top">
        <div className="flex flex-col gap-2 w-32">
          {/* Buy Price */}
          <div className="flex items-center group/input">
            <DollarSign size={12} className="text-blue-500/50 mr-2 shrink-0 group-focus-within/input:text-blue-500" />
            <input
              type="number"
              value={localData.buyPrice || ''}
              onChange={(e) => setLocalData({ ...localData, buyPrice: e.target.value })}
              onBlur={(e) => handleBlurOrEnter('buyPrice', e.target.value)}
              onKeyDown={(e) => handleKeyDown(e, 'buyPrice', localData.buyPrice)}
              placeholder="买入价"
              className="w-full bg-transparent border-0 border-b border-slate-800 text-sm text-slate-200 font-mono focus:border-blue-500 focus:ring-0 px-0 py-0.5 placeholder-slate-700 transition-colors"
            />
          </div>
          {/* Target */}
          <div className="flex items-center group/input">
            <Target size={12} className="text-emerald-500/50 mr-2 shrink-0 group-focus-within/input:text-emerald-500" />
            <input 
              type="number"
              value={localData.targetPrice}
              onChange={(e) => setLocalData({ ...localData, targetPrice: e.target.value })}
              onBlur={(e) => handleBlurOrEnter('targetPrice', e.target.value)}
              onKeyDown={(e) => handleKeyDown(e, 'targetPrice', localData.targetPrice)}
              placeholder="目标价"
              className="w-full bg-transparent border-0 border-b border-slate-800 text-sm text-slate-200 font-mono focus:border-emerald-500 focus:ring-0 px-0 py-0.5 placeholder-slate-700 transition-colors"
            />
          </div>
          {/* Stop */}
          <div className="flex items-center group/input">
            <ShieldAlert size={12} className="text-rose-500/50 mr-2 shrink-0 group-focus-within/input:text-rose-500" />
            <input 
              type="number"
              value={localData.stopLoss}
              onChange={(e) => setLocalData({ ...localData, stopLoss: e.target.value })}
              onBlur={(e) => handleBlurOrEnter('stopLoss', e.target.value)}
              onKeyDown={(e) => handleKeyDown(e, 'stopLoss', localData.stopLoss)}
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
        <StarRating value={localData.confidence} onChange={(val) => handleImmediateChange('confidence', val)} />
      </td>

      {/* 8. NOTES */}
      <td className="p-4 align-top">
        <input 
          type="text"
          value={localData.notes}
          onChange={(e) => setLocalData({ ...localData, notes: e.target.value })}
          onBlur={(e) => handleBlurOrEnter('notes', e.target.value)}
          onKeyDown={(e) => handleKeyDown(e, 'notes', localData.notes)}
          placeholder="添加逻辑/备注..."
          className="w-full min-w-[140px] bg-transparent text-sm text-slate-300 placeholder-slate-700 border-0 focus:ring-0 px-0 py-0 transition-all focus:placeholder-slate-600"
        />
      </td>

      {/* 9. ACTIONS */}
      <td className="p-4 align-top text-right">
        <div className="flex items-center justify-end gap-1">
          <button
            onClick={() => onRemove(data.id)}
            className="text-slate-600 hover:text-rose-500 p-1.5 rounded-md hover:bg-rose-950/30 transition-all opacity-0 group-hover:opacity-100 focus:opacity-100"
            title="删除"
          >
            <Trash2 size={16} />
          </button>
        </div>
      </td>
    </tr>
  );
};

export default StockRow;