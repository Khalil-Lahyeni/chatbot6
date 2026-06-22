import { Module } from '@nestjs/common';
import { PrismaService } from '../config/prisma.service';

// services
import { RealAlertService } from './service/real-alert.service';
import { AnomalyAlertService } from './service/anomaly-alert.service';
import { PredictedAlertService } from './service/predicted-alert.service';

// handlers
import { RealAlertHandler } from './handlers/real-alert.handler';
import { AnomalyAlertHandler } from './handlers/anomaly-alert.handler';
import { PredictedAlertHandler } from './handlers/predicted-alert.handler';


// ✅ import gateway
import { AlertGateway } from '../websocket/alert.gateway';

@Module({
  providers: [
    PrismaService,

    RealAlertService,
    AnomalyAlertService,
    PredictedAlertService,

    RealAlertHandler,
    AnomalyAlertHandler,
    PredictedAlertHandler,
    AlertGateway,
    {
      provide: 'KAFKA_HANDLERS',
      useFactory: (
        real: RealAlertHandler,
        anomaly: AnomalyAlertHandler,
        predicted: PredictedAlertHandler,
      ) => [real, anomaly, predicted],
      inject: [
        RealAlertHandler,
        AnomalyAlertHandler,
        PredictedAlertHandler,
        
      ],
    },
  ],
  exports: ['KAFKA_HANDLERS'],
})
export class AlertModule {}