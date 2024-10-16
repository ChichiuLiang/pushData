package org.example.controller;

import org.example.service.ReceiverService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author 800416
 * @Date 2024/10/16
 */
@RestController
public class ReceiverController {

    @Resource
    private ReceiverService receiverService;

    @PostMapping(value = "/receiveT1803Data")
    public void receiveT1803Data(@RequestBody String data){
        receiverService.receiveT1803Data(data);
    }




    @PostMapping(value = "/receiveT2502Data")
    public void receiveT2502Data(@RequestBody String data){
        receiverService.receiveT2502Data(data);
    }
}
