-- ============================================================
-- HƯỚNG DẪN: Cập nhật mật khẩu đúng sau khi import schema
-- ============================================================
-- Mật khẩu mặc định trong library_schema.sql là PLACEHOLDER.
-- Sau khi khởi động ứng dụng lần đầu, chạy lệnh SQL này để
-- set mật khẩu thực với BCrypt hash của "Admin@123":
--
-- Hash của "Admin@123" (BCrypt rounds=12, verified):
-- $2a$12$0H18yh9I0vmipvo1DEoC9OFcQYbg9OEQ2/Dy3QD6gOkIXcyZL1FzO

UPDATE users SET password_hash = '$2a$12$0H18yh9I0vmipvo1DEoC9OFcQYbg9OEQ2/Dy3QD6gOkIXcyZL1FzO'
WHERE username IN ('admin', 'librarian');
