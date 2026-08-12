export interface Matter {
  id: string;
  title: string;
  client: string;
  clientName?: string;
  clientId?: number;
  court: string;
  type: MatterType;
  area: string;
  nextHearing: string;
  nextHearingDate?: string;
  adv: string;
  advocate?: string;
  status: 'Active' | 'Closed' | 'Archived' | 'Stayed' | 'Urgent';
  caseNo: string;
  caseNumber?: string;
  cnrNumber?: string;
  stage: string;
  bg: string;
  filingDate?: string;
  oppositeParty?: string;
  priority?: string;
  coCounsel?: string;
  opposingCounsel?: string;
  limitationDeadline?: string;
  relatedMatterId?: string;
}

export type MatterType = string;
