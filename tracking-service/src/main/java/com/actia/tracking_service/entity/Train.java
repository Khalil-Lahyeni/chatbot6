package com.actia.tracking_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "train")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Train {
    @Id
    @Column(name = "train_id", nullable = false, unique = true)
    private Long trainId;

    @Column(name = "train_name")
    private String name;

    @UpdateTimestamp
    @Column(name = "update_at")
    private Instant updateAt;

    @Column
    private String mission;

    @Column
    private String baseline;

    @Column
    private String diversity;

    @Column
    private String database;

}
