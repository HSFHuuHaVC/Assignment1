## Mô tả
<!-- Thay đổi gì và vì sao -->

Closes #

## Loại thay đổi
- [ ] feat  - [ ] fix  - [ ] refactor  - [ ] docs  - [ ] chore  - [ ] test

## Cách kiểm thử
1. Đăng nhập bằng ...
2. ...
Kết quả mong đợi: ...

## Ảnh chụp màn hình
<!-- Bắt buộc với thay đổi giao diện -->
<!-- Desktop: đính kèm ảnh thật, liệt kê TC-D đã chạy và phần chưa kiểm tra. -->

## Checklist
- [ ] Tiêu đề PR theo Conventional Commits
- [ ] Đã rebase/merge develop mới nhất
- [ ] `mvn verify` → `BUILD SUCCESS` trên máy mình
- [ ] Đã tự test các trường hợp chính và trường hợp lỗi
- [ ] Controller không gọi repository; nghiệp vụ nằm ở service
- [ ] Desktop: Facade chỉ gọi Service; UI dùng record; mapping trong transaction
- [ ] Desktop: Facade ghi kiểm tra ADMIN; gọi Facade qua FxAsync, UI cập nhật trên FX thread
- [ ] Không chứa mật khẩu/thông tin nhạy cảm
- [ ] Đã cập nhật README (nếu cần)
