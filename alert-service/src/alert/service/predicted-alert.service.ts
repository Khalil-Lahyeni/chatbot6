import { Injectable } from '@nestjs/common';
import { PrismaService } from '../../config/prisma.service';

@Injectable()
export class PredictedAlertService {
  constructor(private prisma: PrismaService) {}

  async create(data: any) {
    return this.prisma.predictedAlert.create({
      data: {
        trainId: data.trainId,
        probability: data.probability,
        type: data.type,
        description: data.description,
      },
    });
  }
}