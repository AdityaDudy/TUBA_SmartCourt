import { Component, inject, signal, OnInit, ViewChild, ElementRef, AfterViewChecked, effect } from '@angular/core';
import { AiService } from '../../../../core/services/ai.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-ai-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ai-page.component.html',
  styleUrl: './ai-page.component.scss'
})
export class AiPageComponent implements OnInit, AfterViewChecked {
  private aiService = inject(AiService);

  @ViewChild('scrollContainer') private scrollContainer!: ElementRef<HTMLDivElement>;

  prompt      = signal('');
  loading     = signal(false);
  messages    = this.aiService.messages;
  conversations = this.aiService.conversations;
  activeChatId  = this.aiService.activeChatId;

  constructor() {
    effect(() => {
      // Track changes to messages or loading state and scroll down
      this.messages();
      this.loading();
      setTimeout(() => this.scrollToBottom(), 50);
    });
  }

  ngOnInit() {
    this.aiService.loadUserConversations();
  }

  ngAfterViewChecked() {
    this.scrollToBottom();
  }

  private scrollToBottom(): void {
    if (this.scrollContainer?.nativeElement) {
      const el = this.scrollContainer.nativeElement;
      el.scrollTop = el.scrollHeight;
    }
  }

  startNewChat() {
    this.aiService.startNewChat();
  }

  selectConversation(id: string) {
    this.aiService.selectConversation(id);
  }

  deleteConversation(id: string, event: MouseEvent) {
    event.stopPropagation(); // don't also trigger selectConversation
    this.aiService.deleteConversation(id);
  }

  sendMessage() {
    const text = this.prompt().trim();
    if (!text) return;

    this.prompt.set('');
    this.loading.set(true);

    this.aiService.sendMessage(text).subscribe({
      next: () => {
        // Updated in real-time by the service signal
      },
      error: () => {
        this.loading.set(false);
      },
      complete: () => {
        this.loading.set(false);
      }
    });
  }

  formatMarkdown(text: string): string {
    if (!text) return '';
    let html = text;

    // 1. Bold: **text** -> <strong>text</strong>
    html = html.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');

    // 2. Italic: *text* -> <em>text</em> (restricted to same-line, non-asterisk contents)
    html = html.replace(/\*([^\*\n]+)\*/g, '<em>$1</em>');

    // 3. Newlines to <br>
    html = html.replace(/\n/g, '<br>');

    // 4. Lists: convert leading "- " or "* " to "• "
    html = html.replace(/^[-*]\s+/g, '• ');
    html = html.replace(/<br>[-*]\s+/g, '<br>• ');

    return html;
  }
}
