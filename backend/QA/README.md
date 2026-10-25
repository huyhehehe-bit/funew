# QA — đăng nhập JWT, phân quyền và tài khoản

Phạm vi chỉnh sửa: `backend/QA/` và `frontend/QA/`, theo [PROMPT.md](../../PROMPT.md) và [CONTRIBUTING.md](../../CONTRIBUTING.md). Schema Module 1 được mô tả trong [NOTE-MODULE1.md](../../NOTE-MODULE1.md).

## Xác thực hiện tại

Backend cấp JWT HS256 sau khi kiểm tra mật khẩu BCrypt. Client gửi `Authorization: Bearer <accessToken>` cho các request tiếp theo. HTTP session và cookie `JSESSIONID` không còn dùng để đăng nhập.

JWT mặc định sống **900 giây (15 phút)**. Backend kiểm tra chữ ký, thuật toán, issuer, audience, subject là ID tài khoản, thời gian phát hành/hết hạn và mã token. Tài khoản cùng vai trò được đọc lại từ database ở mỗi request, nên đổi quyền, khóa tài khoản, xóa mềm hoặc ngừng dùng vai trò có hiệu lực ngay. Đổi mật khẩu làm token cũ mất hiệu lực; đổi tên/email giữ token hợp lệ.

Không thêm dependency hay thay POM: codec chỉ chấp nhận HS256 và dùng HMAC của JDK cùng Jackson đã có trong dự án. Các quy tắc kiểm tra dựa trên [RFC 8725](https://www.rfc-editor.org/rfc/rfc8725.html) và [RFC 7519](https://www.rfc-editor.org/rfc/rfc7519.html).

## Cấu hình khóa ký

Bắt buộc đặt `JWT_SECRET`: Base64 của khóa ngẫu nhiên ít nhất 32 byte. Không có khóa mặc định trong source. Ví dụ PowerShell, từ thư mục `backend`:

```powershell
$jwtKeyBytes = New-Object byte[] 32
$jwtRandom = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtRandom.GetBytes($jwtKeyBytes)
$env:JWT_SECRET = [Convert]::ToBase64String($jwtKeyBytes)
$jwtRandom.Dispose()
.\mvnw.cmd spring-boot:run
```

Giữ cùng khóa trong cấu hình riêng của môi trường nếu cần token hoạt động sau khi khởi động lại; tạo khóa mới sẽ vô hiệu hóa toàn bộ token cũ. Không commit hoặc ghi khóa/token vào log. Khi triển khai, truyền token qua HTTPS.

Có thể đặt các property bằng biến môi trường hoặc tham số Spring:

| Property | Mặc định | Ý nghĩa |
| --- | --- | --- |
| `app.jwt.secret` / `JWT_SECRET` | Bắt buộc | Khóa ký Base64 |
| `app.jwt.ttl-seconds` | `900` | Thời hạn từ 1 đến 3600 giây |
| `app.jwt.issuer` | `funews` | Nơi phát hành |
| `app.jwt.audience` | `funews-api` | API nhận token |

Không cần sửa `backend/shared/src/main/resources/application.properties`. Thiếu khóa hoặc khóa quá ngắn thì backend dừng khởi động và báo lỗi cấu hình.

## API đăng nhập và đăng xuất

`POST /api/auth/login`:

```json
{ "username": "Admin", "password": "Admin" }
```

Response:

```json
{
  "accessToken": "<JWT>",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": 1,
    "fullName": "Admin",
    "email": "admin@fu.edu.vn",
    "roleId": 1,
    "roleCode": "ADMIN",
    "isActive": true,
    "username": "admin@fu.edu.vn",
    "dateOfBirth": null,
    "createdAt": "2026-10-25T09:00:00",
    "status": 1
  }
}
```

Ví dụ PowerShell:

```powershell
$loginBody = @{ username = 'Admin'; password = 'Admin' } | ConvertTo-Json
$auth = Invoke-RestMethod http://localhost:8080/api/auth/login -Method Post -ContentType application/json -Body $loginBody
$authHeaders = @{ Authorization = "Bearer $($auth.accessToken)" }
Invoke-RestMethod http://localhost:8080/api/auth/me -Headers $authHeaders
Invoke-RestMethod http://localhost:8080/api/auth/logout -Method Post -Headers $authHeaders
```

- `GET /api/auth/me` trả thông tin tài khoản hiện tại.
- `POST /api/auth/logout` trả 204 và thu hồi token đang gửi. Request dùng lại token đó trả 401.
- Token thiếu/sai/hết hạn trả 401; token hợp lệ nhưng thiếu quyền trả 403.
- Response không chứa mật khẩu hoặc hash.
- CORS trong `SecurityConfig` bổ sung header `Authorization` vào chính sách origin hiện có, phục vụ frontend ở cổng 5173.

Danh sách thu hồi token nằm trong bộ nhớ của **một backend**, tự dọn khi token hết hạn. Khi restart, danh sách này mất: nếu giữ cùng khóa ký, bản sao token đã logout có thể hợp lệ lại đến thời điểm hết hạn. Khi chạy nhiều instance hoặc cần thu hồi bền vững, nhóm cần thống nhất thêm kho chung (database/Redis). Hiện chưa có refresh token; hết 15 phút thì đăng nhập lại.

Swagger có scheme `bearerAuth`: đăng nhập lấy `accessToken`, bấm **Authorize** và dán token. Phần mô tả cookie trong `backend/shared/.../OpenApiConfig.java` và hướng dẫn Postman dùng chung vẫn thuộc bản cũ; cần chủ sở hữu cập nhật khi ghép nhóm.

## Frontend QA

`frontend/QA/api.js` lưu token trong `sessionStorage` theo tab, tự gắn Bearer và trả đối tượng user cho màn hình Login. Khi storage bị chặn, dùng bộ nhớ của trang. Khi refresh, client đọc token rồi gọi `/api/auth/me`; khi logout hoặc nhận 401 từ token hiện tại thì xóa token.

Để giữ các màn hình Huy/Long đang đọc hợp đồng cũ hoạt động, client cung cấp thêm alias `name = fullName`, `role = roleId`. Chỉ payload tài khoản/hồ sơ được đổi `name/role` sang `fullName/roleId`; dữ liệu bài viết và danh mục giữ nguyên hợp đồng của từng thành viên.

`api.js` được cả nhóm sử dụng. Thay đổi trong lần này: đăng nhập chuyển sang JWT, client tự gửi Authorization và không gửi cookie xác thực. Khi ghép nhóm, báo các thành viên dùng bản client này.

## Quy tắc nghiệp vụ tài khoản

- Mật khẩu lưu BCrypt; để trống mật khẩu khi sửa thì giữ mật khẩu cũ.
- Email/username trùng ở tài khoản chưa xóa bị từ chối.
- Tạo tài khoản bỏ username thì dùng email; ngày sinh chưa biết giữ NULL.
- Không xóa chính mình, tài khoản có bài viết hoặc xóa/hạ quyền/khóa admin hoạt động cuối cùng.
- Tài khoản xóa mềm hoặc ngừng hoạt động không được đăng nhập.
- `GET /api/roles` trả vai trò đang dùng cho admin.

## Kiểm thử

Lệnh chuẩn của dự án, từ `backend`:

```powershell
.\mvnw.cmd test
```

Toàn backend còn lỗi biên dịch sẵn có ở `Huy/NewsService.java` và `Long/CategoryService.java`. Các test dùng chung cũng còn fixture cookie/DTO cũ. Để kiểm tra riêng QA, từ thư mục gốc:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File backend/QA/tools/run-qa-tests.ps1
node --test frontend/QA/api.test.mjs
```

Tool QA dùng JDK 17 cùng dependency đã có trong Maven cache, không tải thư viện hoặc sửa POM. Có thể truyền `-MavenRepository <đường dẫn>` nếu cache khác mặc định. Artifact và log nằm trong `backend/QA/.verification/`, đã được bỏ qua bằng `.gitignore` của QA.

Các test kiểm tra chữ ký/claim/thời hạn và token giả; login không tạo session; logout không dùng lại được token; quyền/trạng thái/mật khẩu thay đổi; CORS; mật khẩu Unicode; cùng các trường tài khoản của Module 1. Web test dùng controller/filter thật và mock repository/service, không phải kiểm thử toàn ứng dụng với SQL Server.

Frontend, từ `frontend`:

```powershell
npm.cmd run lint
npm.cmd run build
```
