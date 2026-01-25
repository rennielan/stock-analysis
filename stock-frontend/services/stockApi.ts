import { StockData, StrategyType } from '../types';

const API_BASE_URL = '/api/stocks';

// Helper to convert Backend format to Frontend format
const transformToFrontend = (data: any): StockData => ({
  id: String(data.id),
  code: data.code, // 新增 code
  symbol: data.symbol,
  name: data.name,
  currentPrice: data.currentPrice,
  changePercent: data.changePercent,
  strategy: data.strategy as StrategyType,
  targetPrice: data.targetPrice !== null ? String(data.targetPrice) : '',
  stopLoss: data.stopLoss !== null ? String(data.stopLoss) : '',
  confidence: data.confidence,
  notes: data.notes || ''
});

// Helper to convert Frontend format to Backend format
const transformToBackend = (data: Partial<StockData>): any => {
  const payload: any = { ...data };

  // Convert empty strings to null for numeric fields
  if (payload.targetPrice === '') payload.targetPrice = null;
  if (payload.stopLoss === '') payload.stopLoss = null;

  return payload;
};

export const stockApi = {
  getAllStocks: async (): Promise<StockData[]> => {
    const response = await fetch(API_BASE_URL);
    if (!response.ok) {
      throw new Error('Failed to fetch stocks');
    }
    const data = await response.json();
    return data.map(transformToFrontend);
  },

  createStock: async (stock: Partial<StockData>): Promise<StockData> => {
    const response = await fetch(API_BASE_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(transformToBackend(stock)),
    });
    if (!response.ok) {
      throw new Error('Failed to create stock');
    }
    const data = await response.json();
    return transformToFrontend(data);
  },

  updateStock: async (id: string, updates: Partial<StockData>): Promise<StockData> => {
    const response = await fetch(`${API_BASE_URL}/${id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(transformToBackend(updates)),
    });
    if (!response.ok) {
      throw new Error('Failed to update stock');
    }
    const data = await response.json();
    return transformToFrontend(data);
  },

  deleteStock: async (id: string): Promise<void> => {
    const response = await fetch(`${API_BASE_URL}/${id}`, {
      method: 'DELETE',
    });
    if (!response.ok) {
      throw new Error('Failed to delete stock');
    }
  },

  searchStocks: async (keyword: string): Promise<any[]> => {
    const response = await fetch(`${API_BASE_URL}/search?keyword=${encodeURIComponent(keyword)}`);
    if (!response.ok) {
      throw new Error('Failed to search stocks');
    }
    return response.json();
  }
};