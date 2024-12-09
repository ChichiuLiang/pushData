package org.example.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.model.AlarmConfigModel;
import org.example.service.AlarmTriggerService;
import org.example.utils.ListUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
@Slf4j
@Service
public class AlarmHandleServiceImpl {

    private static final long ALARM_COOLDOWN_MILLIS = 24 * 60 * 60 * 1000; // 10 minutes in milliseconds
    private static final long CANCEL_ALARM_COOLDOWN_MILLIS = 20 * 60 * 1000; // 10 minutes in milliseconds
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private AlarmTriggerService alarmTriggerService;
    @Autowired
    private AlarmConfigAttributeServiceImpl attributeService;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final Logger logger = LoggerFactory.getLogger(AlarmHandleServiceImpl.class);


//    @Autowired
//    private AlarmMethodsServiceImpl alarmMethodsService;

    //"{\"frameType\":\"17\",\"deviceType\":\"1312_V1_1\",\"factorySign\":\"0000\",\"address\":\"13\",\"dataType\":\"3\",\"data\":{\"B0\":0},\"dateTime\":\"2024-12-08 16:41:37\"}"
    public void processMessage(String topic,String message) {
        try {
            // 检查消息的合法性
            if (message == null || message.isEmpty()) {
                return;
            }
            logger.info(" {} Received message: {}",topic, message);
//            // 去除最外层的双引号并解码转义字符
//            message = message.trim().replaceAll("^\"|\"$", "").replace("\\\"", "\"");
            // 解析 JSON 消息
            JsonNode jsonMessage = objectMapper.readTree(message);
            JsonNode dataNode = jsonMessage.path("data");
            String frameType = jsonMessage.get("frameType").asText();

            // 非插入操作直接过滤
            if (!"17".equals(frameType)||topic==null) {
                return;
            }

            //alarmMethodsService.sendToSocketRealTimeData(message);
            // 提取关键字段
            String barCode = topic.replace("IEMS_", "");
            if (barCode.length() > 44) {
                return;
            }
            int dataType = jsonMessage.path("dataType").asInt();
            String deviceAddress = jsonMessage.path("address").asText();
            String deviceTypeCode = jsonMessage.path("deviceType").asText();
            String dateTime = jsonMessage.path("dateTime").asText();

            // 从缓存或服务获取设备模型配置
            Map<String, List<AlarmConfigModel>> typeCodeToModels = attributeService.getTypeCodeToModels();
            List<AlarmConfigModel> models = typeCodeToModels.get(deviceTypeCode + "-" + dataType);

            // 模型配置为空直接返回
            if (ListUtil.isNull(models)) {
                return;
                }

            // 遍历该一个协议的模型配置，检查报警条件
            for (AlarmConfigModel model : models) {
                if (model == null || model.getDataType() == null) {
                    continue;
                }

                String fieldName = model.getAttributeNo(); // 属性编号
                String maxAlarm = model.getMaxAlarm();
                String minAlarm = model.getMinAlarm();
                String specifyAlarm = model.getSpecifyAlarm();
                String fieldValue = dataNode.path(fieldName).asText().trim();

                // 字段值为空跳过
                if (fieldValue.isEmpty()) {
                    continue;
                }

                // 设置模型相关信息
                model.setBarCode(barCode);
                model.setDeviceAddress(deviceAddress);
                model.setLastValue(fieldValue);
                model.setTime(dateTime);

                // 生成 Redis Key
                String redisKey = generateRedisKey(barCode, deviceTypeCode, String.valueOf(dataType), deviceAddress, fieldName );

                // 检查是否报警
                boolean isAlarm = checkAlarm(fieldValue, maxAlarm, minAlarm, specifyAlarm);

                if (isAlarm) {
                    // 特殊编码处理
                    if (fieldValue.codePointAt(0) == '0') {
                        logger.error("Unicode 编码问题: fieldName={}, fieldValue={}", fieldName, fieldValue);
                        return;
                    }

                    // 值为0触发报警
                    if ("0".equals(fieldValue)) {
                        logger.error("值为0 报警判断错误: message={}", message);
                        return;
                    }

                    // 处理报警
                    handleAlarm(redisKey, model,message);
                } else {
                    // 解除报警
                    handleCancelAlarm(redisKey, model);
                }
            }
        } catch (IOException e) {
            logger.error("解析消息出错: message={}, error={}", message, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("处理消息出错: message={}, error={}", message, e.getMessage(), e);
        }
    }




    private String generateRedisKey(String barCode, String deviceTypeCode,String dataType, String deviceAddress, String fieldName ) {
        return   "alarm:" + barCode + "_" + deviceTypeCode + "_" + dataType +  "_" +  deviceAddress + "_" + fieldName;
    }

    /**
     * 处理报警
     *
     * @param redisKey
     * @param model
     * @param message
     */
    private void handleAlarm(String redisKey, AlarmConfigModel model, String message) {
        // Lua 脚本，用于原子操作
        String script =
                "local lastAlarmTime = tonumber(redis.call('GET', KEYS[1])) " +
                        "if lastAlarmTime == nil or (tonumber(ARGV[1]) - lastAlarmTime >= tonumber(ARGV[2])) then " +
                        "  redis.call('SET', KEYS[1], ARGV[1]) " +
                        "  return 1 " +
                        "else " +
                        "  return 0 " +
                        "end";

        // 获取当前时间戳
        long currentTime = System.currentTimeMillis();

        // 执行 Lua 脚本
        Long result = (Long) redisTemplate.execute(new DefaultRedisScript<>(script, Long.class),
                Collections.singletonList(redisKey),
                String.valueOf(currentTime),
                String.valueOf(ALARM_COOLDOWN_MILLIS));

        // 如果脚本返回 1，表示可以触发报警
        if (result != null && result == 1) {
            // 执行报警
            alarmTriggerService.triggerAlarmAsync(redisKey, model, message);
        }
    }



    private void handleCancelAlarm(String redisKey, AlarmConfigModel model) {
        // Lua 脚本，用于原子操作
        String script =
                "local lastAlarmTime = tonumber(redis.call('GET', KEYS[1])) " +
                        "if lastAlarmTime ~= nil and (tonumber(ARGV[1]) - lastAlarmTime >= tonumber(ARGV[2])) then " +
                        "  redis.call('DEL', KEYS[1]) " +  // 删除 key，表示取消报警
                        "  return 1 " +
                        "else " +
                        "  return 0 " +
                        "end";

        // 获取当前时间戳
        long currentTime = System.currentTimeMillis();

        // 执行 Lua 脚本
        Long result = (Long) redisTemplate.execute(new DefaultRedisScript<>(script, Long.class),
                Collections.singletonList(redisKey),
                String.valueOf(currentTime),
                String.valueOf(ALARM_COOLDOWN_MILLIS));

        // 如果脚本返回 1，表示可以取消报警
        if (result != null && result == 1) {
            // 执行取消报警
            alarmTriggerService.cancelAlarmAsync(redisKey, model);
        }
    }

    /**
     * 判断是否为报警
     * @param fieldValue
     * @param maxAlarm
     * @param minAlarm
     * @param specifyAlarm
     * @return
     */
    private boolean checkAlarm(String fieldValue, String maxAlarm, String minAlarm, String specifyAlarm) {
        boolean isAlarm = false;
        if (fieldValue == null) {
            return isAlarm;
        }

        // 特殊值报警
        if (specifyAlarm != null) {
            String[] arr = specifyAlarm.split(",");
            for (String value : arr) {
                if (fieldValue.equalsIgnoreCase(value)) {  // 使用 equalsIgnoreCase 忽略大小写差异
                    isAlarm = true;
                    return isAlarm;
                }
            }
        }

        // 尝试将十六进制字符串转换为 long
        try {
            long fieldVal = Long.parseLong(fieldValue, 16);
            Long maxVal = maxAlarm != null ? Long.parseLong(maxAlarm, 16) : null;
            Long minVal = minAlarm != null ? Long.parseLong(minAlarm, 16) : null;

            // 阈值报警
            if (maxVal != null && fieldVal >= maxVal) {
                isAlarm = true;
            } else if (minVal != null && fieldVal <= minVal) {
                isAlarm = true;
            }
        } catch (NumberFormatException e) {
            // 处理转换失败的情况
            System.err.println("Invalid hexadecimal value: " + e.getMessage());
        }

        return isAlarm;
    }




}