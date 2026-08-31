package com.example.hrmspolicies2.enums;

public enum Role {

    HR_ADMIN("/hr-admin/dashboard"),
    LEGAL_REVIEWER("/legal-reviewer/dashboard"),
    HR_HEAD("/hr-head/dashboard"),
    MANAGING_DIRECTOR("/managing-director/dashboard"),
    MANAGER("/manager/dashboard"),
    EMPLOYEE("/employee/dashboard");

    private final String dashboardPath;

    Role(String dashboardPath) {
        this.dashboardPath = dashboardPath;
    }

    public String getDashboardPath() {
        return dashboardPath;
    }
}