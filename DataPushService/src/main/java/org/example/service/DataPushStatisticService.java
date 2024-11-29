package org.example.service;

import com.alibaba.fastjson.JSONObject;
import org.example.entity.TableMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DataPushStatisticService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${remoteReceiveUrl}")
    private String remoteReceiveUrl;

    // 定时任务，定期查询数据并推送到服务B
    @Scheduled(cron = "0 0 * * * ?")  // 每小时执行一次
    public void pushDataToServiceB() {
        List<TableMapping> mappings = getTableMappings();
        for (TableMapping mapping : mappings) {
            String sourceTable = mapping.getSourceTable();
            String destinationTable = mapping.getDestinationTable();
            String sourceFields = mapping.getSourceFields();
            String destinationFields = mapping.getDestinationFields();

            // 将源字段和目标字段转为列表
            List<String> sourceFieldList = Arrays.asList(sourceFields.split(","));
            List<String> destinationFieldList = Arrays.asList(destinationFields.split(","));

            // 从源表查询数据
            List<Map<String, Object>> data = queryData(sourceTable);

            // 转换字段和设备ID
            for (Map<String, Object> row : data) {
                Map<String, Object> transformedRow = transformRow(row, sourceFieldList, destinationFieldList);
                // 将转换后的数据推送到服务B
                pushToServiceB(destinationTable, transformedRow);
            }
        }
    }

    private List<TableMapping> getTableMappings() {
        // 查询所有的表映射配置
        String sql = "SELECT * FROM table_mapping";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(TableMapping.class));
    }

    private List<Map<String, Object>> queryData(String tableName) {
        // 根据表名查询数据（这里以查询所有数据为例）
        String sql = "SELECT * FROM " + tableName;
        return jdbcTemplate.queryForList(sql);
    }

    private Map<String, Object> transformRow(Map<String, Object> row, List<String> sourceFields, List<String> destinationFields) {
        Map<String, Object> transformedRow = new HashMap<>();

        // 设备ID转换：通过查询服务器B获取设备ID的转换
        String deviceId = (String) row.get("device_id");
        String convertedDeviceId = queryDeviceIdConversion(deviceId);
        transformedRow.put("device_id", convertedDeviceId);

        // 字段映射
        for (int i = 0; i < sourceFields.size(); i++) {
            String sourceField = sourceFields.get(i).trim();
            String destinationField = destinationFields.get(i).trim();
            transformedRow.put(destinationField, row.get(sourceField));
        }
        return transformedRow;
    }

    private String queryDeviceIdConversion(String deviceId) {
        // 查询服务器B获取设备ID的转换规则（假设服务器B有一个API接口能根据deviceId查询转换结果）
        String url = remoteReceiveUrl + "/api/device/id/" + deviceId;
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, null, String.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                // 假设返回的JSON结构是 { "converted_device_id": "NEW_ID" }
                JSONObject jsonResponse = new JSONObject(Boolean.parseBoolean(response.getBody()));
                return jsonResponse.getString("converted_device_id");
            }
        } catch (Exception e) {
            // 如果查询失败，则返回原始设备ID
            e.printStackTrace();
        }
        return deviceId;  // 如果转换失败，返回原始设备ID
    }

    private void pushToServiceB(String destinationTable, Map<String, Object> transformedRow) {
        // 将转换后的数据通过HTTP请求推送到服务B
        String url = remoteReceiveUrl + "/api/data";
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(transformedRow);
        restTemplate.postForEntity(url, request, String.class);
    }
}
