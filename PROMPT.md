# Cách nhờ Claude viết phần của mình

Tài liệu này dạy cách đặt câu lệnh (prompt) để Claude viết đúng phần của bạn và **không đụng vào folder của hai người kia**.

Ví dụ bên dưới viết cho Huy. Đổi `Huy` thành `QA` hoặc `Long` tùy bạn là ai.

## Một prompt tốt gồm 4 phần

| Phần | Trả lời câu hỏi | Ví dụ |
| --- | --- | --- |
| **Bối cảnh** | Đang ở đâu, repo gì | "Repo FU News, Spring Boot + React, chia folder theo người." |
| **Việc cần làm** | Làm gì, cụ thể | "Viết `NewsService` và `NewsController` cho CRUD bài viết và dashboard." |
| **Ranh giới** | Được sửa file nào | "Chỉ sửa trong `backend/Huy/`. Không đụng `backend/QA/`, `backend/Long/`, `backend/shared/`, `backend/pom.xml`." |
| **Tiêu chí xong** | Thế nào là xong | "`mvnw test` xanh, `GET /api/news` trả danh sách, server tự gán tác giả." |

Thiếu **Ranh giới** là lỗi hay gặp nhất — Claude sẽ tự tiện sửa file của người khác cho code chạy được, rồi bạn commit lên là đụng bài của bạn cùng nhóm.

## Câu khóa phải có trong mọi prompt

> Chỉ sửa file trong `backend/Huy/` và `frontend/Huy/`. Nếu cần đổi file ngoài hai folder đó thì **dừng lại và hỏi tôi trước**, đừng tự sửa.

Chép nguyên câu này vào cuối mỗi prompt. Nó là thứ giữ cho ba phần không ảnh hưởng nhau.

## Prompt mẫu — chép và sửa lại cho mình

### 1. Nhờ viết trọn phần của mình

```text
Repo FU News: backend Spring Boot 3.5.16 (Java 17) + frontend React/Vite,
source chia theo ba nguoi trong folder QA, Long, Huy. Toi la Huy.

Hay viet phan backend cua toi, hien dang de trong:
- backend/Huy/.../controller/NewsController.java
- backend/Huy/.../service/NewsService.java
- backend/Huy/.../config/DataInitializer.java

Dac ta day du nam trong comment o dau moi file do, va bang API trong README.md.
Dung ApiDtos cua Long lam kieu du lieu vao/ra, nem ApiException khi loi.

Xong thi chay `mvnw test` cho toi xem ket qua.

Chi sua file trong backend/Huy/. Neu can doi file ngoai folder do
(nhat la ApiDtos cua Long hay SecurityConfig cua QA) thi dung lai va hoi toi truoc.
```

### 2. Nhờ thêm một endpoint

```text
Them endpoint GET /api/news/recent tra ve 10 bai viet moi nhat,
sap xep theo createdDate giam dan.

Sua trong backend/Huy/ thoi: them method vao NewsController va NewsService.
Khong doi ApiDtos cua Long — neu bat buoc phai them truong moi thi hoi toi truoc.
```

### 3. Nhờ sửa lỗi

Dán **nguyên văn** thông báo lỗi, đừng kể lại bằng lời.

```text
Chay `mvnw spring-boot:run` bi loi nay:

<dan nguyen van doan loi vao day>

Tim nguyen nhan va sua. Chi sua trong backend/Huy/.
Neu loi nam o file cua nguoi khac thi bao toi biet file nao, dong nao,
va de toi nhan tin cho nguoi do — dung tu sua.
```

### 4. Nhờ viết test

```text
Viet test tich hop cho phan cua toi vao
backend/Huy/src/test/java/com/funews/backend/SecurityAndNewsIntegrationTest.java.

Ke thua BackendIntegrationSupport o backend/shared/src/test/.
Can 6 test: dang nhap giu phien, chua dang nhap bi 401, ghi thieu CSRF bi 403,
Staff tao duoc bai viet, server tu gan tac gia khi client gui createdById gia,
va CRUD bai viet day du.

Chi tao/sua file trong backend/Huy/src/test/.
```

### 5. Nhờ giải thích code để bảo vệ đồ án

```text
Giai thich cho toi luong chay cua GET /api/dashboard trong phan cua toi:
tu luc frontend goi api, qua Spring Security, NewsController, NewsService,
repository, xuong H2 roi tra ve.

Giai thich theo kieu de toi tra loi duoc khi thay hoi. Khong sua code.
```

### 6. Nhờ kiểm tra trước khi commit

```text
Toi sap commit. Hay chay `git status` va kiem tra giup:
- co file nao ngoai backend/Huy/ va frontend/Huy/ bi thay doi khong
- `mvnw test` co xanh khong
- `npm run lint` va `npm run build` o frontend co sach khong

Neu co file ngoai folder cua toi bi dong vao thi liet ke ra, dung tu `git add`.
```

### 7. Nhờ viết luôn câu commit

```text
Viet giup toi message commit cho nhung thay doi dang co trong backend/Huy/.
Mot dong tieu de bat dau bang "Huy: ", roi vai gach dau dong mo ta.
Dung commit ho toi, chi viet message thoi.
```

## Prompt tệ và prompt tốt

| Tệ | Vì sao tệ | Tốt hơn |
| --- | --- | --- |
| "Làm cho nó chạy đi" | Không biết "nó" là gì, "chạy" là thế nào | "Chạy `mvnw spring-boot:run` bị lỗi sau: \<dán lỗi\>. Tìm nguyên nhân và sửa trong `backend/Huy/`." |
| "Viết API tin tức" | Không rõ endpoint nào, trả về gì | "Viết `NewsController` với 5 endpoint theo bảng API trong README, dùng `ApiDtos.NewsResponse`." |
| "Sửa hết lỗi trong dự án" | Sẽ đụng vào folder của cả ba người | "Liệt kê lỗi trong `backend/Huy/`, đừng sửa gì ngoài folder đó." |
| "Code lại từ đầu cho đẹp" | Mơ hồ, dễ đập cả phần người khác | "Tách `NewsService.createNews` thành hàm nhỏ hơn, giữ nguyên hành vi, giữ test xanh." |
| "Thêm trường author vào bài viết" | Trường này nằm trong `ApiDtos` của Long | "Tôi cần thêm trường `author` vào `NewsResponse`, nhưng file đó của Long. Soạn giúp tôi tin nhắn để tôi nhắn Long." |

## Mẹo khi làm nhiều lượt

- **Nói rõ "tôi là Huy" ngay câu đầu.** Claude sẽ tự biết ranh giới cho những câu sau.
- **Dán nguyên văn lỗi.** Đừng kể lại "nó báo lỗi gì đó về database" — dán cả đoạn stack trace.
- **Một prompt một việc.** "Viết controller rồi viết test rồi sửa frontend rồi commit" làm xong sẽ khó kiểm tra. Tách ra.
- **Bảo chạy kiểm tra, đừng tin suông.** Thêm "chạy `mvnw test` và cho tôi xem kết quả" vào cuối prompt. Code trông đúng mà không chạy là chuyện bình thường.
- **Yêu cầu giải thích ngắn.** Thêm "giải thích ngắn gọn những gì bạn đã sửa" để bạn hiểu code mình sắp commit — lúc thầy hỏi còn trả lời được.
- **Muốn dừng lại xem trước khi sửa**, nói: "Nói cho tôi bạn định sửa những file nào và sửa gì, đừng sửa vội."

## Không nên nhờ

- **Commit và push hộ.** Bạn tự chạy `git add`, `git commit`, `git push` để kiểm soát được cái gì lên repo. Nhờ Claude soạn message thì được.
- **Sửa file của người khác "cho nhanh".** Dù chỉ một dòng. Đó là lúc conflict bắt đầu.
- **Sửa `backend/pom.xml` hay `frontend/package.json` một mình.** Nhắn nhóm trước.
- **Xóa folder hay file hàng loạt** mà chưa xem danh sách sẽ xóa.

## Nếu thật sự cần đổi file dùng chung

Ví dụ bạn cần thêm một trường vào `ApiDtos` của Long:

```text
Toi can them truong `viewCount` vao ApiDtos.NewsResponse, nhung file do cua Long.

Dung sua. Thay vao do:
1. Chi cho toi chinh xac can them dong nao, o file nao.
2. Soan giup toi mot tin nhan ngan de toi gui cho Long nho ban ay them.
3. Cho toi biet phan nao cua toi se phai sua theo sau khi Long them xong.
```

Đây là cách giữ cho hợp đồng API vẫn là của một người, và nhóm không giẫm chân nhau.
