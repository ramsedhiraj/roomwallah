import React, { useState, useRef, useEffect } from 'react';
import { MessageSquare, X, Send, Sparkles, User, Brain, AlertCircle, Trash2, CheckCircle2, History, Languages } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { apiClient } from '../services/api';
import { getTranslation, Locale } from '../utils/i18n';

interface Message {
  id: string;
  sender: 'ai' | 'user';
  text: string;
  timestamp: Date;
}

interface ChatSession {
  id: string;
  title: string;
  summaryFlag: string;
  hasSummary: boolean;
  messages: Message[];
}

const INITIAL_SESSIONS: ChatSession[] = [
  {
    id: 'session-1',
    title: 'Noida Flat Hunt',
    summaryFlag: 'Noida, Balcony, <20k',
    hasSummary: true,
    messages: [
      { id: 'm1', sender: 'ai', text: 'Hello! Welcome back to Noida Flat Hunt.', timestamp: new Date(Date.now() - 3600000) },
      { id: 'm2', sender: 'user', text: 'Show me flats under 20k with balcony', timestamp: new Date(Date.now() - 3500000) },
      { id: 'm3', sender: 'ai', text: 'I found 2 BHK apartments in Sector 62 matching under 18k.', timestamp: new Date(Date.now() - 3400000) },
    ]
  },
  {
    id: 'session-2',
    title: 'Bangalore Studio Search',
    summaryFlag: 'Bangalore, Gym, Tech Hub',
    hasSummary: true,
    messages: [
      { id: 'm4', sender: 'ai', text: 'Hi, let us search for studios in Bangalore.', timestamp: new Date(Date.now() - 7200000) },
      { id: 'm5', sender: 'user', text: 'Need a gym nearby', timestamp: new Date(Date.now() - 7100000) },
    ]
  }
];

interface AiPropertyAssistantProps {
  isPage?: boolean;
}

export default function AiPropertyAssistant({ isPage = false }: AiPropertyAssistantProps) {
  const [locale, setLocale] = useState<Locale>('en-IN');
  const [isOpen, setIsOpen] = useState(isPage);
  const [sessions, setSessions] = useState<ChatSession[]>(INITIAL_SESSIONS);
  const [activeSessionId, setActiveSessionId] = useState<string>('session-1');
  const [inputText, setInputText] = useState('');
  const [isTyping, setIsTyping] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const activeSession = sessions.find(s => s.id === activeSessionId) || sessions[0] || {
    id: 'temp',
    title: 'New Session',
    summaryFlag: 'No criteria yet',
    hasSummary: false,
    messages: []
  };

  // Auto-scroll to bottom of messages
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [sessions, activeSessionId, isTyping]);

  const handleSendMessage = async (textToSend: string) => {
    if (!textToSend.trim()) return;

    const userMessage: Message = {
      id: `user-${Date.now()}`,
      sender: 'user',
      text: textToSend,
      timestamp: new Date(),
    };

    // Update session messages locally
    setSessions(prev => prev.map(session => {
      if (session.id === activeSessionId) {
        // dynamically update summary flag based on inputs
        let summary = session.summaryFlag;
        if (textToSend.toLowerCase().includes('noida')) summary = 'Noida sector query';
        if (textToSend.toLowerCase().includes('rent')) summary = 'Rent index analysis';

        return {
          ...session,
          summaryFlag: summary,
          messages: [...session.messages, userMessage],
        };
      }
      return session;
    }));

    setInputText('');
    setIsTyping(true);

    try {
      const response = await apiClient.post('/ai/chat', {
        message: textToSend,
        sessionId: activeSessionId,
      });
      if (response.data?.reply) {
        setSessions(prev => prev.map(session => {
          if (session.id === activeSessionId) {
            return {
              ...session,
              messages: [
                ...session.messages,
                {
                  id: `ai-${Date.now()}`,
                  sender: 'ai',
                  text: response.data.reply,
                  timestamp: new Date(),
                }
              ]
            };
          }
          return session;
        }));
        setIsTyping(false);
        return;
      }
    } catch (err) {
      console.warn('Chat API failed. Replying locally.');
    }

    setTimeout(() => {
      let replyText = 'I am here to help you find broker-free apartments. Could you specify which city or budget you have in mind?';
      const cleanText = textToSend.toLowerCase();

      if (cleanText.includes('rent') || cleanText.includes('price')) {
        replyText = 'The average rent in Bangalore Indiranagar is ₹18,000 to ₹35,000/mo for 2 BHK flats. In Noida, it ranges around ₹12,000 to ₹22,000/mo.';
      } else if (cleanText.includes('visit') || cleanText.includes('book')) {
        replyText = 'I can help you coordinate a visit! Simply browse the listing, click "Book Visit", and select your preferred date. I will notify the owner instantly.';
      } else if (cleanText.includes('hello') || cleanText.includes('hi')) {
        replyText = 'Hello there! How can I assist you with your rental search or property listing management today?';
      }

      setSessions(prev => prev.map(session => {
        if (session.id === activeSessionId) {
          return {
            ...session,
            messages: [
              ...session.messages,
              {
                id: `ai-${Date.now()}`,
                sender: 'ai',
                text: replyText,
                timestamp: new Date(),
              }
            ]
          };
        }
        return session;
      }));
      setIsTyping(false);
    }, 1000);
  };

  const handleFormSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    handleSendMessage(inputText);
  };

  // Deletion requests: Delete single message
  const handleDeleteMessage = (messageId: string) => {
    setSessions(prev => prev.map(session => {
      if (session.id === activeSessionId) {
        return {
          ...session,
          messages: session.messages.filter(msg => msg.id !== messageId),
        };
      }
      return session;
    }));
  };

  // Deletion requests: Delete entire session
  const handleDeleteSession = (sessionId: string) => {
    const remaining = sessions.filter(s => s.id !== sessionId);
    setSessions(remaining);
    if (remaining.length > 0) {
      setActiveSessionId(remaining[0].id);
    }
  };

  // Deletion requests: Clear active session messages
  const handleClearActiveHistory = () => {
    setSessions(prev => prev.map(session => {
      if (session.id === activeSessionId) {
        return {
          ...session,
          messages: [],
        };
      }
      return session;
    }));
  };

  const handleCreateNewSession = () => {
    const newId = `session-${Date.now()}`;
    const newSession: ChatSession = {
      id: newId,
      title: `Session ${sessions.length + 1}`,
      summaryFlag: 'New criteria',
      hasSummary: true,
      messages: [
        { id: `ai-${Date.now()}`, sender: 'ai', text: 'Hello! Let us start a fresh property query session.', timestamp: new Date() }
      ]
    };
    setSessions([...sessions, newSession]);
    setActiveSessionId(newId);
  };

  const t = (key: Parameters<typeof getTranslation>[1]) => getTranslation(locale, key);

  const renderLanguageDropdown = () => (
    <div className="flex items-center space-x-1 bg-slate-900 border border-slate-800 rounded-lg px-2 py-1">
      <Languages className="w-3.5 h-3.5 text-indigo-400" />
      <select
        value={locale}
        onChange={(e) => setLocale(e.target.value as Locale)}
        className="bg-transparent border-none text-[10px] text-slate-300 font-semibold focus:ring-0 focus:outline-none cursor-pointer py-0.5"
      >
        <option value="en-IN" className="bg-slate-950">EN</option>
        <option value="hi-IN" className="bg-slate-950">HI</option>
        <option value="mr-IN" className="bg-slate-950">MR</option>
      </select>
    </div>
  );

  const renderSessionsList = () => (
    <div className="border-r border-slate-850 w-48 shrink-0 flex flex-col justify-between bg-slate-950/40">
      <div className="p-3 border-b border-slate-850 flex justify-between items-center">
        <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider flex items-center gap-1">
          <History className="w-3.5 h-3.5 text-indigo-400" />
          <span>Sessions</span>
        </span>
        <button
          onClick={handleCreateNewSession}
          className="text-[10px] bg-indigo-600/10 hover:bg-indigo-650/25 border border-indigo-500/20 text-indigo-400 px-1.5 py-0.5 rounded font-bold"
        >
          + New
        </button>
      </div>

      <div className="flex-1 overflow-y-auto p-2 space-y-1">
        {sessions.map(s => (
          <div
            key={s.id}
            onClick={() => setActiveSessionId(s.id)}
            className={`group p-2.5 rounded-lg text-left cursor-pointer transition-all relative border ${
              activeSessionId === s.id
                ? 'bg-slate-900 border-indigo-500/50 text-white'
                : 'border-transparent text-slate-400 hover:bg-slate-900/60 hover:text-slate-200'
            }`}
          >
            <div className="font-semibold text-xs truncate pr-4">{s.title}</div>
            
            {/* History summary flags */}
            {s.hasSummary && (
              <span className="text-[8px] bg-indigo-500/10 text-indigo-400 px-1.5 py-0.5 rounded border border-indigo-500/20 mt-1 inline-block truncate max-w-full">
                {s.summaryFlag}
              </span>
            )}

            {/* Session deletion */}
            <button
              onClick={(e) => {
                e.stopPropagation();
                handleDeleteSession(s.id);
              }}
              className="absolute right-2 top-2.5 opacity-0 group-hover:opacity-100 p-0.5 text-slate-500 hover:text-red-400 rounded transition-opacity"
              title="Delete session"
            >
              <Trash2 className="w-3 h-3" />
            </button>
          </div>
        ))}
      </div>

      {sessions.length > 0 && (
        <div className="p-2 border-t border-slate-850">
          <button
            onClick={handleClearActiveHistory}
            className="w-full flex items-center justify-center space-x-1 py-1.5 bg-slate-900 hover:bg-slate-850 hover:text-red-400 border border-slate-800 rounded text-[10px] text-slate-400 transition-colors font-semibold"
          >
            <Trash2 className="w-3 h-3" />
            <span>{t('clearHistory')}</span>
          </button>
        </div>
      )}
    </div>
  );

  // If this is rendered as a standalone page route
  if (isPage) {
    return (
      <div className="max-w-5xl mx-auto px-4 py-8 text-slate-100 min-h-[75vh] flex flex-col">
        <div className="bg-slate-900 border border-slate-800 rounded-2xl flex flex-col flex-grow overflow-hidden shadow-2xl h-[650px]">
          {/* Top Panel Header */}
          <div className="bg-slate-950 px-6 py-4 border-b border-slate-850 flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <div className="p-2 bg-indigo-600/10 text-indigo-400 rounded-lg">
                <Brain className="w-5 h-5 animate-pulse-subtle" />
              </div>
              <div>
                <h1 className="text-base font-bold text-white leading-tight">{t('assistantTitle')}</h1>
                <span className="text-[10px] text-emerald-400 font-semibold flex items-center gap-1 mt-0.5">
                  <span className="h-1.5 w-1.5 rounded-full bg-emerald-500"></span> Online
                </span>
              </div>
            </div>
            
            <div className="flex items-center space-x-2">
              {renderLanguageDropdown()}
              <span className="text-xs text-slate-500">Full-screen Mode</span>
            </div>
          </div>

          <div className="flex flex-1 overflow-hidden">
            {/* Sidebar list sessions */}
            {renderSessionsList()}

            {/* Chat message flow */}
            <div className="flex-1 flex flex-col justify-between overflow-hidden bg-slate-900/40">
              <div className="flex-1 overflow-y-auto p-6 space-y-4">
                {activeSession.messages.length === 0 ? (
                  <div className="text-center py-20 text-slate-500 text-xs italic">
                    No conversation history. Send a query below.
                  </div>
                ) : (
                  activeSession.messages.map((msg) => (
                    <div
                      key={msg.id}
                      className={`flex gap-3 max-w-[80%] group ${msg.sender === 'user' ? 'ml-auto flex-row-reverse' : ''}`}
                    >
                      <div
                        className={`w-8 h-8 rounded-full flex items-center justify-center shrink-0 text-white ${
                          msg.sender === 'ai' ? 'bg-indigo-600' : 'bg-slate-700'
                        }`}
                      >
                        {msg.sender === 'ai' ? <Brain className="w-4.5 h-4.5" /> : <User className="w-4.5 h-4.5" />}
                      </div>

                      <div className="relative">
                        <div
                          className={`rounded-2xl p-4 text-sm leading-relaxed ${
                            msg.sender === 'ai'
                              ? 'bg-slate-950 border border-slate-850 text-slate-100'
                              : 'bg-indigo-600 text-white'
                          }`}
                        >
                          <p>{msg.text}</p>
                        </div>

                        {/* Individual message deletion support */}
                        <button
                          onClick={() => handleDeleteMessage(msg.id)}
                          className="absolute -top-1 -right-2 bg-slate-950 hover:bg-slate-850 border border-slate-800 text-slate-400 hover:text-red-400 p-1 rounded-full opacity-0 group-hover:opacity-100 transition-opacity shadow"
                          title="Delete message"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </div>
                  ))
                )}

                {isTyping && (
                  <div className="flex gap-3 max-w-[80%]">
                    <div className="w-8 h-8 rounded-full bg-indigo-600 flex items-center justify-center shrink-0 text-white">
                      <Brain className="w-4.5 h-4.5" />
                    </div>
                    <div className="bg-slate-950 border border-slate-850 text-slate-400 rounded-2xl px-4 py-3 text-sm flex items-center space-x-1">
                      <span className="h-2 w-2 bg-slate-500 rounded-full animate-bounce" style={{ animationDelay: '0ms' }}></span>
                      <span className="h-2 w-2 bg-slate-500 rounded-full animate-bounce" style={{ animationDelay: '150ms' }}></span>
                      <span className="h-2 w-2 bg-slate-500 rounded-full animate-bounce" style={{ animationDelay: '300ms' }}></span>
                    </div>
                  </div>
                )}
                <div ref={messagesEndRef} />
              </div>

              {/* Quick Prompt Chips */}
              <div className="px-6 py-2.5 border-t border-slate-850 bg-slate-950/40 flex flex-wrap gap-1.5">
                {[
                  'Show average rent in Noida Sector 62',
                  'How can I book a visit calendar slot?',
                  'Explain trust score badges',
                ].map((chip) => (
                  <button
                    key={chip}
                    onClick={() => handleSendMessage(chip)}
                    className="px-3 py-1 rounded-full bg-slate-950 border border-slate-800 hover:border-slate-700 hover:text-white transition-colors text-[10px] text-slate-450"
                  >
                    {chip}
                  </button>
                ))}
              </div>

              {/* Text Form Input */}
              <div className="p-4 bg-slate-950 border-t border-slate-850">
                <form onSubmit={handleFormSubmit} className="flex gap-2">
                  <input
                    type="text"
                    value={inputText}
                    onChange={(e) => setInputText(e.target.value)}
                    placeholder="Ask RoomWallah AI..."
                    className="flex-1 bg-slate-900 border border-slate-850 rounded-xl px-4 py-2.5 text-slate-100 placeholder-slate-555 focus:outline-none focus:border-indigo-500 text-sm"
                  />
                  <button
                    type="submit"
                    disabled={!inputText.trim()}
                    className="bg-indigo-600 hover:bg-indigo-500 text-white px-4.5 py-2.5 rounded-xl transition-colors disabled:opacity-50 flex items-center space-x-1"
                  >
                    <span>{t('send')}</span>
                    <Send className="w-3.5 h-3.5" />
                  </button>
                </form>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // Floating Drawer Version
  return (
    <>
      {/* Floating Trigger Button */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="fixed bottom-6 right-6 z-50 p-4 bg-gradient-to-tr from-indigo-600 to-indigo-500 hover:opacity-95 text-white rounded-full shadow-2xl transition-all duration-300 hover:scale-105 flex items-center justify-center border border-indigo-500/35 glow-indigo"
        title="Open AI Property Assistant"
        data-testid="ai-assistant-toggle"
      >
        <MessageSquare className="w-6 h-6" />
      </button>

      {/* Slide-out Drawer */}
      <AnimatePresence>
        {isOpen && (
          <motion.div
            initial={{ x: '100%' }}
            animate={{ x: 0 }}
            exit={{ x: '100%' }}
            transition={{ type: 'spring', damping: 25, stiffness: 220 }}
            className="fixed top-0 right-0 w-full sm:w-[520px] h-full bg-slate-950 border-l border-slate-850 z-50 shadow-2xl flex flex-col text-slate-200"
            data-testid="ai-chat-drawer"
          >
            {/* Header */}
            <div className="p-4 border-b border-slate-850 flex items-center justify-between bg-slate-900/60">
              <div className="flex items-center space-x-2">
                <Sparkles className="w-5 h-5 text-indigo-400 animate-pulse-subtle" />
                <div>
                  <h2 className="text-sm font-bold text-white">{t('assistantTitle')}</h2>
                  <span className="text-[10px] text-emerald-450 font-semibold block mt-0.5">Online Helper</span>
                </div>
              </div>

              <div className="flex items-center space-x-2">
                {renderLanguageDropdown()}
                <button
                  onClick={() => setIsOpen(false)}
                  className="p-1.5 bg-slate-900 hover:bg-slate-800 text-slate-400 hover:text-white rounded-lg transition-colors"
                  data-testid="ai-chat-close-btn"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
            </div>

            {/* Split Screen drawer body */}
            <div className="flex flex-1 overflow-hidden">
              {renderSessionsList()}

              {/* Message log panel */}
              <div className="flex-grow flex flex-col justify-between overflow-hidden">
                <div className="flex-1 overflow-y-auto p-4 space-y-4">
                  {activeSession.messages.length === 0 ? (
                    <div className="text-center py-20 text-slate-500 text-xs italic">
                      No conversation history. Send a query below.
                    </div>
                  ) : (
                    activeSession.messages.map((msg) => (
                      <div
                        key={msg.id}
                        className={`flex gap-2 max-w-[85%] group ${msg.sender === 'user' ? 'ml-auto flex-row-reverse' : ''}`}
                      >
                        <div
                          className={`w-7 h-7 rounded-full flex items-center justify-center shrink-0 text-white text-[10px] ${
                            msg.sender === 'ai' ? 'bg-indigo-600' : 'bg-slate-700'
                          }`}
                        >
                          {msg.sender === 'ai' ? <Brain className="w-3.5 h-3.5" /> : <User className="w-3.5 h-3.5" />}
                        </div>

                        <div className="relative">
                          <div
                            className={`rounded-2xl p-3 text-xs leading-relaxed ${
                              msg.sender === 'ai'
                                ? 'bg-slate-900 border border-slate-850 text-slate-100'
                                : 'bg-indigo-600 text-white'
                            }`}
                          >
                            <p>{msg.text}</p>
                          </div>

                          <button
                            onClick={() => handleDeleteMessage(msg.id)}
                            className="absolute -top-1 -right-2 bg-slate-950 hover:bg-slate-850 border border-slate-800 text-slate-400 hover:text-red-400 p-0.5 rounded-full opacity-0 group-hover:opacity-100 transition-opacity shadow"
                            title="Delete message"
                          >
                            <Trash2 className="w-3 h-3" />
                          </button>
                        </div>
                      </div>
                    ))
                  )}

                  {isTyping && (
                    <div className="flex gap-2 max-w-[85%]">
                      <div className="w-7 h-7 rounded-full bg-indigo-600 flex items-center justify-center shrink-0 text-white">
                        <Brain className="w-3.5 h-3.5" />
                      </div>
                      <div className="bg-slate-900 border border-slate-850 text-slate-450 rounded-2xl px-3 py-2 text-xs flex items-center space-x-1">
                        <span className="h-1.5 w-1.5 bg-slate-500 rounded-full animate-bounce" style={{ animationDelay: '0ms' }}></span>
                        <span className="h-1.5 w-1.5 bg-slate-500 rounded-full animate-bounce" style={{ animationDelay: '150ms' }}></span>
                        <span className="h-1.5 w-1.5 bg-slate-500 rounded-full animate-bounce" style={{ animationDelay: '300ms' }}></span>
                      </div>
                    </div>
                  )}
                  <div ref={messagesEndRef} />
                </div>

                {/* Input panel drawer */}
                <div className="p-3 bg-slate-900 border-t border-slate-850">
                  <form onSubmit={handleFormSubmit} className="flex gap-1.5">
                    <input
                      type="text"
                      value={inputText}
                      onChange={(e) => setInputText(e.target.value)}
                      placeholder="Ask RoomWallah AI..."
                      className="flex-grow bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-slate-100 placeholder-slate-500 focus:outline-none focus:border-indigo-500 text-xs"
                      data-testid="ai-chat-input"
                    />
                    <button
                      type="submit"
                      disabled={!inputText.trim()}
                      className="bg-indigo-600 hover:bg-indigo-500 text-white p-2 rounded-lg transition-colors disabled:opacity-50"
                      data-testid="ai-chat-send-btn"
                    >
                      <Send className="w-3.5 h-3.5" />
                    </button>
                  </form>
                </div>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
}
