//package org.example.service.impl;
//
//import com.alibaba.fastjson.JSONObject;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.HttpEntity;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpMethod;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.HashMap;
//import java.util.Map;
//@Service
//public class RealTimeDataPushServiceImpl {
//
//    private static final Logger logger = LoggerFactory.getLogger(RealTimeDataPushServiceImpl.class);
//    //socket
//    @Value("${socket}")
//    private String socket;
//    public String realTimeDataSendToSocket(Integer areaInfoId, String time, Integer deviceId){
////        try {
//        Map<String,String> paramMap = new HashMap<>();
//
//        JSONObject jsonObject = new JSONObject();
//        jsonObject.put("time", time);
//        jsonObject.put("deviceId", deviceId);
//        paramMap.put("areaInfoId", areaInfoId.toString());
//        paramMap.put("data", jsonObject.toString());
//
//        RestTemplate client = new RestTemplate();
//        // 新建Http头，add方法可以添加参数
//        HttpHeaders headers = new HttpHeaders();
//        // 设置请求发送方式
//        HttpMethod method = HttpMethod.POST;
//        // 以表单的方式提交
//        //headers.setContentType(MediaType.APPLICATION_JSON);
//        // 将请求头部和参数合成一个请求
//        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(paramMap, headers);
//        return  doSocket(client,method,requestEntity);
//    }
//
//    private String doSocket(RestTemplate client, HttpMethod method, HttpEntity<Map<String, String>> requestEntity) {
//        try {
//            // 执行HTTP请求，将返回的结构使用String 类格式化（可设置为对应返回值格式的类）
//            ResponseEntity<String> response = client.exchange(socket, method, requestEntity,String.class);
//            return response.getBody();
//        }
//        catch (Exception exception){
//            logger.error("储能实时数据推送 post请求错误");
//            return "储能实时数据推送 post请求错误";
//        }
//    }
//
//    public void sendToSocket(String message) {
//
//
//    }
//}
