# TUBA SmartCourt — Page API Integration Specification

This document details all backend REST API endpoints, request payloads, response DTO structures, and component initiator triggers used across the **Dashboard Page**, **Advocate Diary Page**, and **Court Tracker Page**.

---

## 1. Dashboard Page (`/app/dashboard`)

### 1.1 `GET /api/dashboard/stats`
* **Initiator Component / Function**: `DashboardPageComponent.ngOnInit()` → `DataService.loadDashboardStats()`
* **HTTP Method**: `GET`
* **URL**: `/api/dashboard/stats`
* **Request Query Params / Payload**: *None*
* **Response Payload Format**:
```json
{
  "status": 200,
  "message": "Success",
  "data": {
    "activeMatters": 7,
    "closedThisYear": 0,
    "hearingsToday": 0,
    "hearingsThisWeek": 0,
    "openTasks": 1,
    "urgentHearings": 0,
    "pendingFilings": 0,
    "totalOutstanding": 15000.0,
    "totalCollected": 45000.0,
    "overdueInvoices": 1
  }
}
```

---

### 1.2 `GET /api/hearings/today`
* **Initiator Component / Function**: `DashboardPageComponent.ngOnInit()` → `DataService.loadHearings()`
* **HTTP Method**: `GET`
* **URL**: `/api/hearings/today`
* **Request Query Params / Payload**: *None*
* **Response Payload Format**:
```json
{
  "status": 200,
  "data": [
    {
      "id": 101,
      "matterId": 12,
      "caseTitle": "State of Delhi vs. Rahul Sharma",
      "court": "Delhi High Court",
      "bench": "Court No. 3 (Bench B)",
      "hearingDate": "2026-08-03",
      "hearingTime": "10:30 AM",
      "status": "Urgent",
      "purpose": "Arguments",
      "judgeName": "Hon'ble Mr. Justice A.K. Roy"
    }
  ]
}
```

---

### 1.3 `GET /api/matters`
* **Initiator Component / Function**: `DashboardPageComponent.ngOnInit()` → `DataService.loadMatters()`
* **HTTP Method**: `GET`
* **URL**: `/api/matters`
* **Request Query Params / Payload**: *None*
* **Response Payload Format**:
```json
{
  "status": 200,
  "data": [
    {
      "id": 12,
      "title": "State of Delhi vs. Rahul Sharma",
      "caseNo": "CRL.A. 403/2025",
      "cnrNumber": "DLHC010045122025",
      "clientId": 5,
      "clientName": "Rahul Sharma",
      "court": "Delhi High Court",
      "type": "Criminal Appeal",
      "status": "Active",
      "advocate": "Adv. Amit Sharma"
    }
  ]
}
```

---

### 1.4 `GET /api/tasks`
* **Initiator Component / Function**: `DashboardPageComponent.ngOnInit()` → `DataService.loadTasks()`
* **HTTP Method**: `GET`
* **URL**: `/api/tasks`
* **Request Query Params / Payload**: *None*
* **Response Payload Format**:
```json
{
  "status": 200,
  "data": [
    {
      "id": 44,
      "title": "File Rejoinder Affidavit",
      "matterId": 12,
      "matterTitle": "State of Delhi vs. Rahul Sharma",
      "assignedTo": "Adv. Amit Sharma",
      "priority": "High",
      "dueDate": "2026-08-05",
      "done": false,
      "status": "Open"
    }
  ]
}
```

---

### 1.5 `GET /api/filings`
* **Initiator Component / Function**: `DashboardPageComponent.ngOnInit()` → `DataService.loadFilings()`
* **HTTP Method**: `GET`
* **URL**: `/api/filings`
* **Request Query Params / Payload**: *None*
* **Response Payload Format**:
```json
{
  "status": 200,
  "data": [
    {
      "id": 8,
      "title": "Interlocutory Application for Stay",
      "matterId": 12,
      "matterTitle": "State of Delhi vs. Rahul Sharma",
      "court": "Delhi High Court",
      "status": "Draft",
      "dueDate": "2026-08-04",
      "advocate": "Adv. Amit Sharma"
    }
  ]
}
```

---

### 1.6 `GET /api/dashboard/timeline`
* **Initiator Component / Function**: `DashboardPageComponent.ngOnInit()` → `DataService.getTimeline()`
* **HTTP Method**: `GET`
* **URL**: `/api/dashboard/timeline`
* **Request Query Params / Payload**: *None*
* **Response Payload Format**:
```json
{
  "status": 200,
  "data": [
    {
      "date": "2026-08-03",
      "time": "09:30 AM",
      "title": "State of Delhi vs. Rahul Sharma",
      "sub": "Delhi High Court · Bench B",
      "urgent": true
    },
    {
      "date": "2026-08-03",
      "time": "11:30 AM",
      "title": "Client Briefing — Commercial Arbitration",
      "sub": "Diary Appointment · Apex Builders Matter",
      "urgent": false
    }
  ]
}
```

---

### 1.7 `GET /api/dashboard/team-performance`
* **Initiator Component / Function**: `DashboardPageComponent.ngOnInit()` → `DataService.getTeamPerformance()`
* **HTTP Method**: `GET`
* **URL**: `/api/dashboard/team-performance`
* **Request Query Params / Payload**: *None*
* **Response Payload Format**:
```json
{
  "status": 200,
  "data": [
    {
      "name": "Adv. Amit Sharma",
      "tasksOpen": 8,
      "tasksTotal": 15,
      "percentage": 53,
      "role": "admin"
    },
    {
      "name": "Adv. Priya Kapoor",
      "tasksOpen": 5,
      "tasksTotal": 12,
      "percentage": 42,
      "role": "senior"
    }
  ]
}
```

---

### 1.8 `GET /api/dashboard/court-distribution`
* **Initiator Component / Function**: `DashboardPageComponent.ngOnInit()` → `DataService.getCourtDistribution()`
* **HTTP Method**: `GET`
* **URL**: `/api/dashboard/court-distribution`
* **Request Query Params / Payload**: *None*
* **Response Payload Format**:
```json
{
  "status": 200,
  "data": [
    {
      "court": "Delhi High Court",
      "count": 4,
      "pct": 57
    },
    {
      "court": "Supreme Court of India",
      "count": 3,
      "pct": 43
    }
  ]
}
```

---

### 1.9 `GET /api/dashboard/revenue`
* **Initiator Component / Function**: `DashboardPageComponent.ngOnInit()` → `DataService.getRevenue()`
* **HTTP Method**: `GET`
* **URL**: `/api/dashboard/revenue`
* **Request Query Params / Payload**: *None*
* **Response Payload Format**:
```json
{
  "status": 200,
  "data": {
    "month": "Aug",
    "year": 2026,
    "collected": 45000.0,
    "outstanding": 15000.0,
    "overdue": 5000.0,
    "weeklyData": [
      { "label": "Mon", "amount": 6750 },
      { "label": "Tue", "amount": 11250 },
      { "label": "Wed", "amount": 4500 },
      { "label": "Thu", "amount": 9000 },
      { "label": "Fri", "amount": 9000 },
      { "label": "Sat", "amount": 3150 },
      { "label": "Sun", "amount": 1350 }
    ]
  }
}
```

---

### 1.10 `PUT /api/tasks/{id}/toggle-done`
* **Initiator Component / Function**: `DashboardPageComponent.toggleTask(t)` → `DataService.toggleTaskDone(id)`
* **HTTP Method**: `PUT`
* **URL**: `/api/tasks/{id}/toggle-done`
* **Request Payload**: `{}`
* **Response Payload Format**:
```json
{
  "status": 200,
  "data": {
    "id": 44,
    "title": "File Rejoinder Affidavit",
    "done": true,
    "status": "Completed"
  }
}
```

---

## 2. Advocate Diary Page (`/app/diary`)

### 2.1 `GET /api/diary/scope-options`
* **Initiator Component / Function**: `DiaryPageComponent.ngOnInit()` → `DataService.getDiaryScopeOptions()`
* **HTTP Method**: `GET`
* **URL**: `/api/diary/scope-options`
* **Request Query Params / Payload**: *None*
* **Response Payload Format**:
```json
{
  "status": 200,
  "data": {
    "canTeam": true,
    "canOrg": true,
    "teamMembers": [
      {
        "id": 1,
        "name": "Adv. Amit Sharma",
        "email": "amit@tubalaw.com",
        "role": "Admin",
        "initials": "AS",
        "gradient": "linear-gradient(135deg,#b45309,#d97706)"
      }
    ],
    "orgMembers": []
  }
}
```

---

### 2.2 `GET /api/diary/events`
* **Initiator Component / Function**: `DiaryPageComponent.loadEvents()` → `DataService.getDiaryEvents(year, month, scope, memberId)`
* **HTTP Method**: `GET`
* **URL**: `/api/diary/events?year=2026&month=8&scope=own`
* **Request Query Parameters**:
  - `year`: `2026`
  - `month`: `8`
  - `scope`: `own` | `team` | `org`
  - `memberId` (optional): `1`
* **Response Payload Format**:
```json
{
  "status": 200,
  "data": [
    {
      "id": 302,
      "title": "Client Strategy Meeting",
      "eventDate": "2026-08-04",
      "eventTime": "14:00",
      "type": "meeting",
      "matterId": 12,
      "matterTitle": "State of Delhi vs. Rahul Sharma",
      "clientName": "Rahul Sharma",
      "court": "Chamber 402, High Court Block",
      "notes": "Discuss cross-examination strategy for PW-1.",
      "urgent": true,
      "ownerId": 1,
      "ownerName": "Adv. Amit Sharma"
    }
  ]
}
```

---

### 2.3 `POST /api/diary/events`
* **Initiator Component / Function**: `DiaryPageComponent.save()` → `DataService.createDiaryEvent(payload)`
* **HTTP Method**: `POST`
* **URL**: `/api/diary/events`
* **Request Payload**:
```json
{
  "title": "Pre-Trial Conference with Senior Counsel",
  "eventDate": "2026-08-06",
  "eventTime": "16:30",
  "type": "conference",
  "matterId": 12,
  "court": "Delhi High Court",
  "notes": "Finalize list of documents.",
  "urgent": false
}
```
* **Response Payload Format**:
```json
{
  "status": 201,
  "message": "Event added to diary.",
  "data": {
    "id": 303,
    "title": "Pre-Trial Conference with Senior Counsel",
    "eventDate": "2026-08-06",
    "eventTime": "16:30",
    "type": "conference",
    "matterId": 12,
    "matterTitle": "State of Delhi vs. Rahul Sharma",
    "ownerId": 1,
    "ownerName": "Adv. Amit Sharma"
  }
}
```

---

### 2.4 `PUT /api/diary/events/{id}`
* **Initiator Component / Function**: `DiaryPageComponent.saveEdit()` → `DataService.updateDiaryEvent(id, payload)`
* **HTTP Method**: `PUT`
* **URL**: `/api/diary/events/303`
* **Request Payload**:
```json
{
  "title": "Updated: Pre-Trial Conference with Senior Counsel",
  "eventTime": "17:00",
  "urgent": true
}
```
* **Response Payload Format**:
```json
{
  "status": 200,
  "data": {
    "id": 303,
    "title": "Updated: Pre-Trial Conference with Senior Counsel",
    "eventTime": "17:00",
    "urgent": true
  }
}
```

---

### 2.5 `DELETE /api/diary/events/{id}`
* **Initiator Component / Function**: `DiaryPageComponent.deleteEvent(e)` → `DataService.deleteDiaryEvent(id)`
* **HTTP Method**: `DELETE`
* **URL**: `/api/diary/events/303`
* **Request Payload**: *None*
* **Response Payload Format**:
```json
{
  "status": 200,
  "data": null
}
```

---

### 2.6 `GET /api/hearings/filter`
* **Initiator Component / Function**: `DiaryPageComponent.loadEvents()` → `DataService.filterHearings('All')`
* **HTTP Method**: `GET`
* **URL**: `/api/hearings/filter?type=All`
* **Request Query Parameters**: `type=All`
* **Response Payload Format**:
```json
{
  "status": 200,
  "data": [
    {
      "id": 101,
      "matterId": 12,
      "caseTitle": "State of Delhi vs. Rahul Sharma",
      "court": "Delhi High Court",
      "hearingDate": "2026-08-04",
      "hearingTime": "10:30 AM",
      "status": "Scheduled"
    }
  ]
}
```

---

## 3. Court Tracker Pages (`/app/tracker` & `/app/tracker/:cnr`)

### 3.1 `GET /api/masters`
* **Initiator Component / Function**: `TrackerPageComponent.ngOnInit()` → `DataService.loadMasters()`
* **HTTP Method**: `GET`
* **URL**: `/api/masters`
* **Request Query Params / Payload**: *None*
* **Response Payload Format**:
```json
{
  "status": 200,
  "data": {
    "courts": ["Supreme Court of India", "Delhi High Court", "Bombay High Court", "Madras High Court", "NCLT Mumbai"],
    "caseTypes": ["CRL.A.", "CS(OS)", "W.P.(C)", "COMP.APP"],
    "practiceAreas": ["Criminal Law", "Commercial Disputes", "Constitutional Law"]
  }
}
```

---

### 3.2 `GET /api/tracker/history`
* **Initiator Component / Function**: `TrackerPageComponent.ngOnInit()` → `DataService.getRecentSearches()`
* **HTTP Method**: `GET`
* **URL**: `/api/tracker/history`
* **Request Query Params / Payload**: *None*
* **Response Payload Format**:
```json
{
  "status": 200,
  "data": [
    {
      "cnr": "DLHC010045122025",
      "caseTitle": "State of Delhi vs. Rahul Sharma",
      "court": "Delhi High Court",
      "searchedAt": "2026-08-02T15:24:00Z"
    }
  ]
}
```

---

### 3.3 `GET /api/tracker/search?cnr={cnr}`
* **Initiator Component / Function**: `TrackerPageComponent.search()` → `DataService.searchByCnr(cnr)`
* **HTTP Method**: `GET`
* **URL**: `/api/tracker/search?cnr=DLHC010045122025`
* **Request Query Parameters**: `cnr=DLHC010045122025`
* **Response Payload Format (Async Queue - 202 Accepted)**:
```json
{
  "status": 202,
  "data": {
    "jobId": 9042,
    "cnr": "DLHC010045122025",
    "status": "RUNNING",
    "message": "Scrape worker dispatched to court server portal."
  }
}
```
* **Response Payload Format (Cached - 200 OK)**:
```json
{
  "status": 200,
  "data": {
    "status": "DONE",
    "cnr": "DLHC010045122025",
    "result": {
      "cnr": "DLHC010045122025",
      "caseNumber": "CRL.A. 403/2025",
      "court": "Delhi High Court",
      "caseStatus": "PENDING",
      "nextHearingDate": "2026-08-10"
    }
  }
}
```

---

### 3.4 `GET /api/tracker/search-by-case-number`
* **Initiator Component / Function**: `TrackerPageComponent.search()` → `DataService.searchByCaseNumber(caseType, number, year)`
* **HTTP Method**: `GET`
* **URL**: `/api/tracker/search-by-case-number?caseType=CS(OS)&number=403&year=2026`
* **Request Query Parameters**:
  - `caseType`: `CS(OS)`
  - `number`: `403`
  - `year`: `2026`
* **Response Payload Format**:
```json
{
  "status": 200,
  "data": {
    "jobId": 9043,
    "cnr": "DLHC010040302026",
    "status": "DONE",
    "result": {
      "cnr": "DLHC010040302026",
      "caseNumber": "CS(OS) 403/2026",
      "court": "Delhi High Court"
    }
  }
}
```

---

### 3.5 `GET /api/tracker/jobs/{jobId}/status`
* **Initiator Component / Function**: `TrackerPageComponent.startPolling(jobId)` → `DataService.pollJobStatus(jobId)`
* **HTTP Method**: `GET`
* **URL**: `/api/tracker/jobs/9042/status`
* **Request Query Params / Payload**: *None*
* **Response Payload Format**:
```json
{
  "status": 200,
  "data": {
    "jobId": 9042,
    "cnr": "DLHC010045122025",
    "status": "DONE",
    "errorMessage": null
  }
}
```

---

### 3.6 `GET /api/tracker/detail/{cnr}`
* **Initiator Component / Function**: `CaseDetailPageComponent.loadDetail()` → `DataService.getCaseDetail(cnr)`
* **HTTP Method**: `GET`
* **URL**: `/api/tracker/detail/DLHC010045122025`
* **Request Query Params / Payload**: *None*
* **Response Payload Format**:
```json
{
  "status": 200,
  "data": {
    "cnr": "DLHC010045122025",
    "caseNumber": "CRL.A. 403/2025",
    "filingNumber": "F/1029/2025",
    "filingDate": "2025-01-15",
    "registrationDate": "2025-01-16",
    "firstHearingDate": "2025-01-20",
    "nextHearingDate": "2026-08-10",
    "caseStatus": "PENDING",
    "court": "Delhi High Court",
    "bench": "Court 3",
    "judgeName": "Hon'ble Mr. Justice A.K. Roy",
    "petitioners": [{ "name": "State of Delhi", "advocate": "Public Prosecutor" }],
    "respondents": [{ "name": "Rahul Sharma", "advocate": "Adv. Amit Sharma" }],
    "orders": [
      {
        "id": 701,
        "orderDate": "2026-06-12",
        "orderCategory": "INTERIM_ORDER",
        "pdfUrl": "/api/tracker/orders/DLHC010045122025/701/download",
        "fileSizeBytes": 1048576
      }
    ],
    "history": [
      {
        "businessDate": "2026-06-12",
        "hearingPurpose": "Arguments on Bail Application",
        "judgeName": "Hon'ble Mr. Justice A.K. Roy",
        "nextDate": "2026-08-10"
      }
    ],
    "alertActive": true,
    "matterId": 12,
    "matterTitle": "State of Delhi vs. Rahul Sharma",
    "cacheSource": "DB",
    "lastFetchedAt": "2026-08-03T10:45:00Z"
  }
}
```

---

### 3.7 `POST /api/tracker/refresh/{cnr}`
* **Initiator Component / Function**: `CaseDetailPageComponent.refresh()` → `DataService.refreshCase(cnr)`
* **HTTP Method**: `POST`
* **URL**: `/api/tracker/refresh/DLHC010045122025`
* **Request Payload**: `{}`
* **Response Payload Format**:
```json
{
  "status": 200,
  "message": "Refresh queued — updating from court servers...",
  "data": {
    "jobId": 9045,
    "status": "RUNNING"
  }
}
```

---

### 3.8 `POST /api/tracker/alert/{cnr}`
* **Initiator Component / Function**: `CaseDetailPageComponent.toggleAlert()` → `DataService.toggleAlert(cnr, enabled)`
* **HTTP Method**: `POST`
* **URL**: `/api/tracker/alert/DLHC010045122025`
* **Request Payload**:
```json
{
  "enabled": true
}
```
* **Response Payload Format**:
```json
{
  "status": 200,
  "message": "Case alerts enabled.",
  "data": null
}
```

---

### 3.9 `GET /api/tracker/suggest-matter/{cnr}`
* **Initiator Component / Function**: `CaseDetailPageComponent.loadMatterSuggestion()` → `DataService.suggestMatterLink(cnr)`
* **HTTP Method**: `GET`
* **URL**: `/api/tracker/suggest-matter/DLHC010045122025`
* **Request Query Params / Payload**: *None*
* **Response Payload Format**:
```json
{
  "status": 200,
  "data": {
    "matterId": 12,
    "matterTitle": "State of Delhi vs. Rahul Sharma",
    "confidenceScore": 0.95,
    "matchReason": "Case number matches CRL.A. 403/2025"
  }
}
```

---

### 3.10 `POST /api/tracker/link-matter/{cnr}`
* **Initiator Component / Function**: `CaseDetailPageComponent.confirmMatterLink()` → `DataService.linkCaseToMatter(cnr, matterId)`
* **HTTP Method**: `POST`
* **URL**: `/api/tracker/link-matter/DLHC010045122025`
* **Request Payload**:
```json
{
  "matterId": 12
}
```
* **Response Payload Format**:
```json
{
  "status": 200,
  "message": "Case linked to matter.",
  "data": null
}
```

---

### 3.11 `GET /api/tracker/orders/{cnr}/{orderId}/download`
* **Initiator Component / Function**: `CaseDetailPageComponent.downloadOrder(order)` → `DataService.downloadOrder(cnr, orderId)`
* **HTTP Method**: `GET`
* **URL**: `/api/tracker/orders/DLHC010045122025/701/download`
* **Request Query Params / Payload**: *None*
* **Response Header**: `Content-Type: application/pdf`, `Content-Disposition: attachment; filename="Order-2026-06-12.pdf"`
* **Response Body**: Binary PDF File Stream

---

### 3.12 `POST /api/tracker/orders/{cnr}/{orderId}/save-to-matter`
* **Initiator Component / Function**: `CaseDetailPageComponent.saveToMatterVault(order)` → `DataService.saveOrderToMatterVault(cnr, orderId)`
* **HTTP Method**: `POST`
* **URL**: `/api/tracker/orders/DLHC010045122025/701/save-to-matter`
* **Request Payload**: `{}`
* **Response Payload Format**:
```json
{
  "status": 200,
  "message": "Order saved to Matter Document Vault!",
  "data": {
    "saved": true,
    "docId": 1042,
    "alreadyExisted": false
  }
}
```

---

### 3.13 `GET /api/tracker/download-all/{cnr}`
* **Initiator Component / Function**: `CaseDetailPageComponent.downloadAll()` → `DataService.downloadAllOrders(cnr)`
* **HTTP Method**: `GET`
* **URL**: `/api/tracker/download-all/DLHC010045122025`
* **Request Query Params / Payload**: *None*
* **Response Header**: `Content-Type: application/zip`, `Content-Disposition: attachment; filename="Orders-DLHC010045122025.zip"`
* **Response Body**: Binary ZIP Archive Stream

---

### 3.14 `GET /api/tracker/export/{cnr}?format=pdf`
* **Initiator Component / Function**: `CaseDetailPageComponent.exportPdf()` → `DataService.exportCase(cnr, 'pdf')`
* **HTTP Method**: `GET`
* **URL**: `/api/tracker/export/DLHC010045122025?format=pdf`
* **Request Query Parameters**: `format=pdf`
* **Response Header**: `Content-Type: application/pdf`, `Content-Disposition: attachment; filename="CaseSummary-DLHC010045122025.pdf"`
* **Response Body**: Binary PDF Summary Report Stream
