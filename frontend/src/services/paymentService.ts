import { apiClient } from './api';

// ──────────────────────────────────────────────
// TypeScript Interfaces
// ──────────────────────────────────────────────

export interface PaymentDto {
  id: string;
  bookingId: string;
  tenantId: string;
  ownerId: string;
  amount: number;
  currency: string;
  status: 'PENDING' | 'CAPTURED' | 'FAILED' | 'REFUNDED';
  gatewayProvider: string;
  gatewayPaymentId?: string;
  idempotencyKey?: string;
  riskScore?: number;
  riskDecision?: string;
  createdAt?: string;
}

export interface RefundDto {
  id: string;
  paymentId: string;
  amount: number;
  currency: string;
  status: 'PENDING' | 'SUCCEEDED' | 'FAILED';
  gatewayRefundId?: string;
  reason?: string;
}

export interface InvoiceDto {
  id: string;
  invoiceNumber: string;
  bookingId: string;
  paymentId: string;
  refundId?: string;
  type: 'RECEIPT' | 'REFUND_RECEIPT';
  amount: number;
  currency: string;
  pdfPath?: string;
  createdAt?: string;
}

export interface EscrowAccountDto {
  id: string;
  bookingId: string;
  paymentId: string;
  tenantId: string;
  ownerId: string;
  balance: number;
  currency: string;
  status: 'HELD' | 'RELEASED' | 'REFUNDED';
  heldAt?: string;
  releasedAt?: string;
  expectedReleaseAt?: string;
}

export interface PayoutDto {
  id: string;
  ownerId: string;
  amount: number;
  currency: string;
  status: 'PENDING' | 'PROCESSING' | 'SUCCEEDED' | 'FAILED';
  gatewayPayoutId?: string;
  destinationAccount?: string;
  createdAt?: string;
}

export interface DisputeDto {
  id: string;
  paymentId: string;
  reason: string;
  amount: number;
  currency: string;
  status: string;
  evidenceJson?: string;
  createdAt?: string;
}

export interface WebhookDto {
  id: string;
  gatewayProvider: string;
  eventType: string;
  processed: boolean;
  processedAt?: string;
  errorReason?: string;
  payload?: string;
  createdAt?: string;
}

// ──────────────────────────────────────────────
// Payload Interfaces
// ──────────────────────────────────────────────

export interface InitiatePaymentPayload {
  bookingId: string;
  amount: number;
  currency: string;
  gatewayProvider: 'STRIPE' | 'RAZORPAY' | 'CASHFREE';
  idempotencyKey?: string;
}

export interface InitiateRefundPayload {
  amount: number;
  reason?: string;
}

export interface GenerateInvoicePayload {
  bookingId: string;
  paymentId: string;
  type: 'RECEIPT' | 'REFUND_RECEIPT';
  refundId?: string;
}

export interface InitiatePayoutPayload {
  amount: number;
  currency: string;
  destinationAccount: string;
}

// ──────────────────────────────────────────────
// Payment Service
// ──────────────────────────────────────────────

export const paymentService = {

  // Payments
  initiatePayment: async (data: InitiatePaymentPayload): Promise<PaymentDto> => {
    const headers: Record<string, string> = {};
    if (data.idempotencyKey) {
      headers['Idempotency-Key'] = data.idempotencyKey;
    }
    const response = await apiClient.post('/payments', data, { headers });
    return response.data.data;
  },

  capturePayment: async (paymentId: string, gatewayPaymentId: string): Promise<PaymentDto> => {
    const response = await apiClient.post(`/admin/payments/${paymentId}/capture`, { gatewayPaymentId });
    return response.data.data;
  },

  failPayment: async (paymentId: string, errorReason: string): Promise<PaymentDto> => {
    const response = await apiClient.post(`/admin/payments/${paymentId}/fail`, { errorReason });
    return response.data.data;
  },

  getMyPayments: async (): Promise<PaymentDto[]> => {
    const response = await apiClient.get('/payments/my');
    return response.data.data;
  },

  getPayment: async (paymentId: string): Promise<PaymentDto> => {
    const response = await apiClient.get(`/payments/${paymentId}`);
    return response.data.data;
  },

  // Refunds
  initiateRefund: async (paymentId: string, data: InitiateRefundPayload): Promise<RefundDto> => {
    const response = await apiClient.post(`/payments/${paymentId}/refunds`, data);
    return response.data.data;
  },

  // Invoices
  getInvoice: async (invoiceId: string): Promise<InvoiceDto> => {
    const response = await apiClient.get(`/payments/invoices/${invoiceId}`);
    return response.data.data;
  },

  getMyInvoices: async (): Promise<InvoiceDto[]> => {
    const response = await apiClient.get('/payments/invoices/my');
    return response.data.data;
  },

  generateInvoice: async (data: GenerateInvoicePayload): Promise<InvoiceDto> => {
    const response = await apiClient.post('/payments/invoices/generate', data);
    return response.data.data;
  },

  getInvoicePdfUrl: (invoiceId: string): string => {
    const baseURL = apiClient.defaults.baseURL || 'http://localhost:8080/api/v1';
    return `${baseURL}/payments/invoices/${invoiceId}/pdf`;
  },

  // Escrow (Owner)
  getOwnerEscrowAccounts: async (): Promise<EscrowAccountDto[]> => {
    const response = await apiClient.get('/owner/payments/escrow');
    return response.data.data;
  },

  releaseFunds: async (escrowId: string): Promise<EscrowAccountDto> => {
    const response = await apiClient.post(`/admin/payments/escrow/${escrowId}/release`);
    return response.data.data;
  },

  // Payouts (Owner)
  initiatePayout: async (data: InitiatePayoutPayload): Promise<PayoutDto> => {
    const response = await apiClient.post('/owner/payments/payouts', data);
    return response.data.data;
  },

  getOwnerPayouts: async (): Promise<PayoutDto[]> => {
    const response = await apiClient.get('/owner/payments/payouts');
    return response.data.data;
  },

  // Admin
  getAllPayments: async (): Promise<PaymentDto[]> => {
    const response = await apiClient.get('/admin/payments');
    return response.data.data;
  },

  getAllDisputes: async (): Promise<DisputeDto[]> => {
    const response = await apiClient.get('/admin/payments/disputes');
    return response.data.data;
  },

  getAllWebhooks: async (): Promise<WebhookDto[]> => {
    const response = await apiClient.get('/admin/payments/webhooks');
    return response.data.data;
  },

  retryWebhook: async (webhookId: string): Promise<void> => {
    await apiClient.post(`/admin/payments/webhooks/${webhookId}/retry`);
  },
};
