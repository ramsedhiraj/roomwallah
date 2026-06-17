import { useState, useEffect, useRef } from 'react';
import { useParams, Link } from 'react-router-dom';
import { 
  ArrowLeft, 
  UploadCloud, 
  Image as ImageIcon, 
  Video as VideoIcon, 
  FileText as FileIcon, 
  Trash2, 
  Star, 
  AlertCircle, 
  CheckCircle2, 
  Loader2, 
  RefreshCw,
  Copy,
  Check
} from 'lucide-react';
import { apiClient } from '../services/api';
import { Property, PropertyMedia, MediaType } from '../types';

interface UploadQueueItem {
  id: string;
  file: File;
  mediaType: MediaType;
  progress: number;
  status: 'QUEUED' | 'UPLOADING' | 'COMPLETED' | 'FAILED';
  error?: string;
}

export default function PropertyMediaManagerPage() {
  const { id } = useParams<{ id: string }>();
  const [property, setProperty] = useState<Property | null>(null);
  const [mediaList, setMediaList] = useState<PropertyMedia[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [actionLoading, setActionLoading] = useState(false);
  
  const [uploadQueue, setUploadQueue] = useState<UploadQueueItem[]>([]);
  const [draggedIndex, setDraggedIndex] = useState<number | null>(null);
  const [copied, setCopied] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [selectedMediaType, setSelectedMediaType] = useState<MediaType>('IMAGE');

  const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

  const getFullFileUrl = (relativeUrl: string) => {
    const cleanUrl = relativeUrl.startsWith('/api/v1') 
      ? relativeUrl.substring('/api/v1'.length) 
      : relativeUrl;
    return `${API_URL}${cleanUrl}`;
  };

  const fetchPropertyAndMedia = async () => {
    try {
      setLoading(true);
      const [propRes, mediaRes] = await Promise.all([
        apiClient.get(`/properties/${id}`),
        apiClient.get(`/media/properties/${id}`)
      ]);
      setProperty(propRes.data.data);
      setMediaList(mediaRes.data.data || []);
      setError(null);
    } catch (err: any) {
      console.error(err);
      setError(err.response?.data?.message || 'Failed to fetch property details and media');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (id) {
      fetchPropertyAndMedia();
    }
  }, [id]);

  // Keep checking processing status if there are pending items
  useEffect(() => {
    const hasPending = mediaList.some(m => m.processingStatus === 'PENDING' || m.processingStatus === 'PROCESSING');
    if (!hasPending) return;

    const interval = setInterval(async () => {
      try {
        const res = await apiClient.get(`/media/properties/${id}`);
        setMediaList(res.data.data || []);
      } catch (err) {
        console.error('Failed to poll media status', err);
      }
    }, 3000);

    return () => clearInterval(interval);
  }, [mediaList, id]);

  const handleCopyRef = () => {
    if (property) {
      navigator.clipboard.writeText(property.listingRef);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  // Policy Limit Checkers
  const limits: Record<MediaType, { current: number; max: number }> = {
    IMAGE: { current: mediaList.filter(m => m.mediaType === 'IMAGE').length, max: 20 },
    VIDEO: { current: mediaList.filter(m => m.mediaType === 'VIDEO').length, max: 2 },
    FLOOR_PLAN: { current: mediaList.filter(m => m.mediaType === 'FLOOR_PLAN').length, max: 1 },
    VIRTUAL_TOUR: { current: mediaList.filter(m => m.mediaType === 'VIRTUAL_TOUR').length, max: 1 },
    PANORAMA: { current: mediaList.filter(m => m.mediaType === 'PANORAMA').length, max: 2 },
    GLB: { current: mediaList.filter(m => m.mediaType === 'GLB').length, max: 2 },
    GLTF: { current: mediaList.filter(m => m.mediaType === 'GLTF').length, max: 2 },
    USDZ: { current: mediaList.filter(m => m.mediaType === 'USDZ').length, max: 2 },
    OBJ: { current: mediaList.filter(m => m.mediaType === 'OBJ').length, max: 2 }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      addFilesToQueue(Array.from(e.target.files));
    }
  };

  const addFilesToQueue = (files: File[]) => {
    const newItems = files.map(file => {
      const type = selectedMediaType;
      
      // Basic check for limits before adding
      const pendingCountOfThisType = uploadQueue.filter(q => q.mediaType === type && q.status !== 'FAILED').length;
      const totalPlanned = limits[type].current + pendingCountOfThisType;

      if (totalPlanned >= limits[type].max) {
        setError(`Cannot add ${file.name}: Maximum limit of ${limits[type].max} reached for ${type}`);
        return null;
      }

      return {
        id: Math.random().toString(36).substring(7),
        file,
        mediaType: type,
        progress: 0,
        status: 'QUEUED' as const
      };
    }).filter(Boolean) as UploadQueueItem[];

    if (newItems.length > 0) {
      setUploadQueue(prev => [...prev, ...newItems]);
      // Process one by one
      newItems.forEach(item => uploadFile(item));
    }
  };

  const uploadFile = async (item: UploadQueueItem) => {
    const formData = new FormData();
    formData.append('file', item.file);
    formData.append('mediaType', item.mediaType);

    setUploadQueue(prev => prev.map(q => q.id === item.id ? { ...q, status: 'UPLOADING' } : q));

    try {
      const response = await apiClient.post(`/media/properties/${id}`, formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        },
        onUploadProgress: (progressEvent) => {
          const percentCompleted = Math.round((progressEvent.loaded * 100) / (progressEvent.total || 1));
          setUploadQueue(prev => prev.map(q => q.id === item.id ? { ...q, progress: percentCompleted } : q));
        }
      });

      setUploadQueue(prev => prev.map(q => q.id === item.id ? { ...q, status: 'COMPLETED', progress: 100 } : q));
      setSuccess('Media files uploaded successfully');
      
      // Add newly created media item to local state
      if (response.data?.data) {
        setMediaList(prev => [...prev, response.data.data]);
      }
    } catch (err: any) {
      console.error(err);
      const errMsg = err.response?.data?.message || 'Upload failed';
      setUploadQueue(prev => prev.map(q => q.id === item.id ? { ...q, status: 'FAILED', error: errMsg } : q));
      setError(`Failed to upload ${item.file.name}: ${errMsg}`);
    }
  };

  const retryUpload = (item: UploadQueueItem) => {
    // Reset status and progress
    const resetItem = { ...item, status: 'QUEUED' as const, progress: 0, error: undefined };
    setUploadQueue(prev => prev.map(q => q.id === item.id ? resetItem : q));
    uploadFile(resetItem);
  };

  const removeQueueItem = (itemId: string) => {
    setUploadQueue(prev => prev.filter(q => q.id !== itemId));
  };

  // Reorder Handlers (Native HTML5 Drag and Drop)
  const handleDragStart = (e: React.DragEvent, index: number) => {
    setDraggedIndex(index);
    e.dataTransfer.effectAllowed = 'move';
  };

  const handleDragOver = (e: React.DragEvent, index: number) => {
    e.preventDefault();
    if (draggedIndex === null || draggedIndex === index) return;

    const updatedList = [...mediaList];
    const item = updatedList.splice(draggedIndex, 1)[0];
    updatedList.splice(index, 0, item);
    
    setDraggedIndex(index);
    setMediaList(updatedList);
  };

  const handleDragEnd = async () => {
    setDraggedIndex(null);
    try {
      const mediaIds = mediaList.map(m => m.id);
      await apiClient.patch('/media/reorder', {
        propertyId: id,
        mediaIds
      });
      setSuccess('Gallery display order updated');
    } catch (err: any) {
      console.error(err);
      setError(err.response?.data?.message || 'Failed to persist media display order');
      // Re-fetch to align
      fetchPropertyAndMedia();
    }
  };

  // Operations
  const handleSetCover = async (mediaId: string) => {
    try {
      setError(null);
      await apiClient.patch(`/media/${mediaId}/cover`);
      setSuccess('Cover image updated successfully');
      
      // Update local state: unset previous cover and set new one
      setMediaList(prev => prev.map(m => ({
        ...m,
        isCover: m.id === mediaId
      })));
    } catch (err: any) {
      console.error(err);
      setError(err.response?.data?.message || 'Failed to update cover image');
    }
  };

  const handleDeleteMedia = async (mediaId: string) => {
    if (!window.confirm('Are you sure you want to remove this media?')) return;
    
    try {
      setError(null);
      await apiClient.delete(`/media/${mediaId}`);
      setSuccess('Media deleted successfully');
      setMediaList(prev => prev.filter(m => m.id !== mediaId));
    } catch (err: any) {
      console.error(err);
      setError(err.response?.data?.message || 'Failed to delete media');
    }
  };

  const getLimitBarColor = (current: number, max: number) => {
    const percentage = (current / max) * 100;
    if (percentage >= 100) return 'bg-rose-500 shadow-rose-500/20';
    if (percentage >= 75) return 'bg-amber-500 shadow-amber-500/20';
    return 'bg-emerald-500 shadow-emerald-500/20';
  };

  const handleSubmitForReview = async () => {
    if (!window.confirm('Are you sure you want to submit this property for review? It will be locked from further edits until reviewed by an admin.')) return;
    
    setActionLoading(true);
    setError(null);
    try {
      await apiClient.post(`/properties/${id}/submit`);
      setSuccess('Property submitted for review successfully.');
      fetchPropertyAndMedia();
    } catch (err: any) {
      console.error(err);
      setError(err.response?.data?.message || 'Failed to submit property for review.');
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 animate-fade-in text-slate-100">
      {/* Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 mb-8">
        <div className="flex items-center gap-4">
          <Link to="/listings" className="p-3 bg-slate-900 border border-slate-800 rounded-xl hover:border-slate-700 text-slate-400 hover:text-white transition-all shadow-md">
            <ArrowLeft className="w-5 h-5" />
          </Link>
          <div>
            <div className="flex items-center gap-2 flex-wrap">
              <h1 className="text-3xl font-extrabold tracking-tight bg-gradient-to-r from-white via-slate-100 to-indigo-200 bg-clip-text text-transparent">
                Property Media Manager
              </h1>
              {property && (
                <span className="px-3 py-1 rounded-full text-xs font-semibold bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 uppercase">
                  {property.propertyType}
                </span>
              )}
            </div>
            {property && (
              <p className="text-slate-400 mt-1 flex items-center gap-2 flex-wrap">
                <span>{property.title}</span>
                <span className="text-slate-600">|</span>
                <button 
                  onClick={handleCopyRef}
                  className="flex items-center gap-1 px-2 py-0.5 rounded bg-slate-950 border border-slate-900 hover:border-slate-800 text-xs font-mono text-slate-400 hover:text-slate-300 transition-colors"
                >
                  <span>Ref: {property.listingRef}</span>
                  {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                </button>
              </p>
            )}
          </div>
        </div>

        {property && property.status === 'DRAFT' && (
          <button
            onClick={handleSubmitForReview}
            disabled={actionLoading}
            className="flex items-center gap-2 px-6 py-3 rounded-xl bg-gradient-to-r from-emerald-600 to-emerald-500 hover:from-emerald-500 hover:to-emerald-400 text-white font-bold shadow-lg shadow-emerald-500/20 hover:-translate-y-0.5 active:translate-y-0 transition-all disabled:opacity-50"
          >
            {actionLoading ? <Loader2 className="w-5 h-5 animate-spin" /> : <CheckCircle2 className="w-5 h-5" />}
            Submit for Review
          </button>
        )}
      </div>

      {error && (
        <div className="mb-6 p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-sm flex items-center gap-3">
          <AlertCircle className="w-5 h-5 flex-shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {success && (
        <div className="mb-6 p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-sm flex items-center gap-3">
          <CheckCircle2 className="w-5 h-5 flex-shrink-0" />
          <span>{success}</span>
        </div>
      )}

      {loading ? (
        <div className="flex items-center justify-center min-h-[300px]">
          <Loader2 className="w-10 h-10 border-4 border-indigo-500 border-t-transparent rounded-full animate-spin text-primary" />
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Left / Upload side */}
          <div className="lg:col-span-1 space-y-6">
            {/* Limits card */}
            <div className="glass border border-slate-850 p-6 rounded-2xl space-y-4">
              <h3 className="text-lg font-bold text-slate-200">Media Policy Limits</h3>
              <div className="space-y-3.5">
                {/* Images Limit */}
                <div>
                  <div className="flex justify-between text-sm mb-1">
                    <span className="text-slate-400 flex items-center gap-1.5"><ImageIcon className="w-4 h-4" /> Gallery Images</span>
                    <span className="font-semibold text-slate-200">{limits.IMAGE.current} / {limits.IMAGE.max}</span>
                  </div>
                  <div className="w-full bg-slate-950 rounded-full h-2 overflow-hidden border border-slate-900">
                    <div 
                      className={`h-full rounded-full transition-all duration-500 ${getLimitBarColor(limits.IMAGE.current, limits.IMAGE.max)}`}
                      style={{ width: `${(limits.IMAGE.current / limits.IMAGE.max) * 100}%` }}
                    ></div>
                  </div>
                </div>

                {/* Videos Limit */}
                <div>
                  <div className="flex justify-between text-sm mb-1">
                    <span className="text-slate-400 flex items-center gap-1.5"><VideoIcon className="w-4 h-4" /> Videos</span>
                    <span className="font-semibold text-slate-200">{limits.VIDEO.current} / {limits.VIDEO.max}</span>
                  </div>
                  <div className="w-full bg-slate-950 rounded-full h-2 overflow-hidden border border-slate-900">
                    <div 
                      className={`h-full rounded-full transition-all duration-500 ${getLimitBarColor(limits.VIDEO.current, limits.VIDEO.max)}`}
                      style={{ width: `${(limits.VIDEO.current / limits.VIDEO.max) * 100}%` }}
                    ></div>
                  </div>
                </div>

                {/* Floor Plans Limit */}
                <div>
                  <div className="flex justify-between text-sm mb-1">
                    <span className="text-slate-400 flex items-center gap-1.5"><FileIcon className="w-4 h-4" /> Floor Plans</span>
                    <span className="font-semibold text-slate-200">{limits.FLOOR_PLAN.current} / {limits.FLOOR_PLAN.max}</span>
                  </div>
                  <div className="w-full bg-slate-950 rounded-full h-2 overflow-hidden border border-slate-900">
                    <div 
                      className={`h-full rounded-full transition-all duration-500 ${getLimitBarColor(limits.FLOOR_PLAN.current, limits.FLOOR_PLAN.max)}`}
                      style={{ width: `${(limits.FLOOR_PLAN.current / limits.FLOOR_PLAN.max) * 100}%` }}
                    ></div>
                  </div>
                </div>
              </div>
            </div>

            {/* Upload form card */}
            <div className="glass border border-slate-850 p-6 rounded-2xl space-y-6">
              <div>
                <h3 className="text-lg font-bold text-slate-200">Upload Assets</h3>
                <p className="text-xs text-slate-400 mt-1">Select media type and drop files below.</p>
              </div>

              {/* Media Type Tabs */}
              <div className="grid grid-cols-3 gap-2 bg-slate-950 p-1 rounded-xl border border-slate-900">
                <button
                  type="button"
                  onClick={() => setSelectedMediaType('IMAGE')}
                  className={`py-2 px-3 rounded-lg text-xs font-bold transition-all flex flex-col items-center gap-1 ${selectedMediaType === 'IMAGE' ? 'bg-indigo-500 text-white shadow-md' : 'text-slate-400 hover:text-slate-200'}`}
                >
                  <ImageIcon className="w-4 h-4" />
                  <span>Images</span>
                </button>
                <button
                  type="button"
                  onClick={() => setSelectedMediaType('VIDEO')}
                  className={`py-2 px-3 rounded-lg text-xs font-bold transition-all flex flex-col items-center gap-1 ${selectedMediaType === 'VIDEO' ? 'bg-indigo-500 text-white shadow-md' : 'text-slate-400 hover:text-slate-200'}`}
                >
                  <VideoIcon className="w-4 h-4" />
                  <span>Videos</span>
                </button>
                <button
                  type="button"
                  onClick={() => setSelectedMediaType('FLOOR_PLAN')}
                  className={`py-2 px-3 rounded-lg text-xs font-bold transition-all flex flex-col items-center gap-1 ${selectedMediaType === 'FLOOR_PLAN' ? 'bg-indigo-500 text-white shadow-md' : 'text-slate-400 hover:text-slate-200'}`}
                >
                  <FileIcon className="w-4 h-4" />
                  <span>Floor Plan</span>
                </button>
              </div>

              {/* Drag and Drop Zone */}
              <div 
                onClick={() => fileInputRef.current?.click()}
                className="border-2 border-dashed border-slate-800 hover:border-indigo-500/60 rounded-2xl p-8 text-center cursor-pointer transition-all bg-slate-900/10 hover:bg-indigo-500/[0.02] flex flex-col items-center justify-center group"
                onDragOver={(e) => { e.preventDefault(); e.currentTarget.classList.add('border-indigo-500'); }}
                onDragLeave={(e) => { e.preventDefault(); e.currentTarget.classList.remove('border-indigo-500'); }}
                onDrop={(e) => {
                  e.preventDefault();
                  e.currentTarget.classList.remove('border-indigo-500');
                  if (e.dataTransfer.files) {
                    addFilesToQueue(Array.from(e.dataTransfer.files));
                  }
                }}
              >
                <input 
                  type="file" 
                  ref={fileInputRef} 
                  className="hidden" 
                  multiple 
                  accept={
                    selectedMediaType === 'IMAGE' 
                      ? 'image/jpeg,image/png,image/webp,image/gif' 
                      : selectedMediaType === 'VIDEO' 
                        ? 'video/mp4,video/webm,video/ogg' 
                        : 'application/pdf,image/jpeg,image/png,image/webp'
                  }
                  onChange={handleFileChange}
                />
                <div className="w-12 h-12 rounded-xl bg-slate-900 border border-slate-800 flex items-center justify-center text-slate-400 group-hover:text-indigo-400 group-hover:border-indigo-500/30 transition-all mb-4">
                  <UploadCloud className="w-6 h-6 animate-pulse" />
                </div>
                <h4 className="text-sm font-semibold text-slate-200 group-hover:text-white">Click or drag files here</h4>
                <p className="text-xs text-slate-500 mt-1">
                  {selectedMediaType === 'IMAGE' 
                    ? 'JPEG, PNG, WEBP, GIF (Max 10MB)' 
                    : selectedMediaType === 'VIDEO' 
                      ? 'MP4, WEBM, OGG (Max 100MB)' 
                      : 'PDF, JPG, PNG (Max 20MB)'}
                </p>
              </div>

              {/* Upload Queue list */}
              {uploadQueue.length > 0 && (
                <div className="space-y-3 pt-4 border-t border-slate-850">
                  <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-400">Upload Queue</h4>
                  <div className="space-y-2 max-h-[200px] overflow-y-auto pr-1">
                    {uploadQueue.map(item => (
                      <div key={item.id} className="p-3 bg-slate-950 border border-slate-900 rounded-xl flex items-center justify-between gap-3 text-xs">
                        <div className="min-w-0 flex-grow">
                          <p className="text-slate-300 truncate font-medium">{item.file.name}</p>
                          <div className="flex items-center gap-2 mt-1">
                            <span className="text-[10px] text-slate-500 font-bold uppercase">{item.mediaType}</span>
                            {item.status === 'UPLOADING' && (
                              <div className="flex-grow bg-slate-900 h-1.5 rounded-full overflow-hidden border border-slate-900 max-w-[80px]">
                                <div className="bg-indigo-500 h-full" style={{ width: `${item.progress}%` }}></div>
                              </div>
                            )}
                            {item.status === 'COMPLETED' && <span className="text-emerald-400 text-[10px] font-bold">Uploaded</span>}
                            {item.status === 'FAILED' && <span className="text-rose-500 text-[10px] font-bold">Failed</span>}
                          </div>
                        </div>
                        <div className="flex items-center gap-1.5 flex-shrink-0">
                          {item.status === 'FAILED' && (
                            <button 
                              onClick={() => retryUpload(item)} 
                              className="p-1 text-slate-400 hover:text-white rounded hover:bg-slate-900"
                              title="Retry"
                            >
                              <RefreshCw className="w-3.5 h-3.5" />
                            </button>
                          )}
                          <button 
                            onClick={() => removeQueueItem(item.id)} 
                            className="p-1 text-slate-400 hover:text-rose-400 rounded hover:bg-slate-900"
                            title="Remove"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* Right / Grid gallery side */}
          <div className="lg:col-span-2 space-y-6">
            <div className="glass border border-slate-850 p-6 rounded-2xl space-y-6 min-h-[500px]">
              <div>
                <h3 className="text-lg font-bold text-slate-200">Media Gallery</h3>
                <p className="text-xs text-slate-400 mt-1">Drag and drop cards to reorder. Setting cover image automatically unsets the previous one.</p>
              </div>

              {mediaList.length === 0 ? (
                <div className="flex flex-col items-center justify-center text-center p-12 min-h-[300px]">
                  <div className="w-16 h-16 rounded-2xl bg-slate-900 border border-slate-850 flex items-center justify-center text-slate-500 mb-4">
                    <ImageIcon className="w-8 h-8" />
                  </div>
                  <h4 className="text-slate-300 font-bold">No media uploaded</h4>
                  <p className="text-slate-500 text-xs mt-2 max-w-sm">
                    Upload property images, video walkthroughs, and floor plans to showcase this listing.
                  </p>
                </div>
              ) : (
                <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4">
                  {mediaList.map((media, index) => (
                    <div 
                      key={media.id}
                      draggable
                      onDragStart={(e) => handleDragStart(e, index)}
                      onDragOver={(e) => handleDragOver(e, index)}
                      onDragEnd={handleDragEnd}
                      className={`relative rounded-xl overflow-hidden bg-slate-950 border transition-all duration-300 group flex flex-col justify-between ${draggedIndex === index ? 'opacity-40 scale-95 border-indigo-500/50 shadow-lg shadow-indigo-500/10' : 'border-slate-900 hover:border-slate-700/80 shadow-md'}`}
                    >
                      {/* Media Body / Preview */}
                      <div className="relative h-32 bg-slate-900 flex items-center justify-center overflow-hidden border-b border-slate-900">
                        {media.mediaType === 'IMAGE' ? (
                          <img 
                            src={getFullFileUrl(media.url)} 
                            alt={media.objectKey} 
                            className="object-cover w-full h-full group-hover:scale-105 transition-all duration-300"
                          />
                        ) : media.mediaType === 'VIDEO' ? (
                          <div className="w-full h-full relative flex items-center justify-center">
                            <video 
                              src={getFullFileUrl(media.url)}
                              className="object-cover w-full h-full"
                              muted
                            />
                            <div className="absolute inset-0 bg-black/40 flex items-center justify-center text-white">
                              <VideoIcon className="w-8 h-8" />
                            </div>
                          </div>
                        ) : (
                          <div className="flex flex-col items-center justify-center text-slate-500">
                            <FileIcon className="w-8 h-8 mb-1" />
                            <span className="text-[10px] font-bold font-mono truncate max-w-[100px]">{media.mimeType.split('/')[1]?.toUpperCase() || 'FLOOR PLAN'}</span>
                          </div>
                        )}

                        {/* Top overlays: Cover indicator & Media type tag */}
                        <div className="absolute top-2 left-2 z-10 flex gap-1.5 flex-wrap">
                          {media.isCover && (
                            <span className="px-2 py-0.5 rounded bg-amber-500 text-slate-950 text-[10px] font-extrabold tracking-wider flex items-center gap-1 shadow-md uppercase">
                              <Star className="w-3 h-3 fill-current" /> Cover
                            </span>
                          )}
                          <span className="px-1.5 py-0.5 rounded bg-black/60 backdrop-blur-md text-slate-300 text-[9px] font-bold tracking-wider uppercase border border-white/10">
                            {media.mediaType.replace('_', ' ')}
                          </span>
                        </div>

                        {/* Right overlay: status indicator */}
                        <div className="absolute top-2 right-2 z-10">
                          {media.processingStatus === 'PENDING' && (
                            <span className="p-1 rounded-full bg-amber-500/10 border border-amber-500/30 text-amber-500 flex items-center justify-center" title="Queued for processing">
                              <Loader2 className="w-3.5 h-3.5 animate-spin" />
                            </span>
                          )}
                          {media.processingStatus === 'PROCESSING' && (
                            <span className="p-1 rounded-full bg-indigo-500/10 border border-indigo-500/30 text-indigo-400 flex items-center justify-center animate-pulse" title="Processing...">
                              <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                            </span>
                          )}
                          {media.processingStatus === 'FAILED' && (
                            <span className="p-1 rounded-full bg-rose-500/10 border border-rose-500/30 text-rose-500 flex items-center justify-center" title="Processing failed (virus, NSFW, or invalid)">
                              <AlertCircle className="w-3.5 h-3.5" />
                            </span>
                          )}
                          {media.processingStatus === 'READY' && (
                            <span className="p-1 rounded-full bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 flex items-center justify-center" title="Ready & Safe">
                              <CheckCircle2 className="w-3.5 h-3.5" />
                            </span>
                          )}
                        </div>
                      </div>

                      {/* Info & Operations */}
                      <div className="p-3 bg-slate-950 flex flex-col justify-between">
                        <div className="flex items-center justify-between text-[10px] text-slate-500 font-mono mb-2">
                          <span>{(media.fileSize / (1024 * 1024)).toFixed(2)} MB</span>
                          {media.width && media.height && <span>{media.width}x{media.height}</span>}
                          {media.durationSeconds && <span>{media.durationSeconds}s</span>}
                        </div>

                        {/* Buttons row */}
                        <div className="flex items-center justify-between gap-2 border-t border-slate-900 pt-2.5">
                          {media.mediaType === 'IMAGE' && media.processingStatus === 'READY' && (
                            <button
                              onClick={() => handleSetCover(media.id)}
                              disabled={media.isCover}
                              className={`flex-grow py-1 px-2.5 rounded text-[10px] font-bold flex items-center justify-center gap-1 transition-all ${media.isCover ? 'bg-slate-900 text-slate-600 border border-slate-850 cursor-not-allowed' : 'bg-slate-900 hover:bg-slate-850 border border-slate-800 text-amber-400 hover:text-amber-300'}`}
                            >
                              <Star className={`w-3.5 h-3.5 ${media.isCover ? 'fill-current' : ''}`} />
                              <span>{media.isCover ? 'Cover' : 'Make Cover'}</span>
                            </button>
                          )}
                          
                          {/* Filler when no cover button can be rendered */}
                          {(media.mediaType !== 'IMAGE' || media.processingStatus !== 'READY') && (
                            <div className="flex-grow text-[9px] text-slate-500 font-medium">
                              {media.processingStatus === 'FAILED' ? 'Scan failed' : 'Safe validation'}
                            </div>
                          )}

                          <button
                            onClick={() => handleDeleteMedia(media.id)}
                            className="p-1.5 rounded bg-slate-900 border border-slate-800 hover:border-rose-500/30 text-slate-400 hover:text-rose-500 transition-colors"
                            title="Delete Media"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
