export enum StrategyType {
  WATCH = 'WATCH',
  BUY_READY = 'BUY_READY',
  SELL_READY = 'SELL_READY',
  HOLDING = 'HOLDING'
}

export interface TradeRecord {
  id: string;
  stockName: string;
  tradeTime: string;
  quantity: number;
  tradeDirection: string;
  tradePrice: number;
  totalAmount: number;
}

export interface StockData {
  id: string;
  code: string; // 新增 code (如 sh.600000)
  symbol: string; // 纯数字代码 (如 600000)
  name?: string;
  currentPrice: number;
  changePercent: number;
  
  // User Editable Plan
  strategy: StrategyType;
  buyPrice: string;    // 买入价
  targetPrice: string; // Keep as string for input handling, parse for calc
  stopLoss: string;    // Keep as string for input handling
  confidence: number;  // 1-5
  notes: string;

  // Holdings (持仓信息)
  referenceShares?: number | null;  // 参考持股数量
  costPrice?: number | null;        // 成本价
  marketValue?: number;             // 持仓市值 (衍生字段)
  totalCost?: number;               // 持仓成本 (衍生字段)
  profitLoss?: number;              // 持仓盈亏 (衍生字段)
  profitLossRatio?: number;         // 盈亏比例 (衍生字段)
  perShareProfitLoss?: number;      // 单股盈亏 (衍生字段)
  dailyProfitLoss?: number;         // 当日盈亏

  // Trades
  tradeRecords?: TradeRecord[];
}

export const STRATEGY_CONFIG = {
  [StrategyType.WATCH]: { label: '👀 观望', color: 'bg-slate-700 text-slate-200 border-slate-600' },
  [StrategyType.BUY_READY]: { label: '🟢 准备买入', color: 'bg-emerald-900/30 text-emerald-400 border-emerald-800' },
  [StrategyType.SELL_READY]: { label: '🔴 准备卖出', color: 'bg-rose-900/30 text-rose-400 border-rose-800' },
  [StrategyType.HOLDING]: { label: '🔒 持仓中', color: 'bg-blue-900/30 text-blue-400 border-blue-800' },
};