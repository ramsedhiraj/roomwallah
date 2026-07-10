import React, { Component, ErrorInfo, ReactNode } from 'react';
import { ShieldAlert, RefreshCw, Home } from 'lucide-react';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export default class ErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false,
    error: null
  };

  public static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('ErrorBoundary caught an unhandled error:', error, errorInfo);
  }

  private handleReload = () => {
    window.location.reload();
  };

  private handleGoHome = () => {
    window.location.href = '/';
  };

  public render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen flex items-center justify-center bg-[#090d16] text-[#f1f5f9] px-4 py-12">
          {/* Ambient Glow */}
          <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-primary/10 rounded-full blur-[120px] pointer-events-none"></div>
          <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-secondary/15 rounded-full blur-[120px] pointer-events-none"></div>

          <div className="relative max-w-lg w-full glass border border-slate-800 rounded-3xl p-8 md:p-10 shadow-2xl text-center backdrop-blur-md">
            {/* Icon Banner */}
            <div className="mx-auto flex items-center justify-center w-16 h-16 rounded-2xl bg-rose-500/10 border border-rose-500/20 text-rose-500 mb-6 animate-pulse">
              <ShieldAlert className="w-8 h-8" />
            </div>

            {/* Error Message */}
            <h1 className="text-3xl font-extrabold tracking-tight bg-gradient-to-r from-white via-slate-200 to-slate-400 bg-clip-text text-transparent mb-4">
              Something went wrong
            </h1>
            <p className="text-slate-400 text-sm leading-relaxed mb-6">
              A runtime rendering error occurred in the application. We have logged this issue and our team is investigating.
            </p>

            {this.state.error && (
              <div className="text-left bg-slate-950/80 border border-slate-900 rounded-xl p-4 mb-8 max-h-36 overflow-y-auto font-mono text-xs text-rose-350">
                <p className="font-bold mb-1">{this.state.error.name}: {this.state.error.message}</p>
                <p className="opacity-70 whitespace-pre-wrap">{this.state.error.stack}</p>
              </div>
            )}

            {/* Action Buttons */}
            <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
              <button
                onClick={this.handleReload}
                className="w-full sm:w-auto flex items-center justify-center gap-2 px-6 py-3 rounded-xl bg-gradient-to-r from-primary to-secondary text-white font-semibold hover:opacity-95 shadow-md hover:translate-y-[-1px] active:translate-y-[0px] transition-all"
              >
                <RefreshCw className="w-4 h-4" />
                Reload Page
              </button>
              <button
                onClick={this.handleGoHome}
                className="w-full sm:w-auto flex items-center justify-center gap-2 px-6 py-3 rounded-xl bg-slate-900 border border-slate-800 hover:border-slate-700 hover:bg-slate-850 text-slate-350 hover:text-white font-semibold transition-all"
              >
                <Home className="w-4 h-4" />
                Go to Homepage
              </button>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
