# zsp — ERI ImGui Menu

Repository này dành cho **menu ImGui iOS ARM64** của dự án zsp, không liên quan đến `Menumod` (ERI Login Key).

## Nội dung

- `eri-imgui/README.md`: kiến trúc menu, hướng dẫn build và giới hạn kiểm thử.
- `eri-imgui/src.tar.xz.b64`: gói mã nguồn Objective-C++ / ImGui frontend được giải nén trong workflow.
- `eri-imgui/fix_overlay.py`: bản vá giao diện, tránh lớp UIKit cũ che menu và hiển thị tiêu đề tiếng Việt 7 màu.
- `.github/workflows/build-ios-imgui-rebuild.yml`: GitHub Actions biên dịch `ERIImGuiOverlay.dylib` cho ARM64 iPhoneOS (chưa ký).

## Trạng thái

GitHub Actions đã biên dịch thành công thư viện iOS ARM64. **Chưa kiểm thử trên iPhone**. Dylib chỉ là module giao diện; **không phải** bản thay thế hoàn chỉnh cho thư viện chức năng cũ. Nếu dùng trong IPA phải kiểm tra khả năng nạp dylib, bố trí thư viện và chữ ký mã trước khi phân phối.

Xem [workflow build](../../actions/workflows/build-ios-imgui-rebuild.yml) để tải artifact của lần chạy thành công.
