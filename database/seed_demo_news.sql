/* ============================================================================
   FU News - Du lieu bai mau cho TRANG CHU doc bao
   ----------------------------------------------------------------------------
   Chay file nay tren DB DANG CO (khong DROP gi) de trang chu co nhieu bai da
   dang kem anh. Tat ca lenh deu co IF NOT EXISTS / dieu kien nen chay lai nhieu
   lan khong tao trung.

   Dung sau khi da chay module1_schema.sql it nhat mot lan (can san cac danh muc
   tuyen-sinh / su-kien / hoc-bong va tai khoan a@ , b@fu.edu.vn).

   Anh lay tu picsum.photos (can mang luc mo trang). Nguon (source) deu la URL
   that vi NewsRequest da bat @URL - bai co source khong phai URL se khong sua
   duoc qua giao dien nua.
   ============================================================================ */

/* --- 0. Vá dữ liệu cũ: đưa source về URL, thêm ảnh cho bài đã đăng ---------- */
UPDATE dbo.articles SET source = N'https://fu.edu.vn/tuyen-sinh'
 WHERE slug = N'thong-bao-tuyen-sinh-2026' AND (source IS NULL OR source NOT LIKE 'http%');
UPDATE dbo.articles SET source = N'https://fu.edu.vn/ctsv'
 WHERE slug IN (N'ngay-hoi-viec-lam-2026', N'hoc-bong-ky-xuan-2027')
   AND (source IS NULL OR source NOT LIKE 'http%');
UPDATE dbo.articles SET thumbnail_url = N'https://picsum.photos/seed/thong-bao-tuyen-sinh-2026/800/450'
 WHERE slug = N'thong-bao-tuyen-sinh-2026' AND thumbnail_url IS NULL;
GO

/* --- 1. Sau bai PUBLISHED mau, moi bai mot slug rieng ----------------------- */
IF NOT EXISTS (SELECT 1 FROM dbo.articles WHERE slug = N'le-khai-giang-2026')
INSERT INTO dbo.articles
    (title, slug, summary, content, thumbnail_url, source, category_id, status,
     author_id, reviewer_id, submitted_at, reviewed_at, published_at)
SELECT N'Le khai giang nam hoc 2026-2027',
       N'le-khai-giang-2026',
       N'Truong to chuc le khai giang chao don hon 5000 tan sinh vien.',
       N'Sang nay, truong long trong to chuc le khai giang nam hoc moi voi su tham du cua lanh dao nha truong va dong dao sinh vien.
Trong dien van, hieu truong nhan manh muc tieu nang cao chat luong dao tao va day manh nghien cuu ung dung.',
       N'https://picsum.photos/seed/le-khai-giang-2026/800/450',
       N'https://fu.edu.vn/tin-tuc/khai-giang-2026',
       c.id, N'PUBLISHED', author.id, reviewer.id,
       '2026-09-02T08:00:00', '2026-09-03T10:00:00', '2026-09-05T07:30:00'
FROM dbo.categories c
CROSS JOIN (SELECT id FROM dbo.accounts WHERE email = 'b@fu.edu.vn') author
CROSS JOIN (SELECT id FROM dbo.accounts WHERE email = 'a@fu.edu.vn') reviewer
WHERE c.slug = N'su-kien';
GO

IF NOT EXISTS (SELECT 1 FROM dbo.articles WHERE slug = N'hoi-thao-ai-giao-duc')
INSERT INTO dbo.articles
    (title, slug, summary, content, thumbnail_url, source, category_id, status,
     author_id, reviewer_id, submitted_at, reviewed_at, published_at)
SELECT N'Hoi thao Tri tue nhan tao trong giao duc',
       N'hoi-thao-ai-giao-duc',
       N'Cac chuyen gia chia se cach ung dung AI vao giang day va danh gia.',
       N'Hoi thao thu hut hon 300 giang vien va sinh vien quan tam toi cong nghe giao duc.
Nhieu bao cao trinh bay kinh nghiem dua tro ly AI vao lop hoc va cham bai tu dong.',
       N'https://picsum.photos/seed/hoi-thao-ai-giao-duc/800/450',
       N'https://fu.edu.vn/tin-tuc/hoi-thao-ai',
       c.id, N'PUBLISHED', author.id, reviewer.id,
       '2026-09-10T08:00:00', '2026-09-11T10:00:00', '2026-09-12T07:30:00'
FROM dbo.categories c
CROSS JOIN (SELECT id FROM dbo.accounts WHERE email = 'b@fu.edu.vn') author
CROSS JOIN (SELECT id FROM dbo.accounts WHERE email = 'a@fu.edu.vn') reviewer
WHERE c.slug = N'su-kien';
GO

IF NOT EXISTS (SELECT 1 FROM dbo.articles WHERE slug = N'chung-ket-lap-trinh-2026')
INSERT INTO dbo.articles
    (title, slug, summary, content, thumbnail_url, source, category_id, status,
     author_id, reviewer_id, submitted_at, reviewed_at, published_at)
SELECT N'Chung ket cuoc thi lap trinh sinh vien 2026',
       N'chung-ket-lap-trinh-2026',
       N'Hai muoi doi xuat sac tranh tai o vong chung ket lap trinh toan truong.',
       N'Vong chung ket dien ra cang thang voi cac bai toan thuat toan va xu ly du lieu lon.
Doi vo dich gianh suat thuc tap tai cac doanh nghiep cong nghe doi tac cua truong.',
       N'https://picsum.photos/seed/chung-ket-lap-trinh-2026/800/450',
       N'https://fu.edu.vn/tin-tuc/lap-trinh-2026',
       c.id, N'PUBLISHED', author.id, reviewer.id,
       '2026-09-14T08:00:00', '2026-09-15T10:00:00', '2026-09-16T07:30:00'
FROM dbo.categories c
CROSS JOIN (SELECT id FROM dbo.accounts WHERE email = 'b@fu.edu.vn') author
CROSS JOIN (SELECT id FROM dbo.accounts WHERE email = 'a@fu.edu.vn') reviewer
WHERE c.slug = N'su-kien';
GO

IF NOT EXISTS (SELECT 1 FROM dbo.articles WHERE slug = N'hoc-bong-tai-nang-thu-2026')
INSERT INTO dbo.articles
    (title, slug, summary, content, thumbnail_url, source, category_id, status,
     author_id, reviewer_id, submitted_at, reviewed_at, published_at)
SELECT N'Hoc bong tai nang mua thu 2026',
       N'hoc-bong-tai-nang-thu-2026',
       N'Truong trao hoc bong toan phan cho sinh vien co thanh tich xuat sac.',
       N'Chuong trinh hoc bong tai nang danh cho sinh vien dat diem dau vao cao va co thanh tich hoc tap noi bat.
Ho so nop truc tuyen qua cong thong tin sinh vien truoc cuoi thang.',
       N'https://picsum.photos/seed/hoc-bong-tai-nang-thu-2026/800/450',
       N'https://fu.edu.vn/hoc-bong/tai-nang-2026',
       c.id, N'PUBLISHED', author.id, reviewer.id,
       '2026-09-18T08:00:00', '2026-09-19T10:00:00', '2026-09-20T07:30:00'
FROM dbo.categories c
CROSS JOIN (SELECT id FROM dbo.accounts WHERE email = 'b@fu.edu.vn') author
CROSS JOIN (SELECT id FROM dbo.accounts WHERE email = 'a@fu.edu.vn') reviewer
WHERE c.slug = N'hoc-bong';
GO

IF NOT EXISTS (SELECT 1 FROM dbo.articles WHERE slug = N'trao-doi-sinh-vien-quoc-te')
INSERT INTO dbo.articles
    (title, slug, summary, content, thumbnail_url, source, category_id, status,
     author_id, reviewer_id, submitted_at, reviewed_at, published_at)
SELECT N'Chuong trinh trao doi sinh vien quoc te',
       N'trao-doi-sinh-vien-quoc-te',
       N'Mo dang ky trao doi mot hoc ky tai cac truong doi tac chau A.',
       N'Sinh vien nam hai va nam ba du dieu kien ngoai ngu co the dang ky mot hoc ky trao doi.
Chi phi duoc ho tro mot phan, ket qua hoc tap duoc cong nhan tuong duong.',
       N'https://picsum.photos/seed/trao-doi-sinh-vien-quoc-te/800/450',
       N'https://fu.edu.vn/tuyen-sinh/trao-doi-quoc-te',
       c.id, N'PUBLISHED', author.id, reviewer.id,
       '2026-09-22T08:00:00', '2026-09-23T10:00:00', '2026-09-24T07:30:00'
FROM dbo.categories c
CROSS JOIN (SELECT id FROM dbo.accounts WHERE email = 'b@fu.edu.vn') author
CROSS JOIN (SELECT id FROM dbo.accounts WHERE email = 'a@fu.edu.vn') reviewer
WHERE c.slug = N'tuyen-sinh';
GO

IF NOT EXISTS (SELECT 1 FROM dbo.articles WHERE slug = N'thu-vien-so-ra-mat')
INSERT INTO dbo.articles
    (title, slug, summary, content, thumbnail_url, source, category_id, status,
     author_id, reviewer_id, submitted_at, reviewed_at, published_at)
SELECT N'Thu vien so chinh thuc ra mat',
       N'thu-vien-so-ra-mat',
       N'Sinh vien tra cuu hon mot trieu tai lieu dien tu moi luc moi noi.',
       N'Thu vien so cho phep muon va doc tai lieu truc tuyen, tich hop tim kiem thong minh.
He thong lien ket voi cac co so du lieu hoc thuat quoc te phuc vu nghien cuu.',
       N'https://picsum.photos/seed/thu-vien-so-ra-mat/800/450',
       N'https://fu.edu.vn/tuyen-sinh/thu-vien-so',
       c.id, N'PUBLISHED', author.id, reviewer.id,
       '2026-09-26T08:00:00', '2026-09-27T10:00:00', '2026-09-28T07:30:00'
FROM dbo.categories c
CROSS JOIN (SELECT id FROM dbo.accounts WHERE email = 'b@fu.edu.vn') author
CROSS JOIN (SELECT id FROM dbo.accounts WHERE email = 'a@fu.edu.vn') reviewer
WHERE c.slug = N'tuyen-sinh';
GO
