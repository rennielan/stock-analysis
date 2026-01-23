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