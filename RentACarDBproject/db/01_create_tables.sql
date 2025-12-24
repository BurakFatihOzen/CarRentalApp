
-- ============================================================================
-- ARAÇ KİRALAMA VERİTABANI (CAR RENTAL DB) - KURULUM VE TEST SENARYOSU
-- ============================================================================
-- Açıklama: Bu script, araç kiralama sistemi için gerekli tabloları sıfırdan 
-- oluşturur, ilişkileri kurar, örnek verileri (seed data) ekler ve 
-- test sorgularını çalıştırır.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 0. TEMİZLİK İŞLEMLERİ (DROP TABLES)
-- ----------------------------------------------------------------------------
-- Mevcut tabloları temizliyoruz. 'CASCADE' komutu, bu tablolara bağlı olan 
-- diğer tabloların veya kısıtlamaların (constraints) da silinmesini sağlar.
-- Böylece "tablo zaten var" hatası almadan scripti tekrar tekrar çalıştırabiliriz.
DROP TABLE IF EXISTS rental CASCADE;
DROP TABLE IF EXISTS reservation CASCADE;
DROP TABLE IF EXISTS customer CASCADE;
DROP TABLE IF EXISTS vehicle CASCADE;
DROP TABLE IF EXISTS branch CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- ----------------------------------------------------------------------------
-- 1. KULLANICILAR TABLOSU (USERS)
-- ----------------------------------------------------------------------------
-- Sisteme giriş yapacak personelin ve yöneticilerin tutulduğu tablo.
-- Username ve email benzersiz (UNIQUE) olmalıdır.
CREATE TABLE users(
    user_id     SERIAL PRIMARY KEY,       -- Otomatik artan benzersiz ID
    username    VARCHAR(50)  NOT NULL UNIQUE, 
    email       VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(150) NOT NULL,    -- Gerçek hayatta burası hashlenmiş olmalı
    role        VARCHAR(20)  NOT NULL CHECK(role IN ('ADMIN','STAFF')) 
    -- Sadece 'ADMIN' veya 'STAFF' rolü girilebilir, veri bütünlüğü sağlanır.
);

-- ----------------------------------------------------------------------------
-- 2. ŞUBELER TABLOSU (BRANCH)
-- ----------------------------------------------------------------------------
-- Araçların bulunduğu ve kiralama işlemlerinin yapıldığı fiziksel lokasyonlar.
CREATE TABLE branch(
    branch_id   SERIAL PRIMARY KEY,
    branch_name VARCHAR(50) NOT NULL UNIQUE, -- Şube isimleri karışmaması için Unique
    branch_city VARCHAR(50) NOT NULL,
    phone       VARCHAR(20)
);

-- ----------------------------------------------------------------------------
-- 3. ARAÇLAR TABLOSU (VEHICLE)
-- ----------------------------------------------------------------------------
-- Filodaki tüm araçların kaydı. Her araç bir şubeye aittir.
CREATE TABLE vehicle(
    vehicle_id    SERIAL PRIMARY KEY,
    branch_id     INT NOT NULL REFERENCES branch(branch_id) ON DELETE CASCADE, 
    -- NOT: Şube silinirse, o şubeye bağlı araçlar da otomatik silinir (CASCADE).
    
    brand         VARCHAR(50) NOT NULL,
    model         VARCHAR(50) NOT NULL,
    plate         VARCHAR(20) NOT NULL UNIQUE, -- Plaka her araç için tekil olmalı
    daily_price   NUMERIC(10,2) NOT NULL CHECK(daily_price > 0), -- Fiyat 0 veya negatif olamaz
    vehicle_status VARCHAR(20) NOT NULL CHECK(vehicle_status IN('AVAILABLE','RESERVED','RENTED','MAINTENANCE'))
    -- Aracın durumu sadece belirtilen 4 seçenekten biri olabilir.
);

-- ----------------------------------------------------------------------------
-- 4. MÜŞTERİLER TABLOSU (CUSTOMER)
-- ----------------------------------------------------------------------------
-- Aracı kiralayan kişilerin bilgileri. Ehliyet numarası tekil anahtardır.
CREATE TABLE customer(
    customer_id SERIAL PRIMARY KEY,
    full_name   VARCHAR(50) NOT NULL,
    phone       VARCHAR(15) NOT NULL,
    license_no  VARCHAR(13) NOT NULL UNIQUE -- Aynı ehliyetle ikinci kayıt açılamaz
);

-- ----------------------------------------------------------------------------
-- 5. REZERVASYONLAR TABLOSU (RESERVATION)
-- ----------------------------------------------------------------------------
-- Müşterinin aracı kiralama talebi. Henüz araç teslim edilmemiştir, sadece yer ayrılmıştır.
CREATE TABLE reservation(
    reservation_id SERIAL PRIMARY KEY,
    customer_id    INT NOT NULL REFERENCES customer(customer_id) ON DELETE CASCADE,
    -- Müşteri silinirse geçmiş rezervasyonları da temizlenir.
    
    vehicle_id     INT NOT NULL REFERENCES vehicle(vehicle_id) ON DELETE CASCADE,
    -- Araç sistemden kalkarsa rezervasyonları da silinir.
    
    start_date     DATE NOT NULL,
    end_date       DATE NOT NULL,
    total_price    NUMERIC(10,2) NOT NULL CHECK (total_price >= 0),
    reservation_status VARCHAR(20) NOT NULL CHECK(reservation_status IN('PENDING','APPROVED','CANCELLED')),
    
    CHECK (end_date >= start_date) -- Bitiş tarihi, başlangıç tarihinden önce olamaz.
);
-- 1. Önce eski kısıtlamayı (constraint) kaldır
ALTER TABLE reservation DROP CONSTRAINT IF EXISTS reservation_reservation_status_check;

-- 2. 'COMPLETED' değerini de içeren yeni kısıtlamayı ekler
ALTER TABLE reservation ADD CONSTRAINT reservation_reservation_status_check 
CHECK (reservation_status IN ('PENDING', 'APPROVED', 'CANCELLED', 'COMPLETED'));

-- ----------------------------------------------------------------------------
-- 6. KİRALAMALAR TABLOSU (RENTAL)
-- ----------------------------------------------------------------------------
-- Aracın fiziksel olarak müşteriye teslim edildiği anı temsil eder.
-- Rezervasyon onaylandığında ve müşteri geldiğinde buraya kayıt atılır.
CREATE TABLE rental(
    rental_id         SERIAL PRIMARY KEY,
    reservation_id    INT NOT NULL UNIQUE REFERENCES reservation(reservation_id) ON DELETE CASCADE,
    -- NOT: Her rezervasyon sadece 1 kiralama işlemine dönüşebilir (UNIQUE).
    -- Rezervasyon silinirse kiralama kaydı da silinir.
    
    pickup_branch_id  INT NOT NULL REFERENCES branch(branch_id), -- Aracı aldığı şube
    dropoff_branch_id INT NOT NULL REFERENCES branch(branch_id), -- Aracı bırakacağı şube (Farklı olabilir)
    
    rental_date       DATE NOT NULL,
    return_date       DATE, -- Henüz iade etmediyse NULL olabilir
    payment_status    VARCHAR(20) NOT NULL CHECK(payment_status IN('UNPAID','PAID')),
    
    CHECK (return_date IS NULL OR return_date >= rental_date) -- İade tarihi kiralama tarihinden önce olamaz
);


-- ============================================================================
-- VERİ GİRİŞİ (DATA SEEDING)
-- ============================================================================

-- 1. ŞUBELERİN EKLENMESİ
-- ------------------------------------------------------------
INSERT INTO branch(branch_name, branch_city, phone) VALUES
('Kizilay Subesi', 'Ankara', '0312 000 00 01'),
('Besevler Subesi', 'Ankara', '0312 000 00 02'),
('Sariyer Subesi', 'Istanbul', '0312 058 00 24');

-- 2. ARAÇLARIN EKLENMESİ
-- ------------------------------------------------------------
-- NOT: branch_id'yi manuel yazmak yerine (1, 2 gibi), alt sorgu (subquery) ile
-- isme göre çekiyoruz. Bu sayede ID'ler değişse bile kod hata vermez.
INSERT INTO vehicle(branch_id, brand, model, plate, daily_price, vehicle_status) VALUES

-- KIZILAY ŞUBESİ (Lüks ve Orta Segment)
((SELECT branch_id FROM branch WHERE branch_name = 'Kizilay Subesi'), 'Alfa Romeo', 'Giulia', '06 BRK 058', 6785.00, 'AVAILABLE'),
((SELECT branch_id FROM branch WHERE branch_name = 'Kizilay Subesi'), 'BMW', '320i', '06 GKM 73', 5500.00, 'AVAILABLE'),
((SELECT branch_id FROM branch WHERE branch_name = 'Kizilay Subesi'), 'Fiat', 'Linea', '06 MRT 233', 1000.00, 'MAINTENANCE'), -- Bakımda
((SELECT branch_id FROM branch WHERE branch_name = 'Kizilay Subesi'), 'Fiat', 'Egea', '06 ABC 123', 1200.00, 'AVAILABLE'),
((SELECT branch_id FROM branch WHERE branch_name = 'Kizilay Subesi'), 'Volvo', 'XC90', '06 VOL 90', 7500.00, 'AVAILABLE'),
((SELECT branch_id FROM branch WHERE branch_name = 'Kizilay Subesi'), 'Audi', 'A6', '06 AUD 06', 6200.00, 'AVAILABLE'),
((SELECT branch_id FROM branch WHERE branch_name = 'Kizilay Subesi'), 'Volkswagen', 'Passat', '06 PAS 10', 2800.00, 'AVAILABLE'),

-- BEŞEVLER ŞUBESİ (Ekonomik ve SUV)
((SELECT branch_id FROM branch WHERE branch_name = 'Besevler Subesi'), 'Dacia', 'Lodgy', '06 ABK 071', 1200.00, 'MAINTENANCE'),
((SELECT branch_id FROM branch WHERE branch_name = 'Besevler Subesi'), 'Nissan', 'Qashqai', '06 ATK 33', 3000.00, 'AVAILABLE'),
((SELECT branch_id FROM branch WHERE branch_name = 'Besevler Subesi'), 'Fiat', 'Egea', '06 EGE 55', 1150.00, 'AVAILABLE'),
((SELECT branch_id FROM branch WHERE branch_name = 'Besevler Subesi'), 'Renault', 'Clio', '06 CLO 55', 1100.00, 'AVAILABLE'),
((SELECT branch_id FROM branch WHERE branch_name = 'Besevler Subesi'), 'Toyota', 'Corolla', '06 TYT 01', 1500.00, 'AVAILABLE'),
((SELECT branch_id FROM branch WHERE branch_name = 'Besevler Subesi'), 'Opel', 'Astra', '06 OPL 44', 1400.00, 'MAINTENANCE'),
((SELECT branch_id FROM branch WHERE branch_name = 'Besevler Subesi'), 'Peugeot', '3008', '06 PGT 20', 2200.00, 'AVAILABLE'),

-- SARIYER ŞUBESİ (İstanbul - Karışık Segment)
((SELECT branch_id FROM branch WHERE branch_name = 'Sariyer Subesi'), 'Fiat', 'Freemont', '34 JRN 23', 4700.00, 'RESERVED'), -- Rezerve
((SELECT branch_id FROM branch WHERE branch_name = 'Sariyer Subesi'), 'Mercedes', 'E180', '34 ACN 876', 5100.00, 'RESERVED'),
((SELECT branch_id FROM branch WHERE branch_name = 'Sariyer Subesi'), 'Fiat', 'Egea', '34 ABC 124', 1200.00, 'RESERVED'),
((SELECT branch_id FROM branch WHERE branch_name = 'Sariyer Subesi'), 'Ford', 'Focus', '34 FRD 34', 1600.00, 'AVAILABLE'),
((SELECT branch_id FROM branch WHERE branch_name = 'Sariyer Subesi'), 'Honda', 'Civic', '34 HND 99', 1700.00, 'AVAILABLE'),
((SELECT branch_id FROM branch WHERE branch_name = 'Sariyer Subesi'), 'Hyundai', 'Tucson', '34 HYN 55', 2300.00, 'RESERVED');


-- 3. MÜŞTERİLERİN EKLENMESİ
-- ------------------------------------------------------------
INSERT INTO customer(full_name, phone, license_no) VALUES
('Burak Özen', '0544 795 04 11', 'LIC-TR-0001'),
('Mehmet Gökmenoglu', '0532 869 12 23', 'LIC-TR-0002'),
('Bayram Ali Atik', '0505 119 17 13', 'LIC-TR-0003'),
('Muhammed Enes Atay', '0532 002 06 06', 'LIC-TR-0004'),
('Necaattin Barışçı',  '0544 222 22 22', 'LIC-TR-0005'),
('Musa Usa', '0544 120 15 11', 'LIC-TR-0006');


-- 4. KULLANICILARIN (SİSTEM YÖNETİCİLERİ) EKLENMESİ
-- ------------------------------------------------------------
INSERT INTO users(username, email, password, role) VALUES
('admin1', 'admin@demo.com', 'memet123', 'ADMIN'),
('staff1', 'staff@demo.com', 'burak123', 'STAFF');


-- 5. REZERVASYONLARIN EKLENMESİ
-- ------------------------------------------------------------
-- Burada müşteri ID ve araç ID'lerini yine subquery ile buluyoruz.
-- Bu yöntem veri tutarlılığı açısından en güvenli yoldur.
INSERT INTO reservation(customer_id, vehicle_id, start_date, end_date, total_price, reservation_status) VALUES

-- Senaryo 1: Burak Özen -> Alfa Romeo (2 Günlük Bekleyen Rezervasyon)
(
  (SELECT customer_id FROM customer WHERE full_name='Burak Özen'),
  (SELECT vehicle_id FROM vehicle WHERE plate='06 BRK 058'),
  '2025-12-20', '2025-12-22', 13570.00, 'PENDING'
),

-- Senaryo 2: Mehmet Gökmenoglu -> BMW (Onaylanmış Rezervasyon)
(
  (SELECT customer_id FROM customer WHERE full_name='Mehmet Gökmenoglu'),
  (SELECT vehicle_id FROM vehicle WHERE plate='06 GKM 73'),
  '2025-12-21', '2025-12-24', 16500.00, 'APPROVED'
),

-- Senaryo 3: Bayram Ali Atik -> Nissan Qashqai (Beklemede)
(
  (SELECT customer_id FROM customer WHERE full_name='Bayram Ali Atik'),
  (SELECT vehicle_id FROM vehicle WHERE plate='06 ATK 33'),
  '2025-12-23', '2025-12-25', 6000.00, 'PENDING'
),

-- Senaryo 4: Muhammed Enes Atay -> Mercedes (Lüks Araç Onayı)
(
  (SELECT customer_id FROM customer WHERE full_name='Muhammed Enes Atay'),
  (SELECT vehicle_id FROM vehicle WHERE plate='34 ACN 876'),
  '2025-12-24', '2025-12-26', 10200.00, 'APPROVED'
),

-- Senaryo 5: Necaattin Barışçı -> Volvo XC90 (Yüksek Tutar)
(
  (SELECT customer_id FROM customer WHERE full_name='Necaattin Barışçı'),
  (SELECT vehicle_id FROM vehicle WHERE plate='06 VOL 90'),
  '2025-12-25', '2025-12-28', 22500.00, 'PENDING'
),

-- Senaryo 6: Musa Usa -> Fiat Egea (İptal Edilmiş Rezervasyon)
(
  (SELECT customer_id FROM customer WHERE full_name='Musa Usa'),
  (SELECT vehicle_id FROM vehicle WHERE plate='06 EGE 55'),
  '2025-12-26', '2025-12-30', 4600.00, 'CANCELLED'
);


-- ============================================================================
-- SORGULAR VE KONTROLLER (TEST QUERIES)
-- ============================================================================

-- 1. Tüm araçları listeleyelim
SELECT * FROM vehicle;

-- 2. Belirli şubelerin iletişim bilgilerini getirelim
SELECT branch_id, branch_name, branch_city, phone
FROM branch
WHERE branch_name IN ('Kizilay Subesi','Besevler Subesi');

-- 3. Mükerrer Şube Temizliği (Advanced SQL)
-- NOT: Bu sorgu, aynı isimde birden fazla şube varsa, ID'si büyük olanı siler,
-- en küçük ID'li (ilk eklenen) kaydı tutar. Veri temizliği için kritiktir.
DELETE FROM branch
WHERE branch_id NOT IN (
    SELECT MIN(branch_id)
    FROM branch
    GROUP BY branch_name
);

-- 4. Kalan şubeleri kontrol edelim
SELECT * FROM branch;

-- 5. Müşteri listesi (ID sırasına göre)
SELECT customer_id, full_name
FROM customer
ORDER BY customer_id;

-- 6. Rezervasyon Durum Kontrolü
-- (Not: Orijinal kodda 'status' yazıyordu, doğrusu 'reservation_status'tür)
SELECT reservation_id, customer_id, vehicle_id, reservation_status 
FROM reservation;

-- 7. Veritabanındaki tüm tabloları listeleme (System Catalog)
SELECT table_name
FROM information_schema.tables
WHERE table_schema='public'
ORDER BY table_name;

-- 8. Genel Kontrol (Her tablodan veri çekme)
SELECT * FROM branch;
SELECT * FROM vehicle;
SELECT * FROM customer;
SELECT * FROM users;
SELECT * FROM reservation;
SELECT * FROM rental;

-- 9. Sadece ONAYLANMIŞ (APPROVED) rezervasyonları getir
SELECT reservation_id, reservation_status
FROM reservation
WHERE reservation_status = 'APPROVED';

-- 10. Bekleyen (PENDING) rezervasyonları silme işlemi
-- NOT: Bu işlem PENDING durumundaki tüm kayıtları kalıcı olarak siler!
DELETE FROM reservation
WHERE reservation_status = 'PENDING';

-- 11. Spesifik Araç Kontrolü (ID = 19)
-- (Not: Orijinal kodda 'status' yazıyordu, doğrusu 'vehicle_status'tür)
SELECT vehicle_id, plate, vehicle_status 
FROM vehicle 
WHERE vehicle_id = 19;