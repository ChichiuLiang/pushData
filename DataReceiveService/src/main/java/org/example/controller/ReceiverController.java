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

    @PostMapping(value = "/receiveData")
    public void receiveData(@RequestBody String data){
        //System.out.println("receiver1-work");
        receiverService.receiveData(data);
    }
}
