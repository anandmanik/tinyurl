package com.amtinyurl.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_urls")
@IdClass(UserUrlId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUrl {

    @Id
    @Column(name = "user_id_lower", length = 6, nullable = false, columnDefinition = "CHAR(6)")
    @Size(min = 6, max = 6)
    @NotBlank
    private String userIdLower;

    @Id
    @Column(name = "code", length = 7, nullable = false, columnDefinition = "CHAR(7)")
    @Size(min = 7, max = 7)
    @NotBlank
    private String code;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "code", insertable = false, updatable = false)
    private Url url;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}