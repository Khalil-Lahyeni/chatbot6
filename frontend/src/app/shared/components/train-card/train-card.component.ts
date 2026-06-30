import { ChangeDetectionStrategy, Component, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Train } from '../../models/train.model';
import { IconComponent } from '../icon/icon.component';

@Component({
  selector: 'app-train-card',
  standalone: true,
  imports: [CommonModule, RouterLink, IconComponent],
  templateUrl: './train-card.component.html',
  styleUrl: './train-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TrainCardComponent {
  train = input.required<Train>();
  readonly trainUpdated = output<Train>();

  isEditing = signal(false);

  // Form field signals for tracking changes
  editName = signal('');
  editMission = signal('');
  editDiversity = signal('');
  editBaseline = signal('');

  get lastUpdatedLabel(): string {
    const raw = this.train().updateAt;
    if (!raw) return 'Never updated';
    const diffMs = Date.now() - new Date(raw).getTime();
    const mins   = Math.floor(diffMs / 60_000);
    if (mins < 1)   return 'Just now';
    if (mins < 60)  return `${mins} min ago`;
    const hrs = Math.floor(mins / 60);
    if (hrs  < 24)  return `${hrs} h ago`;
    return `${Math.floor(hrs / 24)} d ago`;
  }

  get statusClass(): string {
    const status = this.train().status?.toLowerCase() || '';
    if (status.includes('operation') || status.includes('running') || status.includes('ok')) {
      return 'status-operation';
    } else if (status.includes('stop') || status.includes('halt') || status.includes('warn')) {
      return 'status-stopped';
    }
    return 'status-unknown';
  }

  startEdit(ev: Event): void {
    ev.stopPropagation();
    ev.preventDefault();
    const t = this.train();
    this.editName.set(t.trainName);
    this.editMission.set(t.mission);
    this.editDiversity.set(t.diversity);
    this.editBaseline.set(t.baseline || '');
    this.isEditing.set(true);
  }

  cancelChanges(ev: Event): void {
    ev.stopPropagation();
    ev.preventDefault();
    this.isEditing.set(false);
  }

  saveChanges(ev: Event): void {
    ev.stopPropagation();
    ev.preventDefault();
    const updatedTrain: Train = {
      ...this.train(),
      trainName: this.editName().trim(),
      mission: this.editMission().trim(),
      diversity: this.editDiversity().trim(),
      baseline: this.editBaseline().trim() || undefined,
    };
    this.trainUpdated.emit(updatedTrain);
    this.isEditing.set(false);
  }
}
