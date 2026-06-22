import { Controller, Get, Post, Body, Param, Delete } from '@nestjs/common';
import { AlertService } from '../service/alert.service';

@Controller('alerts')
export class AlertController {
  constructor(private readonly alertService: AlertService) {}

  @Post()
  create(@Body() body: { message: string; type: string; status: string }) {
    return this.alertService.createAlert(body);
  }

  @Get()
  findAll() {
    return this.alertService.getAllAlerts();
  }

  @Get(':id')
  findOne(@Param('id') id: string) {
    return this.alertService.getAlertById(id);
  }

  @Delete(':id')
  remove(@Param('id') id: string) {
    return this.alertService.deleteAlert(id);
  }
}
