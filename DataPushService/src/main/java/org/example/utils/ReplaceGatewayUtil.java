package org.example.utils;

import org.springframework.stereotype.Component;

@Component
public class ReplaceGatewayUtil {





    public static String replaceGatewayIds(String input) {
        //社区替换
        return input
                .replace("01 00 1A 00 00 00 00 00 00 00 00 00 00 00 00", "F1 00 1A 00 00 00 00 00 00 00 00 00 00 00 00")
                .replace("02 00 02 04 00 04 03 00 00 08 04 04 00 00 01", "F2 00 02 04 00 04 03 00 00 08 04 04 00 00 01")
                .replace("02 00 02 04 00 06 00 05 01 03 04 06 00 00 01", "F2 00 02 04 00 06 00 05 01 03 04 06 00 00 01")
                .replace("02 00 12 00 00 00 00 00 00 00 00 00 00 00 00", "F2 00 12 00 00 00 00 00 00 00 00 00 00 00 00");
    }

    public static String replaceGatewayIdsBySJD(String input) {
        // 重构重复代码，减少冗余
        return input
                .replace("02 00 12 00 00 00 00 00 00 00 00 00 00 00 00", "E2 00 12 00 00 00 00 00 00 00 00 00 00 00 00");

    }
}
