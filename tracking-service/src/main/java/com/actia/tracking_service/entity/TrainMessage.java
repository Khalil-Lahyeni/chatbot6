package com.actia.tracking_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "train_message")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class TrainMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long messageId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "message_type")
    private String messageType;

    @Column(name = "message_name")
    private String name;

    @Column(name = "is_critical", nullable = false)
    private boolean isCritical;

    @ManyToOne
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;
}
