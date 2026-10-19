-- Du lieu mau danh RIENG cho test tich hop.
-- Test dung H2 in-memory voi ddl-auto=create-drop: Hibernate tao bang tu entity,
-- roi file nay do du lieu vao. Khong can IF NOT EXISTS vi moi lan chay la database moi.
--
-- Noi dung phai giong het data-sqlserver.sql de test phan anh dung thuc te.

INSERT INTO accounts (name, email, password_hash, role) VALUES
    ('Nguyen Van A', 'a@fu.edu.vn',     '$2a$10$Wyb/D763Cp.k/ITD8s1yXeK.S4m/81BXi86lcuG/4sVZqd4DVADSS', 1),
    ('Tran Thi B',   'b@fu.edu.vn',     '$2a$10$sFJqjSh.vltVK/KxP141uu1HakD1h7lgIyQN0SrBcSBn5YAfLrA4a', 2),
    ('Le Van C',     'c@fu.edu.vn',     '$2a$10$m1lXvQazRju5fPCewTzXZO/3chgJ6nRV5uJUpM3vSKUP7J3eJEtF2', 2),
    ('Admin',        'admin@fu.edu.vn', '$2a$10$8HaFznDL4HD0Hk3VUYq79.Kfo0fuOewjAsYD5T8.VZrHd10cdIgoW', 1);

INSERT INTO categories (name, description, parent_id, is_active) VALUES
    ('Tuyen sinh', 'Tin tuc tuyen sinh', NULL, 1),
    ('Su kien',    'Su kien cua truong', NULL, 1);

INSERT INTO categories (name, description, parent_id, is_active)
SELECT 'Hoc bong', 'Thong tin hoc bong', id, 0 FROM categories WHERE name = 'Tuyen sinh';

INSERT INTO news_articles
    (title, headline, content, source, category_id, tags, status, created_by_id, created_date)
SELECT 'Thong bao tuyen sinh 2026', 'Truong mo dot tuyen sinh moi',
       'Noi dung chi tiet ve dot tuyen sinh nam 2026.', 'Phong tuyen sinh',
       c.id, 'tuyensinh, 2026', 1, a.id, DATE '2026-09-01'
FROM categories c, accounts a
WHERE c.name = 'Tuyen sinh' AND a.email = 'b@fu.edu.vn';

INSERT INTO news_articles
    (title, headline, content, source, category_id, tags, status, created_by_id, created_date)
SELECT 'Ngay hoi viec lam', 'Hon 50 doanh nghiep tham gia',
       'Ngay hoi viec lam duoc to chuc tai san truong.', 'Phong CTSV',
       c.id, 'sukien, vieclam', 0, a.id, DATE '2026-09-15'
FROM categories c, accounts a
WHERE c.name = 'Su kien' AND a.email = 'c@fu.edu.vn';
