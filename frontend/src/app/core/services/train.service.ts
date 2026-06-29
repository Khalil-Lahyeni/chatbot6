import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Train } from '../../shared/models/train.model';

export interface TrainResponse {
  trainId: number;
  name: string;
  mission: string;
  baseline?: string;
  diversity?: string;
  database?: string;
  updateAt?: string;
}

export interface TrainRequest {
  name: string;
  mission: string;
  baseline?: string;
  diversity?: string;
  database?: string;
}

export interface Page<T> {
  content: T[];
  totalElements?: number;
  totalPages?: number;
  size?: number;
  number?: number;
}

@Injectable({
  providedIn: 'root',
})
export class TrainService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiGatewayUrl;
  private readonly trainsUrl = `${this.baseUrl}${environment.api.trains}`;

  getTrains(page: number = 0, size: number = 100): Observable<Train[]> {
    return this.http
      .get<Page<TrainResponse>>(`${this.trainsUrl}`, {
        params: {
          page: page.toString(),
          size: size.toString(),
        },
      })
      .pipe(
        map((pageObj) => {
          const content = pageObj.content || [];
          return content.map((res) => this.mapResponseToTrain(res));
        })
      );
  }

  createTrain(train: Train): Observable<Train> {
    const request: TrainRequest = {
      name: train.trainName,
      mission: train.mission,
      baseline: train.baseline,
      diversity: train.diversity,
      database: train.database,
    };
    return this.http
      .post<TrainResponse>(`${this.trainsUrl}`, request)
      .pipe(map((res) => this.mapResponseToTrain(res)));
  }

  updateTrain(id: number, train: Train): Observable<Train> {
    const request: TrainRequest = {
      name: train.trainName,
      mission: train.mission,
      baseline: train.baseline,
      diversity: train.diversity,
      database: train.database,
    };

    return this.http
      .put<TrainResponse>(`${this.trainsUrl}/${id}`, request)
      .pipe(map((res) => this.mapResponseToTrain(res)));
  }

  private mapResponseToTrain(res: TrainResponse): Train {
    return {
      id: res.trainId,
      trainName: res.name,
      mission: res.mission,
      diversity: res.diversity || '',
      baseline: res.baseline || '',
      database: res.database || '',
      updateAt: res.updateAt,
      status: res.trainId % 2 === 0 ? 'Stopped' : 'In operation',
    };
  }
}
