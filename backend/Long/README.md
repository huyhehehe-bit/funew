# Long — Danh mục, DTO, xử lý lỗi

Chỉ sửa `backend/Long/` và `frontend/Long/`. Quy trình commit: [CONTRIBUTING.md](../../CONTRIBUTING.md). Cách nhờ Claude viết code: [PROMPT.md](../../PROMPT.md).

## Đã có sẵn — không phải viết lại

| File | Nội dung |
| --- | --- |
| [entity/Category.java](src/main/java/com/funews/backend/entity/Category.java) | Bảng danh mục |
| [repository/CategoryRepository.java](src/main/java/com/funews/backend/repository/CategoryRepository.java) | Truy vấn danh mục |
| [dto/ApiDtos.java](src/main/java/com/funews/backend/dto/ApiDtos.java) | **Dùng chung** — hợp đồng JSON của toàn bộ API |
| [exception/ApiException.java](src/main/java/com/funews/backend/exception/ApiException.java) | **Dùng chung** — lỗi nghiệp vụ kèm HTTP status |
| [exception/ApiExceptionHandler.java](src/main/java/com/funews/backend/exception/ApiExceptionHandler.java) | **Dùng chung** — đổi lỗi thành JSON `{ "message": "..." }` |

## Đã viết

| File | Việc |
| --- | --- |
| [controller/CategoryController.java](src/main/java/com/funews/backend/controller/CategoryController.java) | `/api/categories`, `/api/categories/{id}` |
| [service/CategoryService.java](src/main/java/com/funews/backend/service/CategoryService.java) | Nghiệp vụ danh mục |

Bảng endpoint đầy đủ ở [README chung](../../README.md#hợp-đồng-api). Tài liệu API trực quan: chạy backend rồi mở `http://localhost:8080/swagger-ui.html`, phần của bạn nằm ở nhóm **3. Categories**.

Quy tắc nghiệp vụ phải giữ:

- Không cho chọn chính nó hoặc hậu duệ làm danh mục cha — tránh vòng lặp.
- Không xóa được danh mục còn danh mục con hoặc đang có bài viết: `throw new ApiException(HttpStatus.CONFLICT, "...")`.
- Tên danh mục bắt buộc; `parentId = 0` nghĩa là không có cha.
- Phân quyền ghi (chỉ Admin) đã khai sẵn trong `SecurityConfig` của QA — không phải tự kiểm tra lại.

## Phần frontend của bạn — đã xong

`frontend/Long/`: [index.css](../../frontend/Long/index.css), [components/](../../frontend/Long/components), [pages/Categories.jsx](../../frontend/Long/pages/Categories.jsx), [pages/Settings.jsx](../../frontend/Long/pages/Settings.jsx).

`components/` (Header, Menu, Modal, Toolbar, Logo, LoadStatus) được QA và Huy đều dùng — đổi props là ảnh hưởng màn hình của họ.

## Phối hợp — bạn giữ hợp đồng API của cả nhóm

`ApiDtos`, `ApiException` và `ApiExceptionHandler` nằm trong folder của bạn nhưng **QA và Huy đều phụ thuộc**. Đổi tên trường, kiểu dữ liệu hay validation là đổi hợp đồng API:

1. Nhắn nhóm trước.
2. Sửa trên một nhánh riêng, mở Pull Request để hai người kia duyệt.
3. Chạy đủ test trước khi push, và báo cả bên frontend.

News của Huy tham chiếu danh mục qua `categoryId` — giữ nguyên `id`, `name`, `parentId`, `isActive` trong `CategoryResponse`.

Settings của bạn gọi `/api/profile`, endpoint đó do QA viết.

## Trước khi push

```powershell
cd backend
.\mvnw.cmd test
```

1. Admin thêm, sửa, xóa danh mục chưa được dùng; khởi động lại vẫn còn dữ liệu.
2. Tạo danh mục cha con; không cho xóa cha khi còn con.
3. Không cho chọn chính nó hoặc hậu duệ làm cha.
4. Không xóa được danh mục đang có bài viết, trả 409 kèm `message` rõ nghĩa.
5. Staff đọc được danh mục bằng GET nhưng POST, PUT, DELETE trả 403.
6. Sửa tên và email qua `/api/profile`; email trùng bị từ chối.
7. Payload profile có thêm `role` không được nâng quyền.
