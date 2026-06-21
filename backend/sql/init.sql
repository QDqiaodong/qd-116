SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

CREATE TABLE IF NOT EXISTS tooling_asset (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tooling_code VARCHAR(50) UNIQUE NOT NULL,
    product_name VARCHAR(100),
    workstation VARCHAR(50),
    entry_date DATE,
    status VARCHAR(20) DEFAULT 'IN_USE',
    image_url VARCHAR(500),
    remark VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS transfer_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tooling_code VARCHAR(50) NOT NULL,
    from_workstation VARCHAR(50),
    to_workstation VARCHAR(50),
    transfer_time DATETIME,
    operator VARCHAR(50),
    remark VARCHAR(500)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS inventory_check (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    check_month VARCHAR(7) NOT NULL,
    total_book INT,
    total_actual INT,
    missing_count INT,
    misplaced_count INT,
    scrapped_excluded_count INT,
    difference INT,
    checker VARCHAR(50),
    check_time DATETIME,
    remark VARCHAR(500),
    INDEX idx_check_month (check_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS scrap_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tooling_code VARCHAR(50) NOT NULL,
    scrap_reason VARCHAR(500),
    scrap_date DATE,
    operator VARCHAR(50),
    remark VARCHAR(500)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tooling_inventory_diff (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    check_month VARCHAR(7) NOT NULL,
    tooling_code VARCHAR(50) NOT NULL,
    book_exists TINYINT(1),
    actual_exists TINYINT(1),
    diff_type VARCHAR(20),
    checker VARCHAR(50),
    check_time DATETIME,
    workstation VARCHAR(50),
    remark VARCHAR(500),
    handle_status VARCHAR(20),
    handle_type VARCHAR(40),
    handle_time DATETIME,
    handler VARCHAR(50),
    handle_remark VARCHAR(500),
    expected_workstation VARCHAR(50),
    actual_found_workstation VARCHAR(50),
    corrected_workstation VARCHAR(50),
    INDEX idx_tooling_code (tooling_code),
    INDEX idx_check_month (check_month),
    INDEX idx_handle_status (handle_status),
    INDEX idx_diff_type (diff_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO tooling_asset (tooling_code, product_name, workstation, entry_date, status, remark) VALUES
('TL-2024-001', '定位块A型', '注塑机01', '2024-01-15', 'IN_USE', '标准定位块'),
('TL-2024-002', '定位块B型', '注塑机02', '2024-03-20', 'IN_USE', '大型定位块'),
('TL-2024-003', '定位块C型', '注塑机03', '2024-06-10', 'SCRAPPED', '已报废');

-- Redis规格模板数据请通过API初始化：POST /api/spec-template/{category}
