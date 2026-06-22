import {
  ChangeDetectionStrategy, Component, ElementRef,
  HostListener, computed, inject, signal
} from '@angular/core';
import { Router, NavigationEnd, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter, map } from 'rxjs/operators';
import { IconComponent } from '../icon/icon.component';
import { AlertItem } from '../../models/nav-item.model';
import { AuthService } from '../../../core/services/auth.service';
import { TrainFilterService } from '../../../core/services/train-filter.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [IconComponent, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss',
})
export class HeaderComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  readonly filterService = inject(TrainFilterService);
  private host = inject(ElementRef<HTMLElement>);

  readonly isTrainsPage = toSignal(
    this.router.events.pipe(
      filter((e) => e instanceof NavigationEnd),
      map(() => this.router.url.split('?')[0] === '/trains')
    ),
    { initialValue: this.router.url.split('?')[0] === '/trains' }
  );

  readonly user = computed(() => {
    const u = this.authService.currentUser();
    if (!u) return { name: 'Guest', initials: 'G', email: '', role: '' };
    const parts = u.username.split(/[\s._-]/);
    const initials = parts.length >= 2
      ? (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
      : u.username.slice(0, 2).toUpperCase();
    return { name: u.username, initials, email: u.email, role: u.roles?.[0] ?? 'Operator' };
  });

  readonly alerts = signal<AlertItem[]>([
    { id: 'ALT-001', level: 'critical', train: 'TGV-7701', msg: 'Brake system overheating',         time: '2 min ago',  line: 'South-East' },
    { id: 'ALT-002', level: 'critical', train: 'TGV-7701', msg: 'GPS communication lost',           time: '4 min ago',  line: 'South-East' },
    { id: 'ALT-003', level: 'warning',  train: 'TER-0942', msg: 'Battery voltage dropping (71%)',   time: '12 min ago', line: 'North-East' },
    { id: 'ALT-004', level: 'warning',  train: 'TER-1187', msg: 'Bogie 3 vibrations out of range',  time: '18 min ago', line: 'Occitania'  },
    { id: 'ALT-005', level: 'info',     train: 'RER-A-31', msg: 'Preventive maintenance scheduled', time: '34 min ago', line: 'RER-A'      },
    { id: 'ALT-006', level: 'info',     train: 'INT-2210', msg: 'Firmware update successful',       time: '1 h ago',    line: 'Brittany'   },
  ]);

  notifOpen = signal(false);
  profileOpen = signal(false);
  sortOpen = signal(false);
  filterOpen = signal(false);

  toggleNotif(ev: Event) {
    ev.stopPropagation();
    this.notifOpen.update(v => !v);
    this.profileOpen.set(false);
    this.sortOpen.set(false);
    this.filterOpen.set(false);
  }

  toggleProfile(ev: Event) {
    ev.stopPropagation();
    this.profileOpen.update(v => !v);
    this.notifOpen.set(false);
    this.sortOpen.set(false);
    this.filterOpen.set(false);
  }

  toggleSort(ev: Event) {
    ev.stopPropagation();
    this.sortOpen.update(v => !v);
    this.filterOpen.set(false);
    this.notifOpen.set(false);
    this.profileOpen.set(false);
  }

  toggleFilter(ev: Event) {
    ev.stopPropagation();
    this.filterOpen.update(v => !v);
    this.sortOpen.set(false);
    this.notifOpen.set(false);
    this.profileOpen.set(false);
  }

  logout(): void {
    this.authService.logout();
  }

  onSearch(event: Event) {
    const input = event.target as HTMLInputElement;
    this.filterService.searchQuery.set(input.value);
  }

  @HostListener('document:click', ['$event'])
  onOutsideClick(ev: MouseEvent) {
    if (!this.host.nativeElement.contains(ev.target as Node)) {
      this.notifOpen.set(false);
      this.profileOpen.set(false);
      this.sortOpen.set(false);
      this.filterOpen.set(false);
    }
  }
}
