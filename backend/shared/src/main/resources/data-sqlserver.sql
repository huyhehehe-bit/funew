-- Du lieu mau cho SQL Server.
-- Moi lenh deu co IF NOT EXISTS nen chay lai nhieu lan khong nhan doi du lieu,
-- va khong ghi de nhung gi nguoi dung da them.
--
-- Mat khau luu duoi dang hash BCrypt, khong bao gio luu dang thuong.
-- a@ / b@ / c@ co mat khau "123", admin@ co mat khau "Admin".
-- b@ va c@ cung mat khau nhung hash khac han nhau vi BCrypt dung salt ngau nhien.

IF NOT EXISTS (SELECT 1 FROM dbo.accounts)
INSERT INTO dbo.accounts (name, email, password_hash, role) VALUES
    (N'Nguyen Van A', 'a@fu.edu.vn',     '$2a$10$Wyb/D763Cp.k/ITD8s1yXeK.S4m/81BXi86lcuG/4sVZqd4DVADSS', 1),
    (N'Tran Thi B',   'b@fu.edu.vn',     '$2a$10$sFJqjSh.vltVK/KxP141uu1HakD1h7lgIyQN0SrBcSBn5YAfLrA4a', 2),
    (N'Le Van C',     'c@fu.edu.vn',     '$2a$10$m1lXvQazRju5fPCewTzXZO/3chgJ6nRV5uJUpM3vSKUP7J3eJEtF2', 2),
    (N'Admin',        'admin@fu.edu.vn', '$2a$10$8HaFznDL4HD0Hk3VUYq79.Kfo0fuOewjAsYD5T8.VZrHd10cdIgoW', 1);

IF NOT EXISTS (SELECT 1 FROM dbo.categories)
INSERT INTO dbo.categories (name, description, parent_id, is_active) VALUES
    (N'Tuyen sinh', N'Tin tuc tuyen sinh',   NULL, 1),
    (N'Su kien',    N'Su kien cua truong',   NULL, 1);

IF NOT EXISTS (SELECT 1 FROM dbo.categories WHERE name = N'Hoc bong')
INSERT INTO dbo.categories (name, description, parent_id, is_active)
SELECT N'Hoc bong', N'Thong tin hoc bong', id, 0 FROM dbo.categories WHERE name = N'Tuyen sinh';

IF NOT EXISTS (SELECT 1 FROM dbo.news_articles)
INSERT INTO dbo.news_articles
    (title, headline, content, source, category_id, tags, status, created_by_id, created_date)
SELECT N'Thong bao tuyen sinh 2026', N'Truong mo dot tuyen sinh moi',
       N'Noi dung chi tiet ve dot tuyen sinh nam 2026.', N'Phong tuyen sinh',
       c.id, N'tuyensinh, 2026', 1, a.id, '2026-09-01'
FROM dbo.categories c, dbo.accounts a
WHERE c.name = N'Tuyen sinh' AND a.email = 'b@fu.edu.vn';

IF NOT EXISTS (SELECT 1 FROM dbo.news_articles WHERE title = N'Ngay hoi viec lam')
INSERT INTO dbo.news_articles
    (title, headline, content, source, category_id, tags, status, created_by_id, created_date)
SELECT N'Ngay hoi viec lam', N'Hon 50 doanh nghiep tham gia',
       N'Ngay hoi viec lam duoc to chuc tai san truong.', N'Phong CTSV',
       c.id, N'sukien, vieclam', 0, a.id, '2026-09-15'
FROM dbo.categories c, dbo.accounts a
WHERE c.name = N'Su kien' AND a.email = 'c@fu.edu.vn';
