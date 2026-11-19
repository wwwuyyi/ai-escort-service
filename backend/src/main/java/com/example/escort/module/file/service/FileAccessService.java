package com.example.escort.module.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.escort.common.BizException;
import com.example.escort.common.ResultCode;
import com.example.escort.module.auth.entity.SysUser;
import com.example.escort.module.auth.mapper.SysUserMapper;
import com.example.escort.module.elderly.entity.ElderlyProfile;
import com.example.escort.module.elderly.mapper.ElderlyProfileMapper;
import com.example.escort.module.escort.entity.EscortProfile;
import com.example.escort.module.escort.mapper.EscortProfileMapper;
import com.example.escort.module.order.entity.EscortOrder;
import com.example.escort.module.order.mapper.EscortOrderMapper;
import com.example.escort.module.record.entity.MedicalDocument;
import com.example.escort.module.record.entity.MedicalRecord;
import com.example.escort.module.record.mapper.MedicalDocumentMapper;
import com.example.escort.module.record.mapper.MedicalRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 上传文件对象级授权。
 *
 * 文件归属通过业务表反查：
 * - medical_document.image_url → medical_record → elderly_profile.family_id
 * - escort_profile.materials  → 陪诊师本人
 * - sys_user.avatar           → 用户本人
 * ADMIN 可访问全部上传文件；其余用户只能访问与自身业务相关的文件。
 */
@Service
@RequiredArgsConstructor
public class FileAccessService {

    private final MedicalDocumentMapper documentMapper;
    private final MedicalRecordMapper recordMapper;
    private final ElderlyProfileMapper elderlyMapper;
    private final EscortOrderMapper orderMapper;
    private final EscortProfileMapper escortProfileMapper;
    private final SysUserMapper userMapper;

    /** 仅允许 /upload/ 下的单层文件名，禁止目录穿越 */
    public boolean isSafeUploadPath(String path) {
        if (path == null) return false;
        String p = path.trim();
        if (!p.startsWith("/upload/")) return false;
        String name = p.substring("/upload/".length());
        if (name.isBlank() || name.contains("..") || name.contains("/") || name.contains("\\")) return false;
        return name.matches("[A-Za-z0-9._-]+");
    }

    /**
     * 校验当前用户是否有权访问该文件；无权抛 403，非法路径抛 404。
     */
    public void assertCanAccess(Long userId, String role, String path) {
        if (!isSafeUploadPath(path)) throw new BizException(ResultCode.NOT_FOUND);
        if (userId == null) throw new BizException(ResultCode.UNAUTHORIZED);
        if ("ADMIN".equals(role)) return;

        // 1) 病历/检查单据图片：按档案所属老人校验
        List<MedicalDocument> docs = documentMapper.selectList(
                new LambdaQueryWrapper<MedicalDocument>().eq(MedicalDocument::getImageUrl, path));
        for (MedicalDocument doc : docs) {
            MedicalRecord record = recordMapper.selectById(doc.getRecordId());
            if (record == null) continue;
            if (canAccessElderly(userId, role, record.getElderlyId())) return;
        }

        // 2) 陪诊师资质材料：仅本人
        List<EscortProfile> profiles = escortProfileMapper.selectList(
                new LambdaQueryWrapper<EscortProfile>().like(EscortProfile::getMaterials, path));
        for (EscortProfile p : profiles) {
            if (p.getUserId() != null && p.getUserId().equals(userId)) return;
        }

        // 3) 用户头像：仅本人
        List<SysUser> owners = userMapper.selectList(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getAvatar, path));
        for (SysUser u : owners) {
            if (u.getId().equals(userId)) return;
        }

        throw new BizException(ResultCode.FORBIDDEN);
    }

    private boolean canAccessElderly(Long userId, String role, Long elderlyId) {
        ElderlyProfile elderly = elderlyMapper.selectById(elderlyId);
        if (elderly == null) return false;
        if ("FAMILY".equals(role)) {
            return elderly.getFamilyId().equals(userId);
        }
        if ("ESCORT".equals(role)) {
            Long count = orderMapper.selectCount(new LambdaQueryWrapper<EscortOrder>()
                    .eq(EscortOrder::getEscortId, userId)
                    .eq(EscortOrder::getElderlyId, elderlyId));
            return count != null && count > 0;
        }
        return false;
    }
}
