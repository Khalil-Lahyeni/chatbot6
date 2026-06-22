import { Module } from '@nestjs/common';
import { KafkaConsumer } from './kafka.consumer';
import { AlertModule } from '../alert/alert.module';
import { WebsocketModule } from '../websocket/websocket.module';

@Module({
  imports: [AlertModule, WebsocketModule],
  providers: [KafkaConsumer],
})
export class KafkaModule {}