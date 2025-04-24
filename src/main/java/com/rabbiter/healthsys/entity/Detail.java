package com.rabbiter.healthsys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("detail")
public class Detail implements Serializable {


    @TableId(type = IdType.AUTO)
    @TableField(value = "id")
    private Integer id;


    @TableField("sport_type")
    private String sportType;

    private String disease;

    private String method;

    private String notes;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSportType() {
        return sportType;
    }

    public void setSportType(String sportType) {
        this.sportType = sportType;
    }

    public String getDisease() {
        return disease;
    }

    public void setDisease(String disease) {
        this.disease = disease;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return "Detail{" +
                "id=" + id +
                ", sportType='" + sportType + '\'' +
                ", disease='" + disease + '\'' +
                ", method='" + method + '\'' +
                ", notes='" + notes + '\'' +
                '}';
    }
}
