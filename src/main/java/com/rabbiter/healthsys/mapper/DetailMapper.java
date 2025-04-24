package com.rabbiter.healthsys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rabbiter.healthsys.entity.Detail;

import java.util.List;

public interface DetailMapper extends BaseMapper<Detail> {
    List<Detail> getDetailInfo(String sportName);
}

