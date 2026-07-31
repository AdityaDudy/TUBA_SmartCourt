export interface Client {
  id: string;
  code?: string;
  name: string;
  type: ClientType;
  mobile: string;
  email: string;
  pan: string;
  gst?: string;
  gstin?: string;
  aadhar?: string;
  matters?: number;
  activeMatterCount?: number;
  status: 'Active' | 'Inactive';
  address?: string;
  initials?: string;
  gradient?: string;

  // Extended client profile fields
  displayName?: string;
  dob?: string;
  gender?: string;
  fatherSpouseName?: string;
  alternateMobile?: string;
  billingAddress?: string;
  idProofType?: string;
  idProofNumber?: string;
  assignedAdvocate?: string;
  clientSince?: string;
  referralSource?: string;

  vakalatnamaOnFile?: boolean;
  engagementLetterSigned?: boolean;
  conflictNotes?: string;
  dataConsent?: boolean;

  // Corporate Extension fields
  cin?: string;
  registeredOfficeAddress?: string;
  authorizedSignatoryName?: string;
  authorizedSignatoryDesignation?: string;
  incorporationDate?: string;
  createdBy?: string;
}

export type ClientType = string;
