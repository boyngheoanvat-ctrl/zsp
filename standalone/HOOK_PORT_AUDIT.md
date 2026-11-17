# ERI Standalone — kiểm tra 4 hook gốc

Báo cáo kiểm tra **file `zsmvn.dylib` ARM64 thực tế**, SHA-256 `2cd3a94775d118ce43ed739b55b57d07fe163057bf4bb864ec552cc75fd25fd4`. Địa chỉ dưới đây là **địa chỉ VM của mã trong dylib cũ**, KHÔNG PHẢI offset trong `UnityFramework`/game và không thể dùng trực tiếp để hook bản mới.

| Nhóm | Hàm đã xác minh từ bảng symbol ARM64 | Địa chỉ trong dylib cũ | Đã tái triển khai độc lập? |
| --- | --- | --- | --- |
| Tốc độ xe | `ZSM_GetRealVehiclePhysicsCaps`, `ZSM_ApplyForwardVelocityFinishAssist` | `0x4014c`, `0x479d8` | **Chưa** |
| Xe/skin | `ZSM_ApplyVehicleAndSkinMod`, `ZSM_ApplyUnlockAllGarageVehiclesAndSkins` | `0x21f44`, `0x23c24` | **Chưa** |
| Replay bot | `ZSM_UpdateGroup5ShadowReplayBot`, `ZSM_ReleaseActiveShadowReplayBot` | `0x27ee8`, `0x49f14` | **Chưa** |
| Tự động ghép trận | `ZSM_UpdateAutoMatchAndRoom`, `ZSM_QueryIsMatchingNow` | `0x29fa8`, `0x4b950` | **Chưa** |

## Kết quả kỹ thuật

- Đây là các hàm *local* và sử dụng `ZSMRuntimeChain`, nhiều biến trạng thái cùng hàm hỗ trợ khác bên trong dylib cũ; không thể gọi chúng từ `ERIStandalone.dylib` khi đã bỏ dylib cũ.
- V2 đã build thành công **một dylib độc lập**, có Dear ImGui, icon ERI, 7 màu, quét tên lớp IL2CPP chỉ đọc. Điều khiển thử nghiệm `UnityEngine.Time.timeScale` (0,5–2×) không phải tăng tốc độ xe.
- Không có bản `UnityFramework`/IPA và `dump.cs` tương ứng **đúng phiên bản ZingSpeed** trong tập dữ liệu được kiểm tra để xác thực mapping method/field và cơ chế hoạt động trong game.
- Bốn hook phải được tái triển khai bằng code của bản mới và kiểm thử trên thiết bị với đúng phiên bản game. Chỉ đưa tên chức năng lên ImGui không phải là tích hợp.

**Không đánh dấu các hook trên là hoàn thành; không tuyên bố hoạt động nếu chưa kiểm thử.** Mã nguồn bản độc lập và workflow ARM64 vẫn nằm trong `standalone/`.
