
import { Injectable, Logger } from '@nestjs/common';
import { AnomalyAlertService } from '../service/anomaly-alert.service';
import { AlertGateway } from '../../websocket/alert.gateway';
import { KafkaHandler } from '../../kafka/interfaces/kafka-handler.interface';
import { kafkaConfig } from '../../config/kafka.config';

@Injectable()

export class AnomalyAlertHandler implements KafkaHandler {
  topic = kafkaConfig.topics.anomaly;
  private logger = new Logger(AnomalyAlertHandler.name);
  
  constructor(
    private service: AnomalyAlertService,
    private gateway: AlertGateway,
  ) {}

  async handle(data: any) {
    this.gateway.sendAnomalyAlert({ type: 'ANOMALY', ...data });
    this.service.create(data).catch(console.error);
  }
}

