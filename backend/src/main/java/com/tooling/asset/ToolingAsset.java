package com.tooling.asset;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "tooling_asset")
public class ToolingAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String toolingCode;

    private String productName;

    private String workstation;

    private LocalDate entryDate;

    @Enumerated(EnumType.STRING)
    private ToolingStatus status;

    private String imageUrl;

    private String remark;

    private String lastStatusChangeRemark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
