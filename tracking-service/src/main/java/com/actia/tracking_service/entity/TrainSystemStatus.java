package com.actia.tracking_service.entity;

import com.actia.tracking_service.enums.SystemHealthStatus;
import com.actia.tracking_service.enums.UpdateStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "train_system_status")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainSystemStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UpdateTimestamp
    @Column(name = "update_at", nullable = false)
    private Instant updateAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "pacis_status")
    private SystemHealthStatus pacisStatus;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "cctv_status")
    private SystemHealthStatus cctvStatus;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "rear_view_status")
    private SystemHealthStatus rearViewStatus;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "update_status")
    private UpdateStatus updateStatus;

    @ManyToOne
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;
}
