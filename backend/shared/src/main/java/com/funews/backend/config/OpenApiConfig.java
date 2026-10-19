package com.funews.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cau hinh tai lieu API (Swagger / OpenAPI 3).
 *
 * Mo trinh duyet tai http://localhost:8080/swagger-ui.html
 * File OpenAPI dang JSON: http://localhost:8080/v3/api-docs
 *
 * Phan dung chung cua ca nhom - doi phai bao nhom.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI funewsOpenApi() {
        return new OpenAPI().info(new Info()
                .title("FU News Management API")
                .version("1.0.0")
                .license(new License().name("SBA301 - Group Assignment, Fall 2026"))
                .description("""
                        REST API cho he thong quan ly tin tuc FU News.

                        ## Cach dang nhap de thu API

                        Goi `POST /api/auth/login` voi body:

                        ```json
                        { "username": "Admin", "password": "Admin" }
                        ```

                        Xong. Phien duoc giu bang cookie `JSESSIONID`; trinh duyet va Swagger UI
                        tu gui kem cookie do cho moi request sau. Khong can lay token truoc,
                        khong can dat header gi them.

                        Dang xuat: `POST /api/auth/logout`.

                        ## Endpoint mo cong khai

                        Khach chua dang nhap van GET duoc:

                        - `GET /api/news` va `GET /api/news/{id}` - doc bai viet
                        - `GET /api/categories` va `GET /api/categories/{id}` - doc danh muc
                        - `GET /api/accounts/lookup` - ID va ten tac gia

                        Moi thao tac ghi, cung voi `/api/dashboard`, `/api/accounts`
                        va `/api/profile`, van yeu cau dang nhap.

                        ## Tai khoan thu nghiem

                        | Tai khoan | Mat khau | Quyen |
                        |---|---|---|
                        | `Admin` hoac `admin@fu.edu.vn` | `Admin` | Admin |
                        | `a@fu.edu.vn` | `123` | Admin |
                        | `b@fu.edu.vn` | `123` | Staff |
                        | `c@fu.edu.vn` | `123` | Staff |

                        Admin dung duoc moi endpoint. Staff khong quan ly duoc tai khoan,
                        va chi doc duoc danh muc chu khong sua duoc.

                        ## Quy uoc du lieu

                        - `role`: 1 = Admin, 2 = Staff
                        - `status` va `isActive`: 0 = Inactive, 1 = Active
                        - `parentId` = 0: danh muc khong co cha
                        - Voi bai viet, server tu xac dinh `createdById` va `createdDate`;
                          client gui len cung bi bo qua.

                        ## Quy uoc loi

                        Loi tra ve JSON dang `{ "message": "..." }`:

                        | Ma | Y nghia |
                        |---|---|
                        | 400 | Du lieu khong hop le (kem truong `errors` chi ro tung field) |
                        | 401 | Chua dang nhap hoac phien het han |
                        | 403 | Khong du quyen |
                        | 404 | Khong tim thay ban ghi |
                        | 409 | Trung du lieu, hoac ban ghi dang duoc tham chieu |
                        """));
    }
}
