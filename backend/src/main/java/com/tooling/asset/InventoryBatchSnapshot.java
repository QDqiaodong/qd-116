package com.tooling.asset;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "inventory_batch_snapshot", indexes = {
        @Index(name = "idx_batch_id", columnList = "batchId"),
        @Index(name = "idx_tooling_code", columnList = "toolingCode"),
        @Index(name = "idx_batch_tooling", columnList = "batchId,toolingCode", unique = true)
})
public class InventoryBatchSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long batchId;

    private String batchMonth;

    private String toolingCode;

    private String productName;

    private String bookWorkstation;

    @Enumerated(EnumType.STRING)
    private ToolingStatus bookStatus;

    private LocalDate bookEntryDate;

    private String bookImageUrl;

    private String bookRemark;

    private LocalDateTime snapshotTime;

    private String region;
}
