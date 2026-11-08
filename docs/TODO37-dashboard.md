# TODO 37 — Desktop Dashboard

Dashboard nằm tại `/desktop/fxml/dashboard.fxml`. Khi tích hợp với TODO 33–34,
nạp FXML bằng `FXMLLoader` với `setControllerFactory(applicationContext::getBean)`
và đặt root vào vùng nội dung của cửa sổ chính.

Màn hình dùng cùng bốn service với `/admin` của web: số phim đang chiếu,
số khách hàng, số đơn PENDING và số suất chiếu hôm nay theo phòng.
Các truy vấn chạy trong `Task` trên virtual thread; cập nhật giao diện trên FX thread.
Nút Làm mới bị vô hiệu trong lúc tải để tránh truy vấn chồng nhau.

Mỗi lần màn hình được gắn vào Scene, Dashboard tải dữ liệu và bắt đầu Timeline
60 giây. Khi rời Scene, Timeline dừng và tác vụ đang tải bị huỷ. Khi tải lỗi,
giữ dữ liệu đã hiển thị và cho phép làm mới để thử lại.

## Kiểm thử tự động

```shell
mvn -Dtest=DashboardFacadeTest test
```

Kiểm tra ánh xạ đủ ba KPI, số suất theo phòng, ngày không có suất và việc truy vấn
lại dữ liệu khi làm mới.

## Kiểm tra giao diện sau khi tích hợp TODO 33–34

- Mở Dashboard, đối chiếu ba KPI và biểu đồ với `/admin` trên cùng cơ sở dữ liệu.
- Tạo suất chiếu hôm nay trên web, bấm Làm mới và kiểm tra cột phòng tương ứng.
- Tạo thêm suất chiếu trên web, chờ 60 giây và kiểm tra cập nhật tự động.
- Chuyển sang màn hình khác, kiểm tra không còn truy vấn định kỳ của Dashboard.
- Quay lại Dashboard, kiểm tra tải dữ liệu ngay và tiếp tục tự cập nhật.
- Với ngày không có suất, kiểm tra biểu đồ rỗng và thông báo tương ứng.
- Ngắt kết nối cơ sở dữ liệu, kiểm tra thông báo lỗi và nút Làm mới hoạt động lại.

Các bước giao diện cần khung JavaFX/điều hướng của TODO 33–34; chưa được chạy
trong nhánh TODO 37 này.
