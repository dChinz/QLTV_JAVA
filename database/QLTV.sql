-- ============================================================
-- SEQUENCES (thay thế AUTO_INCREMENT)
-- ============================================================
CREATE SEQUENCE seq_users     START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_categories START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_books     START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_members   START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_borrow    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_fines     START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- ============================================================
-- USERS
-- ============================================================
CREATE TABLE users (
  id            NUMBER          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  username      VARCHAR2(50)    NOT NULL,
  password_hash VARCHAR2(255)   NOT NULL,
  full_name     VARCHAR2(100)   NOT NULL,
  email         VARCHAR2(100),
  role          VARCHAR2(20)    DEFAULT 'LIBRARIAN' NOT NULL,
  active        NUMBER(1,0)     DEFAULT 1 NOT NULL,

  created_at    TIMESTAMP       DEFAULT SYSTIMESTAMP,
  updated_at    TIMESTAMP       DEFAULT SYSTIMESTAMP,

  CONSTRAINT uq_users_username UNIQUE (username),
  CONSTRAINT uq_users_email    UNIQUE (email),
  CONSTRAINT chk_users_role    CHECK (role IN ('ADMIN', 'LIBRARIAN')),
  CONSTRAINT chk_users_active  CHECK (active IN (0, 1))
);

-- Trigger tự động cập nhật updated_at
CREATE OR REPLACE TRIGGER trg_users_upd
  BEFORE UPDATE ON users
  FOR EACH ROW
BEGIN
  :NEW.updated_at := SYSTIMESTAMP;
END;
/

-- ============================================================
-- CATEGORIES
-- ============================================================
CREATE TABLE categories (
  id          NUMBER        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name        VARCHAR2(100) NOT NULL,
  description CLOB,

  deleted_at  TIMESTAMP     NULL,

  created_at  TIMESTAMP     DEFAULT SYSTIMESTAMP,
  updated_at  TIMESTAMP     DEFAULT SYSTIMESTAMP
);

-- Oracle không hỗ trợ UNIQUE trên (name, deleted_at) có NULL linh hoạt như MySQL,
-- dùng function-based unique index để xử lý soft-delete:
CREATE UNIQUE INDEX uq_categories_name
  ON categories (
    name,
    NVL(TO_CHAR(deleted_at, 'YYYYMMDDHH24MISS'), 'ACTIVE')
  );

CREATE INDEX idx_categories_not_deleted ON categories (deleted_at);

CREATE OR REPLACE TRIGGER trg_categories_upd
  BEFORE UPDATE ON categories
  FOR EACH ROW
BEGIN
  :NEW.updated_at := SYSTIMESTAMP;
END;
/

-- ============================================================
-- BOOKS
-- ============================================================
CREATE TABLE books (
  id               NUMBER        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  isbn             VARCHAR2(20),
  title            VARCHAR2(255) NOT NULL,
  author           VARCHAR2(255) NOT NULL,
  publisher        VARCHAR2(255),
  publish_year     NUMBER(4),
  category_id      NUMBER,

  total_copies     NUMBER        DEFAULT 1 NOT NULL,

  description      CLOB,
  cover_image_path VARCHAR2(500),

  deleted_at       TIMESTAMP     NULL,

  created_at       TIMESTAMP     DEFAULT SYSTIMESTAMP,
  updated_at       TIMESTAMP     DEFAULT SYSTIMESTAMP,

  CONSTRAINT fk_books_category
    FOREIGN KEY (category_id)
    REFERENCES categories(id)
    ON DELETE SET NULL,

  CONSTRAINT chk_books_total CHECK (total_copies >= 0)
);

CREATE UNIQUE INDEX uq_books_isbn
  ON books (
    isbn,
    NVL(TO_CHAR(deleted_at, 'YYYYMMDDHH24MISS'), 'ACTIVE')
  );

CREATE INDEX idx_books_title       ON books (title);
CREATE INDEX idx_books_author      ON books (author);
CREATE INDEX idx_books_category    ON books (category_id);
CREATE INDEX idx_books_not_deleted ON books (deleted_at);

CREATE OR REPLACE TRIGGER trg_books_upd
  BEFORE UPDATE ON books
  FOR EACH ROW
BEGIN
  :NEW.updated_at := SYSTIMESTAMP;
END;
/

-- ============================================================
-- MEMBERS
-- ============================================================
CREATE TABLE members (
  id          NUMBER        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  member_code VARCHAR2(20)  NOT NULL,
  full_name   VARCHAR2(100) NOT NULL,
  email       VARCHAR2(100),
  phone       VARCHAR2(20),
  address     CLOB,

  join_date   DATE          DEFAULT TRUNC(SYSDATE) NOT NULL,
  expiry_date DATE          NOT NULL,

  status      VARCHAR2(20)  DEFAULT 'ACTIVE' NOT NULL,

  lost_book_count NUMBER    DEFAULT 0 NOT NULL,

  deleted_at  TIMESTAMP     NULL,

  created_at  TIMESTAMP     DEFAULT SYSTIMESTAMP,
  updated_at  TIMESTAMP     DEFAULT SYSTIMESTAMP,

  CONSTRAINT chk_members_status
    CHECK (status IN ('ACTIVE', 'EXPIRED', 'SUSPENDED'))
);

CREATE UNIQUE INDEX uq_members_code
  ON members (
    member_code,
    NVL(TO_CHAR(deleted_at, 'YYYYMMDDHH24MISS'), 'ACTIVE')
  );

CREATE UNIQUE INDEX uq_members_email
  ON members (
    email,
    NVL(TO_CHAR(deleted_at, 'YYYYMMDDHH24MISS'), 'ACTIVE')
  );

CREATE INDEX idx_members_name        ON members (full_name);
CREATE INDEX idx_members_not_deleted ON members (deleted_at);

CREATE OR REPLACE TRIGGER trg_members_upd
  BEFORE UPDATE ON members
  FOR EACH ROW
BEGIN
  :NEW.updated_at := SYSTIMESTAMP;
END;
/

-- ============================================================
-- BORROW RECORDS
-- ============================================================
CREATE TABLE borrow_records (
  id          NUMBER       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  member_id   NUMBER       NOT NULL,
  book_id     NUMBER       NOT NULL,

  borrow_date DATE         DEFAULT TRUNC(SYSDATE) NOT NULL,
  due_date    DATE         NOT NULL,
  return_date DATE,

  status      VARCHAR2(20) DEFAULT 'BORROWING' NOT NULL,

  notes       CLOB,

  created_at  TIMESTAMP    DEFAULT SYSTIMESTAMP,
  updated_at  TIMESTAMP    DEFAULT SYSTIMESTAMP,

  CONSTRAINT fk_borrow_member
    FOREIGN KEY (member_id) REFERENCES members(id),

  CONSTRAINT fk_borrow_book
    FOREIGN KEY (book_id) REFERENCES books(id),

  CONSTRAINT chk_borrow_status
    CHECK (status IN ('BORROWING', 'RETURNED', 'OVERDUE', 'LOST'))
);

CREATE INDEX idx_borrow_member ON borrow_records (member_id);
CREATE INDEX idx_borrow_book   ON borrow_records (book_id);
CREATE INDEX idx_borrow_status ON borrow_records (status);

CREATE OR REPLACE TRIGGER trg_borrow_upd
  BEFORE UPDATE ON borrow_records
  FOR EACH ROW
BEGIN
  :NEW.updated_at := SYSTIMESTAMP;
END;
/

-- ============================================================
-- FINES
-- ============================================================
CREATE TABLE fines (
  id               NUMBER         GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  borrow_record_id NUMBER         NULL,
  member_id        NUMBER         NULL,

  amount           NUMBER(10,2)   DEFAULT 0.00 NOT NULL,
  reason           VARCHAR2(255),

  paid             NUMBER(1,0)    DEFAULT 0 NOT NULL,
  paid_date        DATE,

  created_at       TIMESTAMP      DEFAULT SYSTIMESTAMP,
  updated_at       TIMESTAMP      DEFAULT SYSTIMESTAMP,

  CONSTRAINT fk_fine_borrow
    FOREIGN KEY (borrow_record_id)
    REFERENCES borrow_records(id)
    ON DELETE CASCADE,

  CONSTRAINT fk_fine_member
    FOREIGN KEY (member_id)
    REFERENCES members(id)
    ON DELETE CASCADE,

  CONSTRAINT chk_fine_has_ref
    CHECK (borrow_record_id IS NOT NULL OR member_id IS NOT NULL),

  CONSTRAINT chk_fine_paid
    CHECK (paid IN (0, 1))
);

CREATE INDEX idx_fines_paid ON fines (paid);

CREATE OR REPLACE TRIGGER trg_fines_upd
  BEFORE UPDATE ON fines
  FOR EACH ROW
BEGIN
  :NEW.updated_at := SYSTIMESTAMP;
END;
/

COMMIT;