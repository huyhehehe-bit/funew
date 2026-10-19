# Kiểm thử API bằng Postman

Đáp ứng **Yêu cầu 3** của đề: *dùng Postman test các API lõi (CRUD) — validate logic*.

File collection: [FUNews.postman_collection.json](FUNews.postman_collection.json) — **37 request** chia thành 8 nhóm.

## Cách chạy

1. Khởi động backend trước:

   ```powershell
   cd backend
   .\mvnw.cmd spring-boot:run
   ```

2. Mở Postman → **Import** → chọn `postman/FUNews.postman_collection.json`.

3. Kiểm tra biến `baseUrl` của collection. Mặc định `http://localhost:8080`. Nếu backend chạy cổng khác thì sửa lại: chuột phải collection → **Edit** → tab **Variables**.

4. Chuột phải collection → **Run collection** → **Run FU News Management API**.

Các request **phụ thuộc nhau theo thứ tự** nên phải chạy tuần tự từ trên xuống: đăng nhập trước, tạo danh mục, tạo bài viết trong danh mục đó, kiểm tra validate, rồi dọn dẹp.

## Không cần token

API dùng phiên đăng nhập bằng cookie `JSESSIONID`, không dùng CSRF token cũng không dùng JWT. Đăng nhập một lần ở nhóm **00 – Auth**, Postman tự giữ cookie cho mọi request sau đó.

Không có script chạy ngầm, không có bước lấy token, không phải đặt header gì ngoài `Content-Type`.

## Nội dung 8 nhóm

| Nhóm | Số request | Kiểm tra gì |
| --- | --- | --- |
| **00 – Auth** (QA) | 3 | Đăng nhập Admin, xem tài khoản hiện tại, sai mật khẩu trả 401 |
| **01 – Categories CRUD** (Long) | 4 | Danh sách, tạo, xem, sửa danh mục |
| **02 – News CRUD** (Huy) | 4 | Danh sách, tạo, xem, sửa bài viết |
| **03 – Dashboard** (Huy) | 1 | Bốn chỉ số đếm và tối đa 5 bài mới nhất |
| **04 – Accounts CRUD** (QA) | 5 | Danh sách, tạo, xem, sửa, xóa tài khoản |
| **05 – Validate logic** | 5 | Năm tình huống dữ liệu sai bị từ chối |
| **06 – Dọn dẹp** | 3 | Xóa bài viết và danh mục vừa tạo |
| **07 – Phân quyền và phiên** | 12 | Đăng xuất, đọc công khai, 401, quyền Staff, đăng nhập lại |

## Năm ca validate logic

| Request | Mong đợi | Quy tắc nghiệp vụ |
| --- | --- | --- |
| Tiêu đề rỗng | **400** kèm `errors.title` | `@NotBlank` trong `NewsRequest` |
| `status: 5` | **400** | `@Min(0) @Max(1)` |
| `categoryId: 999999` | **404** | `NewsService` ném `ApiException` |
| Email `ADMIN@fu.edu.vn` | **409** | Trùng email không phân biệt hoa thường |
| Xóa danh mục còn bài viết | **409** | `CategoryService` chặn khi còn tham chiếu |

## Ba ca chứng minh server không tin client

Đây là phần đáng nói nhất khi demo:

1. **Tạo bài viết** — request cố tình gửi kèm `createdById: 99999` và `createdDate: "2000-01-01"`. Test khẳng định response trả về **ID của tài khoản đang đăng nhập** và **ngày hôm nay**, chứng minh server ghi đè chứ không tin dữ liệu client.

2. **Sửa bài viết** — test so sánh `createdById` và `createdDate` trước và sau khi sửa, khẳng định hai trường **không đổi**.

3. **Danh sách tài khoản** — test duyệt từng tài khoản khẳng định **không có** trường `password` hay `passwordHash` nào lọt ra ngoài.

## Phân quyền

Nhóm 07 đăng xuất Admin rồi đăng nhập lại bằng Staff `c@fu.edu.vn` / `123` để kiểm tra:

| Thao tác | Staff |
| --- | --- |
| Đọc danh mục | 200 — được |
| Tạo danh mục | **403** — bị chặn |
| Xem danh sách tài khoản | **403** — bị chặn |
| Tạo bài viết | 201 — được |

## Đọc công khai

Nhóm 07 còn kiểm tra ranh giới giữa đọc và ghi khi **chưa đăng nhập**:

| Thao tác | Mong đợi |
| --- | --- |
| `GET /api/news` | **200** — khách đọc được tin tức |
| `GET /api/categories` | **200** — khách đọc được danh mục |
| `GET /api/dashboard` | **401** — thống kê vẫn phải đăng nhập |
| `POST /api/news` | **401** — mở đọc không có nghĩa là mở ghi |

Đây là ca đáng nói khi demo: mở quyền đọc cho khách vãng lai nhưng ghi vẫn khóa chặt.

## Chạy bằng dòng lệnh

Muốn chạy không cần mở Postman:

```bash
npx --yes newman run postman/FUNews.postman_collection.json --env-var baseUrl=http://localhost:8080
```

Hữu ích khi muốn chụp màn hình kết quả để đưa vào báo cáo.

## Lưu ý

Collection ghi thật vào database: tạo một danh mục, vài bài viết và một tài khoản `postman@fu.edu.vn`. Nhóm **06 – Dọn dẹp** và request cuối nhóm 07 xóa lại những thứ đó. Nếu chạy dở chừng rồi dừng, có thể còn sót dữ liệu thừa — xóa tay trên giao diện, hoặc `DROP DATABASE FUNews` rồi chạy lại backend để script seed lại từ đầu.
