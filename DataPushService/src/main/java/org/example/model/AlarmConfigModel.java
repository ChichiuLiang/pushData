package org.example.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AlarmConfigModel {
    //da.id,dt.type_code,attribute_no,attribute_name_cn,attribute_name,data_type,da.alarm_value,da.max_alarm,da.min_alarm,da.alarm_level_id,al.alarm_level_name,dt.id as device_type_id,moudle_name
    private String attributeId;
    private String typeCode;
    private String attributeNo;
    private String attributeNameCn;
    private String attributeName;
    private String dataType;
    private String alarmValue;
    private String maxAlarm;
    private String minAlarm;
    private String alarmLevelId;
    private String alarmLevelName;
    private String deviceTypeId;
    private String moudleName;

    private String SpecifyAlarm;//特定值报警

    //以下值需要设置
    private String barCode;
    private String lastValue;
    private String deviceAddress;
    private String time;

    private String deviceId;//转换后的设备id

    @Override
    public String toString() {
        return "AlarmConfigModel{" +
                "attributeId='" + attributeId + '\'' +
                ", typeCode='" + typeCode + '\'' +
                ", attributeNo='" + attributeNo + '\'' +
                ", attributeNameCn='" + attributeNameCn + '\'' +
                ", attributeName='" + attributeName + '\'' +
                ", dataType='" + dataType + '\'' +
                ", alarmValue='" + alarmValue + '\'' +
                ", maxAlarm='" + maxAlarm + '\'' +
                ", minAlarm='" + minAlarm + '\'' +
                ", alarmLevelId='" + alarmLevelId + '\'' +
                ", alarmLevelName='" + alarmLevelName + '\'' +
                ", deviceTypeId='" + deviceTypeId + '\'' +
                ", moudleName='" + moudleName + '\'' +
                ", SpecifyAlarm='" + SpecifyAlarm + '\'' +
                ", barCode='" + barCode + '\'' +
                ", lastValue='" + lastValue + '\'' +
                ", deviceAddress='" + deviceAddress + '\'' +
                ", time='" + time + '\'' +
                ", deviceId=" + deviceId +
                '}';
    }
}
