export interface Hearing {
  id?: string;
  matterId?: number;
  hearingDate?: string;
  sr?: number;
  title?: string;
  caseTitle?: string;
  caseNo?: string;
  caseNumber?: string;
  court?: string;
  bench?: string;
  time?: string;
  hearingTime?: string;
  stage?: string;
  adv?: string;
  advocate?: string;
  status?: 'Urgent' | 'Ready' | 'Pending' | 'Filed' | 'Scheduled' | 'Completed' | string;
  type?: 'SC' | 'HC' | 'Tribunal' | 'Other' | string;
}
