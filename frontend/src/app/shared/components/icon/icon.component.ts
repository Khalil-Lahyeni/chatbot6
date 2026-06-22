import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { IconName } from '../../models/nav-item.model';

@Component({
  selector: 'app-icon',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <svg
      [attr.width]="size()"
      [attr.height]="size()"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      [attr.stroke-width]="stroke()"
      stroke-linecap="round"
      stroke-linejoin="round">
      @switch (name()) {
        @case ('grid') {
          <rect x="3" y="3" width="7" height="7" rx="1.5"/>
          <rect x="14" y="3" width="7" height="7" rx="1.5"/>
          <rect x="3" y="14" width="7" height="7" rx="1.5"/>
          <rect x="14" y="14" width="7" height="7" rx="1.5"/>
        }
        @case ('radio') {
          <circle cx="12" cy="12" r="2"/>
          <path d="M16.24 7.76a6 6 0 0 1 0 8.49M7.76 16.24a6 6 0 0 1 0-8.49M19.07 4.93a10 10 0 0 1 0 14.14M4.93 19.07a10 10 0 0 1 0-14.14"/>
        }
        @case ('wrench') {
          <path d="M14.7 6.3a4 4 0 0 0-5.4 5.4L3 18l3 3 6.3-6.3a4 4 0 0 0 5.4-5.4l-2.5 2.5-2.5-2.5z"/>
        }
        @case ('alert') {
          <path d="M10.3 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.7 3.86a2 2 0 0 0-3.42 0z"/>
          <path d="M12 9v4"/>
          <circle cx="12" cy="17" r=".5" fill="currentColor"/>
        }
        @case ('settings') {
          <circle cx="12" cy="12" r="3"/>
          <path d="M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z"/>
        }
        @case ('bell') {
          <path d="M6 8a6 6 0 0 1 12 0c0 7 3 8 3 8H3s3-1 3-8"/>
          <path d="M10.3 21a2 2 0 0 0 3.4 0"/>
        }
        @case ('search') {
          <circle cx="11" cy="11" r="7"/>
          <path d="m21 21-4.3-4.3"/>
        }
        @case ('user') {
          <circle cx="12" cy="8" r="4"/>
          <path d="M4 21a8 8 0 0 1 16 0"/>
        }
        @case ('logout') {
          <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
          <path d="m16 17 5-5-5-5"/>
          <path d="M21 12H9"/>
        }
        @case ('chevron')  { <path d="m9 18 6-6-6-6"/> }
        @case ('chevronD') { <path d="m6 9 6 6 6-6"/> }
        @case ('check')    { <path d="m5 13 4 4L19 7"/> }
        @case ('dot')      { <circle cx="12" cy="12" r="4" fill="currentColor" stroke="none"/> }
        @case ('arrow') {
          <path d="M5 12h14"/>
          <path d="m13 6 6 6-6 6"/>
        }
        @case ('train') {
          <rect x="4" y="3" width="16" height="16" rx="2"/>
          <path d="M4 11h16"/>
          <path d="M12 3v8"/>
          <path d="m8 19-2 3"/>
          <path d="m16 19 2 3"/>
          <circle cx="8" cy="15" r="1.5" fill="currentColor" stroke="none"/>
          <circle cx="16" cy="15" r="1.5" fill="currentColor" stroke="none"/>
        }
        @case ('filter') {
          <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/>
        }
        @case ('sort') {
          <path d="m21 16-4 4-4-4M17 20V4M3 8l4-4 4 4M7 4v16"/>
        }
        @case ('edit') {
          <path d="M12 20h9M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5z"/>
        }
      }
    </svg>
  `,
})
export class IconComponent {
  name = input.required<IconName>();
  size = input<number>(18);
  stroke = input<number>(1.6);
}
