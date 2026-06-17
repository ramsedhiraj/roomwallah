import { useState, useEffect, useCallback, useRef } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Search, MapPin, SlidersHorizontal, Bookmark, Sparkles, AlertTriangle, ShieldCheck, Heart, RefreshCw } from 'lucide-react';
import { searchService } from '../services/searchService';
import type { PropertyCard, SearchParams, TrendingQueryItem } from '../services/searchService';
import PropertyResultsGrid from '../components/PropertyResultsGrid';
import RecommendationCarousel from '../components/RecommendationCarousel';
import SavedSearchDrawer from '../components/SavedSearchDrawer';

const CITIES = ['Mumbai', 'Delhi', 'Bangalore', 'Pune', 'Hyderabad', 'Chennai'];
const PROPERTY_TYPES = [
  { value: 'APARTMENT', label: 'Apartment' },
  { value: 'INDEPENDENT_HOUSE', label: 'House' },
  { value: 'VILLA', label: 'Villa' },
  { value: 'PENTHOUSE', label: 'Penthouse' }
];

export default function SearchPage() {
  const [searchParams, setSearchParams] = useSearchParams();

  // Search query states
  const [q, setQ] = useState('');
  const [city, setCity] = useState('Mumbai');
  const [locality, setLocality] = useState('');
  const [propertyType, setPropertyType] = useState('');
  const [listingPurpose, setListingPurpose] = useState('RENT');
  const [minPrice, setMinPrice] = useState('');
  const [maxPrice, setMaxPrice] = useState('');
  const [bedrooms, setBedrooms] = useState('');
  const [bathrooms, setBathrooms] = useState('');
  const [petFriendly, setPetFriendly] = useState(false);
  const [ownerVerified, setOwnerVerified] = useState(false);

  // Advanced filters
  const [furnishingStatus, setFurnishingStatus] = useState('');
  const [parkingCount, setParkingCount] = useState('');
  const [facingDirection, setFacingDirection] = useState('');
  const [availabilityDate, setAvailabilityDate] = useState('');
  const [minTrustScore, setMinTrustScore] = useState('');
  const [explain, setExplain] = useState(false);

  // Bounding box bounds (updated by Leaflet map)
  const [bbox, setBbox] = useState<{
    bboxNorthEastLat: number;
    bboxNorthEastLon: number;
    bboxSouthWestLat: number;
    bboxSouthWestLon: number;
  } | null>(null);

  // Geospatial radius search
  const [geoRadiusActive, setGeoRadiusActive] = useState(false);
  const [latitude, setLatitude] = useState('');
  const [longitude, setLongitude] = useState('');
  const [radiusKm, setRadiusKm] = useState('5');

  // Sorting and pagination states
  const [sortBy, setSortBy] = useState('createdAt');
  const [sortDir, setSortDir] = useState('desc');
  const [results, setResults] = useState<PropertyCard[]>([]);
  const [nextCursor, setNextCursor] = useState<string | null>(null);
  const [totalCount, setTotalCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [loadMoreLoading, setLoadMoreLoading] = useState(false);
  const [errorState, setErrorState] = useState<string | null>(null);

  // Autocomplete & Trending states
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [trending, setTrending] = useState<TrendingQueryItem[]>([]);

  // Drawer states
  const [isSavedSearchOpen, setIsSavedSearchOpen] = useState(false);
  const [saveSearchNotification, setSaveSearchNotification] = useState(false);
  const [notificationFrequency, setNotificationFrequency] = useState('INSTANT');

  // Online/Offline status check
  const [isOnline, setIsOnline] = useState(navigator.onLine);

  // Leaflet map integrations
  const [leafletLoaded, setLeafletLoaded] = useState(false);
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const mapInstanceRef = useRef<any>(null);
  const markerClusterGroupRef = useRef<any>(null);
  const abortControllerRef = useRef<AbortController | null>(null);

  // Listen to network status change
  useEffect(() => {
    const handleOnline = () => setIsOnline(true);
    const handleOffline = () => setIsOnline(false);

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  // Load Leaflet CDN script and css on mount
  useEffect(() => {
    if ((window as any).L) {
      setLeafletLoaded(true);
      return;
    }
    
    // CSS link
    const cssLink = document.createElement('link');
    cssLink.rel = 'stylesheet';
    cssLink.href = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css';
    document.head.appendChild(cssLink);
    
    // JS script
    const script = document.createElement('script');
    script.src = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js';
    script.async = true;
    script.onload = () => {
      // Load marker cluster script/styles too!
      const clusterCss = document.createElement('link');
      clusterCss.rel = 'stylesheet';
      clusterCss.href = 'https://unpkg.com/leaflet.markercluster@1.4.1/dist/MarkerCluster.css';
      document.head.appendChild(clusterCss);

      const clusterDefaultCss = document.createElement('link');
      clusterDefaultCss.rel = 'stylesheet';
      clusterDefaultCss.href = 'https://unpkg.com/leaflet.markercluster@1.4.1/dist/MarkerCluster.Default.css';
      document.head.appendChild(clusterDefaultCss);

      const clusterScript = document.createElement('script');
      clusterScript.src = 'https://unpkg.com/leaflet.markercluster@1.4.1/dist/leaflet.markercluster.js';
      clusterScript.async = true;
      clusterScript.onload = () => {
        setLeafletLoaded(true);
      };
      document.body.appendChild(clusterScript);
    };
    document.body.appendChild(script);
  }, []);

  // Parse filters from URL search params on mount
  useEffect(() => {
    const queryParam = searchParams.get('q') || '';
    const cityParam = searchParams.get('city') || 'Mumbai';
    const localityParam = searchParams.get('locality') || '';
    const typeParam = searchParams.get('propertyType') || '';
    const purposeParam = searchParams.get('listingPurpose') || 'RENT';
    const minPriceParam = searchParams.get('minPrice') || '';
    const maxPriceParam = searchParams.get('maxPrice') || '';
    const bedroomsParam = searchParams.get('bedrooms') || '';
    const bathroomsParam = searchParams.get('bathrooms') || '';
    const petParam = searchParams.get('petFriendly') === 'true';
    const verifiedParam = searchParams.get('ownerVerified') === 'true';
    
    const furnishParam = searchParams.get('furnishingStatus') || '';
    const parkingParam = searchParams.get('parkingCount') || '';
    const facingParam = searchParams.get('facingDirection') || '';
    const dateParam = searchParams.get('availabilityDate') || '';
    const trustParam = searchParams.get('minTrustScore') || '';
    const explainParam = searchParams.get('explain') === 'true';

    const bboxNeLat = searchParams.get('bboxNorthEastLat');
    const bboxNeLon = searchParams.get('bboxNorthEastLon');
    const bboxSwLat = searchParams.get('bboxSouthWestLat');
    const bboxSwLon = searchParams.get('bboxSouthWestLon');

    setQ(queryParam);
    setCity(cityParam);
    setLocality(localityParam);
    setPropertyType(typeParam);
    setListingPurpose(purposeParam);
    setMinPrice(minPriceParam);
    setMaxPrice(maxPriceParam);
    setBedrooms(bedroomsParam);
    setBathrooms(bathroomsParam);
    setPetFriendly(petParam);
    setOwnerVerified(verifiedParam);
    
    setFurnishingStatus(furnishParam);
    setParkingCount(parkingParam);
    setFacingDirection(facingParam);
    setAvailabilityDate(dateParam);
    setMinTrustScore(trustParam);
    setExplain(explainParam);

    if (bboxNeLat && bboxNeLon && bboxSwLat && bboxSwLon) {
      setBbox({
        bboxNorthEastLat: Number(bboxNeLat),
        bboxNorthEastLon: Number(bboxNeLon),
        bboxSouthWestLat: Number(bboxSwLat),
        bboxSouthWestLon: Number(bboxSwLon)
      });
    }
  }, []);

  // Sync state changes to URL search params
  const syncParamsToUrl = useCallback(() => {
    const params: Record<string, string> = {};
    if (q) params.q = q;
    if (city) params.city = city;
    if (locality) params.locality = locality;
    if (propertyType) params.propertyType = propertyType;
    if (listingPurpose) params.listingPurpose = listingPurpose;
    if (minPrice) params.minPrice = minPrice;
    if (maxPrice) params.maxPrice = maxPrice;
    if (bedrooms) params.bedrooms = bedrooms;
    if (bathrooms) params.bathrooms = bathrooms;
    if (petFriendly) params.petFriendly = 'true';
    if (ownerVerified) params.ownerVerified = 'true';
    
    if (furnishingStatus) params.furnishingStatus = furnishingStatus;
    if (parkingCount) params.parkingCount = parkingCount;
    if (facingDirection) params.facingDirection = facingDirection;
    if (availabilityDate) params.availabilityDate = availabilityDate;
    if (minTrustScore) params.minTrustScore = minTrustScore;
    if (explain) params.explain = 'true';
    if (sortBy) params.sortBy = sortBy;
    if (sortDir) params.sortDir = sortDir;

    if (bbox) {
      params.bboxNorthEastLat = bbox.bboxNorthEastLat.toString();
      params.bboxNorthEastLon = bbox.bboxNorthEastLon.toString();
      params.bboxSouthWestLat = bbox.bboxSouthWestLat.toString();
      params.bboxSouthWestLon = bbox.bboxSouthWestLon.toString();
    }

    setSearchParams(params, { replace: true });
  }, [q, city, locality, propertyType, listingPurpose, minPrice, maxPrice, bedrooms, bathrooms, petFriendly, ownerVerified, furnishingStatus, parkingCount, facingDirection, availabilityDate, minTrustScore, explain, sortBy, sortDir, bbox, setSearchParams]);

  useEffect(() => {
    syncParamsToUrl();
  }, [q, city, locality, propertyType, listingPurpose, minPrice, maxPrice, bedrooms, bathrooms, petFriendly, ownerVerified, furnishingStatus, parkingCount, facingDirection, availabilityDate, minTrustScore, explain, sortBy, sortDir, bbox]);

  // Debounce autocomplete suggestion queries
  useEffect(() => {
    if (q.trim().length < 2) {
      setSuggestions([]);
      return;
    }
    const delayDebounce = setTimeout(async () => {
      try {
        const list = await searchService.getAutoComplete(q, city);
        setSuggestions(list);
      } catch (err) {
        console.error(err);
      }
    }, 300);

    return () => clearTimeout(delayDebounce);
  }, [q, city]);

  // Load trending queries on startup/city update
  useEffect(() => {
    async function loadTrending() {
      try {
        const list = await searchService.getTrendingQueries(city);
        setTrending(list);
      } catch (err) {
        console.error(err);
      }
    }
    loadTrending();
  }, [city]);

  // Main search function
  const executeSearch = useCallback(async (append = false, customParams?: SearchParams) => {
    try {
      if (append) {
        setLoadMoreLoading(true);
      } else {
        setLoading(true);
      }

      // Abort previous pending search calls
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
      }
      const abortController = new AbortController();
      abortControllerRef.current = abortController;

      // Input Sanitization & City extraction
      let finalParams: SearchParams;
      if (customParams) {
        finalParams = { ...customParams };
        if (finalParams.q) {
          const trimmedQ = finalParams.q.replace(/[%_]/g, '').trim();
          const matchedCity = CITIES.find(c => c.toLowerCase() === trimmedQ.toLowerCase());
          if (matchedCity) {
            finalParams.city = matchedCity;
            finalParams.q = undefined;
          } else {
            finalParams.q = trimmedQ;
          }
        }
      } else {
        let sanitizedQ = q || undefined;
        let searchCity = city || undefined;
        if (q) {
          const trimmedQ = q.replace(/[%_]/g, '').trim();
          const matchedCity = CITIES.find(c => c.toLowerCase() === trimmedQ.toLowerCase());
          if (matchedCity) {
            searchCity = matchedCity;
            sanitizedQ = undefined;
          } else {
            sanitizedQ = trimmedQ;
          }
        }

        finalParams = {
          q: sanitizedQ,
          city: searchCity,
          locality: locality || undefined,
          propertyType: propertyType || undefined,
          listingPurpose: listingPurpose || undefined,
          minPrice: minPrice ? Number(minPrice) : undefined,
          maxPrice: maxPrice ? Number(maxPrice) : undefined,
          bedrooms: bedrooms ? Number(bedrooms) : undefined,
          bathrooms: bathrooms ? Number(bathrooms) : undefined,
          petFriendly: petFriendly || undefined,
          ownerVerified: ownerVerified || undefined,
          latitude: geoRadiusActive && latitude ? Number(latitude) : undefined,
          longitude: geoRadiusActive && longitude ? Number(longitude) : undefined,
          radiusKm: geoRadiusActive && radiusKm ? Number(radiusKm) : undefined,
          furnishingStatus: furnishingStatus || undefined,
          parkingCount: parkingCount ? Number(parkingCount) : undefined,
          facingDirection: facingDirection || undefined,
          availabilityDate: availabilityDate || undefined,
          minTrustScore: minTrustScore ? Number(minTrustScore) : undefined,
          bboxNorthEastLat: bbox?.bboxNorthEastLat,
          bboxNorthEastLon: bbox?.bboxNorthEastLon,
          bboxSouthWestLat: bbox?.bboxSouthWestLat,
          bboxSouthWestLon: bbox?.bboxSouthWestLon,
          explain: explain || undefined,
          sortBy,
          sortDir,
          cursor: append ? (nextCursor || undefined) : undefined,
          size: 9
        };
      }

      const response = await searchService.searchProperties(finalParams, abortController.signal);
      
      if (append) {
        setResults((prev) => [...prev, ...response.results]);
      } else {
        setResults(response.results);
      }
      setNextCursor(response.nextCursor);
      setTotalCount(response.totalCount);
      setErrorState(null);
    } catch (err: any) {
      if (err.name === 'CanceledError' || err.name === 'AbortError') {
        return;
      }
      console.error('Search failed', err);
      setErrorState(err.message || 'An error occurred during search. Please try again.');
    } finally {
      if (append) {
        setLoadMoreLoading(false);
      } else {
        setLoading(false);
      }
    }
  }, [q, city, locality, propertyType, listingPurpose, minPrice, maxPrice, bedrooms, bathrooms, petFriendly, ownerVerified, geoRadiusActive, latitude, longitude, radiusKm, furnishingStatus, parkingCount, facingDirection, availabilityDate, minTrustScore, bbox, explain, sortBy, sortDir, nextCursor]);

  // Trigger search when filter states update
  useEffect(() => {
    executeSearch(false);
  }, [city, listingPurpose, propertyType, bedrooms, bathrooms, petFriendly, ownerVerified, furnishingStatus, parkingCount, facingDirection, availabilityDate, minTrustScore, explain, sortBy, sortDir, geoRadiusActive, bbox]);

  // Initialize and synchronize Leaflet Map viewport bounding-box lazily
  useEffect(() => {
    if (!leafletLoaded || !mapContainerRef.current) return;
    const L = (window as any).L;

    // Use default city coordinates
    let defaultCoords: [number, number] = [19.0760, 72.8777]; // Mumbai
    if (city === 'Delhi') defaultCoords = [28.7041, 77.1025];
    else if (city === 'Bangalore') defaultCoords = [12.9716, 77.5946];
    else if (city === 'Pune') defaultCoords = [18.5204, 73.8567];
    else if (city === 'Hyderabad') defaultCoords = [17.3850, 78.4867];
    else if (city === 'Chennai') defaultCoords = [13.0827, 80.2707];

    const map = L.map(mapContainerRef.current, {
      zoomControl: true,
      scrollWheelZoom: true
    }).setView(defaultCoords, 11);
    mapInstanceRef.current = map;

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap contributors'
    }).addTo(map);

    // Marker cluster group
    const markerClusters = L.markerClusterGroup();
    map.addLayer(markerClusters);
    markerClusterGroupRef.current = markerClusters;

    // Sync viewport boundaries on movement
    map.on('moveend', () => {
      const bounds = map.getBounds();
      const ne = bounds.getNorthEast();
      const sw = bounds.getSouthWest();
      
      setBbox({
        bboxNorthEastLat: ne.lat,
        bboxNorthEastLon: ne.lng,
        bboxSouthWestLat: sw.lat,
        bboxSouthWestLon: sw.lng
      });
    });

    return () => {
      map.remove();
      mapInstanceRef.current = null;
    };
  }, [leafletLoaded, city]);

  // Redraw cluster markers on map when results update
  useEffect(() => {
    if (!mapInstanceRef.current || !leafletLoaded || !markerClusterGroupRef.current) return;
    const L = (window as any).L;
    const clusters = markerClusterGroupRef.current;

    clusters.clearLayers();

    results.forEach((property) => {
      if (property.latitude && property.longitude) {
        const marker = L.marker([property.latitude, property.longitude])
          .bindPopup(`
            <div class="p-2 text-slate-900 text-xs font-sans">
              <h4 class="font-bold text-sm text-slate-800">${property.title}</h4>
              <p class="text-slate-500 mt-1">${property.locality}, ${property.city}</p>
              <p class="font-bold text-primary mt-1">₹${property.price.toLocaleString('en-IN')}</p>
              <a href="/properties/${property.propertyId}" class="mt-2 block w-full py-1 text-center bg-primary text-white rounded font-semibold text-[10px] hover:bg-opacity-95">
                View Details
              </a>
            </div>
          `);
        clusters.addLayer(marker);
      }
    });
  }, [results, leafletLoaded]);

  const handleApplySavedSearch = (serialized: string) => {
    try {
      const parsed = JSON.parse(serialized);
      setQ(parsed.text || '');
      if (parsed.filter) {
        setCity(parsed.filter.city || 'Mumbai');
        setLocality(parsed.filter.locality || '');
        setPropertyType(parsed.filter.propertyType || '');
        setListingPurpose(parsed.filter.listingPurpose || 'RENT');
        if (parsed.filter.priceRange) {
          setMinPrice(parsed.filter.priceRange.minPrice || '');
          setMaxPrice(parsed.filter.priceRange.maxPrice || '');
        }
        setBedrooms(parsed.filter.bedrooms || '');
        setBathrooms(parsed.filter.bathrooms || '');
        setPetFriendly(!!parsed.filter.petFriendly);
        setOwnerVerified(!!parsed.filter.ownerVerified);
      }
    } catch (e) {
      console.error('Failed to parse query', e);
    }
  };

  const handleSaveSearch = async () => {
    const currentQuery = {
      text: q || undefined,
      filter: {
        city,
        locality: locality || undefined,
        propertyType: propertyType || undefined,
        listingPurpose: listingPurpose || undefined,
        priceRange: (minPrice || maxPrice) ? {
          minPrice: minPrice ? Number(minPrice) : null,
          maxPrice: maxPrice ? Number(maxPrice) : null
        } : null,
        bedrooms: bedrooms ? Number(bedrooms) : null,
        bathrooms: bathrooms ? Number(bathrooms) : null,
        petFriendly: petFriendly || null,
        ownerVerified: ownerVerified || null
      }
    };

    try {
      await searchService.createSavedSearch(
        JSON.stringify(currentQuery), 
        saveSearchNotification,
        notificationFrequency
      );
      alert('Search saved successfully!');
    } catch (e) {
      console.error(e);
      alert('Failed to save search.');
    }
  };

  return (
    <div className="container mx-auto px-4 py-8 space-y-6">
      {/* Skip-to-content accessibility link */}
      <a href="#search-results" className="sr-only focus:not-sr-only focus:absolute focus:z-50 focus:px-4 focus:py-2 focus:bg-primary focus:text-white rounded-lg m-2">
        Skip to search results
      </a>

      {/* Recommendations Slider */}
      <RecommendationCarousel />

      {/* Connection Recovery Banner */}
      {!isOnline && (
        <div data-testid="offline-banner" className="bg-amber-950/80 border border-amber-800 text-amber-200 px-4 py-3 rounded-2xl flex items-center justify-between text-xs gap-3">
          <span className="flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 text-amber-400" />
            You are currently offline. Showing cached search results from local storage.
          </span>
          <button 
            onClick={() => executeSearch(false)}
            className="px-3 py-1 bg-amber-800 hover:bg-amber-700 text-amber-100 rounded-lg font-semibold flex items-center gap-1.5"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            Retry
          </button>
        </div>
      )}

      {errorState && (
        <div className="bg-rose-950/80 border border-rose-800 text-rose-200 px-4 py-3 rounded-2xl flex items-center justify-between text-xs gap-3">
          <span className="flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 text-rose-400" />
            {errorState}
          </span>
          <button 
            onClick={() => executeSearch(false)}
            className="px-3 py-1 bg-rose-800 hover:bg-rose-700 text-rose-100 rounded-lg font-semibold"
          >
            Retry Search
          </button>
        </div>
      )}

      <div className="flex flex-col lg:flex-row gap-8">
        {/* Sidebar Filters */}
        <aside className="w-full lg:w-80 flex-shrink-0 glass p-6 rounded-2xl h-fit border border-slate-800/80 space-y-6" role="search" aria-label="Search filters">
          <div className="flex items-center justify-between pb-4 border-b border-slate-800/80">
            <h2 className="text-lg font-bold text-slate-200 flex items-center gap-2">
              <SlidersHorizontal className="w-4 h-4 text-primary" />
              Filters
            </h2>
            <button
              onClick={() => {
                setLocality('');
                setPropertyType('');
                setMinPrice('');
                setMaxPrice('');
                setBedrooms('');
                setBathrooms('');
                setPetFriendly(false);
                setOwnerVerified(false);
                setGeoRadiusActive(false);
                setFurnishingStatus('');
                setParkingCount('');
                setFacingDirection('');
                setAvailabilityDate('');
                setMinTrustScore('');
                setExplain(false);
                setBbox(null);
              }}
              className="text-xs text-primary hover:text-opacity-80 transition-all focus:ring-2 focus:ring-primary focus:outline-none rounded"
            >
              Clear All
            </button>
          </div>

          {/* Listing Purpose */}
          <div className="flex bg-slate-800/40 p-1 rounded-xl border border-slate-700/40">
            <button
              onClick={() => setListingPurpose('RENT')}
              className={`flex-1 py-2 text-xs font-semibold rounded-lg transition-all ${
                listingPurpose === 'RENT'
                  ? 'bg-primary text-primary-foreground shadow-md'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              For Rent
            </button>
            <button
              onClick={() => setListingPurpose('SALE')}
              className={`flex-1 py-2 text-xs font-semibold rounded-lg transition-all ${
                listingPurpose === 'SALE'
                  ? 'bg-primary text-primary-foreground shadow-md'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              For Sale
            </button>
          </div>

          {/* City Selection */}
          <div className="space-y-2">
            <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider">City</label>
            <select
              id="city-filter"
              value={city}
              onChange={(e) => setCity(e.target.value)}
              className="w-full bg-slate-800/40 border border-slate-700/50 rounded-xl px-4 py-2.5 text-sm text-slate-200 focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary"
            >
              {CITIES.map((c) => (
                <option key={c} value={c} className="bg-slate-900">
                  {c}
                </option>
              ))}
            </select>
          </div>

          {/* Locality Input */}
          <div className="space-y-2">
            <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Locality</label>
            <input
              type="text"
              placeholder="e.g. Bandra"
              value={locality}
              onChange={(e) => setLocality(e.target.value)}
              className="w-full bg-slate-800/40 border border-slate-700/50 rounded-xl px-4 py-2.5 text-sm text-slate-200 focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary"
            />
          </div>

          {/* Property Type */}
          <div className="space-y-2">
            <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Property Type</label>
            <div className="grid grid-cols-2 gap-2">
              {PROPERTY_TYPES.map((t) => (
                <button
                  key={t.value}
                  onClick={() => setPropertyType(propertyType === t.value ? '' : t.value)}
                  className={`py-2 px-3 text-xs font-medium rounded-xl border text-center transition-all ${
                    propertyType === t.value
                      ? 'bg-primary/20 border-primary text-primary'
                      : 'bg-slate-800/30 border-slate-700/50 text-slate-400 hover:border-slate-600/50 hover:text-slate-300'
                  }`}
                >
                  {t.label}
                </button>
              ))}
            </div>
          </div>

          {/* Furnishing Status */}
          <div className="space-y-2">
            <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Furnishing</label>
            <select
              value={furnishingStatus}
              onChange={(e) => setFurnishingStatus(e.target.value)}
              className="w-full bg-slate-800/40 border border-slate-700/50 rounded-xl px-4 py-2.5 text-sm text-slate-200 focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary"
            >
              <option value="" className="bg-slate-900">All Furnishing Types</option>
              <option value="FULLY_FURNISHED" className="bg-slate-900">Fully Furnished</option>
              <option value="SEMI_FURNISHED" className="bg-slate-900">Semi Furnished</option>
              <option value="UNFURNISHED" className="bg-slate-900">Unfurnished</option>
            </select>
          </div>

          {/* Price Range */}
          <div className="space-y-2">
            <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Price Range</label>
            <div className="flex gap-2 items-center">
              <input
                id="min-price"
                type="number"
                placeholder="Min"
                value={minPrice}
                onChange={(e) => setMinPrice(e.target.value)}
                className="w-full bg-slate-800/40 border border-slate-700/50 rounded-xl px-3 py-2 text-sm text-slate-200 focus:outline-none focus:border-primary"
              />
              <span className="text-slate-600">-</span>
              <input
                id="max-price"
                type="number"
                placeholder="Max"
                value={maxPrice}
                onChange={(e) => setMaxPrice(e.target.value)}
                className="w-full bg-slate-800/40 border border-slate-700/50 rounded-xl px-3 py-2 text-sm text-slate-200 focus:outline-none focus:border-primary"
              />
            </div>
          </div>

          {/* Bedrooms BHK */}
          <div className="space-y-2">
            <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Bedrooms (BHK)</label>
            <div className="flex gap-2">
              {['1', '2', '3', '4'].map((b) => (
                <button
                  key={b}
                  onClick={() => setBedrooms(bedrooms === b ? '' : b)}
                  className={`w-10 h-10 text-xs font-semibold rounded-xl border flex items-center justify-center transition-all ${
                    bedrooms === b
                      ? 'bg-primary border-primary text-primary-foreground shadow-md'
                      : 'bg-slate-800/30 border-slate-700/50 text-slate-400 hover:border-slate-600'
                  }`}
                >
                  {b}
                </button>
              ))}
            </div>
          </div>

          {/* Parking Count */}
          <div className="space-y-2">
            <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Parking Count (Min)</label>
            <div className="flex gap-2">
              {['0', '1', '2', '3'].map((p) => (
                <button
                  key={p}
                  onClick={() => setParkingCount(parkingCount === p ? '' : p)}
                  className={`w-10 h-10 text-xs font-semibold rounded-xl border flex items-center justify-center transition-all ${
                    parkingCount === p
                      ? 'bg-primary border-primary text-primary-foreground shadow-md'
                      : 'bg-slate-800/30 border-slate-700/50 text-slate-400 hover:border-slate-600'
                  }`}
                >
                  {p}+
                </button>
              ))}
            </div>
          </div>

          {/* Facing Direction */}
          <div className="space-y-2">
            <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Facing Direction</label>
            <select
              value={facingDirection}
              onChange={(e) => setFacingDirection(e.target.value)}
              className="w-full bg-slate-800/40 border border-slate-700/50 rounded-xl px-4 py-2.5 text-sm text-slate-200 focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary"
            >
              <option value="" className="bg-slate-900">All Directions</option>
              <option value="NORTH" className="bg-slate-900">North</option>
              <option value="SOUTH" className="bg-slate-900">South</option>
              <option value="EAST" className="bg-slate-900">East</option>
              <option value="WEST" className="bg-slate-900">West</option>
              <option value="NORTHEAST" className="bg-slate-900">North-East</option>
              <option value="NORTHWEST" className="bg-slate-900">North-West</option>
              <option value="SOUTHEAST" className="bg-slate-900">South-East</option>
              <option value="SOUTHWEST" className="bg-slate-900">South-West</option>
            </select>
          </div>

          {/* Availability Date */}
          <div className="space-y-2">
            <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Available From</label>
            <input
              type="date"
              value={availabilityDate}
              onChange={(e) => setAvailabilityDate(e.target.value)}
              className="w-full bg-slate-800/40 border border-slate-700/50 rounded-xl px-4 py-2.5 text-sm text-slate-200 focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary"
            />
          </div>

          {/* Minimum Trust Score */}
          <div className="space-y-2">
            <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider flex justify-between">
              <span>Min Trust Score</span>
              <span className="text-primary font-bold">{minTrustScore || '0'}+</span>
            </label>
            <input
              type="range"
              min="0"
              max="100"
              value={minTrustScore || '0'}
              onChange={(e) => setMinTrustScore(e.target.value === '0' ? '' : e.target.value)}
              className="w-full accent-primary bg-slate-800 h-1 rounded-lg cursor-pointer"
            />
          </div>

          {/* Geospatial Radius search toggle */}
          <div className="pt-4 border-t border-slate-800/80 space-y-3">
            <label className="flex items-center gap-2 text-xs font-semibold text-slate-400 uppercase tracking-wider cursor-pointer">
              <input
                type="checkbox"
                checked={geoRadiusActive}
                onChange={() => setGeoRadiusActive(!geoRadiusActive)}
                className="rounded text-primary focus:ring-0 bg-slate-800"
              />
              Search by Geo-Radius
            </label>
            {geoRadiusActive && (
              <div className="space-y-2 p-3 bg-slate-900/50 border border-slate-800 rounded-xl">
                <div>
                  <input
                    type="number"
                    placeholder="Latitude"
                    value={latitude}
                    onChange={(e) => setLatitude(e.target.value)}
                    className="w-full bg-slate-800/30 border border-slate-700/40 rounded-lg px-3 py-1.5 text-xs text-slate-200 focus:outline-none focus:border-primary"
                  />
                </div>
                <div>
                  <input
                    type="number"
                    placeholder="Longitude"
                    value={longitude}
                    onChange={(e) => setLongitude(e.target.value)}
                    className="w-full bg-slate-800/30 border border-slate-700/40 rounded-lg px-3 py-1.5 text-xs text-slate-200 focus:outline-none focus:border-primary"
                  />
                </div>
                <div className="flex justify-between items-center gap-2">
                  <span className="text-[10px] text-slate-500 font-medium">Radius (km)</span>
                  <input
                    type="number"
                    value={radiusKm}
                    onChange={(e) => setRadiusKm(e.target.value)}
                    className="w-16 bg-slate-800/30 border border-slate-700/40 rounded-lg px-2 py-1 text-center text-xs text-slate-200 focus:outline-none focus:border-primary"
                  />
                </div>
              </div>
            )}
          </div>

          {/* Quick Filters */}
          <div className="pt-4 border-t border-slate-800/80 space-y-3">
            <label className="flex items-center justify-between text-xs text-slate-400 cursor-pointer">
              <span className="flex items-center gap-1.5">
                <ShieldCheck className="w-4 h-4 text-emerald-400" />
                Verified Owner Listings
              </span>
              <input
                type="checkbox"
                checked={ownerVerified}
                onChange={() => setOwnerVerified(!ownerVerified)}
                className="rounded text-primary focus:ring-0 bg-slate-800"
              />
            </label>
            <label className="flex items-center justify-between text-xs text-slate-400 cursor-pointer">
              <span>Pet Friendly Only</span>
              <input
                type="checkbox"
                checked={petFriendly}
                onChange={() => setPetFriendly(!petFriendly)}
                className="rounded text-primary focus:ring-0 bg-slate-800"
              />
            </label>
            <label className="flex items-center justify-between text-xs text-slate-400 cursor-pointer">
              <span className="flex items-center gap-1.5">
                <Sparkles className="w-4 h-4 text-yellow-400" />
                Explain Ranking Match (Admin)
              </span>
              <input
                type="checkbox"
                checked={explain}
                onChange={() => setExplain(!explain)}
                className="rounded text-primary focus:ring-0 bg-slate-800"
              />
            </label>
          </div>
        </aside>

        {/* Main Content Area */}
        <main className="flex-1 space-y-6">
          {/* Search Bar / Input Row */}
          <div className="flex flex-col md:flex-row gap-4 items-stretch relative">
            <div className="flex-1 relative">
              <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-500" />
              <input
                type="text"
                placeholder="Search by city, locality, or landmark..."
                value={q}
                onChange={(e) => {
                  setQ(e.target.value);
                  setShowSuggestions(true);
                }}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    setShowSuggestions(false);
                    executeSearch(false);
                  }
                }}
                onFocus={() => setShowSuggestions(true)}
                onBlur={() => setTimeout(() => setShowSuggestions(false), 200)}
                className="w-full bg-slate-800/40 border border-slate-800/80 focus:border-primary rounded-2xl pl-12 pr-6 py-4 text-slate-200 placeholder-slate-500 focus:outline-none transition-all"
              />
              
              {/* Autocomplete suggestions dropdown */}
              {showSuggestions && suggestions.length > 0 && (
                <ul role="listbox" className="absolute top-full left-0 right-0 mt-2 bg-slate-900 border border-slate-800 rounded-xl overflow-hidden z-20 shadow-2xl list-none p-0 m-0">
                  {suggestions.map((s, idx) => (
                    <li
                      key={idx}
                      role="option"
                      onMouseDown={() => {
                        setQ(s);
                        setShowSuggestions(false);
                        executeSearch(false, { q: s, city, listingPurpose });
                      }}
                      className="w-full text-left px-5 py-3 text-sm text-slate-300 hover:bg-slate-800/50 hover:text-primary transition-all border-b border-slate-800/40 last:border-0 cursor-pointer"
                    >
                      {s}
                    </li>
                  ))}
                </ul>
              )}
            </div>

            {/* Action Toggles */}
            <div className="flex gap-2">
              <button
                onClick={() => setIsSavedSearchOpen(true)}
                className="px-4 py-3 bg-slate-800/30 border border-slate-800/80 rounded-2xl text-slate-300 hover:text-primary hover:border-primary transition-all flex items-center gap-2"
                title="Saved Searches"
              >
                <Bookmark className="w-4 h-4" />
                <span className="hidden sm:inline text-xs font-semibold">Saved Searches</span>
              </button>

              <button
                onClick={() => {
                  const opt = confirm("Would you like to get notifications when new listings match this search?");
                  setSaveSearchNotification(opt);
                  if (opt) {
                    const freq = prompt("Enter digest frequency (INSTANT, DAILY, WEEKLY):", "INSTANT");
                    if (freq) {
                      setNotificationFrequency(freq.toUpperCase());
                    }
                  }
                  handleSaveSearch();
                }}
                className="px-5 py-3 bg-primary text-primary-foreground font-semibold rounded-2xl hover:bg-opacity-95 shadow-lg shadow-primary/20 text-xs flex items-center gap-2"
              >
                Save This Search
              </button>
            </div>
          </div>

          {/* Trending queries block */}
          {trending.length > 0 && (
            <div className="flex flex-wrap items-center gap-2 text-xs">
              <span className="text-slate-500 font-semibold uppercase tracking-wider">Trending:</span>
              {trending.map((t, idx) => (
                <button
                  key={idx}
                  onClick={() => {
                    setQ(t.queryText);
                    executeSearch(false, { q: t.queryText, city, listingPurpose });
                  }}
                  className="px-2.5 py-1 rounded-full bg-slate-800/55 border border-slate-700/30 text-slate-300 hover:border-primary hover:text-primary transition-all"
                >
                  {t.queryText}
                </button>
              ))}
            </div>
          )}

          {/* Interactive Map Split View */}
          <div className="grid grid-cols-1 xl:grid-cols-2 gap-6 items-stretch">
            {/* Left side: Results Summary & Grid */}
            <div className="space-y-4" id="search-results">
              <div className="flex items-center justify-between border-b border-slate-800/50 pb-4">
                <span className="text-xs text-muted-foreground font-semibold">
                  Showing {results.length} of {totalCount} listings
                </span>

                {/* Sort order selector */}
                <div className="flex gap-2 text-xs">
                  <select
                    value={`${sortBy}_${sortDir}`}
                    onChange={(e) => {
                      const [field, dir] = e.target.value.split('_');
                      setSortBy(field);
                      setSortDir(dir);
                    }}
                    className="bg-transparent text-slate-300 hover:text-slate-100 font-semibold focus:outline-none cursor-pointer"
                  >
                    <option value="createdAt_desc" className="bg-slate-900">Newest Listings</option>
                    <option value="price_asc" className="bg-slate-900">Price: Low to High</option>
                    <option value="price_desc" className="bg-slate-900">Price: High to Low</option>
                    <option value="trustScore_desc" className="bg-slate-900">Highest Trust Score</option>
                    <option value="relevance_desc" className="bg-slate-900">Best Match / Relevance</option>
                  </select>
                </div>
              </div>

              {/* Property Grid */}
              <PropertyResultsGrid results={results} loading={loading} />

              {/* Pagination */}
              {nextCursor && !loading && (
                <div className="flex justify-center pt-6">
                  <button
                    onClick={() => executeSearch(true)}
                    disabled={loadMoreLoading}
                    className="px-6 py-3 bg-slate-800/50 hover:bg-slate-800 border border-slate-700/50 rounded-xl text-slate-300 hover:text-slate-200 transition-all text-xs font-semibold flex items-center gap-2"
                  >
                    {loadMoreLoading ? (
                      <>
                        <span className="w-3.5 h-3.5 border-2 border-slate-400 border-t-transparent rounded-full animate-spin" />
                        Loading More...
                      </>
                    ) : (
                      'Load More Listings'
                    )}
                  </button>
                </div>
              )}
            </div>

            {/* Right side: Split Map */}
            <div className="h-[550px] rounded-2xl overflow-hidden border border-slate-800 sticky top-8 shadow-2xl z-10" aria-label="Interactive Map split view" role="application">
              {leafletLoaded ? (
                <div ref={mapContainerRef} className="w-full h-full" />
              ) : (
                <div className="w-full h-full bg-slate-800/20 flex flex-col items-center justify-center gap-3">
                  <span className="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin" />
                  <span className="text-xs text-slate-400 font-semibold">Loading Map Viewport...</span>
                </div>
              )}
            </div>
          </div>
        </main>
      </div>

      {/* Slide-over Saved Searches Panel */}
      <SavedSearchDrawer
        isOpen={isSavedSearchOpen}
        onClose={() => setIsSavedSearchOpen(false)}
        onSelectSearch={handleApplySavedSearch}
      />
    </div>
  );
}
