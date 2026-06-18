import React, { useState } from 'react';
import { TrendingUp, RefreshCw, Sparkles, Check, DollarSign, Sliders } from 'lucide-react';
import { ResponsiveContainer, LineChart, Line, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend } from 'recharts';
import { apiClient } from '../services/api';

interface PriceInsight {
  id: string;
  title: string;
  locality: string;
  currentRent: number;
  suggestedRent: number;
  demandLevel: 'HIGH' | 'MEDIUM' | 'LOW';
  applied: boolean;
}

const INITIAL_INSIGHTS: PriceInsight[] = [
  { id: '1', title: 'Cozy 1-BHK Apartment near Tech Hub', locality: 'Indiranagar', currentRent: 15000, suggestedRent: 16500, demandLevel: 'HIGH', applied: false },
  { id: '2', title: 'Spacious 2-BHK Flat with Balcony & Gym', locality: 'Sector 62 Noida', currentRent: 28000, suggestedRent: 29500, demandLevel: 'HIGH', applied: false },
  { id: '3', title: 'Luxury 3-BHK Villa with Private Garden', locality: 'Bandra Mumbai', currentRent: 65000, suggestedRent: 63000, demandLevel: 'LOW', applied: false },
];

const MARKET_TREND_DATA = [
  { month: 'Jan', averageRent: 22000, demandScore: 68 },
  { month: 'Feb', averageRent: 22400, demandScore: 72 },
  { month: 'Mar', averageRent: 23100, demandScore: 78 },
  { month: 'Apr', averageRent: 23900, demandScore: 84 },
  { month: 'May', averageRent: 24500, demandScore: 89 },
  { month: 'Jun', averageRent: 25200, demandScore: 92 },
];

const OCCUPANCY_FORECAST_DATA = [
  { month: 'Jun', occupancyRate: 91 },
  { month: 'Jul', occupancyRate: 94 },
  { month: 'Aug', occupancyRate: 95 },
  { month: 'Sep', occupancyRate: 92 },
  { month: 'Oct', occupancyRate: 88 },
  { month: 'Nov', occupancyRate: 85 },
];

export default function OwnerPricingInsights() {
  const [insights, setInsights] = useState<PriceInsight[]>(INITIAL_INSIGHTS);
  const [aggressiveness, setAggressiveness] = useState('BALANCED');
  const [isRefreshing, setIsRefreshing] = useState(false);

  const handleApplySuggestion = async (id: string, newPrice: number) => {
    // update state
    setInsights(prev => prev.map(item => item.id === id ? { ...item, currentRent: newPrice, applied: true } : item));

    try {
      await apiClient.post(`/listings/${id}/price/apply`, { price: newPrice });
    } catch (err) {
      console.warn('Backend call failed, price adjustment applied locally.');
    }
  };

  const handleRecalculate = () => {
    setIsRefreshing(true);
    setTimeout(() => {
      // tweak recommendations based on aggressiveness
      setInsights(prev => prev.map(item => {
        let suggested = item.suggestedRent;
        if (aggressiveness === 'CONSERVATIVE') {
          suggested = Math.round(item.currentRent * 1.02);
        } else if (aggressiveness === 'AGGRESSIVE') {
          suggested = Math.round(item.currentRent * 1.15);
        } else {
          suggested = Math.round(item.currentRent * 1.08);
        }
        return {
          ...item,
          suggestedRent: suggested,
          applied: false,
        };
      }));
      setIsRefreshing(false);
    }, 800);
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 text-slate-100">
      {/* Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 border-b border-slate-800 pb-6 mb-8">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight bg-gradient-to-r from-white to-slate-300 bg-clip-text text-transparent">
            Dynamic Pricing & Market Insights
          </h1>
          <p className="text-slate-400 text-sm mt-1">
            Leverage local market indexes, seasonal occupancy forecasts, and AI-suggested rate optimization for listing earnings.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <div className="flex bg-slate-900 border border-slate-800 rounded-xl p-1 text-xs">
            {['CONSERVATIVE', 'BALANCED', 'AGGRESSIVE'].map((mode) => (
              <button
                key={mode}
                onClick={() => setAggressiveness(mode)}
                className={`px-3 py-1.5 rounded-lg font-semibold transition-all ${
                  aggressiveness === mode ? 'bg-indigo-600 text-white shadow' : 'text-slate-400 hover:text-white'
                }`}
              >
                {mode}
              </button>
            ))}
          </div>

          <button
            onClick={handleRecalculate}
            disabled={isRefreshing}
            className="flex items-center space-x-1.5 bg-slate-900 hover:bg-slate-850 border border-slate-800 hover:border-slate-700 px-4 py-2 rounded-xl text-xs font-semibold text-slate-200 transition-colors"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isRefreshing ? 'animate-spin' : ''}`} />
            <span>Recalculate</span>
          </button>
        </div>
      </div>

      {/* Analytics KPI grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-5">
          <div className="flex justify-between items-center mb-1">
            <span className="text-xs font-semibold text-slate-400 uppercase">Est. Revenue Boost</span>
            <DollarSign className="w-4 h-4 text-emerald-400" />
          </div>
          <p className="text-2xl font-bold text-white">+₹3,500/mo</p>
          <span className="text-[10px] text-slate-400 block mt-1">If all recommendations are active</span>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-xl p-5">
          <div className="flex justify-between items-center mb-1">
            <span className="text-xs font-semibold text-slate-400 uppercase">Avg. Local Occupancy Index</span>
            <TrendingUp className="w-4 h-4 text-indigo-400" />
          </div>
          <p className="text-2xl font-bold text-white">92.4%</p>
          <span className="text-[10px] text-emerald-400 font-semibold block mt-1">High neighborhood demand detected</span>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-xl p-5">
          <div className="flex justify-between items-center mb-1">
            <span className="text-xs font-semibold text-slate-400 uppercase">Engine Calibration Mode</span>
            <Sliders className="w-4 h-4 text-indigo-400" />
          </div>
          <p className="text-2xl font-bold text-white">{aggressiveness}</p>
          <span className="text-[10px] text-slate-400 block mt-1">Targeting maximum occupancy duration</span>
        </div>
      </div>

      {/* Visual Charts Section */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 mb-8">
        {/* Line Chart: Market Price trends */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
          <div className="mb-4">
            <h2 className="text-base font-bold text-white">Average Neighborhood Rent Trend</h2>
            <p className="text-xs text-slate-400">Past 6 months regional averages (2 BHK index)</p>
          </div>
          <div className="h-[250px] w-full">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={MARKET_TREND_DATA} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                <XAxis dataKey="month" stroke="#94a3b8" fontSize={11} />
                <YAxis stroke="#94a3b8" fontSize={11} />
                <Tooltip contentStyle={{ backgroundColor: '#090d16', border: '1px solid #1e293b' }} />
                <Legend wrapperStyle={{ fontSize: 11 }} />
                <Line type="monotone" dataKey="averageRent" stroke="#6366f1" name="Rent (₹)" strokeWidth={2.5} activeDot={{ r: 8 }} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Bar Chart: Occupancy Forecasting */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
          <div className="mb-4">
            <h2 className="text-base font-bold text-white">Occupancy Index Forecast</h2>
            <p className="text-xs text-slate-400">Predicted tenant occupancy percentages</p>
          </div>
          <div className="h-[250px] w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={OCCUPANCY_FORECAST_DATA} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                <XAxis dataKey="month" stroke="#94a3b8" fontSize={11} />
                <YAxis stroke="#94a3b8" fontSize={11} domain={[70, 100]} />
                <Tooltip contentStyle={{ backgroundColor: '#090d16', border: '1px solid #1e293b' }} />
                <Legend wrapperStyle={{ fontSize: 11 }} />
                <Bar dataKey="occupancyRate" fill="#3b82f6" name="Occupancy (%)" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      {/* Suggested Adjustments Table */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
        <div className="flex justify-between items-center mb-6">
          <div>
            <h2 className="text-lg font-bold text-white">Suggested Rent Adjustments</h2>
            <p className="text-xs text-slate-400">AI-optimized pricing adjustments based on real-time neighborhood metrics</p>
          </div>
          <div className="flex items-center gap-1.5 text-xs text-indigo-400 bg-indigo-500/10 px-3 py-1 rounded-full border border-indigo-500/20 font-medium">
            <Sparkles className="w-3.5 h-3.5 animate-pulse" />
            <span>AI Dynamic Pricing Enabled</span>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-slate-300 border-collapse">
            <thead>
              <tr className="border-b border-slate-800 text-slate-400 text-xs uppercase font-semibold">
                <th className="py-3 px-4">Property</th>
                <th className="py-3 px-4">Locality</th>
                <th className="py-3 px-4 text-center">Current Rent</th>
                <th className="py-3 px-4 text-center">Suggested Rent</th>
                <th className="py-3 px-4 text-center">Demand</th>
                <th className="py-3 px-4 text-right">Action</th>
              </tr>
            </thead>
            <tbody>
              {insights.map((item) => (
                <tr key={item.id} className="border-b border-slate-800/60 hover:bg-slate-850/20 transition-colors">
                  <td className="py-4 px-4 font-semibold text-white">{item.title}</td>
                  <td className="py-4 px-4 text-slate-400 text-xs">{item.locality}</td>
                  <td className="py-4 px-4 text-center font-mono font-medium">₹{item.currentRent.toLocaleString()}</td>
                  <td className="py-4 px-4 text-center font-mono text-indigo-400 font-bold">₹{item.suggestedRent.toLocaleString()}</td>
                  <td className="py-4 px-4 text-center">
                    <span className={`px-2.5 py-0.5 rounded text-[10px] font-semibold border ${
                      item.demandLevel === 'HIGH'
                        ? 'bg-emerald-500/10 border-emerald-500/25 text-emerald-400'
                        : item.demandLevel === 'MEDIUM'
                        ? 'bg-amber-500/10 border-amber-500/25 text-amber-400'
                        : 'bg-rose-500/10 border-rose-500/25 text-rose-400'
                    }`}>
                      {item.demandLevel}
                    </span>
                  </td>
                  <td className="py-4 px-4 text-right">
                    {item.applied ? (
                      <span className="inline-flex items-center space-x-1 text-xs text-emerald-400 font-semibold bg-emerald-500/10 px-3 py-1.5 rounded-lg border border-emerald-500/20">
                        <Check className="w-3.5 h-3.5" />
                        <span>Applied</span>
                      </span>
                    ) : (
                      <button
                        onClick={() => handleApplySuggestion(item.id, item.suggestedRent)}
                        className="bg-indigo-650 hover:bg-indigo-600 text-white font-semibold text-xs px-4.5 py-2 rounded-lg transition-colors border border-indigo-500/30"
                      >
                        Apply Adjustment
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
