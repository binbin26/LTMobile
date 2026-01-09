package smart.study.planner.data.model

/**
 * Language options
 */
enum class Language {
    VI, // Tiếng Việt
    EN  // English
}

/**
 * Theme mode options
 */
enum class ThemeMode {
    LIGHT,  // Sáng
    DARK,   // Tối
    SYSTEM  // Theo hệ thống
}

/**
 * Calendar view mode
 */
enum class CalendarViewMode {
    MONTH,  // Tháng
    WEEK    // Tuần
}

/**
 * Task filter options
 */
enum class TaskFilter {
    DUE_TODAY,   // Đến hạn hôm nay
    DUE_LATER,   // Đến hạn sau
    ALL          // Tất cả
}

/**
 * Task sort options
 */
enum class TaskSortOption {
    DATE,       // Theo ngày
    PRIORITY,   // Theo độ ưu tiên
    CATEGORY,   // Theo danh mục
    TITLE       // Theo tiêu đề
}

/**
 * Sync status
 */
enum class SyncStatus {
    IDLE,           // Không có gì
    SYNCING,        // Đang đồng bộ
    SUCCESS,        // Thành công
    ERROR           // Lỗi
}

