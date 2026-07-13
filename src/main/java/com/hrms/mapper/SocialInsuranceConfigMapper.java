package com.hrms.mapper;

import com.hrms.entity.SocialInsuranceConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 社保公积金配置 Mapper
 */
@Mapper
public interface SocialInsuranceConfigMapper {

    /** 根据城市查询当前生效的配置 */
    SocialInsuranceConfig selectByCity(@Param("cityName") String cityName);

    /** 查询最新的默认配置 */
    SocialInsuranceConfig selectLatest();
}
