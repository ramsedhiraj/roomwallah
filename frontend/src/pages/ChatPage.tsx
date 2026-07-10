import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { chatService, ConversationResponse, MessageResponse } from '../services/chatService';
import { useAuthStore } from '../store/authStore';
import { MessageSquare, Send, CheckCheck, User, ArrowLeft, Loader2 } from 'lucide-react';

export default function ChatPage() {
  const { id: routeId } = useParams<{ id?: string }>();
  const navigate = useNavigate();
  const currentUser = useAuthStore((state) => state.user);

  const [conversations, setConversations] = useState<ConversationResponse[]>([]);
  const [activeConversation, setActiveConversation] = useState<ConversationResponse | null>(null);
  const [messages, setMessages] = useState<MessageResponse[]>([]);
  const [newMessageText, setNewMessageText] = useState('');
  const [loadingConversations, setLoadingConversations] = useState(true);
  const [loadingMessages, setLoadingMessages] = useState(false);
  const [sendingMessage, setSendingMessage] = useState(false);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const messagePollingRef = useRef<NodeJS.Timeout | null>(null);

  // 1. Fetch conversations on mount
  useEffect(() => {
    fetchConversations();
    return () => {
      if (messagePollingRef.current) clearInterval(messagePollingRef.current);
    };
  }, []);

  // 2. Handle active conversation switching
  useEffect(() => {
    if (activeConversation) {
      fetchMessages(activeConversation.id);
      chatService.markAsRead(activeConversation.id).then(() => {
        // Update unread count locally in conversation list
        setConversations(prev =>
          prev.map(c => c.id === activeConversation.id ? { ...c, unreadCount: 0 } : c)
        );
      });

      // Poll messages every 5 seconds for simulated real-time experience
      if (messagePollingRef.current) clearInterval(messagePollingRef.current);
      messagePollingRef.current = setInterval(() => {
        pollMessages(activeConversation.id);
      }, 5000);
    } else {
      if (messagePollingRef.current) {
        clearInterval(messagePollingRef.current);
        messagePollingRef.current = null;
      }
      setMessages([]);
    }
  }, [activeConversation]);

  // 3. Select default conversation if routeId matches
  useEffect(() => {
    if (routeId && conversations.length > 0) {
      const selected = conversations.find(c => c.id === routeId || c.bookingId === routeId);
      if (selected) {
        setActiveConversation(selected);
      }
    }
  }, [routeId, conversations]);

  const fetchConversations = async () => {
    try {
      setLoadingConversations(true);
      const data = await chatService.getConversations();
      setConversations(data);
      if (routeId) {
        const selected = data.find(c => c.id === routeId || c.bookingId === routeId);
        if (selected) {
          setActiveConversation(selected);
        }
      } else if (data.length > 0 && !activeConversation) {
        // Select first conversation by default on desktop
        setActiveConversation(data[0]);
      }
    } catch (err) {
      console.error('Failed to load conversations:', err);
    } finally {
      setLoadingConversations(false);
    }
  };

  const fetchMessages = async (convId: string) => {
    try {
      setLoadingMessages(true);
      const data = await chatService.getMessages(convId);
      setMessages(data);
      scrollToBottom();
    } catch (err) {
      console.error('Failed to load messages:', err);
    } finally {
      setLoadingMessages(false);
    }
  };

  const pollMessages = async (convId: string) => {
    try {
      const data = await chatService.getMessages(convId);
      // Only update if message count changes or message contents differ to avoid flickering
      setMessages(prev => {
        if (prev.length !== data.length || JSON.stringify(prev) !== JSON.stringify(data)) {
          setTimeout(scrollToBottom, 50);
          return data;
        }
        return prev;
      });
    } catch (err) {
      console.error('Failed to poll messages:', err);
    }
  };

  const handleSend = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeConversation || !newMessageText.trim() || sendingMessage) return;

    const textToSend = newMessageText.trim();
    setNewMessageText('');
    setSendingMessage(true);

    try {
      const sentMsg = await chatService.sendMessage(activeConversation.id, textToSend);
      setMessages(prev => [...prev, sentMsg]);
      scrollToBottom();

      // Update latest message in conversations list
      setConversations(prev =>
        prev.map(c => c.id === activeConversation.id
          ? { ...c, latestMessage: textToSend, latestMessageTime: new Date().toISOString() }
          : c
        )
      );
    } catch (err) {
      console.error('Failed to send message:', err);
      // Restore input text on error
      setNewMessageText(textToSend);
      alert('Failed to send message. Please try again.');
    } finally {
      setSendingMessage(false);
    }
  };

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const getOtherParticipantName = (conv: ConversationResponse) => {
    if (!currentUser) return '';
    return currentUser.role === 'OWNER' ? conv.tenantName : conv.ownerName;
  };

  const getOtherParticipantRole = () => {
    if (!currentUser) return '';
    return currentUser.role === 'OWNER' ? 'Tenant' : 'Property Owner';
  };

  const formatMessageTime = (isoString: string) => {
    return new Date(isoString).toLocaleTimeString(undefined, {
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const formatConversationTime = (isoString: string) => {
    const date = new Date(isoString);
    const today = new Date();
    if (date.toDateString() === today.toDateString()) {
      return date.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' });
    }
    return date.toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
  };

  return (
    <div className="max-w-7xl mx-auto px-4 py-8 h-[calc(100vh-140px)] min-h-[500px] flex flex-col">
      <div className="bg-card border border-border rounded-2xl overflow-hidden shadow-xl flex-1 flex flex-col md:flex-row">
        
        {/* Left Sidebar: Conversation List */}
        <div className={`w-full md:w-80 lg:w-96 border-r border-border flex flex-col ${activeConversation && 'hidden md:flex'}`}>
          <div className="p-4 border-b border-border bg-slate-900/10 flex items-center justify-between">
            <h1 className="text-xl font-bold tracking-tight flex items-center gap-2">
              <MessageSquare className="h-5 w-5 text-primary" />
              Inbox Messages
            </h1>
          </div>

          <div className="flex-1 overflow-y-auto divide-y divide-border">
            {loadingConversations ? (
              <div className="flex flex-col items-center justify-center py-20 text-muted-foreground gap-3">
                <Loader2 className="h-6 w-6 animate-spin text-primary" />
                <span>Loading inbox...</span>
              </div>
            ) : conversations.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-20 text-center px-4">
                <p className="text-sm text-muted-foreground">Your inbox is empty.</p>
                <p className="text-xs text-muted-foreground mt-1">Submit a booking request to start a conversation with a property owner.</p>
              </div>
            ) : (
              conversations.map(conv => {
                const isActive = activeConversation?.id === conv.id;
                const otherName = getOtherParticipantName(conv);
                return (
                  <button
                    key={conv.id}
                    onClick={() => {
                      setActiveConversation(conv);
                      navigate(`/chat/${conv.id}`);
                    }}
                    className={`w-full p-4 text-left flex items-start gap-3 transition-all hover:bg-slate-800/20 ${isActive ? 'bg-primary/10 border-l-4 border-primary' : 'bg-transparent'}`}
                  >
                    <div className="p-2 bg-slate-800 rounded-full shrink-0">
                      <User className="h-4 w-4 text-slate-400" />
                    </div>
                    <div className="flex-1 min-w-0 space-y-1">
                      <div className="flex justify-between items-baseline">
                        <h4 className="font-bold text-sm text-slate-200 truncate">{otherName}</h4>
                        <span className="text-[10px] text-muted-foreground">
                          {formatConversationTime(conv.latestMessageTime)}
                        </span>
                      </div>
                      <p className="text-xs text-muted-foreground truncate">
                        {conv.latestMessage || 'No messages yet'}
                      </p>
                      <div className="flex justify-between items-center pt-1">
                        <span className="text-[9px] text-muted-foreground bg-slate-800 px-1.5 py-0.5 rounded font-mono">
                          Ref: {conv.bookingId.substring(0, 8)}
                        </span>
                        {conv.unreadCount > 0 && (
                          <span className="bg-primary text-white text-[10px] font-extrabold px-1.5 py-0.5 rounded-full shrink-0">
                            {conv.unreadCount}
                          </span>
                        )}
                      </div>
                    </div>
                  </button>
                );
              })
            )}
          </div>
        </div>

        {/* Right Pane: Message Area */}
        <div className={`flex-1 flex flex-col bg-slate-950/20 ${!activeConversation && 'hidden md:flex'}`}>
          {activeConversation ? (
            <>
              {/* Header */}
              <div className="p-4 border-b border-border bg-slate-900/10 flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <button
                    onClick={() => setActiveConversation(null)}
                    className="md:hidden p-1.5 hover:bg-slate-800 rounded-xl"
                  >
                    <ArrowLeft className="h-5 w-5" />
                  </button>
                  <div className="p-2 bg-slate-800 rounded-full">
                    <User className="h-5 w-5 text-slate-400" />
                  </div>
                  <div>
                    <h3 className="font-bold text-sm text-slate-200">{getOtherParticipantName(activeConversation)}</h3>
                    <p className="text-[10px] text-muted-foreground">{getOtherParticipantRole()}</p>
                  </div>
                </div>
                <div className="text-[10px] text-muted-foreground border border-border bg-card/50 px-2.5 py-1 rounded-xl">
                  Booking ID: {activeConversation.bookingId.substring(0, 8)}
                </div>
              </div>

              {/* Message List */}
              <div className="flex-1 overflow-y-auto p-4 space-y-4">
                {loadingMessages && messages.length === 0 ? (
                  <div className="flex justify-center items-center py-20">
                    <Loader2 className="h-6 w-6 animate-spin text-primary" />
                  </div>
                ) : (
                  messages.map(msg => {
                    const isMe = msg.senderId === currentUser?.id;
                    return (
                      <div
                        key={msg.id}
                        className={`flex ${isMe ? 'justify-end' : 'justify-start'}`}
                      >
                        <div
                          className={`max-w-[75%] rounded-2xl px-4 py-2.5 shadow ${
                            isMe
                              ? 'bg-gradient-to-r from-primary to-secondary text-white rounded-tr-none'
                              : 'bg-card border border-border text-slate-200 rounded-tl-none'
                          }`}
                        >
                          <p className="text-xs whitespace-pre-wrap leading-relaxed">{msg.content}</p>
                          <div className="flex items-center justify-end gap-1 mt-1 text-[8px] text-slate-300">
                            <span>{formatMessageTime(msg.createdAt)}</span>
                            {isMe && (
                              <CheckCheck className={`w-3.5 h-3.5 ${msg.read ? 'text-sky-300' : 'text-slate-400'}`} />
                            )}
                          </div>
                        </div>
                      </div>
                    );
                  })
                )}
                <div ref={messagesEndRef} />
              </div>

              {/* Message Input Form */}
              <form onSubmit={handleSend} className="p-4 border-t border-border bg-slate-900/10 flex items-center gap-3">
                <textarea
                  value={newMessageText}
                  onChange={(e) => setNewMessageText(e.target.value)}
                  placeholder="Type your message..."
                  rows={1}
                  required
                  className="flex-1 p-3 rounded-xl border border-border bg-background text-xs focus:outline-none focus:ring-1 focus:ring-primary resize-none min-h-[42px] max-h-[80px]"
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' && !e.shiftKey) {
                      e.preventDefault();
                      handleSend(e);
                    }
                  }}
                />
                <button
                  type="submit"
                  disabled={!newMessageText.trim() || sendingMessage}
                  className="p-3 bg-primary text-white rounded-xl hover:bg-primary/90 transition-all disabled:opacity-50 disabled:hover:bg-primary"
                  aria-label="Send Message"
                >
                  {sendingMessage ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    <Send className="h-4 w-4" />
                  )}
                </button>
              </form>
            </>
          ) : (
            <div className="flex-1 flex flex-col items-center justify-center py-20 text-center text-muted-foreground gap-4">
              <MessageSquare className="h-12 w-12 text-slate-700 animate-pulse" />
              <div>
                <h3 className="font-extrabold text-slate-350 text-sm">Select a Conversation</h3>
                <p className="text-xs mt-1 px-4">Choose a contact in the list to review transaction messages and chat history.</p>
              </div>
            </div>
          )}
        </div>

      </div>
    </div>
  );
}
