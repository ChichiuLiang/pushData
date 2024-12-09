package org.example.model;

import java.util.HashSet;
import java.util.Set;

public class HomeInfoModel {

    //area_name,bigdata_area_id,bigdata_area_level_id,hi.home_name,hi.id as homeId,additional_bigdata_area_id,additional_bigdata_area_level_id,bar_code
    private String areaName;
    private String bdAreaId;
    private String bdAreaLevelId;
    private String homeName;
    private String homeId;
    private String additionalBDAreaId;
    private String additionalBDAreaLevelId;
    private String barCode;
    private Set<Integer>  bdAreaIdsSet = new HashSet<>();
    private Set<Integer>  bdAreaLevelIdsSet = new HashSet<>();

    public String getBarCode() {
        return barCode;
    }

    public void setBarCode(String barCode) {
        this.barCode = barCode;
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public String getBdAreaId() {
        return bdAreaId;
    }

    public void setBdAreaId(String bdAreaId) {
        this.bdAreaId = bdAreaId;
    }

    public String getBdAreaLevelId() {
        return bdAreaLevelId;
    }

    public void setBdAreaLevelId(String bdAreaLevelId) {
        this.bdAreaLevelId = bdAreaLevelId;
    }

    public String getHomeName() {
        return homeName;
    }

    public void setHomeName(String homeName) {
        this.homeName = homeName;
    }

    public String getHomeId() {
        return homeId;
    }

    public void setHomeId(String homeId) {
        this.homeId = homeId;
    }

    public String getAdditionalBDAreaId() {
        return additionalBDAreaId;
    }

    public void setAdditionalBDAreaId(String additionalBDAreaId) {
        this.additionalBDAreaId = additionalBDAreaId;
    }

    public String getAdditionalBDAreaLevelId() {
        return additionalBDAreaLevelId;
    }

    public void setAdditionalBDAreaLevelId(String additionalBDAreaLevelId) {
        this.additionalBDAreaLevelId = additionalBDAreaLevelId;
    }

    public Set<Integer> getBdAreaIdsSet() {
        return bdAreaIdsSet;
    }

    public void setBdAreaIdsSet(Set<Integer> bdAreaIdsSet) {
        this.bdAreaIdsSet = bdAreaIdsSet;
    }

    public Set<Integer> getBdAreaLevelIdsSet() {
        return bdAreaLevelIdsSet;
    }

    public void setBdAreaLevelIdsSet(Set<Integer> bdAreaLevelIdsSet) {
        this.bdAreaLevelIdsSet = bdAreaLevelIdsSet;
    }
}
