package com.panghu.food.utils;


import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * 文件处理工具类
 */
public class FileUtils {

    // 允许的图片格式
    private static final String[] ALLOWED_IMAGE_EXTENSIONS = {"jpg", "jpeg", "png"};

    // 最大文件大小(2MB)
    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024;

    /**
     * 验证图片文件
     */
    public static void validateImageFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("上传文件不能为空");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IOException("文件大小不能超过2MB");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !isAllowedExtension(originalFilename)) {
            throw new IOException("不支持的文件格式，仅允许jpg、jpeg、png");
        }
    }

    /**
     * 生成唯一文件名
     */
    public static String generateFileName(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
        return UUID.randomUUID().toString() + "." + extension;
    }

    /**
     * 保存文件到指定目录
     */
    public static void saveFile(MultipartFile file, String basePath, String fileName) throws IOException {
        File directory = new File(basePath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File destFile = new File(directory, fileName);
        file.transferTo(destFile);
    }

    /**
     * 检查文件扩展名是否允许
     */
    private static boolean isAllowedExtension(String filename) {
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        for (String allowed : ALLOWED_IMAGE_EXTENSIONS) {
            if (allowed.equals(extension)) {
                return true;
            }
        }
        return false;
    }
}
