package com.actia.tracking_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "media_database")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaDatabase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UpdateTimestamp
    @Column(name = "update_at", nullable = false)
    private Instant updateAt;

    @Column(name = "device_ip")
    private String deviceIp;

    @Column(nullable = false)
    private String name;

    @Column(name = "version_number")
    private String versionNumber;

    @Column(name = "is_active")
    private boolean active;

    @Column(name = "activation_date")
    private Instant activationDate;

    @ManyToOne
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;
}

