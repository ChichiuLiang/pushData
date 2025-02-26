package org.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@Service
public class QuickQueryService {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    public Map<String, String>  queryForMapString(String sql) {
        final Map<String, String> resultMap = new HashMap<>();

        jdbcTemplate.query(sql, new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                if (columnCount != 2) {
                    throw new IllegalArgumentException("SQL query must return exactly two columns");
                }

                String key = rs.getString(1);
                String value = rs.getObject(2).toString();
                resultMap.put(key, value);
            }
        });

        return resultMap;
    }
}
