package org.example.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class BarCodeRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Set<String> getBarCodes() {
        String sql = "SELECT  bar_code FROM iems_app.energy_station es " +
                "JOIN home_info hi ON hi.area_info_id = es.area_id ";
        return jdbcTemplate.queryForList(sql, String.class).stream().collect(Collectors.toSet());
    }
}
