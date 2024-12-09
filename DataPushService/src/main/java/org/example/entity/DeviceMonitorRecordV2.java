package org.example.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@JsonInclude(JsonInclude.Include.NON_NULL) // 只包含属性值不为 null 的属性
@Table(name="device_monitor_record_v2")
public class DeviceMonitorRecordV2 {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; // None
    @Column(name = "device_atrribute_id")
    private Integer device_atrribute_id; // None
    @Column(name = "device_id")
    private Integer device_id; // None
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "occur_time")
    private LocalDateTime occur_time; // None
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "recover_time")
    private LocalDateTime recover_time; // None
    @Column(name = "status")
    private Integer status; // None
    @Column(name = "is_alarmed")
    private Integer isAlarm;
    @Column(name = "is_canceled")
    private Integer isCancel;

    @Column(name = "last_value")
    private String lastValue;
    @Transient
    private String errorValue;
    @Transient
    private String recoverValue;
    @Transient
    private String alarmLevelValue;
    @Transient
    private String alarmLevel;


    public DeviceMonitorRecordV2() {}

    public DeviceMonitorRecordV2(Integer device_atrribute_id, Integer device_id, LocalDateTime occur_time, Integer status) {
        this.device_atrribute_id = device_atrribute_id;
        this.device_id = device_id;
        this.occur_time = occur_time;
        this.status = status;
    }

    public DeviceMonitorRecordV2(Integer id, Integer device_atrribute_id, Integer device_id, LocalDateTime occur_time, LocalDateTime recover_time, Integer status)  {
        this.id = id;
        this.device_atrribute_id = device_atrribute_id;
        this.device_id = device_id;
        this.occur_time = occur_time;
        this.recover_time = recover_time;
        this.status = status;
    }

    public DeviceMonitorRecordV2(Integer device_atrribute_id, Integer device_id, LocalDateTime occur_time, LocalDateTime recover_time, Integer status)  {
        this.id = id;
        this.device_atrribute_id = device_atrribute_id;
        this.device_id = device_id;
        this.occur_time = occur_time;
        this.recover_time = recover_time;
        this.status = status;
    }

    public DeviceMonitorRecordV2(Integer deviceId, Integer atrributeId, String occurTime, String recoverTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        this.device_atrribute_id = atrributeId;
        this.device_id = deviceId;
        if (occurTime!=null){
            this.occur_time = LocalDateTime.parse(occurTime,formatter);
        }
        this.status = 1;
        if (recoverTime !=null){
            this.recover_time = LocalDateTime.parse(recoverTime,formatter);
            this.status = 0;
        }
    }

    public DeviceMonitorRecordV2(Integer deviceId, Integer atrributeId, String occurTime, String recoverTime, Integer status) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        this.device_atrribute_id = atrributeId;
        this.device_id = deviceId;
        if (occurTime!=null){
            this.occur_time = LocalDateTime.parse(occurTime,formatter);
        }
        if (recoverTime !=null){
            this.recover_time = LocalDateTime.parse(recoverTime,formatter);
        }
        this.status  =status;

    }

    public void setOccur_timeFromString(String occur_timeString) {
        if (occur_timeString.length() == 10) {
            occur_timeString += " 00:00:00";
        }
        this.occur_time = LocalDateTime.parse(occur_timeString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public void setRecover_timeFromString(String recover_timeString) {
        if (recover_timeString.length() == 10) {
            recover_timeString += " 00:00:00";
        }
        this.recover_time = LocalDateTime.parse(recover_timeString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getDevice_atrribute_id() {
        return device_atrribute_id;
    }

    public void setDevice_atrribute_id(Integer deviceAtrributeId) {
        this.device_atrribute_id = deviceAtrributeId;
    }

    public Integer getDevice_id() {
        return device_id;
    }

    public void setDevice_id(Integer deviceId) {
        this.device_id = deviceId;
    }

    public LocalDateTime getOccur_time() {
        return occur_time;
    }

    public void setOccur_time(LocalDateTime occurTime) {
        this.occur_time = occurTime;
    }

    public LocalDateTime getRecover_time() {
        return recover_time;
    }

    public void setRecover_time(LocalDateTime recoverTime) {
        this.recover_time = recoverTime;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getIsAlarm() {
        return isAlarm;
    }

    public void setIsAlarm(Integer isAlarm) {
        this.isAlarm = isAlarm;
    }

    public Integer getIsCancel() {
        return isCancel;
    }

    public void setIsCancel(Integer isCancel) {
        this.isCancel = isCancel;
    }

    public String getErrorValue() {
        return errorValue;
    }

    public void setErrorValue(String errorValue) {
        this.errorValue = errorValue;
    }

    public String getLastValue() {
        return lastValue;
    }

    public void setLastValue(String lastValue) {
        this.lastValue = lastValue;
    }

    public String getRecoverValue() {
        return recoverValue;
    }

    public void setRecoverValue(String recoverValue) {
        this.recoverValue = recoverValue;
    }

    public String getAlarmLevelValue() {
        return alarmLevelValue;
    }

    public void setAlarmLevelValue(String alarmLevelValue) {
        this.alarmLevelValue = alarmLevelValue;
    }

    public String getAlarmLevel() {
        return alarmLevel;
    }

    public void setAlarmLevel(String alarmLevel) {
        this.alarmLevel = alarmLevel;
    }
}
