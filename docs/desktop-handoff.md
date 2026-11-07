# Bàn giao nền tảng desktop Assignment 2

Ngày kiểm tra: 07/11/2026. Owner: Chương. Baseline: `origin/develop` tại `89c2069`. Phạm vi: I20–I24, TODO 33–36; phần README/test/tài liệu của I34. Không thực hiện release, tag, merge main/develop hoặc tạo issue/PR.

## Nhánh để tạo PR

Các nhánh kế thừa nhau nhằm giữ mỗi PR chỉ chứa một phần việc khi chọn base dưới đây. Sau khi merge PR trước, cập nhật nhánh kế tiếp và chuyển base về `develop` nếu phù hợp với cách merge. Nếu squash merge làm GitHub vẫn hiện commit cũ, đồng bộ các nhánh phụ thuộc trước khi merge tiếp; không merge mù toàn bộ diff đã được review ở PR trước.

| Mã | Head | Base ban đầu | Phạm vi |
|---|---|---|---|
| I20 | `chore/20-desktop-repo-templates` | `develop` | Template issue/PR và CODEOWNERS |
| I21 | `feature/21-desktop-bootstrap` | `chore/20-desktop-repo-templates` | JavaFX, hai main, profile, session sớm, test bootstrap |
| I23 | `feature/23-desktop-utilities` | `feature/21-desktop-bootstrap` | FxAsync, AlertUtil, FxValidator, MoneyUtil, PageResult |
| I22 | `feature/22-desktop-navigation` | `feature/23-desktop-utilities` | SceneManager/View, main FXML/CSS, MainController, test UI |
| I24 | `feature/24-desktop-login` | `feature/22-desktop-navigation` | AuthFacade, LoginController/FXML, startup tới login, facade/UI tests |
| I34 | `docs/34-desktop-release` | `feature/24-desktop-login` | README, ảnh thật, biên bản, phụ lục Word và test đóng process; **không phát hành** |

Các commit theo convention có sẵn của Assignment 1: `chore(github)`, `feat(desktop)`, `test(desktop)`, `docs(readme)` và mô tả phần việc. Không gắn số `Refs #...` giả khi chưa có số issue được xác nhận. Tên nhánh có số mã kế hoạch để dễ phân biệt, ví dụ feature/21-desktop-bootstrap; số này không phải xác nhận số issue GitHub.

## Nền tảng để Hà Quyền Long dùng tiếp

- F0: dùng bootstrap/profile/session và các tiện ích đã có trên `feature/23-desktop-utilities`. Nên lấy nhánh bàn giao cuối cùng sau khi Leader hợp nhất qua PR để tránh tự copy file.
- F1: đăng nhập/menu hoạt động trên `feature/24-desktop-login`. Mỗi người chỉ thêm màn hình/Facade/model/test của mình, không tạo lại framework nền tảng.
- Controller FXML: `@Component @Scope("prototype")`, constructor injection; nạp bằng `SceneManager` đã có controller factory.
- Giữ tên FXML trong enum `View`: `dashboard.fxml`, `genre.fxml`, `movie.fxml`, `movie-dialog.fxml`, `schedule.fxml`, `counter-sale.fxml`, `booking.fxml`, `report.fxml`.
- `SceneManager.showIn` hiện thông báo khi chưa có resource. Thêm FXML thật đúng đường dẫn là được nạp; trước nghiệm thu toàn bài phải có đủ màn hình và không còn đường đi vào thông báo tạm.
- Facade chỉ gọi Service; map entity sang record trong transaction đọc; method ghi dùng transaction ghi và gọi `DesktopSession.requireAdmin()`. Controller/component không import entity hoặc Repository.
- Lấy giá trị control trên FX thread, snapshot biến cục bộ trước khi truyền vào `FxAsync`. Không đọc control trong `Task.call()`.
- `FxAsync.run` có callback thành công/thất bại trên FX thread, trả Task; `runVoid` dùng cho thao tác không trả giá trị. Control đã bind disable thì truyền `busy=null`, đưa running state vào binding; không gọi setDisable lên property đã bind.
- `FxValidator.validate(form)` dùng Form DTO cũ; `mark(errors, fields)` tô đỏ và gắn tooltip. `PageResult` dùng page index từ 0, dữ liệu bất biến, luôn có ít nhất một trang hiển thị.
- `MoneyUtil` không chia sẻ NumberFormat mutable giữa các Task. `AlertUtil` unwrap lỗi async, giữ message nghiệp vụ và che chi tiết kết nối/stack trace khỏi UI.
- Test mỗi miền trong file riêng dưới `src/test/java/com/fu/cinema/desktop/`; thư mục này đã được bỏ ignore. Chưa cần thay đổi seed chung để phát triển một màn hình.
- CSS có vùng Chương/Hà/Quyền/Long. Chỉ sửa selector của miền mình; không format lại toàn bộ file. Chọn tên record riêng khi hai màn hình cần dữ liệu khác nhau.

## Kết quả và phạm vi kiểm tra

| Kiểm tra | Kết quả |
|---|---|
| Baseline repository trước sửa | `mvn -o -B -ntp verify` pass, 1 smoke test |
| Bootstrap/context/session | Pass: Spring NONE không web context; không cần toolkit khi test H2; quyền mặc định bị chặn, ADMIN/logout, từ chối CUSTOMER |
| Tiện ích | Pass: mapping page rỗng/bất biến, validation MovieForm, phân loại lỗi, định dạng VNĐ |
| AuthFacade H2 | 4 test pass: ADMIN, CUSTOMER, sai mật khẩu, ADMIN LOCKED; dùng AuthService/seed thật |
| JavaFX native | FXML/controller prototype, cửa sổ 1200×720 và 1000×640, binding/ảnh login/main; Task success/failure/cancel và callback đúng thread |
| Luồng login JavaFX + H2 | Pass: trống bị disable, sai password/CUSTOMER bị từ chối, ADMIN vào main, logout xoá session và về login |
| Đóng process | Test child JVM chạy ứng dụng thật, đóng Stage cuối, đóng Spring/Hikari và process tự kết thúc; không có Tomcat |
| Chạy đồng thời SQL Server | Web cổng 18082 và desktop dùng `local` cùng `FUCinemaTicketDB`; cả hai kết nối/khởi động; desktop không Tomcat |
| Web Playwright | Trang chủ và login; ADMIN login → dashboard, đơn, báo cáo HTTP 200; không tràn ngang ở 1280/390; không có console/network error ở các trang kiểm tra cuối |
| Kiểm tra trực tiếp desktop bằng computer-use | Công cụ không liệt kê cửa sổ JavaFX của process chạy từ tool session; không xác nhận thao tác tay qua công cụ này. Bằng chứng UI là test JavaFX và ảnh snapshot thật |

Lệnh cuối dùng test được commit: `mvn -o -B -ntp -Ddesktop.ui.tests=true clean verify` → **BUILD SUCCESS, 16 test, 0 failure, 0 error, 0 skip**. Mặc định không có cờ sẽ skip 4 test UI trên máy không có desktop.

**Test legacy ngoài repository:** thử khôi phục các test/fixture cũ đang bị ignore trên máy để kiểm tra rộng hơn. Tổng lượt chạy cùng desktop: 138 test, 6 failure, 67 error, 4 skip, chủ yếu do dữ liệu seed trùng fixture. Chạy lại `AdminBookingPagesTests,SeatSelectionPageTests` trên baseline `89c2069` chưa sửa cũng lỗi unique `rooms.room_name='Room 01'`. Không commit các test legacy hay sửa backend để ép chúng pass. Không coi lượt kiểm tra này thành công; clone sạch không có các file bị ignore đó.

## Checklist bàn giao và giới hạn

- Đã triển khai code nền tảng TODO 33–36; không thay entity/service/Repository backend.
- Đã chuẩn bị template/CODEOWNERS. Chưa thao tác Settings của GitHub: quyền thành viên, branch rules, board, milestones và labels phải đối chiếu/cấu hình trên repo thật. Không tạo issue/PR vì nhóm sẽ tạo sau.
- TC-D02/D03 và phần login/logout của TC-D04/D20 được kiểm tra bằng H2 + JavaFX; account LOCKED được test qua service thật. TC-D01 startup/không Tomcat và shutdown được xác minh riêng; không kết luận dashboard của TC-D04 đúng khi chưa có màn hình.
- TODO 37–45, TC-D05–D19 liên quan nghiệp vụ desktop và TC-D16 tranh chấp web/desktop chưa thực hiện trong phạm vi Leader. Không tick chúng từ việc web cũ vẫn hoạt động.
- Có hai màn hình thật, chưa đủ yêu cầu sáu màn hình của toàn Assignment 2; ảnh các chức năng khác sẽ do owner cung cấp sau.
- Phụ lục Word chỉ ghi nền tảng, cách chạy và kiểm tra đã thực hiện; không thay báo cáo hoàn chỉnh của cả nhóm.
- Không release/tag hoặc đổi version trong đợt này theo yêu cầu người dùng.
