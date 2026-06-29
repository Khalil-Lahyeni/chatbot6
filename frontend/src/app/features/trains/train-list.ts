import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Train } from '../../shared/models/train.model';
import { TrainCardComponent } from '../../shared/components/train-card/train-card.component';
import { AddTrainCardComponent } from '../../shared/components/add-train-card/add-train-card.component';
import { TrainFilterService } from '../../core/services/train-filter.service';
import { TrainService } from '../../core/services/train.service';

@Component({
  selector: 'app-train-list',
  standalone: true,
  imports: [CommonModule, TrainCardComponent, AddTrainCardComponent],
  templateUrl: './train-list.html',
  styleUrl: './train-list.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TrainListComponent implements OnInit {
  private readonly filterService = inject(TrainFilterService);
  private readonly trainService = inject(TrainService);

  readonly trains = signal<Train[]>([]);

  ngOnInit(): void {
    this.trainService.getTrains().subscribe((trains) => {
      this.trains.set(trains);
    });
  }

  readonly filteredTrains = computed(() => {
    let result = [...this.trains()];

    // Search query filtering (by name only)
    const search = this.filterService.searchQuery().trim().toLowerCase();
    if (search) {
      result = result.filter((train) =>
        train.trainName.toLowerCase().includes(search)
      );
    }

    // Mission filtering
    const missions = this.filterService.selectedMissions();
    if (missions.length > 0) {
      result = result.filter((train) => missions.includes(train.mission));
    }

    // Status filtering
    const statuses = this.filterService.selectedStatuses();
    if (statuses.length > 0) {
      result = result.filter((train) => statuses.includes(train.status));
    }

    // Baseline filtering
    const baselines = this.filterService.selectedBaselines();
    if (baselines.length > 0) {
      result = result.filter((train) => {
        const bl = train.baseline || 'N/A';
        return baselines.includes(bl);
      });
    }

    // Sorting
    const sort = this.filterService.sortBy();
    result.sort((a, b) => {
      if (sort === 'name') {
        return a.trainName.localeCompare(b.trainName, undefined, { numeric: true, sensitivity: 'base' });
      } else if (sort === 'status') {
        return a.status.localeCompare(b.status);
      } else if (sort === 'lastUpdated') {
        return b.id - a.id;
      }
      return 0;
    });

    return result;
  });

  onTrainCreated(newTrain: Train): void {
    this.trainService.createTrain(newTrain).subscribe((saved) => {
      this.trains.update((list) => [...list, saved]);
    });
  }

  onTrainUpdated(updated: Train): void {
    this.trainService.updateTrain(updated.id, updated).subscribe((saved) => {
      this.trains.update((list) =>
        list.map((t) => (t.id === saved.id ? saved : t))
      );
    });
  }
}
