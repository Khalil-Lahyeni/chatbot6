
import { Injectable, Logger } from '@nestjs/common';
import { PredictedAlertService } from '../service/predicted-alert.service';
import { AlertGateway } from '../../websocket/alert.gateway';
import { KafkaHandler } from '../../kafka/interfaces/kafka-handler.interface';
import { kafkaConfig } from '../../config/kafka.config';


@Injectable()
export class PredictedAlertHandler implements KafkaHandler {
  topic = kafkaConfig.topics.predicted;
  private logger = new Logger(PredictedAlertHandler.name);
  constructor(
    private service: PredictedAlertService,
    private gateway: AlertGateway,
  ) {}

  async handle(data: any) {
    this.gateway.sendPredictedAlert({ type: 'PREDICTED', ...data });

    this.service.create(data).catch(console.error);
  }
}