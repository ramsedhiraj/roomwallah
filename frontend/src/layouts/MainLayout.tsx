import { useState, useEffect } from 'react';
import { Outlet, Link, useNavigate } from 'react-router-dom';
import { Menu, X, Shield, ChevronRight, LogOut, User as UserIcon, Settings, Key, Building2, Search, Heart } from 'lucide-react';
import { useAuthStore } from '../store/authStore';
import { apiClient } from '../services/api';
import NotificationCenter from '../components/NotificationCenter';
import { useWishlistStore } from '../store/wishlistStore';

export default function MainLayout() {
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [isScrolled, setIsScrolled] = useState(false);
  const { isAuthenticated, user, logout } = useAuthStore();
  const navigate = useNavigate();
  const [headerSearch, setHeaderSearch] = useState('');

  const handleHeaderSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (headerSearch.trim()) {
      navigate(`/search?q=${encodeURIComponent(headerSearch.trim())}`);
      setHeaderSearch('');
    }
  };

  const loadWishlist = useWishlistStore((state) => state.load);
  const wishlistLoaded = useWishlistStore((state) => state.isLoaded);

  useEffect(() => {
    if (isAuthenticated && !wishlistLoaded) {
      loadWishlist();
    }
  }, [isAuthenticated, wishlistLoaded, loadWishlist]);

  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 20);
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  const handleLogout = async () => {
    try {
      const rawRefreshToken = localStorage.getItem('refreshToken');
      if (rawRefreshToken) {
        await apiClient.post('/auth/logout', { refreshToken: rawRefreshToken });
      }
    } catch (err) {
      console.error('Server logout failed:', err);
    } finally {
      logout();
      navigate('/');
    }
  };

  return (
    <div className="flex flex-col min-h-screen bg-[#090d16] text-[#f1f5f9]">
      {/* Header / Navbar */}
      <header className={`fixed top-0 left-0 w-full z-50 transition-all duration-300 ${
        isScrolled ? 'glass py-3 shadow-lg' : 'bg-transparent py-5'
      }`}>
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between">
            {/* Logo */}
            <Link to="/" className="flex items-center space-x-2 text-2xl font-bold tracking-tight">
              <span className="flex items-center justify-center w-10 h-10 rounded-lg bg-gradient-to-tr from-primary to-secondary text-white font-extrabold shadow-md glow-indigo">
                R
              </span>
              <span className="bg-gradient-to-r from-white via-slate-100 to-indigo-200 bg-clip-text text-transparent">
                RoomWallah
              </span>
            </Link>

            {/* Desktop Navigation */}
            <nav className="hidden md:flex items-center space-x-8 text-sm font-medium">
              <a href="#features" className="text-slate-300 hover:text-white transition-colors">Features</a>
              <a href="#about" className="text-slate-300 hover:text-white transition-colors">About</a>
              <a href="#testimonials" className="text-slate-300 hover:text-white transition-colors">Reviews</a>
              <a href="#faq" className="text-slate-300 hover:text-white transition-colors">FAQ</a>
            </nav>

            {/* Header Search Bar */}
            <form onSubmit={handleHeaderSearch} className="hidden lg:flex items-center relative max-w-[200px] w-full mx-4">
              <input
                type="text"
                value={headerSearch}
                onChange={(e) => setHeaderSearch(e.target.value)}
                placeholder="Search properties..."
                className="w-full pl-3 pr-8 py-1.5 rounded-xl bg-slate-900/60 border border-slate-800 focus:border-primary text-xs text-white placeholder-slate-500 focus:outline-none transition-all"
              />
              <button type="button" onClick={handleHeaderSearch} className="absolute right-2.5 text-slate-400 hover:text-white transition-colors">
                <Search className="w-3.5 h-3.5" />
              </button>
            </form>

            {/* Desktop CTA Action buttons */}
            <div className="hidden md:flex items-center space-x-4">
              {isAuthenticated ? (
                <div className="relative flex items-center space-x-3">
                  <Link 
                    to="/listings" 
                    className="flex items-center space-x-2 px-3 py-2 rounded-xl bg-slate-900/80 border border-slate-800 hover:border-slate-700 hover:bg-slate-850/80 transition-all text-sm font-semibold text-slate-200"
                  >
                    <Building2 className="w-4 h-4 text-primary" />
                    <span>My Listings</span>
                  </Link>

                  <Link 
                    to="/wishlist" 
                    className="flex items-center space-x-2 px-3 py-2 rounded-xl bg-slate-900/80 border border-slate-800 hover:border-slate-700 hover:bg-slate-850/80 transition-all text-sm font-semibold text-slate-200"
                    title="My Wishlist"
                  >
                    <Heart className="w-4 h-4 text-red-500 fill-red-500/20" />
                    <span>Wishlist</span>
                  </Link>

                  <NotificationCenter />

                  <Link 
                    to="/profile" 
                    className="flex items-center space-x-2.5 px-3.5 py-2 rounded-xl bg-slate-900/80 border border-slate-800 hover:border-slate-700 hover:bg-slate-850/80 transition-all text-sm font-semibold text-slate-200"
                  >
                    <UserIcon className="w-4 h-4 text-slate-400" />
                    <span>{user?.fullName}</span>
                  </Link>

                  <Link 
                    to="/settings" 
                    className="p-2.5 rounded-xl bg-slate-900/80 border border-slate-800 hover:border-slate-700 hover:bg-slate-850/80 text-slate-400 hover:text-white transition-all"
                    title="Account Settings"
                  >
                    <Settings className="w-4 h-4" />
                  </Link>

                  <Link 
                    to="/change-password" 
                    className="p-2.5 rounded-xl bg-slate-900/80 border border-slate-800 hover:border-slate-700 hover:bg-slate-850/80 text-slate-400 hover:text-white transition-all"
                    title="Change Password"
                  >
                    <Key className="w-4 h-4" />
                  </Link>

                  <button 
                    onClick={handleLogout}
                    className="p-2.5 rounded-xl bg-slate-900/80 border border-slate-800 hover:border-red-900/30 hover:bg-rose-950/20 text-red-400 hover:text-red-300 transition-all"
                    title="Sign Out"
                  >
                    <LogOut className="w-4 h-4" />
                  </button>
                </div>
              ) : (
                <>
                  <Link 
                    to="/login"
                    className="text-sm font-semibold text-slate-300 hover:text-white transition-colors px-4 py-2"
                  >
                    Sign In
                  </Link>
                  <Link 
                    to="/register"
                    className="text-sm font-semibold px-5 py-2.5 rounded-lg bg-gradient-to-r from-primary to-secondary text-white hover:opacity-95 transition-all shadow-md flex items-center gap-1.5 hover:translate-y-[-1px] active:translate-y-[0px]"
                  >
                    Get Started
                    <ChevronRight className="w-4 h-4" />
                  </Link>
                </>
              )}
            </div>

            {/* Mobile menu button */}
            <div className="md:hidden flex items-center">
              <button
                onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
                className="text-slate-300 hover:text-white p-2"
                aria-label="Toggle menu"
              >
                {isMobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
              </button>
            </div>
          </div>
        </div>

        {/* Mobile Navigation Drawer */}
        {isMobileMenuOpen && (
          <div className="md:hidden glass border-t border-slate-800 animate-fade-in">
            <div className="px-2 pt-2 pb-4 space-y-1 sm:px-3">
              <a
                href="#features"
                onClick={() => setIsMobileMenuOpen(false)}
                className="block px-3 py-2.5 rounded-md text-base font-medium text-slate-300 hover:bg-slate-800 hover:text-white transition-colors"
              >
                Features
              </a>
              <a
                href="#about"
                onClick={() => setIsMobileMenuOpen(false)}
                className="block px-3 py-2.5 rounded-md text-base font-medium text-slate-300 hover:bg-slate-800 hover:text-white transition-colors"
              >
                About
              </a>
              <a
                href="#testimonials"
                onClick={() => setIsMobileMenuOpen(false)}
                className="block px-3 py-2.5 rounded-md text-base font-medium text-slate-300 hover:bg-slate-800 hover:text-white transition-colors"
              >
                Reviews
              </a>
              <a
                href="#faq"
                onClick={() => setIsMobileMenuOpen(false)}
                className="block px-3 py-2.5 rounded-md text-base font-medium text-slate-300 hover:bg-slate-800 hover:text-white transition-colors"
              >
                FAQ
              </a>
              <div className="pt-4 pb-2 border-t border-slate-800 flex flex-col space-y-2 px-3">
                {isAuthenticated ? (
                  <>
                    <div className="w-full text-center py-2 text-xs font-medium text-slate-400">
                      Signed in as {user?.fullName} ({user?.role})
                    </div>
                    <Link
                      to="/listings"
                      onClick={() => setIsMobileMenuOpen(false)}
                      className="w-full text-center py-2.5 rounded-lg text-sm font-semibold text-slate-350 hover:bg-slate-800 flex items-center justify-center gap-1.5"
                    >
                      My Listings
                    </Link>
                    <Link
                      to="/wishlist"
                      onClick={() => setIsMobileMenuOpen(false)}
                      className="w-full text-center py-2.5 rounded-lg text-sm font-semibold text-slate-350 hover:bg-slate-800 flex items-center justify-center gap-1.5"
                    >
                      Wishlist
                    </Link>
                    <button
                      onClick={() => {
                        setIsMobileMenuOpen(false);
                        handleLogout();
                      }}
                      className="w-full text-center py-2.5 rounded-lg text-sm font-semibold text-red-400 hover:bg-slate-800 flex items-center justify-center gap-1.5"
                    >
                      <LogOut className="w-4 h-4" />
                      Sign Out
                    </button>
                  </>
                ) : (
                  <>
                    <Link
                      to="/login"
                      onClick={() => setIsMobileMenuOpen(false)}
                      className="w-full text-center py-2.5 rounded-lg text-sm font-semibold text-slate-300 hover:bg-slate-800"
                    >
                      Sign In
                    </Link>
                    <Link
                      to="/register"
                      onClick={() => setIsMobileMenuOpen(false)}
                      className="w-full text-center py-2.5 rounded-lg text-sm font-semibold bg-gradient-to-r from-primary to-secondary text-white block"
                    >
                      Get Started
                    </Link>
                  </>
                )}
              </div>
            </div>
          </div>
        )}
      </header>

      {/* Main Content Area */}
      <main className="flex-grow pt-24">
        <Outlet />
      </main>

      {/* Footer */}
      <footer className="bg-[#06080e] border-t border-slate-900 py-12 mt-20">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
            <div className="md:col-span-2 space-y-4">
              <div className="flex items-center space-x-2 text-xl font-bold">
                <span className="flex items-center justify-center w-8 h-8 rounded bg-gradient-to-tr from-primary to-secondary text-white font-extrabold text-sm">
                  R
                </span>
                <span>RoomWallah</span>
              </div>
              <p className="text-slate-400 text-sm max-w-sm">
                RoomWallah is a broker-resistant full-stack portal designed to connect tenants with verified owners without broker interference. High trust, zero spam.
              </p>
              <div className="flex items-center space-x-1.5 text-slate-500 text-xs">
                <Shield className="w-4 h-4 text-emerald-500" />
                <span>Broker-Resistance Protocol Active</span>
              </div>
            </div>
            
            <div>
              <h3 className="text-sm font-semibold text-slate-300 uppercase tracking-wider mb-4">Platform</h3>
              <ul className="space-y-2 text-sm text-slate-400">
                <li><a href="#features" className="hover:text-white transition-colors">Features</a></li>
                <li><a href="#tours" className="hover:text-white transition-colors">3D Property Tours</a></li>
                <li><a href="#trust" className="hover:text-white transition-colors">Trust Scoring</a></li>
              </ul>
            </div>

            <div>
              <h3 className="text-sm font-semibold text-slate-300 uppercase tracking-wider mb-4">Company</h3>
              <ul className="space-y-2 text-sm text-slate-400">
                <li><a href="#about" className="hover:text-white transition-colors">About Us</a></li>
                <li><a href="#privacy" className="hover:text-white transition-colors">Privacy Policy</a></li>
                <li><a href="#terms" className="hover:text-white transition-colors">Terms of Service</a></li>
              </ul>
            </div>
          </div>
          
          <div className="mt-8 pt-8 border-t border-slate-900 text-center text-xs text-slate-500 flex flex-col sm:flex-row items-center justify-between gap-4">
            <p>&copy; {new Date().getFullYear()} RoomWallah. All rights reserved.</p>
            <p>Made with ❤️ for broker-free renting.</p>
          </div>
        </div>
      </footer>
    </div>
  );
}
