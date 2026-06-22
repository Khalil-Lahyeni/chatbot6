import { Component, OnInit, inject, signal, effect } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AuthService } from './core/services/auth.service';
import { LoadingSpinner } from './shared/components/loading-spinner/loading-spinner';
import { SidebarComponent } from './shared/components/sidebar/sidebar.component';
import { HeaderComponent } from './shared/components/header/header.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, LoadingSpinner, SidebarComponent, HeaderComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App implements OnInit {
  readonly auth = inject(AuthService);
  readonly collapsed = signal(true);

  constructor() {
    effect(() => {
      const user = this.auth.currentUser();
      if (user) {
        console.log('[Auth] Session active →', user.username, '| Rôles:', user.roles);
      } else if (!this.auth.isLoading()) {
        console.log('[Auth] Aucune session active');
      }
    });
  }

  ngOnInit(): void {
    this.auth.checkSession().subscribe();
  }

  onCollapsedChange(value: boolean): void {
    this.collapsed.set(value);
  }
}
