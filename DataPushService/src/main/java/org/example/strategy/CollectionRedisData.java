package org.example.strategy;


/**
 * @author lee
 * @date 2023/11/10
 */
public interface CollectionRedisData {
    String getPushUrl();

    boolean verify(String deviceType);
}
