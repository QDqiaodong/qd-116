package com.tooling.asset;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface HighRiskTransferApprovalRepository extends JpaRepository<HighRiskTransferApproval, Long>, JpaSpecificationExecutor<HighRiskTransferApproval> {

    List<HighRiskTransferApproval> findByToolingCodeOrderByApplyTimeDesc(String toolingCode);

    List<HighRiskTransferApproval> findByStatusOrderByApplyTimeDesc(ApprovalStatus status);

    Optional<HighRiskTransferApproval> findFirstByToolingCodeAndStatusInOrderByApplyTimeDesc(String toolingCode, List<ApprovalStatus> statuses);

    void deleteByToolingCode(String toolingCode);
}
