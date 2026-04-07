-- ============================================================
-- HỆ THỐNG QUẢN LÝ THƯ VIỆN - DATABASE SCHEMA
-- ============================================================

CREATE DATABASE IF NOT EXISTS library_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE library_db;

-- ============================================================
-- BẢNG 1: users - Tài khoản hệ thống (Admin, Thủ thư)
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(100) UNIQUE,
    role          ENUM('ADMIN', 'LIBRARIAN') NOT NULL DEFAULT 'LIBRARIAN',
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ============================================================
-- BẢNG 2: categories - Danh mục / Thể loại sách
-- ============================================================
CREATE TABLE IF NOT EXISTS categories (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- BẢNG 3: books - Sách
-- ============================================================
CREATE TABLE IF NOT EXISTS books (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    isbn             VARCHAR(20) UNIQUE,
    title            VARCHAR(255) NOT NULL,
    author           VARCHAR(255) NOT NULL,
    publisher        VARCHAR(255),
    publish_year     INT,
    category_id      INT,
    total_copies     INT NOT NULL DEFAULT 1,
    available_copies INT NOT NULL DEFAULT 1,
    description      TEXT,
    cover_image_path VARCHAR(500),
    deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_book_category FOREIGN KEY (category_id)
        REFERENCES categories(id) ON DELETE SET NULL,
    CONSTRAINT chk_copies CHECK (available_copies >= 0 AND available_copies <= total_copies)
);

CREATE INDEX idx_books_title    ON books(title);
CREATE INDEX idx_books_author   ON books(author);
CREATE INDEX idx_books_category ON books(category_id);

-- ============================================================
-- BẢNG 4: members - Đọc giả
-- ============================================================
CREATE TABLE IF NOT EXISTS members (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    member_code VARCHAR(20)  NOT NULL UNIQUE,
    full_name   VARCHAR(100) NOT NULL,
    email       VARCHAR(100) UNIQUE,
    phone       VARCHAR(20),
    address     TEXT,
    join_date   DATE NOT NULL DEFAULT (CURRENT_DATE),
    expiry_date DATE NOT NULL,
    status      ENUM('ACTIVE', 'EXPIRED', 'SUSPENDED') NOT NULL DEFAULT 'ACTIVE',
    deleted     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_members_code  ON members(member_code);
CREATE INDEX idx_members_name  ON members(full_name);

-- ============================================================
-- BẢNG 5: borrow_records - Phiếu mượn/trả sách
-- ============================================================
CREATE TABLE IF NOT EXISTS borrow_records (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    member_id   INT  NOT NULL,
    book_id     INT  NOT NULL,
    borrow_date DATE NOT NULL DEFAULT (CURRENT_DATE),
    due_date    DATE NOT NULL,
    return_date DATE,
    status      ENUM('BORROWING', 'RETURNED', 'OVERDUE', 'LOST') NOT NULL DEFAULT 'BORROWING',
    notes       TEXT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_borrow_member FOREIGN KEY (member_id)
        REFERENCES members(id) ON DELETE RESTRICT,
    CONSTRAINT fk_borrow_book FOREIGN KEY (book_id)
        REFERENCES books(id) ON DELETE RESTRICT
);

CREATE INDEX idx_borrow_member ON borrow_records(member_id);
CREATE INDEX idx_borrow_book   ON borrow_records(book_id);
CREATE INDEX idx_borrow_status ON borrow_records(status);

-- ============================================================
-- BẢNG 6: fines - Phí phạt
-- ============================================================
CREATE TABLE IF NOT EXISTS fines (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    borrow_record_id INT            NOT NULL,
    amount           DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    reason           VARCHAR(255),
    paid             BOOLEAN NOT NULL DEFAULT FALSE,
    paid_date        DATE,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_fine_borrow FOREIGN KEY (borrow_record_id)
        REFERENCES borrow_records(id) ON DELETE CASCADE
);

-- ============================================================
-- SEED DATA
-- ============================================================

-- Tài khoản admin (password: Admin@123 - đã hash bằng BCrypt)
-- BCrypt hash của mật khẩu "Admin@123" (rounds=12, verified)
INSERT INTO users (username, password_hash, full_name, email, role) VALUES
('admin',    '$2a$12$0H18yh9I0vmipvo1DEoC9OFcQYbg9OEQ2/Dy3QD6gOkIXcyZL1FzO', 'Quản Trị Viên',  'admin@library.com',    'ADMIN'),
('librarian','$2a$12$0H18yh9I0vmipvo1DEoC9OFcQYbg9OEQ2/Dy3QD6gOkIXcyZL1FzO', 'Nguyễn Thủ Thư', 'librarian@library.com','LIBRARIAN');
-- Mật khẩu mặc định cả hai: Admin@123

-- Danh mục sách
INSERT INTO categories (name, description) VALUES
('Khoa học & Công nghệ',  'Sách về khoa học tự nhiên, công nghệ thông tin, kỹ thuật'),
('Văn học & Nghệ thuật',  'Tiểu thuyết, truyện ngắn, thơ ca, kịch bản'),
('Kinh tế & Kinh doanh',  'Quản trị, tài chính, khởi nghiệp, marketing'),
('Lịch sử & Địa lý',      'Lịch sử Việt Nam, thế giới, địa lý nhân văn'),
('Tâm lý & Kỹ năng sống', 'Phát triển bản thân, tâm lý học, kỹ năng mềm'),
('Giáo dục & Thiếu nhi',  'Sách giáo khoa, truyện tranh, giáo dục sớm'),
('Ngoại ngữ',             'Sách học tiếng Anh, Nhật, Hàn, Trung và các ngôn ngữ khác'),
('Khoa học xã hội',       'Xã hội học, chính trị, triết học, pháp luật');

-- Sách mẫu
INSERT INTO books (isbn, title, author, publisher, publish_year, category_id, total_copies, available_copies, description) VALUES
('978-604-2-27100-1', 'Lập Trình Java Cơ Bản',           'Nguyễn Văn Học',    'NXB Thông Tin',    2022, 1, 5, 5, 'Giáo trình Java dành cho người mới bắt đầu'),
('978-604-1-12345-2', 'Clean Code',                       'Robert C. Martin',  'NXB Lao Động',     2020, 1, 3, 3, 'Nghệ thuật viết code sạch'),
('978-604-3-45678-3', 'Đắc Nhân Tâm',                    'Dale Carnegie',     'NXB Tổng Hợp',     2019, 5, 8, 8, 'Kỹ năng giao tiếp và tạo ảnh hưởng'),
('978-604-2-98765-4', 'Nhà Giả Kim',                      'Paulo Coelho',      'NXB Hội Nhà Văn',  2021, 2, 6, 6, 'Tiểu thuyết triết học về hành trình tìm kiếm'),
('978-604-1-11111-5', 'Khởi Nghiệp Tinh Gọn',            'Eric Ries',         'NXB Lao Động',     2020, 3, 4, 4, 'Phương pháp Lean Startup'),
('978-604-2-22222-6', 'Tư Duy Nhanh và Chậm',            'Daniel Kahneman',   'NXB Thế Giới',     2022, 5, 5, 5, 'Hai hệ thống tư duy của con người'),
('978-604-3-33333-7', 'Lịch Sử Việt Nam',                 'Nhiều Tác Giả',     'NXB Giáo Dục',     2018, 4, 10, 10,'Lịch sử Việt Nam từ thời dựng nước'),
('978-604-1-44444-8', 'English Grammar in Use',           'Raymond Murphy',    'Cambridge Press',  2019, 7, 7, 7, 'Ngữ pháp tiếng Anh thực hành'),
('978-604-2-55555-9', 'Python cho Khoa Học Dữ Liệu',      'Wes McKinney',      'NXB Khoa Học',     2023, 1, 4, 4, 'Pandas, NumPy, và phân tích dữ liệu'),
('978-604-3-66666-0', 'Cha Giàu Cha Nghèo',              'Robert Kiyosaki',   'NXB Trẻ',          2020, 3, 6, 6, 'Tư duy tài chính cá nhân');

-- Đọc giả mẫu
INSERT INTO members (member_code, full_name, email, phone, address, join_date, expiry_date, status) VALUES
('TV001', 'Trần Thị Mai',      'mai.tran@email.com',    '0901234567', '123 Nguyễn Huệ, Q1, TP.HCM',    '2024-01-10', '2026-01-10', 'ACTIVE'),
('TV002', 'Lê Văn Hùng',       'hung.le@email.com',     '0912345678', '456 Lê Lợi, Q3, TP.HCM',        '2024-02-15', '2026-02-15', 'ACTIVE'),
('TV003', 'Phạm Thị Lan',      'lan.pham@email.com',    '0923456789', '789 Trần Phú, Hà Nội',           '2024-03-20', '2025-03-20', 'EXPIRED'),
('TV004', 'Nguyễn Văn Đức',    'duc.nguyen@email.com',  '0934567890', '321 Hoàng Văn Thụ, Đà Nẵng',    '2024-04-05', '2026-04-05', 'ACTIVE'),
('TV005', 'Hoàng Thị Thu',     'thu.hoang@email.com',   '0945678901', '654 Pasteur, Q1, TP.HCM',        '2024-05-12', '2026-05-12', 'ACTIVE'),
('TV006', 'Vũ Minh Tuấn',      'tuan.vu@email.com',     '0956789012', '987 Đinh Tiên Hoàng, Hà Nội',   '2024-06-18', '2026-06-18', 'ACTIVE'),
('TV007', 'Đặng Thị Hoa',      'hoa.dang@email.com',    '0967890123', '147 Hai Bà Trưng, TP.HCM',       '2024-07-22', '2026-07-22', 'ACTIVE'),
('TV008', 'Bùi Quốc Huy',      'huy.bui@email.com',     '0978901234', '258 Lý Thường Kiệt, Huế',        '2024-08-30', '2026-08-30', 'ACTIVE');

-- Phiếu mượn sách mẫu
INSERT INTO borrow_records (member_id, book_id, borrow_date, due_date, return_date, status) VALUES
(1, 1, '2025-01-05', '2025-01-19', '2025-01-18', 'RETURNED'),
(2, 3, '2025-01-10', '2025-01-24', '2025-01-25', 'RETURNED'),
(1, 4, '2025-02-01', '2025-02-15', NULL,          'BORROWING'),
(3, 5, '2025-01-20', '2025-02-03', NULL,          'OVERDUE'),
(4, 2, '2025-02-10', '2025-02-24', '2025-02-23', 'RETURNED'),
(5, 6, '2025-02-15', '2025-03-01', NULL,          'BORROWING'),
(6, 8, '2025-03-01', '2025-03-15', NULL,          'BORROWING'),
(7, 9, '2025-01-15', '2025-01-29', NULL,          'OVERDUE');

-- Phí phạt mẫu
INSERT INTO fines (borrow_record_id, amount, reason, paid) VALUES
(2, 5000,  'Trả sách trễ 1 ngày', TRUE),
(4, 70000, 'Trả sách trễ 14 ngày (5.000đ/ngày)', FALSE),
(8, 35000, 'Trả sách trễ 7 ngày', FALSE);
