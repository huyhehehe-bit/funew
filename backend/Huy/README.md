# Huy — Tin tức, dashboard, CORS, dữ liệu mẫu

Chỉ sửa `backend/Huy/` và `frontend/Huy/`. Quy trình commit: [CONTRIBUTING.md](../../CONTRIBUTING.md). Cách nhờ Claude viết code: [PROMPT.md](../../PROMPT.md).

## Đã có sẵn — không phải viết lại

| File | Nội dung |
| --- | --- |
| [entity/NewsArticle.java](src/main/java/com/funews/backend/entity/NewsArticle.java) | Bảng bài viết |
| [repository/NewsArticleRepository.java](src/main/java/com/funews/backend/repository/NewsArticleRepository.java) | Truy vấn bài viết |
| [config/CorsConfig.java](src/main/java/com/funews/backend/config/CorsConfig.java) | **Tích hợp frontend** — origin được phép, cookie qua `allowCredentials` |

`CorsConfig` cho phép `localhost:5173`, `127.0.0.1:5173` (dev) và `localhost:4173`, `127.0.0.1:4173` (preview). Đây là cổng nối sang React — sửa sai là cả nhóm không gọi được API.

## Đã viết

| File | Việc |
| --- | --- |
| [controller/NewsController.java](src/main/java/com/funews/backend/controller/NewsController.java) | `/api/news`, `/api/news/{id}`, `/api/dashboard` |
| [service/NewsService.java](src/main/java/com/funews/backend/service/NewsService.java) | Nghiệp vụ bài viết và thống kê |
| [dto/NewsRequest.java](src/main/java/com/funews/backend/dto/NewsRequest.java), [NewsResponse](src/main/java/com/funews/backend/dto/NewsResponse.java), [DashboardResponse](src/main/java/com/funews/backend/dto/DashboardResponse.java) | Ba DTO của riêng bạn — hợp đồng JSON cho News và Dashboard |
| [test/NewsIntegrationTest.java](src/test/java/com/funews/backend/NewsIntegrationTest.java) | 7 test cho bài viết và dashboard |

Bảng endpoint đầy đủ ở [README chung](../../README.md#hợp-đồng-api). Tài liệu API trực quan: chạy backend rồi mở `http://localhost:8080/swagger-ui.html`, phần của bạn nằm ở nhóm **4. News**.

`NewsIntegrationTest` đăng nhập giả bằng `user(...)` của spring-security-test vì `POST /api/auth/login` của QA chưa có. Khi QA viết xong, đổi sang `login()` của `BackendIntegrationSupport` để kiểm tra cả luồng phiên thật, và bổ sung các test về CORS với phân quyền Admin/Staff.

Quy tắc nghiệp vụ đã cài trong code:

- Server tự xác định tác giả (tài khoản đang đăng nhập) và ngày tạo. Client gửi `createdById` hay `createdDate` thì bỏ qua.
- Sửa bài giữ nguyên tác giả và ngày tạo ban đầu.
- Tiêu đề bắt buộc, `status` chỉ nhận 0 hoặc 1, `categoryId` phải tồn tại.
- Dashboard trả tổng tài khoản, danh mục, bài viết, bài đang hoạt động, và tối đa 5 bài mới nhất.

Dữ liệu mẫu giờ nằm trong `shared/src/main/resources/data-sqlserver.sql` thay vì code Java. Đây là file dùng chung của cả nhóm — sửa phải báo nhóm.

## Phần frontend của bạn — đã xong

`frontend/Huy/`: [useList.js](../../frontend/Huy/useList.js), [useAction.js](../../frontend/Huy/useAction.js), [pages/Dashboard.jsx](../../frontend/Huy/pages/Dashboard.jsx), [pages/News.jsx](../../frontend/Huy/pages/News.jsx).

`useList` và `useAction` được QA và Long đều dùng — đổi giá trị trả về là phải kiểm tra lại tất cả nơi gọi.

## Phối hợp

- Ba DTO `NewsRequest`, `NewsResponse`, `DashboardResponse` giờ **nằm trong folder của bạn** — tự sửa được, không phải nhờ Long nữa. Nhưng đổi tên trường là đổi hợp đồng API, frontend phải sửa theo.
- Bài viết tham chiếu tài khoản (tác giả) và danh mục — giữ hợp đồng `categoryId`, `createdById`, `createdDate`.

## Trước khi push

```powershell
cd backend
.\mvnw.cmd test
```

1. Admin và Staff xem và tạo bài viết; danh mục và tên tác giả hiển thị đúng.
2. Gửi tác giả hoặc ngày giả qua API: server vẫn dùng tài khoản đăng nhập và ngày hiện tại.
3. Sửa bài giữ nguyên tác giả và ngày tạo; xóa bài rồi tải lại không còn.
4. Dashboard đúng tổng tài khoản, danh mục, bài viết, bài hoạt động và tối đa 5 bài mới nhất.
5. Tiêu đề thiếu, `status` sai hoặc `categoryId` không tồn tại bị từ chối.
6. Chưa đăng nhập vẫn đọc được `/api/news`, nhưng `/api/dashboard` trả 401 và tạo bài viết cũng trả 401.
7. CORS chấp nhận `localhost:5173`, từ chối origin lạ; `DROP DATABASE FUNews` rồi chạy lại thì schema và dữ liệu mẫu được tạo lại.
