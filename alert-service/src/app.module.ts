import { Module } from '@nestjs/common';
import { AppController } from './app.controller';
import { AppService } from './app.service';
import { AlertModule } from './alert/alert.module';
import { KafkaModule } from './kafka/kafka.module';
import { WebsocketModule } from './websocket/websocket.module';

@Module({
  imports: [AlertModule , KafkaModule , WebsocketModule],
  controllers: [AppController],
  providers: [AppService],
})
export class AppModule {}
