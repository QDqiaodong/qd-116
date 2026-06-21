package com.tooling.asset;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "inventory_check")
public class InventoryCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String checkMonth;

    private Integer totalBook;

    private Integer totalActual;

    private Integer missingCount;

    private Integer misplacedCount;

    private Integer scrappedExcludedCount;

    private Integer difference;

    private String checker;

    private LocalDateTime checkTime;

    private String remark;
}
