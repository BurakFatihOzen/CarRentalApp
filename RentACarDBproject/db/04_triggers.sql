-- ----------------------------------------------------------------------------
-- TRIGGER 1: trgReserve
-- Açıklama: Rezervasyon onaylandığında (APPROVED), aracı 'RESERVED' moduna alır.
-- ----------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION trgReserve()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    -- Eğer rezervasyon durumu 'APPROVED' olarak güncellendiyse:
    IF NEW.reservation_status = 'APPROVED' THEN
        -- İlgili aracı bul ve durumunu 'RESERVED' yap.
        UPDATE vehicle
        SET vehicle_status = 'RESERVED'
        WHERE vehicle_id = NEW.vehicle_id;
    END IF;
    RETURN NEW; -- İşlemin devam etmesine izin ver.
END;
$$;

-- Trigger Tanımlaması
-- NOT: Tablo tanımında kolon adı 'reservation_status' olduğu için
-- 'UPDATE OF status' yerine 'UPDATE OF reservation_status' yazılmalıdır.
CREATE TRIGGER Reserve
AFTER UPDATE OF reservation_status ON Reservation
FOR EACH ROW
EXECUTE FUNCTION trgReserve();

-- ------------------------
-- TEST SENARYOSU 1
-- ------------------------

-- Önce mevcut duruma bakalım (Araç ve Rezervasyon)
SELECT reservation_id, vehicle_id, reservation_status FROM reservation;
SELECT vehicle_id, plate, vehicle_status FROM vehicle;

-- 3 Numaralı rezervasyonu onaylayalım (Bu işlem Trigger'ı tetikler)
UPDATE reservation
SET reservation_status = 'APPROVED'
WHERE reservation_id = 3;

-- SONUÇ KONTROLÜ:
-- 3 numaralı rezervasyondaki aracın durumu otomatik olarak 'RESERVED' oldu mu?
SELECT vehicle_id, plate, vehicle_status
FROM vehicle
WHERE vehicle_id = (SELECT vehicle_id FROM reservation WHERE reservation_id = 3);

-- Tüm araçların son durumu
SELECT * FROM Vehicle;

-- ----------------------------------------------------------------------------
-- TRIGGER 2: trgRentalInsert
-- Açıklama: Rental tablosuna yeni kayıt eklendiğinde (araç teslim edildiğinde),
-- aracın durumunu 'RENTED' olarak günceller.
-- ----------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION trgRentalInsert()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_vehicle_id INT; -- Aracı bulmak için geçici değişken
BEGIN
    -- Yeni eklenen kiralama kaydındaki (NEW.reservation_id) bilgisini kullanarak
    -- rezervasyon tablosundan araç ID'sini çekiyoruz.
    SELECT vehicle_id INTO v_vehicle_id
    FROM reservation
    WHERE reservation_id = NEW.reservation_id;
    
    -- Bulunan aracın durumunu 'RENTED' yapıyoruz.
    UPDATE vehicle
    SET vehicle_status = 'RENTED'
    WHERE vehicle_id = v_vehicle_id;

    RETURN NEW;
END;
$$;

-- Trigger Tanımlaması
CREATE TRIGGER RentalInsert
AFTER INSERT ON RENTAL 
FOR EACH ROW
EXECUTE FUNCTION trgRentalInsert();

-- ------------------------
-- TEST SENARYOSU 2
-- ------------------------

-- 1. Adım: Onaylanmış (APPROVED) en son rezervasyonu bulalım.
SELECT reservation_id, vehicle_id, reservation_status
FROM reservation
WHERE reservation_status = 'APPROVED'
ORDER BY reservation_id DESC
LIMIT 1;

-- 2. Adım: Bu aracın şu anki durumu nedir? (Muhtemelen RESERVED olmalı)
SELECT v.vehicle_id, v.plate, v.vehicle_status
FROM vehicle v
WHERE v.vehicle_id = (
  SELECT vehicle_id
  FROM reservation
  WHERE reservation_status='APPROVED'
  ORDER BY reservation_id DESC
  LIMIT 1
);

-- 3. Adım: Kiralama işlemini başlatalım (INSERT işlemi Trigger'ı tetikler)
INSERT INTO rental(reservation_id, pickup_branch_id, dropoff_branch_id, rental_date, return_date, payment_status)
VALUES (
  (SELECT reservation_id FROM reservation WHERE reservation_status='APPROVED' ORDER BY reservation_id DESC LIMIT 1),
  (SELECT branch_id FROM branch WHERE branch_name='Kizilay Subesi'),
  (SELECT branch_id FROM branch WHERE branch_name='Besevler Subesi'),
  CURRENT_DATE,
  NULL, -- Henüz iade edilmedi
  'UNPAID'
);

-- SONUÇ KONTROLÜ:
-- Araç durumu 'RESERVED'den 'RENTED'a döndü mü?
SELECT v.vehicle_id, v.plate, v.vehicle_status
FROM vehicle v
WHERE v.vehicle_id = (
  SELECT vehicle_id
  FROM reservation
  WHERE reservation_status='APPROVED'
  ORDER BY reservation_id DESC
  LIMIT 1
);

-- Son 5 kiralama kaydı
SELECT rental_id, reservation_id, rental_date, return_date, payment_status
FROM rental
ORDER BY rental_id DESC
LIMIT 5;


-- ----------------------------------------------------------------------------
-- TRIGGER 3: trgReturnDate
-- Açıklama: Rental tablosunda iade tarihi (return_date) girildiğinde,
-- aracı tekrar 'AVAILABLE' (Müsait) durumuna getirir.
-- ----------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION trgReturnDate()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_vehicle_id INT;
BEGIN
    -- Eğer return_date alanı NULL değilse (yani araç iade edildiyse)
    IF NEW.return_date IS NOT NULL THEN
        
        -- Rezervasyon ID üzerinden araç ID'sini bul
        SELECT vehicle_id INTO v_vehicle_id 
        FROM Reservation 
        WHERE reservation_id = NEW.reservation_id;

        -- Aracın durumunu 'AVAILABLE' yap
        UPDATE vehicle
        SET vehicle_status = 'AVAILABLE'
        WHERE vehicle_id = v_vehicle_id;
    END IF;

    RETURN NEW;
END;
$$;

-- Trigger Tanımlaması
-- Sadece 'return_date' kolonu güncellendiğinde çalışır. Performans dostudur.
CREATE TRIGGER ReturnDate
AFTER UPDATE OF return_date ON Rental
FOR EACH ROW
EXECUTE FUNCTION trgReturnDate();

-- ------------------------
-- TEST SENARYOSU 3
-- ------------------------

-- 5 numaralı kiralama işlemini bitirelim (Aracı iade alalım)
-- return_date'i bugünden 2 gün sonrasına set ediyoruz.
UPDATE rental
SET return_date = CURRENT_DATE + 2
WHERE rental_id = 5; -- NOT: Mevcut bir ID kullandığından emin olmalısın.

-- SONUÇ KONTROLÜ:
-- İlgili araç tekrar 'AVAILABLE' oldu mu?
SELECT vehicle_id, plate, vehicle_status
FROM vehicle
WHERE vehicle_id = (SELECT vehicle_id FROM reservation WHERE reservation_id = 3);


-- ----------------------------------------------------------------------------
-- TRIGGER 4: check_vehicle_approved
-- Açıklama: Bir araç için zaten APPROVED statüsünde aktif bir rezervasyon varsa,
-- ikinci bir rezervasyonun onaylanmasını (APPROVED yapılmasını) engeller.
-- ----------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION check_vehicle_approved()
RETURNS TRIGGER AS $$
BEGIN
    -- Eğer işlem yapılan satırın yeni durumu 'APPROVED' ise kontrol et:
    IF NEW.reservation_status = 'APPROVED' THEN
        
        IF EXISTS (
            SELECT 1
            FROM reservation
            WHERE vehicle_id = NEW.vehicle_id           -- Aynı araç mı?
              AND reservation_status = 'APPROVED'       -- Zaten onaylı mı?
              AND reservation_id <> NEW.reservation_id  -- Kendisi hariç mi? (Update işlemlerinde hata almamak için)
        ) THEN
            -- Eğer yukarıdaki şartları sağlayan bir kayıt varsa HATA FIRLAT
            RAISE EXCEPTION 
            'HATA: Bu araç (ID: %) için zaten ONAYLANMIŞ bir rezervasyon mevcut!', NEW.vehicle_id;
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger Tanımlaması
-- Hem yeni kayıt eklerken (INSERT) hem güncelleme yaparken (UPDATE) çalışmalı.
-- BEFORE trigger kullanılır çünkü hata varsa işlem veritabanına yazılmadan durdurulmalı.
CREATE TRIGGER trg_check_vehicle_approved
BEFORE INSERT OR UPDATE ON reservation
FOR EACH ROW
EXECUTE FUNCTION check_vehicle_approved();

-- ------------------------
-- TEST SENARYOSU 4
-- ------------------------

-- Örnek: 6 numaralı rezervasyonu iptal edelim (Conflict testi değil, genel update testi)
UPDATE reservation
SET reservation_status='CANCELLED'
WHERE reservation_id=6;

-- ÇAKIŞMA TESTİ YAPMAK İÇİN:
-- 1. Önce bir aracı APPROVED yapın.
-- 2. Sonra aynı araca ait başka bir rezervasyonu APPROVED yapmaya çalışın.
-- 3. SQL Error: "Bu araç için zaten APPROVED bir rezervasyon var" hatasını almalısınız.
