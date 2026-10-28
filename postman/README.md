# Kiểm thử API bằng Postman

[FUNews.postman_collection.json](FUNews.postman_collection.json) — **66 request, 11 nhóm, 100 assertion**. Phủ CRUD, validate logic, phân quyền và toàn bộ luồng duyệt bài của Module 1.

## Chạy

Cần backend đang chạy ở `localhost:8080` và database đã chạy `database/module1_schema.sql`.

Bằng dòng lệnh, không cần cài gì:

```bash
npx --yes newman run postman/FUNews.postman_collection.json
```

Hoặc mở Postman: **Import** → chọn file JSON → chọn collection → **Run**.

Kết quả mong đợi: **66/66 request, 100/100 assertion xanh**. Chạy lại nhiều lần vẫn xanh — nhóm cuối cùng dọn sạch dữ liệu mà bộ test tạo ra.

## Quan trọng: chạy tuần tự

Bộ này **không chạy lẻ từng request được**. Các request đầu đăng nhập rồi lưu `accessToken` và id vào biến collection cho các request sau dùng:

| Biến | Lấy từ đâu |
| --- | --- |
| `adminToken`, `editorToken`, `contributorToken`, `readerToken` | 4 request đăng nhập đầu tiên |
| `categoryId` | tạo danh mục |
| `newsId`, `newsVersion` | tạo bài viết, cập nhật lại sau mỗi lần ghi |
| `notificationId` | hộp thư của tác giả |
| `accountId` | tạo tài khoản |

Muốn đổi địa chỉ backend thì sửa biến `baseUrl` (mặc định `http://localhost:8080`).

## Có gì trong 11 nhóm

| Nhóm | Kiểm gì |
| --- | --- |
| **00 - Auth và JWT** | Đăng nhập 4 vai trò, `/auth/me`, sai mật khẩu → 401, token rác → 401, không token → 401 |
| **01 - Danh mục** | CRUD, slug tự sinh, lọc `?status=true\|false` |
| **02 - Bài viết CRUD** | Tạo nháp, sửa, và **hiển thị theo vai trò**: khách chỉ thấy bài đã đăng, tác giả thấy bản nháp của mình, độc giả không thấy |
| **03 - Luồng duyệt bài** | Nộp → khóa sửa (409) → hàng đợi → trả lại thiếu lý do (400) → trả lại kèm lý do → sửa → nộp lại → duyệt → đăng → nhật ký → gỡ |
| **04 - Thông báo** | Tác giả nhận được thông báo kèm lý do, đếm chưa đọc, đánh dấu đã đọc |
| **05 - Quy tắc biên tập** | Cộng tác viên không sửa được (403), Admin sửa được, ngưỡng vượt giới hạn (400) |
| **06 - Dashboard** | Có `publishedCount`, `pendingReviewCount`, tối đa 5 bài gần nhất |
| **07 - Tài khoản** | CRUD tài khoản, danh sách vai trò, không bao giờ lộ `passwordHash` |
| **08 - Validate logic** | Tạo bài thẳng lên `PUBLISHED` → 400, tiêu đề rỗng → 400, danh mục lạ → 404, email sai → 400, email trùng → 409, xóa danh mục đang có bài → 409 |
| **09 - Phân quyền** | Cộng tác viên xem tài khoản/hàng đợi → 403, độc giả duyệt bài → 403, khách ghi → 401 |
| **10 - Dọn dẹp** | Xóa mềm bài, xóa tài khoản và danh mục thử, đăng xuất rồi dùng lại token cũ → 401 |

## Xác thực đã đổi sang JWT

Bản trước giữ phiên bằng cookie `JSESSIONID` nên Postman tự gửi kèm. Giờ **không còn cookie nào**: mỗi request mang header `Authorization: Bearer {{...Token}}`, và `POST /api/auth/logout` thu hồi token ngay lập tức — nhóm 10 kiểm đúng điều đó.

## Vài khẳng định đáng chú ý

Không chỉ kiểm mã trạng thái, bộ này còn kiểm cả hành vi:

- **Server tự gán tác giả**: request tạo bài cố tình không gửi `authorId`, test khẳng định `authorId` bằng id của người đang đăng nhập.
- **Không ai tự đăng bài**: tạo bài với `status: "PUBLISHED"` phải bị 400 — nếu không chặn thì cộng tác viên chỉ cần sửa một chữ là đi vòng qua cả luồng duyệt.
- **Lý do trả bài là bắt buộc**: gửi `note` toàn khoảng trắng vẫn phải bị 400.
- **Nhật ký ghi đủ**: sau khi đăng, nhật ký phải có cả `SUBMIT`, `REQUEST_CHANGES` và `PUBLISH`.
- **Xóa là xóa mềm**: xóa xong `GET` trả 404, nhưng dòng vẫn nằm trong database.
