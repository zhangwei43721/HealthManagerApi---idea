package com.rabbiter.healthsys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rabbiter.healthsys.entity.SportInfo;
import com.rabbiter.healthsys.mapper.SportInfoMapper;
import com.rabbiter.healthsys.service.ISportInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import javax.transaction.Transactional;
import java.util.List;

/**
 * 运动知识服务实现类
 * 
 * @author Skyforever
 * @since 2025-05-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SportInfoServiceImpl extends ServiceImpl<SportInfoMapper, SportInfo> implements ISportInfoService {

    private final SportInfoMapper sportInfoMapper;


    @Override
    public List<SportInfo> getAllSportInfos() {
        return sportInfoMapper.selectList(null);
    }

    @Transactional
    @Override
    public boolean addSport(SportInfo sport) {
        QueryWrapper<SportInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("sport_type", sport.getSportType());
        List<SportInfo> list = this.baseMapper.selectList(queryWrapper);

        if (list.isEmpty()) {
            this.baseMapper.insert(sport);
            return true;
        } else {
            return false;
        }
    }


    @Override
    public void updateSport(SportInfo sport) {
        this.baseMapper.updateById(sport);
    }

    @Override
    public SportInfo getSportById(Integer id) {
        SportInfo sportInfo = this.baseMapper.selectById(id);
        return sportInfo;
    }

    @Override
    public void deletUserById(Integer id) {
        this.baseMapper.deleteById(id);
    }

}

