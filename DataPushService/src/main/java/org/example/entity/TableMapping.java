package org.example.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Table;

@Setter
@Getter
public class TableMapping {
    private String sourceTable;
    private String destinationTable;
    private String sourceFields;
    private String destinationFields;
    //private String deviceIdMapping; // 设备ID转换规则（不通过数据库配置，而是查询服务器B）

    // Getters and Setters

}
