package actia.monitoring.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;

@Entity
@Table(name = "trains")
@Data                 // Génère automatiquement Getters, Setters, toString, equals, hashCode (via Lombok)
@NoArgsConstructor    // Génère le constructeur sans argument exigé par JPA
@AllArgsConstructor   // Génère le constructeur avec tous les arguments
public class Train {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "train_id")
    private Long id;

    @Column(name = "name")
    private String name;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updateAt;

    @Column(name = "mission")
    private String mission;

    @Column(name = "baseline")
    private String baseline;

    @Column(name = "diversity")
    private String diversity;

    // "database" est renommé en "db_name" en base de données pour éviter les conflits SQL
    @Column(name = "db_name")
    private String database;
}