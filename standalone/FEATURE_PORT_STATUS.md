# ERI Standalone V2 — trạng thái chuyển chức năng ZSM

Mục tiêu: **một** file `ERIStandalone.dylib` iOS ARM64, không nạp/link `zsmvn.dylib` hoặc `ERIImGuiOverlay.dylib`.

## Phần đã viết
- Dò IL2CPP đã nạp và tìm `UnityEngine.Time::get_timeScale()` / `set_timeScale(System.Single)` qua export `il2cpp_resolve_icall`.
- Bộ điều khiển thử nghiệm thời gian Unity trong khoảng 0.5–2.0x khi icall có sẵn; lưu rồi khôi phục giá trị ban đầu lúc tắt hoặc app ra nền. **Không phải tốc độ xe**.
- Nút chủ động quét lớp IL2CPP *chỉ đọc* và báo có/không xác nhận được các tên lớp `VehicleMgr`, `GarageMgr`, `MatchMgr`, `QuickMatchMdl`, `MatchVideoMdl`. Không scan bộ nhớ lạ, không tạo nút giả.
- Giao diện Dear ImGui/Metal và các tùy chỉnh UI từ V1 giữ nguyên.

## Chưa triển khai từ mã máy cũ

| Nhóm | Dấu vết trong `zsmvn.dylib` gốc | Trạng thái |
|---|---|---|
| Tốc độ xe, boost | `ZSM_fSpeedMultiplier`, `ZSM_GetRealVehiclePhysicsCaps`, `VehicleMgr` | Chưa port vật lý xe; Unity TimeScale không thay thế |
| Xe/skin/garage | `ZSM_ApplyVehicleAndSkinMod`, `ZSM_ApplyUnlockAllGarageVehiclesAndSkins`, `GarageMgr` | Chưa port |
| Replay bot | `ZSM_UpdateGroup5ShadowReplayBot`, `MatchVideoMdl` | Chưa port |
| Tự động ghép trận | `ZSM_UpdateAutoMatchAndRoom`, `MatchMgr`, `QuickMatchMdl` | Chưa port |

Các hàm này nằm trong mã ARM64 cũ, có cấu trúc dữ liệu/offset phụ thuộc phiên bản game. **Không thể chứng minh cùng chức năng chỉ bằng việc sao chép tên symbol hoặc cấu hình;** phải có phương thức gọi và trường IL2CPP đã xác minh cho đúng phiên bản game, sau đó kiểm thử thực tế.

## Kiểm tra

Chạy workflow `Build ERI standalone one-dylib ARM64`. Trước khi sử dụng, xem tab `Tinh nang game` để kiểm tra IL2CPP có mặt và API Unity Time có thể nạp. Đây không phải bằng chứng an toàn khi chạy trong game thật. Bản này không cần dylib cũ và không liên quan tới `Menumod`.
