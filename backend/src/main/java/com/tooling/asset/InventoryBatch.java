package com.tooling.asset;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(name = "inventory_batch", uniqueConstraints = {
        @UniqueConstraint(columnNames = "batchMonth")
})
public class InventoryBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String batchMonth;

    private String batchName;

    @Enumerated(EnumType.STRING)
    private BatchStatus status;

    private Integer totalBookCount;

    private Integer scrappedExcludedCount;

    private String creator;

    private LocalDateTime createTime;

    private String freezer;

    private LocalDateTime freezeTime;

    private String closer;

    private LocalDateTime closeTime;

    private String remark;

    public enum BatchStatus {
        DRAFT("草稿"),
        FROZEN("已冻结"),
        CLOSED("已关闭");

        private final String description;

        BatchStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
