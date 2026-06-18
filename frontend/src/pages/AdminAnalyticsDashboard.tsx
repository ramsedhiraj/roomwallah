import React, { useState } from 'react';
import { 
  AreaChart, Area, BarChart, Bar, PieChart, Pie, Cell,
  XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer 
} from 'recharts';
import { 
  TrendingUp, Users, Home, ShieldAlert, DollarSign, 
  MapPin, ArrowUpRight, ArrowDownRight, RefreshCw, Download 
} from 'lucide-react';
import { motion } from 'framer-motion';

// Mock Data
const revenueData = [
  { month: 'Jan', revenue: 450000, bookings: 320 },
  { month: 'Feb', revenue: 520000, bookings: 380 },
  { month: 'Mar', revenue: 610000, bookings: 460 },
  { month: 'Apr', revenue: 580000, bookings: 420 },
  { month: 'May', revenue: 710000, bookings: 540 },
  { month: 'Jun', revenue: 850000, bookings: 680 },
];

const geoDistribution = [
  { name: 'Mumbai', value: 45, color: '#6366f1' },
  { name: 'Bengaluru', value: 30, color: '#a855f7' },
  { name: 'Delhi NCR', value: 15, color: '#ec4899' },
  { name: 'Pune', value: 10, color: '#10b981' },
];

const userAcquisition = [
  { day: 'Mon', Owners: 12, Tenants: 45 },
  { day: 'Tue', Owners: 19, Tenants: 58 },
  { day: 'Wed', Owners: 15, Tenants: 62 },
  { day: 'Thu', Owners: 22, Tenants: 75 },
  { day: 'Fri', Owners: 30, Tenants: 90 },
  { day: 'Sat', Owners: 25, Tenants: 110 },
  { day: 'Sun', Owners: 18, Tenants: 85 },
];

const conversionMetrics = [
  { stage: 'Search Views', count: 12000, rate: 100 },
  { stage: 'Detail Clicks', count: 4800, rate: 40 },
  { stage: 'Visits Booked', count: 1200, rate: 10 },
  { stage: 'Escrow Initiated', count: 360, rate: 3 },
  { stage: 'Confirmed', count: 240, rate: 2 },
];

export default function AdminAnalyticsDashboard() {
  const [timeframe, setTimeframe] = useState<'30D' | '90D' | '1Y'>('30D');
  const [loading, setLoading] = useState(false);

  const handleRefresh = () => {
    setLoading(true);
    setTimeout(() => setLoading(false), 800);
  };

  const handleExportCSV = () => {
    const headers = 'Month,Revenue (INR),Bookings\n';
    const rows = revenueData.map(r => `${r.month},${r.revenue},${r.bookings}`).join('\n');
    const blob = new Blob([headers + rows], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.setAttribute("href", url);
    link.setAttribute("download", `platform_metrics_${timeframe}.csv`);
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
            <TrendingUp className="w-8 h-8 text-primary" />
            Admin Analytics Dashboard
          </h1>
          <p className="text-muted-foreground text-sm">
            Real-time platform growth, key performance indicators, and financial statistics.
          </p>
        </div>
        
        <div className="flex items-center gap-2">
          <button
            onClick={handleExportCSV}
            className="flex items-center gap-1.5 px-3 py-1.5 border border-slate-800 rounded-xl bg-slate-900/50 hover:bg-slate-900 transition-all text-xs font-semibold text-slate-400 hover:text-white"
          >
            <Download className="w-3.5 h-3.5" />
            <span>Export CSV</span>
          </button>

          <div className="flex bg-slate-900 border border-slate-800 rounded-xl p-1 text-xs font-semibold">
            {(['30D', '90D', '1Y'] as const).map((t) => (
              <button
                key={t}
                onClick={() => setTimeframe(t)}
                className={`px-3 py-1.5 rounded-lg transition-all ${
                  timeframe === t ? 'bg-primary text-white' : 'text-slate-400 hover:text-white'
                }`}
              >
                {t}
              </button>
            ))}
          </div>
          <button
            onClick={handleRefresh}
            className="p-2 border border-slate-800 rounded-xl bg-slate-900/50 hover:bg-slate-900 transition-all text-slate-400 hover:text-white"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin text-primary' : ''}`} />
          </button>
        </div>
      </div>

      {/* KPI Section */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        {[
          { title: 'Total Revenue', value: '₹14,85,000', change: '+18.4%', up: true, desc: 'vs last month', icon: <DollarSign className="w-5 h-5 text-indigo-400" /> },
          { title: 'Active Listings', value: '3,842', change: '+12.1%', up: true, desc: 'with 85% verified', icon: <Home className="w-5 h-5 text-purple-400" /> },
          { title: 'User registrations', value: '18,245', change: '+8.2%', up: true, desc: 'both Owners & Tenants', icon: <Users className="w-5 h-5 text-pink-400" /> },
          { title: 'Fraud Alerts Raised', value: '8', change: '-24%', up: false, desc: 'resolved by AI Engine', icon: <ShieldAlert className="w-5 h-5 text-rose-400" /> },
        ].map((kpi, idx) => (
          <motion.div
            key={kpi.title}
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.3, delay: idx * 0.05 }}
            className="glass rounded-2xl p-6 border border-white/5 shadow-md flex flex-col justify-between"
          >
            <div className="flex justify-between items-center">
              <span className="text-xs font-bold uppercase tracking-widest text-slate-400">{kpi.title}</span>
              <div className="p-2 rounded-xl bg-slate-950/80 border border-slate-800/80">{kpi.icon}</div>
            </div>
            <div className="mt-4">
              <h3 className="text-3xl font-extrabold text-white tracking-tight">{kpi.value}</h3>
              <div className="flex items-center gap-1.5 mt-2 text-xs">
                <span className={`font-bold flex items-center ${kpi.up ? 'text-emerald-400' : 'text-rose-400'}`}>
                  {kpi.up ? <ArrowUpRight className="w-3.5 h-3.5" /> : <ArrowDownRight className="w-3.5 h-3.5" />}
                  {kpi.change}
                </span>
                <span className="text-slate-500">{kpi.desc}</span>
              </div>
            </div>
          </motion.div>
        ))}
      </div>

      {/* Main Charts Row */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Revenue Area Chart */}
        <div className="lg:col-span-2 glass rounded-2xl p-6 border border-white/5 space-y-4">
          <div className="flex justify-between items-center">
            <div>
              <h2 className="text-lg font-bold">Revenue & Booking Trends</h2>
              <p className="text-xs text-muted-foreground">Monthly summary of booking platform commission and escrow activity</p>
            </div>
          </div>
          <div className="h-80 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={revenueData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                <defs>
                  <linearGradient id="colorRevenue" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#6366f1" stopOpacity={0.4}/>
                    <stop offset="95%" stopColor="#6366f1" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" opacity={0.3} />
                <XAxis dataKey="month" stroke="#64748b" fontSize={12} />
                <YAxis stroke="#64748b" fontSize={12} />
                <Tooltip 
                  contentStyle={{ backgroundColor: '#0f172a', borderColor: '#334155', borderRadius: '12px', color: '#f8fafc' }}
                  labelStyle={{ fontWeight: 'bold', color: '#a5b4fc' }}
                />
                <Legend />
                <Area name="Platform Revenue (INR)" type="monotone" dataKey="revenue" stroke="#6366f1" strokeWidth={2} fillOpacity={1} fill="url(#colorRevenue)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Geo Distribution Pie Chart */}
        <div className="glass rounded-2xl p-6 border border-white/5 flex flex-col justify-between space-y-4">
          <div>
            <h2 className="text-lg font-bold">Geographic Breakdown</h2>
            <p className="text-xs text-muted-foreground">Active supply concentration per major city market</p>
          </div>
          <div className="h-60 relative flex justify-center items-center">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={geoDistribution}
                  cx="50%"
                  cy="50%"
                  innerRadius={60}
                  outerRadius={80}
                  paddingAngle={5}
                  dataKey="value"
                >
                  {geoDistribution.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip 
                  contentStyle={{ backgroundColor: '#0f172a', borderColor: '#334155', borderRadius: '12px', color: '#f8fafc' }}
                />
              </PieChart>
            </ResponsiveContainer>
            <div className="absolute flex flex-col items-center justify-center">
              <MapPin className="w-6 h-6 text-primary mb-0.5 animate-bounce" />
              <span className="text-xl font-bold text-white">Cities</span>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-2 text-xs">
            {geoDistribution.map((item) => (
              <div key={item.name} className="flex items-center gap-2 bg-slate-950/40 border border-slate-900 rounded-xl p-2.5">
                <span className="w-3 h-3 rounded-full shrink-0" style={{ backgroundColor: item.color }} />
                <span className="text-slate-300 font-semibold">{item.name}:</span>
                <span className="text-white font-extrabold ml-auto">{item.value}%</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Secondary Charts Row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* User Acquisition Bar Chart */}
        <div className="glass rounded-2xl p-6 border border-white/5 space-y-4">
          <div>
            <h2 className="text-lg font-bold">User Registrations</h2>
            <p className="text-xs text-muted-foreground">Owner vs Tenant account conversions over the past week</p>
          </div>
          <div className="h-80">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={userAcquisition} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" opacity={0.3} />
                <XAxis dataKey="day" stroke="#64748b" fontSize={12} />
                <YAxis stroke="#64748b" fontSize={12} />
                <Tooltip
                  contentStyle={{ backgroundColor: '#0f172a', borderColor: '#334155', borderRadius: '12px', color: '#f8fafc' }}
                />
                <Legend />
                <Bar name="Tenants" dataKey="Tenants" fill="#a855f7" radius={[4, 4, 0, 0]} />
                <Bar name="Owners" dataKey="Owners" fill="#10b981" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Funnel Conversion Metrics */}
        <div className="glass rounded-2xl p-6 border border-white/5 space-y-4 flex flex-col justify-between">
          <div>
            <h2 className="text-lg font-bold">Booking Conversion Funnel</h2>
            <p className="text-xs text-muted-foreground">Visitor to lease agreement transition rates</p>
          </div>
          <div className="space-y-4 py-2">
            {conversionMetrics.map((item, idx) => (
              <div key={item.stage} className="space-y-1.5">
                <div className="flex justify-between items-center text-xs">
                  <span className="font-semibold text-slate-350">{item.stage}</span>
                  <div className="space-x-1.5">
                    <span className="font-extrabold text-slate-250">{item.count.toLocaleString()}</span>
                    <span className="text-[10px] bg-slate-900 border border-slate-800 text-slate-400 px-1.5 py-0.5 rounded">
                      {item.rate}%
                    </span>
                  </div>
                </div>
                <div className="w-full bg-slate-950 border border-slate-900 rounded-full h-3 overflow-hidden">
                  <motion.div
                    initial={{ width: 0 }}
                    animate={{ width: `${item.rate}%` }}
                    transition={{ duration: 0.8, delay: idx * 0.1 }}
                    className={`h-full rounded-full bg-gradient-to-r ${
                      idx < 2 ? 'from-indigo-500 to-purple-500' : 'from-purple-500 to-pink-500'
                    }`}
                  />
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
