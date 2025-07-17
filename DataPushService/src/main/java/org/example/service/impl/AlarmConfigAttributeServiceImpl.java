package org.example.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import lombok.extern.slf4j.Slf4j;
import org.example.model.AlarmConfigModel;
import org.example.model.HomeInfoModel;
import org.example.repository.DeviceMonitorRecordV2Repository;
import org.example.utils.ListUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class AlarmConfigAttributeServiceImpl implements CommandLineRunner {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private DeviceMonitorRecordV2Repository deviceMonitorRecordV2Repository;
    private Map<String, AlarmConfigModel> typeCodeDataTypeNoToModels = new ConcurrentHashMap<>();
    private Map<String,List<AlarmConfigModel>> typeCodeToModels =  new ConcurrentHashMap<>();

    private Map<Integer,Set<Integer>> bdAreaIdToBDAreaLevelId =  new ConcurrentHashMap<>();

    private Map<String, HomeInfoModel> barCodeToModels =  new ConcurrentHashMap<>();


    private Map<String,String> deviceToIndex =  new ConcurrentHashMap<>();

    public Map<String, AlarmConfigModel> getConfigs() {
        return typeCodeDataTypeNoToModels;
    }

    public void setConfigs(Map<String, AlarmConfigModel> typeCodeDataTypeNoToModels) {
        this.typeCodeDataTypeNoToModels = typeCodeDataTypeNoToModels;
    }

    public void putConfigs(Map<String, AlarmConfigModel> typeCodeDataTypeNoToModels) {
        this.typeCodeDataTypeNoToModels.putAll(typeCodeDataTypeNoToModels);
    }

    public Map<String, String> getDeviceToIndex() {
        return deviceToIndex;
    }

    public void setDeviceToIndex(Map<String, String> deviceToIndex) {
        this.deviceToIndex = deviceToIndex;
    }

    public void putDeviceToIndex(String barCodeDeviceTypeAddress,String index) {
        this.deviceToIndex.put(barCodeDeviceTypeAddress,index);
    }

    public Map<String, List<AlarmConfigModel>> getTypeCodeToModels() {
        return typeCodeToModels;
    }

    public void setTypeCodeToModels(Map<String, List<AlarmConfigModel>> typeCodeToModels) {
        this.typeCodeToModels = typeCodeToModels;
    }

    public Map<String, AlarmConfigModel> getTypeCodeDataTypeNoToModels() {
        return typeCodeDataTypeNoToModels;
    }

    public void setTypeCodeDataTypeNoToModels(Map<String, AlarmConfigModel> typeCodeDataTypeNoToModels) {
        this.typeCodeDataTypeNoToModels = typeCodeDataTypeNoToModels;
    }

    public Map<String, HomeInfoModel> getBarCodeToModels() {
        return barCodeToModels;
    }

    public void setBarCodeToModels(Map<String, HomeInfoModel> barCodeToModels) {
        this.barCodeToModels = barCodeToModels;
    }

    @Override
    public void run(String... args) throws Exception {
        typeCodeDataTypeNoToModels.clear();
        typeCodeToModels.clear();
        bdAreaIdToBDAreaLevelId.clear();
        barCodeToModels.clear();
        deviceToIndex.clear();
        log.info("应用启动后自动调用 getRemoteDeviceIdMap 方法...");
        init();  // 在启动时自动调用
    }

    public void init(){
        //程序启动后的初始化方法
        //queryAlarmConfigAttribute();
//        queryBarcodeToModel();
        queryMoudelIndex();
    }

    public List<AlarmConfigModel> queryAlarmConfigAttribute() {
        List<AlarmConfigModel> results = new ArrayList<>();
        String sql = "SELECT concat(da.id) as attribute_id,dt.type_code, attribute_no, attribute_name_cn, attribute_name, concat(data_type) as data_type, da.alarm_value, da.max_alarm, da.min_alarm, concat(da.alarm_level_id) as alarm_level_id, al.alarm_level_name, concat(dt.id) as device_type_id, moudle_name,da.specify_alarm " +
                "FROM iems_app.device_atrribute da " +
                "JOIN device_type dt ON dt.id = da.device_type_id " +
                "JOIN alarm_level al ON al.id = da.alarm_level_id " +
                "WHERE da.is_on = 1 AND alarm_level_id > 0";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            for (Map<String, Object> row : rows) {
            AlarmConfigModel model = new AlarmConfigModel();
            model.setAttributeId((String) row.get("attribute_id"));
            model.setTypeCode((String) row.get("type_code"));
            model.setAttributeNo((String) row.get("attribute_no"));
            model.setAttributeNameCn((String) row.get("attribute_name_cn"));
            model.setAttributeName((String) row.get("attribute_name"));
            model.setDataType((String) row.get("data_type"));
            model.setAlarmValue((String) row.get("alarm_value"));
            model.setMaxAlarm((String) row.get("max_alarm"));
            model.setMinAlarm((String) row.get("min_alarm"));
            model.setAlarmLevelId((String) row.get("alarm_level_id"));
            model.setAlarmLevelName((String) row.get("alarm_level_name"));
            model.setDeviceTypeId((String) row.get("device_type_id"));
            model.setMoudleName((String) row.get("moudle_name"));
            model.setSpecifyAlarm((String) row.get("specify_alarm"));
            // 将模型对象添加到结果列表和配置映射中
            results.add(model);
            String key = generateKey(model.getTypeCode(), model.getDataType(), model.getAttributeNo());
            typeCodeDataTypeNoToModels.put(key, model);
            String typeCodeDataTypeKey = model.getTypeCode()+"-"+model.getDataType();
            if (!typeCodeToModels.containsKey(typeCodeDataTypeKey)) {
                typeCodeToModels.put(typeCodeDataTypeKey, new ArrayList<>());
            }
            typeCodeToModels.get(typeCodeDataTypeKey).add(model);
        }
        return results;
    }

    public String generateKey(String typeCode,String dataType,String attributeNo) {
        return typeCode + "_" + dataType + "_" + attributeNo;
    }


//    public Map<String, HomeInfoModel> queryBarcodeToModel(){
//        String sql = "SELECT area_name,bigdata_area_id,bigdata_area_level_id,hi.home_name,hi.id as homeId,additional_bigdata_area_id,additional_bigdata_area_level_id,bar_code FROM iems_app.bigdata_base_relation bbr\n" +
//                "join home_info hi on hi.area_info_id = bbr.bigdata_area_id";
//        List<Object> query = entityManager.createNativeQuery(sql).getResultList();
//        Map<String, HomeInfoModel> result = new HashMap<>();
//        if (ListUtil.isNull(query)){
//            return new HashMap<>();
//        }
//        for (Object o:query){
//            JSONArray jsonArray = JSONArray.parseArray(JSON.toJSONString(o));
//            HomeInfoModel model = new HomeInfoModel();
//            model.setAreaName(jsonArray.getString(0));
//            model.setBdAreaId(jsonArray.getString(1));
//            model.setBdAreaLevelId(jsonArray.getString(2));
//            model.setHomeName(jsonArray.getString(3));
//            model.setHomeId(jsonArray.getString(4));
//            model.setAdditionalBDAreaId(jsonArray.getString(5));
//            model.setAdditionalBDAreaLevelId(jsonArray.getString(6));
//            model.setBarCode(jsonArray.getString(7));
//            if (model.getAdditionalBDAreaId()!=null){
//                String[] arr = model.getAdditionalBDAreaId().split(",");
//                Arrays.stream(arr)
//                        .map(Integer::parseInt)  // 将字符串转换为整数
//                        .filter(obj -> true)  // 过滤掉空值（虽然这里不应该有）
//                        .forEach(model.getBdAreaIdsSet()::add);  // 将每个整数添加到 Set
//            }
//            if (model.getAdditionalBDAreaLevelId()!=null){
//                String[] arr = model.getAdditionalBDAreaLevelId().split(",");
//                Arrays.stream(arr)
//                        .map(Integer::parseInt)  // 将字符串转换为整数
//                        .filter(obj -> true)  // 过滤掉空值（虽然这里不应该有）
//                        .forEach(model.getBdAreaLevelIdsSet()::add);  // 将每个整数添加到 Set
//            }
//            result.put(model.getBarCode(),model);
//            barCodeToModels.put(model.getBarCode(),model);
//            Integer bdAreaId = Integer.parseInt(model.getBdAreaId());
//            if (!bdAreaIdToBDAreaLevelId.containsKey(bdAreaId)){
//                bdAreaIdToBDAreaLevelId.put(bdAreaId,new HashSet<>());
//            }
//            bdAreaIdToBDAreaLevelId.get(bdAreaId).add(Integer.parseInt(model.getBdAreaLevelId()));
//        }
//        return result;
//    }

    public HomeInfoModel getHomeInfoModel(String barcode){
        return barCodeToModels.getOrDefault(barcode,new HomeInfoModel());
    }

    public Set<Integer> getBDAreaLevelId(Integer bdAreaId){
        return bdAreaIdToBDAreaLevelId.getOrDefault(bdAreaId,new HashSet<>());
    }
    public List<String> getPhoneList(List<Integer> bigDataAreaLevelIds){
        return deviceMonitorRecordV2Repository.queryPhoneNumber(bigDataAreaLevelIds);
    }


    public Map<String,Integer> queryMoudelIndex(){
        Map<String,Integer> result = new HashMap<>();
        String sql ="SELECT dt.type_code,bar_code,dbi.address FROM iems_app.device_base_info dbi\n" +
                " join device_type dt on dt.id = dbi.device_type_id \n" +
                " join home_info hi on hi.id = dbi.home_id\n" +
                " where dbi.id > 0 and dbi.home_id>0\n" +
                "order by home_id,device_type_id,address";
        List<Object> query = entityManager.createNativeQuery(sql).getResultList();
        String key = "";
        int index = 1;
        for (Object o:query){
            JSONArray jsonArray = JSONArray.parseArray(JSON.toJSONString(o));
            String typeCode = jsonArray.getString(0);
            if (!(typeCode.startsWith("1707")||typeCode.startsWith("1704"))){
                //非1707或1704的没有index
                continue;
            }
            String barCode = jsonArray.getString(1);
            String address = jsonArray.getString(2);
            String generateKey = barCode  + typeCode  + address;
            if (!key.equals(generateKey)){
                key = generateKey;
                index = 1;
            }
            deviceToIndex.put(key,String.valueOf(index));
            index ++ ;
        }
        return result;
    }

    public String getIndexFromMap(String key){
        String index = deviceToIndex.getOrDefault(key,null);
        if (index==null){
            return "";
        }
        else {
            return " " + index + "#子模块";
        }
    }
}
