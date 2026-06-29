package com.actia.tracking_service.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "train_location_status")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UpdateTimestamp
    @Column(name = "update_at", nullable = false)
    private Instant updateAt;

    private String currentStation;

    private String nextStation;

    private String destination;

    @ManyToOne
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;


}


