# Kiểm thử issue 5

Phạm vi: TODO 11 phần contract và tiện ích dùng chung, cùng TODO 12.
Các service implementation nghiệp vụ được thực hiện trong issue chức năng;
không có implementation giả hoặc controller tạm trong production.

## Hợp đồng dùng chung

- Có 9 interface, gồm ReviewService để hỗ trợ phần bonus khi cần.
- DTO và form được khai báo cùng interface để mỗi commit tự biên dịch được.
- SessionUser không chứa mật khẩu; khóa session là SessionUtils.CURRENT_USER.
- GlobalModelAttributes cung cấp người dùng hiện tại cho menu của các view.
- ServiceImpl dùng constructor injection, @Transactional(readOnly = true)
  ở class và @Transactional ở method ghi dữ liệu.

## Chạy kiểm thử

```powershell
mvn verify
```

ContractValidationTests kiểm tra validation và thông tin session;
SharedHelperTests kiểm tra session, tuổi, ngày giờ và URL phân trang có bộ lọc.
ErrorHandlingTests kiểm tra 403/404/500, sai kiểu ID, tài nguyên không tồn tại,
giữ mã 405, servlet sendError và trang lỗi mặc định, menu theo session,
redirect về cùng website, flash message và việc escape HTML.

Fixture nằm trong src/test, bật bằng profile issue5-fixture và không được
đóng gói vào ứng dụng. Không dùng các URL fixture như chức năng của website.
Các URL /movies/{id} thực tế được bổ sung trong issue trang phim.

Test Q11 của issue 3 vẫn dùng SQL Server riêng theo testing-issue-3.md;
nếu không thiết lập biến kết nối SQL Server thì test đó được báo SKIPPED.

## Quy ước xử lý lỗi

- ResourceNotFoundException, NoResourceFoundException và ID sai kiểu: 404.
- AccessDeniedException: 403. Các lỗi MVC có status xác định giữ nguyên status.
- BusinessException không được controller bắt: flash error và redirect về
  Referer cùng origin; Referer thiếu, lỗi hoặc trỏ ra website khác dùng /.
- Exception bất ngờ: log chi tiết ở server và trang 500 với thông báo chung.
- Các trang lỗi không hiển thị exception, message nội bộ hoặc stack trace.
- Lỗi form cần controller bắt và trả lại form cùng dữ liệu đã nhập;
  handler dùng cho các thao tác không cần hiển thị lại form.
