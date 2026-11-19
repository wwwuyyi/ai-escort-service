package com.example.escort.module.file.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.example.escort.common.BizException;
import com.example.escort.common.Result;
import com.example.escort.common.ResultCode;
import com.example.escort.module.file.service.FileAccessService;
import com.example.escort.module.file.service.FileUrlSigner;
import com.example.escort.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 文件上传/受控下载控制器
 * /api/file（上传、签名申请需登录；下载由短期签名保护）
 */
@Slf4j
@Tag(name = "文件上传模块")
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    /** 允许上传的文件后缀白名单（图片/PDF，不含Office文档） */
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "gif", "bmp", "pdf"
    ));

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "jpg", "image/jpeg", "jpeg", "image/jpeg", "png", "image/png",
            "gif", "image/gif", "bmp", "image/bmp", "pdf", "application/pdf"
    );

    /** 扩展名 → 文件头真实类型（BUG-028） */
    private static final Map<String, String> EXTENSION_REAL_TYPES = new HashMap<>();
    static {
        EXTENSION_REAL_TYPES.put("jpg", "JPEG");
        EXTENSION_REAL_TYPES.put("jpeg", "JPEG");
        EXTENSION_REAL_TYPES.put("png", "PNG");
        EXTENSION_REAL_TYPES.put("gif", "GIF");
        EXTENSION_REAL_TYPES.put("bmp", "BMP");
        EXTENSION_REAL_TYPES.put("pdf", "PDF");
    }

    private static final long MAX_UPLOAD_BYTES = 10L * 1024 * 1024;

    private final FileAccessService fileAccessService;
    private final FileUrlSigner fileUrlSigner;

    // ===== POST /api/file/upload 文件上传 =====
    @Operation(summary = "文件上传（图片/PDF，限10MB，白名单后缀，UUID重命名，需登录）")
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "上传文件不能为空");
        }
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new BizException(ResultCode.PAYLOAD_TOO_LARGE, "文件过大，最大支持10MB");
        }

        // 后缀白名单校验
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.contains(".")) {
            throw new BizException(ResultCode.BAD_REQUEST, "文件格式不支持");
        }
        String ext = originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BizException(ResultCode.BAD_REQUEST,
                    "文件格式不支持，仅允许：" + String.join(",", ALLOWED_EXTENSIONS));
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            log.error("读取上传文件失败", e);
            throw new BizException(ResultCode.INTERNAL_ERROR, "文件读取失败，请重试");
        }
        validateRealType(content, ext);

        // UUID重命名，保留原始后缀
        String newFileName = IdUtil.fastSimpleUUID() + "." + ext;
        File dest = new File("./upload/" + newFileName).getAbsoluteFile();
        try {
            FileUtil.mkParentDirs(dest);
            Files.write(dest.toPath(), content);
            String relativePath = "/upload/" + newFileName;
            log.info("文件上传成功: original={}, saved={}, size={}", originalName, relativePath, content.length);
            return Result.ok(relativePath);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return Result.fail(500, "文件上传失败，请重试");
        }
    }

    /**
     * 扩展名与真实文件头/图片解码双重校验（BUG-028）。
     * 不信任 Content-Type 或扩展名；拒绝 HTML/SVG/Office 及伪装文件。
     */
    private void validateRealType(byte[] content, String ext) {
        String expected = EXTENSION_REAL_TYPES.get(ext);
        String actual = detectRealType(content);
        if (expected == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "文件格式不支持");
        }
        if (!expected.equals(actual)) {
            throw new BizException(ResultCode.BAD_REQUEST, "文件内容与扩展名不符");
        }
        if (!"PDF".equals(expected)) {
            try {
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(content));
                if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                    throw new BizException(ResultCode.BAD_REQUEST, "图片内容无法解码，文件可能已损坏或伪装");
                }
            } catch (IOException e) {
                throw new BizException(ResultCode.BAD_REQUEST, "图片内容无法解码，文件可能已损坏或伪装");
            }
        }
    }

    private String detectRealType(byte[] b) {
        if (b == null || b.length == 0) return "UNKNOWN";
        if (b.length >= 8
                && (b[0] & 0xFF) == 0x89 && (b[1] & 0xFF) == 0x50
                && (b[2] & 0xFF) == 0x4E && (b[3] & 0xFF) == 0x47
                && (b[4] & 0xFF) == 0x0D && (b[5] & 0xFF) == 0x0A
                && (b[6] & 0xFF) == 0x1A && (b[7] & 0xFF) == 0x0A) {
            return "PNG";
        }
        if (b.length >= 3
                && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "JPEG";
        }
        if (b.length >= 6 && startsWithAscii(b, "GIF87a")) return "GIF";
        if (b.length >= 6 && startsWithAscii(b, "GIF89a")) return "GIF";
        if (b.length >= 2 && b[0] == 'B' && b[1] == 'M') return "BMP";
        if (b.length >= 5 && startsWithAscii(b, "%PDF-")) return "PDF";
        return "UNKNOWN";
    }

    private boolean startsWithAscii(byte[] b, String prefix) {
        if (b.length < prefix.length()) return false;
        for (int i = 0; i < prefix.length(); i++) {
            if (b[i] != (byte) prefix.charAt(i)) return false;
        }
        return true;
    }

    /**
     * 申请短期签名下载 URL。
     * 先做对象级授权，成功后返回 5 分钟有效的签名 URL；URL 中不包含登录 JWT。
     */
    @Operation(summary = "申请文件短期签名 URL（5分钟有效，下载 URL 不含 JWT）")
    @GetMapping("/signed-url")
    public Result<Map<String, Object>> signedUrl(@RequestParam String path) {
        Long userId = UserContext.getUserId();
        fileAccessService.assertCanAccess(userId, UserContext.getUserRole(), path);
        String url = fileUrlSigner.buildSignedUrl(path, userId);
        return Result.ok(Map.of("url", url, "expiresIn", fileUrlSigner.getTtlSeconds()));
    }

    /**
     * 受控下载：只有持有有效短期签名（path+uid+expires 的 HMAC）才能访问。
     * 该接口在 WebMvcConfig 中排除登录拦截器，因为 <img> 不能携带 Authorization。
     */
    @Operation(summary = "受控文件下载（短期签名）")
    @GetMapping("/download")
    public void download(@RequestParam String path,
                         @RequestParam Long uid,
                         @RequestParam long expires,
                         @RequestParam String sig,
                         HttpServletResponse response) throws IOException {
        if (!fileAccessService.isSafeUploadPath(path)
                || !fileUrlSigner.verify(path, uid, expires, sig)) {
            throw new BizException(ResultCode.FORBIDDEN, "文件链接无效或已过期");
        }
        File file = resolveUploadFile(path);
        if (!file.isFile()) throw new BizException(ResultCode.NOT_FOUND, "文件不存在");

        String ext = path.substring(path.lastIndexOf('.') + 1).toLowerCase();
        response.setContentType(CONTENT_TYPES.getOrDefault(ext, "application/octet-stream"));
        response.setContentLengthLong(file.length());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=\"" + file.getName() + "\"");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Cache-Control", "private, max-age=" + fileUrlSigner.getTtlSeconds());
        Files.copy(file.toPath(), response.getOutputStream());
    }

    private File resolveUploadFile(String path) throws IOException {
        String name = path.substring("/upload/".length());
        File root = new File("./upload").getCanonicalFile();
        File target = new File(root, name).getCanonicalFile();
        if (!target.getPath().startsWith(root.getPath() + File.separator)) {
            throw new BizException(ResultCode.NOT_FOUND, "文件不存在");
        }
        return target;
    }
}
