package com.actia.tracking_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "train_configuration")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class TrainConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UpdateTimestamp
    @Column(name = "update_at", nullable = false)
    private Instant updateAt;

    @Column(nullable = false)
    private boolean visible;

    @Column(name = "ccu1_ip")
    private String ccu1Ip;

    @Column(name = "ccu2_ip")
    private String ccu2Ip;

    @ManyToOne
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;

}

