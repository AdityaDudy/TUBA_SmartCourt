export interface InvoiceLineItem {
  id?: number;
  feeType: string;
  description: string;
  amount: number;
  sourceReference?: string;
}

export interface Payment {
  id?: number;
  invoiceId: number;
  amount: number;
  paymentDate: string;
  mode: string;
  referenceNo?: string;
}

export interface Expense {
  id?: number;
  matterId?: number;
  matterTitle?: string;
  clientId?: number;
  clientName?: string;
  createdById?: number;
  createdByName?: string;
  category: string;
  amount: number;
  date: string;
  billable: boolean;
  invoiced: boolean;
  invoiceId?: number;
  receiptPath?: string;
}

export interface PendingBillable {
  id: string;
  type: 'Filing' | 'Hearing' | 'Task' | 'Expense';
  title: string;
  description: string;
  suggestedAmount: number;
  date: string;
  matterId?: number;
  matterTitle?: string;
  clientId?: number;
  clientName?: string;
}

export interface Invoice {
  id: number;
  invoiceNo?: string;
  clientId?: number;
  clientName?: string;
  matterId?: number;
  matterTitle?: string;
  amount: number;
  paidAmount?: number;
  status: 'Paid' | 'Unpaid' | 'Overdue' | 'Partially Paid' | 'Draft';
  dueDate?: string;
  paidDate?: string;
  description?: string;
  lineItems?: InvoiceLineItem[];
}
