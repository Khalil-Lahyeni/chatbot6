import { ChangeDetectionStrategy, Component, computed, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IconComponent } from '../icon/icon.component';
import { Train } from '../../models/train.model';

@Component({
  selector: 'app-add-train-card',
  standalone: true,
  imports: [CommonModule, IconComponent],
  templateUrl: './add-train-card.component.html',
  styleUrl: './add-train-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AddTrainCardComponent {
  readonly trainCreated = output<Train>();

  readonly isOpen = signal(false);

  readonly formTrainId   = signal('');
  readonly formName      = signal('');
  readonly formMission   = signal('');
  readonly formDiversity = signal('');
  readonly formBaseline  = signal('');

  readonly isValid = computed(() => {
    const id = Number(this.formTrainId().trim());
    return Number.isInteger(id) && id > 0 && this.formName().trim().length > 0;
  });

  open(): void {
    this.isOpen.set(true);
  }

  cancel(): void {
    this.isOpen.set(false);
    this.reset();
  }

  save(): void {
    if (!this.isValid()) return;
    const train: Train = {
      id: Number(this.formTrainId().trim()),
      trainName: this.formName().trim(),
      mission: this.formMission().trim(),
      diversity: this.formDiversity().trim(),
      baseline: this.formBaseline().trim() || undefined,
      status: 'In operation',
    };
    this.trainCreated.emit(train);
    this.isOpen.set(false);
    this.reset();
  }

  private reset(): void {
    this.formTrainId.set('');
    this.formName.set('');
    this.formMission.set('');
    this.formDiversity.set('');
    this.formBaseline.set('');
  }
}
