package org.example.web;

import org.example.service.ConfigQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ConfigQueryControl {

    private final ConfigQueryService configQueryService;

    public ConfigQueryControl(ConfigQueryService configQueryService) {
        this.configQueryService = configQueryService;
    }

    @GetMapping("/getLocalDeviceIdMap")
    public Map<String, String> getLocalDeviceIdToBarCodeTypeAddressMap() {
        return configQueryService.getLocalDeviceIdToBarCodeTypeAddressMap();
    }

    @GetMapping("/getRemoteDeviceIdMap")
    public Map<String, String> getRemoteDeviceIdMap() {
        return configQueryService.getRemoteDeviceIdMap();
    }

    @PostMapping("/device")
    public Integer insertRemoteDeviceInfo(String barCode, String deviceName, String address, String typeCode, int protocolNum) {
        return configQueryService.insertRemoteDeviceInfo(barCode, deviceName, address, typeCode, protocolNum);
    }

}
