import { Injectable } from '@nestjs/common';
import { PrismaService } from '../../config/prisma.service';

@Injectable()
export class AnomalyAlertService {
  constructor(private prisma: PrismaService) {}

  async create(data: any) {
    return this.prisma.anomalyAlert.create({
      data: {
        trainId: data.trainId,
        probability: data.probability,
        type: data.type,
        description: data.description,
      },
    });
  }
}