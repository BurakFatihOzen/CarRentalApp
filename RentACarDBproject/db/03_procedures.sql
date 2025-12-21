-- ============================================================================
-- STORED PROCEDURES (İŞ MANTIĞI FONKSİYONLARI)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- PROCEDURE 1: Yeni Rezervasyon Oluşturma (CreateReservationSP)
-- ----------------------------------------------------------------------------
-- Açıklama: Ehliyet ve Plaka bilgisi ile rezervasyon oluşturur.
-- Fiyatı otomatik hesaplar, tarih kontrolü yapar.
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION CreateReservationSP(
    p_license_no VARCHAR,
    p_plate VARCHAR,
    p_start_date DATE,
    p_end_date DATE
)
RETURNS TABLE(reservation_id INT, total_price NUMERIC)
LANGUAGE plpgsql
AS $$
DECLARE 
    v_customer_id INT;
    v_vehicle_id INT;
    v_daily_price NUMERIC(10,2);
    v_days INT;
BEGIN
    -- 1. Müşteri ID'sini bul
    SELECT c.customer_id INTO v_customer_id 
    FROM customer c
    WHERE c.license_no = p_license_no;

    IF v_customer_id IS NULL THEN
        RAISE EXCEPTION 'Müşteri bulunamadı! Ehliyet No: %', p_license_no;
    END IF;

    -- 2. Araç ID'sini ve Günlük Fiyatını bul
    -- DÜZELTME: Orijinal kodda WHERE koşulu eksikti, rastgele bir aracın fiyatını alıyordu.
    SELECT v.vehicle_id, v.daily_price INTO v_vehicle_id, v_daily_price
    FROM vehicle v
    WHERE v.plate = p_plate AND v.vehicle_status = 'AVAILABLE';

    IF v_vehicle_id IS NULL THEN
        RAISE EXCEPTION 'Araç bulunamadı veya şu an MÜSAİT (AVAILABLE) değil. Plaka: %', p_plate;
    END IF;

    -- 3. Tarih Kontrolü
    IF p_end_date < p_start_date THEN
        RAISE EXCEPTION 'HATA: Bitiş tarihi başlangıç tarihinden önce olamaz!';
    END IF;

    -- 4. Gün ve Fiyat Hesabı
    v_days := (p_end_date - p_start_date);
    IF v_days = 0 THEN
        v_days := 1; -- Aynı gün teslim edilse bile en az 1 günlük ücret alınır.
    END IF;

    total_price := v_daily_price * v_days;

    -- 5. Kayıt Ekleme (Insert)
    INSERT INTO reservation(customer_id, vehicle_id, start_date, end_date, total_price, reservation_status)
    VALUES (v_customer_id, v_vehicle_id, p_start_date, p_end_date, total_price, 'PENDING')
    RETURNING reservation.reservation_id INTO reservation_id;

    RETURN NEXT;
END;
$$;


-- ----------------------------------------------------------------------------
-- PROCEDURE 2: Rezervasyon Onaylama (ApproveReservationProcedure)
-- ----------------------------------------------------------------------------
-- Açıklama: Bekleyen bir rezervasyonu onaylar. 
-- NOT: Bu işlem çalıştığında daha önce yazdığımız TRIGGER devreye girer ve araç statusunu günceller.
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION ApproveReservationProcedure(p_reservation_id INT)
RETURNS TABLE(reservation_id INT, new_reservation_status VARCHAR)
LANGUAGE plpgsql
AS $$
DECLARE
    v_current_status VARCHAR(20);
BEGIN
    -- Mevcut durumu kontrol et
    SELECT r.reservation_status INTO v_current_status 
    FROM reservation r
    WHERE r.reservation_id = p_reservation_id;

    IF v_current_status IS NULL THEN
        RAISE EXCEPTION 'Rezervasyon bulunamadı. ID: %', p_reservation_id;
    END IF;

    IF v_current_status = 'CANCELLED' THEN
        RAISE EXCEPTION 'İptal edilmiş (CANCELLED) bir rezervasyon onaylanamaz!';
    END IF;
    
    IF v_current_status = 'APPROVED' THEN
        RAISE EXCEPTION 'Bu rezervasyon zaten daha önce onaylanmış.';
    END IF;

    -- Güncelleme Yap
    UPDATE reservation
    SET reservation_status = 'APPROVED'
    WHERE reservation.reservation_id = p_reservation_id;

    reservation_id := p_reservation_id;
    new_reservation_status := 'APPROVED';

    RETURN NEXT;
END;
$$;


-- ----------------------------------------------------------------------------
-- PROCEDURE 3: Kiralama Başlatma / Araç Teslimi (StartRentalProcedure)
-- ----------------------------------------------------------------------------
-- Açıklama: Müşteri şubeye geldiğinde Rezervasyonu Kiralamaya (Rental) dönüştürür.
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION StartRentalProcedure(p_reservation_id INT)
RETURNS TABLE(rental_id INT, vehicle_plate VARCHAR)
LANGUAGE plpgsql
AS $$
DECLARE
    v_status VARCHAR(20);
    v_vehicle_id INT;
    v_branch_id INT;
BEGIN
    -- 1. Rezervasyon Kontrolü
    SELECT reservation_status, vehicle_id 
    INTO v_status, v_vehicle_id
    FROM reservation
    WHERE reservation_id = p_reservation_id;

    IF v_status IS NULL THEN
        RAISE EXCEPTION 'Rezervasyon bulunamadı. ID: %', p_reservation_id;
    END IF;

    IF v_status <> 'APPROVED' THEN
        RAISE EXCEPTION 'Sadece ONAYLANMIŞ (APPROVED) rezervasyonlar kiralamaya dönüştürülebilir. Şu anki durum: %', v_status;
    END IF;

    -- 2. Aracın bulunduğu şubeyi bul (Pickup Branch olarak kullanacağız)
    SELECT branch_id INTO v_branch_id FROM vehicle WHERE vehicle_id = v_vehicle_id;

    -- 3. Rental Kaydı Oluştur
    -- NOT: DDL'de pickup_branch_id ve dropoff_branch_id zorunluydu.
    -- Şimdilik ikisini de aracın bulunduğu şube yapıyoruz.
    INSERT INTO rental (
        reservation_id,
        pickup_branch_id,
        dropoff_branch_id,
        rental_date,
        payment_status
    )
    VALUES (
        p_reservation_id,
        v_branch_id, -- Aracı aldığı yer
        v_branch_id, -- Teslim edeceği yer (Varsayılan olarak aynı şube)
        CURRENT_DATE,
        'UNPAID'
    )
    RETURNING rental.rental_id INTO rental_id;

    -- 4. Araç Durumunu Güncelle (Gerçi Trigger'ımız bunu yapıyor ama garanti olsun)
    UPDATE vehicle SET vehicle_status = 'RENTED' WHERE vehicle_id = v_vehicle_id;

    -- 5. Plakayı dön (Bilgi amaçlı)
    SELECT plate INTO vehicle_plate FROM vehicle WHERE vehicle_id = v_vehicle_id;

    RETURN NEXT;
END;
$$;


-- ----------------------------------------------------------------------------
-- PROCEDURE 4: Kiralama Bitirme / Araç İade (FinishRentalProcedure)
-- ----------------------------------------------------------------------------
-- Açıklama: Araç geri geldiğinde Rental kaydını kapatır ve aracı boşa çıkarır.
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION FinishRentalProcedure(p_rental_id INT)
RETURNS TABLE(r_id INT, r_return_date DATE, r_payment_status VARCHAR)
LANGUAGE plpgsql
AS $$
DECLARE 
    v_return_date DATE;
    v_vehicle_id INT;
BEGIN
    -- Mevcut durumu kontrol et
    SELECT r.return_date, res.vehicle_id
    INTO v_return_date, v_vehicle_id
    FROM rental r
    JOIN reservation res ON r.reservation_id = res.reservation_id
    WHERE r.rental_id = p_rental_id;

    IF v_return_date IS NOT NULL THEN
        RAISE EXCEPTION 'Bu kiralama işlemi zaten kapatılmış/araç teslim edilmiş. ID: %', p_rental_id;
    END IF;

    -- 1. Rental Tablosunu Güncelle (Tarih ve Ödeme)
    -- DÜZELTME: Orijinal kodda UPDATE eksikti, sadece SELECT vardı.
    UPDATE rental
    SET return_date = CURRENT_DATE,
        payment_status = 'PAID'
    WHERE rental_id = p_rental_id;

    -- 2. Aracı Boşa Çıkar (Trigger'ımız olsa da burada manuel de yapabiliriz)
    -- DÜZELTME: 'status' sütun adı 'vehicle_status' olarak düzeltildi.
    UPDATE vehicle
    SET vehicle_status = 'AVAILABLE'
    WHERE vehicle_id = v_vehicle_id;

    RETURN QUERY
    SELECT rental_id, return_date, payment_status
    FROM rental
    WHERE rental_id = p_rental_id;
    
END;
$$;


-- ----------------------------------------------------------------------------
-- PROCEDURE 5: Rezervasyon İptali (CancelReservationProcedure)
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION CancelReservationProcedure(p_reservation_id INT)
RETURNS TABLE(reservation_id INT, new_status VARCHAR)
LANGUAGE plpgsql
AS $$
DECLARE
    v_status VARCHAR(20);
BEGIN
    SELECT reservation_status INTO v_status
    FROM reservation WHERE reservation_id = p_reservation_id;

    IF v_status IS NULL THEN RAISE EXCEPTION 'Rezervasyon bulunamadı.'; END IF;

    -- Sadece Bekleyenler iptal edilebilir (Kurumsal kural)
    IF v_status <> 'PENDING' THEN
        RAISE EXCEPTION 'Sadece PENDING statüsündeki rezervasyonlar iptal edilebilir.';
    END IF;

    UPDATE reservation
    SET reservation_status = 'CANCELLED'
    WHERE reservation_id = p_reservation_id;

    reservation_id := p_reservation_id;
    new_status := 'CANCELLED';
    RETURN NEXT;
END;
$$;


-- ----------------------------------------------------------------------------
-- PROCEDURE 6: Fiyat Hesaplama Yardımcısı (CalculateRentalPriceProcedure)
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION CalculateRentalPriceProcedure(p_reservation_id INT)
RETURNS NUMERIC
LANGUAGE plpgsql
AS $$
DECLARE
    v_start DATE;
    v_end DATE;
    v_price NUMERIC;
    v_days INT;
BEGIN
    SELECT r.start_date, r.end_date, v.daily_price
    INTO v_start, v_end, v_price
    FROM reservation r
    JOIN vehicle v ON r.vehicle_id = v.vehicle_id
    WHERE r.reservation_id = p_reservation_id;

    v_days := (v_end - v_start);
    IF v_days <= 0 THEN v_days := 1; END IF;

    RETURN v_days * v_price;
END;
$$;


-- ============================================================================
-- TEST SORGULARI (SCENARIOS)
-- ============================================================================

-- 1. TEST: Rezervasyon Oluşturma (Ford Focus - 34 FRD 34 - AVAILABLE olmalı)
-- Önce aracı AVAILABLE yapalım test için:
UPDATE vehicle SET vehicle_status='AVAILABLE' WHERE plate='34 FRD 34';

SELECT * FROM CreateReservationSP(
  'LIC-TR-0001',   -- Burak Özen
  '34 FRD 34',     -- Ford Focus
  '2025-12-20',
  '2025-12-23'     -- 3 Gün
);

-- Oluşan kaydı görelim
SELECT * FROM reservation ORDER BY reservation_id DESC LIMIT 1;


-- 2. TEST: Rezervasyonu Onaylama
-- Yukarıda oluşan ID'yi (muhtemelen en son ID) parametre veriyoruz.
SELECT * FROM ApproveReservationProcedure((SELECT MAX(reservation_id) FROM reservation));

-- Trigger Kontrolü: Araç RESERVED oldu mu?
SELECT plate, vehicle_status FROM vehicle WHERE plate='34 FRD 34';


-- 3. TEST: Kiralama Başlatma (Teslim Etme)
SELECT * FROM StartRentalProcedure((SELECT MAX(reservation_id) FROM reservation));

-- Trigger Kontrolü: Araç RENTED oldu mu?
SELECT plate, vehicle_status FROM vehicle WHERE plate='34 FRD 34';


-- 4. TEST: Kiralama Bitirme (İade Alma)
-- En son oluşan rental_id'yi bulup bitirelim
SELECT * FROM FinishRentalProcedure((SELECT MAX(rental_id) FROM rental));

-- Sonuç Kontrolü: Araç AVAILABLE, Rental PAID oldu mu?
SELECT plate, vehicle_status FROM vehicle WHERE plate='34 FRD 34';
SELECT * FROM rental ORDER BY rental_id DESC LIMIT 1;