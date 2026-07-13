package com.hrms.service;

import com.hrms.common.exception.BaseException;
import com.hrms.dto.PositionSaveDTO;
import com.hrms.entity.Position;
import com.hrms.mapper.EmployeeMapper;
import com.hrms.mapper.PositionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 职位管理服务
 * 支持 M (管理)、P (专业)、S (支持) 三大序列
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PositionService {

    /** 合法的职位序列 */
    private static final Set<String> VALID_SEQUENCES = new HashSet<>(Arrays.asList("M", "P", "S"));

    private final PositionMapper positionMapper;
    private final EmployeeMapper employeeMapper;

    // ═══════════════ 查询 ═══════════════

    /** 查询所有有效职位 */
    public List<Position> listAll() {
        return positionMapper.selectAll();
    }

    /** 根据ID查询职位 */
    public Position getById(Long id) {
        Position position = positionMapper.selectById(id);
        if (position == null) {
            throw BaseException.notFound("职位不存在");
        }
        return position;
    }

    /** 按序列查询 */
    public List<Position> listBySequence(String sequence) {
        if (!VALID_SEQUENCES.contains(sequence)) {
            throw BaseException.badRequest("无效的职位序列[" + sequence + "]，必须为 M/P/S");
        }
        return positionMapper.selectBySequence(sequence);
    }

    // ═══════════════ 创建 ═══════════════

    @Transactional
    public void create(PositionSaveDTO dto) {
        // 1. 校验序列
        if (!VALID_SEQUENCES.contains(dto.getSequence())) {
            throw BaseException.badRequest("职位序列必须为 M/P/S 之一，当前值: " + dto.getSequence());
        }

        // 2. 校验试用期
        if (dto.getDefaultProbationMonths() == null || dto.getDefaultProbationMonths() <= 0) {
            throw BaseException.badRequest("试用期必须大于0个月");
        }

//        // 3. 编码唯一性（取消）
//        if (positionMapper.countByCode(dto.getPositionCode(), null) > 0) {
//            throw BaseException.badRequest("职位编码[" + dto.getPositionCode() + "]已存在");
//        }

        // 4. 插入
        Position position = new Position();
        position.setPositionName(dto.getPositionName());
        position.setPositionCode(dto.getPositionCode());
        position.setSequence(dto.getSequence());
        position.setGradeRange(dto.getGradeRange());
        position.setDefaultProbationMonths(dto.getDefaultProbationMonths());
        position.setDescription(dto.getDescription());
        position.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);

        positionMapper.insert(position);
        log.info("职位创建成功: id={}, name={}, sequence={}", position.getId(),
                position.getPositionName(), position.getSequence());
    }

    // ═══════════════ 更新 ═══════════════

    @Transactional
    public void update(PositionSaveDTO dto) {
        if (dto.getId() == null) {
            throw BaseException.badRequest("更新时职位ID不能为空");
        }

        Position existing = positionMapper.selectById(dto.getId());
        if (existing == null) {
            throw BaseException.notFound("职位不存在");
        }

        // 校验序列
        if (!VALID_SEQUENCES.contains(dto.getSequence())) {
            throw BaseException.badRequest("职位序列必须为 M/P/S 之一");
        }

        // 校验试用期
        if (dto.getDefaultProbationMonths() == null || dto.getDefaultProbationMonths() <= 0) {
            throw BaseException.badRequest("试用期必须大于0个月");
        }

//        // 编码唯一性（取消）
//        if (positionMapper.countByCode(dto.getPositionCode(), dto.getId()) > 0) {
//            throw BaseException.badRequest("职位编码[" + dto.getPositionCode() + "]已存在");
//        }

        Position position = new Position();
        position.setId(dto.getId());
        position.setPositionName(dto.getPositionName());
        position.setPositionCode(dto.getPositionCode());
        position.setSequence(dto.getSequence());
        position.setGradeRange(dto.getGradeRange());
        position.setDefaultProbationMonths(dto.getDefaultProbationMonths());
        position.setDescription(dto.getDescription());
        position.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);

        positionMapper.update(position);
        log.info("职位更新成功: id={}, name={}", position.getId(), position.getPositionName());
    }

    // ═══════════════ 删除 ═══════════════

    @Transactional
    public void delete(Long id) {
        Position position = positionMapper.selectById(id);
        if (position == null) {
            throw BaseException.notFound("职位不存在");
        }

        int inUseCount = employeeMapper.countByPositionId(id);
        if (inUseCount > 0) {
            throw BaseException.badRequest("该职位下有 " + inUseCount + " 名在职员工，无法删除");
        }

        positionMapper.deleteById(id);
        log.info("职位删除成功: id={}, name={}", id, position.getPositionName());
    }
}
