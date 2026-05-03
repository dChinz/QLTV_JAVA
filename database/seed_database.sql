-- ============================================================
-- SEED DATA — Oracle version
-- ============================================================

-- USERS
INSERT INTO users (username, password_hash, full_name, email, role, active)
VALUES ('admin',
        '$2a$12$l5DI8WdKWEnW4C2m2Sv9auf5bzdppSW8J9C0joRTZ198Benq0e8LW',
        'Quản trị hệ thống', 'admin@library.local', 'ADMIN', 1);

INSERT INTO users (username, password_hash, full_name, email, role, active)
VALUES ('thuthu01',
        '$2a$12$l5DI8WdKWEnW4C2m2Sv9auf5bzdppSW8J9C0joRTZ198Benq0e8LW',
        'Nguyễn Thị Thu', 'thuthu01@library.local', 'LIBRARIAN', 1);

-- CATEGORIES
INSERT INTO categories (name, description)
VALUES ('Văn học', 'Tiểu thuyết, truyện ngắn');

INSERT INTO categories (name, description)
VALUES ('Khoa học', 'STEM, phổ thông');

INSERT INTO categories (name, description)
VALUES ('Lịch sử', 'Việt Nam và thế giới');

-- BOOKS
INSERT INTO books (isbn, title, author, publisher, publish_year, category_id, total_copies, description)
VALUES ('9786043375001', 'Đắc nhân tâm', 'Dale Carnegie', 'NXB Trẻ', 2016, 1, 5, NULL);

INSERT INTO books (isbn, title, author, publisher, publish_year, category_id, total_copies, description)
VALUES ('9786049927305', 'Sapiens', 'Yuval Noah Harari', 'NXB Trẻ', 2019, 2, 3, NULL);

INSERT INTO books (isbn, title, author, publisher, publish_year, category_id, total_copies, description)
VALUES ('8935235256254', 'Lịch sử Việt Nam (bản khảo cứu)', 'Nhiều tác giả',
        'NXB Khoa học xã hội', 2018, 3, 2, NULL);

INSERT INTO books (isbn, title, author, publisher, publish_year, category_id, total_copies, description)
VALUES (NULL, 'Sổ tay nội bộ thư viện', 'Ban biên tập',
        NULL, NULL, NULL, 1, 'Không phân loại — category_id NULL');

-- MEMBERS
INSERT INTO members (member_code, full_name, email, phone, address, join_date, expiry_date, status, lost_book_count)
VALUES ('TV2025001', 'Trần Văn An', 'an.tran@email.test', '0901000001', 'Hà Nội',
        DATE '2025-01-10', DATE '2027-01-10', 'ACTIVE', 0);

INSERT INTO members (member_code, full_name, email, phone, address, join_date, expiry_date, status, lost_book_count)
VALUES ('TV2025002', 'Lê Thị Bình', 'binh.le@email.test', '0901000002', 'TP.HCM',
        DATE '2024-06-01', DATE '2025-04-01', 'EXPIRED', 0);

INSERT INTO members (member_code, full_name, email, phone, address, join_date, expiry_date, status, lost_book_count)
VALUES ('TV2025003', 'Phạm Minh Cường', 'cuong.pham@email.test', '0901000003', 'Đà Nẵng',
        DATE '2023-03-15', DATE '2026-03-15', 'SUSPENDED', 5);

-- BORROW_RECORDS
INSERT INTO borrow_records (member_id, book_id, borrow_date, due_date, return_date, status, notes)
VALUES (1, 1, DATE '2026-04-15', DATE '2026-04-30', NULL, 'BORROWING', NULL);

INSERT INTO borrow_records (member_id, book_id, borrow_date, due_date, return_date, status, notes)
VALUES (1, 2, DATE '2026-03-01', DATE '2026-03-16', DATE '2026-03-14', 'RETURNED', 'Trả đúng hạn');

INSERT INTO borrow_records (member_id, book_id, borrow_date, due_date, return_date, status, notes)
VALUES (1, 3, DATE '2025-11-01', DATE '2025-11-16', NULL, 'OVERDUE', 'Quá hạn lâu — ví dụ test');

-- FINES
INSERT INTO fines (borrow_record_id, member_id, amount, reason, paid, paid_date, created_at)
VALUES (3, NULL, 50000.00, 'Phạt quá hạn (ví dụ)',
        0, NULL, TIMESTAMP '2025-12-01 10:00:00');

INSERT INTO fines (borrow_record_id, member_id, amount, reason, paid, paid_date, created_at)
VALUES (NULL, 2, 100000.00, 'Gia hạn thẻ đọc (ví dụ — phí theo nghiệp vụ)',
        1, DATE '2025-05-01', TIMESTAMP '2025-04-28 09:00:00');

COMMIT;