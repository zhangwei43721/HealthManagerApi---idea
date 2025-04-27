package com.rabbiter.healthsys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("j_body_notes")
public class BodyNotes implements Serializable {

    private static final long serialVersionUID = 1L;

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

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "BodyNotes{" +
                "id=" + id +
                ", notesid=" + notesid +
                ", name='" + name + '\'' +
                ", age=" + age +
                ", gender='" + gender + '\'' +
                ", height=" + height +
                ", weight=" + weight +
                ", bloodSugar=" + bloodSugar +
                ", bloodPressure='" + bloodPressure + '\'' +
                ", bloodLipid='" + bloodLipid + '\'' +
                ", heartRate=" + heartRate +
                ", vision=" + vision +
                ", sleepDuration=" + sleepDuration +
                ", sleepQuality='" + sleepQuality + '\'' +
                ", smoking=" + smoking +
                ", drinking=" + drinking +
                ", exercise=" + exercise +
                ", foodTypes='" + foodTypes + '\'' +
                ", waterConsumption=" + waterConsumption +
                ", Date=" + Date +
                '}';
    }

    public Integer getNotesid() {
        return notesid;
    }

    public void setNotesid(Integer notesid) {
        this.notesid = notesid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Double getHeight() {
        return height;
    }

    public void setHeight(Double height) {
        this.height = height;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public Double getBloodSugar() {
        return bloodSugar;
    }

    public void setBloodSugar(Double bloodSugar) {
        this.bloodSugar = bloodSugar;
    }

    public String getBloodPressure() {
        return bloodPressure;
    }

    public void setBloodPressure(String bloodPressure) {
        this.bloodPressure = bloodPressure;
    }

    public String getBloodLipid() {
        return bloodLipid;
    }

    public void setBloodLipid(String bloodLipid) {
        this.bloodLipid = bloodLipid;
    }

    public double getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(double heartRate) {
        this.heartRate = heartRate;
    }

    public Integer getVision() {
        return vision;
    }

    public void setVision(Integer vision) {
        this.vision = vision;
    }

    public double getSleepDuration() {
        return sleepDuration;
    }

    public void setSleepDuration(double sleepDuration) {
        this.sleepDuration = sleepDuration;
    }

    public String getSleepQuality() {
        return sleepQuality;
    }

    public void setSleepQuality(String sleepQuality) {
        this.sleepQuality = sleepQuality;
    }

    public boolean isSmoking() {
        return smoking;
    }

    public void setSmoking(boolean smoking) {
        this.smoking = smoking;
    }

    public boolean isDrinking() {
        return drinking;
    }

    public void setDrinking(boolean drinking) {
        this.drinking = drinking;
    }

    public boolean isExercise() {
        return exercise;
    }

    public void setExercise(boolean exercise) {
        this.exercise = exercise;
    }

    public String getFoodTypes() {
        return foodTypes;
    }

    public void setFoodTypes(String foodTypes) {
        this.foodTypes = foodTypes;
    }

    public double getWaterConsumption() {
        return waterConsumption;
    }

    public void setWaterConsumption(double waterConsumption) {
        this.waterConsumption = waterConsumption;
    }

    public java.util.Date getDate() {
        return Date;
    }

    public void setDate(java.util.Date date) {
        Date = date;
    }

}

