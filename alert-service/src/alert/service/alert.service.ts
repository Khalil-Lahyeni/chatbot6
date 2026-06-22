import { Injectable } from '@nestjs/common';
import { PrismaService } from '../../config/prisma.service';

@Injectable()
export class AlertService {
  constructor(private prisma: PrismaService) {}

  async createAlert(data: {
    message: string;
    type: string;
    status: string;
  }) {
    return this.prisma.alert.create({
      data: data,
    });
  }

  async getAllAlerts() {
    return this.prisma.alert.findMany({
      orderBy: { createdAt: 'desc' },
    });
  }

  async getAlertById(id: string) {
    return this.prisma.alert.findUnique({
      where: { id },
    });
  }

  async deleteAlert(id: string) {
    return this.prisma.alert.delete({
      where: { id },
    });
  }
}
