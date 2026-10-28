# FU News — Module 1: Editorial Workflow & Approval Management

Cập nhật 28/10/2026. Tài liệu gốc: **SRS mục 3** (FR-ED-001…004, NFR-ED-001…005, AC-ED-001…004). Nhóm chọn Module 1, **không** làm Module 2 (comment & engagement).

Trạng thái: backend biên dịch xanh, **70/70 test tích hợp đạt**, smoke test **48/48** chạy thật trên SQL Server, frontend `lint` và `build` sạch.

## 1. Đối chiếu từng yêu cầu của SRS

| Yêu cầu | Trạng thái | Nằm ở đâu |
| --- | --- | --- |
| **FR-ED-001** Máy trạng thái 6 bước | Đủ | `ArticleStatus` + `ck_articles_status`; mọi chuyển trạng thái đi qua một hàm riêng trong `NewsService`. `PUT /api/news/{id}` từ chối mọi request đổi trạng thái |
| **FR-ED-002** Trả bài kèm lý do bắt buộc | Đủ | `POST /{id}/request-changes`, ràng buộc ở DTO, service **và** `ck_audit_note_required` dưới database |
| FR-ED-002 *inline revision requests* | **Cố ý không làm** | SRS viết "notes **or** inline revision requests" — nhóm chọn vế notes |
| **FR-ED-003** Hẹn giờ đăng tự động | Đủ | `scheduledPublishAt` + `ScheduledPublisher` chạy mỗi phút |
| **FR-ED-004** Nhật ký kiểm toán | Đủ | `article_audit_logs`, ghi actor + thời điểm + trạng thái trước/sau cho mọi bước; xem bằng `GET /{id}/audit` và nút Lịch sử |
| **NFR-ED-001** Phản hồi < 2 giây | Đủ (chưa đo tải) | `ix_articles_review_queue`, `ix_articles_author`, `ix_articles_public`, `ix_notifications_inbox` |
| **NFR-ED-002** Sẵn sàng 99.5% | Ngoài phạm vi đồ án | — |
| **NFR-ED-003** RBAC cho publish/override | Đủ | `SecurityConfig` chặn theo URL + `NewsService` kiểm theo tác giả; test có ca Contributor và Reader bị 403 |
| **NFR-ED-004** Nhật ký bất biến | Đủ | `@Immutable` ở tầng Java **và** trigger `trg_audit_logs_immutable` ở database (thử UPDATE/DELETE đều lỗi 50001) |
| **NFR-ED-005** Duyệt trong 3 cú bấm | Đủ | Nút Duyệt / Trả lại nằm ngay trên dòng bài ở màn hình Bài viết |
| **AC-ED-001** Nộp xong thì khóa sửa | Đủ | Tác giả PUT bài PENDING_REVIEW → 409 |
| **AC-ED-002** Tác giả được thông báo kèm lý do | Đủ | Bảng `notifications` + chuông trên thanh tiêu đề |
| **AC-ED-003** Tới giờ thì tự đăng | Đủ | `publishDueArticles()` đăng dưới tên `system@fu.edu.vn` |
| **AC-ED-004** Mọi lần đổi trạng thái đều vào nhật ký | Đủ | Không có đường nào đổi trạng thái mà không ghi log |
| **Mục 3.2** Administrator cấu hình quy tắc biên tập | Đủ | Bảng `editorial_rules` + màn hình **Rules**, ba quy tắc đều có hiệu lực thật |

Ba quy tắc trong màn hình Rules (bản 1.3):

| Quy tắc | Backend áp dụng ở đâu |
| --- | --- |
| Tự động đăng bài khi tới giờ hẹn | tắt thì `publishDueArticles()` trả về 0, bài nằm chờ biên tập bấm Đăng ngay |
| Duyệt bài phải ghi chú | bật thì `approve` thiếu note trả 400 |
| Số ký tự tóm tắt tối thiểu | chặn ngay lúc nộp bài và lúc tạo bài ở trạng thái PENDING_REVIEW |

## 2. Chạy dự án

**Một lần cho mỗi máy.** Tạo file riêng chứa mật khẩu — `.gitignore` đã chặn, không bao giờ lên git:

```bash
cd backend && cp application.properties.private.example application.properties.private
```

Trong đó điền `spring.datasource.password` của mình và `app.jwt.secret` (Base64 của khóa ngẫu nhiên ít nhất 32 byte, cách sinh xem `backend/QA/README.md`). **Thiếu `app.jwt.secret` là backend không khởi động.**

**Database** tên `FUNews_M1`, 10 bảng. Database `FUNews` cũ chỉ còn 3 bảng của bản trước, đừng trỏ backend vào nó.

```bash
sqlcmd -S localhost -U sa -P <mat khau> -I -Q "IF DB_ID('FUNews_M1') IS NULL CREATE DATABASE FUNews_M1"
```

```bash
sqlcmd -S localhost -U sa -P <mat khau> -I -d FUNews_M1 -i database/module1_schema.sql
```

Tham số `-I` bắt buộc: bảng có filtered index và cột tính toán, thiếu nó thì `QUOTED_IDENTIFIER` tắt và mọi lệnh ghi đều lỗi. Script chạy lại nhiều lần không lỗi và không nhân đôi dữ liệu; cuối script in bảng đếm dòng và các dòng OK/THIẾU để biết đã chạy trọn chưa.

**Chạy:**

```bash
cd backend && ./mvnw.cmd spring-boot:run
```

```bash
cd frontend && npm.cmd run dev
```

Tài khoản mẫu: `Admin`/`Admin` (ADMIN), `a@fu.edu.vn`/`123` (EDITOR), `b@` và `c@`/`123` (CONTRIBUTOR), `reader@`/`123` (READER). Riêng `system@fu.edu.vn` không đăng nhập được — nó chỉ để tác vụ hẹn giờ đứng tên trong nhật ký.

## 3. Những điểm thiết kế nên biết khi bảo vệ

- **Xác thực bằng JWT Bearer, không có phiên.** Token không mang theo vai trò: mỗi request `JwtAuthenticationFilter` đọc lại tài khoản từ database, nên hạ quyền hay khóa tài khoản có hiệu lực ngay với token đã phát. Đăng xuất thu hồi token đó.
- **Xóa mềm toàn bộ.** Không có lệnh `DELETE` nào trong service bài viết. Mọi truy vấn đều lọc `is_deleted = 0`.
- **Hiển thị theo vai trò lọc ở backend, không ở trình duyệt.** Khách và READER chỉ thấy PUBLISHED; CONTRIBUTOR thấy thêm bài của chính mình; EDITOR và ADMIN thấy hết. Bản nháp của người khác trả **404** chứ không 403 — không tiết lộ là bài đó tồn tại.
- **`status` trong `NewsRequest` chỉ nhận DRAFT hoặc PENDING_REVIEW**, và sửa bài không đổi được trạng thái. Nếu không chặn, Contributor chỉ cần PUT một chữ là tự đăng bài, đi vòng qua cả luồng duyệt.
- **Cột `articles.version`** làm optimistic locking: hai người cùng sửa một bài thì người ghi sau nhận 409 chứ không ghi đè âm thầm.
- **Nhật ký bất biến ở tầng database.** Muốn xóa một dòng (dọn database thử nghiệm) phải gỡ trigger ra tay — cố ý bắt làm ba bước:

```bash
sqlcmd -S localhost -U sa -P <mat khau> -I -d FUNews_M1 -Q "DISABLE TRIGGER dbo.trg_audit_logs_immutable ON dbo.article_audit_logs"
```

## 4. Phân công file

Mỗi người vẫn sở hữu folder của mình. Những file dùng chung hoặc thuộc folder người khác mà đợt này phải sửa:

| File | Chủ | Vì sao phải sửa |
| --- | --- | --- |
| `backend/shared/.../application.properties` | chung | trỏ sang `FUNews_M1`, tắt `spring.sql.init`, bỏ mật khẩu khỏi file lên git |
| `backend/shared/.../data-h2.sql` | chung | dữ liệu mẫu cho test theo schema mới |
| `backend/shared/.../BackendIntegrationSupport.java` | chung | thêm `token()`/`bearer()` và khóa JWT cho test |
| `backend/QA/.../SecurityConfig.java` | QA | phân quyền cho các endpoint duyệt bài, hộp thư, quy tắc |
| `backend/QA/.../JwtIntegrationSupport.java` | QA | `@TestConfiguration` để fixture không lọt vào context thật |
| `backend/Long/.../CategoryService.java`, `CategoryRepository.java` | Long | theo model mới (boolean, slug, lọc theo trạng thái) |
| `frontend/QA/App.jsx` | QA | truyền `user` xuống News, thêm menu Rules |
| `frontend/Long/components/Header.jsx` | Long | gắn chuông thông báo |
| `frontend/Long/pages/Categories.jsx` | Long | `isActive` thành boolean, sửa bộ lọc theo trạng thái |

## 5. Cố ý không làm

- **Inline revision requests** (vế sau của FR-ED-002) — SRS cho chọn một trong hai, nhóm chọn notes bắt buộc.
- **Module 2** (comment, moderation, reaction) — ngoài phạm vi đã chọn. Hai bảng `article_ratings` và cột `view_count` có sẵn trong schema nhưng **chưa có API**, đó là phần chấm sao 1-5 làm thêm, không tính vào Module 1.
- **Gửi email** — AC-ED-002 chỉ nói "notified", nhóm làm thông báo trong ứng dụng.
- **Lọc danh mục ở phía server** (`GET /api/categories?status=`) đã có và có test, nhưng frontend đang lọc trên mảng đã tải. Dữ liệu còn ít nên chưa cần đổi.
