import { UserCheck, Shield, Video, Compass, Sparkles, ArrowRight, ShieldCheck, HelpCircle } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import TrendingHomes from '../components/TrendingHomes';
import RecommendedForYou from '../components/RecommendedForYou';

export default function LandingPage() {
  const navigate = useNavigate();
  const features = [
    {
      icon: <UserCheck className="w-8 h-8 text-indigo-400" />,
      title: "Verified Owners",
      description: "Direct owner listings backed by property title validation and physical verification checks. Say goodbye to fake agent ads.",
      badge: "Verification Protocol"
    },
    {
      icon: <Shield className="w-8 h-8 text-fuchsia-400" />,
      title: "Trust Score",
      description: "A dynamic rating for landlords and tenants based on digital footprint, reviews, and anti-spam community feedback.",
      badge: "AI Guard"
    },
    {
      icon: <Video className="w-8 h-8 text-pink-400" />,
      title: "Video Walkthroughs",
      description: "Pre-verified, high-definition videos uploaded by owners, ensuring the home looks exactly as advertised before your visit.",
      badge: "Real-Preview"
    },
    {
      icon: <Compass className="w-8 h-8 text-cyan-400" />,
      title: "3D Property Tours",
      description: "Immersive 3D virtual tours allowing you to inspect every corner, measure spacing, and evaluate layouts from your screen.",
      badge: "Virtual Space"
    }
  ];

  const faqs = [
    {
      q: "How does RoomWallah prevent broker listings?",
      a: "We use a combination of strict KYC checks, phone number matching against known broker databases, and community flagging algorithms to detect and permanently block intermediaries."
    },
    {
      q: "Is the platform free to use?",
      a: "Yes, searching properties and listing your first home is free. We offer premium verification packs for advanced visibility."
    },
    {
      q: "What is a Trust Score?",
      a: "It's a scoring system that rates user credibility based on verified history, past tenancy agreements, rent payment records, and landlord/tenant reviews."
    }
  ];

  return (
    <div className="relative overflow-hidden w-full">
      {/* Background gradients */}
      <div className="absolute top-[-10%] left-[-10%] w-[50%] h-[50%] rounded-full bg-indigo-900/10 blur-[120px] pointer-events-none"></div>
      <div className="absolute bottom-[-10%] right-[-10%] w-[50%] h-[50%] rounded-full bg-purple-900/10 blur-[120px] pointer-events-none"></div>

      {/* Hero Section */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-12 pb-20 md:pt-20 md:pb-32 text-center relative z-10">
        <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full glass text-xs font-medium text-slate-300 mb-6 border border-slate-800 animate-fade-in">
          <Sparkles className="w-3.5 h-3.5 text-primary animate-pulse" />
          <span>Next-Gen Broker-Free Property Marketplace</span>
        </div>

        <h1 className="text-4xl sm:text-6xl md:text-7xl font-extrabold tracking-tight mb-6 leading-tight animate-slide-up">
          Find{" "}
          <span className="bg-gradient-to-r from-primary via-indigo-300 to-secondary bg-clip-text text-transparent">
            Verified Homes
          </span>{" "}
          <br className="hidden sm:inline" />
          Without Brokers
        </h1>

        <p className="max-w-2xl mx-auto text-base sm:text-xl text-slate-400 mb-10 leading-relaxed animate-fade-in" style={{ animationDelay: '0.2s' }}>
          RoomWallah matches verified owners directly with tenants and buyers. No brokerage fees, no broker spam, and full smart contract tenancy support.
        </p>

        {/* CTA Buttons */}
        <div className="flex flex-col sm:flex-row items-center justify-center gap-4 animate-fade-in" style={{ animationDelay: '0.3s' }}>
          <button
            onClick={() => navigate('/search')}
            className="w-full sm:w-auto px-8 py-4 bg-gradient-to-r from-primary to-secondary text-white font-semibold rounded-xl hover:opacity-95 hover:shadow-lg hover:shadow-indigo-500/10 hover:translate-y-[-2px] transition-all flex items-center justify-center gap-2"
          >
            Explore Properties
            <ArrowRight className="w-5 h-5" />
          </button>
          
          <button
            onClick={() => navigate('/listings/create')}
            className="w-full sm:w-auto px-8 py-4 bg-slate-900/50 hover:bg-slate-800/80 text-white border border-slate-800 font-semibold rounded-xl hover:translate-y-[-2px] transition-all flex items-center justify-center"
          >
            List Your Property
          </button>
        </div>

        {/* Trust verification ticker */}
        <div className="mt-16 flex flex-wrap items-center justify-center gap-x-12 gap-y-6 text-xs text-slate-500 uppercase tracking-widest border-t border-slate-900 pt-8 max-w-4xl mx-auto">
          <div className="flex items-center gap-2">
            <ShieldCheck className="w-4 h-4 text-primary" />
            <span>AI Anti-Broker Filter</span>
          </div>
          <div className="flex items-center gap-2">
            <ShieldCheck className="w-4 h-4 text-primary" />
            <span>Digital Signatures Ready</span>
          </div>
          <div className="flex items-center gap-2">
            <ShieldCheck className="w-4 h-4 text-primary" />
            <span>KYC Verified Listings</span>
          </div>
        </div>
      </section>

      {/* Discovery Section: Trending & Recommendations */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 relative z-10 space-y-8 border-t border-slate-900">
        <TrendingHomes />
        <RecommendedForYou />
      </section>

      {/* Feature Cards Section */}
      <section id="features" className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16 border-t border-slate-900 relative z-10">
        <div className="text-center max-w-3xl mx-auto mb-16">
          <h2 className="text-3xl sm:text-4xl font-extrabold mb-4 bg-gradient-to-r from-white to-slate-300 bg-clip-text text-transparent">
            Built for Direct Relationships
          </h2>
          <p className="text-slate-400">
            Every feature on RoomWallah is engineered to enforce truthfulness, filter out real-estate middlemen, and make home hunting seamless.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
          {features.map((feat, idx) => (
            <div 
              key={idx}
              className="glass glass-hover p-8 rounded-2xl transition-all duration-300 flex flex-col justify-between group"
            >
              <div className="space-y-4">
                <div className="p-3 bg-slate-900/40 rounded-xl w-fit border border-slate-800/60 group-hover:scale-105 transition-transform duration-300">
                  {feat.icon}
                </div>
                <div className="space-y-2">
                  <span className="text-[10px] uppercase tracking-widest text-primary font-bold">
                    {feat.badge}
                  </span>
                  <h3 className="text-2xl font-bold text-slate-100">{feat.title}</h3>
                  <p className="text-slate-400 text-sm leading-relaxed">{feat.description}</p>
                </div>
              </div>
              <div className="mt-6 pt-4 border-t border-slate-800/50 flex items-center justify-between text-xs text-slate-500 font-medium">
                <span>Phase 1 Integration Ready</span>
                <span className="text-primary group-hover:translate-x-1 transition-transform">Learn more &rarr;</span>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* Trust & Broker Detection Protocol Section */}
      <section id="about" className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16 border-t border-slate-900 relative z-10">
        <div className="glass p-8 sm:p-12 rounded-3xl overflow-hidden relative border border-slate-800">
          <div className="absolute top-0 right-0 w-[40%] h-[100%] bg-gradient-to-l from-indigo-500/5 to-transparent pointer-events-none"></div>
          
          <div className="max-w-2xl space-y-6 relative z-10">
            <span className="px-3.5 py-1 rounded-full bg-indigo-500/10 text-primary text-xs font-semibold border border-indigo-500/20">
              Technology Stack
            </span>
            <h2 className="text-3xl sm:text-4xl font-bold leading-tight text-slate-100">
              Say Goodbye to Fake Ads and Heavy Brokerage Commissions
            </h2>
            <p className="text-slate-400 leading-relaxed text-sm sm:text-base">
              Traditional real-estate portals are filled with duplicate listings posted by agents charging up to two months of rent as fee. RoomWallah uses custom verification logic, automated title matching, and local community verification checks to keep brokers out completely.
            </p>
            <div className="flex flex-col sm:flex-row gap-4 pt-4">
              <div className="flex items-center gap-3">
                <div className="w-5 h-5 rounded-full bg-emerald-500/10 flex items-center justify-center text-emerald-400 text-xs font-bold">✓</div>
                <span className="text-sm font-medium text-slate-300">0% Commission Ever</span>
              </div>
              <div className="flex items-center gap-3">
                <div className="w-5 h-5 rounded-full bg-emerald-500/10 flex items-center justify-center text-emerald-400 text-xs font-bold">✓</div>
                <span className="text-sm font-medium text-slate-300">Anti-Duplicate System</span>
              </div>
              <div className="flex items-center gap-3">
                <div className="w-5 h-5 rounded-full bg-emerald-500/10 flex items-center justify-center text-emerald-400 text-xs font-bold">✓</div>
                <span className="text-sm font-medium text-slate-300">Self-Executing Rental Agreements</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* FAQ Section */}
      <section id="faq" className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-16 border-t border-slate-900 relative z-10">
        <h2 className="text-3xl font-bold text-center mb-12 text-slate-100">Frequently Asked Questions</h2>
        <div className="space-y-6">
          {faqs.map((faq, idx) => (
            <div key={idx} className="glass p-6 rounded-xl border border-slate-800">
              <div className="flex gap-4">
                <HelpCircle className="w-6 h-6 text-primary shrink-0" />
                <div className="space-y-2">
                  <h3 className="font-bold text-slate-200 text-base">{faq.q}</h3>
                  <p className="text-slate-400 text-sm leading-relaxed">{faq.a}</p>
                </div>
              </div>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
