package org.example.repository;

import org.example.entity.DeviceMonitorRecordV2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DeviceMonitorRecordV2Repository extends JpaRepository<DeviceMonitorRecordV2, Long> {
//    @Query("SELECT new DeviceMonitorRecordV2(t.device_atrribute_id, t.device_id, t.occur_time, t.recover_time, t.status) FROM DeviceMonitorRecordV2 t WHERE t.device_id = :deviceId AND t.frame_datetime BETWEEN :startFrameDatetime AND :endFrameDatetime")
//    List<DeviceMonitorRecordV2> findValuesByDeviceIdAndFrameDatetimeBetween(int deviceId, LocalDateTime startFrameDatetime, LocalDateTime endFrameDatetime);

    @Query("SELECT t FROM DeviceMonitorRecordV2 t WHERE t.device_id = ?2 AND t.device_atrribute_id = ?1 and t.status !=0")
    List<DeviceMonitorRecordV2> findByAttributeId(Integer deviceAtrributeId,Integer deviceId);

    @Query("SELECT t FROM DeviceMonitorRecordV2 t WHERE t.device_id = ?2 AND t.device_atrribute_id = ?1 and  t.occur_time between ?3 and ?4 ")
    List<DeviceMonitorRecordV2> findByAtrributeIdToday(Integer deviceAtrributeId, Integer deviceId, LocalDateTime start, LocalDateTime  end);
    // 更新 deviceId 字段方法
    @Modifying
    @Query("UPDATE DeviceMonitorRecordV2 SET device_id = :deviceId WHERE id = :id")
    void updateDeviceId(@Param("deviceId") Integer deviceId, @Param("id") Long id);

    // 更新 occurTime 字段方法
    @Modifying
    @Query("UPDATE DeviceMonitorRecordV2 SET occur_time = :occurTime WHERE id = :id")
    void updateOccurTime(@Param("occurTime") LocalDateTime occurTime, @Param("id") Long id);

    // 更新 recoverTime 字段方法
    @Modifying
    @Query("UPDATE DeviceMonitorRecordV2 SET recover_time = :recoverTime WHERE id = :id")
    void updateRecoverTime(@Param("recoverTime") LocalDateTime recoverTime, @Param("id") Long id);

    // 更新 status 字段方法
    @Modifying
    @Query("UPDATE DeviceMonitorRecordV2 SET status = :status WHERE id = :id")
    void updateStatus(@Param("status") Integer status, @Param("id") Long id);

    // 删除数据方法
    @Modifying
    @Query("DELETE FROM DeviceMonitorRecordV2 WHERE id = :id")
    void deleteEnergyData(@Param("id") Long id);

    @Query(value = "select a.device_id,a.occur_time,a.recover_time,a.status,b.attribute_name_cn from device_monitor_record_v2 a\n" +
            "inner join device_atrribute b on a.device_atrribute_id = b.id\n" +
            "where device_id = ?1 and occur_time between ?2 and ?3",nativeQuery = true)
    List<Object> queryEnergyDeviceAlarmRecord(Integer deviceId,String startTime,String endTime);


    @Query(value = "SELECT phone_number FROM iems_app.alarm_user_info_bigdata where bigdata_db_area_level_id in (?1) ",nativeQuery = true)
    List<String> queryPhoneNumber(List<Integer> bigDataAreaLevelIds);

    @Modifying
    @Transactional
    @Query(value = "insert  into iems_app.device_monitor_record_v2(device_atrribute_id, device_id, occur_time,   status, is_alarmed, is_canceled, last_value)  values( ?, ?, ?, ?, ?, ?, ?) ",nativeQuery = true)
    void insertRecords(Integer deviceAtrributeId, Integer deviceId, String occurTime,   Integer status, Integer isAlarm, Integer isCancel, String lastValue);

    @Modifying
    @Transactional
    @Query(value = "update iems_app.device_monitor_record_v2 set device_atrribute_id=?1, device_id=?2,   recover_time=?3, status =?4, is_alarmed =?5, is_canceled=?6 \n" +
            "where id = ?7 ",nativeQuery = true)
    void updateRecords(Integer deviceAtrributeId, Integer deviceId,  String recoverTime, Integer status, Integer isAlarm, Integer isCancel, Integer id);

    @Query(value = "SELECT id FROM iems_app.device_monitor_record_v2 where device_id = ?1 and device_atrribute_id = ?2 and is_alarmed = 1 and is_canceled =0 ",nativeQuery = true)
    List<Integer> queryIdsRecordExist(Integer deviceId, Integer attributeId);

    @Query(value = "SELECT count(*) FROM iems_app.device_monitor_record_v2 where device_id = ?1 and device_atrribute_id = ?2 and is_alarmed = 1 and is_canceled =0 ",nativeQuery = true)
    Integer queryAlarmRecordExist(Integer deviceId, Integer attributeId);
}