
export interface KafkaHandler {
  topic: string;
  handle(data: any): Promise<void>;
}
