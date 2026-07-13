package com.hrms.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 封装分页查询结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    /** 总记录数 */
    private long total;

    /** 当前页数据集合 */
    private List<T> records;

    /** 当前页码 */
    private int page;

    /** 每页条数 */
    private int size;

    /** 仅 total + records（兼容旧调用） */
    public static <T> PageResult<T> of(long total, List<T> records) {
        return new PageResult<>(total, records, 0, 0);
    }

    /** 完整分页信息 */
    public static <T> PageResult<T> of(List<T> records, long total, int page, int size) {
        return new PageResult<>(total, records, page, size);
    }
}
