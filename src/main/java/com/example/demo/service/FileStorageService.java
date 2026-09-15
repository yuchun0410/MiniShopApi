package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

// 專門負責檔案「實際落地硬碟」的讀寫，跟資料庫操作刻意分開成獨立的 Service。
// 這樣分開的原因：硬碟寫入不受 @Transactional 保護——DB 那邊失敗會自動 rollback，
// 但已經寫進硬碟的檔案不會自動消失，所以呼叫端（ProductServiceImpl）如果在 DB 那步失敗，
// 要自己在 catch 區塊呼叫 delete() 手動補刪，避免留下沒有對應資料庫紀錄的「孤兒檔案」。
@Service
public class FileStorageService {

    private final Path uploadDir;

    public FileStorageService(@Value("${app.upload.dir:uploads/products}") String uploadDirPath) {
        this.uploadDir = Paths.get(uploadDirPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new IllegalStateException("無法建立上傳目錄: " + this.uploadDir, e);
        }
    }

    // 把上傳的檔案寫進硬碟，回傳實際存放的完整路徑（要存進 DB 的 file_path）
    public String store(MultipartFile file, String storedFileName) {
        try (InputStream in = file.getInputStream()) {
            Path target = uploadDir.resolve(storedFileName);
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString();
        } catch (IOException e) {
            throw new IllegalStateException("檔案儲存失敗: " + file.getOriginalFilename(), e);
        }
    }

    // 產生不會重複、又保留副檔名的儲存檔名（避免不同使用者上傳同名檔案互相覆蓋）
    public String generateStoredFileName(String originalFileName) {
        String ext = "";
        int dotIndex = originalFileName == null ? -1 : originalFileName.lastIndexOf('.');
        if (dotIndex >= 0) {
            ext = originalFileName.substring(dotIndex);
        }
        return UUID.randomUUID() + ext;
    }

    // 讀取硬碟上實際的檔案內容，給附件下載/預覽端點用
    public byte[] load(String filePath) {
        try {
            return Files.readAllBytes(Paths.get(filePath));
        } catch (IOException e) {
            throw new IllegalStateException("讀取檔案失敗: " + filePath, e);
        }
    }

    // 補償動作：DB 交易失敗時，把已經寫入硬碟的檔案刪掉，避免孤兒檔案殘留
    public void delete(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            // 刪除失敗只記錄，不要讓它蓋掉原本真正的錯誤
            System.err.println("補償刪除檔案失敗: " + filePath + " - " + e.getMessage());
        }
    }
}
