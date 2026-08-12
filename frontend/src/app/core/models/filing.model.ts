export interface Filing {
  id: string;
  matterId?: number;
  doc: string;
  title: string;
  matter: string;
  matterTitle?: string;
  filingType?: string;
  court: string;
  due: string;
  dueDate?: string;
  filedDate?: string;
  advocate?: string;
  stage: string;
  status: string;
  description?: string;
  notes?: string;
  s3Url?: string;
  source?: string;  // "task" = from task submission, "manual" = created via Filings form
  createdAt?: string;
}

export type FilingStage =
  | 'Draft'
  | 'Under Review'
  | 'Approved'
  | 'Signed'
  | 'Filed'
  | 'Defects Raised'
  | 'Defects Cleared'
  | 'Returned';
