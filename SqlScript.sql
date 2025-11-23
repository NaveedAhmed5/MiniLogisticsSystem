CREATE DATABASE IF NOT EXISTS logistics_db;
USE logistics_db;

CREATE TABLE users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL, 
    phone VARCHAR(20),
    role VARCHAR(20) NOT NULL 
);

CREATE TABLE vehicles (
    vehicle_id INT PRIMARY KEY AUTO_INCREMENT,
    model VARCHAR(50),
    plate_no VARCHAR(20),
    capacity DOUBLE,
    insurance_info VARCHAR(100)
);

CREATE TABLE drivers (
    user_id INT PRIMARY KEY,
    license_no VARCHAR(50),
    status VARCHAR(20) DEFAULT 'Pending', 
    rating DOUBLE DEFAULT 5.0,
    earnings DOUBLE DEFAULT 0.0,
    vehicle_id INT,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id) ON DELETE SET NULL
);

CREATE TABLE deliveries (
    delivery_id INT PRIMARY KEY AUTO_INCREMENT,
    description VARCHAR(200),
    pickup VARCHAR(100),
    dropoff VARCHAR(100),
    status VARCHAR(20) DEFAULT 'PENDING', 
    assigned_driver_id INT DEFAULT 0, 
    fee DOUBLE DEFAULT 0.0,
    customer_contact VARCHAR(100)
);

CREATE TABLE assignments (
    assignment_id INT PRIMARY KEY AUTO_INCREMENT,
    delivery_id INT,
    priority VARCHAR(20),
    deadline DATETIME,
    assigned_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (delivery_id) REFERENCES deliveries(delivery_id) ON DELETE CASCADE
);

CREATE TABLE audit_logs (
    log_id INT PRIMARY KEY AUTO_INCREMENT,
    category VARCHAR(50),
    details TEXT,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO users (name, email, password, role) VALUES ('SuperAdmin', 'admin', 'admin123', 'ADMIN');

INSERT INTO users (name, email, password, phone, role) VALUES ('Bob Driver', 'bob', 'pass', '555-0101', 'DRIVER');
SET @uid = LAST_INSERT_ID(); 

INSERT INTO vehicles (model, plate_no, capacity, insurance_info) VALUES ('Toyota Van', 'ABC-123', 500.0, 'INS-999');
SET @vid = LAST_INSERT_ID();

INSERT INTO drivers (user_id, license_no, status, rating, vehicle_id, earnings) VALUES (@uid, 'LIC-999', 'Active', 4.8, @vid, 150.0);

INSERT INTO deliveries (description, pickup, dropoff, status, assigned_driver_id, fee, customer_contact) 
VALUES ('Medical Supplies', 'Hospital A', 'Clinic B', 'ASSIGNED', @uid, 50.0, 'Dr. Smith');

INSERT INTO deliveries (description, pickup, dropoff, status, assigned_driver_id, fee, customer_contact) 
VALUES ('Office Chairs', 'Warehouse', 'Office 5', 'PENDING', 0, 75.0, 'Manager John');