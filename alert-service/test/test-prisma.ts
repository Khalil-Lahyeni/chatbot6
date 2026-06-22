import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

async function main() {
  const alert = await prisma.alert.create({
    data: {
      message: "Test alert",
      type: "INFO",
      status: "NEW",
    },
  });

  console.log(alert);
}

main()
  .catch(console.error)
  .finally(() => prisma.$disconnect());