package com.rabbiter.healthsys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sport_info")
public class SportInfo implements Serializable {
    @TableId(type = IdType.AUTO)
    @TableField(value = "id")
    private Integer id;

    @TableField(value = "sport_type")
    private String sportType;

    @TableField(value = "suitable_time")
    private String suitableTime;

    @TableField(value = "suitable_heart_rate")
    private String suitableHeartRate;

    @TableField(value = "suitable_frequency")
    private String suitableFrequency;

    @TableField(value = "recommended_speed")
    private String recommendedSpeed;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "SportInfo{" +
                "id=" + id +
                ", sportType='" + sportType + '\'' +
                ", suitableTime='" + suitableTime + '\'' +
                ", suitableHeartRate='" + suitableHeartRate + '\'' +
                ", suitableFrequency='" + suitableFrequency + '\'' +
                ", recommendedSpeed='" + recommendedSpeed + '\'' +
                '}';
    }

    public String getSportType() {
        return sportType;
    }

    public void setSportType(String sportType) {
        this.sportType = sportType;
    }

    public String getSuitableTime() {
        return suitableTime;
    }

    public void setSuitableTime(String suitableTime) {
        this.suitableTime = suitableTime;
    }

    public String getSuitableHeartRate() {
        return suitableHeartRate;
    }

    public void setSuitableHeartRate(String suitableHeartRate) {
        this.suitableHeartRate = suitableHeartRate;
    }

    public String getSuitableFrequency() {
        return suitableFrequency;
    }

    public void setSuitableFrequency(String suitableFrequency) {
        this.suitableFrequency = suitableFrequency;
    }

    public String getRecommendedSpeed() {
        return recommendedSpeed;
    }

    public void setRecommendedSpeed(String recommendedSpeed) {
        this.recommendedSpeed = recommendedSpeed;
    }

}

