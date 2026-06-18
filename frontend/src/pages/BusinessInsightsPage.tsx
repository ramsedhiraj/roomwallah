import React, { useState } from 'react';
import { 
  BarChart, Bar, LineChart, Line, XAxis, YAxis, CartesianGrid, 
  Tooltip, Legend, ResponsiveContainer, RadarChart, PolarGrid, 
  PolarAngleAxis, PolarRadiusAxis, Radar 
} from 'recharts';
import { 
  Info, BarChart2, Download 
} from 'lucide-react';

// Mock Data
const pricingTrends = [
  { area: 'Indiranagar, BLR', avgRent: 45000, demandScore: 92 },
  { area: 'Koramangala, BLR', avgRent: 38000, demandScore: 88 },
  { area: 'Bandra West, MUM', avgRent: 85000, demandScore: 95 },
  { area: 'Powai, MUM', avgRent: 58000, demandScore: 82 },
  { area: 'GK 2, DEL', avgRent: 62000, demandScore: 80 },
  { area: 'DLF Phase 3, GGN', avgRent: 48000, demandScore: 85 },
];

const matchIndices = [
  { subject: '1 BHK Rooms', Supply: 120, Demand: 340 },
  { subject: '2 BHK Flats', Supply: 290, Demand: 410 },
  { subject: '3 BHK Apartments', Supply: 180, Demand: 210 },
  { subject: 'Villas/Houses', Supply: 45, Demand: 90 },
  { subject: 'PG/Co-Living', Supply: 480, Demand: 650 },
];

const churnAnalytics = [
  { week: 'Wk 1', churnRate: 2.1, retentionRate: 97.9 },
  { week: 'Wk 2', churnRate: 1.8, retentionRate: 98.2 },
  { week: 'Wk 3', churnRate: 1.9, retentionRate: 98.1 },
  { week: 'Wk 4', churnRate: 1.5, retentionRate: 98.5 },
  { week: 'Wk 5', churnRate: 1.4, retentionRate: 98.6 },
  { week: 'Wk 6', churnRate: 1.2, retentionRate: 98.8 },
];

export default function BusinessInsightsPage() {
  const [activeTab, setActiveTab] = useState<'demand' | 'pricing' | 'churn'>('demand');
  const [cityFilter, setCityFilter] = useState<'ALL' | 'BLR' | 'MUM' | 'DEL'>('ALL');

  const handleExportCSV = () => {
    let headers = '';
    let rows = '';
    
    if (activeTab === 'demand') {
      headers = 'Category,Supply,Demand\n';
      rows = matchIndices.map(m => `"${m.subject}",${m.Supply},${m.Demand}`).join('\n');
    } else if (activeTab === 'pricing') {
      headers = 'Locality,Avg Rent (INR),Demand Score\n';
      rows = pricingTrends.map(p => `"${p.area}",${p.avgRent},${p.demandScore}`).join('\n');
    } else {
      headers = 'Week,Churn Rate (%),Retention Rate (%)\n';
      rows = churnAnalytics.map(c => `${c.week},${c.churnRate},${c.retentionRate}`).join('\n');
    }

    const blob = new Blob([headers + rows], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.setAttribute("href", url);
    link.setAttribute("download", `market_insights_${activeTab}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div className="max-w-7xl mx-auto px-4 py-8 space-y-6 animate-fade-in text-slate-100">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight flex items-center gap-3">
            <BarChart2 className="w-8 h-8 text-primary" />
            Market & Business Insights
          </h1>
          <p className="text-muted-foreground text-sm">
            AI-driven market demand analysis, neighborhood pricing index, and user retention curves.
          </p>
        </div>

        {/* Tab Selection & Export */}
        <div className="flex items-center gap-2 self-stretch sm:self-auto flex-wrap">
          <button
            onClick={handleExportCSV}
            className="flex items-center gap-1.5 px-3 py-2 border border-slate-800 rounded-xl bg-slate-900/50 hover:bg-slate-900 transition-all text-xs font-semibold text-slate-400 hover:text-white"
          >
            <Download className="w-3.5 h-3.5" />
            <span>Export CSV</span>
          </button>

          <div className="flex bg-slate-900 border border-slate-800 rounded-xl p-1 text-xs font-semibold">
            {(['demand', 'pricing', 'churn'] as const).map((tab) => (
              <button
                key={tab}
                onClick={() => setActiveTab(tab)}
                className={`px-4 py-2 rounded-lg transition-all capitalize ${
                  activeTab === tab ? 'bg-primary text-white' : 'text-slate-400 hover:text-white'
                }`}
              >
                {tab} Insights
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Advisory Banner */}
      <div className="bg-indigo-950/20 border border-indigo-900/30 rounded-2xl p-4 flex items-start gap-3 text-slate-300">
        <Info className="w-5 h-5 text-primary shrink-0 mt-0.5" />
        <p className="text-xs leading-relaxed">
          <span className="font-bold text-white">Market advisory:</span> Demand for 1 BHK and Co-living units in major technology corridors (Indiranagar, Bangalore & Bandra West, Mumbai) has increased by 14% over the last quarter. Consider recommending listing optimization to owners in these areas.
        </p>
      </div>

      {/* Dynamic Tab Renderings */}
      {activeTab === 'demand' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Supply vs Demand Radar */}
          <div className="lg:col-span-2 glass rounded-2xl p-6 border border-white/5 space-y-4">
            <div>
              <h2 className="text-lg font-bold">Supply vs Demand Mapping</h2>
              <p className="text-xs text-muted-foreground">Comparative representation of active listings vs tenant request volumes</p>
            </div>
            <div className="h-80 w-full flex justify-center">
              <ResponsiveContainer width="100%" height="100%">
                <RadarChart cx="50%" cy="50%" outerRadius="80%" data={matchIndices}>
                  <PolarGrid stroke="#334155" />
                  <PolarAngleAxis dataKey="subject" stroke="#64748b" fontSize={11} />
                  <PolarRadiusAxis stroke="#64748b" fontSize={10} angle={30} />
                  <Radar name="Active Listings (Supply)" dataKey="Supply" stroke="#10b981" fill="#10b981" fillOpacity={0.4} />
                  <Radar name="Tenant Requests (Demand)" dataKey="Demand" stroke="#6366f1" fill="#6366f1" fillOpacity={0.4} />
                  <Legend />
                  <Tooltip contentStyle={{ backgroundColor: '#0f172a', borderColor: '#334155', borderRadius: '12px', color: '#f8fafc' }} />
                </RadarChart>
              </ResponsiveContainer>
            </div>
          </div>

          {/* Key Insights List */}
          <div className="glass rounded-2xl p-6 border border-white/5 space-y-4 flex flex-col justify-between">
            <h2 className="text-lg font-bold">Demand Indicators</h2>
            <div className="space-y-3.5 flex-1 mt-2">
              <div className="p-3 bg-slate-950/60 border border-slate-900 rounded-xl space-y-1">
                <div className="text-xs font-bold text-primary uppercase">Critical Shortage</div>
                <div className="text-sm font-bold text-white">1 BHK Rooms (Indiranagar)</div>
                <p className="text-[11px] text-slate-400">Demand outpaces active supply listings by 2.8x. Average match response is under 4 hours.</p>
              </div>
              <div className="p-3 bg-slate-950/60 border border-slate-900 rounded-xl space-y-1">
                <div className="text-xs font-bold text-emerald-400 uppercase font-semibold">Equilibrium</div>
                <div className="text-sm font-bold text-white">3 BHK Apartments</div>
                <p className="text-[11px] text-slate-400">Escrow payouts match expected timelines. Ideal lease conversion cycles at 18 days.</p>
              </div>
              <div className="p-3 bg-slate-950/60 border border-slate-900 rounded-xl space-y-1">
                <div className="text-xs font-bold text-amber-400 uppercase font-semibold">Over-saturated</div>
                <div className="text-sm font-bold text-white">Luxury Villas / Penthouses</div>
                <p className="text-[11px] text-slate-400">Supply listings are active for an average of 45 days. Owners are open to negotiation packages.</p>
              </div>
            </div>
          </div>
        </div>
      )}

      {activeTab === 'pricing' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 animate-fade-in">
          {/* Average Rental Rates */}
          <div className="lg:col-span-2 glass rounded-2xl p-6 border border-white/5 space-y-4">
            <div className="flex justify-between items-center flex-wrap gap-2">
              <div>
                <h2 className="text-lg font-bold">Locality Rental Index</h2>
                <p className="text-xs text-muted-foreground">Average monthly rental cost vs active demand score (1-100)</p>
              </div>
              <div className="flex bg-slate-950 border border-slate-800 rounded-xl p-1 text-[11px] font-semibold">
                {(['ALL', 'BLR', 'MUM', 'DEL'] as const).map(f => (
                  <button
                    key={f}
                    onClick={() => setCityFilter(f)}
                    className={`px-2.5 py-1 rounded-lg transition-all ${
                      cityFilter === f ? 'bg-primary text-white' : 'text-slate-400 hover:text-white'
                    }`}
                  >
                    {f}
                  </button>
                ))}
              </div>
            </div>
            <div className="h-80">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart
                  data={pricingTrends.filter(t => cityFilter === 'ALL' || t.area.includes(cityFilter))}
                  margin={{ top: 10, right: 10, left: -15, bottom: 0 }}
                >
                  <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" opacity={0.3} />
                  <XAxis dataKey="area" stroke="#64748b" fontSize={11} />
                  <YAxis yAxisId="left" stroke="#64748b" fontSize={11} />
                  <YAxis yAxisId="right" orientation="right" stroke="#64748b" fontSize={11} />
                  <Tooltip contentStyle={{ backgroundColor: '#0f172a', borderColor: '#334155', borderRadius: '12px', color: '#f8fafc' }} />
                  <Legend />
                  <Bar yAxisId="left" name="Avg Rent (INR)" dataKey="avgRent" fill="#6366f1" radius={[4, 4, 0, 0]} />
                  <Bar yAxisId="right" name="Demand score" dataKey="demandScore" fill="#ec4899" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>

          {/* Pricing Indexes Details */}
          <div className="glass rounded-2xl p-6 border border-white/5 space-y-4">
            <h2 className="text-lg font-bold">Local Trends Analysis</h2>
            <div className="space-y-4 text-xs">
              <div className="flex justify-between items-center p-3 bg-slate-950/60 rounded-xl border border-slate-900">
                <div>
                  <div className="font-bold text-white">Bandra West, MUM</div>
                  <div className="text-[10px] text-slate-500">Highest rental yields across India</div>
                </div>
                <div className="text-right">
                  <div className="text-sm font-extrabold text-primary">₹85,000</div>
                  <div className="text-[10px] text-emerald-400">+5.2% MoM</div>
                </div>
              </div>

              <div className="flex justify-between items-center p-3 bg-slate-950/60 rounded-xl border border-slate-900">
                <div>
                  <div className="font-bold text-white">Indiranagar, BLR</div>
                  <div className="text-[10px] text-slate-500">Fastest listings depletion rate</div>
                </div>
                <div className="text-right">
                  <div className="text-sm font-extrabold text-primary">₹45,000</div>
                  <div className="text-[10px] text-emerald-400">+3.8% MoM</div>
                </div>
              </div>

              <div className="flex justify-between items-center p-3 bg-slate-950/60 rounded-xl border border-slate-900">
                <div>
                  <div className="font-bold text-white">GK 2, Delhi NCR</div>
                  <div className="text-[10px] text-slate-500">High preference for family units</div>
                </div>
                <div className="text-right">
                  <div className="text-sm font-extrabold text-primary">₹62,000</div>
                  <div className="text-[10px] text-slate-400">0.0% MoM</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {activeTab === 'churn' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 animate-fade-in">
          {/* User Retention & Churn Curve */}
          <div className="lg:col-span-2 glass rounded-2xl p-6 border border-white/5 space-y-4">
            <div>
              <h2 className="text-lg font-bold">Weekly Churn & Retention Analytics</h2>
              <p className="text-xs text-muted-foreground">User account activity retention statistics over 6 weeks cohorts</p>
            </div>
            <div className="h-80">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={churnAnalytics} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" opacity={0.3} />
                  <XAxis dataKey="week" stroke="#64748b" fontSize={12} />
                  <YAxis stroke="#64748b" fontSize={12} />
                  <Tooltip contentStyle={{ backgroundColor: '#0f172a', borderColor: '#334155', borderRadius: '12px', color: '#f8fafc' }} />
                  <Legend />
                  <Line name="Retention Rate (%)" type="monotone" dataKey="retentionRate" stroke="#10b981" strokeWidth={2.5} activeDot={{ r: 8 }} />
                  <Line name="Churn Rate (%)" type="monotone" dataKey="churnRate" stroke="#ef4444" strokeWidth={2} />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </div>

          {/* Retention recommendations */}
          <div className="glass rounded-2xl p-6 border border-white/5 space-y-4">
            <h2 className="text-lg font-bold">Retention Optimization</h2>
            <p className="text-xs text-muted-foreground leading-relaxed">
              Based on the 6-weeks cohort analysis, tenants who submit verified KYC profile documents within 48 hours show a 94% retention rate compared to unverified tenants.
            </p>
            <div className="space-y-3 pt-2 text-xs">
              <div className="p-3 bg-indigo-950/10 border border-indigo-900/20 rounded-xl">
                <span className="font-bold text-indigo-300">Action A:</span> Prominently feature the trust verification wizard in the landing sidebar for new users.
              </div>
              <div className="p-3 bg-purple-950/10 border border-purple-900/20 rounded-xl">
                <span className="font-bold text-purple-300">Action B:</span> Implement instant notification channels (Webhooks/SSE) for chat responses.
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
