/* =====================================================================
   FU News Management System - Enhancement
   Module 1: Editorial Workflow & Approval Management
   + Role Reader va chuc nang danh gia 1-5 sao

   SQL Server 2022
   Database: FUNews
   Phien ban: 1.0   Ngay: 2026-10-22

   NGUYEN TAC THIET KE
   -------------------
   1. XOA MEM toan bo. Khong co lenh DELETE nao trong ung dung.
      Moi bang co is_deleted / deleted_at / deleted_by_id.
      Ngoai le duy nhat: article_audit_logs - log phai bat bien (NFR-ED-004).

   2. Rang buoc UNIQUE phai la FILTERED INDEX voi WHERE is_deleted = 0.
      Neu dung UNIQUE thuong thi xoa mem roi tao lai cung email/slug se bi chan,
      vi dong cu van nam trong bang.

   3. Khoa ngoai deu la NO ACTION (mac dinh). Khong can CASCADE vi khong bao gio
      xoa cung. Them nua, bang articles co 4 khoa ngoai tro ve accounts - neu
      dung CASCADE thi SQL Server bao loi "multiple cascade paths" va tu choi.

   4. Noi dung bai viet dung NVARCHAR(MAX), KHONG dung TEXT.
      Kieu TEXT da bi Microsoft khai tu tu SQL Server 2005.
      NVARCHAR(MAX) chua khoang 1 ty ky tu Unicode - thua cho bai vai nghin chu.
      Dung NVARCHAR chu khong VARCHAR vi tieng Viet co dau.

   CACH CHAY
   ---------
   sqlcmd -S localhost -E -d FUNews -i module1_schema.sql
   Hoac mo bang SSMS, chon database FUNews roi Execute.

   Script chay lai nhieu lan khong loi va khong nhan doi du lieu.
   ===================================================================== */


/* ---------------------------------------------------------------------
   BAT BUOC: filtered index va cot tinh toan PERSISTED chi tao duoc khi
   QUOTED_IDENTIFIER va ANSI_NULLS deu ON. SSMS bat san, nhung sqlcmd thi
   TAT mac dinh - khong co hai dong nay se loi "CREATE INDEX failed because
   the following SET options have incorrect settings: QUOTED_IDENTIFIER".
--------------------------------------------------------------------- */
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
GO


/* ---------------------------------------------------------------------
   PHAN 0 - XOA SACH DE LAM LAI TU DAU  (MAC DINH TAT)
   Bo dau /* */ bao quanh khoi duoi day neu muon dung.
   CANH BAO: xoa vinh vien moi du lieu dang co.
   Thu tu xoa nguoc voi thu tu tao, de khong vuong khoa ngoai.
--------------------------------------------------------------------- */
/*
DROP TABLE IF EXISTS dbo.article_ratings;
DROP TABLE IF EXISTS dbo.article_audit_logs;
DROP TABLE IF EXISTS dbo.article_tags;
DROP TABLE IF EXISTS dbo.tags;
DROP VIEW  IF EXISTS dbo.v_published_articles;
DROP TABLE IF EXISTS dbo.articles;
DROP TABLE IF EXISTS dbo.categories;
DROP TABLE IF EXISTS dbo.accounts;
DROP TABLE IF EXISTS dbo.roles;
GO
*/


/* =====================================================================
   PHAN 1 - CAU TRUC BANG
   ===================================================================== */

/* --- 1.1 Vai tro ----------------------------------------------------
   Bang tra cuu. Khong xoa mem vi so luong co dinh va duoc tham chieu
   boi moi tai khoan. Muon ngung dung mot role thi dat is_active = 0.
-------------------------------------------------------------------- */
IF OBJECT_ID('dbo.roles', 'U') IS NULL
CREATE TABLE dbo.roles (
    id          BIGINT        IDENTITY(1,1) NOT NULL PRIMARY KEY,
    code        NVARCHAR(30)  NOT NULL UNIQUE,
    name        NVARCHAR(100) NOT NULL,
    description NVARCHAR(500) NOT NULL DEFAULT N'',
    is_active   BIT           NOT NULL DEFAULT 1
);
GO

/* --- 1.2 Tai khoan --------------------------------------------------
   deleted_by_id tro ve chinh bang accounts: luu ai da xoa tai khoan nay.
-------------------------------------------------------------------- */
IF OBJECT_ID('dbo.accounts', 'U') IS NULL
CREATE TABLE dbo.accounts (
    id            BIGINT        IDENTITY(1,1) NOT NULL PRIMARY KEY,
    full_name     NVARCHAR(150) NOT NULL,
    email         NVARCHAR(254) NOT NULL,
    password_hash NVARCHAR(255) NOT NULL,
    role_id       BIGINT        NOT NULL,
    is_active     BIT           NOT NULL DEFAULT 1,
    is_deleted    BIT           NOT NULL DEFAULT 0,
    deleted_at    DATETIME2(0)  NULL,
    deleted_by_id BIGINT        NULL,
    created_at    DATETIME2(0)  NOT NULL DEFAULT SYSDATETIME(),
    updated_at    DATETIME2(0)  NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_accounts_role       FOREIGN KEY (role_id)       REFERENCES dbo.roles(id),
    CONSTRAINT fk_accounts_deleted_by FOREIGN KEY (deleted_by_id) REFERENCES dbo.accounts(id)
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_accounts_email')
CREATE UNIQUE INDEX ux_accounts_email
    ON dbo.accounts(email) WHERE is_deleted = 0;
GO

/* --- 1.3 Danh muc ---------------------------------------------------
   parent_id tu tham chieu de lam danh muc nhieu cap (giong VnExpress:
   Thoi su > Chinh tri). display_order de sap xep menu theo y muon.
-------------------------------------------------------------------- */
IF OBJECT_ID('dbo.categories', 'U') IS NULL
CREATE TABLE dbo.categories (
    id            BIGINT         IDENTITY(1,1) NOT NULL PRIMARY KEY,
    name          NVARCHAR(150)  NOT NULL,
    slug          NVARCHAR(180)  NOT NULL,
    description   NVARCHAR(2000) NOT NULL DEFAULT N'',
    parent_id     BIGINT         NULL,
    display_order INT            NOT NULL DEFAULT 0,
    is_active     BIT            NOT NULL DEFAULT 1,
    is_deleted    BIT            NOT NULL DEFAULT 0,
    deleted_at    DATETIME2(0)   NULL,
    deleted_by_id BIGINT         NULL,
    created_at    DATETIME2(0)   NOT NULL DEFAULT SYSDATETIME(),
    updated_at    DATETIME2(0)   NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_categories_parent     FOREIGN KEY (parent_id)     REFERENCES dbo.categories(id),
    CONSTRAINT fk_categories_deleted_by FOREIGN KEY (deleted_by_id) REFERENCES dbo.accounts(id)
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_categories_slug')
CREATE UNIQUE INDEX ux_categories_slug
    ON dbo.categories(slug) WHERE is_deleted = 0;
GO

/* --- 1.4 Bai viet ---------------------------------------------------
   status: may trang thai 6 buoc theo FR-ED-001.
     DRAFT             nhap, chi tac gia thay
     PENDING_REVIEW    da nop, khoa sua, cho bien tap
     CHANGES_REQUESTED bien tap tra lai kem ly do
     SCHEDULED         da duyet, cho toi gio dang (gop "Approved / Scheduled")
     PUBLISHED         dang cong khai
     ARCHIVED          go khoi trang cong khai, van giu trong database

   rating_avg la COT TINH TOAN PERSISTED: SQL Server tu tinh tu rating_sum
   va rating_count roi luu san. Code Java khong bao gio ghi vao cot nay,
   nen diem trung binh khong the lech voi du lieu that.
-------------------------------------------------------------------- */
IF OBJECT_ID('dbo.articles', 'U') IS NULL
CREATE TABLE dbo.articles (
    id                   BIGINT         IDENTITY(1,1) NOT NULL PRIMARY KEY,
    title                NVARCHAR(300)  NOT NULL,
    slug                 NVARCHAR(350)  NOT NULL,
    summary              NVARCHAR(1000) NOT NULL DEFAULT N'',
    content              NVARCHAR(MAX)  NOT NULL,
    thumbnail_url        NVARCHAR(500)  NULL,
    source               NVARCHAR(500)  NOT NULL DEFAULT N'',
    category_id          BIGINT         NOT NULL,
    status               NVARCHAR(20)   NOT NULL DEFAULT N'DRAFT',
    author_id            BIGINT         NOT NULL,
    reviewer_id          BIGINT         NULL,
    submitted_at         DATETIME2(0)   NULL,
    reviewed_at          DATETIME2(0)   NULL,
    scheduled_publish_at DATETIME2(0)   NULL,
    published_at         DATETIME2(0)   NULL,
    view_count           BIGINT         NOT NULL DEFAULT 0,
    rating_count         BIGINT         NOT NULL DEFAULT 0,
    rating_sum           BIGINT         NOT NULL DEFAULT 0,
    /* DECIMAL(3,2) chua duoc 0.00 den 9.99 - du cho thang diem 1.00 den 5.00.
       Khong boc CAST thi phep chia tra ve so kieu 3.5000000000000000000000
       va con so do se chui thang vao JSON tra cho frontend. */
    rating_avg           AS (CAST(CASE WHEN rating_count = 0 THEN 0
                                       ELSE CAST(rating_sum AS DECIMAL(10,4)) / rating_count
                                  END AS DECIMAL(3,2))) PERSISTED,
    is_featured          BIT            NOT NULL DEFAULT 0,
    is_deleted           BIT            NOT NULL DEFAULT 0,
    deleted_at           DATETIME2(0)   NULL,
    deleted_by_id        BIGINT         NULL,
    created_at           DATETIME2(0)   NOT NULL DEFAULT SYSDATETIME(),
    updated_at           DATETIME2(0)   NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_articles_category   FOREIGN KEY (category_id)   REFERENCES dbo.categories(id),
    CONSTRAINT fk_articles_author     FOREIGN KEY (author_id)     REFERENCES dbo.accounts(id),
    CONSTRAINT fk_articles_reviewer   FOREIGN KEY (reviewer_id)   REFERENCES dbo.accounts(id),
    CONSTRAINT fk_articles_deleted_by FOREIGN KEY (deleted_by_id) REFERENCES dbo.accounts(id),
    CONSTRAINT ck_articles_status CHECK (status IN
        (N'DRAFT', N'PENDING_REVIEW', N'CHANGES_REQUESTED',
         N'SCHEDULED', N'PUBLISHED', N'ARCHIVED')),
    /* Da SCHEDULED thi bat buoc phai co gio hen - FR-ED-003 */
    CONSTRAINT ck_articles_scheduled CHECK
        (status <> N'SCHEDULED' OR scheduled_publish_at IS NOT NULL),
    /* Da PUBLISHED thi bat buoc phai co thoi diem dang */
    CONSTRAINT ck_articles_published CHECK
        (status <> N'PUBLISHED' OR published_at IS NOT NULL),
    CONSTRAINT ck_articles_rating CHECK (rating_sum >= 0 AND rating_count >= 0)
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_articles_slug')
CREATE UNIQUE INDEX ux_articles_slug
    ON dbo.articles(slug) WHERE is_deleted = 0;
GO

/* Phuc vu tac vu nen hen gio dang va hang doi duyet bai */
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_articles_status_schedule')
CREATE INDEX ix_articles_status_schedule
    ON dbo.articles(status, scheduled_publish_at) WHERE is_deleted = 0;
GO

/* Phuc vu trang cong khai: tin moi nhat theo danh muc */
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_articles_public')
CREATE INDEX ix_articles_public
    ON dbo.articles(category_id, published_at DESC)
    INCLUDE (title, summary, thumbnail_url, rating_avg)
    WHERE is_deleted = 0 AND status = N'PUBLISHED';
GO

/* --- 1.5 The (tag) --------------------------------------------------
   Tach bang rieng thay vi luu chuoi "a, b, c" nhu truoc, de lam duoc
   trang xem bai theo tag giong VnExpress.
-------------------------------------------------------------------- */
IF OBJECT_ID('dbo.tags', 'U') IS NULL
CREATE TABLE dbo.tags (
    id         BIGINT        IDENTITY(1,1) NOT NULL PRIMARY KEY,
    name       NVARCHAR(100) NOT NULL,
    slug       NVARCHAR(120) NOT NULL,
    is_deleted BIT           NOT NULL DEFAULT 0,
    deleted_at DATETIME2(0)  NULL,
    created_at DATETIME2(0)  NOT NULL DEFAULT SYSDATETIME()
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_tags_slug')
CREATE UNIQUE INDEX ux_tags_slug ON dbo.tags(slug) WHERE is_deleted = 0;
GO

IF OBJECT_ID('dbo.article_tags', 'U') IS NULL
CREATE TABLE dbo.article_tags (
    article_id BIGINT NOT NULL,
    tag_id     BIGINT NOT NULL,
    CONSTRAINT pk_article_tags PRIMARY KEY (article_id, tag_id),
    CONSTRAINT fk_article_tags_article FOREIGN KEY (article_id) REFERENCES dbo.articles(id),
    CONSTRAINT fk_article_tags_tag     FOREIGN KEY (tag_id)     REFERENCES dbo.tags(id)
);
GO

/* --- 1.6 Nhat ky kiem toan ------------------------------------------
   FR-ED-004 va NFR-ED-004: ghi lai moi lan doi trang thai, va log phai
   BAT BIEN. Bang nay co tinh KHONG co cot xoa mem - khong ai duoc xoa,
   ke ca Administrator.
-------------------------------------------------------------------- */
IF OBJECT_ID('dbo.article_audit_logs', 'U') IS NULL
CREATE TABLE dbo.article_audit_logs (
    id          BIGINT         IDENTITY(1,1) NOT NULL PRIMARY KEY,
    article_id  BIGINT         NOT NULL,
    from_status NVARCHAR(20)   NULL,
    to_status   NVARCHAR(20)   NOT NULL,
    action      NVARCHAR(30)   NOT NULL,
    note        NVARCHAR(2000) NULL,
    actor_id    BIGINT         NOT NULL,
    created_at  DATETIME2(0)   NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_audit_article FOREIGN KEY (article_id) REFERENCES dbo.articles(id),
    CONSTRAINT fk_audit_actor   FOREIGN KEY (actor_id)   REFERENCES dbo.accounts(id),
    /* FR-ED-002: tra bai lai thi BAT BUOC co ly do. Rang buoc o tang database,
       khong phu thuoc vao viec tang ung dung co kiem tra hay khong. */
    CONSTRAINT ck_audit_note_required CHECK
        (to_status <> N'CHANGES_REQUESTED' OR note IS NOT NULL)
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_audit_article')
CREATE INDEX ix_audit_article ON dbo.article_audit_logs(article_id, created_at DESC);
GO

/* --- 1.7 Danh gia sao -----------------------------------------------
   Doc gia cham 1-5 sao. Moi nguoi chi cham MOT lan cho MOT bai,
   nhung duoc sua diem (UPDATE chu khong INSERT them dong moi).
-------------------------------------------------------------------- */
IF OBJECT_ID('dbo.article_ratings', 'U') IS NULL
CREATE TABLE dbo.article_ratings (
    id         BIGINT       IDENTITY(1,1) NOT NULL PRIMARY KEY,
    article_id BIGINT       NOT NULL,
    account_id BIGINT       NOT NULL,
    score      TINYINT      NOT NULL,
    is_deleted BIT          NOT NULL DEFAULT 0,
    deleted_at DATETIME2(0) NULL,
    created_at DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
    updated_at DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_ratings_article FOREIGN KEY (article_id) REFERENCES dbo.articles(id),
    CONSTRAINT fk_ratings_account FOREIGN KEY (account_id) REFERENCES dbo.accounts(id),
    CONSTRAINT ck_ratings_score   CHECK (score BETWEEN 1 AND 5)
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_ratings_article_account')
CREATE UNIQUE INDEX ux_ratings_article_account
    ON dbo.article_ratings(article_id, account_id) WHERE is_deleted = 0;
GO


/* =====================================================================
   PHAN 2 - VIEW TIEN DUNG
   Trang cong khai PHAI doc qua view nay, khong truy van thang bang
   articles. Neu khong se ro bai DRAFT va PENDING_REVIEW ra ngoai.
   ===================================================================== */
IF OBJECT_ID('dbo.v_published_articles', 'V') IS NOT NULL
    DROP VIEW dbo.v_published_articles;
GO

CREATE VIEW dbo.v_published_articles AS
SELECT a.id, a.title, a.slug, a.summary, a.content, a.thumbnail_url, a.source,
       a.category_id, c.name AS category_name, c.slug AS category_slug,
       a.author_id, acc.full_name AS author_name,
       a.published_at, a.view_count,
       a.rating_count, a.rating_avg, a.is_featured
FROM dbo.articles a
JOIN dbo.categories c  ON c.id   = a.category_id
JOIN dbo.accounts   acc ON acc.id = a.author_id
WHERE a.is_deleted = 0
  AND a.status     = N'PUBLISHED';
GO


/* =====================================================================
   PHAN 3 - DU LIEU MAU
   Moi lenh co IF NOT EXISTS nen chay lai khong nhan doi,
   va khong ghi de nhung gi nguoi dung da them.

   Mat khau luu dang hash BCrypt, khong bao gio luu dang thuong.
   admin@ co mat khau "Admin", cac tai khoan con lai la "123".
   Luu y: b@ va c@ cung mat khau nhung hash khac han nhau,
   vi BCrypt sinh salt ngau nhien moi lan ma hoa.
   ===================================================================== */

/* --- 3.1 Vai tro ---------------------------------------------------- */
IF NOT EXISTS (SELECT 1 FROM dbo.roles)
INSERT INTO dbo.roles (code, name, description) VALUES
    (N'ADMIN',       N'Quan tri vien',  N'Ghi de moi luong, gan vai tro, cau hinh quy tac bien tap'),
    (N'EDITOR',      N'Bien tap vien',  N'Duyet bai, tra lai kem ly do, hen gio dang'),
    (N'CONTRIBUTOR', N'Cong tac vien',  N'Tao va nop bai cua chinh minh, khong tu dang duoc'),
    (N'READER',      N'Doc gia',        N'Doc bai da xuat ban, tim kiem, cham diem 1-5 sao');
GO

/* --- 3.2 Tai khoan -------------------------------------------------- */
IF NOT EXISTS (SELECT 1 FROM dbo.accounts)
INSERT INTO dbo.accounts (full_name, email, password_hash, role_id)
SELECT v.full_name, v.email, v.password_hash, r.id
FROM (VALUES
    (N'Admin',        'admin@fu.edu.vn',  '$2a$10$8HaFznDL4HD0Hk3VUYq79.Kfo0fuOewjAsYD5T8.VZrHd10cdIgoW', N'ADMIN'),
    (N'Nguyen Van A', 'a@fu.edu.vn',      '$2a$10$Wyb/D763Cp.k/ITD8s1yXeK.S4m/81BXi86lcuG/4sVZqd4DVADSS', N'EDITOR'),
    (N'Tran Thi B',   'b@fu.edu.vn',      '$2a$10$sFJqjSh.vltVK/KxP141uu1HakD1h7lgIyQN0SrBcSBn5YAfLrA4a', N'CONTRIBUTOR'),
    (N'Le Van C',     'c@fu.edu.vn',      '$2a$10$m1lXvQazRju5fPCewTzXZO/3chgJ6nRV5uJUpM3vSKUP7J3eJEtF2', N'CONTRIBUTOR'),
    (N'Doc gia D',    'reader@fu.edu.vn', '$2a$10$GmqHtoePRBSR2GKTMFtx5.ms0JvoNDXr2r75EbcDP3uWwx9iimb2O', N'READER')
) AS v(full_name, email, password_hash, role_code)
JOIN dbo.roles r ON r.code = v.role_code;
GO

/* --- 3.3 Danh muc --------------------------------------------------- */
IF NOT EXISTS (SELECT 1 FROM dbo.categories)
INSERT INTO dbo.categories (name, slug, description, parent_id, display_order) VALUES
    (N'Tuyen sinh', N'tuyen-sinh', N'Tin tuc tuyen sinh',  NULL, 1),
    (N'Su kien',    N'su-kien',    N'Su kien cua truong',  NULL, 2);
GO

IF NOT EXISTS (SELECT 1 FROM dbo.categories WHERE slug = N'hoc-bong')
INSERT INTO dbo.categories (name, slug, description, parent_id, display_order)
SELECT N'Hoc bong', N'hoc-bong', N'Thong tin hoc bong', id, 1
FROM dbo.categories WHERE slug = N'tuyen-sinh';
GO

/* --- 3.4 The -------------------------------------------------------- */
IF NOT EXISTS (SELECT 1 FROM dbo.tags)
INSERT INTO dbo.tags (name, slug) VALUES
    (N'tuyen sinh', N'tuyen-sinh'),
    (N'2026',       N'2026'),
    (N'viec lam',   N'viec-lam'),
    (N'hoc bong',   N'hoc-bong');
GO

/* --- 3.5 Bai viet mau ----------------------------------------------
   Ba bai o ba trang thai khac nhau de demo duoc ca luong lam viec:
   mot bai da dang, mot bai dang cho duyet, mot bai bi tra lai.
-------------------------------------------------------------------- */
IF NOT EXISTS (SELECT 1 FROM dbo.articles)
INSERT INTO dbo.articles
    (title, slug, summary, content, source, category_id, status,
     author_id, reviewer_id, submitted_at, reviewed_at, published_at)
SELECT N'Thong bao tuyen sinh 2026',
       N'thong-bao-tuyen-sinh-2026',
       N'Truong mo dot tuyen sinh moi cho nam hoc 2026-2027',
       N'Noi dung chi tiet ve dot tuyen sinh nam 2026. Phan nay dung NVARCHAR(MAX) nen chua duoc bai dai vai nghin chu.',
       N'Phong tuyen sinh',
       c.id, N'PUBLISHED',
       author.id, reviewer.id,
       '2026-08-28T09:00:00', '2026-08-30T14:00:00', '2026-09-01T07:00:00'
FROM dbo.categories c
CROSS JOIN (SELECT id FROM dbo.accounts WHERE email = 'b@fu.edu.vn') author
CROSS JOIN (SELECT id FROM dbo.accounts WHERE email = 'a@fu.edu.vn') reviewer
WHERE c.slug = N'tuyen-sinh';
GO

IF NOT EXISTS (SELECT 1 FROM dbo.articles WHERE slug = N'ngay-hoi-viec-lam-2026')
INSERT INTO dbo.articles
    (title, slug, summary, content, source, category_id, status, author_id, submitted_at)
SELECT N'Ngay hoi viec lam 2026',
       N'ngay-hoi-viec-lam-2026',
       N'Hon 50 doanh nghiep tham gia tuyen dung tai san truong',
       N'Ngay hoi viec lam duoc to chuc tai san truong, du kien thu hut hon 2000 sinh vien.',
       N'Phong CTSV',
       c.id, N'PENDING_REVIEW', a.id, '2026-10-20T10:30:00'
FROM dbo.categories c
CROSS JOIN (SELECT id FROM dbo.accounts WHERE email = 'c@fu.edu.vn') a
WHERE c.slug = N'su-kien';
GO

IF NOT EXISTS (SELECT 1 FROM dbo.articles WHERE slug = N'hoc-bong-ky-xuan-2027')
INSERT INTO dbo.articles
    (title, slug, summary, content, source, category_id, status,
     author_id, reviewer_id, submitted_at, reviewed_at)
SELECT N'Hoc bong ky Xuan 2027',
       N'hoc-bong-ky-xuan-2027',
       N'Danh sach hoc bong du kien cho ky Xuan 2027',
       N'Bai nay dang o trang thai CHANGES_REQUESTED de demo luong tra bai ve cho tac gia sua.',
       N'Phong CTSV',
       c.id, N'CHANGES_REQUESTED',
       author.id, reviewer.id,
       '2026-10-18T08:00:00', '2026-10-19T09:15:00'
FROM dbo.categories c
CROSS JOIN (SELECT id FROM dbo.accounts WHERE email = 'b@fu.edu.vn') author
CROSS JOIN (SELECT id FROM dbo.accounts WHERE email = 'a@fu.edu.vn') reviewer
WHERE c.slug = N'hoc-bong';
GO

/* --- 3.6 Gan the cho bai viet --------------------------------------- */
IF NOT EXISTS (SELECT 1 FROM dbo.article_tags)
INSERT INTO dbo.article_tags (article_id, tag_id)
SELECT a.id, t.id
FROM dbo.articles a
JOIN dbo.tags t ON
     (a.slug = N'thong-bao-tuyen-sinh-2026' AND t.slug IN (N'tuyen-sinh', N'2026'))
  OR (a.slug = N'ngay-hoi-viec-lam-2026'    AND t.slug = N'viec-lam')
  OR (a.slug = N'hoc-bong-ky-xuan-2027'     AND t.slug = N'hoc-bong');
GO

/* --- 3.7 Nhat ky kiem toan cho cac bai mau -------------------------- */
IF NOT EXISTS (SELECT 1 FROM dbo.article_audit_logs)
INSERT INTO dbo.article_audit_logs (article_id, from_status, to_status, action, note, actor_id, created_at)
SELECT a.id, N'DRAFT', N'PENDING_REVIEW', N'SUBMIT', NULL, a.author_id, a.submitted_at
FROM dbo.articles a WHERE a.submitted_at IS NOT NULL;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.article_audit_logs WHERE action = N'REQUEST_CHANGES')
INSERT INTO dbo.article_audit_logs (article_id, from_status, to_status, action, note, actor_id, created_at)
SELECT a.id, N'PENDING_REVIEW', N'CHANGES_REQUESTED', N'REQUEST_CHANGES',
       N'Bo sung danh sach nganh duoc cap hoc bong va han nop ho so.',
       a.reviewer_id, a.reviewed_at
FROM dbo.articles a WHERE a.slug = N'hoc-bong-ky-xuan-2027';
GO

IF NOT EXISTS (SELECT 1 FROM dbo.article_audit_logs WHERE action = N'PUBLISH')
INSERT INTO dbo.article_audit_logs (article_id, from_status, to_status, action, note, actor_id, created_at)
SELECT a.id, N'SCHEDULED', N'PUBLISHED', N'PUBLISH', NULL, a.reviewer_id, a.published_at
FROM dbo.articles a WHERE a.slug = N'thong-bao-tuyen-sinh-2026';
GO

/* --- 3.8 Mot danh gia mau -------------------------------------------
   Cap nhat ca bang rating va hai cot dem tren articles trong cung
   mot transaction. Ung dung cung phai lam dung nhu vay.
-------------------------------------------------------------------- */
IF NOT EXISTS (SELECT 1 FROM dbo.article_ratings)
BEGIN
    BEGIN TRANSACTION;

    INSERT INTO dbo.article_ratings (article_id, account_id, score)
    SELECT a.id, r.id, 5
    FROM dbo.articles a
    CROSS JOIN (SELECT id FROM dbo.accounts WHERE email = 'reader@fu.edu.vn') r
    WHERE a.slug = N'thong-bao-tuyen-sinh-2026';

    UPDATE a
    SET rating_count = a.rating_count + 1,
        rating_sum   = a.rating_sum + 5,
        updated_at   = SYSDATETIME()
    FROM dbo.articles a
    WHERE a.slug = N'thong-bao-tuyen-sinh-2026';

    COMMIT TRANSACTION;
END
GO


/* =====================================================================
   PHAN 4 - KIEM TRA SAU KHI CHAY
   ===================================================================== */
SELECT 'roles'               AS bang, COUNT(*) AS so_dong FROM dbo.roles
UNION ALL SELECT 'accounts',           COUNT(*) FROM dbo.accounts
UNION ALL SELECT 'categories',         COUNT(*) FROM dbo.categories
UNION ALL SELECT 'articles',           COUNT(*) FROM dbo.articles
UNION ALL SELECT 'tags',               COUNT(*) FROM dbo.tags
UNION ALL SELECT 'article_tags',       COUNT(*) FROM dbo.article_tags
UNION ALL SELECT 'article_audit_logs', COUNT(*) FROM dbo.article_audit_logs
UNION ALL SELECT 'article_ratings',    COUNT(*) FROM dbo.article_ratings;
GO
