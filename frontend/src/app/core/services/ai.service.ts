import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, Subject, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DataService } from './data.service';
import { AuthService } from './auth.service';

// ── Types ──────────────────────────────────────────────────────
export interface ChatMessage {
  role: 'user' | 'assistant' | 'system';
  content: string;
  timestamp: Date;
  isStreaming?: boolean;
}

export interface RagContext {
  activeMatters: number;
  hearingsToday: number;
  urgentHearings: number;
  openTasks: number;
  overdueTasks: number;
  pendingFilings: number;
  totalClients: number;
  outstandingAmount: number;
  todayHearings: { title: string; court: string; time: string }[];
  recentMatters: { id: string; title: string; stage: string }[];
}

export interface ChatRequest {
  messages: Omit<ChatMessage, 'timestamp' | 'isStreaming'>[];
  context: RagContext;
  stream: boolean;
  model?: string;
}

export interface StreamChunk {
  id: string;
  choices: {
    delta: { content?: string; role?: string };
    finish_reason: string | null;
  }[];
}

// ── Service ────────────────────────────────────────────────────
@Injectable({ providedIn: 'root' })
export class AiService {
  private readonly http     = inject(HttpClient);
  private readonly dataSvc  = inject(DataService);

  private readonly BASE_URL    = environment.aiGatewayUrl;
  private readonly CHAT_ENDPOINT = `${this.BASE_URL}/api/v1/chat`;
  private readonly STREAM_ENDPOINT = `${this.BASE_URL}/api/v1/chat/stream`;

  // Conversation history
  private _history: ChatMessage[] = [];

  messages = signal<Array<{ sender: 'user' | 'assistant'; text: string }>>([]);
  conversations = signal<Array<{ id: string; title: string; messages: Array<{ sender: 'user' | 'assistant'; text: string }>; timestamp: string }>>([]);
  activeChatId = signal<string>('');

  private readonly authSvc = inject(AuthService);

  constructor() {
    this.initSseStreams();
  }

  loadUserConversations() {
    const email = this.authSvc.currentUser()?.email;
    if (!email) {
      this.conversations.set([]);
      this.startNewChat();
      return;
    }
    const saved = localStorage.getItem(`courtos_ai_conversations_${email}`);
    if (saved) {
      try {
        const list = JSON.parse(saved);
        this.conversations.set(list);
        this.startNewChat();
      } catch (e) {
        this.conversations.set([]);
        this.startNewChat();
      }
    } else {
      this.conversations.set([]);
      this.startNewChat();
    }
  }

  startNewChat() {
    const newId = 'chat_' + Date.now();
    this.activeChatId.set(newId);
    this.messages.set([
      { sender: 'assistant', text: 'Hello! I am your AI Legal Assistant. Ask me to summarize a case, draft a contract clause, or search relevant sections.' }
    ]);
    this._history = [];
  }

  saveCurrentConversation() {
    const email = this.authSvc.currentUser()?.email;
    if (!email) return;

    const currentId = this.activeChatId();
    const currentMsgs = this.messages();

    const firstUserMsg = currentMsgs.find(m => m.sender === 'user')?.text || 'New Conversation';
    const cleanTitle = firstUserMsg.length > 30 ? firstUserMsg.substring(0, 30) + '...' : firstUserMsg;

    this.conversations.update(list => {
      const idx = list.findIndex(c => c.id === currentId);
      let updated = [...list];
      if (idx > -1) {
        updated[idx] = {
          ...updated[idx],
          messages: currentMsgs,
          timestamp: new Date().toISOString()
        };
      } else {
        const newConv = {
          id: currentId,
          title: cleanTitle,
          messages: currentMsgs,
          timestamp: new Date().toISOString()
        };
        updated = [newConv, ...updated];
      }

      if (updated.length > 10) {
        updated = updated.slice(0, 10);
      }

      localStorage.setItem(`courtos_ai_conversations_${email}`, JSON.stringify(updated));
      return updated;
    });
  }

  selectConversation(id: string) {
    const conv = this.conversations().find(c => c.id === id);
    if (conv) {
      this.activeChatId.set(conv.id);
      this.messages.set(conv.messages);
      this._history = conv.messages.map((m: any) => ({
        role: m.sender === 'user' ? 'user' : 'assistant',
        content: m.text,
        timestamp: new Date()
      }));
    }
  }

  deleteConversation(id: string) {
    const email = this.authSvc.currentUser()?.email;
    this.conversations.update(list => {
      const updated = list.filter(c => c.id !== id);
      if (email) {
        localStorage.setItem(`courtos_ai_conversations_${email}`, JSON.stringify(updated));
      }
      return updated;
    });
    // If the deleted chat was active, start fresh
    if (this.activeChatId() === id) {
      this.startNewChat();
    }
  }

  clearServiceState() {
    this.messages.set([]);
    this.conversations.set([]);
    this.activeChatId.set('');
    this._history = [];
  }

  private initSseStreams() {
    // Keep it empty
  }

  // ── Build RAG Context ─────────────────────────────────────
  private buildContext(): RagContext {
    const ds = this.dataSvc;
    return {
      activeMatters:     ds.activeMattersCount(),
      hearingsToday:     ds.todayHearingsCount(),
      urgentHearings:    ds.urgentHearingsCount(),
      openTasks:         ds.openTasksCount(),
      overdueTasks:      ds.overdueTasksCount(),
      pendingFilings:    ds.pendingFilingsCount(),
      totalClients:      ds.getClients().length,
      outstandingAmount: ds.totalOutstanding(),
      todayHearings: ds.getHearings().slice(0, 5).map((h: any) => ({
        title: h.title, court: h.court, time: h.time,
      })),
      recentMatters: ds.getMatters().slice(0, 5).map((m: any) => ({
        id: m.id, title: m.title, stage: m.stage,
      })),
    };
  }

  // ── System Prompt ─────────────────────────────────────────
  private buildSystemPrompt(ctx: RagContext): string {
    return `You are TUBA Legal AI — an intelligent legal assistant for CourtOS, India's smart court management platform.

You help Indian advocates with:
- Matter and case tracking
- Hearing schedules and cause lists
- Task management and filing deadlines
- Legal research and document drafting
- Billing and invoice queries
- Client information

Current Practice Statistics (live from the system):
- Active Matters: ${ctx.activeMatters}
- Hearings Today: ${ctx.hearingsToday} (${ctx.urgentHearings} urgent)
- Open Tasks: ${ctx.openTasks} (${ctx.overdueTasks} overdue)
- Total Clients: ${ctx.totalClients}
- Outstanding Fees: ₹${ctx.outstandingAmount.toLocaleString('en-IN')}

Today's Scheduled Hearings:
${ctx.todayHearings.map((h, i) => `${i + 1}. ${h.title} — ${h.court} at ${h.time}`).join('\n')}

Recent Active Matters:
${ctx.recentMatters.map(m => `- ${m.id}: ${m.title} (Stage: ${m.stage})`).join('\n')}

Respond in a professional but conversational tone. Use Indian legal terminology where appropriate (SC, HC, NCLT, ITAT, IBC, etc.). Keep responses concise and actionable. If asked about specific matters not in context, acknowledge limitations and suggest checking the Matters module.`;
  }

  // ── Send Message (Streaming) ──────────────────────────────
  sendMessage(userMessage: string): Observable<string> {
    const streamSubject = new Subject<string>();

    // 1. Add user message to signal & history
    this.messages.update(list => [...list, { sender: 'user', text: userMessage }]);
    this.saveCurrentConversation();

    this._history.push({
      role: 'user',
      content: userMessage,
      timestamp: new Date(),
    });

    // 2. Add placeholder assistant message for streaming
    this.messages.update(list => [...list, { sender: 'assistant', text: '' }]);

    const ctx = this.buildContext();

    const request: ChatRequest = {
      messages: [
        { role: 'system', content: this.buildSystemPrompt(ctx) },
        ...this._history.map(m => ({ role: m.role, content: m.content })),
      ],
      context: ctx,
      stream: true,
      model: 'llama3-70b-8192', // or gpt-4o, etc.
    };

    // SSE streaming via EventSource (GET with params) or fetch with ReadableStream
    this._streamWithFetch(request, streamSubject);

    return streamSubject.asObservable();
  }

  // ── SSE Streaming via fetch ReadableStream ────────────────
  private async _streamWithFetch(request: ChatRequest, subject: Subject<string>) {
    try {
      const token = localStorage.getItem('courtos_access_token');
      const headers: Record<string, string> = {
        'Content-Type': 'application/json',
        'Accept': 'text/event-stream',
      };
      if (token) headers['Authorization'] = `Bearer ${token}`;

      const response = await fetch(this.STREAM_ENDPOINT, {
        method: 'POST',
        headers,
        body: JSON.stringify(request),
      });

      if (!response.ok || !response.body) {
        throw new Error(`AI Gateway error: ${response.status} ${response.statusText}`);
      }

      const reader   = response.body.getReader();
      const decoder  = new TextDecoder();
      let fullContent = '';
      let buffer = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        const chunk = buffer + decoder.decode(value, { stream: true });
        const lines = chunk.split('\n');
        
        // Save the last incomplete line to the buffer
        buffer = lines.pop() || '';

        for (const line of lines) {
          const trimmed = line.trim();
          if (!trimmed.startsWith('data:')) continue;
          const data = trimmed.slice(trimmed.startsWith('data: ') ? 6 : 5).trim();
          if (data === '[DONE]') break;

          try {
            const parsed = JSON.parse(data) as StreamChunk;
            const delta  = parsed.choices?.[0]?.delta?.content ?? '';
            if (delta) {
              fullContent += delta;
              
              // Update last assistant message text in signal
              this.messages.update(list => {
                const copy = [...list];
                const last = copy[copy.length - 1];
                if (last && last.sender === 'assistant') {
                  last.text = last.text + delta;
                }
                return copy;
              });

              subject.next(delta);
            }
          } catch {
            // non-JSON SSE line, ignore
          }
        }
      }

      // Save full assistant response to history & session chat
      this._history.push({
        role: 'assistant',
        content: fullContent,
        timestamp: new Date(),
      });
      this.saveCurrentConversation();

      subject.complete();

    } catch (err) {
      console.error('[AiService] Stream error:', err);
      // Fallback: return a graceful error message
      const fallback = this._getFallbackResponse(
        this._history[this._history.length - 1]?.content ?? ''
      );
      
      // Update last assistant message in signal with fallback
      this.messages.update(list => {
        const copy = [...list];
        const last = copy[copy.length - 1];
        if (last && last.sender === 'assistant') {
          last.text = fallback;
        }
        return copy;
      });

      this._history.push({ role: 'assistant', content: fallback, timestamp: new Date() });
      this.saveCurrentConversation();
      
      subject.complete();
    }
  }

  // ── Non-Streaming Fallback ────────────────────────────────
  sendMessageSync(userMessage: string): Observable<string> {
    const ctx     = this.buildContext();
    const request: ChatRequest = {
      messages: [
        { role: 'system', content: this.buildSystemPrompt(ctx) },
        { role: 'user',   content: userMessage },
      ],
      context: ctx,
      stream: false,
    };

    return new Observable(observer => {
      this.http
        .post<{ content: string }>(`${this.CHAT_ENDPOINT}`, request, {
          headers: new HttpHeaders({
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${localStorage.getItem('courtos_access_token') ?? ''}`,
          }),
        })
        .subscribe({
          next:  (res) => { observer.next(res.content); observer.complete(); },
          error: () => {
            const fb = this._getFallbackResponse(userMessage);
            observer.next(fb);
            observer.complete();
          },
        });
    });
  }

  // ── Keyword Fallback (when AI gateway is offline) ─────────
  private _getFallbackResponse(query: string): string {
    const q   = query.toLowerCase();
    const ds  = this.dataSvc;
    const ctx = this.buildContext();

    if (q.includes('hearing') || q.includes('today') || q.includes('cause')) {
      return `<strong>Today's ${ctx.hearingsToday} hearings:</strong><br>` +
        ctx.todayHearings.map((h, i) => `${i + 1}. ${h.court} — ${h.title} · ${h.time}`).join('<br>');
    }
    if (q.includes('task') || q.includes('overdue')) {
      return `<strong>${ctx.openTasks} open tasks.</strong> ${ctx.overdueTasks} overdue. Check the Tasks module for details.`;
    }
    if (q.includes('billing') || q.includes('invoice') || q.includes('outstanding')) {
      return `Outstanding amount: <strong>₹${ctx.outstandingAmount.toLocaleString('en-IN')}</strong>. Visit the Billing module for invoice details.`;
    }
    if (q.includes('client')) {
      return `<strong>${ctx.totalClients} clients</strong> registered in the system. Use the Clients module to search and view details.`;
    }
    if (q.includes('matter')) {
      return `<strong>${ctx.activeMatters} active matters</strong> currently. Use the Matters module for full details and to filter by type.`;
    }
    if (q.includes('filing')) {
      return `<strong>${ctx.pendingFilings} filings</strong> are pending. Check the Filings module for deadlines.`;
    }
    return `I'm your TUBA Legal AI Assistant. I can help with matters, hearings, tasks, billing, and legal research. Try asking: <em>"Show today's hearings"</em>, <em>"Any overdue tasks?"</em>, or <em>"Billing status"</em>.<br><br><small style="color:var(--txt3)">Note: AI Gateway is currently offline — running in limited mode.</small>`;
  }

  // ── Document Q&A (RAG) ────────────────────────────────────
  askAboutDocument(documentText: string, question: string): Observable<string> {
    const request = {
      messages: [
        {
          role: 'system',
          content: 'You are a legal document analyst. Answer questions about the provided document accurately and cite relevant sections.',
        },
        {
          role: 'user',
          content: `Document:\n\n${documentText}\n\nQuestion: ${question}`,
        },
      ],
      stream: false,
    };

    return new Observable(observer => {
      this.http
        .post<{ content: string }>(this.CHAT_ENDPOINT, request)
        .subscribe({
          next:  (res) => { observer.next(res.content); observer.complete(); },
          error: () => { observer.next('Unable to analyse document at this time.'); observer.complete(); },
        });
    });
  }
}
