package org.example.service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

@Service
public class ConfigQueryService implements CommandLineRunner {

    @Value("${remoteReceiveUrl}")
    private String remoteReceiveUrl;

    @Value("${localUrl}")
    private String localUrl;

    private final RestTemplate restTemplate;

    // 使用构造函数注入 RestTemplate
    public ConfigQueryService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private Map<String, String> barCodeTypeAddressMap = new HashMap<>();

    /**
     * CommandLineRunner 中执行的方法
     * @param args 命令行参数
     * @throws Exception 异常
     */
    @Override
    public void run(String... args) throws Exception {
        System.out.println("应用启动后自动调用 getRemoteDeviceIdMap 方法...");
        init();  // 在启动时自动调用
    }

    public void init(){
        Map<String, String> temp = getRemoteDeviceIdMap();
        barCodeTypeAddressMap.putAll(temp);
    }

    /**
     * 获取远程设备ID映射
     * @return 设备ID映射
     */
    public Map<String, String> getRemoteDeviceIdMap() {
        String url = remoteReceiveUrl + "/queryDeviceIdMap";
        return doGetRequest(url, Map.class); // 使用通用的 GET 请求方法
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
    public Integer insertRemoteDeviceInfo(String barCode, String deviceName, String address, String typeCode, int protocolNum) {
        String url = remoteReceiveUrl + "/device";
        // 创建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("barCode", barCode);
        requestBody.put("deviceName", deviceName);
        requestBody.put("address", address);
        requestBody.put("typeCode", typeCode);
        requestBody.put("protocolNum", protocolNum);

        return doPostRequest(url, requestBody, Integer.class); // 使用通用的 POST 请求方法
    }

    /**
     * 获取本地设备ID映射
     * @return 本地设备ID映射
     */
    public Map<String, String> getLocalDeviceIdMap() {
        String url = localUrl + "/queryDeviceIdMap";
        return doGetRequest(url, Map.class); // 使用通用的 GET 请求方法
    }

    /**
     * 封装的 GET 请求方法
     * @param url 请求的 URL
     * @param responseType 响应类型
     * @param <T> 响应类型
     * @return 响应数据
     */
    private <T> T doGetRequest(String url, Class<T> responseType) {
        return restTemplate.getForObject(url, responseType);
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
        return restTemplate.postForObject(url, requestBody, responseType);
    }
}
