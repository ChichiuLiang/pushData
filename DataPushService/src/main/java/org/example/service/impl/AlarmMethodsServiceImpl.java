//package org.example.service.impl;
//
//import com.alibaba.fastjson.JSON;
//import com.alibaba.fastjson.JSONObject;
//import org.example.model.HomeInfoModel;
//import org.example.utils.HttpClient;
//import org.example.utils.ListUtil;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.HttpEntity;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpMethod;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Service;
//import org.springframework.util.LinkedMultiValueMap;
//import org.springframework.util.MultiValueMap;
//import org.springframework.util.StringUtils;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@Service
//public class AlarmMethodsServiceImpl {
//    private static final Logger logger = LoggerFactory.getLogger(AlarmMethodsServiceImpl.class);
//    @Autowired
//    private RestTemplate restTemplate;
//    //短信
//    @Value("${sms}")
//    private String sms;
//    @Value("${userId}")
//    private String userId;
//    @Value("${password}")
//    private String password;
//
//    //邮件
//    @Value("${email}")
//    private String email;
//
//    //socket
//    @Value("${socket}")
//    private String socket;
//
//    @Value("${socketRealTimeData}")
//    private String socketRealTimeData;
//    @Autowired
//    private AlarmConfigAttributeServiceImpl alarmConfigAttributeService;
//    //新的发送短信接口
//    public void sendSMSNew(Integer templateId, List<Integer> areaLevelIds, String[] keywords) {
////        String url = "http://10.2.44.199/alarmUser/sendSmS";
////
////        List<Integer> levelList = new ArrayList<>();
////        if (areaLevelIds.size() > 0) {
////            for (Integer areaLevelId : areaLevelIds) {
////                if (areaLevelId.intValue() != 238 || areaLevelId.intValue() != 355
////                        || areaLevelId.intValue() != 490 || areaLevelId.intValue() != 479
////                        || areaLevelId.intValue() != 356 || areaLevelId.intValue() != 358
////                        || areaLevelId.intValue() != 342) {
////                    levelList.add(areaLevelId);
////                }
////            }
////        }
////        if (areaLevelIds.isEmpty()) {
////            areaLevelIds.add(2);
////        }
////        String ids = areaLevelIds.stream().map(String::valueOf).collect(Collectors.joining(","));
////        List<Integer> areaIds = entityManager.createNativeQuery("select area_id from area_level where id in (" + ids + ")").getResultList();
////        if (areaIds.size() > 0 && areaIds.get(0) != 300){
////            levelList.add(355);
////        }
////        List<String> phoneList = alarmUserRepository.findPhoneNumber(levelList);
////
////        String sms_receiver = StringUtils.collectionToDelimitedString(phoneList, ",");
////        String[] phones = sms_receiver.split(",");
////
////        Map<String, Object> map = new HashMap<>();
////        map.put("appId", "5eeef6d8c2543026f49ba8330b66516b");
////        map.put("appKey", "0e867927c0b2b8b9fcb6c2c3098a1f03");
////        map.put("templateId", templateId);
////        map.put("sign", false);
////        Map<String, String[]> content = new HashMap<>();
////        content.put("phones", phones);
////        content.put("keywords", keywords);
////        map.put("content", content);
////
////        HttpEntity entity = new HttpEntity(map);
////
////        ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
//
//    }
//
//
//    /*
//    发送邮件
//     */
////    public String sendEmail(String msg){
////        List<String> emailList = alarmUserRepository.findEmail();
////        String email_receiver = StringUtils.collectionToDelimitedString(emailList, ",");
////
////        MultiValueMap<String,String> paramMap = new LinkedMultiValueMap<>();
////        paramMap.add("email",email_receiver);//正式收件人
//////        paramMap.add("email","800119@gree.com.cn,800308@gree.com.cn");//测试收件人
////        paramMap.add("subject","设备异常报警");
////        paramMap.add("body",msg);
////        return HttpClient.sendPostRequest(email,paramMap);
////    }
//
//    /*
//    发送socket
//     */
//    public String sendToSocket(Integer areaInfoId, String time, Integer deviceId, String deviceName, String currentValue, String thresholdValue, String attributeName, String thresholdValueName, String areaName, String homeName, String alarmLevel, String alarmLevelValue, Integer areaLevelId){
////        try {
//        Map<String,String> paramMap = new HashMap<>();
//
//        JSONObject jsonObject = new JSONObject();
//        jsonObject.put("time", time);
//        jsonObject.put("deviceId", deviceId);
//        jsonObject.put("deviceName", deviceName);
//        jsonObject.put("attributeName", attributeName);
//        jsonObject.put("thresholdValueName", thresholdValueName);
//        jsonObject.put("currentValue", currentValue);
//        jsonObject.put("thresholdValue", thresholdValue);
//        jsonObject.put("areaName",areaName);
//        jsonObject.put("homeName",homeName);
//        jsonObject.put("alarmLevel",alarmLevel);
//        jsonObject.put("alarmLevelValue",alarmLevelValue);
//        jsonObject.put("areaLevelId",areaLevelId);
//        paramMap.put("areaInfoId", areaInfoId.toString());
//        paramMap.put("deviceWarnData", jsonObject.toString());
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
//            logger.error("doSocket post请求错误");
//            return "doSocket post请求错误";
//        }
//    }
//
//    public String sendToSocketForStorage(Integer areaInfoId, String time, Integer deviceId, String deviceName, String currentValue, String thresholdValue, String attributeName, String thresholdValueName){
//        MultiValueMap<String,String> paramMap = new LinkedMultiValueMap<>();
//        JSONObject jsonObject = new JSONObject();
//        jsonObject.put("time", time);
//        jsonObject.put("deviceId", deviceId);
//        jsonObject.put("deviceName", deviceName);
//        jsonObject.put("attributeName", attributeName);
//        jsonObject.put("thresholdValueName", thresholdValueName);
//        jsonObject.put("currentValue", currentValue);
//        jsonObject.put("thresholdValue", thresholdValue);
//        paramMap.add("areaInfoId", areaInfoId.toString());
//        paramMap.add("deviceWarnData", jsonObject.toString());
//        return HttpClient.sendPostRequest(socket,paramMap);
//    }
//
//    public String sendToSocketRealTimeData(String message) {
//        MultiValueMap<String,String> paramMap = new LinkedMultiValueMap<>();
//        if (message.isEmpty()){
//            return "消息为空";
//        }
//         // 将字符串转换为JSONObject
//        JSONObject object = JSON.parseObject(message);
//        object.remove("operType");
//        String barCode = object.getString("barCode");
//        HomeInfoModel homeInfoModel = alarmConfigAttributeService.getHomeInfoModel(barCode);
//        String areaId = homeInfoModel.getBdAreaId();
//
//
//        paramMap.add("areaInfoId", areaId);
//        paramMap.add("storageMsg", object.toJSONString());
//        return HttpClient.sendPostRequest(socketRealTimeData,paramMap);
//    }
//
//    // 用于接口的发送短信服务
//    public String sendSMSForInterface(JSONObject data) {
//
//        JSONObject jsonData = data.getJSONObject("data");
//        String receivers = jsonData.getString("receivers");
//        String msg = jsonData.getString("msg");
//
//        MultiValueMap<String, String> paramMap = new LinkedMultiValueMap<>();
//        paramMap.add("userId", userId);
//        paramMap.add("password", password);
//
//        paramMap.add("pszMobis", receivers);//正式收件人
//        paramMap.add("pszMsg", msg);
//        paramMap.add("iMobiCount", "1");
//        paramMap.add("pszSubPort", "*");
//        return HttpClient.sendPostRequest(sms, paramMap);
//    }
//
//    //新的发送短信接口
//    public void sendSMSForStorage(Integer templateId,List<String> phoneList, String[] keywords ) {
//        String url = "http://10.2.44.199/alarmUser/sendSmS";
////        List<String> phoneList = new ArrayList<>();
////        List<AlarmUser> alarmUsers = alarmUserBSRepository.findAlarmUserByAreaLevelIds(areaLevelIds);
//        if (ListUtil.isNull(phoneList)){
//            return;
//        }
////        for (AlarmUser user:alarmUsers){
////            String phoneNumber = user.getPhoneNumber();
////            phoneList.add(phoneNumber);
////        }
//        String sms_receiver = StringUtils.collectionToDelimitedString(phoneList, ",");
//        String[] phones = sms_receiver.split(",");
//        Map<String, Object> map = new HashMap<>();
//        map.put("appId", "5eeef6d8c2543026f49ba8330b66516b");
//        map.put("appKey", "0e867927c0b2b8b9fcb6c2c3098a1f03");
//        map.put("templateId", templateId);
//        map.put("sign", false);
//        Map<String, String[]> content = new HashMap<>();
//        content.put("phones", phones);
//        content.put("keywords", keywords);
//        map.put("content", content);
//        HttpEntity entity = new HttpEntity(map);
//        doSendSMS(entity,url);
//
//    }
//
//
//    private void doSendSMS(HttpEntity entity,String url){
//        try {
//            ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
//            //logger.info(responseEntity.getBody());
//        }catch (Exception e){
//            logger.error((e.getMessage()));
//            e.printStackTrace();
//        }
//    }
//
//
//}
