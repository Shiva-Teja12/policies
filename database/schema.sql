-- ================================================================
-- HRMS Policies - MySQL schema
--
-- In the "dev" Spring profile, Hibernate creates/updates these
-- tables automatically (spring.jpa.hibernate.ddl-auto=update).
-- In the "prod" profile, ddl-auto=validate, so this script (or an
-- equivalent migration) is the source of truth for the real schema -
-- Spring only checks it matches, it never alters production tables.
-- ================================================================

CREATE DATABASE IF NOT EXISTS hrmspolicies2;
USE hrmspolicies2;

-- ----------------------------------------------------------------
-- users
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL
);

-- ----------------------------------------------------------------
-- policies
-- One user (creator) can create many policies -> policies.created_by
-- is a foreign key to users.id (Many-to-One from Policy to User).
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS policies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    code VARCHAR(30) NOT NULL UNIQUE,
    category VARCHAR(100) NOT NULL,
    content TEXT,
    applicability VARCHAR(100) NOT NULL,
    mandatory BOOLEAN NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_by BIGINT,
    created_at DATETIME,
    updated_at DATETIME,
    CONSTRAINT fk_policies_created_by
        FOREIGN KEY (created_by) REFERENCES users(id)
        ON DELETE SET NULL,
    INDEX idx_policies_category (category),
    INDEX idx_policies_status (status)
);
