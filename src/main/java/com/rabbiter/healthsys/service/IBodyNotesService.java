package com.rabbiter.healthsys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rabbiter.healthsys.entity.BodyNotes;

import java.util.List;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author rabbiter
 * @since 2021-07-26
 */
public interface IBodyNotesService extends IService<BodyNotes> {
    boolean insert(BodyNotes bodyNotes);
    List<BodyNotes> getBodyNotes(Integer id);
    void delete(Integer id);
    BodyNotes getUserBodyById(Integer notesid);
    void updateUserBody(BodyNotes bodyNotes);
    void deleteUserBodyById(Integer id);
    List<BodyNotes> getLatestBodyNotesByUserId(Integer userId);

}
