package org.example.schedule;

import org.example.cache.BarCodeCache;
import org.example.repository.BarCodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class BarCodeRefreshService {

    @Autowired
    private BarCodeRepository barCodeRepository;

    @Autowired
    private BarCodeCache barCodeCache;

    // 每 5 分钟执行一次
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void refreshBarCodes() {
        Set<String> newBarCodes = barCodeRepository.getBarCodes();
        barCodeCache.updateBarCodes(newBarCodes);
    }
}
