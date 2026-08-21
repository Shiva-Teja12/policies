package com.practice.springbootdemo.advance_performance_module.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserCodeGeneratorService {
    private final JdbcTemplate jdbcTemplate;
    public UserCodeGeneratorService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    /**
     * Generates a unique sequential employee code, e.g. EMP001, EMP002, etc.
     */
    public String generateEmployeeCode() {
        Long number = jdbcTemplate.queryForObject(
                "SELECT nextval('employee_code_seq')",
                Long.class
        );
        return String.format("EMP%03d", number != null ? number : 1L);
    }
    /**
     * Generates a unique sequential manager code, e.g. MGR001, MGR002, etc.
     */
    public String generateManagerCode() {
        Long number = jdbcTemplate.queryForObject(
                "SELECT nextval('manager_code_seq')",
                Long.class
        );
        return String.format("MGR%03d", number != null ? number : 1L);
    }
    /**
     * Generates a unique sequential HR code, e.g. HR001, HR002, etc.
     */
    public String generateHRCode() {
        Long number = jdbcTemplate.queryForObject(
                "SELECT nextval('hr_code_seq')",
                Long.class
        );
        return String.format("HR%03d", number != null ? number : 1L);
    }
}
