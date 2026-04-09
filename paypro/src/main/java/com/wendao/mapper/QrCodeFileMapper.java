package com.wendao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wendao.entity.QrCodeFile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 二维码文件 Mapper
 */
@Mapper
public interface QrCodeFileMapper extends BaseMapper<QrCodeFile> {
}
