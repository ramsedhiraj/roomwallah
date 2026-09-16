import React, { useState } from 'react';
import { 
  ShieldCheck, ArrowRight, Search, MapPin, Building, 
  IndianRupee, Sparkles, CheckCircle2, ChevronDown, 
  ShieldAlert, Lock, Home, KeyRound, Award, Users, FileCheck2
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import TrendingHomes from '../components/TrendingHomes';
import RecommendedForYou from '../components/RecommendedForYou';

export default function LandingPage() {
  const navigate = useNavigate();

  // Search Console State
  const [purpose, setPurpose] = useState<'RENT' | 'SALE'>('RENT');
  const [city, setCity] = useState('');
  const [propertyType, setPropertyType] = useState('');
  const [budgetRange, setBudgetRange] = useState('');
  const [openFaq, setOpenFaq] = useState<number | null>(0);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const params = new URLSearchParams();
    if (purpose) params.set('listingPurpose', purpose);
    if (city.trim()) params.set('city', city.trim());
    if (propertyType) params.set('propertyType', propertyType);
    if (budgetRange) {
      const [min, max] = budgetRange.split('-');
      if (min) params.set('minPrice', min);
      if (max) params.set('maxPrice', max);
    }
    navigate(`/search?${params.toString()}`);
  };

  const handleQuickCity = (cityName: string) => {
    navigate(`/search?city=${encodeURIComponent(cityName)}&listingPurpose=${purpose}`);
  };

  const features = [
    {
      icon: <FileCheck2 className="w-6 h-6 text-indigo-400" />,
      title: "Property & Document Verification",
      description: "Property listings undergo identity and document verification before going live. Zero duplicate agent ads.",
      badge: "Verified Listings"
    },
    {
      icon: <Award className="w-6 h-6 text-amber-400" />,
      title: "Owner & Tenant Trust Scores",
      description: "Multi-factor trust scoring calculated from KYC validation, payment history, and authentic tenancy track record.",
      badge: "Trust Engine"
    },
    {
      icon: <Lock className="w-6 h-6 text-emerald-400" />,
      title: "Deposit Protection Protocol",
      description: "Security deposits are held under milestone-based terms, protecting both tenants and owners during tenancy.",
      badge: "Deposit Shield"
    },
    {
      icon: <KeyRound className="w-6 h-6 text-cyan-400" />,
      title: "Direct Owner Contact",
      description: "Connect directly with property owners via verified chat and schedule physical visits. Never pay a single rupee in brokerage.",
      badge: "Zero Middlemen"
    }
  ];

  const faqs = [
    {
      q: "How does RoomWallah guarantee zero brokerage?",
      a: "Unlike traditional classified sites that sell your phone number to real estate brokers, RoomWallah restricts listing permissions to verified title owners. We algorithmically detect and ban broker phone numbers, photos, and duplicate registrations."
    },
    {
      q: "How are properties verified before being shown?",
      a: "When an owner submits a property, our verification engine reviews property title documents, electricity bill records, and cross-references geospatial boundary checks. Once approved, the listing receives the 'Verified Owner' trust badge."
    },
    {
      q: "What is the RoomWallah Trust Score?",
      a: "Trust Score is an objective 0-100 rating reflecting user legitimacy. It factors in verified identity (Aadhaar/PAN KYC), ownership proof, on-time rent payment records, and verified reviews from past tenancies."
    },
    {
      q: "How do visit bookings work?",
      a: "Once you find a home you love, select 'Book Visit' on the property page. Choose an available date and time slot. The owner receives an instant SMS/email confirmation and visit details are added to both your calendars."
    },
    {
      q: "Is RoomWallah completely free for tenants?",
      a: "Yes. Searching, filtering, viewing video tours, booking in-person visits, and contacting verified owners is 100% free with zero brokerage fees."
    }
  ];

  const popularCities = [
    { name: "Pune", localities: "Baner, Hinjewadi, Kharadi, Viman Nagar" },
    { name: "Mumbai", localities: "Andheri, Bandra, Powai, Thane" },
    { name: "Bengaluru", localities: "Indiranagar, Koramangala, Whitefield, HSR" },
    { name: "Delhi NCR", localities: "Gurgaon Cyber City, Noida Sec 62, Saket" },
    { name: "Hyderabad", localities: "Gachibowli, Hitec City, Kondapur, Madhapur" }
  ];

  return (
    <div className="relative overflow-hidden w-full">
      {/* Background ambient lighting */}
      <div className="absolute top-[-15%] left-[20%] w-[60%] h-[500px] rounded-full bg-indigo-600/10 blur-[140px] pointer-events-none"></div>
      <div className="absolute top-[30%] right-[-10%] w-[40%] h-[400px] rounded-full bg-violet-600/10 blur-[140px] pointer-events-none"></div>

      {/* HERO SECTION */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-8 pb-16 md:pt-14 md:pb-24 relative z-10">
        <div className="text-center max-w-3xl mx-auto space-y-4 mb-10">
          <div className="inline-flex items-center space-x-2 px-3.5 py-1.5 rounded-full bg-indigo-500/10 border border-indigo-500/20 text-xs font-semibold text-indigo-300">
            <Sparkles className="w-3.5 h-3.5 text-indigo-400" />
            <span>Direct Owner Marketplace • 0% Brokerage Guarantee</span>
          </div>

          <h1 className="text-4xl sm:text-6xl lg:text-7xl font-black tracking-tight text-white leading-[1.1]">
            Find Verified Homes.{' '}
            <span className="bg-gradient-to-r from-indigo-400 via-violet-300 to-indigo-200 bg-clip-text text-transparent">
              Direct From Owners.
            </span>
          </h1>

          <p className="text-base sm:text-lg text-slate-400 max-w-2xl mx-auto leading-relaxed">
            Eliminate fake agent listings, spam calls, and brokerage fees. Connect with title-verified owners, inspect authentic virtual previews, and book visits safely.
          </p>
        </div>

        {/* PRIMARY REAL ESTATE SEARCH CONSOLE */}
        <div className="max-w-4xl mx-auto glass-card rounded-3xl p-4 sm:p-6 border border-slate-800/90 shadow-2xl shadow-black/60 relative z-20">
          {/* Mode Switcher */}
          <div className="flex items-center gap-2 mb-4 pb-4 border-b border-slate-800/80">
            <button
              type="button"
              onClick={() => setPurpose('RENT')}
              className={`px-5 py-2 rounded-xl text-xs sm:text-sm font-bold transition-all ${
                purpose === 'RENT'
                  ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/30'
                  : 'text-slate-400 hover:text-white hover:bg-slate-800/50'
              }`}
            >
              Rent a Home
            </button>
            <button
              type="button"
              onClick={() => setPurpose('SALE')}
              className={`px-5 py-2 rounded-xl text-xs sm:text-sm font-bold transition-all ${
                purpose === 'SALE'
                  ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/30'
                  : 'text-slate-400 hover:text-white hover:bg-slate-800/50'
              }`}
            >
              Buy Property
            </button>
            <span className="ml-auto text-[11px] text-emerald-400 font-medium hidden sm:flex items-center gap-1">
              <CheckCircle2 className="w-3.5 h-3.5" />
              100% Zero Brokerage
            </span>
          </div>

          {/* Search Inputs Grid */}
          <form onSubmit={handleSearchSubmit} className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
            {/* City / Locality */}
            <div className="relative">
              <label className="text-[10px] font-bold uppercase tracking-wider text-slate-400 block mb-1">
                City / Locality
              </label>
              <div className="relative">
                <input
                  type="text"
                  value={city}
                  onChange={(e) => setCity(e.target.value)}
                  placeholder="e.g. Pune, Baner"
                  className="w-full pl-8 pr-3 py-2.5 rounded-xl bg-slate-900/90 border border-slate-800 focus:border-indigo-500 text-xs font-medium text-white placeholder-slate-400 focus:outline-none focus:ring-1 focus:ring-indigo-500 transition-all"
                />
                <MapPin className="w-4 h-4 text-slate-400 absolute left-2.5 top-2.5" />
              </div>
            </div>

            {/* Property Type */}
            <div>
              <label className="text-[10px] font-bold uppercase tracking-wider text-slate-400 block mb-1">
                Property Type
              </label>
              <div className="relative">
                <select
                  value={propertyType}
                  onChange={(e) => setPropertyType(e.target.value)}
                  className="w-full pl-8 pr-3 py-2.5 rounded-xl bg-slate-900/90 border border-slate-800 focus:border-indigo-500 text-xs font-medium text-white focus:outline-none focus:ring-1 focus:ring-indigo-500 transition-all appearance-none cursor-pointer"
                >
                  <option value="">All Types</option>
                  <option value="APARTMENT">Apartment / Flat</option>
                  <option value="INDEPENDENT_HOUSE">Independent House</option>
                  <option value="VILLA">Luxury Villa</option>
                  <option value="STUDIO">Studio Apartment</option>
                  <option value="PENTHOUSE">Penthouse</option>
                </select>
                <Building className="w-4 h-4 text-slate-400 absolute left-2.5 top-2.5 pointer-events-none" />
              </div>
            </div>

            {/* Budget Range */}
            <div>
              <label className="text-[10px] font-bold uppercase tracking-wider text-slate-400 block mb-1">
                Budget
              </label>
              <div className="relative">
                <select
                  value={budgetRange}
                  onChange={(e) => setBudgetRange(e.target.value)}
                  className="w-full pl-8 pr-3 py-2.5 rounded-xl bg-slate-900/90 border border-slate-800 focus:border-indigo-500 text-xs font-medium text-white focus:outline-none focus:ring-1 focus:ring-indigo-500 transition-all appearance-none cursor-pointer"
                >
                  <option value="">Any Budget</option>
                  {purpose === 'RENT' ? (
                    <>
                      <option value="0-20000">Under ₹20,000 / mo</option>
                      <option value="20000-35000">₹20,000 - ₹35,000 / mo</option>
                      <option value="35000-50000">₹35,000 - ₹50,000 / mo</option>
                      <option value="50000-100000">₹50,000 - ₹1,00,000 / mo</option>
                      <option value="100000-10000000">₹1,00,000+ / mo</option>
                    </>
                  ) : (
                    <>
                      <option value="0-5000000">Under ₹50 Lakh</option>
                      <option value="5000000-10000000">₹50 Lakh - ₹1 Crore</option>
                      <option value="10000000-20000000">₹1 Crore - ₹2 Crore</option>
                      <option value="20000000-50000000">₹2 Crore - ₹5 Crore</option>
                      <option value="50000000-500000000">₹5 Crore+</option>
                    </>
                  )}
                </select>
                <IndianRupee className="w-4 h-4 text-slate-400 absolute left-2.5 top-2.5 pointer-events-none" />
              </div>
            </div>

            {/* Submit Button */}
            <div className="flex flex-col justify-end">
              <button
                type="submit"
                className="w-full py-2.5 px-4 bg-gradient-to-r from-indigo-600 to-violet-600 hover:from-indigo-500 hover:to-violet-500 text-white font-bold text-xs rounded-xl shadow-lg shadow-indigo-600/30 hover:translate-y-[-1px] active:translate-y-[0px] transition-all flex items-center justify-center gap-2"
              >
                <Search className="w-4 h-4" />
                <span>Search Properties</span>
              </button>
            </div>
          </form>

          {/* Quick city pills */}
          <div className="mt-4 pt-3 border-t border-slate-800/60 flex items-center gap-2 overflow-x-auto no-scrollbar text-xs">
            <span className="text-slate-400 text-[11px] font-semibold shrink-0">Popular:</span>
            {popularCities.map((c) => (
              <button
                key={c.name}
                type="button"
                onClick={() => handleQuickCity(c.name)}
                className="px-2.5 py-1 rounded-lg bg-slate-900/60 hover:bg-slate-800 border border-slate-800 hover:border-slate-700 text-slate-300 text-[11px] whitespace-nowrap transition-colors"
              >
                {c.name}
              </button>
            ))}
          </div>
        </div>

        {/* TRUST METRICS STRIP */}
        <div className="mt-14 max-w-5xl mx-auto grid grid-cols-2 md:grid-cols-4 gap-4 sm:gap-6 text-center">
          <div className="glass p-4 rounded-2xl border border-slate-800/80">
            <div className="text-2xl sm:text-3xl font-black text-white">₹0</div>
            <div className="text-xs text-slate-400 font-medium mt-0.5">Brokerage Fees Paid</div>
          </div>
          <div className="glass p-4 rounded-2xl border border-slate-800/80">
            <div className="text-2xl sm:text-3xl font-black text-emerald-400">100%</div>
            <div className="text-xs text-slate-400 font-medium mt-0.5">Title-Verified Owners</div>
          </div>
          <div className="glass p-4 rounded-2xl border border-slate-800/80">
            <div className="text-2xl sm:text-3xl font-black text-indigo-400">2,400+</div>
            <div className="text-xs text-slate-400 font-medium mt-0.5">Active Verified Listings</div>
          </div>
          <div className="glass p-4 rounded-2xl border border-slate-800/80">
            <div className="text-2xl sm:text-3xl font-black text-amber-400">4.9 / 5</div>
            <div className="text-xs text-slate-400 font-medium mt-0.5">Direct Tenant Trust Rating</div>
          </div>
        </div>
      </section>

      {/* DISCOVERY SECTIONS */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 relative z-10 space-y-10 border-t border-slate-900">
        <TrendingHomes />
        <RecommendedForYou />
      </section>

      {/* VERIFICATION PROTOCOL / VALUE PROPOSITION */}
      <section id="features" className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20 border-t border-slate-900 relative z-10">
        <div className="text-center max-w-3xl mx-auto mb-16 space-y-3">
          <span className="px-3.5 py-1 rounded-full bg-indigo-500/10 text-indigo-400 text-xs font-bold uppercase tracking-wider border border-indigo-500/20">
            The Verification Protocol
          </span>
          <h2 className="text-3xl sm:text-5xl font-black text-white tracking-tight">
            Engineered For Direct Trust
          </h2>
          <p className="text-slate-400 text-sm sm:text-base leading-relaxed">
            RoomWallah replaces middlemen with direct owner document verification, automated spam filtering, and structured deposit protection.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 lg:gap-8">
          {features.map((feat, idx) => (
            <div
              key={idx}
              className="glass glass-hover p-7 rounded-2xl border border-slate-800 flex flex-col justify-between group"
            >
              <div className="space-y-3.5">
                <div className="p-3 bg-slate-900/60 rounded-xl w-fit border border-slate-800 group-hover:scale-105 transition-transform">
                  {feat.icon}
                </div>
                <div>
                  <span className="text-[10px] uppercase font-bold tracking-widest text-indigo-400">
                    {feat.badge}
                  </span>
                  <h3 className="text-xl font-bold text-white mt-1">{feat.title}</h3>
                  <p className="text-slate-400 text-sm leading-relaxed mt-2">{feat.description}</p>
                </div>
              </div>
              <div className="mt-6 pt-4 border-t border-slate-800/60 flex items-center justify-between text-xs text-slate-500">
                <span>Active in All Top Metros</span>
                <span className="text-indigo-400 font-semibold group-hover:translate-x-1 transition-transform">
                  Learn more &rarr;
                </span>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* POPULAR LOCATIONS EXPLORATION */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16 border-t border-slate-900 relative z-10">
        <div className="flex flex-col sm:flex-row sm:items-end justify-between mb-10 gap-4">
          <div>
            <h2 className="text-2xl sm:text-3xl font-black text-white">Explore Top Indian Rental Hubs</h2>
            <p className="text-sm text-slate-400 mt-1">Direct owner listings verified across major IT and residential corridors</p>
          </div>
          <button
            onClick={() => navigate('/search')}
            className="text-xs font-bold text-indigo-400 hover:text-indigo-300 flex items-center gap-1.5 self-start sm:self-auto"
          >
            <span>View all 20+ cities</span>
            <ArrowRight className="w-4 h-4" />
          </button>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {popularCities.map((cityObj, idx) => (
            <div
              key={idx}
              onClick={() => handleQuickCity(cityObj.name)}
              className="glass p-5 rounded-2xl border border-slate-800/80 hover:border-indigo-500/40 cursor-pointer transition-all hover:-translate-y-1 group"
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2.5">
                  <div className="w-8 h-8 rounded-lg bg-indigo-500/10 border border-indigo-500/20 text-indigo-400 flex items-center justify-center font-bold text-xs">
                    {cityObj.name.charAt(0)}
                  </div>
                  <h3 className="text-base font-bold text-white group-hover:text-indigo-300 transition-colors">
                    {cityObj.name}
                  </h3>
                </div>
                <ArrowRight className="w-4 h-4 text-slate-500 group-hover:text-indigo-400 transition-colors group-hover:translate-x-1" />
              </div>
              <p className="text-xs text-slate-400 mt-2.5 line-clamp-1">
                {cityObj.localities}
              </p>
            </div>
          ))}
        </div>
      </section>

      {/* TRUST COMPARISON: TRADITIONAL BROKERS VS ROOMWALLAH */}
      <section className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-16 border-t border-slate-900 relative z-10">
        <div className="glass-card rounded-3xl p-6 sm:p-10 border border-slate-800">
          <div className="text-center max-w-2xl mx-auto mb-8 space-y-2">
            <h2 className="text-2xl sm:text-3xl font-black text-white">The RoomWallah Difference</h2>
            <p className="text-xs sm:text-sm text-slate-400">Why thousands of tenants and owners are ditching local broker networks</p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Traditional Brokers */}
            <div className="p-6 rounded-2xl bg-slate-900/50 border border-rose-950/40 space-y-3">
              <div className="flex items-center gap-2 text-rose-400 font-bold text-sm">
                <ShieldAlert className="w-4 h-4" />
                <span>Traditional Broker Platforms</span>
              </div>
              <ul className="space-y-2 text-xs text-slate-400">
                <li className="flex items-start gap-2">
                  <span className="text-rose-500 font-bold">✕</span>
                  <span>1 to 2 months rent charged as brokerage commission</span>
                </li>
                <li className="flex items-start gap-2">
                  <span className="text-rose-500 font-bold">✕</span>
                  <span>Fake, bait-and-switch photos taken years ago</span>
                </li>
                <li className="flex items-start gap-2">
                  <span className="text-rose-500 font-bold">✕</span>
                  <span>Phone numbers sold to hundreds of telemarketers</span>
                </li>
                <li className="flex items-start gap-2">
                  <span className="text-rose-500 font-bold">✕</span>
                  <span>Arbitrary deposit deductions at move-out</span>
                </li>
              </ul>
            </div>

            {/* RoomWallah */}
            <div className="p-6 rounded-2xl bg-indigo-950/20 border border-indigo-500/30 space-y-3">
              <div className="flex items-center gap-2 text-emerald-400 font-bold text-sm">
                <ShieldCheck className="w-4 h-4" />
                <span>RoomWallah Direct Protocol</span>
              </div>
              <ul className="space-y-2 text-xs text-slate-300">
                <li className="flex items-start gap-2">
                  <span className="text-emerald-400 font-bold">✓</span>
                  <span>Zero brokerage ever — direct owner agreements</span>
                </li>
                <li className="flex items-start gap-2">
                  <span className="text-emerald-400 font-bold">✓</span>
                  <span>Multi-tier document and owner identity verification</span>
                </li>
                <li className="flex items-start gap-2">
                  <span className="text-emerald-400 font-bold">✓</span>
                  <span>Zero spam: private in-app chat & calendar scheduling</span>
                </li>
                <li className="flex items-start gap-2">
                  <span className="text-emerald-400 font-bold">✓</span>
                  <span>Milestone-protected rental security deposit management</span>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </section>

      {/* ACCORDION FAQ */}
      <section id="faq" className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-16 border-t border-slate-900 relative z-10">
        <div className="text-center mb-10 space-y-2">
          <h2 className="text-3xl font-black text-white">Frequently Asked Questions</h2>
          <p className="text-sm text-slate-400">Everything you need to know about broker-free renting on RoomWallah</p>
        </div>

        <div className="space-y-3">
          {faqs.map((faq, idx) => {
            const isOpen = openFaq === idx;
            return (
              <div
                key={idx}
                className="glass rounded-2xl border border-slate-800/80 overflow-hidden transition-all"
              >
                <button
                  type="button"
                  onClick={() => setOpenFaq(isOpen ? null : idx)}
                  className="w-full p-5 text-left flex items-center justify-between gap-4 text-sm font-bold text-slate-200 hover:text-white transition-colors"
                >
                  <span>{faq.q}</span>
                  <ChevronDown className={`w-4 h-4 text-slate-400 shrink-0 transition-transform duration-200 ${isOpen ? 'rotate-180 text-indigo-400' : ''}`} />
                </button>
                {isOpen && (
                  <div className="px-5 pb-5 text-xs text-slate-400 leading-relaxed border-t border-slate-800/40 pt-3 animate-fade-in">
                    {faq.a}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </section>

      {/* OWNER CTA SECTION */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20 border-t border-slate-900 relative z-10">
        <div className="glass-card rounded-3xl p-8 sm:p-14 border border-indigo-500/20 text-center relative overflow-hidden">
          <div className="max-w-2xl mx-auto space-y-5 relative z-10">
            <h2 className="text-3xl sm:text-5xl font-black text-white tracking-tight">
              Are You a Property Owner?
            </h2>
            <p className="text-sm sm:text-base text-slate-300 leading-relaxed">
              List your rental home or property for free. Access over 50,000+ verified corporate tenants without entertaining a single broker call.
            </p>
            <div className="pt-2 flex flex-col sm:flex-row items-center justify-center gap-3">
              <button
                onClick={() => navigate('/listings/create')}
                className="w-full sm:w-auto px-8 py-3.5 bg-gradient-to-r from-indigo-600 to-violet-600 hover:from-indigo-500 hover:to-violet-500 text-white font-bold text-sm rounded-xl shadow-lg shadow-indigo-600/30 transition-all flex items-center justify-center gap-2"
              >
                <span>Post Your Property (Free)</span>
                <ArrowRight className="w-4 h-4" />
              </button>
              <button
                onClick={() => navigate('/search')}
                className="w-full sm:w-auto px-8 py-3.5 bg-slate-900 hover:bg-slate-800 text-slate-300 hover:text-white border border-slate-800 font-semibold text-sm rounded-xl transition-all"
              >
                Explore Properties First
              </button>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
