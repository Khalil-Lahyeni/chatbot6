import { Injectable, computed, signal } from '@angular/core';

export interface ActiveFilter {
  id: string;
  type: 'mission' | 'status' | 'baseline';
  value: string;
  label: string;
}

@Injectable({
  providedIn: 'root',
})
export class TrainFilterService {
  readonly searchQuery = signal<string>('');
  readonly sortBy = signal<string>('name'); // 'name' | 'status' | 'lastUpdated'
  
  readonly selectedMissions = signal<string[]>([]);
  readonly selectedStatuses = signal<string[]>([]);
  readonly selectedBaselines = signal<string[]>([]);

  // Derived signal for all active filters (used for chips)
  readonly activeFilters = computed<ActiveFilter[]>(() => {
    const list: ActiveFilter[] = [];
    
    this.selectedMissions().forEach((mission) => {
      list.push({
        id: `mission-${mission}`,
        type: 'mission',
        value: mission,
        label: `Mission: ${mission}`,
      });
    });

    this.selectedStatuses().forEach((status) => {
      list.push({
        id: `status-${status}`,
        type: 'status',
        value: status,
        label: `Status: ${status}`,
      });
    });

    this.selectedBaselines().forEach((baseline) => {
      list.push({
        id: `baseline-${baseline}`,
        type: 'baseline',
        value: baseline,
        label: `Baseline: ${baseline}`,
      });
    });

    return list;
  });

  toggleMission(mission: string): void {
    this.selectedMissions.update((prev) =>
      prev.includes(mission) ? prev.filter((m) => m !== mission) : [...prev, mission]
    );
  }

  toggleStatus(status: string): void {
    this.selectedStatuses.update((prev) =>
      prev.includes(status) ? prev.filter((s) => s !== status) : [...prev, status]
    );
  }

  toggleBaseline(baseline: string): void {
    this.selectedBaselines.update((prev) =>
      prev.includes(baseline) ? prev.filter((b) => b !== baseline) : [...prev, baseline]
    );
  }

  removeFilter(filter: ActiveFilter): void {
    if (filter.type === 'mission') {
      this.selectedMissions.update((prev) => prev.filter((m) => m !== filter.value));
    } else if (filter.type === 'status') {
      this.selectedStatuses.update((prev) => prev.filter((s) => s !== filter.value));
    } else if (filter.type === 'baseline') {
      this.selectedBaselines.update((prev) => prev.filter((b) => b !== filter.value));
    }
  }

  clearAll(): void {
    this.searchQuery.set('');
    this.sortBy.set('name');
    this.selectedMissions.set([]);
    this.selectedStatuses.set([]);
    this.selectedBaselines.set([]);
  }
}
