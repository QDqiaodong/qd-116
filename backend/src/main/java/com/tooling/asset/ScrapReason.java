package com.tooling.asset;

import jakarta.persistence.Entity;
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
@Table(name = "scrap_reason", uniqueConstraints = {
        @UniqueConstraint(columnNames = "reasonCode")
})
public class ScrapReason {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String reasonCode;

    private String reasonName;

    private String category;

    private Boolean enabled;

    private Integer sortOrder;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
