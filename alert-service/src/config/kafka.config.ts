
export const kafkaConfig = {
  clientId: 'alert-service',

  brokers: ['localhost:9092'], 
  groupId: 'alert-consumer-group',

  topics: {
    real: 'real-alert-topic',
    anomaly: 'anomaly-alert-topic',
    predicted: 'predicted-alert-topic',
  },

  consumer: {
    sessionTimeout: 30000,
    heartbeatInterval: 3000,
  },

  retry: {
    retries: 5,
    initialRetryTime: 300,
  },
};
