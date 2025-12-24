-- ============================================================================
-- 1. PROCEDURE: CreateReservationSP (Rezervasyon Oluşturma)
-- ============================================================================
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
    v_veh_status VARCHAR(20);
BEGIN
    -- Müşteri kontrolü
    SELECT c.customer_id INTO v_customer_id FROM customer c WHERE c.license_no = p_license_no;
    IF v_customer_id IS NULL THEN
        RAISE EXCEPTION 'Müşteri bulunamadı! Ehliyet No: %', p_license_no;
    END IF;

    -- Araç ve Durum kontrolü
    SELECT v.vehicle_id, v.daily_price, v.vehicle_status 
    INTO v_vehicle_id, v_daily_price, v_veh_status
    FROM vehicle v WHERE v.plate = p_plate;

    IF v_vehicle_id IS NULL THEN
        RAISE EXCEPTION 'Araç bulunamadı. Plaka: %', p_plate;
    END IF;

    -- GÜVENLİK: Bakımdaki araç için rezervasyon engellenir
    IF v_veh_status = 'MAINTENANCE' THEN
        RAISE EXCEPTION 'HATA: Bu araç BAKIMDA. Rezervasyon yapılamaz!';
    END IF;

    -- Tarih Kontrolü
    IF p_end_date < p_start_date THEN
        RAISE EXCEPTION 'HATA: Bitiş tarihi başlangıç tarihinden önce olamaz!';
    END IF;

    v_days := (p_end_date - p_start_date);
    IF v_days <= 0 THEN v_days := 1; END IF;
    total_price := v_daily_price * v_days;

    INSERT INTO reservation(customer_id, vehicle_id, start_date, end_date, total_price, reservation_status)
    VALUES (v_customer_id, v_vehicle_id, p_start_date, p_end_date, total_price, 'PENDING')
    RETURNING reservation.reservation_id INTO reservation_id;

    RETURN NEXT;
END;
$$;

-- ============================================================================
-- 2. PROCEDURE: ApproveReservationProcedure (Onaylama)
-- ============================================================================
CREATE OR REPLACE FUNCTION ApproveReservationProcedure(p_reservation_id INT)
RETURNS TABLE(r_id INT, new_status VARCHAR)
LANGUAGE plpgsql
AS $$
DECLARE
    v_res_status VARCHAR(20);
    v_veh_status VARCHAR(20);
BEGIN
    SELECT r.reservation_status, v.vehicle_status INTO v_res_status, v_veh_status
    FROM reservation r JOIN vehicle v ON r.vehicle_id = v.vehicle_id
    WHERE r.reservation_id = p_reservation_id;

    -- GÜVENLİK KONTROLÜ: Onay anında araç bakımda mı?
    IF v_veh_status = 'MAINTENANCE' THEN
        RAISE EXCEPTION 'HATA: Araç BAKIMDA olduğu için bu rezervasyon ONAYLANAMAZ!';
    END IF;

    IF v_res_status <> 'PENDING' THEN 
        RAISE EXCEPTION 'Sadece beklemedeki (PENDING) kayıtlar onaylanabilir.'; 
    END IF;

    UPDATE reservation SET reservation_status = 'APPROVED' WHERE reservation_id = p_reservation_id;

    r_id := p_reservation_id;
    new_status := 'APPROVED';
    RETURN NEXT;
END;
$$;

-- ============================================================================
-- 3. PROCEDURE: StartRentalProcedure (Kiralama Başlatma)
-- ============================================================================
CREATE OR REPLACE FUNCTION StartRentalProcedure(p_reservation_id INT)
RETURNS TABLE(rental_id INT, vehicle_plate VARCHAR)
LANGUAGE plpgsql
AS $$
DECLARE
    v_res_status VARCHAR(20);
    v_veh_status VARCHAR(20);
    v_vehicle_id INT;
    v_branch_id INT;
BEGIN
    SELECT r.reservation_status, v.vehicle_status, v.vehicle_id, v.branch_id, v.plate
    INTO v_res_status, v_veh_status, v_vehicle_id, v_branch_id, vehicle_plate
    FROM reservation r JOIN vehicle v ON r.vehicle_id = v.vehicle_id
    WHERE r.reservation_id = p_reservation_id;

    IF v_res_status <> 'APPROVED' THEN
        RAISE EXCEPTION 'Sadece ONAYLANMIŞ (APPROVED) kayıtlar kiraya verilebilir.';
    END IF;

    -- GÜVENLİK KONTROLÜ: Teslimat anında bakım kontrolü
    IF v_veh_status = 'MAINTENANCE' THEN
        RAISE EXCEPTION 'HATA: Araç şu an BAKIMDA. Teslimat yapılamaz!';
    END IF;

    INSERT INTO rental (reservation_id, pickup_branch_id, dropoff_branch_id, rental_date, payment_status)
    VALUES (p_reservation_id, v_branch_id, v_branch_id, CURRENT_DATE, 'UNPAID')
    RETURNING rental.rental_id INTO rental_id;
    
    RETURN NEXT;
END;
$$;

-- ============================================================================
-- 4. PROCEDURE: FinishRentalProcedure (İade Alma)
-- ============================================================================
CREATE OR REPLACE FUNCTION FinishRentalProcedure(p_rental_id INT)
RETURNS TABLE(r_id INT, r_return_date DATE, r_payment_status VARCHAR)
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE rental SET return_date = CURRENT_DATE, payment_status = 'PAID'
    WHERE rental_id = p_rental_id AND return_date IS NULL;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Kiralama kaydı bulunamadı veya zaten kapatılmış.';
    END IF;

    RETURN QUERY SELECT p_rental_id, CURRENT_DATE, CAST('PAID' AS VARCHAR);
END;
$$;

-- ============================================================================
-- TEST SENARYOLARI (SQL KONSOLUNDA DENEYEBİLİRSİN)
-- ============================================================================

-- SENARYO 1: BAKIMDAKİ ARAÇ TESTİ (Hata Vermeli)
-- 1. Bir aracı bakıma alalım
UPDATE vehicle SET vehicle_status = 'MAINTENANCE' WHERE plate = '06 ALT 301';

-- 2. Rezervasyon yapmaya çalışalım (HATA fırlatmalı)
SELECT * FROM CreateReservationSP('LIC-TR-0001', '06 ALT 301', '2025-12-25', '2025-12-27');


-- SENARYO 2: BAŞARILI KİRALAMA DÖNGÜSÜ
-- 1. Aracı müsait yapalım
UPDATE vehicle SET vehicle_status = 'AVAILABLE' WHERE plate = '34 ABC 123';

-- 2. Rezervasyon oluştur (ID'yi aklında tut, örn: 10)
SELECT * FROM CreateReservationSP('LIC-TR-0001', '34 ABC 123', '2025-12-22', '2025-12-24');

-- 3. Onayla (Trigger ile Araç RESERVED olur)
SELECT * FROM ApproveReservationProcedure((SELECT MAX(reservation_id) FROM reservation));

-- 4. Kiralama Başlat (Trigger ile Araç RENTED, Rezervasyon COMPLETED olur)
SELECT * FROM StartRentalProcedure((SELECT MAX(reservation_id) FROM reservation));

-- 5. Kiralama Bitir (Trigger ile Araç AVAILABLE olur)
SELECT * FROM FinishRentalProcedure((SELECT MAX(rental_id) FROM rental));

-- SONUÇ KONTROLÜ
SELECT plate, vehicle_status FROM vehicle WHERE plate = '34 ABC 123';