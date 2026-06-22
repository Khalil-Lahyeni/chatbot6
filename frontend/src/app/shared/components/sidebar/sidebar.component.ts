import { ChangeDetectionStrategy, Component, input, output, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { IconComponent } from '../icon/icon.component';
import { NavItem } from '../../models/nav-item.model';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, IconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss',
})
export class SidebarComponent {
  collapsed = input.required<boolean>();
  collapsedChange = output<boolean>();

  readonly items = signal<NavItem[]>([
    { id: 'dashboard', label: 'Dashboard', icon: 'grid',     route: '/dashboard', badge: null },
    { id: 'alerts',    label: 'Alerts',    icon: 'bell',     route: '/alerts',    badge: null },
    { id: 'trains',    label: 'Trains',    icon: 'train',    route: '/trains',    badge: null },
    { id: 'settings',  label: 'Settings',  icon: 'settings', route: '/settings',  badge: null },
    { id: 'profile',   label: 'Profile',   icon: 'user',     route: '/profile',   badge: null },
  ]);

  onEnter() { this.collapsedChange.emit(false); }
  onLeave() { this.collapsedChange.emit(true); }
}
