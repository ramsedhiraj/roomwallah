import React, { useState, useEffect, useRef } from 'react';
import { 
  Bell, Check, Settings,MessageSquare, 
  ShieldAlert, Calendar, DollarSign, Wifi, WifiOff 
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';

export interface Notification {
  id: string;
  title: string;
  message: string;
  category: 'BOOKING' | 'PAYMENT' | 'SECURITY' | 'TRUST' | 'CHAT';
  status: 'UNREAD' | 'READ';
  channel: 'SMS' | 'WHATSAPP' | 'EMAIL' | 'PUSH';
  createdAt: string;
}

const initialNotifications: Notification[] = [
  {
    id: 'notif-1',
    title: 'Booking Confirmed',
    message: 'Your property visit for #PRP-10492 has been scheduled for tomorrow at 10:00 AM.',
    category: 'BOOKING',
    status: 'UNREAD',
    channel: 'WHATSAPP',
    createdAt: '2026-06-15T18:40:00Z',
  },
  {
    id: 'notif-2',
    title: 'Security Alert',
    message: 'A new login attempt was detected from a Chrome browser on Windows.',
    category: 'SECURITY',
    status: 'UNREAD',
    channel: 'EMAIL',
    createdAt: '2026-06-15T16:15:00Z',
  },
  {
    id: 'notif-3',
    title: 'Escrow Released',
    message: 'Payout of ₹45,000 has been released to owner account.',
    category: 'PAYMENT',
    status: 'READ',
    channel: 'SMS',
    createdAt: '2026-06-14T12:00:00Z',
  },
  {
    id: 'notif-4',
    title: 'Trust Document Verified',
    message: 'Your landlord verification KYC application has been approved!',
    category: 'TRUST',
    status: 'READ',
    channel: 'EMAIL',
    createdAt: '2026-06-13T09:30:00Z',
  },
];

export default function NotificationCenter() {
  const [notifications, setNotifications] = useState<Notification[]>(initialNotifications);
  const [isOpen, setIsOpen] = useState(false);
  const [isLive] = useState(true); // Mock SSE state
  const dropdownRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();

  // Close dropdown on click outside
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const unreadCount = notifications.filter(n => n.status === 'UNREAD').length;

  const markAllRead = () => {
    setNotifications(prev => prev.map(n => ({ ...n, status: 'READ' })));
  };

  const markAsRead = (id: string, e: React.MouseEvent) => {
    e.stopPropagation();
    setNotifications(prev => prev.map(n => n.id === id ? { ...n, status: 'READ' } : n));
  };

  const getCategoryIcon = (category: string) => {
    switch (category) {
      case 'BOOKING': return <Calendar className="w-4 h-4 text-indigo-400" />;
      case 'PAYMENT': return <DollarSign className="w-4 h-4 text-emerald-400" />;
      case 'SECURITY': return <ShieldAlert className="w-4 h-4 text-rose-400 animate-pulse" />;
      case 'TRUST': return <Check className="w-4 h-4 text-purple-400" />;
      default: return <MessageSquare className="w-4 h-4 text-sky-400" />;
    }
  };

  return (
    <div className="relative" ref={dropdownRef}>
      {/* Trigger Bell Button */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="relative p-2.5 rounded-xl bg-slate-900 border border-slate-800 hover:border-slate-700 hover:bg-slate-850 transition-all text-slate-400 hover:text-white"
        aria-label="Notifications Dropdown"
      >
        <Bell className="w-4 h-4" />
        {unreadCount > 0 && (
          <span className="absolute -top-1 -right-1 w-5 h-5 bg-rose-500 text-white rounded-full flex items-center justify-center text-[10px] font-bold border-2 border-[#090d16] animate-bounce">
            {unreadCount}
          </span>
        )}
      </button>

      {/* Dropdown panel */}
      <AnimatePresence>
        {isOpen && (
          <motion.div
            initial={{ opacity: 0, y: 10, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 10, scale: 0.95 }}
            transition={{ duration: 0.2 }}
            className="absolute right-0 mt-3 w-80 sm:w-96 bg-slate-950 border border-slate-900 shadow-2xl rounded-2xl overflow-hidden z-50 text-xs"
          >
            {/* Header info */}
            <div className="p-4 border-b border-slate-900 bg-slate-900/30 flex justify-between items-center">
              <div>
                <h3 className="font-extrabold text-slate-200 text-sm">Alert Notifications</h3>
                {/* SSE Status */}
                <div className="flex items-center gap-1.5 mt-1 text-[10px] text-slate-500">
                  {isLive ? (
                    <>
                      <Wifi className="w-3 h-3 text-emerald-400" />
                      <span>Live connection active</span>
                    </>
                  ) : (
                    <>
                      <WifiOff className="w-3 h-3 text-rose-400" />
                      <span>Disconnected</span>
                    </>
                  )}
                </div>
              </div>
              <div className="flex items-center gap-2">
                {unreadCount > 0 && (
                  <button
                    onClick={markAllRead}
                    className="text-xs text-primary font-bold hover:underline"
                  >
                    Mark read
                  </button>
                )}
                <button
                  onClick={() => {
                    setIsOpen(false);
                    navigate('/settings/preferences');
                  }}
                  className="p-1 rounded bg-slate-900 border border-slate-800 text-slate-400 hover:text-white"
                  title="Notification settings"
                >
                  <Settings className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>

            {/* List */}
            <div className="max-h-72 overflow-y-auto divide-y divide-slate-900/50">
              {notifications.length === 0 ? (
                <div className="p-8 text-center text-slate-500">
                  No new notifications.
                </div>
              ) : (
                notifications.map((notif) => (
                  <div
                    key={notif.id}
                    className={`p-4 flex gap-3 cursor-pointer hover:bg-slate-900/40 transition-colors ${
                      notif.status === 'UNREAD' ? 'bg-indigo-950/5 border-l-2 border-l-primary' : ''
                    }`}
                    onClick={() => {
                      setIsOpen(false);
                      navigate('/notifications');
                    }}
                  >
                    <div className="p-2 bg-slate-900 border border-slate-800 rounded-xl shrink-0 h-fit mt-0.5">
                      {getCategoryIcon(notif.category)}
                    </div>
                    <div className="space-y-1 flex-1">
                      <div className="flex justify-between items-start">
                        <span className="font-bold text-slate-200">{notif.title}</span>
                        {notif.status === 'UNREAD' && (
                          <button
                            onClick={(e) => markAsRead(notif.id, e)}
                            className="p-0.5 rounded-full hover:bg-slate-900 border border-transparent hover:border-slate-800 text-slate-400 hover:text-emerald-400"
                            title="Mark as read"
                          >
                            <Check className="w-3 h-3" />
                          </button>
                        )}
                      </div>
                      <p className="text-slate-400 leading-relaxed">{notif.message}</p>
                      <span className="text-[9px] text-slate-550 block font-semibold uppercase">
                        {new Date(notif.createdAt).toLocaleDateString('en-IN', {
                          day: 'numeric', month: 'short'
                        })} · {notif.channel} alert
                      </span>
                    </div>
                  </div>
                ))
              )}
            </div>

            {/* Footer */}
            <button
              onClick={() => {
                setIsOpen(false);
                navigate('/notifications');
              }}
              className="w-full text-center py-3 border-t border-slate-900 bg-slate-900/40 hover:bg-slate-900 transition-colors font-bold text-slate-300 hover:text-white"
            >
              View Full History
            </button>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
