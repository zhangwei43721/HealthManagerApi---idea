package com.rabbiter.healthsys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rabbiter.healthsys.entity.BodyNotes;
import com.rabbiter.healthsys.mapper.BodyNotesMapper;
import com.rabbiter.healthsys.service.IBodyNotesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BodyNotesServiceImpl extends ServiceImpl<BodyNotesMapper, BodyNotes> implements IBodyNotesService {

    @Override
    public boolean insert(BodyNotes bodyNotes) {
        bodyNotes.setDate(ObjectUtils.isEmpty(bodyNotes.getDate()) ? new Date() : bodyNotes.getDate());
        this.baseMapper.insert(bodyNotes);
        return true;
    }

    @Override
    public List<BodyNotes> getBodyNotes(Integer id) {
        LambdaQueryWrapper<BodyNotes> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BodyNotes::getId, id);
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public void delete(Integer id) {
        QueryWrapper<BodyNotes> wrapper = new QueryWrapper<>();
        wrapper.eq("id", id);
        baseMapper.delete(wrapper);
    }


    @Override
    public BodyNotes getUserBodyById(Integer notesid) {
        return this.baseMapper.selectById(notesid);
    }

    @Override
    public void updateUserBody(BodyNotes bodyNotes) {
        bodyNotes.setDate(null);
        this.baseMapper.updateById(bodyNotes);
    }

    @Override
    public void deleteUserBodyById(Integer notesid) {
        this.baseMapper.deleteById(notesid);
    }


}

