export interface Train {
  id: number;
  trainName: string;
  mission: string;
  diversity: string;
  status: 'In operation' | 'Stopped' | string;
  baseline?: string;
  database?: string;
  updateAt?: string;
}
