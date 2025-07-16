package org.example.service;
import lombok.extern.slf4j.Slf4j;
import org.example.utils.ListUtil;
import org.example.utils.ReplaceGatewayUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Slf4j
@Service
public class ConfigQueryService implements CommandLineRunner {

    @Value("${remoteReceiveUrl}")
    private String remoteReceiveUrl;

    @Value("${localUrl}")
    private String localUrl;

    private final RestTemplate restTemplate;
    @Autowired
    private EntityManager entityManager;

    // 使用构造函数注入 RestTemplate
    public ConfigQueryService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    @Autowired
    public QuickQueryService quickQueryConfigService;
    @Value("${energyName}")
    private String energyName;
    private Map<String, String> remoteBarCodeTypeAddressMap = new HashMap<>();
    private Map<String, String> localDeviceIdToBarCodeTypeAddressMap = new HashMap<>();
    private Map<String, String> localDeviceIdToInsertRemoteInfoMap = new HashMap<>();
    private List<Integer> localDeviceIds = new ArrayList<>();
    /**
     * CommandLineRunner 中执行的方法
     * @param args 命令行参数
     * @throws Exception 异常
     */
    @Override
    public void run(String... args) throws Exception {
        log.info("应用启动后自动调用 getRemoteDeviceIdMap 方法...");
        init();  // 在启动时自动调用
    }

    public void init(){
        try {
            Map<String, String> temp = getRemoteDeviceIdMap();
            if(temp == null){
                temp = new HashMap<>();
            }
            remoteBarCodeTypeAddressMap.putAll(temp);
            //log.info("初始化 远程设备设备集 本地网关号类型地址:远程deviceId ");

            Map<String, String> temp2 = getLocalDeviceIdToBarCodeTypeAddressMap();
            if(temp2 == null){
                temp2 = new HashMap<>();
            }
            localDeviceIdToBarCodeTypeAddressMap.putAll(temp2);

            Map<String, String> temp3 = getLocalDeviceIdToInsertRemoteInfoMap();//deviceId : 发送到远程的信息集合
            if(temp3 == null){
                temp3 = new HashMap<>();
            }
            localDeviceIdToInsertRemoteInfoMap.putAll(temp3);

            getLocalDeviceIds(localDeviceIds);

            log.info("初始化 本地设备数据集 本地deviceId:本地网关号类型地址 ");
        } catch (Exception e) {
            log.error("获取远程设备ID映射失败：" + e.getMessage());
            e.printStackTrace();
        }

    }

    private void getLocalDeviceIds(List<Integer> localDeviceIds) {
        localDeviceIds.clear();
        String sql="SELECT id FROM iems_app.device_base_info where id>0 ";
        List<Integer> deviceIds = entityManager.createNativeQuery(sql).getResultList();
        if(ListUtil.isNull(deviceIds)){
            return;
        }
        localDeviceIds.addAll(deviceIds);
    }

    /**
     * 获取本地设备ID映射
     * @return 本地设备ID映射
     */
    public Map<String, String> getLocalDeviceIdToBarCodeTypeAddressMap() {
        String sql = "SELECT dbi.id,concat(bar_code,dt.type_code,address) FROM iems_app.device_base_info dbi\n" +
                "join home_info hi on hi.id = dbi.home_id\n" +
                "join device_type dt on dt.id = dbi.device_type_id  \n" +
                " where dbi.id > 0 and dbi.home_id>0 and bar_code is not null \n" +
                "order by home_id,device_type_id,address";
        if("shequ".equals(energyName)){
            //社区的网关号需要映射
            Map<String, String> temp = quickQueryConfigService.queryForMapString(sql);
            Map<String, String> result = new HashMap<>();
            for(Map.Entry<String, String> entry : temp.entrySet()){
                //社区网关号做映射
                String value = ReplaceGatewayUtil.replaceGatewayIds(entry.getValue());
                result.put(entry.getKey(), value);
            }
            return result;
        }
        else if("shangJiaoDa".equals(energyName)){
            //社区的网关号需要映射
            Map<String, String> temp = quickQueryConfigService.queryForMapString(sql);
            Map<String, String> result = new HashMap<>();
            for(Map.Entry<String, String> entry : temp.entrySet()){
                //社区网关号做映射
                String value = ReplaceGatewayUtil.replaceGatewayIdsBySJD(entry.getValue());
                result.put(entry.getKey(), value);
            }
            return result;
        }
        else{
            //如果不是社区的能源站,则无需映射
            return quickQueryConfigService.queryForMapString(sql);
        }
    }



    public Map<String, String> getLocalDeviceIdToInsertRemoteInfoMap() {
        String sql = "SELECT dbi.id,concat(bar_code,'==',IF(dbi.name IS NOT NULL AND dbi.name != '', dbi.name, '上传'),'==',address,'==',dt.type_code,'==',pv.protocol_num) FROM iems_app.device_base_info dbi\n" +
                "join home_info hi on hi.id = dbi.home_id\n" +
                "join device_type dt on dt.id = dbi.device_type_id\n" +
                "join protocol_version pv on pv.id = dt.protocol_id\n" +
                " where dbi.id > 0 and dbi.home_id>0\n" +
                "order by home_id,device_type_id,address";
        if("shequ".equals(energyName)){
            //社区的网关号需要映射
            Map<String, String> temp = quickQueryConfigService.queryForMapString(sql);
            Map<String, String> result = new HashMap<>();
            for(Map.Entry<String, String> entry : temp.entrySet()){
                //社区网关号做映射
                String value = ReplaceGatewayUtil.replaceGatewayIds(entry.getValue());
                result.put(entry.getKey(), value);
            }
            return result;
        }
        else if("shangJiaoDa".equals(energyName)){
            //社区的网关号需要映射
            Map<String, String> temp = quickQueryConfigService.queryForMapString(sql);
            Map<String, String> result = new HashMap<>();
            for(Map.Entry<String, String> entry : temp.entrySet()){
                //社区网关号做映射
                String value = ReplaceGatewayUtil.replaceGatewayIdsBySJD(entry.getValue());
                result.put(entry.getKey(), value);
            }
            return result;
        }
        else{
            return quickQueryConfigService.queryForMapString(sql);
        }



    }

    /**
     * 获取远程设备ID映射
     * @return 设备ID映射
     */
    //  "52 00 00 00 00 00 00 00 00 00 00 00 00 00 00240222": "1030168",
    public Map<String, String> getRemoteDeviceIdMap() {
        String url = remoteReceiveUrl + "/queryDeviceIdMap";
        return doGetRequest(url, Map.class); // 使用通用的 GET 请求方法
    }

    public Integer getRemoteDeviceId(String barCodeTypeAddress ){
        String devId = remoteBarCodeTypeAddressMap.getOrDefault(barCodeTypeAddress, null);
        if(devId != null){
            return Integer.parseInt(devId);
        }else{
            return null;
        }
    }


    public String getLocalDeviceIdToBarCodeTypeAddressMap(String deviceId) {
        return localDeviceIdToBarCodeTypeAddressMap.getOrDefault(deviceId, null);
    }

    public Integer getRemoteDeviceIdByLocalDeviceId(Integer deviceId ){
        if(!localDeviceIds.contains(deviceId)){
            return null;
            //本地无此设备id,则返回
        }
        String barCodeTypeAddress = getLocalDeviceIdToBarCodeTypeAddressMap(String.valueOf(deviceId));
        Integer remoteDeviceId ;
        if(barCodeTypeAddress != null){
            remoteDeviceId = getRemoteDeviceId(barCodeTypeAddress);
        }else{
            //log.error("本地设备ID:{} 本地未找到 BarCodeTypeAddress", deviceId);
            //init();
            return null;
        }
        if(remoteDeviceId != null){
            return remoteDeviceId;
        }
        //远程未注册
        String insertRemoteInfo = localDeviceIdToInsertRemoteInfoMap.getOrDefault(deviceId.toString(), null);
        if(insertRemoteInfo != null){
            //log.error("本地设备ID:{} 在本地设备ID未在本地数据库map找到 InsertRemoteInfo", deviceId);
            String[] insertRemoteInfoArray = insertRemoteInfo.split("==");
            if(insertRemoteInfoArray.length == 5){
                String barCode = insertRemoteInfoArray[0];
                String deviceName = insertRemoteInfoArray[1];
                String address = insertRemoteInfoArray[2];
                String typeCode = insertRemoteInfoArray[3];
                int protocolNum = Integer.parseInt(insertRemoteInfoArray[4]);
                remoteDeviceId = insertRemoteDeviceInfo(barCode, deviceName, address, typeCode, protocolNum);
            }
        }
        else{
            log.error("远程注册设备失败");
        }
        return remoteDeviceId;
    }

    /**
     * 插入设备信息
     * @param barCode 条形码
     * @param deviceName 设备名称
     * @param address 地址
     * @param typeCode 类型码
     * @param protocolNum 协议号
     * @return 设备ID
     */
    public Integer insertRemoteDeviceInfo(String barCode, String deviceName, String address, String typeCode, Integer protocolNum) {
        String url = remoteReceiveUrl + "/device";
        // 创建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("barCode", barCode);
        requestBody.put("deviceName", deviceName);
        requestBody.put("deviceAddr", address);
        requestBody.put("devTypeCode", typeCode);
        requestBody.put("protocolNum", protocolNum);

        return doPostRequest(url, requestBody, Integer.class); // 使用通用的 POST 请求方法
    }


    /**
     * 封装的 POST 请求方法
     * @param url 请求的 URL
     * @param requestBody 请求体
     * @param responseType 响应类型
     * @param <T> 响应类型
     * @return 响应数据
     */
    private <T> T doPostRequest(String url, Object requestBody, Class<T> responseType) {
        try {
            return restTemplate.postForObject(url, requestBody, responseType);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }

    }


    /**
     * 封装的 GET 请求方法
     * @param url 请求的 URL
     * @param responseType 响应类型
     * @param <T> 响应类型
     * @return 响应数据
     */
    private <T> T doGetRequest(String url, Class<T> responseType) {
        try {
            return restTemplate.getForObject(url, responseType);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

}
