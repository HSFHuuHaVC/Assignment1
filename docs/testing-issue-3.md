# Kiểm thử issue #3

Phạm vi: TODO 4–7, 9–10; enum, mapping, repository, Q1–Q12,
DTO/projection, khóa suất chiếu và bean BCrypt dùng chung.

## Bộ kiểm thử mặc định

Chạy từ thư mục chứa pom.xml:

```powershell
mvn verify
```

Bộ mặc định dùng H2 theo profile test, không cần SQL Server. Bao gồm:
unique/FK, enum STRING, quan hệ LAZY, ReviewId/MapsId, cascade/orphanRemoval;
derived query và EntityGraph; Q1–Q10, Q12; phân trang/count;
biên chồng giờ; ghế của đơn đã hủy; doanh thu theo BookingDate và CONFIRMED;
bulk update flush/clear và tính lặp lại an toàn.

Q11 dùng GETDATE của SQL Server và được tách riêng. Khi chưa thiết lập biến
môi trường dưới đây, SqlServerQueryTests được báo SKIPPED. H2 không chứng minh
Q11 chạy được trên SQL Server.

## Kiểm thử Q11 trên SQL Server

Tạo database riêng tên FUCinemaIssue3Test và cấp tài khoản kiểm thử quyền
tạo/xóa bảng trong database này. Test dùng create-drop và không được trỏ
vào database dự án hoặc database chứa dữ liệu cần giữ.

Thiết lập trong phiên PowerShell hiện tại, thay giá trị kết nối bằng cấu hình
máy mình; không lưu mật khẩu vào Git:

```powershell
$env:CINEMA_SQLSERVER_TEST_URL = 'jdbc:sqlserver://localhost:1433;databaseName=FUCinemaIssue3Test;encrypt=true;trustServerCertificate=true'
$env:CINEMA_SQLSERVER_TEST_USERNAME = '<tai-khoan-kiem-thu>'
$env:CINEMA_SQLSERVER_TEST_PASSWORD = '<mat-khau-kiem-thu>'
mvn '-Dtest=SqlServerQueryTests' test
```

Test từ chối URL không có databaseName=FUCinemaIssue3Test. JPA tạo schema
từ entity; test chạy method native thực tế và kiểm tra interface projection:
chỉ OPEN hôm nay theo ngày của SQL Server, hai biên ngày, loại CANCELLED,
phòng không có suất vẫn trả 0, kiểu Long và thứ tự tên phòng.

## Lưu ý cho phần service tiếp theo

- Gọi findByIdForUpdate trong transaction của thao tác đặt vé, rồi kiểm tra
  lại ghế bằng findSoldSeatCodes trước khi lưu. Không đặt unique vô điều kiện
  cho (showtime_id, seat_code), vì vé của đơn CANCELLED được giữ để tra lịch sử.
- Khoảng ngày trong query dùng [from, to). Bộ lọc theo ngày của giao diện cần
  chuyển EndDate thành đầu ngày kế tiếp để bao gồm trọn ngày cuối.
- MovieRepository.search nhận danh sách trạng thái cụ thể; để bỏ bộ lọc
  trạng thái, truyền tất cả MovieStatus. Danh sách rỗng trả về rỗng.
- Q9 có transaction, flushAutomatically và clearAutomatically. Entity đã tải
  trước bulk update sẽ bị detach; cần đọc lại để lấy trạng thái mới.
- PasswordConfig cung cấp PasswordEncoder duy nhất cho seed và auth; không
  tạo thêm bean encoder ở các issue sau.
- Tính hợp lệ nghiệp vụ (tuổi, mật khẩu, thời gian, giá, quyền truy cập) được
  triển khai trong các issue service tương ứng.
