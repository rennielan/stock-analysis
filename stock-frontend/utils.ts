/**
 * Generates random realistic stock data based on the symbol string.
 * This ensures the same symbol always gets the same "random" price 
 * within a session if re-added, or just pure random.
 */
export const generateMockStockData = (symbol: string) => {
  const seed = symbol.length + symbol.charCodeAt(0);
  const basePrice = (seed % 100) * 5 + 20; // Price between 20 and ~520
  
  // Random fluctuation between -5% and +5%
  const isPositive = Math.random() > 0.45;
  const changePercent = (Math.random() * 5 * (isPositive ? 1 : -1));
  
  return {
    currentPrice: parseFloat(basePrice.toFixed(2)),
    changePercent: parseFloat(changePercent.toFixed(2))
  };
};

/**
 * Calculates Risk/Reward Ratio
 * R = |Target - Entry| / |Entry - Stop|
 */
export const calculateRiskReward = (current: number, targetStr: string, stopStr: string): string | null => {
  const target = parseFloat(targetStr);
  const stop = parseFloat(stopStr);

  if (isNaN(target) || isNaN(stop) || isNaN(current)) return null;
  if (target === current || stop === current) return null;

  const reward = Math.abs(target - current);
  const risk = Math.abs(current - stop);

  if (risk === 0) return null;

  const ratio = reward / risk;
  return `1 : ${ratio.toFixed(1)}`;
};