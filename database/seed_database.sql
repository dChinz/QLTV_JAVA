-- CATEGORIES
INSERT INTO categories (name, description) VALUES ('Công nghệ thông tin', 'Sách về lập trình, CSDL, mạng máy tính');
INSERT INTO categories (name, description) VALUES ('Văn học', 'Tiểu thuyết, truyện ngắn, thơ ca');
INSERT INTO categories (name, description) VALUES ('Kinh tế', 'Sách về kinh doanh, tài chính, marketing');
INSERT INTO categories (name, description) VALUES ('Khoa học tự nhiên', 'Toán, lý, hóa, sinh học');
INSERT INTO categories (name, description) VALUES ('Lịch sử', 'Lịch sử Việt Nam và thế giới');

-- USERS
INSERT INTO users (username, password_hash, full_name, email, role) VALUES ('admin',     'hash_admin_123',  'Nguyễn Văn Admin',   'admin@thuvien.vn',     'ADMIN');
INSERT INTO users (username, password_hash, full_name, email, role) VALUES ('thuha',     'hash_thuha_123',  'Trần Thị Thu Hà',    'thuha@thuvien.vn',     'LIBRARIAN');
INSERT INTO users (username, password_hash, full_name, email, role) VALUES ('minhtu',    'hash_minhtu_123', 'Lê Minh Tú',         'minhtu@thuvien.vn',    'LIBRARIAN');
INSERT INTO users (username, password_hash, full_name, email, role) VALUES ('lanphuong', 'hash_lan_123',    'Phạm Lan Phương',    'lanphuong@thuvien.vn', 'LIBRARIAN');
INSERT INTO users (username, password_hash, full_name, email, role) VALUES ('baoan',     'hash_bao_123',    'Hoàng Bảo An',       'baoan@thuvien.vn',     'LIBRARIAN');

-- BOOKS
INSERT INTO books (isbn, title, author, publisher, publish_year, category_id, total_copies, available_copies, description)
VALUES ('978-604-1-001', 'Lập trình Java cơ bản',      'Nguyễn Tiến Dũng', 'NXB Thông tin', 2020, 1, 5, 4, 'Giáo trình Java cho người mới bắt đầu');
INSERT INTO books (isbn, title, author, publisher, publish_year, category_id, total_copies, available_copies, description)
VALUES ('978-604-1-002', 'Cơ sở dữ liệu Oracle',       'Trần Văn Minh',    'NXB Giáo dục',  2021, 1, 3, 2, 'Học Oracle từ cơ bản đến nâng cao');
INSERT INTO books (isbn, title, author, publisher, publish_year, category_id, total_copies, available_copies, description)
VALUES ('978-604-2-001', 'Số đỏ',                       'Vũ Trọng Phụng',   'NXB Văn học',   2019, 2, 4, 3, 'Tiểu thuyết kinh điển Việt Nam');
INSERT INTO books (isbn, title, author, publisher, publish_year, category_id, total_copies, available_copies, description)
VALUES ('978-604-3-001', 'Kinh tế học vi mô',           'Phạm Thị Lan',     'NXB Kinh tế',   2022, 3, 2, 1, 'Giáo trình kinh tế học');
INSERT INTO books (isbn, title, author, publisher, publish_year, category_id, total_copies, available_copies, description)
VALUES ('978-604-5-001', 'Lịch sử Việt Nam',            'Đinh Xuân Lâm',    'NXB Giáo dục',  2018, 5, 6, 5, 'Thông sử Việt Nam từ thời dựng nước');

-- MEMBERS
INSERT INTO members (member_code, full_name, email, phone, join_date, expiry_date)
VALUES ('TV001', 'Nguyễn Thị Mai',  'mai.nt@gmail.com',  '0901234567', DATE '2024-01-10', DATE '2025-01-10');
INSERT INTO members (member_code, full_name, email, phone, join_date, expiry_date)
VALUES ('TV002', 'Trần Văn Hùng',   'hung.tv@gmail.com', '0912345678', DATE '2024-02-15', DATE '2025-02-15');
INSERT INTO members (member_code, full_name, email, phone, join_date, expiry_date)
VALUES ('TV003', 'Lê Thị Hoa',      'hoa.lt@gmail.com',  '0923456789', DATE '2024-03-20', DATE '2025-03-20');
INSERT INTO members (member_code, full_name, email, phone, join_date, expiry_date)
VALUES ('TV004', 'Phạm Quốc Bảo',   'bao.pq@gmail.com',  '0934567890', DATE '2024-04-05', DATE '2025-04-05');
INSERT INTO members (member_code, full_name, email, phone, join_date, expiry_date)
VALUES ('TV005', 'Hoàng Thị Lan',   'lan.ht@gmail.com',  '0945678901', DATE '2024-05-12', DATE '2025-05-12');

-- BORROW_RECORDS
INSERT INTO borrow_records (member_id, book_id, borrow_date, due_date, return_date, status)
VALUES (1, 1, DATE '2024-10-01', DATE '2024-10-15', DATE '2024-10-14', 'RETURNED');

INSERT INTO borrow_records (member_id, book_id, borrow_date, due_date, return_date, status)
VALUES (2, 2, DATE '2024-10-05', DATE '2024-10-19', DATE '2024-10-25', 'RETURNED');

INSERT INTO borrow_records (member_id, book_id, borrow_date, due_date, status)
VALUES (3, 3, DATE '2024-10-10', DATE '2024-10-24', 'OVERDUE');

INSERT INTO borrow_records (member_id, book_id, borrow_date, due_date, return_date, status)
VALUES (4, 4, DATE '2024-11-01', DATE '2024-11-15', DATE '2024-11-20', 'RETURNED');

INSERT INTO borrow_records (member_id, book_id, borrow_date, due_date, status)
VALUES (5, 5, DATE '2024-12-01', DATE '2024-12-15', 'BORROWING');

-- FINES
-- bản ghi 1: trả đúng hạn, không phạt -> bỏ qua
-- bản ghi 2: trả trễ 6 ngày -> phạt, đã thanh toán
INSERT INTO fines (borrow_record_id, amount, reason, paid, paid_date)
VALUES (2, 6000, 'Trả sách trễ 6 ngày', 1, DATE '2024-10-26');

-- bản ghi 3: đang quá hạn -> phạt, chưa thanh toán
INSERT INTO fines (borrow_record_id, amount, reason, paid)
VALUES (3, 20000, 'Trả sách trễ, đang quá hạn', 0);

-- bản ghi 4: trả trễ 5 ngày -> phạt, đã thanh toán
INSERT INTO fines (borrow_record_id, amount, reason, paid, paid_date)
VALUES (4, 5000, 'Trả sách trễ 5 ngày', 1, DATE '2024-11-21');

-- bản ghi 5: đang mượn, chưa đến hạn -> không phạt -> bỏ qua
-- Thêm 2 bản ghi phạt bổ sung cho đủ 5
INSERT INTO fines (borrow_record_id, amount, reason, paid)
VALUES (3, 5000, 'Phí làm hư bìa sách', 0);

INSERT INTO fines (borrow_record_id, amount, reason, paid, paid_date)
VALUES (2, 2000, 'Phí vệ sinh sách', 1, DATE '2024-10-27');

COMMIT;