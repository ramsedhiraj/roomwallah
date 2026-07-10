import { apiClient } from './api';

export interface BookingRequest {
  propertyId: string;
  priceAmount: number;
  priceCurrency: string;
  notes?: string;
  idempotencyKey?: string;
}

export interface BookingResponse {
  id: string;
  propertyId: string;
  tenantId: string;
  ownerId: string;
  status: 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'COMPLETED' | 'REJECTED';
  priceAmount: number;
  priceCurrency: string;
  notes?: string;
  createdAt: string;
}

export interface PropertyVisitRequest {
  propertyId: string;
  visitSlotId: string;
  notes?: string;
}

export interface PropertyVisitResponse {
  id: string;
  propertyId: string;
  tenantId: string;
  visitSlotId: string;
  status: 'SCHEDULED' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW' | 'PENDING_CONFIRMATION';
  startTime: string;
  endTime: string;
  notes?: string;
  createdAt: string;
}

export interface VisitSlotResponse {
  id: string;
  propertyId: string;
  startTime: string;
  endTime: string;
  maxBookings: number;
  currentBookings: number;
  status: 'AVAILABLE' | 'BOOKED' | 'CANCELLED' | 'EXPIRED';
}

export interface LeadResponse {
  id: string;
  propertyId: string;
  tenantId: string;
  ownerId: string;
  status: 'NEW' | 'CONTACTED' | 'QUALIFIED' | 'UNQUALIFIED' | 'CONVERTED' | 'LOST';
  inquiryText?: string;
  contactPhone?: string;
  contactEmail?: string;
  leadScore: number;
  leadScoreExplanation?: string;
  createdAt: string;
}

export interface LeadNote {
  id: string;
  leadId: string;
  authorId: string;
  content: string;
  createdAt: string;
}

export const bookingService = {
  // Tenant Booking Operations
  createBooking: async (data: BookingRequest): Promise<BookingResponse> => {
    const headers: Record<string, string> = {};
    if (data.idempotencyKey) {
      headers['Idempotency-Key'] = data.idempotencyKey;
    }
    const response = await apiClient.post('/bookings', data, { headers });
    return response.data.data;
  },

  getMyBookings: async (): Promise<BookingResponse[]> => {
    const response = await apiClient.get('/bookings/me');
    return response.data.data;
  },

  cancelBooking: async (id: string): Promise<BookingResponse> => {
    const response = await apiClient.post(`/bookings/${id}/cancel`);
    return response.data.data;
  },

  // Tenant Visit Operations
  scheduleVisit: async (data: PropertyVisitRequest): Promise<PropertyVisitResponse> => {
    const response = await apiClient.post('/visits', data);
    return response.data.data;
  },

  getMyVisits: async (): Promise<PropertyVisitResponse[]> => {
    const response = await apiClient.get('/visits/me');
    return response.data.data;
  },

  cancelVisit: async (id: string): Promise<PropertyVisitResponse> => {
    const response = await apiClient.post(`/visits/${id}/cancel`);
    return response.data.data;
  },

  downloadIcsUrl: (id: string): string => {
    const baseURL = apiClient.defaults.baseURL || 'http://localhost:8080/api/v1';
    return `${baseURL}/visits/${id}/ics`;
  },

  // Owner/Admin Booking Actions
  getOwnerBookings: async (): Promise<BookingResponse[]> => {
    const response = await apiClient.get('/admin/bookings');
    return response.data.data;
  },

  approveBooking: async (id: string): Promise<BookingResponse> => {
    const response = await apiClient.post(`/admin/bookings/${id}/approve`);
    return response.data.data;
  },

  rejectBooking: async (id: string, reason: string): Promise<BookingResponse> => {
    const response = await apiClient.post(`/admin/bookings/${id}/reject?reason=${encodeURIComponent(reason)}`);
    return response.data.data;
  },

  // Owner/Admin CRM Leads Actions
  getLeads: async (): Promise<LeadResponse[]> => {
    const response = await apiClient.get('/admin/leads');
    return response.data.data;
  },

  addLeadNote: async (leadId: string, content: string): Promise<LeadResponse> => {
    const response = await apiClient.post(`/admin/leads/${leadId}/notes`, { content });
    return response.data.data;
  },

  getLeadNotes: async (leadId: string): Promise<LeadNote[]> => {
    const response = await apiClient.get(`/admin/leads/${leadId}/notes`);
    return response.data.data;
  },

  assignLead: async (leadId: string, assigneeId: string): Promise<LeadResponse> => {
    const response = await apiClient.post(`/admin/leads/${leadId}/assign`, { assigneeId });
    return response.data.data;
  },

  // Calendar Rules & Slots Generation
  saveCalendarRules: async (params: {
    recurrenceRulesJson: string;
    blackoutDatesJson?: string;
    vacationStart?: string;
    vacationEnd?: string;
  }): Promise<any> => {
    const searchParams = new URLSearchParams();
    searchParams.append('rules', params.recurrenceRulesJson);
    if (params.blackoutDatesJson) {
      searchParams.append('blackouts', params.blackoutDatesJson);
    }
    if (params.vacationStart) {
      searchParams.append('vacationStart', params.vacationStart);
    }
    if (params.vacationEnd) {
      searchParams.append('vacationEnd', params.vacationEnd);
    }

    const response = await apiClient.post('/admin/calendar', searchParams, {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
    });
    return response.data.data;
  },

  getCalendarRules: async (): Promise<any> => {
    try {
      const response = await apiClient.get('/admin/calendar');
      return response.data.data;
    } catch (error: any) {
      if (error.response?.status === 404) {
        return null;
      }
      throw error;
    }
  },

  generateSlots: async (propertyId: string, start: string, end: string): Promise<VisitSlotResponse[]> => {
    const searchParams = new URLSearchParams();
    searchParams.append('propertyId', propertyId);
    searchParams.append('start', start);
    searchParams.append('end', end);

    const response = await apiClient.post('/admin/calendar/slots', searchParams, {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
    });
    return response.data.data;
  },

  getPropertySlots: async (propertyId: string): Promise<VisitSlotResponse[]> => {
    const response = await apiClient.get(`/admin/calendar/slots?propertyId=${propertyId}`);
    return response.data.data;
  },

  recordNoShow: async (visitId: string): Promise<PropertyVisitResponse> => {
    const response = await apiClient.post(`/admin/visits/${visitId}/noshow`);
    return response.data.data;
  },

  completeVisit: async (visitId: string): Promise<PropertyVisitResponse> => {
    const response = await apiClient.post(`/admin/visits/${visitId}/complete`);
    return response.data.data;
  },

  completeBooking: async (bookingId: string): Promise<BookingResponse> => {
    const response = await apiClient.post(`/admin/bookings/${bookingId}/complete`);
    return response.data.data;
  },

  // SSE Stream Connections
  getTenantStreamUrl: (): string => {
    const baseURL = apiClient.defaults.baseURL || 'http://localhost:8080/api/v1';
    return `${baseURL}/bookings/stream`;
  },

  getAdminStreamUrl: (): string => {
    const baseURL = apiClient.defaults.baseURL || 'http://localhost:8080/api/v1';
    return `${baseURL}/admin/bookings/stream`;
  }
};
