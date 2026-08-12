import { Component, inject, signal, computed, OnInit, OnDestroy, ViewChild } from '@angular/core';
import { Router, RouterOutlet, NavigationEnd } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { DataService } from '../../core/services/data.service';
import { ToastService } from '../../core/services/toast.service';
import { SidebarComponent } from '../sidebar/sidebar.component';
import { TopbarComponent } from '../topbar/topbar.component';
import { CommonModule } from '@angular/common';
import { CommandPaletteComponent } from '../../shared/components/command-palette/command-palette.component';
import { Subscription, filter } from 'rxjs';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, SidebarComponent, TopbarComponent, CommonModule, CommandPaletteComponent],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent implements OnInit, OnDestroy {
  readonly auth    = inject(AuthService);
  readonly data    = inject(DataService);
  readonly toast   = inject(ToastService);
  readonly router  = inject(Router);

  sidebarCollapsed = signal(false);
  pageEntering     = signal(false);

  private routeSub?: Subscription;

  ngOnInit() {
    this.routeSub = this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe(() => {
        this.pageEntering.set(true);
        setTimeout(() => this.pageEntering.set(false), 220);
      });
  }

  ngOnDestroy() {
    this.routeSub?.unsubscribe();
  }

  toggleSidebar() {
    this.sidebarCollapsed.update(v => !v);
  }
}

