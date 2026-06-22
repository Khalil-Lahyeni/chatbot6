import {
  WebSocketGateway,
  WebSocketServer,
} from '@nestjs/websockets';
import { Server } from 'socket.io';

@WebSocketGateway({
  cors: {
    origin: '*',
  },
})
export class AlertGateway {
  @WebSocketServer()
  server!: Server;

  sendAlert(alert: any) {
    this.server.emit('new-alert', alert);
  }

  
  sendRealAlert(data: any) {
    console.log('🚆 WS REAL ALERT');
    this.server.emit('real-alert', data);
  }

  
  sendAnomalyAlert(data: any) {
    console.log('⚠️ WS ANOMALY ALERT');
    this.server.emit('anomaly-alert', data);
  }

  
  sendPredictedAlert(data: any) {
    console.log('🔮 WS PREDICTED ALERT');
    this.server.emit('predicted-alert', data);
  }

}
