# LTMobile - Ứng dụng Quản Lý Sự Kiện Học Tập

Ứng dụng Android quản lý sự kiện học tập được xây dựng với kiến trúc MVVM, Jetpack Compose, Room Database và Firebase Realtime Database.

## 🏗️ Kiến trúc

Ứng dụng sử dụng **MVVM (Model-View-ViewModel)** architecture với các layer:

```
app/
├── data/              # Data layer
│   ├── local/         # Room Database (offline)
│   ├── remote/        # Firebase (online)
│   ├── repository/    # Repository implementations
│   └── model/         # Data models
├── domain/            # Domain layer
│   ├── repository/   # Repository interfaces
│   └── usecase/       # Use cases (business logic)
├── presentation/      # Presentation layer
│   ├── screens/       # Jetpack Compose screens
│   ├── components/    # Reusable UI components
│   ├── viewmodel/     # ViewModels
│   └── navigation/    # Navigation setup
└── di/                # Dependency Injection (Hilt)
```

## 🛠️ Công nghệ sử dụng

- **UI**: Jetpack Compose với Material Design 3
- **Architecture**: MVVM với Clean Architecture principles
- **Database**: 
  - Room Database (local/offline)
  - Firebase Realtime Database (online/sync)
- **Dependency Injection**: Hilt
- **Asynchronous**: Kotlin Coroutines & Flow
- **Navigation**: Jetpack Navigation Compose

## 📋 Tính năng

- ✅ Quản lý sự kiện học tập (Thêm/Sửa/Xóa)
- ✅ Xem danh sách sự kiện theo ngày
- ✅ Hiển thị sự kiện sắp đến hạn
- ✅ Hỗ trợ offline (hoạt động không cần mạng)
- ✅ Đồng bộ tự động với Firebase khi có kết nối
- ✅ Phân loại sự kiện (Học tập, Bài tập, Kiểm tra, Seminar, Khác)

## 🚀 Hướng dẫn Setup

### 1. Yêu cầu

- Android Studio Hedgehog | 2023.1.1 hoặc mới hơn
- JDK 11 hoặc mới hơn
- Android SDK 29+ (minSdk: 29, targetSdk: 36)
- Firebase project

### 2. Cài đặt Firebase

1. Tạo project mới trên [Firebase Console](https://console.firebase.google.com/)
2. Thêm Android app vào project với package name: `com.example.ltmobile`
3. Tải file `google-services.json`
4. Đặt file `google-services.json` vào thư mục `app/`

### 3. Cấu hình Firebase Realtime Database

1. Vào Firebase Console > Realtime Database
2. Tạo database mới (chọn location)
3. Cấu hình Rules:

```json
{
  "rules": {
    "users": {
      "$uid": {
        "events": {
          ".read": "$uid === auth.uid",
          ".write": "$uid === auth.uid"
        }
      }
    }
  }
}
```

### 4. Build và chạy

```bash
# Clone repository (nếu có)
git clone <repository-url>
cd LTMobile

# Sync Gradle files
./gradlew build

# Chạy trên emulator/device
./gradlew installDebug
```

## 📱 Cấu trúc màn hình

1. **Trang Chủ**: Hiển thị sự kiện sắp đến hạn
2. **Lịch**: Xem tất cả sự kiện theo tháng
3. **Thêm Mới**: Tạo sự kiện mới
4. **Nhiệm vụ**: Danh sách tất cả nhiệm vụ/sự kiện
5. **Tài khoản**: Quản lý tài khoản (chưa implement)

## 🔄 Chiến lược đồng bộ (Sync Strategy)

### Offline-First Architecture

1. **Khi online**:
   - Đọc/ghi từ Firebase + cache vào Room
   - Đánh dấu events là `isSynced = true`

2. **Khi offline**:
   - Đọc/ghi từ Room database
   - Đánh dấu events là `isSynced = false`

3. **Khi reconnect**:
   - Tự động đồng bộ pending changes (`isSynced = false`) lên Firebase
   - Pull latest data từ Firebase về local

### Flow xử lý

```
User Action → ViewModel → UseCase → Repository
                                    ↓
                            ┌───────┴───────┐
                            ↓               ↓
                      Local (Room)    Firebase (Online)
                            ↓               ↓
                            └───────┬───────┘
                                    ↓
                              Sync Strategy
```

## 🧪 Testing

```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest
```

## 📝 Code Quality

- ✅ Proper error handling với try-catch
- ✅ Coroutines với đúng Dispatchers (IO/Main)
- ✅ Flow cho reactive data streams
- ✅ Type-safe navigation
- ✅ Compose best practices (không có lỗi measurement)
- ✅ Separation of concerns (MVVM)

## 🔒 Security Notes

- Firebase Authentication cần được setup để bảo mật dữ liệu
- ProGuard rules đã được cấu hình cho Firebase và Room
- Không hardcode sensitive data

## 📚 Tài liệu tham khảo

- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [Firebase Realtime Database](https://firebase.google.com/docs/database)
- [Hilt Dependency Injection](https://developer.android.com/training/dependency-injection/hilt-android)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)

## 🤝 Đóng góp

Mọi đóng góp đều được chào đón! Vui lòng tạo issue hoặc pull request.

## 📄 License

[Thêm license của bạn ở đây]

---

**Lưu ý**: Đây là project mẫu với đầy đủ kiến trúc MVVM. Để sử dụng trong production, cần:
- Setup Firebase Authentication
- Implement proper error handling UI
- Thêm unit tests và integration tests
- Cải thiện UI/UX theo design requirements
- Thêm date/time pickers cho AddEventScreen
- Implement event details screen
- Thêm notification/reminder functionality

