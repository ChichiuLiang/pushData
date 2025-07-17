package org.example.cache;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Component
public class BarCodeCache {

    private volatile Set<String> currentBarCodes =  Collections.emptySet();

    public void updateBarCodes(Set<String> newBarCodes) {
        if (newBarCodes != null && !newBarCodes.isEmpty()) {
            this.currentBarCodes = Collections.unmodifiableSet(new HashSet<>(newBarCodes));
        }
    }

    public Set<String> getBarCodes() {
        return currentBarCodes;
    }
}
