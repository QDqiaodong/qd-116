package com.tooling.asset;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScrapRecordRepository extends JpaRepository<ScrapRecord, Long> {

    List<ScrapRecord> findByToolingCodeOrderByScrapDateDesc(String toolingCode);

    long deleteByToolingCode(String toolingCode);
}
