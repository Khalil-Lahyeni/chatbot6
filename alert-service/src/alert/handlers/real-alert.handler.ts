import { Injectable, Logger } from '@nestjs/common';
import { RealAlertService } from '../service/real-alert.service';
import { AlertGateway } from '../../websocket/alert.gateway';
import { KafkaHandler } from '../../kafka/interfaces/kafka-handler.interface';
import { kafkaConfig } from '../../config/kafka.config';


@Injectable()
export class RealAlertHandler implements KafkaHandler {
  topic = kafkaConfig.topics.real;
  private logger = new Logger(RealAlertHandler.name);

  constructor(
    private service: RealAlertService,
    private gateway: AlertGateway,
  ) {}

  async handle(data: any): Promise<void> {
    this.gateway.sendRealAlert({
      type: 'REAL',
      ...data,
    });

    this.service.create(data)
      .then(() => this.logger.log('✅ RealAlert saved'))
      .catch(err => this.logger.error('❌ DB error REAL', err));
  }
}