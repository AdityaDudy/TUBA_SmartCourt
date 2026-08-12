import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { DataService } from '../../../../core/services/data.service';
import { AuthService } from '../../../../core/services/auth.service';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Masters, MASTER_CARDS, MasterCard } from '../../../../core/models';

import { AppButtonComponent } from '../../../../shared/components/button/button.component';

@Component({
  selector: 'app-masters-page',
  standalone: true,
  imports: [CommonModule, FormsModule, AppButtonComponent],
  templateUrl: './masters-page.component.html',
  styleUrl: './masters-page.component.scss'
})
export class MastersPageComponent implements OnInit {
  private ds = inject(DataService);
  private auth = inject(AuthService);
  private router = inject(Router);

  masters = this.ds.masters;

  ngOnInit() {
    if (!this.auth.hasPermission('manage_users')) {
      this.router.navigate(['/app/dashboard']);
      return;
    }
    this.ds.loadMasters().subscribe();
  }

  // Local overrides/customizations for master cards
  customCards = signal<Record<string, Partial<MasterCard>>>(this.loadCustomCards());

  // Dynamic merged cards list
  allMasterCards = computed(() => {
    const backendMasters = this.masters() || {};
    const customs = this.customCards();
    
    // Start with predefined cards and merge overrides
    const cards = MASTER_CARDS.map(c => {
      const override = customs[c.key];
      return override ? { ...c, ...override } as MasterCard : c;
    });

    // Dynamically add cards for backend keys not defined in MASTER_CARDS
    Object.keys(backendMasters).forEach(key => {
      if (!cards.some(c => c.key === key)) {
        const override = customs[key];
        cards.push({
          key: key as keyof Masters,
          title: override?.title || this.formatCategoryTitle(key),
          icon: override?.icon || 'fa-folder-open',
          bg: override?.bg || '#f3f4f6',
          tc: override?.tc || '#4b5563'
        });
      }
    });

    return cards;
  });

  // Modal State for managing items
  selectedCard = signal<MasterCard | null>(null);
  modalItems = signal<string[]>([]);
  newVal = signal('');

  // Inline value editing
  editingItemIndex = signal<number | null>(null);
  editingItemValue = signal('');

  // Modals for Categories
  showAddCategoryForm = signal(false);
  newCategoryTitle = signal('');
  newCategoryIcon = signal('fa-folder-open');
  newCategoryBg = signal('#f3f4f6');
  newCategoryTc = signal('#4b5563');

  showEditCategoryForm = signal(false);
  editCategoryTitle = signal('');
  editCategoryIcon = signal('');
  editCategoryBg = signal('');
  editCategoryTc = signal('');
  cardToEdit = signal<MasterCard | null>(null);



  private loadCustomCards(): Record<string, Partial<MasterCard>> {
    try {
      const saved = localStorage.getItem('custom_master_cards');
      return saved ? JSON.parse(saved) : {};
    } catch {
      return {};
    }
  }

  private saveCustomCards(customs: Record<string, Partial<MasterCard>>) {
    try {
      localStorage.setItem('custom_master_cards', JSON.stringify(customs));
    } catch (e) {
      console.error('Failed to save custom cards to localStorage', e);
    }
  }

  formatCategoryTitle(key: string): string {
    const result = key.replace(/([A-Z])/g, " $1").replace(/_/g, ' ');
    return result.charAt(0).toUpperCase() + result.slice(1).trim();
  }

  // Open manage modal
  openManage(card: MasterCard) {
    this.selectedCard.set(card);
    const currentItems = this.masters()?.[card.key] || [];
    this.modalItems.set([...currentItems]);
    this.newVal.set('');
    this.cancelEditItem();
  }

  // Add Item to Local List
  addLocalItem() {
    const value = this.newVal().trim();
    console.log('addLocalItem called. Value to add:', value);
    if (!value) return;
    
    // Check for duplicates
    if (this.modalItems().includes(value)) {
      console.log('Value already exists in list:', value);
      return;
    }

    this.modalItems.update(items => {
      const updated = [...items, value];
      console.log('Updated local modalItems to:', updated);
      return updated;
    });
    this.newVal.set('');
  }

  // Edit inline item
  startEditItem(index: number, value: string) {
    this.editingItemIndex.set(index);
    this.editingItemValue.set(value);
  }

  cancelEditItem() {
    this.editingItemIndex.set(null);
    this.editingItemValue.set('');
  }

  saveEditItem(index: number) {
    const val = this.editingItemValue().trim();
    if (!val) return;
    this.modalItems.update(items => {
      const copy = [...items];
      copy[index] = val;
      return copy;
    });
    this.cancelEditItem();
  }

  // Remove Item from Local List
  removeLocalItem(index: number) {
    this.modalItems.update(items => items.filter((_, i) => i !== index));
    if (this.editingItemIndex() === index) {
      this.cancelEditItem();
    }
  }

  // Reordering: Move Up
  moveUp(index: number) {
    if (index === 0) return;
    this.modalItems.update(items => {
      const copy = [...items];
      const temp = copy[index];
      copy[index] = copy[index - 1];
      copy[index - 1] = temp;
      return copy;
    });
    this.cancelEditItem();
  }

  // Reordering: Move Down
  moveDown(index: number) {
    if (index === this.modalItems().length - 1) return;
    this.modalItems.update(items => {
      const copy = [...items];
      const temp = copy[index];
      copy[index] = copy[index + 1];
      copy[index + 1] = temp;
      return copy;
    });
    this.cancelEditItem();
  }

  // Save changes to backend
  save() {
    const card = this.selectedCard();
    if (!card) return;

    this.ds.updateMasterCategory(card.key, this.modalItems()).subscribe({
      next: () => {
        this.selectedCard.set(null);
        // Refresh master data
        this.ds.loadMasters().subscribe();
      }
    });
  }

  // Add Master Category
  addCategory() {
    const title = this.newCategoryTitle().trim();
    if (!title) return;

    // Generate camelCase key
    const key = title
      .replace(/(?:^\w|[A-Z]|\b\w)/g, (word, index) => index === 0 ? word.toLowerCase() : word.toUpperCase())
      .replace(/\s+/g, '');

    const newCard: Partial<MasterCard> = {
      key: key as keyof Masters,
      title: title,
      icon: this.newCategoryIcon(),
      bg: this.newCategoryBg(),
      tc: this.newCategoryTc()
    };

    // Save override locally
    const customs = { ...this.customCards(), [key]: newCard };
    this.customCards.set(customs);
    this.saveCustomCards(customs);

    // Call backend to create category with empty items
    this.ds.updateMasterCategory(key as keyof Masters, []).subscribe({
      next: () => {
        this.showAddCategoryForm.set(false);
        this.newCategoryTitle.set('');
        this.ds.loadMasters().subscribe();
      }
    });
  }

  // Edit Master Category Card
  openEditCategory(card: MasterCard, event: Event) {
    event.stopPropagation(); // Avoid opening the manage modal
    this.cardToEdit.set(card);
    this.editCategoryTitle.set(card.title);
    this.editCategoryIcon.set(card.icon);
    this.editCategoryBg.set(card.bg);
    this.editCategoryTc.set(card.tc);
    this.showEditCategoryForm.set(true);
  }

  saveCategoryEdit() {
    const card = this.cardToEdit();
    if (!card) return;

    const updatedCard: Partial<MasterCard> = {
      title: this.editCategoryTitle().trim(),
      icon: this.editCategoryIcon(),
      bg: this.editCategoryBg(),
      tc: this.editCategoryTc()
    };

    const customs = { ...this.customCards(), [card.key]: updatedCard };
    this.customCards.set(customs);
    this.saveCustomCards(customs);

    this.showEditCategoryForm.set(false);
    this.cardToEdit.set(null);
  }

  // Helper to count entries safely
  getValuesCount(key: keyof Masters): number {
    const m = this.masters();
    if (!m) return 0;
    return m[key]?.length || 0;
  }
}
