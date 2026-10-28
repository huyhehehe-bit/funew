-- Du lieu mau danh RIENG cho test tich hop.
-- Test dung H2 in-memory voi ddl-auto=create-drop: Hibernate tao bang tu entity,
-- roi file nay do du lieu vao. Khong can IF NOT EXISTS vi moi lan chay la database moi.
--
-- Noi dung phai giong het phan du lieu mau cua database/module1_schema.sql
-- (PHAN 3) de test phan anh dung thuc te: 4 vai tro, 6 tai khoan (5 nguoi + SYSTEM),
-- 3 danh muc, 4 the, 3 bai viet, 5 dong nhat ky, 1 danh gia, 1 thong bao.
--
-- KHAC BIET SO VOI SQL SERVER - biet de khoi ngac nhien khi viet test:
--   1. Hibernate tao bang tu entity nen H2 KHONG co rang buoc CHECK nao
--      (ck_articles_status, ck_audit_note_required, ck_notifications_type...).
--      Test o H2 de hon that: thu sai gia tri o day van qua.
--   2. Cot rating_avg o SQL Server la cot tinh toan PERSISTED, con o H2 chi la
--      cot thuong nen luon NULL. Dung assert vao no trong test.
--   3. H2 khong co DEFAULT nao, nen moi cot NOT NULL deu phai ghi gia tri tuong
--      minh: created_at, updated_at, is_deleted, view_count, version...
--
-- Mat khau luu duoi dang hash BCrypt, khong bao gio luu dang thuong.
-- a@ / b@ / c@ / reader@ co mat khau "123", admin@ co mat khau "Admin".
-- b@ va c@ cung mat khau nhung hash khac han nhau vi BCrypt dung salt ngau nhien.
-- Tai khoan system@ KHONG dang nhap duoc: status = FALSE va password_hash
-- khong phai hash BCrypt hop le.

-- ---- Vai tro ----
INSERT INTO roles (code, name, description, is_active, created_at) VALUES
    ('ADMIN',       'Quan tri vien', 'Ghi de moi luong, gan vai tro, cau hinh quy tac bien tap', TRUE, TIMESTAMP '2026-08-01 08:00:00'),
    ('EDITOR',      'Bien tap vien', 'Duyet bai, tra lai kem ly do, hen gio dang',               TRUE, TIMESTAMP '2026-08-01 08:00:00'),
    ('CONTRIBUTOR', 'Cong tac vien', 'Tao va nop bai cua chinh minh, khong tu dang duoc',        TRUE, TIMESTAMP '2026-08-01 08:00:00'),
    ('READER',      'Doc gia',       'Doc bai da xuat ban, tim kiem, cham diem 1-5 sao',         TRUE, TIMESTAMP '2026-08-01 08:00:00');

-- ---- Tai khoan ----
INSERT INTO accounts (username, full_name, email, password_hash, role_id, status, is_deleted, created_at, updated_at)
SELECT 'admin@fu.edu.vn', 'Admin', 'admin@fu.edu.vn', '$2a$10$8HaFznDL4HD0Hk3VUYq79.Kfo0fuOewjAsYD5T8.VZrHd10cdIgoW',
       id, TRUE, FALSE, TIMESTAMP '2026-08-01 08:00:00', TIMESTAMP '2026-08-01 08:00:00'
FROM roles WHERE code = 'ADMIN';

INSERT INTO accounts (username, full_name, email, password_hash, role_id, status, is_deleted, created_at, updated_at)
SELECT 'a@fu.edu.vn', 'Nguyen Van A', 'a@fu.edu.vn', '$2a$10$Wyb/D763Cp.k/ITD8s1yXeK.S4m/81BXi86lcuG/4sVZqd4DVADSS',
       id, TRUE, FALSE, TIMESTAMP '2026-08-01 08:00:00', TIMESTAMP '2026-08-01 08:00:00'
FROM roles WHERE code = 'EDITOR';

INSERT INTO accounts (username, full_name, email, password_hash, role_id, status, is_deleted, created_at, updated_at)
SELECT 'b@fu.edu.vn', 'Tran Thi B', 'b@fu.edu.vn', '$2a$10$sFJqjSh.vltVK/KxP141uu1HakD1h7lgIyQN0SrBcSBn5YAfLrA4a',
       id, TRUE, FALSE, TIMESTAMP '2026-08-01 08:00:00', TIMESTAMP '2026-08-01 08:00:00'
FROM roles WHERE code = 'CONTRIBUTOR';

INSERT INTO accounts (username, full_name, email, password_hash, role_id, status, is_deleted, created_at, updated_at)
SELECT 'c@fu.edu.vn', 'Le Van C', 'c@fu.edu.vn', '$2a$10$m1lXvQazRju5fPCewTzXZO/3chgJ6nRV5uJUpM3vSKUP7J3eJEtF2',
       id, TRUE, FALSE, TIMESTAMP '2026-08-01 08:00:00', TIMESTAMP '2026-08-01 08:00:00'
FROM roles WHERE code = 'CONTRIBUTOR';

INSERT INTO accounts (username, full_name, email, password_hash, role_id, status, is_deleted, created_at, updated_at)
SELECT 'reader@fu.edu.vn', 'Doc gia D', 'reader@fu.edu.vn', '$2a$10$GmqHtoePRBSR2GKTMFtx5.ms0JvoNDXr2r75EbcDP3uWwx9iimb2O',
       id, TRUE, FALSE, TIMESTAMP '2026-08-01 08:00:00', TIMESTAMP '2026-08-01 08:00:00'
FROM roles WHERE code = 'READER';

-- Tac vu hen gio dang bai ghi nhat ky duoi ten tai khoan nay - AC-ED-003 + AC-ED-004.
INSERT INTO accounts (username, full_name, email, password_hash, role_id, status, is_deleted, created_at, updated_at)
SELECT 'system@fu.edu.vn', 'He thong', 'system@fu.edu.vn', '!KHONG-PHAI-HASH-KHONG-DANG-NHAP-DUOC',
       id, FALSE, FALSE, TIMESTAMP '2026-08-01 08:00:00', TIMESTAMP '2026-08-01 08:00:00'
FROM roles WHERE code = 'ADMIN';

-- ---- Danh muc ----
INSERT INTO categories (name, slug, description, parent_id, display_order, is_active, is_deleted, created_at, updated_at) VALUES
    ('Tuyen sinh', 'tuyen-sinh', 'Tin tuc tuyen sinh', NULL, 1, TRUE, FALSE,
     TIMESTAMP '2026-08-01 08:00:00', TIMESTAMP '2026-08-01 08:00:00'),
    ('Su kien',    'su-kien',    'Su kien cua truong', NULL, 2, TRUE, FALSE,
     TIMESTAMP '2026-08-01 08:00:00', TIMESTAMP '2026-08-01 08:00:00');

INSERT INTO categories (name, slug, description, parent_id, display_order, is_active, is_deleted, created_at, updated_at)
SELECT 'Hoc bong', 'hoc-bong', 'Thong tin hoc bong', id, 1, TRUE, FALSE,
       TIMESTAMP '2026-08-01 08:00:00', TIMESTAMP '2026-08-01 08:00:00'
FROM categories WHERE slug = 'tuyen-sinh';

-- ---- The ----
INSERT INTO tags (name, slug, is_deleted, created_at) VALUES
    ('tuyen sinh', 'tuyen-sinh', FALSE, TIMESTAMP '2026-08-01 08:00:00'),
    ('2026',       '2026',       FALSE, TIMESTAMP '2026-08-01 08:00:00'),
    ('viec lam',   'viec-lam',   FALSE, TIMESTAMP '2026-08-01 08:00:00'),
    ('hoc bong',   'hoc-bong',   FALSE, TIMESTAMP '2026-08-01 08:00:00');

-- ---- Bai viet: ba trang thai khac nhau de demo duoc ca luong lam viec ----
-- rating_count va rating_sum khop voi dong danh gia o duoi, khong dat tay lech nhau.
INSERT INTO articles (title, slug, summary, content, source, category_id, status, author_id, reviewer_id,
                      submitted_at, reviewed_at, published_at, view_count, rating_count, rating_sum,
                      is_featured, is_deleted, created_at, updated_at, version)
SELECT 'Thong bao tuyen sinh 2026', 'thong-bao-tuyen-sinh-2026',
       'Truong mo dot tuyen sinh moi cho nam hoc 2026-2027',
       'Noi dung chi tiet ve dot tuyen sinh nam 2026.',
       'Phong tuyen sinh',
       c.id, 'PUBLISHED', author.id, reviewer.id,
       TIMESTAMP '2026-08-28 09:00:00', TIMESTAMP '2026-08-30 14:00:00', TIMESTAMP '2026-09-01 07:00:00',
       0, 1, 5, FALSE, FALSE,
       TIMESTAMP '2026-08-28 09:00:00', TIMESTAMP '2026-09-01 07:00:00', 0
FROM categories c,
     (SELECT id FROM accounts WHERE email = 'b@fu.edu.vn') author,
     (SELECT id FROM accounts WHERE email = 'a@fu.edu.vn') reviewer
WHERE c.slug = 'tuyen-sinh';

INSERT INTO articles (title, slug, summary, content, source, category_id, status, author_id,
                      submitted_at, view_count, rating_count, rating_sum,
                      is_featured, is_deleted, created_at, updated_at, version)
SELECT 'Ngay hoi viec lam 2026', 'ngay-hoi-viec-lam-2026',
       'Hon 50 doanh nghiep tham gia tuyen dung tai san truong',
       'Ngay hoi viec lam duoc to chuc tai san truong, du kien thu hut hon 2000 sinh vien.',
       'Phong CTSV',
       c.id, 'PENDING_REVIEW', a.id,
       TIMESTAMP '2026-10-20 10:30:00', 0, 0, 0, FALSE, FALSE,
       TIMESTAMP '2026-10-19 09:00:00', TIMESTAMP '2026-10-20 10:30:00', 0
FROM categories c,
     (SELECT id FROM accounts WHERE email = 'c@fu.edu.vn') a
WHERE c.slug = 'su-kien';

INSERT INTO articles (title, slug, summary, content, source, category_id, status, author_id, reviewer_id,
                      submitted_at, reviewed_at, view_count, rating_count, rating_sum,
                      is_featured, is_deleted, created_at, updated_at, version)
SELECT 'Hoc bong ky Xuan 2027', 'hoc-bong-ky-xuan-2027',
       'Danh sach hoc bong du kien cho ky Xuan 2027',
       'Bai nay dang o trang thai CHANGES_REQUESTED de demo luong tra bai ve cho tac gia sua.',
       'Phong CTSV',
       c.id, 'CHANGES_REQUESTED', author.id, reviewer.id,
       TIMESTAMP '2026-10-18 08:00:00', TIMESTAMP '2026-10-19 09:15:00',
       0, 0, 0, FALSE, FALSE,
       TIMESTAMP '2026-10-17 08:00:00', TIMESTAMP '2026-10-19 09:15:00', 0
FROM categories c,
     (SELECT id FROM accounts WHERE email = 'b@fu.edu.vn') author,
     (SELECT id FROM accounts WHERE email = 'a@fu.edu.vn') reviewer
WHERE c.slug = 'hoc-bong';

-- ---- Gan the cho bai viet ----
INSERT INTO article_tags (article_id, tag_id)
SELECT a.id, t.id
FROM articles a
JOIN tags t ON
     (a.slug = 'thong-bao-tuyen-sinh-2026' AND t.slug IN ('tuyen-sinh', '2026'))
  OR (a.slug = 'ngay-hoi-viec-lam-2026'    AND t.slug = 'viec-lam')
  OR (a.slug = 'hoc-bong-ky-xuan-2027'     AND t.slug = 'hoc-bong');

-- ---- Nhat ky kiem toan ----
INSERT INTO article_audit_logs (article_id, from_status, to_status, action, note, actor_id, created_at)
SELECT a.id, 'DRAFT', 'PENDING_REVIEW', 'SUBMIT', NULL, a.author_id, a.submitted_at
FROM articles a WHERE a.submitted_at IS NOT NULL;

INSERT INTO article_audit_logs (article_id, from_status, to_status, action, note, actor_id, created_at)
SELECT a.id, 'PENDING_REVIEW', 'CHANGES_REQUESTED', 'REQUEST_CHANGES',
       'Bo sung danh sach nganh duoc cap hoc bong va han nop ho so.',
       a.reviewer_id, a.reviewed_at
FROM articles a WHERE a.slug = 'hoc-bong-ky-xuan-2027';

INSERT INTO article_audit_logs (article_id, from_status, to_status, action, note, actor_id, created_at)
SELECT a.id, 'SCHEDULED', 'PUBLISHED', 'PUBLISH', NULL, a.reviewer_id, a.published_at
FROM articles a WHERE a.slug = 'thong-bao-tuyen-sinh-2026';

-- ---- Mot danh gia ----
INSERT INTO article_ratings (article_id, account_id, score, is_deleted, created_at, updated_at)
SELECT a.id, r.id, 5, FALSE, TIMESTAMP '2026-09-02 10:00:00', TIMESTAMP '2026-09-02 10:00:00'
FROM articles a,
     (SELECT id FROM accounts WHERE email = 'reader@fu.edu.vn') r
WHERE a.slug = 'thong-bao-tuyen-sinh-2026';

-- ---- Mot thong bao: khop voi dong REQUEST_CHANGES o tren - AC-ED-002 ----
INSERT INTO notifications (account_id, article_id, type, message, is_read, is_deleted, created_at)
SELECT a.author_id, a.id, 'CHANGES_REQUESTED',
       'Bai Hoc bong ky Xuan 2027 bi tra lai: bo sung danh sach nganh duoc cap hoc bong va han nop ho so.',
       FALSE, FALSE, a.reviewed_at
FROM articles a WHERE a.slug = 'hoc-bong-ky-xuan-2027';

-- ---- Quy tac bien tap: dong cau hinh duy nhat, id = 1 - SRS muc 3.2 ----
INSERT INTO editorial_rules (id, auto_publish_scheduled, require_note_on_approve, min_summary_length, updated_at)
VALUES (1, TRUE, FALSE, 0, TIMESTAMP '2026-08-01 08:00:00');
