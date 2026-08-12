import os
import subprocess

html_content = """<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>Advocate Diary Page — API Specification & Network Trace</title>
<style>
  @page {
    size: A4;
    margin: 12mm;
  }
  body {
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
    color: #1e293b;
    line-height: 1.45;
    font-size: 10pt;
    margin: 0;
    padding: 0;
  }
  .header-card {
    background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
    color: #ffffff;
    padding: 22px 20px;
    border-radius: 8px;
    margin-bottom: 20px;
  }
  .header-card h1 {
    font-size: 20pt;
    margin: 0 0 6px 0;
    font-weight: 800;
  }
  .header-card p {
    font-size: 11pt;
    color: #94a3b8;
    margin: 0;
  }
  .header-card .badge {
    display: inline-block;
    background: #0284c7;
    color: #fff;
    font-size: 8.5pt;
    font-weight: 700;
    padding: 3px 8px;
    border-radius: 4px;
    margin-top: 10px;
  }
  .section-title {
    font-size: 13pt;
    font-weight: 800;
    color: #0f172a;
    border-bottom: 2px solid #0284c7;
    padding-bottom: 4px;
    margin-top: 22px;
    margin-bottom: 14px;
    page-break-after: avoid;
  }
  .api-card {
    background: #f8fafc;
    border: 1px solid #cbd5e1;
    border-left: 5px solid #0284c7;
    border-radius: 6px;
    padding: 12px 16px;
    margin-bottom: 16px;
    page-break-inside: avoid;
  }
  .api-card.POST { border-left-color: #10b981; }
  .api-card.PUT { border-left-color: #f59e0b; }
  .api-card.DELETE { border-left-color: #ef4444; }
  .api-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;
  }
  .api-url {
    font-size: 11.5pt;
    font-weight: 700;
    color: #0f172a;
    font-family: monospace;
  }
  .method {
    display: inline-block;
    padding: 2px 7px;
    font-size: 8.5pt;
    font-weight: 800;
    border-radius: 4px;
    color: #fff;
    background: #0284c7;
  }
  .method.GET { background: #0284c7; }
  .method.POST { background: #10b981; }
  .method.PUT { background: #f59e0b; }
  .method.DELETE { background: #ef4444; }
  .meta-row {
    font-size: 9.5pt;
    margin-bottom: 4px;
    color: #334155;
  }
  .meta-row strong { color: #0f172a; }
  .code-title {
    font-size: 8.5pt;
    font-weight: 700;
    text-transform: uppercase;
    color: #475569;
    margin-top: 8px;
    margin-bottom: 2px;
    letter-spacing: 0.5px;
  }
  pre {
    background: #0f172a;
    color: #f8fafc;
    padding: 9px 12px;
    border-radius: 5px;
    font-family: "Courier New", Courier, monospace;
    font-size: 8pt;
    margin: 4px 0 8px 0;
    white-space: pre-wrap;
    word-break: break-all;
  }
</style>
</head>
<body>

<div class="header-card">
  <h1>Advocate Diary Page — API Network Specification</h1>
  <p>Detailed breakdown of all APIs invoked on <code>/app/diary</code> (Network tab trace)</p>
  <div class="badge">Frontend Component: DiaryPageComponent (diary-page.component.ts)</div>
</div>

<div class="section-title">1. Initial Page Load & Event Fetching APIs</div>

<!-- 1. GET /api/diary/scope-options -->
<div class="api-card GET">
  <div class="api-header">
    <span class="api-url"><span class="method GET">GET</span> /api/diary/scope-options</span>
  </div>
  <div class="meta-row"><strong>Initiator:</strong> <code>DiaryPageComponent.ngOnInit()</code> &rarr; Line 249 (<code>this.ds.getDiaryScopeOptions()</code>)</div>
  <div class="meta-row"><strong>Purpose:</strong> Checks user permissions (<code>scope_team</code> / <code>scope_org</code>) to render scope filter tabs ("My Diary", "Team Diary", "Firm Diary") and populates the team member dropdown list.</div>
  <div class="code-title">Query Parameters / Request Payload</div>
  <pre>None (Headers: Authorization Bearer Token)</pre>
  <div class="code-title">Response Payload Format (200 OK)</div>
  <pre>{
  "status": 200,
  "message": "Success",
  "data": {
    "canTeam": true,
    "canOrg": true,
    "teamMembers": [
      {
        "id": 1,
        "name": "Admin User",
        "email": "admin@tubalaw.com",
        "role": "Admin",
        "initials": "AU",
        "gradient": "linear-gradient(135deg,#b45309,#d97706)"
      }
    ],
    "orgMembers": []
  }
}</pre>
</div>

<!-- 2. GET /api/diary/events -->
<div class="api-card GET">
  <div class="api-header">
    <span class="api-url"><span class="method GET">GET</span> /api/diary/events</span>
  </div>
  <div class="meta-row"><strong>Initiator:</strong> <code>DiaryPageComponent.loadEvents()</code> &rarr; Line 255 (<code>this.ds.getDiaryEvents()</code>)</div>
  <div class="meta-row"><strong>Purpose:</strong> Fetches stored custom advocate appointments, conferences, meetings, and deadlines for the active month and scope.</div>
  <div class="code-title">Query Parameters</div>
  <pre>?year=2026&month=8&scope=own&memberId=1</pre>
  <div class="code-title">Response Payload Format (200 OK)</div>
  <pre>{
  "status": 200,
  "message": "Success",
  "data": [
    {
      "id": 302,
      "title": "Client Strategy Meeting",
      "eventDate": "2026-08-04",
      "eventTime": "14:00",
      "type": "meeting",
      "matterId": 12,
      "matterTitle": "State of Delhi vs. Rahul Sharma",
      "clientId": 5,
      "clientName": "Rahul Sharma",
      "court": "Chamber 402, High Court Block",
      "notes": "Discuss cross-examination strategy.",
      "urgent": true,
      "ownerId": 1,
      "ownerName": "Admin User",
      "createdBy": 1
    }
  ]
}</pre>
</div>

<!-- 3. GET /api/matters -->
<div class="api-card GET">
  <div class="api-header">
    <span class="api-url"><span class="method GET">GET</span> /api/matters</span>
  </div>
  <div class="meta-row"><strong>Initiator:</strong> <code>DiaryPageComponent.loadEvents()</code> &rarr; Line 254 (<code>this.ds.loadMatters()</code>)</div>
  <div class="meta-row"><strong>Purpose:</strong> Loads practice matters to resolve matter titles, client links, and court names for events, tasks, filings, and hearings displayed in the diary calendar.</div>
  <div class="code-title">Query Parameters / Request Payload</div>
  <pre>None</pre>
  <div class="code-title">Response Payload Format (200 OK)</div>
  <pre>{
  "status": 200,
  "message": "Success",
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
      "advocate": "Admin User",
      "coCounsel": null,
      "opposingCounsel": "Adv. Rajesh Kumar",
      "limitationDeadline": null,
      "relatedMatterId": null,
      "createdAt": "2026-07-01T10:00:00Z",
      "updatedAt": null,
      "tenantId": "default"
    }
  ]
}</pre>
</div>

<!-- 4. GET /api/tasks -->
<div class="api-card GET">
  <div class="api-header">
    <span class="api-url"><span class="method GET">GET</span> /api/tasks</span>
  </div>
  <div class="meta-row"><strong>Initiator:</strong> <code>DiaryPageComponent.loadEvents()</code> &rarr; Line 257 (<code>this.ds.loadTasks()</code>)</div>
  <div class="meta-row"><strong>Purpose:</strong> Merges open tasks with due dates onto the advocate diary calendar under the "Task Due" deadline category.</div>
  <div class="code-title">Query Parameters / Request Payload</div>
  <pre>None</pre>
  <div class="code-title">Response Payload Format (200 OK)</div>
  <pre>{
  "status": 200,
  "message": "Success",
  "data": [
    {
      "id": 44,
      "title": "File Rejoinder Affidavit",
      "description": "Prepare and submit rejoinder in High Court",
      "matterId": 12,
      "matterTitle": "State of Delhi vs. Rahul Sharma",
      "assignedTo": "Admin User",
      "priority": "High",
      "dueDate": "2026-08-05",
      "done": false,
      "status": "Open",
      "type": "Task",
      "createdBy": "Admin User"
    }
  ]
}</pre>
</div>

<!-- 5. GET /api/filings -->
<div class="api-card GET">
  <div class="api-header">
    <span class="api-url"><span class="method GET">GET</span> /api/filings</span>
  </div>
  <div class="meta-row"><strong>Initiator:</strong> <code>DiaryPageComponent.loadEvents()</code> &rarr; Line 258 (<code>this.ds.loadFilings()</code>)</div>
  <div class="meta-row"><strong>Purpose:</strong> Merges pending filing deadlines onto the advocate diary calendar under the "Filing Due" deadline category.</div>
  <div class="code-title">Query Parameters / Request Payload</div>
  <pre>None</pre>
  <div class="code-title">Response Payload Format (200 OK)</div>
  <pre>{
  "status": 200,
  "message": "Success",
  "data": [
    {
      "id": 8,
      "title": "Interlocutory Application for Stay",
      "matterId": 12,
      "matterTitle": "State of Delhi vs. Rahul Sharma",
      "court": "Delhi High Court",
      "filingType": "Application",
      "stage": "Drafting",
      "status": "Draft",
      "dueDate": "2026-08-04",
      "filedDate": null,
      "advocate": "Admin User",
      "notes": null,
      "s3Url": null,
      "source": "manual"
    }
  ]
}</pre>
</div>

<!-- 6. GET /api/hearings/filter -->
<div class="api-card GET">
  <div class="api-header">
    <span class="api-url"><span class="method GET">GET</span> /api/hearings/filter</span>
  </div>
  <div class="meta-row"><strong>Initiator:</strong> <code>DiaryPageComponent.loadEvents()</code> &rarr; Line 259 (<code>this.ds.filterHearings('All')</code>)</div>
  <div class="meta-row"><strong>Purpose:</strong> Merges scheduled court hearings directly into the advocate diary view.</div>
  <div class="code-title">Query Parameters</div>
  <pre>?type=All</pre>
  <div class="code-title">Response Payload Format (200 OK)</div>
  <pre>{
  "status": 200,
  "message": "Success",
  "data": [
    {
      "id": 101,
      "matterId": 12,
      "caseTitle": "State of Delhi vs. Rahul Sharma",
      "court": "Delhi High Court",
      "bench": "Court No. 3 (Bench B)",
      "hearingDate": "2026-08-04",
      "hearingTime": "10:30 AM",
      "status": "Scheduled",
      "purpose": "Arguments",
      "judgeName": "Hon'ble Mr. Justice A.K. Roy",
      "tenantId": "default"
    }
  ]
}</pre>
</div>

<div class="section-title">2. User Action / Mutation APIs</div>

<!-- 7. POST /api/diary/events -->
<div class="api-card POST">
  <div class="api-header">
    <span class="api-url"><span class="method POST">POST</span> /api/diary/events</span>
  </div>
  <div class="meta-row"><strong>Initiator:</strong> <code>DiaryPageComponent.save()</code> &rarr; Line 445 (<code>this.ds.createDiaryEvent()</code>)</div>
  <div class="meta-row"><strong>Purpose:</strong> Triggered when the advocate submits the "Add Diary Event" modal form.</div>
  <div class="code-title">Request Body Payload</div>
  <pre>{
  "title": "Client Strategy Meeting",
  "eventDate": "2026-08-04",
  "eventTime": "14:00",
  "type": "meeting",
  "matterId": 12,
  "court": "Delhi High Court",
  "notes": "Discuss cross-examination strategy.",
  "urgent": true
}</pre>
  <div class="code-title">Response Payload Format (201 Created)</div>
  <pre>{
  "status": 201,
  "message": "Event added to diary.",
  "data": {
    "id": 302,
    "title": "Client Strategy Meeting",
    "eventDate": "2026-08-04",
    "eventTime": "14:00",
    "type": "meeting",
    "matterId": 12,
    "matterTitle": "State of Delhi vs. Rahul Sharma",
    "clientId": 5,
    "clientName": "Rahul Sharma",
    "court": "Delhi High Court",
    "notes": "Discuss cross-examination strategy.",
    "urgent": true,
    "ownerId": 1,
    "ownerName": "Admin User",
    "createdBy": 1
  }
}</pre>
</div>

<!-- 8. PUT /api/diary/events/{id} -->
<div class="api-card PUT">
  <div class="api-header">
    <span class="api-url"><span class="method PUT">PUT</span> /api/diary/events/{id}</span>
  </div>
  <div class="meta-row"><strong>Initiator:</strong> <code>DiaryPageComponent.saveEdit()</code> &rarr; Line 462 (<code>this.ds.updateDiaryEvent()</code>)</div>
  <div class="meta-row"><strong>Purpose:</strong> Triggered when the advocate edits an event via the "Edit Diary Event" modal form.</div>
  <div class="code-title">Request Body Payload</div>
  <pre>{
  "title": "Updated: Client Strategy Meeting",
  "eventTime": "15:00",
  "urgent": false
}</pre>
  <div class="code-title">Response Payload Format (200 OK)</div>
  <pre>{
  "status": 200,
  "message": "Success",
  "data": {
    "id": 302,
    "title": "Updated: Client Strategy Meeting",
    "eventDate": "2026-08-04",
    "eventTime": "15:00",
    "type": "meeting",
    "urgent": false
  }
}</pre>
</div>

<!-- 9. DELETE /api/diary/events/{id} -->
<div class="api-card DELETE">
  <div class="api-header">
    <span class="api-url"><span class="method DELETE">DELETE</span> /api/diary/events/{id}</span>
  </div>
  <div class="meta-row"><strong>Initiator:</strong> <code>DiaryPageComponent.deleteEvent()</code> &rarr; Line 472 (<code>this.ds.deleteDiaryEvent()</code>)</div>
  <div class="meta-row"><strong>Purpose:</strong> Triggered when the advocate confirms deletion of an event.</div>
  <div class="code-title">Request Payload</div>
  <pre>None</pre>
  <div class="code-title">Response Payload Format (200 OK)</div>
  <pre>{
  "status": 200,
  "message": "Success",
  "data": null
}</pre>
</div>

</body>
</html>
"""

html_path = "c:/Users/adity/TUBA SmartCourt/temp_diary_spec.html"
pdf_path = "c:/Users/adity/TUBA SmartCourt/Advocate_Diary_Page_APIs.pdf"

with open(html_path, "w", encoding="utf-8") as f:
    f.write(html_content)

edge_path = r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
chrome_path = r"C:\Program Files\Google\Chrome\Application\chrome.exe"
browser_exe = edge_path if os.path.exists(edge_path) else chrome_path

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
if os.path.exists(pdf_path):
    print("SUCCESS: Generated PDF at", pdf_path, "Size:", os.path.getsize(pdf_path), "bytes")
else:
    print("ERROR: PDF generation failed.")
