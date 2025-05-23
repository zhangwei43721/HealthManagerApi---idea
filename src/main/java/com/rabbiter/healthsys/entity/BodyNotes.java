package com.rabbiter.healthsys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("j_body_notes")
public class BodyNotes implements Serializable {

    private static final long serialVersionUID = 1L;

    @Setter
    @Getter
    @TableField(value = "id")
    private Integer id;

    @TableId(value = "notes_id", type = IdType.AUTO)
    private Integer notesid;
    private String name;
    private Integer age;
    private String gender;
    private Double height;
    private Double weight;
    @TableField(value = "bloodSugar")
    private Double bloodSugar;
    @TableField(value = "bloodPressure")
    private String bloodPressure;
    @TableField(value = "bloodLipid")
    private String bloodLipid;
    @TableField("heart_rate")
    private double heartRate;
    @TableField("vision")
    private Integer vision;
    @TableField("sleep_duration")
    private double sleepDuration;
    @TableField("sleep_quality")
    private String sleepQuality;
    @TableField("smoking")
    private boolean smoking;
    @TableField("drinking")
    private boolean drinking;
    @TableField("exercise")
    private boolean exercise;

    @TableField("food_types")
    private String foodTypes;

    @TableField("water_consumption")
    private double waterConsumption;

    @TableField("Date")
    private Date Date;


}

