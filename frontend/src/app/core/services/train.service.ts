import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Train } from '../../shared/models/train.model';

export interface TrainResponse {
  id: number;
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

  /**
   * Fetches the page of trains and maps them to the frontend Train model.
   */
  getTrains(page: number = 0, size: number = 100): Observable<Train[]> {
    return this.http
      .get<Page<TrainResponse>>(`${this.baseUrl}/api/monitoring/trains`, {
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

  /**
   * Updates a train details via PUT endpoint.
   */
  updateTrain(id: number, train: Train): Observable<Train> {
    const request: TrainRequest = {
      name: train.trainName,
      mission: train.mission,
      baseline: train.baseline,
      diversity: train.diversity,
      database: train.database,
    };

    return this.http
      .put<TrainResponse>(`${this.baseUrl}/api/monitoring/trains/${id}`, request)
      .pipe(map((res) => this.mapResponseToTrain(res)));
  }

  /**
   * Maps backend TrainResponse to the UI Train model.
   */
  private mapResponseToTrain(res: TrainResponse): Train {
    return {
      id: res.id,
      trainName: res.name,
      mission: res.mission,
      diversity: res.diversity || '',
      baseline: res.baseline || '',
      database: res.database || '',
      updateAt: res.updateAt,
      // Status remains static as it is not present in the backend API
      status: res.id % 2 === 0 ? 'Stopped' : 'In operation',
    };
  }
}
