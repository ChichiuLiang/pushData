package org.example.schedule;

import lombok.extern.slf4j.Slf4j;
import org.example.entity.TableMapping;
import org.example.service.ConfigQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Slf4j
@Component
public class DataPushStatisticScheduledTask {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${remoteReceiveUrl}")
    private String remoteReceiveUrl;

    @Autowired
    private ConfigQueryService configQueryService;

    // 定时任务，定期查询数据并推送到远程服务器
    @Scheduled(cron = "0 */15 * * * ?")  // 每15分钟       执行一次
    public void pushDataToRemoteServer() {

        LocalDateTime nowTime = LocalDateTime.now();
        LocalDateTime startTime = nowTime.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endTime = nowTime;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String startTimeStr = startTime.format(formatter);
        String endTimeStr = endTime.format(formatter);

        String preTimeStart = startTime.minusDays(1).format(formatter);
        doPush(startTimeStr, endTimeStr);
        doPush(preTimeStart, startTimeStr);
    }

    public void doPush(String startTimeStr, String endTimeStr){
        List<TableMapping> mappings = getTableMappings();
        if (mappings == null || mappings.isEmpty()) {
            log.warn("没有找到表映射配置，跳过数据推送");
            return;
        }

        for (TableMapping mapping : mappings) {
            String sourceTable = mapping.getSourceTable();
            String destinationTable = mapping.getDestinationTable();
            List<String> sourceFieldList = Arrays.asList(mapping.getSourceFields().split(","));
            List<String> destinationFieldList = Arrays.asList(mapping.getDestinationFields().split(","));
            String uniqueFields = mapping.getUniqueFields();
            String dateField = mapping.getDateField();
            String destinationFields = mapping.getDestinationFields();
            String convertTextFields = mapping.getConvertTextFields();
            String excludeCondition = mapping.getExcludeCondition();
            // 从源表查询数据
            List<Map<String, Object>> data = queryData(sourceTable, sourceFieldList,dateField,startTimeStr, endTimeStr,convertTextFields,excludeCondition);
            if (data == null || data.isEmpty()) {
                log.warn("表 [{}] 无数据需要推送", sourceTable);
                continue;
            }

            // 处理并推送数据
            for (Map<String, Object> row : data) {
                try {
                    Map<String, Object> transformedRow = transformRow(row, sourceFieldList, destinationFieldList);
                    if (transformedRow != null) {
                        // 将转换后的数据推送到接收服务
                        pushToRemoteServer(destinationTable, transformedRow, uniqueFields,destinationFields);
                    }
                } catch (Exception e) {
                    log.error("处理数据时出错: {}", e.getMessage(), e);
                    // 继续处理下一条数据
                }
            }
        }
    }

    private List<TableMapping> getTableMappings() {
        try {
            String sql = "SELECT * FROM table_mapping";
            return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(TableMapping.class));
        } catch (Exception e) {
            log.error("查询表映射失败: {}", e.getMessage(), e);
            return null;
        }
    }

    private List<Map<String, Object>> queryData(String tableName, List<String> sourceFields, String dateField,String startTime, String endTime,String convertTextFields,String excludeCondition) {
        String fields = String.join(",", sourceFields);
        if(convertTextFields != null && !convertTextFields.isEmpty()){
            String[] convertArr = convertTextFields.split(",");
            for(String field :convertArr){
                fields = fields.replace(field,"concat("+field+") as "+field);
            }
        }

        String sql = "SELECT " + fields + " FROM " + tableName + " WHERE " + dateField + " BETWEEN '" + startTime + "' AND '" + endTime + "'"  ;
        if(excludeCondition != null && !excludeCondition.isEmpty()){
            //添加排除条件
            sql  = sql + " AND "+ excludeCondition;
        }


        if(dateField == null){
            sql = "SELECT " + fields + " FROM " + tableName;
            if(excludeCondition != null && !excludeCondition.isEmpty()){
               //添加排除条件
               sql  = sql + " WHERE "+ excludeCondition;
            }
        }
        try {
            log.info("查询数据 SQL: {}", sql);
            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            log.error("查询数据失败, SQL: {} 错误信息: {}", sql, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 每一条记录 的每个字段
     * @param row
     * @param sourceFields
     * @param destinationFields
     * @return
     */
    private Map<String, Object> transformRow(Map<String, Object> row, List<String> sourceFields, List<String> destinationFields) {
        Map<String, Object> transformedRow = new HashMap<>();

        try {
            // 设备ID转换
            String deviceId = row.get("device_id").toString();
            String convertedDeviceId = queryDeviceIdConversion(deviceId);
            if (convertedDeviceId == null || convertedDeviceId.equals("null")) {
                return null;
            }
            // 字段映射
            for (int i = 0; i < sourceFields.size(); i++) {
                String sourceField = sourceFields.get(i).trim();
                String destinationField = destinationFields.get(i).trim();
                transformedRow.put(destinationField, row.get(sourceField));
            }
            transformedRow.put("device_id", convertedDeviceId);
        } catch (Exception e) {
            log.error("字段转换失败: {}", e.getMessage(), e);
            return null;  // 如果发生异常，跳过此行数据
        }
        return transformedRow;
    }

    private String queryDeviceIdConversion(String deviceId) {
        try {
            return String.valueOf(configQueryService.getRemoteDeviceIdByLocalDeviceId(Integer.parseInt(deviceId)));
        } catch (Exception e) {
            log.error("设备ID转换失败: {}", e.getMessage(), e);
            return null; // 返回null表示转换失败
        }
    }

    /**
     * 将转换后的数据推送到接收服务，包含 unique_fields 以确保唯一性
     * @param destinationTable 目标表
     * @param transformedRow 转换后的数据
     * @param uniqueFields 唯一字段
     */
    private void pushToRemoteServer(String destinationTable, Map<String, Object> transformedRow, String uniqueFields,String destinationFields) {
        // 将转换后的数据通过HTTP请求推送到远程接收服务
        String url = remoteReceiveUrl + "/statistic";
        transformedRow.put("destination_table", destinationTable);  // 将目标表名传递给接收服务
        transformedRow.put("unique_fields", uniqueFields);
        transformedRow.put("destination_fields",destinationFields);
        // 将唯一字段传递给接收服务
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(transformedRow);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                //log.info("数据成功推送到接收服务: {}", response.getBody());
            } else {
                log.warn("定时任务推送统计数据失败，状态码: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("定时任务推送统计数据失败: {} {}  ", e.getMessage(), transformedRow.toString());
        }
    }

    @Scheduled(cron = "0 0 */16 * * ?")
    public void initDataPushConfigQuery() {
        configQueryService.init();
    }
}
