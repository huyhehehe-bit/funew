# QA — Đăng nhập, phiên, phân quyền, tài khoản

Chỉ sửa `backend/QA/` và `frontend/QA/`. Quy trình commit: [CONTRIBUTING.md](../../CONTRIBUTING.md). Cách nhờ Claude viết code: [PROMPT.md](../../PROMPT.md).

## Đã có sẵn — không phải viết lại

| File | Nội dung |
| --- | --- |
| [entity/Account.java](src/main/java/com/funews/backend/entity/Account.java) | Bảng tài khoản |
| [repository/AccountRepository.java](src/main/java/com/funews/backend/repository/AccountRepository.java) | Truy vấn tài khoản |
| [config/SecurityConfig.java](src/main/java/com/funews/backend/config/SecurityConfig.java) | Filter chain, BCrypt, phân quyền theo URL, logout, 401/403 |
| [security/AccountPrincipal.java](src/main/java/com/funews/backend/security/AccountPrincipal.java) | Người dùng đang đăng nhập trong Spring Security |
| [security/RefreshAccountFilter.java](src/main/java/com/funews/backend/security/RefreshAccountFilter.java) | Nạp lại quyền từ database mỗi request |

`SecurityConfig` đã khai sẵn `PasswordEncoder`, `AuthenticationManager`, `SecurityContextRepository`, `SessionAuthenticationStrategy` và `UserDetailsService` — controller chỉ việc inject vào dùng. Logout ở `POST /api/auth/logout` cũng đã xong, không phải viết.

## Đã viết

| File | Việc |
| --- | --- |
| [controller/AuthController.java](src/main/java/com/funews/backend/controller/AuthController.java) | `POST /api/auth/login`, `GET /api/auth/me` |
| [controller/AccountController.java](src/main/java/com/funews/backend/controller/AccountController.java) | `/api/accounts`, `/api/accounts/{id}`, `/api/accounts/lookup`, `/api/profile` |
| [service/AccountService.java](src/main/java/com/funews/backend/service/AccountService.java) | Nghiệp vụ tài khoản và hồ sơ |

Bảng endpoint đầy đủ ở [README chung](../../README.md#hợp-đồng-api). Tài liệu API trực quan: chạy backend rồi mở `http://localhost:8080/swagger-ui.html`, phần của bạn nằm ở nhóm **1. Auth va 2. Accounts**.

Quy tắc nghiệp vụ phải giữ:

- Mật khẩu mã hóa BCrypt; API không bao giờ trả `password` hay hash.
- Sửa tài khoản mà để trống Password thì giữ nguyên mật khẩu cũ.
- Email trùng (không phân biệt hoa thường) bị từ chối — `throw new ApiException(HttpStatus.CONFLICT, "...")`.
- Không xóa được tài khoản đang đăng nhập, tài khoản đã có bài viết, và không xóa hay hạ quyền Admin cuối cùng.

## Phần frontend của bạn — đã xong

`frontend/QA/`: [App.jsx](../../frontend/QA/App.jsx), [api.js](../../frontend/QA/api.js), [data.js](../../frontend/QA/data.js), [pages/Login.jsx](../../frontend/QA/pages/Login.jsx), [pages/Users.jsx](../../frontend/QA/pages/Users.jsx).

`api.js` được Long và Huy đều gọi — đổi cách nó trả lỗi hay giữ cookie là ảnh hưởng cả nhóm, nhắn nhóm trước.

## Phối hợp

- `ApiDtos` là của Long. Bạn dùng `AccountRequest`, `AccountResponse`, `AccountLookup`, `ProfileRequest`. Cần thêm hoặc đổi trường thì nhắn Long, đừng tự sửa.
- `/api/profile` là của bạn nhưng màn hình Settings bên frontend là của Long.
- Luật phân quyền trong `SecurityConfig` ảnh hưởng mọi endpoint của Long và Huy. Sửa là phải báo nhóm và chạy lại toàn bộ test.

## Trước khi push

```powershell
cd backend
.\mvnw.cmd test
```

1. Đăng nhập Admin và Staff; sai mật khẩu trả lỗi rõ ràng.
2. Tải lại trang vẫn giữ phiên; sau khi logout gọi API được bảo vệ trả 401.
3. Admin thêm, sửa, xóa tài khoản chưa có bài viết; API không trả password hay hash.
4. Sửa tài khoản với Password trống vẫn đăng nhập được bằng mật khẩu cũ.
5. Email trùng khác chữ hoa thường và dữ liệu bắt buộc thiếu bị từ chối.
6. Không xóa được tài khoản đang đăng nhập, tài khoản có bài viết, hay Admin cuối cùng.
7. Hạ quyền một Admin khác đang có phiên: phiên đó mất quyền quản lý ngay.
