# FU Cinema Ticket System

Website quản lý rạp chiếu phim và đặt vé trực tuyến cho Assignment 1, môn HSF302. Ứng dụng hỗ trợ khách xem phim, chọn ghế, đặt/hủy vé, quản lý hồ sơ và đánh giá phim; Admin quản lý khách hàng, phim, phòng, suất chiếu, đơn đặt vé và báo cáo doanh thu.

## Công nghệ và yêu cầu

| Thành phần | Phiên bản/cách sử dụng |
|---|---|
| Java | JDK 21 trở lên; khuyến nghị JDK 21 |
| Spring Boot | 4.1.1 |
| Spring Data JPA / Hibernate | Theo dependency management của Spring Boot; Hibernate 7.4.5.Final |
| Giao diện | Thymeleaf, Bootstrap 5.3.3 |
| Database chạy chính | SQL Server; hướng dẫn tạo mới bên dưới dùng SQL Server 2019+ để hỗ trợ UTF-8 |
| Database kiểm thử | H2, profile `test`, chế độ MSSQLServer |
| Xác thực | HttpSession, HandlerInterceptor phân quyền; BCrypt mã hóa mật khẩu |
| Spring Security | Giữ CSRF và security headers; form đăng nhập và phân quyền do ứng dụng xử lý |
| Build | Maven Wrapper đi kèm repository |

Bootstrap và ảnh poster lấy từ Internet. Khi không có kết nối, ứng dụng vẫn có thể khởi động nhưng giao diện/ảnh có thể không tải đủ.

## Cài đặt trên Windows PowerShell

### 1. Clone và mở thư mục chứa pom.xml

```powershell
git clone https://github.com/HSFHuuHaVC/Assignment1.git
Set-Location Assignment1
git switch develop
java -version
```

Đây là repository riêng tư: cần tài khoản đã được cấp quyền truy cập. Nếu IDE mở thư mục cha, chuyển đến đúng thư mục có `pom.xml` trước khi chạy Maven. Khi đã có bản phát hành, có thể checkout tag `v1.0.0`; chỉ dùng lệnh đó sau khi tag thực sự được tạo.

### 2. Chuẩn bị SQL Server

Bật dịch vụ SQL Server và TCP/IP; kiểm tra cổng kết nối của instance, thường là `1433`. Trong SSMS, kết nối server rồi chạy:

```sql
USE master;
GO
IF DB_ID(N'FUCinemaTicketDB') IS NULL
    CREATE DATABASE FUCinemaTicketDB COLLATE Latin1_General_100_CI_AS_SC_UTF8;
GO
```

Collation UTF-8 giúp database mới lưu tiếng Việt ngay cả với cột `VARCHAR`. Nếu database đã tồn tại với collation khác, lệnh trên giữ nguyên database đó. Các cột chứa tiếng Việt cần dùng `NVARCHAR` hoặc được migrate phù hợp trước khi nhập dữ liệu; đổi kiểu cột không tự phục hồi chữ đã mất dấu. Không xóa database đang có dữ liệu để chạy lại seed.

Ứng dụng dùng `spring.jpa.hibernate.ddl-auto=update` để tạo/cập nhật các bảng từ entity. Tài khoản SQL cần quyền kết nối và tạo/cập nhật schema trong database này. Không cần chạy script tạo bảng thủ công.

### 3. Tạo cấu hình kết nối cá nhân

```powershell
Copy-Item src/main/resources/application-local.properties.example src/main/resources/application-local.properties
```

Mở file vừa tạo, thay server/cổng, username và password bằng thông tin trên máy. Các khóa cần có:

```properties
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=FUCinemaTicketDB;encrypt=true;trustServerCertificate=true
spring.datasource.username=YOUR_SQL_USERNAME
spring.datasource.password=YOUR_SQL_PASSWORD
```

`trustServerCertificate=true` phục vụ môi trường học tập trên máy cá nhân. File `application-local.properties` đã nằm trong `.gitignore`; không commit file này hoặc mật khẩu SQL Server.

### 4. Build và chạy

```powershell
.\mvnw.cmd verify
.\mvnw.cmd spring-boot:run
```

Mở **http://localhost:8080/**. Cấu hình mặc định chọn profile `local`, kết nối SQL Server. Lần đầu Maven cần Internet để tải dependencies. Trên Linux/macOS dùng `./mvnw` thay cho `.\mvnw.cmd`.

Lần khởi động đầu, `DataSeeder` tạo dữ liệu mẫu khi bảng `accounts` chưa có bản ghi. Nếu bảng đã có tài khoản, seed được bỏ qua. Ngày chiếu trong seed tính theo ngày khởi tạo và không tự dịch chuyển khi khởi động lại; để thử đặt vé cần chọn/tạo suất `OPEN` còn ở tương lai.

## Tài khoản mẫu

| Vai trò | Email | Mật khẩu seed |
|---|---|---|
| Admin | admin@fucinema.vn | Admin@123 |
| Customer | an.nv@gmail.com | Customer@123 |
| Customer khoảng 15 tuổi khi seed | binh.tt@gmail.com | Customer@123 |

Đây là tài khoản mẫu của bài tập, không phải mật khẩu kết nối database. Nếu đã đổi mật khẩu hoặc khóa tài khoản thì dùng trạng thái hiện tại trong database.

Admin đăng nhập được đưa tới `/admin`; Customer về `/`. Đăng xuất bằng nút trên menu vì endpoint đăng xuất dùng POST.

## Chạy và kiểm thử với H2

Smoke test trong repository khởi động Spring context bằng profile `test`, kiểm tra ứng dụng nạp được bean, mapping và repository mà không cần SQL Server:

```powershell
.\mvnw.cmd verify
```

Để chạy một bản demo riêng bằng H2:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=test --server.port=8081"
```

Mở **http://localhost:8081/**. H2 dùng database trong bộ nhớ; dữ liệu mất khi dừng tiến trình. Profile `test` dùng `ddl-auto=create-drop`: chỉ dùng cho kiểm thử. Không trỏ profile này vào database có dữ liệu cần giữ.

Smoke test không thay thế kiểm thử nghiệp vụ. Query native Q11 của dashboard dùng hàm SQL Server và cần được xác nhận trên SQL Server. Các test nghiệp vụ cũ và tài liệu kiểm thử cá nhân tiếp tục được giữ cục bộ; chỉ smoke test TODO 32 và ảnh minh họa được thêm lại vào Git.

## Các màn hình chính

Các URL dưới đây dùng sau `http://localhost:8080`. `{id}` là ID có thật; thao tác thêm/sửa/xóa/xác nhận/hủy phải gửi bằng form/nút trên trang, không gõ endpoint POST vào thanh địa chỉ.

| Vai trò | URL | Chức năng |
|---|---|---|
| Guest | `/`, `/movies`, `/movies/{id}` | Danh sách phim, bộ lọc và lịch chiếu |
| Guest | `/login`, `/register` | Đăng nhập, đăng ký |
| Admin | `/admin` | Dashboard |
| Admin | `/admin/customers` | Khách hàng, tài khoản, khóa/mở khóa |
| Admin | `/admin/genres`, `/admin/movies` | Thể loại và phim |
| Admin | `/admin/rooms`, `/admin/showtimes` | Phòng và lịch chiếu |
| Admin | `/admin/bookings`, `/admin/bookings/{id}` | Đơn đặt vé, xác nhận/hủy |
| Admin | `/admin/reports` | Báo cáo theo khoảng ngày |
| Customer | `/customer/showtimes/{id}/seats` | Chọn 1–8 ghế, xem tổng tiền và đặt vé |
| Customer | `/customer/bookings`, `/customer/bookings/{id}` | Lịch sử và hủy đơn của mình |
| Customer | `/customer/profile`, `/customer/password` | Hồ sơ và đổi mật khẩu |
| Customer | `/movies/{id}` | Xem/thêm/sửa đánh giá khi đã xem phim đủ điều kiện |

Chưa đăng nhập vào khu vực bảo vệ sẽ được đưa về `/login`; sai vai trò trả 403. Đổi URL sang đơn của khách khác trả 404.

## Kiến trúc và dữ liệu

```text
controller -> service (interface + impl) -> repository -> entity
src/main/resources/templates   Giao diện Thymeleaf
src/main/resources/static      CSS
src/main/resources/application-local.properties.example  Cấu hình SQL mẫu
src/test/java/com/fu/cinema/FuCinemaTicketSystemApplicationTests.java  Smoke test
docs/images                    Ảnh màn hình TODO 32
```

Database gồm `accounts`, `customers`, `genres`, `movies`, `rooms`, `showtimes`, `bookings`, `tickets`, `reviews`. `customers.account_id` có FK và UNIQUE; `reviews` có khóa kép khách hàng/phim; Booking lưu Ticket bằng cascade trong transaction.

Các query Q1–Q12 nằm trong repository: tìm kiếm/phân trang, trùng lịch, ghế đã bán, chi tiết đơn, doanh thu, hủy hàng loạt, điểm đánh giá và thống kê dashboard. Đơn `CANCELLED` không giữ ghế. Báo cáo chỉ tính đơn `CONFIRMED` theo ngày đặt. Customer chỉ được hủy đơn của mình trước giờ chiếu ít nhất 2 giờ.

## Thành viên và phân công

Điền họ tên và MSSV trước khi nộp bài; các ô đang để trống theo yêu cầu của nhóm.

| Họ tên | MSSV | GitHub | Vai trò | Phạm vi phụ trách |
|---|---|---|---|---|
| | | [chuong123445](https://github.com/chuong123445) | Leader | I01–I03, I05–I07, I12, I14–I16, I19: nền tảng, dữ liệu/query, service contract, xác thực, khách hàng, đơn Admin, đặt vé, lịch sử, hồ sơ, tài liệu và phát hành |
| | | [huuha205](https://github.com/huuha205) | Thành viên | I04, I08–I11, I13, I17–I18: seed, thể loại/phim, phòng/suất chiếu/dashboard, trang chủ, doanh thu và đánh giá |

I01–I19 là mã kế hoạch; số issue thật trên GitHub có thể khác. Reviewer là người còn lại; tác giả không tự approve PR của mình.

## Ảnh minh họa

Ảnh được chụp trên bản demo với dữ liệu thử, không chứa mật khẩu SQL Server. Tên tài khoản, số lượng đơn và ngày trong ảnh có thể khác database trên máy người đọc.

![Trang chủ](docs/images/home.png)

![Dashboard Admin](docs/images/admin-dashboard.png)

![Sơ đồ ghế](docs/images/seat-selection.png)

![Báo cáo doanh thu](docs/images/revenue-report.png)

## Nghiệm thu và phát hành

1. Tác giả và reviewer chạy `mvn verify`; kiểm tra các chức năng Guest/Admin/Customer trên SQL Server.
2. Kiểm tra tạo/sửa trùng lịch, hủy suất và đơn, tranh ghế, quyền sở hữu đơn, độ tuổi và báo cáo. Xử lý các bug chặn release trước khi phát hành.
3. Review và merge PR tài liệu vào `develop`; tạo `release/1.0.0` từ develop đã tích hợp đầy đủ.
4. Review PR `release/1.0.0 -> main` theo teamwork và dùng merge commit.
5. Chỉ sau khi PR release được merge và đủ approval: gắn tag `v1.0.0`, tạo GitHub Release, rồi back-merge `main -> develop` qua PR.
6. Đối chiếu tên/MSSV và cập nhật mục lục trong [báo cáo Word](Report_Assignment1_HSF302.docx) đã bổ sung ảnh; cấp quyền Read cho giảng viên trước khi nộp.

README này không xác nhận tag/release đã được phát hành. Kiểm tra trạng thái thực tế tại [GitHub Releases](https://github.com/HSFHuuHaVC/Assignment1/releases).

Biên bản kiểm thử và các bước phát hành còn lại: [release.md](release.md).
