package com.tooling.asset;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransferRecordRepository extends JpaRepository<TransferRecord, Long> {

    List<TransferRecord> findByToolingCodeOrderByTransferTimeDesc(String toolingCode);
}
