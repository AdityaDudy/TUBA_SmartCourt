import { Component, inject, OnInit, signal } from '@angular/core';
import { DataService } from '../../../../core/services/data.service';
import { CommonModule } from '@angular/common';

@Component({ selector: 'app-kb-page', standalone: true, imports: [CommonModule], templateUrl: './kb-page.component.html', styleUrl: './kb-page.component.scss' })
export class KbPageComponent implements OnInit {
  ds        = inject(DataService);
  tab       = signal<'judgments'|'templates'|'articles'>('judgments');
  judgments = signal<any[]>([]);
  templates = signal<any[]>([]);
  articles  = signal<any[]>([]);
  ngOnInit() {
    this.ds.getJudgments().subscribe(j => this.judgments.set(j));
    this.ds.getTemplates().subscribe(t => this.templates.set(t));
    this.ds.getArticles().subscribe(a => this.articles.set(a));
  }
}
