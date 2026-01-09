# Script để di chuyển package từ com.example.ltmobile sang smart.study.planner

$sourceDir = "app\src\main\java\com\example\ltmobile"
$targetDir = "app\src\main\java\smart\study\planner"

Write-Host "Bắt đầu di chuyển package..." -ForegroundColor Green

# Kiểm tra thư mục nguồn
if (-not (Test-Path $sourceDir)) {
    Write-Host "Thư mục nguồn không tồn tại: $sourceDir" -ForegroundColor Red
    exit 1
}

# Tạo thư mục đích
Write-Host "Tạo thư mục đích: $targetDir" -ForegroundColor Yellow
New-Item -ItemType Directory -Force -Path $targetDir | Out-Null

# Di chuyển tất cả files và thư mục con
Write-Host "Đang di chuyển files..." -ForegroundColor Yellow
Get-ChildItem -Path $sourceDir -Recurse | ForEach-Object {
    $relativePath = $_.FullName.Substring($sourceDir.Length + 1)
    $targetPath = Join-Path $targetDir $relativePath
    
    if ($_.PSIsContainer) {
        # Tạo thư mục
        New-Item -ItemType Directory -Force -Path $targetPath | Out-Null
    } else {
        # Di chuyển file
        $targetFileDir = Split-Path $targetPath -Parent
        New-Item -ItemType Directory -Force -Path $targetFileDir | Out-Null
        Move-Item -Path $_.FullName -Destination $targetPath -Force
        Write-Host "  Di chuyển: $relativePath" -ForegroundColor Gray
    }
}

# Xóa thư mục cũ nếu rỗng
Write-Host "Đang xóa thư mục cũ..." -ForegroundColor Yellow
if (Test-Path $sourceDir) {
    $remainingFiles = Get-ChildItem -Path $sourceDir -Recurse -File
    if ($remainingFiles.Count -eq 0) {
        Remove-Item -Path "app\src\main\java\com" -Recurse -Force -ErrorAction SilentlyContinue
        Write-Host "  Đã xóa thư mục cũ" -ForegroundColor Gray
    } else {
        Write-Host "  Cảnh báo: Vẫn còn files trong thư mục cũ" -ForegroundColor Yellow
    }
}

Write-Host "`nHoàn thành! Vui lòng:" -ForegroundColor Green
Write-Host "1. Sync Gradle Files trong Android Studio" -ForegroundColor Cyan
Write-Host "2. Clean và Rebuild Project" -ForegroundColor Cyan
Write-Host "3. Kiểm tra build thành công" -ForegroundColor Cyan

