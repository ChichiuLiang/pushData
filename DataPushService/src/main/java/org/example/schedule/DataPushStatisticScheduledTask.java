package org.example.schedule;

import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONObject;
import org.example.entity.TableMapping;
import org.example.service.ConfigQueryService;
//import org.example.service.WebSocketService;
import org.example.websocket.PersistentWebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
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

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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
    @Autowired
    private PersistentWebSocketClient webSocketService;


    @Value("${pushLimit}")
    private Integer pushLimit;
//    @Resource
//    private RabbitTemplate rabbitTemplate;

    // 定时任务，定期查询数据并推送到远程服务器
    @Scheduled(cron = "0 */4 * * * ?")  // 每5分钟       执行一次
    public void pushDataToRemoteServer() {

        LocalDateTime nowTime = LocalDateTime.now();
        LocalDateTime startTime = nowTime.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endTime = nowTime;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String startTimeStr = startTime.format(formatter);
        String endTimeStr = endTime.format(formatter);

        String preTimeStart = startTime.minusDays(1).format(formatter);

        List<TableMapping> mappings = getTableMappings();
        doPush(startTimeStr, endTimeStr, mappings,new ArrayList<>());
        doPush(preTimeStart, startTimeStr, mappings,new ArrayList<>());
    }

    /**
     * 测试小时表
     */
    @Scheduled(cron = "0 */4 * * * ?")  // 每5分钟       执行一次
    public void pushDataToRemoteServerStatisticHour() {

        LocalDateTime nowTime = LocalDateTime.now();
        LocalDateTime startTime = nowTime.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endTime = nowTime;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String startTimeStr = startTime.format(formatter);
        String endTimeStr = endTime.format(formatter);

        String preTimeStart = startTime.minusDays(1).format(formatter);
        String sql = "SELECT * FROM iems_app.table_mapping where is_on =3 ";
        List<TableMapping> mappings = getTableMappings(sql);
        List<Integer> deviceIds = new ArrayList<>( );
        doPush(startTimeStr, endTimeStr, mappings,deviceIds);
        doPush(preTimeStart, startTimeStr, mappings,deviceIds);
    }


    @Scheduled(cron = "0 */1 * * * ?")  // 每2分钟       执行一次
    public void pushAlarmDataToRemoteServer() {
        LocalDateTime nowTime = LocalDateTime.now();
        LocalDateTime startTime = nowTime.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endTime = nowTime;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String startTimeStr = startTime.format(formatter);
        String endTimeStr = endTime.format(formatter);
        String preTimeStart = startTime.minusDays(1).format(formatter);
        String sql = "SELECT * FROM iems_app.table_mapping where is_on =2 ";
        List<TableMapping> mappings = getTableMappings(sql);
        doPush(startTimeStr, endTimeStr, mappings,new ArrayList<>());
        doPush(preTimeStart, startTimeStr, mappings,new ArrayList<>());
    }

    public void doPush(String startTimeStr, String endTimeStr,List<TableMapping> mappings,List<Integer> deviceIds){
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
            List<Map<String, Object>> data = queryData(sourceTable, sourceFieldList,dateField,startTimeStr, endTimeStr,convertTextFields,excludeCondition,deviceIds);
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

    public List<TableMapping> getTableMappings() {
        try {
            String sql = "SELECT * FROM iems_app.table_mapping where is_on =1 ";
            return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(TableMapping.class));
        } catch (Exception e) {
            log.error("查询表映射失败: {}", e.getMessage(), e);
            return null;
        }
    }



    public List<TableMapping> getTableMappings(String sql) {
        try {
            return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(TableMapping.class));
        } catch (Exception e) {
            log.error("查询表映射失败: {}", e.getMessage(), e);
            return null;
        }
    }

    private List<Map<String, Object>> queryData(String tableName, List<String> sourceFields, String dateField,String startTime, String endTime,String convertTextFields,String excludeCondition,List<Integer> deviceIds) {
        String fields = String.join(",", sourceFields);
        if(convertTextFields != null && !convertTextFields.isEmpty()){
            String[] convertArr = convertTextFields.split(",");
            for(String field :convertArr){
                fields = fields.replace(field,"concat("+field+") as "+field);
            }
        }
        if(tableName.equals("energy_station_fault_record")){
                fields = fields.replace("cell_number","if(concat(cell_number, ' ',fault_value) is not null,concat(cell_number, ' ',fault_value),'') as cell_number");
        }

        String sql = "SELECT " + fields + " FROM " + tableName + " WHERE " + dateField + " BETWEEN '" + startTime + "' AND '" + endTime + "'"  ;



        if(excludeCondition != null && !excludeCondition.isEmpty()){
            //添加排除条件
            sql  = sql + " AND "+ excludeCondition;
        }



        if(dateField == null){
            sql = "SELECT " + fields + " FROM " + tableName + " WHERE 1=1 ";
            if(excludeCondition != null && !excludeCondition.isEmpty()){
               //添加排除条件
               sql  = sql + " AND "+ excludeCondition;
            }

        }

        if(deviceIds != null && !deviceIds.isEmpty()){
            //添加设备id条件
            String devIds = deviceIds.stream()
                    .map(String::valueOf) // 将每个Integer转换为String
                    .collect(Collectors.joining(",")); // 使用逗号作为分隔符连接所有字符串
            sql  = sql + " AND device_id in ("+devIds+") ";
        }


        if(tableName.equals("energy_station_fault_record")){
            //报警只传最后50条记录
            sql  = sql + " order by id desc limit 50 ";
        }

        try {
            //log.info("查询数据 SQL: {}", sql);
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
            String deviceId = row.get("device_id") != null ? row.get("device_id").toString() : null;
            String convertedDeviceId = deviceId != null ? queryDeviceIdConversion(deviceId) : null;
            // 字段映射
            for (int i = 0; i < sourceFields.size(); i++) {
                String sourceField = sourceFields.get(i).trim();
                String destinationField = destinationFields.get(i).trim();
                transformedRow.put(destinationField, row.get(sourceField));
            }
            if (convertedDeviceId != null&& !convertedDeviceId.isEmpty()&& !convertedDeviceId.equals("0")&& !convertedDeviceId.equals("null")) {
                transformedRow.put("device_id", convertedDeviceId);
            }

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
    private void pushToRemoteServer(String destinationTable, Map<String, Object> transformedRow, String uniqueFields, String destinationFields) {
        try {
            // 构建发送的消息体
            Map<String, Object> messageBody = new HashMap<>(transformedRow);
            messageBody.put("destination_table", destinationTable);
            messageBody.put("unique_fields", uniqueFields);
            messageBody.put("destination_fields", destinationFields);

            // 转换为 JSON 字符串
            String jsonMessage = JSONObject.fromObject(messageBody).toString();

            // 使用 RabbitMQ 发送消息到队列 receiveData-statistic
            webSocketService.sendStatistic(jsonMessage);
        } catch (Exception e) {
            log.error("通过 RabbitMQ 推送统计数据失败: {}", e.getMessage(), e);
        }
    }


    @Scheduled(cron = "0 0 */16 * * ?")
    public void initDataPushConfigQuery() {
        configQueryService.init();
    }
}
