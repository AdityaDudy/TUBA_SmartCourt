import os
import subprocess
import sys

html_content = """<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>TUBA SmartCourt — Complete API Specification</title>
<style>
  @page {
    size: A4;
    margin: 15mm;
  }
  body {
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
    color: #1e293b;
    line-height: 1.5;
    font-size: 11pt;
    margin: 0;
    padding: 0;
  }
  .cover {
    text-align: center;
    padding: 40px 20px;
    background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
    color: #ffffff;
    border-radius: 8px;
    margin-bottom: 30px;
  }
  .cover h1 {
    font-size: 26pt;
    margin: 0 0 10px 0;
    font-weight: 800;
    letter-spacing: -0.5px;
  }
  .cover p {
    font-size: 14pt;
    color: #94a3b8;
    margin: 0;
  }
  .cover .meta {
    font-size: 10pt;
    color: #38bdf8;
    margin-top: 15px;
  }
  h2.section-header {
    font-size: 16pt;
    color: #0f172a;
    border-bottom: 2px solid #3b82f6;
    padding-bottom: 6px;
    margin-top: 30px;
    margin-bottom: 20px;
    page-break-after: avoid;
  }
  .api-card {
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    border-left: 5px solid #3b82f6;
    border-radius: 6px;
    padding: 14px 18px;
    margin-bottom: 22px;
    page-break-inside: avoid;
  }
  .api-card.post { border-left-color: #10b981; }
  .api-card.put { border-left-color: #f59e0b; }
  .api-card.delete { border-left-color: #ef4444; }
  .api-title {
    font-size: 13pt;
    font-weight: 700;
    margin: 0 0 8px 0;
    color: #0f172a;
  }
  .method-badge {
    display: inline-block;
    padding: 3px 8px;
    font-size: 9pt;
    font-weight: 800;
    border-radius: 4px;
    color: #ffffff;
    background: #3b82f6;
    margin-right: 8px;
    vertical-align: middle;
  }
  .method-badge.GET { background: #3b82f6; }
  .method-badge.POST { background: #10b981; }
  .method-badge.PUT { background: #f59e0b; }
  .method-badge.DELETE { background: #ef4444; }
  .api-field {
    font-size: 10pt;
    margin: 4px 0;
    color: #334155;
  }
  .api-field strong {
    color: #0f172a;
  }
  pre {
    background: #0f172a;
    color: #f8fafc;
    padding: 10px 14px;
    border-radius: 6px;
    font-family: "Courier New", Courier, monospace;
    font-size: 8.5pt;
    overflow-x: auto;
    margin: 6px 0 10px 0;
    white-space: pre-wrap;
    word-break: break-all;
  }
  .code-label {
    font-size: 9pt;
    font-weight: 700;
    color: #475569;
    margin-top: 8px;
    margin-bottom: 2px;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }
</style>
</head>
<body>

<div class="cover">
  <h1>TUBA SmartCourt</h1>
  <p>Comprehensive System REST API & Component Specification</p>
  <div class="meta">Target Modules: Dashboard, Advocate Diary, Court Tracker & Core Practice Management</div>
</div>

<!-- ========================================================= -->
<!-- MODULE 1: DASHBOARD PAGE -->
<!-- ========================================================= -->
<h2 class="section-header">1. Dashboard Page Module (`/app/dashboard`)</h2>

<div class="api-card">
  <div class="api-title"><span class="method-badge GET">GET</span> /api/dashboard/stats</div>
  <div class="api-field"><strong>Initiator Component / Function:</strong> <code>DashboardPageComponent.ngOnInit()</code> &rarr; <code>DataService.loadDashboardStats()</code></div>
  <div class="api-field"><strong>Description:</strong> Fetches all primary KPI summary counts for active matters, closed cases, hearings, tasks, filings, and financial metrics.</div>
  <div class="code-label">Request Payload / Params</div>
  <pre>None (Header: Authorization Bearer Token)</pre>
  <div class="code-label">Response Payload Format (200 OK)</div>
  <pre>{
  "status": 200,
  "message": "Success",
  "data": {
    "activeMatters": 7,
    "closedThisYear": 2,
    "hearingsToday": 3,
    "hearingsThisWeek": 8,
    "openTasks": 5,
    "urgentHearings": 1,
    "pendingFilings": 2,
    "totalOutstanding": 15000.0,
    "totalCollected": 45000.0,
    "overdueInvoices": 1
  }
}</pre>
</div>

<div class="api-card">
  <div class="api-title"><span class="method-badge GET">GET</span> /api/hearings/today</div>
  <div class="api-field"><strong>Initiator Component / Function:</strong> <code>DashboardPageComponent.ngOnInit()</code> &rarr; <code>DataService.loadHearings()</code></div>
  <div class="api-field"><strong>Description:</strong> Loads today's cause list hearings for the cause list card on the dashboard.</div>
  <div class="code-label">Request Payload / Params</div>
  <pre>None</pre>
  <div class="code-label">Response Payload Format (200 OK)</div>
  <pre>{
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
}</pre>
</div>

<div class="api-card">
  <div class="api-title"><span class="method-badge GET">GET</span> /api/matters</div>
  <div class="api-field"><strong>Initiator Component / Function:</strong> <code>DashboardPageComponent.ngOnInit()</code> &rarr; <code>DataService.loadMatters()</code></div>
  <div class="api-field"><strong>Description:</strong> Loads client matter list for matter card links and quick navigation.</div>
  <div class="code-label">Request Payload / Params</div>
  <pre>None</pre>
  <div class="code-label">Response Payload Format (200 OK)</div>
  <pre>{
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
}</pre>
</div>

<div class="api-card">
  <div class="api-title"><span class="method-badge GET">GET</span> /api/tasks</div>
  <div class="api-field"><strong>Initiator Component / Function:</strong> <code>DashboardPageComponent.ngOnInit()</code> &rarr; <code>DataService.loadTasks()</code></div>
  <div class="api-field"><strong>Description:</strong> Loads pending tasks for dashboard action cards.</div>
  <div class="code-label">Request Payload / Params</div>
  <pre>None</pre>
  <div class="code-label">Response Payload Format (200 OK)</div>
  <pre>{
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
}</pre>
</div>

<div class="api-card">
  <div class="api-title"><span class="method-badge GET">GET</span> /api/filings</div>
  <div class="api-field"><strong>Initiator Component / Function:</strong> <code>DashboardPageComponent.ngOnInit()</code> &rarr; <code>DataService.loadFilings()</code></div>
  <div class="api-field"><strong>Description:</strong> Loads filing tracking status for pending filing overview card.</div>
  <div class="code-label">Request Payload / Params</div>
  <pre>None</pre>
  <div class="code-label">Response Payload Format (200 OK)</div>
  <pre>{
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
}</pre>
</div>

<div class="api-card">
  <div class="api-title"><span class="method-badge GET">GET</span> /api/dashboard/timeline</div>
  <div class="api-field"><strong>Initiator Component / Function:</strong> <code>DashboardPageComponent.ngOnInit()</code> &rarr; <code>DataService.getTimeline()</code></div>
  <div class="api-field"><strong>Description:</strong> Merges today's court hearings and advocate diary appointments into a single daily timeline schedule feed.</div>
  <div class="code-label">Request Payload / Params</div>
  <pre>None</pre>
  <div class="code-label">Response Payload Format (200 OK)</div>
  <pre>{
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
}</pre>
</div>

<div class="api-card">
  <div class="api-title"><span class="method-badge GET">GET</span> /api/dashboard/team-performance</div>
  <div class="api-field"><strong>Initiator Component / Function:</strong> <code>DashboardPageComponent.ngOnInit()</code> &rarr; <code>DataService.getTeamPerformance()</code></div>
  <div class="api-field"><strong>Description:</strong> Dynamically calculates team member task loads, open vs total tasks, and completion percentages.</div>
  <div class="code-label">Request Payload / Params</div>
  <pre>None</pre>
  <div class="code-label">Response Payload Format (200 OK)</div>
  <pre>{
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
}</pre>
</div>

<div class="api-card">
  <div class="api-title"><span class="method-badge GET">GET</span> /api/dashboard/court-distribution</div>
  <div class="api-field"><strong>Initiator Component / Function:</strong> <code>DashboardPageComponent.ngOnInit()</code> &rarr; <code>DataService.getCourtDistribution()</code></div>
  <div class="api-field"><strong>Description:</strong> Groups active matters by court name and computes percentage distribution.</div>
  <div class="code-label">Request Payload / Params</div>
  <pre>None</pre>
  <div class="code-label">Response Payload Format (200 OK)</div>
  <pre>{
  "status": 200,
  "data": [
    { "court": "Delhi High Court", "count": 4, "pct": 57 },
    { "court": "Supreme Court of India", "count": 3, "pct": 43 }
  ]
}</pre>
</div>

<div class="api-card">
  <div class="api-title"><span class="method-badge GET">GET</span> /api/dashboard/revenue</div>
  <div class="api-field"><strong>Initiator Component / Function:</strong> <code>DashboardPageComponent.ngOnInit()</code> &rarr; <code>DataService.getRevenue()</code></div>
  <div class="api-field"><strong>Description:</strong> Provides financial overview metrics and weekly revenue distribution.</div>
  <div class="code-label">Request Payload / Params</div>
  <pre>None</pre>
  <div class="code-label">Response Payload Format (200 OK)</div>
  <pre>{
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
}</pre>
</div>

<div class="api-card put">
  <div class="api-title"><span class="method-badge PUT">PUT</span> /api/tasks/{id}/toggle-done</div>
  <div class="api-field"><strong>Initiator Component / Function:</strong> <code>DashboardPageComponent.toggleTask(t)</code> &rarr; <code>DataService.toggleTaskDone(id)</code></div>
  <div class="api-field"><strong>Description:</strong> Toggles task completion status directly from the pending task widget.</div>
  <div class="code-label">Request Payload</div>
  <pre>{}</pre>
  <div class="code-label">Response Payload Format (200 OK)</div>
  <pre>{
  "status": 200,
  "data": {
    "id": 44,
    "title": "File Rejoinder Affidavit",
    "done": true,
    "status": "Completed"
  }
}</pre>
</div>

<!-- ========================================================= -->
<!-- MODULE 2: ADVOCATE DIARY PAGE -->
<!-- ========================================================= -->
<h2 class="section-header">2. Advocate Diary Page Module (`/app/diary`)</h2>

<div class="api-card">
  <div class="api-title"><span class="method-badge GET">GET</span> /api/diary/scope-options</div>
  <div class="api-field"><strong>Initiator Component / Function:</strong> <code>DiaryPageComponent.ngOnInit()</code> &rarr; <code>DataService.getDiaryScopeOptions()</code></div>
  <div class="api-field"><strong>Description:</strong> Fetches user permission flags and team/organization member list for scope filtering.</div>
  <div class="code-label">Request Payload / Params</div>
  <pre>None</pre>
  <div class="code-label">Response Payload Format (200 OK)</div>
  <pre>{
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
}</pre>
</div>

<div class="api-card">
  <div class="api-title"><span class="method-badge GET">GET</span> /api/diary/events</div>
  <div class="api-field"><strong>Initiator Component / Function:</strong> <code>DiaryPageComponent.loadEvents()</code> &rarr; <code>DataService.getDiaryEvents(year, month, scope, memberId)</code></div>
  <div class="api-field"><strong>Description:</strong> Fetches advocate diary events for the requested calendar month and scope filter.</div>
  <div class="code-label">Request Query Parameters</div>
  <pre>?year=2026&month=8&scope=own&memberId=1</pre>
  <div class="code-label">Response Payload Format (200 OK)</div>
  <pre>{
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
      "notes": "Discuss cross-examination strategy.",
      "urgent": true,
      "ownerId": 1,
      "ownerName": "Adv. Amit Sharma"
    }
  ]
}</pre>
</div>

<div class="api-card post">
  <div class="api-title"><span class="method-badge POST">POST</span> /api/diary/events</div>
  <div class="api-field"><strong>Initiator Component / Function:</strong> <code>DiaryPageComponent.save()</code> &rarr; <code>DataService.createDiaryEvent(payload)</code></div>
  <div class="api-field"><strong>Description:</strong> Creates a new advocate diary appointment or event.</div>
  <div class="code-label">Request Payload Format</div>
  <pre>{
  "title": "Pre-Trial Conference",
  "eventDate": "2026-08-06",
  "eventTime": "16:30",
  "type": "conference",
  "matterId": 12,
  "court": "Delhi High Court",
  "notes": "Finalize document list.",
  "urgent": false
}</pre>
  <div class="code-label">Response Payload Format (201 Created)</div>
  <pre>{
  "status": 201,
  "message": "Event added to diary.",
  "data": {
    "id": 303,
    "title": "Pre-Trial Conference",
    "eventDate": "2026-08-06",
    "eventTime": "16:30",
    "type": "conference",
    "ownerId": 1,
    "ownerName": "Adv. Amit Sharma"
  }
}</pre>
</div>

<div class="api-card put">
  <div class="api-title"><span class="method-badge PUT">PUT</span> /api/diary/events/{id}</div>
  <div class="api-field"><strong>Initiator Component / Function:</strong> <code>DiaryPageComponent.saveEdit()</code> &rarr; <code>DataService.updateDiaryEvent(id, payload)</code></div>
  <div class="api-field"><strong>Description:</strong> Updates an existing diary event entry.</div>
  <div class="code-label">Request Payload Format</div>
  <pre>{
  "title": "Updated: Pre-Trial Conference",
  "eventTime": "17:00",
  "urgent": true
}</pre>
  <div class="code-label">Response Payload Format (200 OK)</div>
  <pre>{
  "status": 200,
  "data": {
    "id": 303,
    "title": "Updated: Pre-Trial Conference",
    "eventTime": "17:00",
    "urgent": true
  }
}</pre>
</div>

<div class="api-card delete">
  <div class="api-title"><span class="method-badge DELETE">DELETE</span> /api/diary/events/{id}</div>
  <div class="api-field"><strong>Initiator Component / Function:</strong> <code>DiaryPageComponent.deleteEvent(e)</code> &rarr; <code>DataService.deleteDiaryEvent(id)</code></div>
  <div class="api-field"><strong>Description:</strong> Removes an event entry from the diary database.</div>
  <div class="code-label">Request Payload</div>
  <pre>None</pre>
  <div class="code-label">Response Payload Format (200 OK)</div>
  <pre>{
  "status": 200,
  "data": null
}</pre>
</div>

<div class="api-card">
  <div class="api-title"><span class="method-badge GET">GET</span> /api/hearings/filter</div>
  <div class="api-field"><strong>Initiator Component / Function:</strong> <code>DiaryPageComponent.loadEvents()</code> &rarr; <code>DataService.filterHearings('All')</code></div>
  <div class="api-field"><strong>Description:</strong> Fetches all scheduled hearings to merge seamlessly into the diary calendar UI.</div>
  <div class="code-label">Request Query Parameters</div>
  <pre>?type=All</pre>
  <div class="code-label">Response Payload Format (200 OK)</div>
  <pre>{
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
}</pre>
</div>

<!-- ========================================================= -->
<!-- MODULE 3: COURT TRACKER PAGE -->
<!-- ========================================================= -->
<h2 class="section-header">3. Court Tracker Module (`/app/tracker` & `/app/tracker/:cnr`)</h2>

<div class="api-card">
  <div class="api-title"><span class="method-badge GET">GET</span> /api/masters</div>
  <div class="api-field"><strong>Initiator Component / Function:</strong> <code>TrackerPageComponent.ngOnInit()</code> &rarr; <code>DataService.loadMasters()</code></div>
  <div class="api-field"><strong>Description:</strong> Retrieves master list dropdown values for court names and case types.</div>
  <div class="code-label">Request Payload / Params</div>
  <pre>None</pre>
  <div class="code-label">Response Payload Format (200 OK)</div>
  <pre>{
  "status": 200,
  "data": {
    "courts": ["Supreme Court of India", "Delhi High Court", "Bombay High Court", "NCLT Mumbai"],
    "caseTypes": ["CRL.A.", "CS(OS)", "W.P.(C)"],
    "practiceAreas": ["Criminal Law", "Commercial Disputes"]
  }
}</pre>
</div>

<div class="api-card">
  <div class="api-title"><span class="method-badge GET">GET</span> /api/tracker/history</div>
  <div class="api-field"><strong>Initiator Component / Function:</strong> <code>TrackerPageComponent.ngOnInit()</code> &rarr; <code>DataService.getRecentSearches()</code></div>
  <div class="api-field"><strong>Description:</strong> Fetches recent search history chips for the current user.</div>
  <div class="code-label">Request Payload / Params</div>
  <pre>None</pre>
  <div class="code-label">Response Payload Format (200 OK)</div>
  <pre>{
  "status": 200,
  "data": [
    {
      "cnr": "DLHC010045122025",
      "caseTitle": "State of Delhi vs. Rahul Sharma",
      "court": "Delhi High Court",
      "searchedAt": "2026-08-02T15:24:00Z"
    }
  ]
}</pre>
</div>

<div class="api-card">
  <div class="api-title"><span class="method-badge GET">GET</span> /api/tracker/search</div>
  <div class="api-field"><strong>Initiator Component / Function:</strong> <code>TrackerPageComponent.search()</code> &rarr; <code>DataService.searchByCnr(cnr)</code></div>
  <div class="api-field"><strong>Description:</strong> Initiates CNR search. Returns 202+jobId (async scraper job) or 200+result (cached).</div>
  <div class="code-label">Request Query Parameters</div>
  <pre>?cnr=DLHC010045122025</pre>
  <div class="code-label">Response Payload Format (202 Accepted / Async Worker)</div>
  <pre>{
  "status": 202,
  "data": {
    "jobId": 9042,
    "cnr": "DLHC010045122025",
    "status": "RUNNING",
    "message": "Scrape worker dispatched to court portal."
  }
}</pre>
</div>

<div class="api-card">
  <div class="api-title"><span class="method-badge GET">GET</span> /api/tracker/search-by-case-number</div>
  <div class="api-field"><strong>Initiator Component / Function:</strong> <code>TrackerPageComponent.search()</code> &rarr; <code>DataService.searchByCaseNumber(caseType, number, year)</code></div>
  <div class="api-field"><strong>Description:</strong> Resolves Case Type + Number + Year to a standard CNR number and dispatches job.</div>
  <div class="code-label">Request Query Parameters</div>
  <pre>?caseType=CS(OS)&number=403&year=2026</pre>
  <div class="code-label">Response Payload Format (200 OK)</div>
  <pre>{
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
}</pre>
</div>

<div class="api-card">
  <div class="api-title"><span class="method-badge GET">GET</span> /api/tracker/job/{jobId}</div>
  <div class="api-field"><strong>Initiator Component / Function:</strong> <code>TrackerPageComponent.startPolling()</code> &rarr; <code>DataService.pollJobStatus(jobId)</code></div>
  <div class="api-field"><strong>Description:</strong> Polls background worker status (PENDING &rarr; RUNNING &rarr; DONE / FAILED).</div>
  <div class="code-label">Request Payload / Params</div>
  <pre>Path variable: jobId = 9042</pre>
  <div class="code-label">Response Payload Format (200 OK)</div>
  <pre>{
  "status": 200,
  "data": {
    "jobId": 9042,
    "cnr": "DLHC010045122025",
    "status": "DONE",
    "errorMessage": null
  }
}</pre>
</div>

<div class="api-card">
  <div class="api-title"><span class="method-badge GET">GET</span> /api/tracker/{cnr}</div>
  <div class="api-field"><strong>Initiator Component / Function:</strong> <code>CaseDetailPageComponent.loadDetail()</code> &rarr; <code>DataService.getCaseDetail(cnr)</code></div>
  <div class="api-field"><strong>Description:</strong> Fetches complete persisted case details, hearing history, parties, and court order links.</div>
  <div class="code-label">Request Payload / Params</div>
  <pre>Path variable: cnr = DLHC010045122025</pre>
  <div class="code-label">Response Payload Format (200 OK)</div>
  <pre>{
  "status": 200,
  "data": {
    "cnr": "DLHC010045122025",
    "caseNumber": "CRL.A. 403/2025",
    "filingNumber": "F/1029/2025",
    "filingDate": "2025-01-15",
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
    "alertActive": true,
    "matterId": 12,
    "matterTitle": "State of Delhi vs. Rahul Sharma"
  }
}</pre>
</div>

<div class="api-card post">
  <div class="api-title"><span class="method-badge POST">POST</span> /api/tracker/{cnr}/refresh</div>
  <div class="api-field"><strong>Initiator Component / Function:</strong> <code>CaseDetailPageComponent.refresh()</code> &rarr; <code>DataService.refreshCase(cnr)</code></div>
  <div class="api-field"><strong>Description:</strong> Bypasses cache and forces re-scraping from live eCourts portal.</div>
  <div class="code-label">Request Payload</div>
  <pre>{}</pre>
  <div class="code-label">Response Payload Format (200 OK)</div>
  <pre>{
  "status": 200,
  "message": "Refresh queued — updating from court servers...",
  "data": {
    "jobId": 9045,
    "status": "RUNNING"
  }
}</pre>
</div>

<div class="api-card post">
  <div class="api-title"><span class="method-badge POST">POST</span> /api/tracker/{cnr}/alert</div>
  <div class="api-field"><strong>Initiator Component / Function:</strong> <code>CaseDetailPageComponent.toggleAlert()</code> &rarr; <code>DataService.toggleAlert(cnr, enabled)</code></div>
  <div class="api-field"><strong>Description:</strong> Enables/disables automated SMS/email case update notifications for the user.</div>
  <div class="code-label">Request Payload Format</div>
  <pre>{ "enabled": true }</pre>
  <div class="code-label">Response Payload Format (200 OK)</div>
  <pre>{
  "status": 200,
  "message": "Case alerts enabled.",
  "data": null
}</pre>
</div>

<div class="api-card">
  <div class="api-title"><span class="method-badge GET">GET</span> /api/tracker/{cnr}/suggest-matter</div>
  <div class="api-field"><strong>Initiator Component / Function:</strong> <code>CaseDetailPageComponent.loadMatterSuggestion()</code> &rarr; <code>DataService.suggestMatterLink(cnr)</code></div>
  <div class="api-field"><strong>Description:</strong> Suggests matching internal client matter for automatic linking.</div>
  <div class="code-label">Request Payload / Params</div>
  <pre>Path variable: cnr = DLHC010045122025</pre>
  <div class="code-label">Response Payload Format (200 OK)</div>
  <pre>{
  "status": 200,
  "data": {
    "matterId": 12,
    "matterTitle": "State of Delhi vs. Rahul Sharma",
    "confidenceScore": 0.95,
    "matchReason": "Case number matches CRL.A. 403/2025"
  }
}</pre>
</div>

<div class="api-card post">
  <div class="api-title"><span class="method-badge POST">POST</span> /api/tracker/{cnr}/link-matter</div>
  <div class="api-field"><strong>Initiator Component / Function:</strong> <code>CaseDetailPageComponent.confirmMatterLink()</code> &rarr; <code>DataService.linkCaseToMatter(cnr, matterId)</code></div>
  <div class="api-field"><strong>Description:</strong> Links tracked court case with an internal practice matter.</div>
  <div class="code-label">Request Payload Format</div>
  <pre>{ "matterId": 12 }</pre>
  <div class="code-label">Response Payload Format (200 OK)</div>
  <pre>{
  "status": 200,
  "message": "Case linked to matter.",
  "data": null
}</pre>
</div>

<div class="api-card post">
  <div class="api-title"><span class="method-badge POST">POST</span> /api/tracker/{cnr}/orders/{orderId}/save-to-matter</div>
  <div class="api-field"><strong>Initiator Component / Function:</strong> <code>CaseDetailPageComponent.saveToMatterVault(order)</code> &rarr; <code>DataService.saveOrderToMatterVault(cnr, orderId)</code></div>
  <div class="api-field"><strong>Description:</strong> Saves downloaded court order PDF into linked Matter Document Vault.</div>
  <div class="code-label">Request Payload</div>
  <pre>{}</pre>
  <div class="code-label">Response Payload Format (200 OK)</div>
  <pre>{
  "status": 200,
  "message": "Order saved to Matter Document Vault!",
  "data": {
    "saved": true,
    "docId": 1042,
    "alreadyExisted": false
  }
}</pre>
</div>

<div class="api-card">
  <div class="api-title"><span class="method-badge GET">GET</span> /api/tracker/{cnr}/orders/{orderId}/download</div>
  <div class="api-field"><strong>Initiator Component / Function:</strong> <code>CaseDetailPageComponent.downloadOrder(order)</code> &rarr; <code>DataService.downloadOrder(cnr, orderId)</code></div>
  <div class="api-field"><strong>Description:</strong> Streams proxied order PDF document.</div>
  <div class="code-label">Request Payload / Params</div>
  <pre>Path variables: cnr = DLHC010045122025, orderId = 701</pre>
  <div class="code-label">Response Format (200 OK)</div>
  <pre>Binary Stream (application/pdf)</pre>
</div>

<div class="api-card">
  <div class="api-title"><span class="method-badge GET">GET</span> /api/tracker/{cnr}/download-all</div>
  <div class="api-field"><strong>Initiator Component / Function:</strong> <code>CaseDetailPageComponent.downloadAll()</code> &rarr; <code>DataService.downloadAllOrders(cnr)</code></div>
  <div class="api-field"><strong>Description:</strong> Downloads ZIP archive containing all order PDFs for the case.</div>
  <div class="code-label">Request Payload / Params</div>
  <pre>Path variable: cnr = DLHC010045122025</pre>
  <div class="code-label">Response Format (200 OK)</div>
  <pre>Binary Stream (application/zip)</pre>
</div>

<div class="api-card">
  <div class="api-title"><span class="method-badge GET">GET</span> /api/tracker/{cnr}/export</div>
  <div class="api-field"><strong>Initiator Component / Function:</strong> <code>CaseDetailPageComponent.exportPdf()</code> &rarr; <code>DataService.exportCase(cnr, 'pdf')</code></div>
  <div class="api-field"><strong>Description:</strong> Generates and exports comprehensive case summary PDF report.</div>
  <div class="code-label">Request Query Parameters</div>
  <pre>?format=pdf</pre>
  <div class="code-label">Response Format (200 OK)</div>
  <pre>Binary Stream (application/pdf)</pre>
</div>

</body>
</html>
"""

html_path = "c:/Users/adity/TUBA SmartCourt/temp_api_spec.html"
pdf_path = "c:/Users/adity/TUBA SmartCourt/TUBA_SmartCourt_API_Specification.pdf"

with open(html_path, "w", encoding="utf-8") as f:
    f.write(html_content)

edge_path = r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
chrome_path = r"C:\Program Files\Google\Chrome\Application\chrome.exe"

browser_exe = edge_path if os.path.exists(edge_path) else chrome_path

print(f"Using browser: {browser_exe}")

cmd = [
    browser_exe,
    "--headless",
    "--disable-gpu",
    "--no-sandbox",
    f"--print-to-pdf={pdf_path}",
    html_path
]

res = subprocess.run(cmd, capture_output=True, text=True)
print("Return code:", res.returncode)
print("Stdout:", res.stdout)
print("Stderr:", res.stderr)

if os.path.exists(pdf_path):
    print("SUCCESS: Generated PDF at", pdf_path, "Size:", os.path.getsize(pdf_path), "bytes")
else:
    print("ERROR: PDF generation failed.")
