// ── Paginated Response ─────────────────────────────────────────
export interface PagedResponse<T> {
  content:       T[];
  totalElements: number;
  totalPages:    number;
  page:          number;
  size:          number;
  first:         boolean;
  last:          boolean;
}

// ── Generic API Response wrapper ───────────────────────────────
export interface ApiResponse<T> {
  success: boolean;
  data:    T;
  message?: string;
  errors?: string[];
  timestamp?: string;
}

// ── Query Params ───────────────────────────────────────────────
export interface ListParams {
  page?:    number;
  size?:    number;
  sort?:    string;
  order?:   'asc' | 'desc';
  search?:  string;
  filter?:  Record<string, string | string[]>;
}

// ── Dashboard Stats DTO ────────────────────────────────────────
export interface DashboardStatsDto {
  activeMatters:     number;
  closedThisYear:    number;
  hearingsToday:     number;
  hearingsThisWeek:  number;
  pendingFilings:    number;
  openTasks:         number;
  urgentHearings:    number;
  overdueInvoices:   number;
  totalOutstanding:  number;
  totalCollected:    number;
  nextHearingDate?:  string;
}

// ── Login DTO ──────────────────────────────────────────────────
export interface LoginRequest {
  email:    string;
  password: string;
}

export interface LoginResponse {
  accessToken:  string;
  refreshToken: string;
  tokenType:    string;
  expiresIn:    number;
  user:         UserProfileDto;
}

export interface UserProfileDto {
  id:          number;
  name:        string;
  email:       string;
  role:        string;
  department:  string;
  mfa:         boolean;
  avatar?:     string;
  initials:    string;
  gradient:    string;
  permissions: string[];
}

// ── Master Data DTO ────────────────────────────────────────────
export interface MastersDto {
  courts:        string[];
  matterTypes:   string[];
  practiceAreas: string[];
  stages:        string[];
  taskTypes:     string[];
  priorities:    string[];
  docTypes:      string[];
  filingStages:  string[];
  clientTypes:   string[];
  designations:  string[];
  departments:   string[];
  advocates:     string[];
}

// ── Tracker DTOs ─────────────────────────────────────────────────
export interface PartyDto {
  name:      string;
  advocate?: string;
}

export interface HearingDto {
  hearingDate:       string | null;
  judge?:            string;
  purposeOfHearing?: string;
  nextHearingDate?:  string;
  businessRemarks?:  string;
}

export interface OrderDto {
  id:             number;
  orderDate?:     string;
  orderNo?:       string;
  orderType?:     string;
  orderCategory?: 'JUDGMENT' | 'INTERIM';
  downloadUrl?:   string;
  fileSize?:      number;
  mimeType?:      string;
}

export interface CaseDetailResponse {
  cnr:               string;
  caseType?:         string;
  filingNo?:         string;
  filingDate?:       string;
  registrationNo?:   string;
  registrationDate?: string;
  courtName?:        string;
  courtComplex?:     string;
  judgeName?:        string;
  /** PENDING | DISPOSED | STAYED | DISMISSED | TRANSFERRED */
  caseStatus?:       string;
  stageOfCase?:      string;
  nextHearingDate?:  string;
  actsAndSections?:  string[];
  firNo?:            string;
  firYear?:          string;
  policeStation?:    string;
  petitioners?:      PartyDto[];
  respondents?:      PartyDto[];
  hearings?:         HearingDto[];
  orders?:           OrderDto[];
  matterId?:         number;
  matterTitle?:      string;
  alertActive?:      boolean;
  /** 'CACHE' = stale; 'LIVE' = fresh from provider */
  cacheSource?:      string;
  lastSyncedAt?:     string;
}

export interface ScrapeJobStatusResponse {
  jobId?:           number;
  cnr?:             string;
  /** PENDING | RUNNING | DONE | FAILED */
  status:           string;
  errorMessage?:    string;
  result?:          CaseDetailResponse;
}

export interface RecentSearchDto {
  jobId:       number;
  cnr:         string;
  status:      string;
  searchedAt:  string;
  caseTitle?:  string;
}

/** Lightweight candidate row returned by GET /api/tracker/search-advanced */
export interface CaseSearchResultDto {
  cnr:          string;
  caseType?:    string;
  petitioners?: string[];
  respondents?: string[];
  filingDate?:  string;
  courtName?:   string;
  courtCode?:   string;
}

export interface MatterSuggestion {
  matterId:   number;
  matterTitle: string;
  confidence: string;
}

/** @deprecated — use CaseDetailResponse */
export interface TrackerResultDto extends Partial<CaseDetailResponse> {}

// ── Document Folder DTO ────────────────────────────────────────
export interface DocumentFolderDto {
  clientId:   string;
  clientName: string;
  count:      number;
  bg:         string;
  tc:         string;
  lastUpdated: string;
}

// ── Revenue DTO ────────────────────────────────────────────────
export interface RevenueDto {
  month:       string;
  year:        number;
  collected:   number;
  outstanding: number;
  overdue:     number;
  weeklyData:  { label: string; amount: number }[];
}

// ── Team Performance DTO ───────────────────────────────────────
export interface TeamPerformanceDto {
  name:        string;
  tasksOpen:   number;
  tasksTotal:  number;
  percentage:  number;
  role:        string;
}

// ── Diary Event DTO ────────────────────────────────────────────
export interface DiaryEventDto {
  id:          any;
  title:       string;
  date:        string;
  eventDate?:  string;
  time?:       string;
  eventTime?:  string;
  type:        'hearing' | 'meeting' | 'task' | 'deadline';
  matterId?:   number | string;
  matterTitle?: string;
  clientId?:   number | string;
  clientName?: string;
  ownerId?:    number;
  ownerName?:  string;
  createdBy?:  number;
  court?:      string;
  notes?:      string;
  urgent:      boolean;
}

export interface DiaryScopeMember {
  id:       number;
  name:     string;
  email:    string;
  role:     string;
  initials: string;
  gradient: string;
}

export interface DiaryScopeOptions {
  canTeam:     boolean;
  canOrg:      boolean;
  teamMembers: DiaryScopeMember[];
  orgMembers:  DiaryScopeMember[];
}
