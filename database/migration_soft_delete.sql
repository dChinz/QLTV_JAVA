-- Chạy một lần nếu DB cũ chưa có cột deleted (soft delete).
-- Nếu báo "Duplicate column" thì bỏ qua — cột đã tồn tại.
USE library_db;

ALTER TABLE members ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE books   ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;
