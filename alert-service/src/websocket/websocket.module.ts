import { Module } from '@nestjs/common';
import { AlertGateway } from './alert.gateway';

@Module({
  providers: [AlertGateway],
  exports: [AlertGateway],
})
export class WebsocketModule {}