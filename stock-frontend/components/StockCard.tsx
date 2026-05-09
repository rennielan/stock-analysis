import React, { useState, useEffect } from 'react';
import { ExternalLink, Trash2, DollarSign, Target, History } from 'lucide-react';
import { StockData } from '../types';

interface StockCardProps {
  data: StockData;
  onUpdate: (id: string, field: Partial<StockData>) => void;
  onRemove: (id: string) => void;
}

const StockRow: React.FC<StockCardProps> = ({ data, onUpdate, onRemove }) => {
  const [localData, setLocalData] = useState(data);
  const [showTrades, setShowTrades] = useState(false);

  // Sync local state when prop data changes (e.g. from ETL update)
  useEffect(() => {
    setLocalData(data);
  }, [data]);

  const priceColor = localData.changePercent >= 0 ? 'text-emerald-400' : 'text-rose-400';
  
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

  const hasTrades = data.tradeRecords && data.tradeRecords.length > 0;

  return (
    <>
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
            {data.marketValue ? (
              <div className="font-bold text-sm text-slate-100 mt-1.5 font-mono">
                {data.marketValue.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
              </div>
            ) : (
              <span className="text-slate-700 text-xs mt-1.5">-</span>
            )}
          </div>
        </td>

        {/* 2. PLAN (Buy, Target) */}
        <td className="p-4 align-top">
          <div className="flex items-center gap-4">
            {/* Buy Price */}
            <div className="flex flex-col gap-1">
              <div className="flex items-center gap-1 text-[10px] text-slate-500 font-mono uppercase">
                <DollarSign size={10} /> 买入
              </div>
              <input
                type="number"
                value={localData.buyPrice || ''}
                onChange={(e) => setLocalData({ ...localData, buyPrice: e.target.value })}
                onBlur={(e) => handleBlurOrEnter('buyPrice', e.target.value)}
                onKeyDown={(e) => handleKeyDown(e, 'buyPrice', localData.buyPrice)}
                placeholder="-"
                className="w-20 bg-transparent border-b border-slate-800 text-sm text-slate-200 font-mono focus:border-blue-500 focus:ring-0 px-0 py-0.5 placeholder-slate-700 transition-colors"
              />
            </div>

            {/* Target */}
            <div className="flex flex-col gap-1">
              <div className="flex items-center gap-1 text-[10px] text-slate-500 font-mono uppercase">
                <Target size={10} /> 目标
              </div>
              <input
                type="number"
                value={localData.targetPrice}
                onChange={(e) => setLocalData({ ...localData, targetPrice: e.target.value })}
                onBlur={(e) => handleBlurOrEnter('targetPrice', e.target.value)}
                onKeyDown={(e) => handleKeyDown(e, 'targetPrice', localData.targetPrice)}
                placeholder="-"
                className="w-20 bg-transparent border-b border-slate-800 text-sm text-slate-200 font-mono focus:border-emerald-500 focus:ring-0 px-0 py-0.5 placeholder-slate-700 transition-colors"
              />
            </div>
          </div>
        </td>

        {/* 3. HOLDINGS INFO - 持仓信息 */}
        <td className="p-4 align-top">
          {data.referenceShares ? (
            <div className="font-mono text-sm text-slate-200">
              {data.referenceShares.toFixed(2)}
            </div>
          ) : (
            <span className="text-slate-700 text-xs">-</span>
          )}
        </td>

        {/* 4. COST / CURRENT PRICE - 成本/现价 */}
        <td className="p-4 align-top">
          {data.costPrice ? (
            <div className="font-mono text-sm text-slate-200">
              {data.costPrice.toFixed(4)}
            </div>
          ) : (
            <span className="text-slate-700 text-xs">-</span>
          )}
          {data.currentPrice ? (
            <div className="font-mono text-sm text-slate-200 mt-0.5">
              {data.currentPrice.toFixed(4)}
            </div>
          ) : (
            <span className="text-slate-700 text-xs mt-0.5">-</span>
          )}
        </td>

        {/* 5. PROFIT/LOSS - 盈亏 */}
        <td className="p-4 align-top">
          {data.profitLoss !== undefined && data.profitLoss !== 0 ? (
            <div className={`font-mono text-sm font-medium ${data.profitLoss > 0 ? 'text-emerald-400' : 'text-rose-400'}`}>
              {data.profitLoss > 0 ? '+' : ''}{data.profitLoss.toFixed(2)}
            </div>
          ) : (
            <span className="text-slate-700 text-xs">-</span>
          )}
          {data.profitLossRatio !== undefined && data.profitLossRatio !== 0 ? (
            <div className={`font-mono text-xs mt-0.5 ${data.profitLossRatio > 0 ? 'text-emerald-400' : 'text-rose-400'}`}>
              {data.profitLossRatio > 0 ? '+' : ''}{data.profitLossRatio.toFixed(2)}%
            </div>
          ) : (
            <span className="text-slate-700 text-xs">-</span>
          )}
        </td>

        {/* 6. DAILY P&L - 当日盈亏 */}
        <td className="p-4 align-top">
          {data.dailyProfitLoss !== undefined && data.dailyProfitLoss !== 0 ? (
            <div className={`font-mono text-sm font-medium ${data.dailyProfitLoss > 0 ? 'text-emerald-400' : 'text-rose-400'}`}>
              {data.dailyProfitLoss > 0 ? '+' : ''}{data.dailyProfitLoss.toFixed(2)}
            </div>
          ) : (
            <span className="text-slate-700 text-xs">-</span>
          )}
          <div className={`font-mono text-xs mt-0.5 ${priceColor}`}>
            {data.changePercent >= 0 ? '+' : ''}{data.changePercent.toFixed(2)}%
          </div>
        </td>

        {/* 7. NOTES & TRADES TOGGLE */}
        <td className="p-4 align-top">
          <div className="flex flex-col gap-2">
            <input
              type="text"
              value={localData.notes}
              onChange={(e) => setLocalData({ ...localData, notes: e.target.value })}
              onBlur={(e) => handleBlurOrEnter('notes', e.target.value)}
              onKeyDown={(e) => handleKeyDown(e, 'notes', localData.notes)}
              placeholder="添加逻辑/备注..."
              className="w-full min-w-[140px] bg-transparent text-sm text-slate-300 placeholder-slate-700 border-0 focus:ring-0 px-0 py-0 transition-all focus:placeholder-slate-600"
            />
            {hasTrades && (
              <button
                onClick={() => setShowTrades(!showTrades)}
                className="flex items-center gap-1 text-[10px] text-blue-400 hover:text-blue-300 w-fit mt-1 bg-blue-900/20 px-2 py-1 rounded border border-blue-800/50 transition-colors"
              >
                <History size={10} />
                {showTrades ? '隐藏交易记录' : `查看交易记录 (${data.tradeRecords!.length})`}
              </button>
            )}
          </div>
        </td>

        {/* 8. ACTIONS */}
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

      {/* Expanded Trade Records Row */}
      {showTrades && hasTrades && (
        <tr className="bg-slate-900/60 border-b border-slate-800/50">
          <td colSpan={8} className="p-4 pl-12">
            <div className="bg-slate-950 rounded-lg border border-slate-800 overflow-hidden">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-900/80 text-slate-400 border-b border-slate-800">
                  <tr>
                    <th className="px-4 py-2 font-medium">交易时间</th>
                    <th className="px-4 py-2 font-medium">方向</th>
                    <th className="px-4 py-2 font-medium text-right">价格</th>
                    <th className="px-4 py-2 font-medium text-right">数量</th>
                    <th className="px-4 py-2 font-medium text-right">总金额</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/50">
                  {data.tradeRecords!.map((trade) => (
                    <tr key={trade.id} className="hover:bg-slate-800/30 transition-colors">
                      <td className="px-4 py-2 font-mono text-slate-300">
                        {new Date(trade.tradeTime).toLocaleString('zh-CN', {
                          year: 'numeric', month: '2-digit', day: '2-digit',
                          hour: '2-digit', minute: '2-digit'
                        })}
                      </td>
                      <td className="px-4 py-2">
                        <span className={`inline-block px-2 py-0.5 rounded text-[10px] font-bold ${
                          trade.businessName && trade.businessName.includes('买入')
                            ? 'bg-rose-900/30 text-rose-400 border border-rose-800/50'
                            : 'bg-emerald-900/30 text-emerald-400 border border-emerald-800/50'
                        }`}>
                          {trade.businessName && trade.businessName.includes('买入') ? '买入' : '卖出'}
                        </span>
                      </td>
                      <td className="px-4 py-2 text-right font-mono text-slate-300">
                        ${trade.tradePrice.toFixed(3)}
                      </td>
                      <td className="px-4 py-2 text-right font-mono text-slate-300">
                        {trade.quantity.toLocaleString()}
                      </td>
                      <td className="px-4 py-2 text-right font-mono text-slate-300">
                        ${trade.totalAmount ? trade.totalAmount.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '-'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </td>
        </tr>
      )}
    </>
  );
};

export default StockRow;