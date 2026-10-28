# FU News

Hệ thống quản lý tin tức FU News. Một repo, hai folder: [backend/](backend) (Java 17 + Spring Boot 3.5.16 + SQL Server 2022) và [frontend/](frontend) (React 19 + Vite).

Backend chạy ở `http://localhost:8080`, frontend ở `http://localhost:5173`.

## Đối chiếu với đề bài

Môn **SBA301 — Integrate Single Page Application with Spring Boot**, Fall 2026.

| Yêu cầu của đề | Trạng thái |
| --- | --- |
| **1.** Thống nhất, thiết kế làm mịn Frontend | Xong — 6 màn hình React, `npm run build` và `npm run lint` đều sạch |
| **2.** Xây backend dựa vào DB ERD của Assignment 1 | Xong — 10 bảng theo [schema Module 1](database/module1_schema.sql) |
| **2.** REST API phục vụ frontend | Xong — 35 operation, xác thực bằng JWT |
| **2.** **Tạo documentation cho API — Swagger** | Xong — [/swagger-ui.html](#tài-liệu-api--swagger) |
| **3.** **Dùng Postman test API lõi (CRUD), validate logic** | Xong — [66 request, 100 assertion](postman/README.md), `newman` xanh |
| **4.** Tích hợp frontend + backend | Xong — đăng nhập, CRUD, dashboard đều chạy |
| Mỗi member đảm nhiệm 1–2 màn hình | Xong — mỗi người 2 màn hình, cả frontend lẫn backend |
| Quản lý mã nguồn bằng GitHub | Xong |

Ngoài ra còn **70 test tích hợp JUnit** chạy bằng `mvnw test`, không nằm trong yêu cầu nhưng là bằng chứng tốt khi demo.

Phần nâng cao đã làm: **Module 1 — Editorial Workflow & Approval Management**. Đối chiếu từng yêu cầu FR/NFR/AC của SRS nằm ở [NOTE-MODULE1.md](NOTE-MODULE1.md).

## Tài liệu API — Swagger

Khởi động backend rồi mở:

```text
http://localhost:8080/swagger-ui.html
```

File OpenAPI dạng JSON ở `http://localhost:8080/v3/api-docs` — nộp kèm báo cáo được.

Trang này liệt kê đủ 35 operation chia theo 6 nhóm, mỗi nhóm ghi rõ ai phụ trách. Phần mô tả đầu trang hướng dẫn luôn cách đăng nhập để bấm **Try it out**, kèm bảng tài khoản thử nghiệm và bảng mã lỗi.

Thử trực tiếp trên Swagger được, không cần Postman. Chỉ một bước:

1. Chạy `POST /api/auth/login` với body `{ "username": "Admin", "password": "Admin" }`.
2. Chép `accessToken` trong kết quả, bấm **Authorize** ở góc phải rồi dán vào.

Token sống 15 phút. Không có cookie phiên nào: mọi request đều phải mang header `Authorization: Bearer <token>`.

Cấu hình nằm ở [shared/.../OpenApiConfig.java](backend/shared/src/main/java/com/funews/backend/config/OpenApiConfig.java).

## Kiểm thử bằng Postman

[postman/FUNews.postman_collection.json](postman/FUNews.postman_collection.json) — 66 request, 11 nhóm, 100 assertion. Hướng dẫn đầy đủ ở [postman/README.md](postman/README.md).

```bash
npx --yes newman run postman/FUNews.postman_collection.json
```

## Ba người, ba folder

Mỗi người sở hữu một folder trong `backend/` và một folder trong `frontend/`. **Chỉ sửa folder của mình** — nhờ vậy ba người commit song song mà không bao giờ conflict.

| Người | Màn hình | Folder |
| --- | --- | --- |
| QA | Login, Users | `backend/QA/`, `frontend/QA/` — [hướng dẫn](backend/QA/README.md) |
| Long | Categories, Settings | `backend/Long/`, `frontend/Long/` — [hướng dẫn](backend/Long/README.md) |
| Huy | News, Dashboard | `backend/Huy/`, `frontend/Huy/` — [hướng dẫn](backend/Huy/README.md) |

Nhóm dùng ba tầng nhánh: `main` là bản ổn định, `main2` là nơi cả ba đẩy code vào và ghép với nhau, còn `qa` / `long` / `huy` là nhánh làm việc riêng. Chỉ gộp `main2` sang `main` khi không còn xung đột và test chạy xanh.

Quy trình commit đầy đủ: [CONTRIBUTING.md](CONTRIBUTING.md). Cách nhờ Claude viết code phần của mình: [PROMPT.md](PROMPT.md).

## Cấu trúc

```text
FUNews/
├── backend/
│   ├── pom.xml                  # DUNG CHUNG - doi phai bao ca nhom
│   ├── mvnw, mvnw.cmd, .mvn/    # Maven Wrapper, khong can cai Maven
│   ├── shared/                  # DUNG CHUNG
│   │   ├── src/main/java/       #   BackendApplication.java
│   │   ├── src/main/resources/  #   application.properties
│   │   └── src/test/java/       #   BackendIntegrationSupport.java
│   ├── QA/src/main/java/        # Account, AccountRepository, SecurityConfig,
│   │                            # AccountPrincipal, RefreshAccountFilter
│   │                            # + AuthController, AccountController, AccountService (trong)
│   ├── Long/src/main/java/      # Category, CategoryRepository, dto/ (2 DTO),
│   │                            # ApiException, ApiExceptionHandler
│   │                            # + CategoryController, CategoryService (trong)
│   └── Huy/src/main/java/       # NewsArticle, NewsArticleRepository, CorsConfig
│                                # + NewsController, NewsService, dto/ (3 DTO)
└── frontend/
    ├── package.json             # DUNG CHUNG
    ├── vite.config.js, eslint.config.js, index.html, public/
    ├── src/main.jsx             # DUNG CHUNG - diem khoi dong, khong ai can sua nua
    ├── QA/                      # App.jsx, api.js, data.js, pages/Login, pages/Users
    ├── Long/                    # index.css, components/, pages/Categories, pages/Settings
    └── Huy/                     # useList.js, useAction.js, pages/Dashboard, pages/News
```

Ba folder `QA`, `Long`, `Huy` **chạy chung một ứng dụng**, không chạy riêng từng folder. Bên backend, `pom.xml` gom source của cả ba vào một bản build qua `build-helper-maven-plugin`. Bên frontend, `src/main.jsx` nạp `../QA/App.jsx`, rồi `App.jsx` nạp màn hình của Long và Huy.

Package Java vẫn là `com.funews.backend` cho cả ba folder — folder chỉ chia **quyền sở hữu file**, không chia package.

## Chạy backend

Cần **JDK 17 trở lên**. Kiểm tra bằng `java -version`.

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Nếu terminal chưa nhận JDK, đặt `JAVA_HOME` trước:

```powershell
$env:JAVA_HOME = 'C:\Users\dhnga\.jdks\ms-21.0.11'
```

### Chuẩn bị database — làm một lần

Cần **SQL Server 2022** đang chạy và bật **SQL Server Authentication**.

**1. Tạo database** tên `FUNews_M1`:

```powershell
sqlcmd -S localhost -U sa -P <mat khau> -I -Q "IF DB_ID('FUNews_M1') IS NULL CREATE DATABASE FUNews_M1"
```

**2. Chạy schema.** Backend **không** tự tạo bảng; schema nằm ở [database/module1_schema.sql](database/module1_schema.sql):

```powershell
sqlcmd -S localhost -U sa -P <mat khau> -I -d FUNews_M1 -i database/module1_schema.sql
```

Tham số `-I` bắt buộc — bảng có filtered index và cột tính toán, thiếu nó thì `QUOTED_IDENTIFIER` tắt và mọi lệnh ghi đều lỗi. Script **chạy lại nhiều lần không lỗi và không nhân đôi dữ liệu**; cuối script in bảng đếm dòng và các dòng OK/THIẾU để biết đã chạy trọn chưa. Muốn làm lại từ đầu thì `DROP DATABASE FUNews_M1` rồi tạo lại.

**3. Khai tài khoản database và khóa JWT.** Copy `backend/application.properties.private.example` thành `backend/application.properties.private` rồi điền:

```properties
spring.datasource.username=sa
spring.datasource.password=<mat khau that cua ban>
app.jwt.secret=<Base64 cua khoa ngau nhien it nhat 32 byte>
```

File `application.properties.private` **đã nằm trong `.gitignore`** — mật khẩu không bao giờ lên git. Mỗi người trong nhóm tự tạo file này trên máy mình. **Thiếu `app.jwt.secret` là backend không khởi động**; cách sinh khóa xem [backend/QA/README.md](backend/QA/README.md).

Database `FUNews` cũ (3 bảng của bản trước) không còn khớp entity — đừng trỏ backend vào nó.

### Mở bằng IntelliJ IDEA

1. **File → Open**, chọn `backend/pom.xml`, chọn **Open as Project**.
2. **File → Project Structure → Project SDK**: chọn **JDK 17** trở lên, Language level 17.
3. **Maven → Reload All Maven Projects** để IntelliJ nhận ba folder source QA, Long, Huy.
4. Mở `backend/shared/src/main/java/com/funews/backend/BackendApplication.java`, bấm Run cạnh `main()`.
5. Trong Run Configuration, đặt **Working directory** là thư mục `backend`.

Nếu IntelliJ báo `configured for JVM target 5`: **Settings → Build, Execution, Deployment → Compiler → Java Compiler**, đặt **Project bytecode version = 17**, rồi **Build → Rebuild Project**.

## Chạy frontend

Cần **Node.js 20 trở lên**.

```powershell
cd frontend
npm.cmd install
npm.cmd run dev
```

Mở http://localhost:5173. Vite cố định cổng 5173 để khớp CORS; nếu cổng bị chiếm thì dừng tiến trình cũ chứ đừng đổi cổng.

Frontend mặc định gọi `http://localhost:8080`. Muốn đổi, copy `.env.example` thành `.env.local`, sửa `VITE_API_BASE_URL`, rồi khởi động lại Vite. URL này **không** kèm `/api`.

Dùng cùng hostname cho cả hai bên. Nếu mở frontend bằng `127.0.0.1` thì đặt `VITE_API_BASE_URL=http://127.0.0.1:8080`, nếu không cookie `SameSite=lax` sẽ không được gửi.

## Kiểm tra

```powershell
# Tai backend
.\mvnw.cmd test        # chay test tich hop
.\mvnw.cmd verify      # test + dong goi JAR vao target/

# Tai frontend
npm.cmd run lint
npm.cmd run build
```

Test backend dùng **H2 in-memory** chứ không nối vào SQL Server, nên `mvnw test` chạy được kể cả khi chưa bật dịch vụ SQL Server. Hibernate tạo bảng từ entity rồi `data-h2.sql` đổ dữ liệu giống hệt `data-sqlserver.sql`.

## Hợp đồng API

Đây là phần **đã chốt**. Mỗi người giữ DTO của mình trong `dto/` thuộc folder riêng: QA có `AccountRequest`, `AccountResponse`, `AccountLookup`, `ProfileRequest`, `RoleResponse`, `AuthResponse`; Long có `CategoryRequest`, `CategoryResponse`; Huy có `NewsRequest`, `NewsResponse`, `ReviewRequest`, `DashboardResponse`, `NotificationResponse`, `ArticleAuditLogResponse`, `EditorialRulesRequest`, `EditorialRulesResponse`, `TagResponse`, `RatingRequest`, `RatingResponse`. Cả ba cùng nằm trong package `com.funews.backend.dto`.

Phần lớn API yêu cầu đăng nhập. Mở công khai cho khách vãng lai: `POST /api/auth/login`, và **đọc** tin tức `GET /api/news`, `GET /api/news/{id}`, danh mục `GET /api/categories`, `GET /api/categories/{id}`, cùng tên tác giả `GET /api/accounts/lookup`. Mở đọc không mở ghi — mọi POST, PUT, DELETE vẫn phải đăng nhập, và `/api/dashboard`, `/api/accounts`, `/api/profile` cũng vậy.

Xác thực bằng **JWT Bearer**, không có phiên và không có cookie. `POST /api/auth/login` trả về `accessToken`; mọi request sau gửi kèm header `Authorization: Bearer <token>`. Token sống 15 phút, `POST /api/auth/logout` thu hồi token đó ngay. Token **không** mang theo vai trò: mỗi request server đọc lại tài khoản từ database, nên hạ quyền hay khóa tài khoản có hiệu lực tức thì.

| Phương thức | Endpoint | Chức năng | Ai viết |
| --- | --- | --- | --- |
| POST | `/api/auth/login` | `{ "username": "Admin", "password": "Admin" }` → `accessToken` | QA |
| GET | `/api/auth/me` | Tài khoản đang đăng nhập | QA |
| POST | `/api/auth/logout` | Thu hồi token hiện tại, trả 204 | QA |
| GET, POST | `/api/accounts` | Danh sách / tạo tài khoản, Admin | QA |
| GET, PUT, DELETE | `/api/accounts/{id}` | Xem / sửa / xóa tài khoản, Admin | QA |
| GET | `/api/accounts/lookup` | ID và tên tác giả | QA |
| GET | `/api/roles` | Danh sách vai trò | QA |
| GET, PUT | `/api/profile` | Xem / sửa hồ sơ của chính mình | QA |
| GET, POST | `/api/categories` | Danh sách / tạo danh mục; `?status=true\|false` để lọc; ghi cần Admin | Long |
| GET, PUT, DELETE | `/api/categories/{id}` | Xem / sửa / xóa danh mục; ghi cần Admin | Long |
| GET, POST | `/api/news` | Danh sách / tạo bài viết | Huy |
| GET, PUT, DELETE | `/api/news/{id}` | Xem / sửa / xóa bài viết | Huy |
| GET | `/api/news/review-queue` | Hàng đợi duyệt bài, Editor và Admin | Huy |
| GET | `/api/news/{id}/audit` | Nhật ký đổi trạng thái | Huy |
| POST | `/api/news/{id}/submit` | Nộp bài cho biên tập | Huy |
| POST | `/api/news/{id}/request-changes` | Trả bài lại, **bắt buộc** có `note` | Huy |
| POST | `/api/news/{id}/approve` | Duyệt; kèm `scheduledPublishAt` thì hẹn giờ | Huy |
| POST | `/api/news/{id}/publish` | Đăng sớm bài đang chờ giờ hẹn | Huy |
| POST | `/api/news/{id}/archive` | Gỡ khỏi trang công khai | Huy |
| GET | `/api/dashboard` | Thống kê và 5 bài mới nhất theo vai trò | Huy |
| GET | `/api/notifications` | Hộp thư của chính mình | Huy |
| GET | `/api/notifications/unread-count` | Số thông báo chưa đọc | Huy |
| POST | `/api/notifications/{id}/read` | Đánh dấu đã đọc | Huy |
| GET, PUT | `/api/editorial-rules` | Quy tắc biên tập; sửa chỉ Admin | Huy |

Vai trò: `roleId` 1 = ADMIN, 2 = EDITOR, 3 = CONTRIBUTOR, 4 = READER, và `roleCode` trả kèm để frontend khỏi đoán. `isActive` là boolean. `status` của bài viết là một trong `DRAFT`, `PENDING_REVIEW`, `CHANGES_REQUESTED`, `SCHEDULED`, `PUBLISHED`, `ARCHIVED` — khi tạo bài chỉ nhận hai giá trị đầu, các bước sau đi qua endpoint riêng. `parentId: 0`: không có danh mục cha.

**Hiển thị theo vai trò** lọc ở backend: khách và READER chỉ thấy bài `PUBLISHED`; CONTRIBUTOR thấy thêm bài của chính mình ở mọi trạng thái; EDITOR và ADMIN thấy hết. Bản nháp của người khác trả **404** chứ không 403.

Lỗi trả JSON `{ "message": "..." }`: 400 dữ liệu sai, 401 chưa đăng nhập, 403 thiếu quyền, 404 không tìm thấy, 409 trùng dữ liệu hoặc đang được tham chiếu. Phần này do [ApiExceptionHandler](backend/Long/src/main/java/com/funews/backend/exception/ApiExceptionHandler.java) lo, controller chỉ cần `throw new ApiException(...)`.

Phân quyền theo URL đã khai sẵn trong [SecurityConfig](backend/QA/src/main/java/com/funews/backend/config/SecurityConfig.java) — controller không phải tự kiểm tra lại.

## Tài khoản chạy thử

`database/module1_schema.sql` tạo sẵn các tài khoản dưới đây. Mật khẩu lưu dưới dạng hash BCrypt, không bao giờ lưu dạng thường — `b@` và `c@` cùng mật khẩu `123` nhưng hash trong database khác hẳn nhau vì BCrypt dùng salt ngẫu nhiên.

| Tên đăng nhập | Mật khẩu | Vai trò | Làm được gì |
| --- | --- | --- | --- |
| `Admin` hoặc `admin@fu.edu.vn` | `Admin` | ADMIN | ghi đè mọi bước, quản lý tài khoản, danh mục và quy tắc biên tập |
| `a@fu.edu.vn` | `123` | EDITOR | duyệt bài, trả bài kèm lý do, hẹn giờ đăng, gỡ bài |
| `b@fu.edu.vn` | `123` | CONTRIBUTOR | tạo và nộp bài của chính mình, không tự đăng được |
| `c@fu.edu.vn` | `123` | CONTRIBUTOR | như trên |
| `reader@fu.edu.vn` | `123` | READER | chỉ đọc bài đã xuất bản |

Còn một tài khoản `system@fu.edu.vn` **không đăng nhập được** (`status = 0`, hash không hợp lệ) — nó chỉ để tác vụ hẹn giờ đứng tên trong nhật ký, vì `article_audit_logs.actor_id` là NOT NULL.

Admin vào được cả 7 màn hình (thêm **Rules**); các vai trò khác thấy Dashboard, News và Settings.
