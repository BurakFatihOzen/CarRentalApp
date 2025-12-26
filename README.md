# Seal Car Rental Management System

A professional desktop application built with **JavaFX** and **PostgreSQL** designed to manage end-to-end car rental processes, including reservations, rentals, maintenance, and returns.

## Key Features

* **Advanced DB Automation:** Integrated PostgreSQL triggers to handle vehicle status updates (`AVAILABLE`, `RESERVED`, `RENTED`, `MAINTENANCE`) automatically.
* **Business Logic Integrity:** Stored procedures prevent double-booking or renting vehicles currently under maintenance.
* **Modern UI/UX:** High-fidelity, full-screen supported login and management dashboards with professional visuals.
* **Secure Session Management:** Role-based access control for administrative tasks.

## Setup & Installation

Follow these steps to run the project on your local machine:

### 1. Prerequisites
* **JDK 17** or higher.
* **PostgreSQL** Database.
* **IntelliJ IDEA** (Recommended IDE).

### 2. Database Setup
1. Create a new database in PostgreSQL named `car_rental_db`.
2. Execute the SQL scripts located in the `sql/` directory in the following order:
   - `01_create_tables.sql` (Initial Schema)
   - `02_insert_data.sql` (Seed Data)
   - `03_views.sql` (Virtual Tables)
   - `04_triggers.sql` (Automation Rules)
   - `05_procedures.sql` (Rental Business Logic)

### 3. Java Database Configuration
1. Open the `src/service/Db.java` (or your DB connection class) file.
2. Update the credentials based on your local PostgreSQL settings:
   ```java
   private static final String URL = "jdbc:postgresql://localhost:5432/car_rental_db";
   private static final String USER = "your_username";
   private static final String PASS = "your_password";
