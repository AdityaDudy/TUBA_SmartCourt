import { Component, inject, OnInit, signal } from '@angular/core';
import { DataService } from '../../../../core/services/data.service';
import { AuthService } from '../../../../core/services/auth.service';
import { ToastService } from '../../../../core/services/toast.service';
import { ThemeService, BrandTheme, ThemeMode } from '../../../../core/services/theme.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-settings-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './settings-page.component.html',
  styleUrl: './settings-page.component.scss'
})
export class SettingsPageComponent implements OnInit {
  private ds = inject(DataService);
  public auth = inject(AuthService);
  public theme = inject(ThemeService);
  private toast = inject(ToastService);

  // Logged-in user binding
  currentUser = this.auth.currentUser;

  // Editable Profile Settings
  fullName = signal('');
  barCouncilNo = signal('');
  mobile = signal('');
  email = signal('');
  avatarUrl = signal<string | null>(null);

  // Security toggles
  mfaEnabled = signal<boolean>(true);
  biometricLogin = signal<boolean>(false);
  dataEncryption = signal<string>('AES-256');
  auditTrail = signal<boolean>(true);

  // User-specific notification preferences
  hearingReminders = signal<boolean>(true);
  filingAlerts = signal<boolean>(true);
  taskOverdue = signal<boolean>(true);
  dailyCauseList = signal<boolean>(true);

  // Password modal
  showPasswordModal = signal(false);
  oldPassword = signal('');
  newPassword = signal('');
  confirmPassword = signal('');
  passwordError = signal('');
  passwordLoading = signal(false);

  ngOnInit() {
    this.ds.loadUsers().subscribe(() => this.loadUserData());
    this.loadUserData();
    this.loadUserSettings();
  }

  loadUserData() {
    const u = this.currentUser();
    if (u) {
      this.fullName.set(u.name || '');
      this.email.set(u.email || '');
      this.avatarUrl.set(u.avatar || null);
    }

    // Match against DataService loaded users
    const matched = this.ds.users().find(user => u && (user.id === u.id || user.email === u.email));
    if (matched) {
      if (matched.name) this.fullName.set(matched.name);
      if (matched.email) this.email.set(matched.email);
      if (matched.barCouncilNo) this.barCouncilNo.set(matched.barCouncilNo);
      if (matched.mobile) this.mobile.set(matched.mobile);
      if (matched.avatar) this.avatarUrl.set(matched.avatar);
    }
  }

  loadUserSettings() {
    // Load stored user specific notification preferences
    const savedNotifs = localStorage.getItem(`notifs_${this.currentUser()?.id}`);
    if (savedNotifs) {
      try {
        const parsed = JSON.parse(savedNotifs);
        if (parsed.hearingReminders !== undefined) this.hearingReminders.set(parsed.hearingReminders);
        if (parsed.filingAlerts !== undefined) this.filingAlerts.set(parsed.filingAlerts);
        if (parsed.taskOverdue !== undefined) this.taskOverdue.set(parsed.taskOverdue);
        if (parsed.dailyCauseList !== undefined) this.dailyCauseList.set(parsed.dailyCauseList);
      } catch (e) {}
    }
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];
      const reader = new FileReader();
      reader.onload = (e) => {
        const result = e.target?.result as string;
        this.avatarUrl.set(result);
        // Persist avatar URL locally and in auth state
        this.auth.updateUserProfile({ avatar: result });
        const user = this.currentUser();
        if (user) {
          this.ds.updateUser(user.id, { avatar: result } as any).subscribe();
        }
        this.toast.success('Profile picture updated!');
      };
      reader.readAsDataURL(file);
    }
  }

  saveProfile() {
    const user = this.currentUser();
    if (!user) return;

    const payload = {
      name: this.fullName(),
      email: this.email(),
      mobile: this.mobile(),
      barCouncilNo: this.barCouncilNo()
    };

    this.ds.updateUser(user.id, payload as any).subscribe({
      next: () => {
        this.auth.updateUserProfile({ name: this.fullName(), email: this.email() });
        this.toast.success('Profile details saved successfully!');
      },
      error: () => {
        this.toast.error('Failed to save profile details.');
      }
    });
  }

  toggleNotification(prefKey: 'hearingReminders' | 'filingAlerts' | 'taskOverdue' | 'dailyCauseList') {
    if (prefKey === 'hearingReminders') this.hearingReminders.set(!this.hearingReminders());
    if (prefKey === 'filingAlerts') this.filingAlerts.set(!this.filingAlerts());
    if (prefKey === 'taskOverdue') this.taskOverdue.set(!this.taskOverdue());
    if (prefKey === 'dailyCauseList') this.dailyCauseList.set(!this.dailyCauseList());

    const notifs = {
      hearingReminders: this.hearingReminders(),
      filingAlerts: this.filingAlerts(),
      taskOverdue: this.taskOverdue(),
      dailyCauseList: this.dailyCauseList()
    };
    localStorage.setItem(`notifs_${this.currentUser()?.id}`, JSON.stringify(notifs));
    this.toast.success('Notification preferences updated!');
  }

  openChangePasswordModal() {
    this.oldPassword.set('');
    this.newPassword.set('');
    this.confirmPassword.set('');
    this.passwordError.set('');
    this.showPasswordModal.set(true);
  }

  submitChangePassword() {
    this.passwordError.set('');
    if (!this.oldPassword()) {
      this.passwordError.set('Please enter your current password.');
      return;
    }
    if (!this.newPassword() || this.newPassword().length < 6) {
      this.passwordError.set('New password must be at least 6 characters long.');
      return;
    }
    if (this.newPassword() !== this.confirmPassword()) {
      this.passwordError.set('New passwords do not match. Please verify.');
      return;
    }

    this.passwordLoading.set(true);
    this.auth.changePassword(this.oldPassword(), this.newPassword()).subscribe({
      next: () => {
        this.passwordLoading.set(false);
        this.showPasswordModal.set(false);
        this.toast.success('Your password has been changed successfully!');
      },
      error: (err) => {
        this.passwordLoading.set(false);
        this.passwordError.set(err?.error?.message || err?.message || 'Failed to change password. Check your current password.');
      }
    });
  }

  setBrand(b: BrandTheme) {
    this.theme.setBrand(b);
  }

  setMode(m: ThemeMode) {
    this.theme.setMode(m);
  }
}
