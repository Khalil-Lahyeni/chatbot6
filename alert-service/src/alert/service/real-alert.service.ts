import { Injectable } from '@nestjs/common';
import { PrismaService } from '../../config/prisma.service';

@Injectable()
export class RealAlertService {
  constructor(private prisma: PrismaService) {}

  async create(data: any) {
    return this.prisma.realAlert.create({
      data: {
        trainId: data.trainId,
        type: data.type,
        callState: data.callState,
        description: data.description,
        car: data.car,
        carName: data.carName,
        intercom: data.intercom,
        cameras: data.cameras,
      },
    });
  }
}
