package com.rabbiter.healthsys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rabbiter.healthsys.entity.Body;
import com.rabbiter.healthsys.mapper.BodyMapper;
import com.rabbiter.healthsys.service.IBodyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 体征服务实现类
 * 
 * @author Skyforever
 * @since 2025-05-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BodyServiceImpl extends ServiceImpl<BodyMapper, Body> implements IBodyService {

    private final BodyMapper bodyMapper;


    @Override
    public boolean insert(Body body) {
        LambdaQueryWrapper<Body> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Body::getId, body.getId());
        int count = this.baseMapper.selectCount(wrapper).intValue();
        if (count > 0) {
            // 存在相同记录，执行更新
            this.baseMapper.update(body, wrapper);
            return false;
        } else {
            // 不存在相同记录，插入数据库
            this.baseMapper.insert(body);
            return true;
        }
    }


    @Override
    public void update(Body body) {
        this.baseMapper.updateById(body);
    }


    @Override
    public List<Body> getBodyListByUserId(Integer pid) {
        return bodyMapper.getBodyListByUserId(pid);
    }


    @Override
    public Body getBodyById(Integer id) {
        return this.baseMapper.selectById(id);
    }

    @Override
    public void updateBody(Body body) {
        this.baseMapper.updateById(body);
    }

    @Override
    public void deletBodyById(Integer id) {
        // 直接删除指定主键的记录
        this.baseMapper.deleteById(id);
        //删除BodyNotes表中与
    }


}

