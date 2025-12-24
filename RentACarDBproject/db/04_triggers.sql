-- ============================================================================
-- 0. ÖN HAZIRLIK: KISITLAMA VE GÜVENLİK KİLİDİ
-- ============================================================================

-- COMPLETED durumunu sisteme tanıtıyoruz (Hata almamak için şart)
ALTER TABLE reservation DROP CONSTRAINT IF EXISTS reservation_reservation_status_check;
ALTER TABLE reservation ADD CONSTRAINT reservation_reservation_status_check 
CHECK (reservation_status IN ('PENDING', 'APPROVED', 'CANCELLED', 'COMPLETED'));

-- BAKIM KONTROLÜ FONKSİYONU: Araç bakımdayken onay veya kiralama yapılmasını engeller.
CREATE OR REPLACE FUNCTION check_maintenance_integrity()
RETURNS TRIGGER AS $$
DECLARE
    v_veh_status VARCHAR(20);
BEGIN
    SELECT vehicle_status INTO v_veh_status FROM vehicle WHERE vehicle_id = NEW.vehicle_id;

    -- Eğer araç bakımdaysa ve birisi onaylamaya veya kiralamaya çalışıyorsa işlemi DURDUR
    IF v_veh_status = 'MAINTENANCE' AND NEW.reservation_status IN ('APPROVED', 'COMPLETED') THEN
        RAISE EXCEPTION 'HATA: Araç BAKIMDA olduğu için bu işlem yapılamaz!';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Rezervasyon tablosuna bakım korumasını ekle (İşlem yapılmadan önce kontrol eder)
DROP TRIGGER IF EXISTS trg_maintenance_lock ON reservation;
CREATE TRIGGER trg_maintenance_lock
BEFORE UPDATE OF reservation_status ON reservation
FOR EACH ROW EXECUTE FUNCTION check_maintenance_integrity();


-- ============================================================================
-- 1. TRIGGER: trgReserve (Onay -> Rezerve)
-- ============================================================================
CREATE OR REPLACE FUNCTION trgReserve()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.reservation_status = 'APPROVED' THEN
        UPDATE vehicle SET vehicle_status = 'RESERVED' WHERE vehicle_id = NEW.vehicle_id;
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS Reserve ON Reservation;
CREATE TRIGGER Reserve AFTER UPDATE OF reservation_status ON Reservation
FOR EACH ROW EXECUTE FUNCTION trgReserve();


-- ============================================================================
-- 2. TRIGGER: trgRentalInsert (Teslim Etme -> Kirada)
-- ============================================================================
CREATE OR REPLACE FUNCTION trgRentalInsert()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    v_vehicle_id INT;
BEGIN
    SELECT vehicle_id INTO v_vehicle_id FROM reservation WHERE reservation_id = NEW.reservation_id;
    
    -- Aracı Kiraya ver ve Rezervasyonu Tamamla
    UPDATE vehicle SET vehicle_status = 'RENTED' WHERE vehicle_id = v_vehicle_id;
    UPDATE reservation SET reservation_status = 'COMPLETED' WHERE reservation_id = NEW.reservation_id;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS RentalInsert ON Rental;
CREATE TRIGGER RentalInsert AFTER INSERT ON Rental
FOR EACH ROW EXECUTE FUNCTION trgRentalInsert();


-- ============================================================================
-- 3. TRIGGER: trgReturnDate (İade Alındı -> Müsait)
-- ============================================================================
CREATE OR REPLACE FUNCTION trgReturnDate()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    v_vehicle_id INT;
BEGIN
    IF NEW.return_date IS NOT NULL THEN
        SELECT vehicle_id INTO v_vehicle_id FROM Reservation WHERE reservation_id = NEW.reservation_id;
        UPDATE vehicle SET vehicle_status = 'AVAILABLE' WHERE vehicle_id = v_vehicle_id;
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS ReturnDate ON Rental;
CREATE TRIGGER ReturnDate AFTER UPDATE OF return_date ON Rental
FOR EACH ROW EXECUTE FUNCTION trgReturnDate();


-- ============================================================================
-- 4. TRIGGER: check_vehicle_approved (Çakışma Engelleme)
-- ============================================================================
CREATE OR REPLACE FUNCTION check_vehicle_approved()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.reservation_status = 'APPROVED' THEN
        IF EXISTS (
            SELECT 1 FROM reservation
            WHERE vehicle_id = NEW.vehicle_id 
              AND reservation_status = 'APPROVED' 
              AND reservation_id <> NEW.reservation_id
        ) THEN
            RAISE EXCEPTION 'HATA: Bu araç için zaten ONAYLANMIŞ aktif bir rezervasyon mevcut!';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_check_vehicle_approved ON reservation;
CREATE TRIGGER trg_check_vehicle_approved BEFORE INSERT OR UPDATE ON reservation
FOR EACH ROW EXECUTE FUNCTION check_vehicle_approved();






-- ============================================================================
-- 5. TRIGGER: check_rental_integrity (Kiralama Kontrol)
-- ============================================================================
CREATE OR REPLACE FUNCTION check_rental_integrity()
RETURNS TRIGGER AS $$
DECLARE
    v_veh_status VARCHAR(20);
    v_vehicle_id INT;
BEGIN
    -- Rezervasyon üzerinden aracın mevcut durumunu bul
    SELECT vehicle_id INTO v_vehicle_id FROM reservation WHERE reservation_id = NEW.reservation_id;
    SELECT vehicle_status INTO v_veh_status FROM vehicle WHERE vehicle_id = v_vehicle_id;

    -- Eğer araç zaten kiradaysa (RENTED), işlemi durdur ve hata fırlat
    IF v_veh_status = 'RENTED' THEN
        RAISE EXCEPTION 'KRİTİK HATA: Bu araç zaten kirada (RENTED)! Aynı araç tekrar kiralanamaz.';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;





-- ============================================================================
-- 6. TRIGGER: check_vehicle_availability_for_approval (Araç Müsaitlik Kontrol)
-- ============================================================================
CREATE OR REPLACE FUNCTION check_vehicle_availability_for_approval()
RETURNS TRIGGER AS $$
DECLARE
    v_current_status VARCHAR(20);
BEGIN
    -- Aracın güncel durumunu (AVAILABLE, RENTED, MAINTENANCE vb.) alıyoruz
    SELECT vehicle_status INTO v_current_status FROM vehicle WHERE vehicle_id = NEW.vehicle_id;

    -- EĞER rezervasyon ONAYLANMAYA (APPROVED) çalışılıyorsa
    IF NEW.reservation_status = 'APPROVED' THEN
        -- Ve eğer araç MÜSAİT (AVAILABLE) değilse
        IF v_current_status != 'AVAILABLE' THEN
            RAISE EXCEPTION 'KRİTİK HATA: Bu araç şu an % durumunda. Müsait olmayan araç REZERVE EDİLEMEZ!', v_current_status;
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Rezervasyon güncellenmeden ÖNCE (BEFORE) bu kontrolü yapıyoruz
DROP TRIGGER IF EXISTS trg_check_availability ON reservation;
CREATE TRIGGER trg_check_availability
BEFORE UPDATE OF reservation_status ON reservation
FOR EACH ROW EXECUTE FUNCTION check_vehicle_availability_for_approval();


-- Rental tablosuna kayıt atılmadan ÖNCE (BEFORE) kontrolü çalıştır
DROP TRIGGER IF EXISTS trg_rental_integrity_check ON rental;
CREATE TRIGGER trg_rental_integrity_check
BEFORE INSERT ON rental
FOR EACH ROW EXECUTE FUNCTION check_rental_integrity();

-- ----------------------------------------------------------------------------
-- TEST SENARYOLARI (KENDİ TESTLERİN VE GÜNCEL KONTROLLER)
-- ----------------------------------------------------------------------------

-- TEST 1: BAKIM KONTROLÜ (HATA VERMELİ)
-- Aracı bakıma alalım
UPDATE vehicle SET vehicle_status = 'MAINTENANCE' WHERE vehicle_id = (SELECT vehicle_id FROM reservation WHERE reservation_id = 3);
-- Onaylamaya çalışalım (trg_maintenance_lock sayesinde 'HATA: Araç BAKIMDA' mesajı gelmeli)
-- UPDATE reservation SET reservation_status = 'APPROVED' WHERE reservation_id = 3;


-- TEST 2: REZERVASYON ONAY TESTİ
-- Aracı tekrar müsait yapalım
UPDATE vehicle SET vehicle_status = 'AVAILABLE' WHERE vehicle_id = (SELECT vehicle_id FROM reservation WHERE reservation_id = 3);
-- 3 Numaralı rezervasyonu onaylayalım (Trigger Araç 'RESERVED' yapmalı)
UPDATE reservation SET reservation_status = 'APPROVED' WHERE reservation_id = 3;

-- SONUÇ KONTROLÜ 1:
SELECT vehicle_id, plate, vehicle_status FROM vehicle WHERE vehicle_id = (SELECT vehicle_id FROM reservation WHERE reservation_id = 3);


-- TEST 3: KİRALAMA BAŞLATMA (TESLİMAT TESTİ)
-- Kiralama kaydı ekleyelim (Trigger Araç 'RENTED', Rezervasyon 'COMPLETED' yapmalı)
INSERT INTO rental(reservation_id, rental_date, payment_status, pickup_branch_id, dropoff_branch_id)
VALUES (3, CURRENT_DATE, 'UNPAID', 1, 1);

-- SONUÇ KONTROLÜ 2:
SELECT vehicle_status FROM vehicle WHERE vehicle_id = (SELECT vehicle_id FROM reservation WHERE reservation_id = 3);
SELECT reservation_status FROM reservation WHERE reservation_id = 3;


-- TEST 4: KİRALAMA BİTİRME (İADE TESTİ)
-- Aracı iade alalım (Trigger Araç 'AVAILABLE' yapmalı)
UPDATE rental SET return_date = CURRENT_DATE WHERE reservation_id = 3;

-- SONUÇ KONTROLÜ 3:
SELECT vehicle_status FROM vehicle WHERE vehicle_id = (SELECT vehicle_id FROM reservation WHERE reservation_id = 3);

