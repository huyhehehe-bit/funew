# Note: chuyển sang schema Module 1

Ngày 25/10/2026 — nhánh `huy`. Đọc hết file này trước khi merge, vì nó đụng vào folder của cả ba người.

Tài liệu gốc: **SRS mục 3 — Module 1: Editorial Workflow & Approval Management** (FR-ED-001…004, NFR-ED-001…005, AC-ED-001…004). Nhóm chọn Module 1, không làm Module 2 (comment & engagement).

## 1. Chốt trước

**Entity và DTO trong lần commit này là bản chuẩn. QA và Long merge theo, không merge ngược lại.** Lý do: cả 9 bảng của Module 1 là một schema liền mạch, `accounts` và `categories` bị `articles` tham chiếu nên không tách ra sửa riêng từng folder được.

Cụ thể những file thuộc folder người khác mà lần này bị sửa:

| File | Chủ sở hữu | Đổi gì |
| --- | --- | --- |
| `backend/QA/.../entity/Account.java` | QA | `name` → `fullName`, `int role` → `@ManyToOne Role`, thêm xóa mềm + timestamp |
| `backend/QA/.../entity/Role.java` | QA | **mới** |
| `backend/QA/.../dto/*.java` (4 file) | QA | `name` → `fullName`, `role` → `roleId` + `roleCode` |
| `backend/QA/.../dto/RoleResponse.java` | QA | **mới** |
| `backend/Long/.../entity/Category.java` | Long | thêm `slug`, `displayOrder`, xóa mềm, `isActive` thành boolean |
| `backend/Long/.../dto/*.java` (2 file) | Long | thêm `slug`, `displayOrder`, `isActive` boolean |

## 2. Database

Schema thật nằm ở [database/module1_schema.sql](database/module1_schema.sql) (bản 1.1), **không** còn ở `schema-sqlserver.sql`.

Database tên **`FUNews_M1`**, 9 bảng. Database `FUNews` cũ chỉ còn 3 bảng (`accounts`, `categories`, `news_articles`) và không khớp entity nữa — đừng trỏ backend vào nó.

Làm một lần trên máy mình:

```bash
sqlcmd -S localhost -E -Q "IF DB_ID('FUNews_M1') IS NULL CREATE DATABASE FUNews_M1"
```

```bash
sqlcmd -S localhost -E -d FUNews_M1 -i database/module1_schema.sql
```

Script chạy lại nhiều lần không lỗi và không nhân đôi dữ liệu. Cuối script in ra bảng đếm dòng và một dòng OK/THIẾU cho bốn thứ của bản 1.1 — nhìn dòng đó để biết đã chạy trọn chưa.

Bản 1.1 thêm sáu thứ so với 1.0, mỗi thứ vá một lỗ hổng so với SRS:

| Thêm | Vì |
| --- | --- |
| Tài khoản `system@fu.edu.vn` | `article_audit_logs.actor_id` là NOT NULL, mà AC-ED-003 nói bài tự đăng khi tới giờ hẹn — tác vụ nền phải có tài khoản để đứng tên. Tài khoản này `is_active = 0` và `password_hash` không phải hash BCrypt hợp lệ nên không đăng nhập được |
| Bảng `notifications` | AC-ED-002 đòi tác giả **được thông báo** kèm lý do, trước đây lý do chỉ nằm trong nhật ký |
| Cột `articles.version` | optimistic locking, chống tác vụ nền và biên tập viên ghi đè nhau |
| Siết `ck_audit_note_required` | `NOT NULL` một mình vẫn cho lọt chuỗi rỗng, tức là trả bài mà không viết lý do |
| Trigger `trg_audit_logs_immutable` | NFR-ED-004 nói log bất biến; `@Immutable` chỉ chặn tầng Java, mở SSMS vẫn sửa được |
| `ix_articles_review_queue`, `ix_articles_author` | NFR-ED-001 (phản hồi dưới 2 giây) cho hàng đợi duyệt bài và màn hình "bài của tôi" |

Muốn xóa một dòng nhật ký (dọn database thử nghiệm) phải gỡ trigger ra tay — cố ý bắt làm ba bước:

```bash
sqlcmd -S localhost -E -d FUNews_M1 -Q "DISABLE TRIGGER dbo.trg_audit_logs_immutable ON dbo.article_audit_logs"
```

## 3. Ba file dùng chung đã sửa

**`backend/shared/src/main/resources/application.properties`**
- `databaseName=FUNews` → `FUNews_M1`.
- `spring.sql.init.mode=always` → `never`. Để `always` là backend **không khởi động được**: Spring chạy lại `schema-sqlserver.sql` và `data-sqlserver.sql` — hai file của bản cũ, còn `INSERT` vào cột `accounts.name` và `accounts.role` đã bị đổi tên — rồi báo `Invalid column name` ngay lúc boot.
- Bỏ hai dòng `spring.datasource.username=sa` và `password=sa`. Mật khẩu không nằm trong file lên git nữa (xem mục 4).

**`backend/shared/src/test/java/.../BackendIntegrationSupport.java`**
- Thêm một dòng `"spring.sql.init.mode=always"` vào khối `@SpringBootTest(properties = {...})`, để `mode=never` ở trên không làm test mất dữ liệu mẫu. Chỉ thêm property, không sửa logic test.

**`backend/shared/src/main/resources/data-h2.sql`**
- Viết lại theo schema mới, nội dung khớp đúng phần dữ liệu mẫu của `module1_schema.sql`: 4 vai trò, 6 tài khoản (5 người + SYSTEM), 3 danh mục, 4 thẻ, 3 bài viết, 5 dòng nhật ký, 1 đánh giá, 1 thông báo.
- Ba chỗ H2 khác SQL Server, ghi rõ trong đầu file: H2 **không** có ràng buộc CHECK nào (Hibernate sinh bảng từ entity), `rating_avg` ở H2 chỉ là cột thường nên luôn NULL (đừng assert vào nó), và H2 không có DEFAULT nên mọi cột NOT NULL phải ghi giá trị tường minh.

`schema-sqlserver.sql` và `data-sqlserver.sql` giờ là **file chết** — `mode=never` nên Spring không chạy chúng nữa. Để lại cũng không sao, nhưng dễ gây nhầm; nhóm quyết định xóa thì xóa cả hai cùng lúc.

## 4. File riêng: `.private`

Mật khẩu database không nằm trong file nào lên git. Mỗi người tự tạo một lần:

```bash
cd backend && cp application.properties.private.example application.properties.private
```

Rồi điền mật khẩu thật vào `application.properties.private`. `application.properties` nạp nó qua `spring.config.import`, nên code không cần biết gì thêm.

`.gitignore` đã siết để không ai commit nhầm được:

```text
application.properties.private
*.properties.private
*.private
.private/
!*.private.example
```

Dòng cuối là cố ý: file `.example` **phải** lên git, nó chỉ chứa giá trị giả.

Đã kiểm tra `git check-ignore`: `backend/application.properties.private` bị chặn, `backend/application.properties.private.example` không bị chặn, và file thật chưa từng có trong history.

## 5. Việc còn lại — chia theo chủ sở hữu file

`mvnw compile` hiện **21 lỗi**, nằm gọn trong 4 file, không file nào thuộc tầng entity/DTO:

| Người | File | Việc |
| --- | --- | --- |
| QA | `AccountService` (10 lỗi), `AccountPrincipal` (1), `AccountRepository` | `role` không còn là `int` mà là entity `Role`; `getName` → `getFullName`; `countByRole(int)` → `countByRoleId(Long)`; `getAuthorities()` phải map 4 role code thay vì `role == 1 ? ADMIN : STAFF` |
| Long | `CategoryService` (2 lỗi) | `getIsActive/setIsActive(int)` → `isActive/setActive(boolean)` |
| Huy | `NewsService` (8 lỗi), `NewsArticleRepository` | `headline` → `summary`, `tags` → `Set<Tag>`, `status` → `ArticleStatus`, `createdBy` → `author`, `createdDate` → `createdAt` |

Hai bẫy sẽ nổ lúc chạy chứ không lúc biên dịch:

- `findTop5ByOrderByCreatedDateDescIdDesc` và `existsByCreatedById` — Spring Data dựng câu query lúc khởi động, không tìm thấy property là **context không lên nổi**. Đổi thành `...CreatedAtDesc...` và `existsByAuthorId`.
- `Account.role` đang để `LAZY`, nhưng `SecurityConfig.userDetailsService` và `RefreshAccountFilter` tạo `AccountPrincipal` **ngoài transaction** → gọi `account.getRole().getCode()` sẽ ném `LazyInitializationException` ở mọi request. Chuyển sang `EAGER` (bảng tra cứu 4 dòng) hoặc dùng `@EntityGraph`.

Ngoài ra còn ba nhóm việc chưa ai nhận:

1. **Luồng duyệt bài thật** — submit / request changes / approve / schedule / publish / archive, ghi nhật ký mỗi lần, tạo notification, và tác vụ `@Scheduled` cho AC-ED-003. Hiện `NewsRequest.status` cho client tự đặt trạng thái, tức là Contributor PUT thẳng `PUBLISHED` được — phải chặn.
2. **Xóa mềm** — entity đã có cột, nhưng service vẫn gọi `repository.delete()` (DELETE thật) và chưa query nào lọc `is_deleted = 0`.
3. **Frontend + test + tài liệu** — `News.jsx`, `Dashboard.jsx`, `Categories.jsx`, `Users.jsx`, `Header.jsx` còn đọc `headline`, `createdDate`, `status === 1`, `isActive === 1`, `name`, `role`; các file test còn gửi payload cũ; `README.md` và `OpenApiConfig` còn ghi "role: 1 = Admin, 2 = Staff".

## 6. Đã kiểm chứng tới đâu

Đã chạy thật:

- `module1_schema.sql` chạy hai lần liên tiếp trên một database tạm: không lỗi, không nhân đôi. Sau đó chạy trên `FUNews_M1`: dữ liệu cũ nguyên vẹn (`articles` 3, `article_audit_logs` 7, `article_ratings` 1), chỉ thêm tài khoản SYSTEM và bảng `notifications`.
- Trigger nhật ký: thử `UPDATE` và `DELETE` đều bị chặn, lỗi 50001. Trả bài với lý do toàn khoảng trắng bị chặn, lỗi 547. Thông báo loại lạ bị chặn, lỗi 547. Ghi nhật ký bằng tài khoản SYSTEM thì được.
- Optimistic locking: `UPDATE ... WHERE version = <sai>` ăn 0 dòng, `version` đúng ăn 1 dòng.
- Hibernate dựng được metadata từ cả 10 entity và tạo schema lên H2 — tức là `@Generated` trên `rating_avg`, `@Version`, `@ManyToMany` của tags và `@Immutable` đều hợp lệ.
- `data-h2.sql` chạy lên đúng schema Hibernate sinh ra: 9 bảng với số dòng khớp `module1_schema.sql`, quan hệ đúng (thông báo gửi về `b@` là tác giả bài học bổng, 5 dòng nhật ký chia 2/1/2 cho ba bài).

Chưa kiểm chứng được, vì phải đợi tầng logic biên dịch xong:

- Backend khởi động thật và nối được `FUNews_M1` (Hibernate đối chiếu entity với bảng SQL Server).
- `mvnw test` xanh — ngoài `data-h2.sql` đã sửa, các file test còn gửi payload theo hợp đồng API cũ nên vẫn sẽ đỏ.
