import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { TrainService } from '../../core/services/train.service';
import { IconComponent } from '../../shared/components/icon/icon.component';
import { TrustUrlPipe } from '../../shared/pipes/trust-url.pipe';
import { Train } from '../../shared/models/train.model';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-train-dashboard',
  standalone: true,
  imports: [CommonModule, IconComponent, TrustUrlPipe],
  templateUrl: './train-dashboard.html',
  styleUrl: './train-dashboard.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TrainDashboardComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly trainService = inject(TrainService);

  readonly train   = signal<Train | null>(null);
  readonly loading = signal(true);
  readonly error   = signal<string | null>(null);

  readonly statusClass = computed(() => {
    const s = this.train()?.status?.toLowerCase() ?? '';
    if (s.includes('operation')) return 'status-operation';
    if (s.includes('stopped'))   return 'status-stopped';
    return 'status-unknown';
  });

  readonly grafanaUrl = computed(() => {
    const id = this.train()?.id;
    if (!id) return environment.grafanaDashboardUrl;
    return `${environment.grafanaDashboardUrl}&var-trainId=${id}`;
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.trainService.getTrainById(id).subscribe({
      next: (train) => {
        this.train.set(train);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load train data.');
        this.loading.set(false);
      },
    });
  }

  goBack(): void {
    this.router.navigate(['/trains']);
  }
}
