package org.example.model;

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


    public String getAttributeId() {
        return attributeId;
    }

    public void setAttributeId(String attributeId) {
        this.attributeId = attributeId;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }

    public String getAttributeNo() {
        return attributeNo;
    }

    public void setAttributeNo(String attributeNo) {
        this.attributeNo = attributeNo;
    }

    public String getAttributeNameCn() {
        return attributeNameCn;
    }

    public void setAttributeNameCn(String attributeNameCn) {
        this.attributeNameCn = attributeNameCn;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getAlarmValue() {
        return alarmValue;
    }

    public void setAlarmValue(String alarmValue) {
        this.alarmValue = alarmValue;
    }

    public String getMaxAlarm() {
        return maxAlarm;
    }

    public void setMaxAlarm(String maxAlarm) {
        this.maxAlarm = maxAlarm;
    }

    public String getMinAlarm() {
        return minAlarm;
    }

    public void setMinAlarm(String minAlarm) {
        this.minAlarm = minAlarm;
    }

    public String getAlarmLevelId() {
        return alarmLevelId;
    }

    public void setAlarmLevelId(String alarmLevelId) {
        this.alarmLevelId = alarmLevelId;
    }

    public String getAlarmLevelName() {
        return alarmLevelName;
    }

    public void setAlarmLevelName(String alarmLevelName) {
        this.alarmLevelName = alarmLevelName;
    }

    public String getDeviceTypeId() {
        return deviceTypeId;
    }

    public void setDeviceTypeId(String deviceTypeId) {
        this.deviceTypeId = deviceTypeId;
    }

    public String getMoudleName() {
        return moudleName;
    }

    public void setMoudleName(String moudleName) {
        this.moudleName = moudleName;
    }

    public String getBarCode() {
        return barCode;
    }

    public void setBarCode(String barCode) {
        this.barCode = barCode;
    }

    public String getLastValue() {
        return lastValue;
    }

    public void setLastValue(String lastValue) {
        this.lastValue = lastValue;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public void setDeviceAddress(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getSpecifyAlarm() {
        return SpecifyAlarm;
    }

    public void setSpecifyAlarm(String specifyAlarm) {
        SpecifyAlarm = specifyAlarm;
    }

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
                ", barCode='" + barCode + '\'' +
                ", lastValue='" + lastValue + '\'' +
                ", deviceAddress='" + deviceAddress + '\'' +
                ", time='" + time + '\'' +
                '}';
    }
}
