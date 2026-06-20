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
@Table(name = "tooling_inventory_diff")
public class ToolingInventoryDiff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String checkMonth;

    private String toolingCode;

    private Boolean bookExists;

    private Boolean actualExists;

    private String diffType;

    private String checker;

    private LocalDateTime checkTime;

    private String workstation;

    private String remark;

    private String handleStatus;

    private String handleType;

    private LocalDateTime handleTime;

    private String handler;

    private String handleRemark;

    private String expectedWorkstation;

    private String actualFoundWorkstation;

    private String correctedWorkstation;
}
