-- CreateTable
CREATE TABLE "Alert" (
    "id" TEXT NOT NULL,
    "message" TEXT,
    "type" TEXT,
    "status" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "Alert_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "real_alert" (
    "id" SERIAL NOT NULL,
    "trainId" TEXT NOT NULL,
    "type" INTEGER NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "callState" INTEGER NOT NULL,
    "description" TEXT NOT NULL,
    "car" INTEGER NOT NULL,
    "carName" TEXT NOT NULL,
    "intercom" TEXT NOT NULL,
    "cameras" TEXT[],

    CONSTRAINT "real_alert_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "anomaly_alert" (
    "id" SERIAL NOT NULL,
    "probability" DOUBLE PRECISION NOT NULL,
    "type" INTEGER NOT NULL,
    "trainId" TEXT NOT NULL,
    "description" TEXT NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "anomaly_alert_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "predicted_alert" (
    "id" SERIAL NOT NULL,
    "probability" DOUBLE PRECISION NOT NULL,
    "type" INTEGER NOT NULL,
    "trainId" TEXT NOT NULL,
    "description" TEXT NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "predicted_alert_pkey" PRIMARY KEY ("id")
);
