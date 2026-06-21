package com.tooling.asset;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/file")
public class FileController {

    private static final Set<String> ALLOWED_IMAGE_TYPES = new HashSet<>(Arrays.asList(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/webp",
            "image/bmp"
    ));

    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp"
    ));

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private static final byte[][] IMAGE_MAGIC_JPEG = new byte[][]{
            new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}
    };
    private static final byte[][] IMAGE_MAGIC_PNG = new byte[][]{
            new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}
    };
    private static final byte[][] IMAGE_MAGIC_GIF = new byte[][]{
            new byte[]{0x47, 0x49, 0x46}
    };
    private static final byte[][] IMAGE_MAGIC_BMP = new byte[][]{
            new byte[]{0x42, 0x4D}
    };
    private static final byte[][] IMAGE_MAGIC_WEBP = new byte[][]{
            new byte[]{0x52, 0x49, 0x46, 0x46}
    };

    @Value("${file.upload-dir}")
    private String uploadDir;

    private boolean matchesMagic(byte[] header, byte[][] magicList) {
        for (byte[] magic : magicList) {
            if (header.length < magic.length) continue;
            boolean match = true;
            for (int i = 0; i < magic.length; i++) {
                if (header[i] != magic[i]) {
                    match = false;
                    break;
                }
            }
            if (match) return true;
        }
        return false;
    }

    private boolean isImageContent(byte[] header) {
        return matchesMagic(header, IMAGE_MAGIC_JPEG)
                || matchesMagic(header, IMAGE_MAGIC_PNG)
                || matchesMagic(header, IMAGE_MAGIC_GIF)
                || matchesMagic(header, IMAGE_MAGIC_BMP)
                || matchesMagic(header, IMAGE_MAGIC_WEBP);
    }

    @PostMapping("/upload")
    public Result<String> upload(@RequestParam MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return Result.fail("请选择要上传的文件");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            return Result.fail("文件名不能为空");
        }

        String lowerName = originalFilename.toLowerCase(Locale.ROOT);
        boolean hasValidExt = false;
        String extension = "";
        for (String ext : ALLOWED_EXTENSIONS) {
            if (lowerName.endsWith(ext)) {
                hasValidExt = true;
                extension = ext;
                break;
            }
        }
        if (!hasValidExt) {
            return Result.fail("不支持的文件扩展名，仅允许：" + String.join("、", ALLOWED_EXTENSIONS));
        }

        String contentType = file.getContentType();
        if (contentType != null && !contentType.isEmpty()) {
            String lowerCt = contentType.toLowerCase(Locale.ROOT);
            if (!ALLOWED_IMAGE_TYPES.contains(lowerCt)) {
                return Result.fail("不支持的文件类型，仅允许上传图片文件");
            }
        }

        long size = file.getSize();
        if (size <= 0) {
            return Result.fail("文件内容为空");
        }
        if (size > MAX_FILE_SIZE) {
            return Result.fail("文件大小超过限制（最大 10MB）");
        }

        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[12];
            int read = is.read(header);
            if (read < 3 || !isImageContent(header)) {
                return Result.fail("文件内容不是有效的图片文件");
            }
        } catch (IOException e) {
            return Result.fail("无法读取文件内容");
        }

        try (InputStream is = file.getInputStream()) {
            BufferedImage img = ImageIO.read(is);
            if (img == null) {
                return Result.fail("无法解析图片，请确认文件是有效的图片格式");
            }
        } catch (IOException e) {
            return Result.fail("读取图片内容失败");
        }

        Path dir = Paths.get(uploadDir).normalize().toAbsolutePath();
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        String filename = UUID.randomUUID().toString() + extension;
        Path filePath = dir.resolve(filename).normalize();
        if (!filePath.startsWith(dir)) {
            return Result.fail("文件名不合法");
        }
        file.transferTo(filePath.toFile());
        return Result.ok(filename);
    }

    private MediaType getMediaTypeForFilename(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        } else if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        } else if (lower.endsWith(".gif")) {
            return MediaType.IMAGE_GIF;
        } else if (lower.endsWith(".bmp")) {
            return MediaType.parseMediaType("image/bmp");
        } else if (lower.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    @GetMapping("/{filename}")
    public ResponseEntity<Resource> getFile(@PathVariable String filename) throws MalformedURLException {
        if (filename == null || filename.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String lowerName = filename.toLowerCase(Locale.ROOT);
        boolean hasValidExt = false;
        for (String ext : ALLOWED_EXTENSIONS) {
            if (lowerName.endsWith(ext)) {
                hasValidExt = true;
                break;
            }
        }
        if (!hasValidExt) {
            return ResponseEntity.badRequest().build();
        }

        Path dir = Paths.get(uploadDir).normalize().toAbsolutePath();
        Path filePath = dir.resolve(filename).normalize();

        if (!filePath.startsWith(dir)) {
            return ResponseEntity.badRequest().build();
        }

        Resource resource = new UrlResource(filePath.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = getMediaTypeForFilename(filename);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(resource);
    }
}
