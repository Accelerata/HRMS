package com.hrms.mapper;

import com.hrms.dto.DeptEmployeeCountDTO;
import com.hrms.entity.Employee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 员工 Mapper（基础 CRUD + 人数统计）
 */
@Mapper
public interface EmployeeMapper {

    // ────────────── 查询 ──────────────

    /** 根据ID查询员工 */
    Employee selectById(@Param("id") Long id);

    /** 根据工号查询 */
    Employee selectByEmployeeNo(@Param("employeeNo") String employeeNo);

    /** 根据手机号查询 */
    Employee selectByPhone(@Param("phone") String phone);

    /** 根据身份证号查询 */
    Employee selectByIdCard(@Param("idCard") String idCard);

    /** 根据部门ID查询在职员工列表 */
    List<Employee> selectByDeptId(@Param("deptId") Long deptId);

    /** 查询所有员工 */
    List<Employee> selectAll();

    /** 分页条件查询 */
    List<Employee> selectByCondition(@Param("deptId") Long deptId,
                                     @Param("status") Integer status,
                                     @Param("keyword") String keyword,
                                     @Param("offset") int offset,
                                     @Param("size") int size);

    /** 条件查询总数 */
    int countByCondition(@Param("deptId") Long deptId,
                         @Param("status") Integer status,
                         @Param("keyword") String keyword);

    // ────────────── 统计分析 ──────────────

    /** 根据部门ID统计各状态人数 */
    List<DeptEmployeeCountDTO> countByDeptGroupByStatus();

    /** 根据职位ID统计在职员工数 */
    int countByPositionId(@Param("positionId") Long positionId);

    /** 试用期即将到期员工（N天内） */
    List<Employee> selectExpiringProbation(@Param("days") int days);

    // ────────────── 写操作 ──────────────

    /** 新增员工（自动回填主键ID） */
    int insert(Employee employee);

    /** 更新员工 */
    int update(Employee employee);

    /** 软删除员工（标记为已离职） */
    int deleteById(@Param("id") Long id);
}
