import { Injectable, OnModuleInit, Inject, Logger } from '@nestjs/common';
import { Kafka } from 'kafkajs';
import { kafkaConfig } from '../config/kafka.config';
import { KafkaHandler } from './interfaces/kafka-handler.interface';

@Injectable()
export class KafkaConsumer implements OnModuleInit {
  private logger = new Logger(KafkaConsumer.name);

  private kafka = new Kafka({
    clientId: kafkaConfig.clientId,
    brokers: kafkaConfig.brokers,
    retry: kafkaConfig.retry,

  });

  private consumer = this.kafka.consumer({
  groupId: kafkaConfig.groupId,
  sessionTimeout: kafkaConfig.consumer.sessionTimeout,
  heartbeatInterval: kafkaConfig.consumer.heartbeatInterval,

  });

  private handlerMap: Record<string, KafkaHandler>;

  constructor(
    @Inject('KAFKA_HANDLERS')
    private handlers: KafkaHandler[],
  ) {
    this.handlerMap = Object.fromEntries(
      handlers.map(h => [h.topic, h]),
    );
  }

  async onModuleInit() {
    await this.consumer.connect();

    await this.consumer.subscribe({
      topics: Object.keys(this.handlerMap),
    });

    await this.consumer.run({
      eachMessage: async ({ topic, message }) => {
        try {
          const value = message.value?.toString();
          if (!value) return;

          const data = JSON.parse(value);

          const handler = this.handlerMap[topic];

          if (!handler) {
            this.logger.warn(`Unknown topic: ${topic}`);
            return;
          }

          await handler.handle(data);

        } catch (err) {
          this.logger.error('Kafka error', err);
        }
      },
    });
  }
}