import { useState, useEffect } from 'react';
import { Outlet, Link, useNavigate, useLocation } from 'react-router-dom';
import { 
  Menu, X, Shield, LogOut, User as UserIcon, Settings, 
  Building2, Search, Heart, PlusCircle, Compass, Home as HomeIcon,
  CalendarDays, CheckCircle2
} from 'lucide-react';
import { useAuthStore } from '../store/authStore';
import { apiClient } from '../services/api';
import NotificationCenter from '../components/NotificationCenter';
import { useWishlistStore } from '../store/wishlistStore';

export default function MainLayout() {
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [isScrolled, setIsScrolled] = useState(false);
  const [userDropdownOpen, setUserDropdownOpen] = useState(false);
  const { isAuthenticated, user, logout } = useAuthStore();
  const navigate = useNavigate();
  const location = useLocation();
  const [headerSearch, setHeaderSearch] = useState('');

  const handleHeaderSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (headerSearch.trim()) {
      navigate(`/search?q=${encodeURIComponent(headerSearch.trim())}`);
      setHeaderSearch('');
      setIsMobileMenuOpen(false);
    }
  };

  const loadWishlist = useWishlistStore((state) => state.load);
  const wishlistLoaded = useWishlistStore((state) => state.isLoaded);
  const wishlistedIds = useWishlistStore((state) => state.wishlistedIds);

  useEffect(() => {
    if (isAuthenticated && !wishlistLoaded) {
      loadWishlist().catch(() => {});
    }
  }, [isAuthenticated, wishlistLoaded, loadWishlist]);

  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 15);
    };
    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  // Close menus on route change
  useEffect(() => {
    setIsMobileMenuOpen(false);
    setUserDropdownOpen(false);
  }, [location.pathname]);

  const handleLogout = async () => {
    try {
      const rawRefreshToken = localStorage.getItem('refreshToken');
      if (rawRefreshToken) {
        await apiClient.post('/auth/logout', { refreshToken: rawRefreshToken });
      }
    } catch {
      // ignore server logout error
    } finally {
      logout();
      navigate('/');
    }
  };

  const isActive = (path: string) => {
    if (path === '/') return location.pathname === '/';
    return location.pathname.startsWith(path);
  };

  return (
    <div className="flex flex-col min-h-screen bg-[#090d16] text-[#f1f5f9] selection:bg-indigo-500 selection:text-white">
      {/* Header / Navbar */}
      <header 
        className={`fixed top-0 left-0 w-full z-50 transition-all duration-300 ${
          isScrolled 
            ? 'bg-[#090d16]/90 backdrop-blur-md py-3.5 border-b border-slate-800/80 shadow-lg shadow-black/40' 
            : 'bg-transparent py-5'
        }`}
      >
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between gap-4">
            {/* Logo */}
            <Link to="/" className="flex items-center space-x-2.5 shrink-0 group">
              <span className="flex items-center justify-center w-9 h-9 rounded-xl bg-gradient-to-tr from-indigo-600 to-violet-500 text-white font-black text-lg shadow-md shadow-indigo-600/30 group-hover:scale-105 transition-transform">
                R
              </span>
              <span className="text-xl font-black tracking-tight text-white flex items-center gap-1">
                Room<span className="text-indigo-400">Wallah</span>
              </span>
            </Link>

            {/* Desktop Navigation Links */}
            <nav className="hidden lg:flex items-center space-x-1 text-sm font-medium text-slate-300">
              <Link 
                to="/search" 
                className={`px-3 py-1.5 rounded-lg transition-colors ${
                  isActive('/search') ? 'text-white bg-slate-800/60 font-semibold' : 'hover:text-white hover:bg-slate-800/40'
                }`}
              >
                Explore Properties
              </Link>
              <Link 
                to="/#features" 
                className="px-3 py-1.5 rounded-lg hover:text-white hover:bg-slate-800/40 transition-colors"
              >
                Verification Protocol
              </Link>
              <Link 
                to="/#faq" 
                className="px-3 py-1.5 rounded-lg hover:text-white hover:bg-slate-800/40 transition-colors"
              >
                FAQ
              </Link>
            </nav>

            {/* Header Search Bar (Desktop) */}
            <form onSubmit={handleHeaderSearch} className="hidden md:flex items-center relative max-w-xs w-full">
              <input
                type="text"
                value={headerSearch}
                onChange={(e) => setHeaderSearch(e.target.value)}
                placeholder="Search city, locality..."
                className="w-full pl-9 pr-4 py-2 rounded-xl bg-slate-900/80 border border-slate-800 focus:border-indigo-500 text-xs text-white placeholder-slate-400 focus:outline-none focus:ring-1 focus:ring-indigo-500/50 transition-all"
              />
              <Search className="w-4 h-4 text-slate-400 absolute left-3 pointer-events-none" />
            </form>

            {/* Action Buttons */}
            <div className="hidden md:flex items-center space-x-3 shrink-0">
              <Link
                to="/listings/create"
                className="flex items-center space-x-1.5 px-3.5 py-2 rounded-xl bg-slate-850 hover:bg-slate-800 text-slate-200 border border-slate-700/60 text-xs font-semibold hover:border-slate-600 transition-all"
              >
                <PlusCircle className="w-3.5 h-3.5 text-indigo-400" />
                <span>Post Listing</span>
              </Link>

              {isAuthenticated ? (
                <div className="flex items-center space-x-2">
                  <Link 
                    to="/wishlist" 
                    className="relative p-2 rounded-xl bg-slate-900/80 border border-slate-800 hover:border-slate-700 text-slate-300 hover:text-white transition-all"
                    title="Wishlist"
                  >
                    <Heart className={`w-4 h-4 ${wishlistedIds.length > 0 ? 'text-rose-500 fill-rose-500/30' : ''}`} />
                    {wishlistedIds.length > 0 && (
                      <span className="absolute -top-1 -right-1 w-4 h-4 rounded-full bg-rose-500 text-[10px] font-bold text-white flex items-center justify-center">
                        {wishlistedIds.length}
                      </span>
                    )}
                  </Link>

                  <NotificationCenter />

                  {/* Profile Dropdown Toggle */}
                  <div className="relative">
                    <button
                      onClick={() => setUserDropdownOpen(!userDropdownOpen)}
                      className="flex items-center space-x-2 px-3 py-1.5 rounded-xl bg-slate-900/90 border border-slate-800 hover:border-slate-700 text-xs font-semibold text-slate-200 transition-all focus:outline-none"
                    >
                      <div className="w-6 h-6 rounded-full bg-indigo-600/30 border border-indigo-500/40 text-indigo-300 font-bold flex items-center justify-center text-[11px]">
                        {user?.fullName?.charAt(0) || 'U'}
                      </div>
                      <span className="max-w-[100px] truncate">{user?.fullName?.split(' ')[0]}</span>
                    </button>

                    {userDropdownOpen && (
                      <div className="absolute right-0 mt-2 w-56 glass-card rounded-2xl p-2 border border-slate-700/70 shadow-2xl z-50 animate-fade-in">
                        <div className="px-3 py-2 border-b border-slate-800">
                          <p className="text-xs font-bold text-white truncate">{user?.fullName}</p>
                          <p className="text-[11px] text-slate-400 truncate">{user?.email}</p>
                          <span className="inline-block mt-1 px-2 py-0.5 rounded-md bg-indigo-500/10 text-indigo-400 text-[9px] font-semibold tracking-wider uppercase border border-indigo-500/20">
                            {user?.role}
                          </span>
                        </div>

                        <div className="py-1 space-y-0.5">
                          <Link
                            to="/profile"
                            className="flex items-center space-x-2 px-3 py-2 rounded-xl text-xs font-medium text-slate-300 hover:text-white hover:bg-slate-800/60 transition-colors"
                          >
                            <UserIcon className="w-3.5 h-3.5 text-slate-400" />
                            <span>My Profile</span>
                          </Link>
                          <Link
                            to="/listings"
                            className="flex items-center space-x-2 px-3 py-2 rounded-xl text-xs font-medium text-slate-300 hover:text-white hover:bg-slate-800/60 transition-colors"
                          >
                            <Building2 className="w-3.5 h-3.5 text-slate-400" />
                            <span>My Listings</span>
                          </Link>
                          <Link
                            to="/bookings"
                            className="flex items-center space-x-2 px-3 py-2 rounded-xl text-xs font-medium text-slate-300 hover:text-white hover:bg-slate-800/60 transition-colors"
                          >
                            <CalendarDays className="w-3.5 h-3.5 text-slate-400" />
                            <span>My Bookings / Visits</span>
                          </Link>
                          <Link
                            to="/settings"
                            className="flex items-center space-x-2 px-3 py-2 rounded-xl text-xs font-medium text-slate-300 hover:text-white hover:bg-slate-800/60 transition-colors"
                          >
                            <Settings className="w-3.5 h-3.5 text-slate-400" />
                            <span>Settings</span>
                          </Link>
                        </div>

                        <div className="pt-1 border-t border-slate-800">
                          <button
                            onClick={handleLogout}
                            className="w-full flex items-center space-x-2 px-3 py-2 rounded-xl text-xs font-medium text-rose-400 hover:text-rose-300 hover:bg-rose-500/10 transition-colors"
                          >
                            <LogOut className="w-3.5 h-3.5" />
                            <span>Sign Out</span>
                          </button>
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              ) : (
                <div className="flex items-center space-x-2">
                  <Link
                    to="/login"
                    className="text-xs font-semibold text-slate-300 hover:text-white px-3 py-2 transition-colors"
                  >
                    Sign In
                  </Link>
                  <Link
                    to="/register"
                    className="text-xs font-semibold px-4 py-2 rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 text-white hover:opacity-95 shadow-md shadow-indigo-600/20 hover:translate-y-[-1px] transition-all"
                  >
                    Get Started
                  </Link>
                </div>
              )}
            </div>

            {/* Mobile menu trigger */}
            <div className="md:hidden flex items-center gap-2">
              <Link 
                to="/search" 
                className="p-2 rounded-xl bg-slate-900 border border-slate-800 text-slate-300"
                aria-label="Search"
              >
                <Search className="w-4 h-4" />
              </Link>
              <button
                onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
                className="p-2 rounded-xl bg-slate-900 border border-slate-800 text-slate-300 hover:text-white"
                aria-label="Toggle menu"
              >
                {isMobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
              </button>
            </div>
          </div>
        </div>

        {/* Mobile Navigation Drawer */}
        {isMobileMenuOpen && (
          <div className="md:hidden bg-[#0c101c]/95 backdrop-blur-xl border-b border-slate-800 px-4 py-4 space-y-3 animate-fade-in shadow-2xl">
            <form onSubmit={handleHeaderSearch} className="relative w-full">
              <input
                type="text"
                value={headerSearch}
                onChange={(e) => setHeaderSearch(e.target.value)}
                placeholder="Search city, locality..."
                className="w-full pl-9 pr-4 py-2.5 rounded-xl bg-slate-900 border border-slate-800 text-sm text-white placeholder-slate-400 focus:outline-none focus:border-indigo-500"
              />
              <Search className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
            </form>

            <div className="space-y-1">
              <Link
                to="/search"
                className="block px-3 py-2.5 rounded-xl text-sm font-medium text-slate-200 hover:bg-slate-800/60"
              >
                Explore Properties
              </Link>
              <Link
                to="/listings/create"
                className="block px-3 py-2.5 rounded-xl text-sm font-medium text-indigo-400 hover:bg-slate-800/60 font-semibold"
              >
                + List Your Property (Free)
              </Link>
              {isAuthenticated ? (
                <>
                  <Link
                    to="/listings"
                    className="block px-3 py-2.5 rounded-xl text-sm font-medium text-slate-300 hover:bg-slate-800/60"
                  >
                    My Listings
                  </Link>
                  <Link
                    to="/wishlist"
                    className="block px-3 py-2.5 rounded-xl text-sm font-medium text-slate-300 hover:bg-slate-800/60"
                  >
                    Wishlist ({wishlistedIds.length})
                  </Link>
                  <Link
                    to="/bookings"
                    className="block px-3 py-2.5 rounded-xl text-sm font-medium text-slate-300 hover:bg-slate-800/60"
                  >
                    My Bookings
                  </Link>
                  <Link
                    to="/profile"
                    className="block px-3 py-2.5 rounded-xl text-sm font-medium text-slate-300 hover:bg-slate-800/60"
                  >
                    Account Profile
                  </Link>
                  <button
                    onClick={handleLogout}
                    className="w-full text-left px-3 py-2.5 rounded-xl text-sm font-semibold text-rose-400 hover:bg-rose-500/10 flex items-center gap-2"
                  >
                    <LogOut className="w-4 h-4" />
                    Sign Out
                  </button>
                </>
              ) : (
                <div className="pt-2 grid grid-cols-2 gap-2">
                  <Link
                    to="/login"
                    className="text-center py-2.5 rounded-xl bg-slate-900 border border-slate-800 text-sm font-semibold text-white"
                  >
                    Sign In
                  </Link>
                  <Link
                    to="/register"
                    className="text-center py-2.5 rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 text-sm font-semibold text-white"
                  >
                    Get Started
                  </Link>
                </div>
              )}
            </div>
          </div>
        )}
      </header>

      {/* Main Content Area */}
      <main className="flex-grow pt-24 pb-20 md:pb-0">
        <Outlet />
      </main>

      {/* Native-style Mobile Bottom Navigation Bar */}
      <div className="md:hidden fixed bottom-0 left-0 right-0 z-40 bg-[#090d16]/95 backdrop-blur-xl border-t border-slate-800/80 px-2 py-1.5 flex items-center justify-around shadow-2xl">
        <Link
          to="/"
          className={`flex flex-col items-center py-1 px-3 rounded-lg text-[10px] font-medium transition-colors ${
            location.pathname === '/' ? 'text-indigo-400 font-bold' : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          <HomeIcon className="w-5 h-5 mb-0.5" />
          <span>Home</span>
        </Link>
        <Link
          to="/search"
          className={`flex flex-col items-center py-1 px-3 rounded-lg text-[10px] font-medium transition-colors ${
            location.pathname.startsWith('/search') ? 'text-indigo-400 font-bold' : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          <Compass className="w-5 h-5 mb-0.5" />
          <span>Explore</span>
        </Link>
        <Link
          to="/listings/create"
          className="flex flex-col items-center py-1 px-3 text-[10px] font-semibold text-indigo-400"
        >
          <div className="w-7 h-7 -mt-3 rounded-full bg-gradient-to-tr from-indigo-600 to-violet-500 text-white flex items-center justify-center shadow-lg shadow-indigo-600/40">
            <PlusCircle className="w-4 h-4" />
          </div>
          <span className="mt-0.5">Post</span>
        </Link>
        <Link
          to="/wishlist"
          className={`flex flex-col items-center py-1 px-3 rounded-lg text-[10px] font-medium transition-colors relative ${
            location.pathname.startsWith('/wishlist') ? 'text-indigo-400 font-bold' : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          <Heart className="w-5 h-5 mb-0.5" />
          <span>Saved</span>
          {wishlistedIds.length > 0 && (
            <span className="absolute top-0.5 right-3 w-3.5 h-3.5 rounded-full bg-rose-500 text-[9px] font-bold text-white flex items-center justify-center">
              {wishlistedIds.length}
            </span>
          )}
        </Link>
        <Link
          to={isAuthenticated ? "/profile" : "/login"}
          className={`flex flex-col items-center py-1 px-3 rounded-lg text-[10px] font-medium transition-colors ${
            location.pathname.startsWith('/profile') || location.pathname.startsWith('/login')
              ? 'text-indigo-400 font-bold' 
              : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          <UserIcon className="w-5 h-5 mb-0.5" />
          <span>{isAuthenticated ? 'Account' : 'Sign In'}</span>
        </Link>
      </div>

      {/* Footer */}
      <footer className="bg-[#06080e] border-t border-slate-900/90 py-16 mt-24">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-10">
            <div className="md:col-span-2 space-y-4">
              <div className="flex items-center space-x-2.5 text-xl font-bold">
                <span className="flex items-center justify-center w-8 h-8 rounded-xl bg-gradient-to-tr from-indigo-600 to-violet-500 text-white font-black text-sm">
                  R
                </span>
                <span className="text-white">Room<span className="text-indigo-400">Wallah</span></span>
              </div>
              <p className="text-slate-400 text-sm max-w-md leading-relaxed">
                Next-generation zero-brokerage rental portal connecting verified owners directly with tenants and buyers. Backed by multi-document verification, transparent trust scores, and milestone-protected tenancy agreements.
              </p>
              <div className="flex items-center space-x-2 text-xs text-emerald-400 font-medium">
                <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                <span>Zero-Brokerage Community & Document Verification Protocols Active</span>
              </div>
            </div>
            
            <div>
              <h3 className="text-xs font-bold text-slate-300 uppercase tracking-widest mb-4">Discover</h3>
              <ul className="space-y-2.5 text-sm text-slate-400">
                <li><Link to="/search" className="hover:text-white transition-colors">Rental Homes</Link></li>
                <li><Link to="/search?purpose=SALE" className="hover:text-white transition-colors">Buy Property</Link></li>
                <li><Link to="/listings/create" className="hover:text-white transition-colors">Post Free Listing</Link></li>
                <li><Link to="/status" className="hover:text-white transition-colors">System Health</Link></li>
              </ul>
            </div>

            <div>
              <h3 className="text-xs font-bold text-slate-300 uppercase tracking-widest mb-4">Platform & Trust</h3>
              <ul className="space-y-2.5 text-sm text-slate-400">
                <li><a href="#features" className="hover:text-white transition-colors">Verification Rules</a></li>
                <li><a href="#features" className="hover:text-white transition-colors">Anti-Spam Filter</a></li>
                <li><Link to="/login" className="hover:text-white transition-colors">Owner Portal</Link></li>
                <li><Link to="/login" className="hover:text-white transition-colors">Tenant Desk</Link></li>
              </ul>
            </div>
          </div>
          
          <div className="mt-12 pt-8 border-t border-slate-900 text-center text-xs text-slate-500 flex flex-col sm:flex-row items-center justify-between gap-4">
            <p>&copy; {new Date().getFullYear()} RoomWallah Technologies Pvt Ltd. All rights reserved.</p>
            <div className="flex items-center gap-6">
              <span className="hover:text-slate-400 cursor-pointer">Privacy Policy</span>
              <span className="hover:text-slate-400 cursor-pointer">Terms of Service</span>
              <span className="hover:text-slate-400 cursor-pointer">Trust & Security</span>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
}
