CREATE DATABASE "GreenHoodDB"
    WITH
    OWNER = postgres
    ENCODING = 'UTF8';

CREATE TABLE province (
	provinceid INT PRIMARY KEY, -- license plate code
	provincename VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE district (
	districtid SERIAL PRIMARY KEY,
	districtname VARCHAR(50) NOT NULL,
	provinceID INT NOT NULL,

	CONSTRAINT fk_province_district FOREIGN KEY (provinceID) REFERENCES province(provinceid)
);

CREATE TABLE neighborhood (
    neighborhoodid SERIAL PRIMARY KEY,
    neighborhoodname VARCHAR(100) NOT NULL,
    districtID INT NOT NULL,
	
    CONSTRAINT fk_district_neighborhood FOREIGN KEY (districtID) REFERENCES district(districtid) ON DELETE CASCADE
);

CREATE SEQUENCE street_streetid_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE street (
    streetid INTEGER DEFAULT nextval('street_streetid_sequence') PRIMARY KEY,
    streetname VARCHAR(100) NOT NULL,
    neighborhoodID INT NOT NULL,
	
    CONSTRAINT fk_neighborhood_street FOREIGN KEY (neighborhoodID) REFERENCES neighborhood(neighborhoodID) ON DELETE CASCADE
);

ALTER SEQUENCE street_streetid_sequence OWNED BY street.streetid;

CREATE TABLE address (
	addressid SERIAL PRIMARY KEY,
	streetID INT NOT NULL,
	buildingno INT NOT NULL,
	floorno INT NOT NULL,
	doorno INT,

	CONSTRAINT fk_address_street FOREIGN KEY (streetID) REFERENCES street(streetid)
);

CREATE TABLE neighbor (
	fname VARCHAR(30) NOT NULL,
	mname VARCHAR(30) DEFAULT NULL,
	lname VARCHAR(30) NOT NULL,
	neighborid SERIAL NOT NULL UNIQUE,
	tckn CHAR(11) PRIMARY KEY,
	bdate DATE NOT NULL,
	joindate DATE DEFAULT CURRENT_DATE NOT NULL,
	email VARCHAR(70) UNIQUE,
	contactnumber VARCHAR(16) UNIQUE,
	addressID INT UNIQUE,
	sex CHAR(1),

	CONSTRAINT fk_address_id_to_neighbor
	  FOREIGN KEY (addressID)
	  REFERENCES address(addressid)
	  ON DELETE SET NULL
);

CREATE TABLE neighbor_passwords (
    authID SERIAL PRIMARY KEY,
    nID INT NOT NULL UNIQUE,
    passwordhash VARCHAR(255) NOT NULL,
	lastresetrequest TIMESTAMP DEFAULT NULL,
    
    -- Foreign Key (neighbor ID)
    CONSTRAINT fk_neighbor_id
      FOREIGN KEY (nID) 
      REFERENCES neighbor(neighborid) 
      ON DELETE CASCADE
);

CREATE TABLE disposal (
	disposalID SERIAL PRIMARY KEY,
	disposalname VARCHAR(30) UNIQUE NOT NULL,
	tcostcoef NUMERIC NOT NULL,
	scorecoef NUMERIC NOT NULL
);

CREATE TABLE discarded_disposal (
	ddno SERIAL PRIMARY KEY,
	dID INT NOT NULL,
	neighbortckn VARCHAR(11),
	ddate DATE DEFAULT CURRENT_DATE NOT NULL,
	weight NUMERIC NOT NULL,
	volume NUMERIC NOT NULL,
	rstatus BOOL NOT NULL,
	ddscore NUMERIC NOT NULL,
	tcost NUMERIC NOT NULL,

	-- Foreign Key (disposal ID)
    CONSTRAINT fk_disposal_id_for_discarded_disposal
      FOREIGN KEY (dID) 
      REFERENCES disposal(disposalID) 
      ON DELETE CASCADE,

	-- Foreign Key (neighbor TCKN)
    CONSTRAINT fk_neighbor_tckn
      FOREIGN KEY (neighbortckn)
      REFERENCES neighbor(tckn)
	  ON UPDATE CASCADE
      ON DELETE SET NULL,

	CONSTRAINT chk_weight_positive CHECK (weight > 0),
	CONSTRAINT chk_volume_positive CHECK (volume > 0)
);

CREATE TABLE company (
	companyID SERIAL PRIMARY KEY,
	taxnumber VARCHAR(11) UNIQUE NOT NULL,
	cname VARCHAR(100) UNIQUE NOT NULL,
	contactnumber VARCHAR(16) UNIQUE,
	faxnumber VARCHAR(12) UNIQUE,
	addressID INT UNIQUE,
	joindate DATE DEFAULT CURRENT_DATE NOT NULL,
	governmentservice BOOL DEFAULT FALSE NOT NULL,

	CONSTRAINT fk_address_id_to_company
	  FOREIGN KEY (addressID)
	  REFERENCES address(addressid)
	  ON DELETE SET NULL
);

CREATE TABLE company_passwords (
    authID SERIAL PRIMARY KEY,
    cID INT NOT NULL UNIQUE,
    passwordhash VARCHAR(255) NOT NULL,
    
    -- Foreign Key (company ID)
    CONSTRAINT fk_company_id_for_auth
      FOREIGN KEY (cID) 
      REFERENCES company(companyID) 
      ON DELETE CASCADE
);

CREATE TABLE company_disposal (
	cID INT NOT NULL,
	dID INT NOT NULL,

	PRIMARY KEY (cID, dID),

	-- Foreign Key (company ID)
    CONSTRAINT fk_company_id_for_disposal
      FOREIGN KEY (cID)
      REFERENCES company(companyID)
      ON DELETE CASCADE,

	  -- Foreign Key (disposal ID)
    CONSTRAINT fk_disposal_id_for_company_disposal
      FOREIGN KEY (dID)
      REFERENCES disposal(disposalID)
      ON DELETE CASCADE
);

CREATE TABLE reservation (
	reservationNo SERIAL PRIMARY KEY,
	cID INT NOT NULL,
	reservationdate DATE DEFAULT CURRENT_DATE NOT NULL,
	recycledate DATE,

	-- Foreign Key (company ID)
    CONSTRAINT fk_company_id_for_reservation
      FOREIGN KEY (cID)
      REFERENCES company(companyID)
      ON DELETE CASCADE
);

CREATE TABLE reservation_disposal (
	rnumber INT NOT NULL,
	ddnumber INT PRIMARY KEY,

	-- Foreign Key (reservation number)
    CONSTRAINT fk_reservation_number
      FOREIGN KEY (rnumber)
      REFERENCES reservation(reservationNo)
      ON DELETE CASCADE,

	  -- Foreign Key (discarded disposal number)
    CONSTRAINT fk_discarded_disposal_number
      FOREIGN KEY (ddnumber)
      REFERENCES discarded_disposal(ddno)
      ON DELETE CASCADE
);

CREATE TABLE audit_log (
    logid SERIAL PRIMARY KEY,
    tablename VARCHAR(30),
	username VARCHAR(50),
    operation VARCHAR(10),
    recordpk JSONB,
    olddata JSONB,
    newdata JSONB,
    operationdate TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_dd_neighbortckn ON discarded_disposal(neighbortckn);
CREATE INDEX IF NOT EXISTS idx_reservation_cid ON reservation(cID);
CREATE INDEX IF NOT EXISTS idx_dd_ddate ON discarded_disposal(ddate DESC);
CREATE INDEX IF NOT EXISTS idx_street_neighborhood ON street(neighborhoodID);
CREATE INDEX IF NOT EXISTS idx_neighborhood_district ON neighborhood(districtID);
CREATE INDEX IF NOT EXISTS idx_address_street_id ON address(streetID);
CREATE INDEX IF NOT EXISTS idx_district_province_id ON district(provinceID);

-- Log trigger function
CREATE OR REPLACE FUNCTION log_audit_event() RETURNS TRIGGER AS $$
DECLARE
    v_old_data JSONB;
    v_new_data JSONB;
    v_pk_json JSONB := '{}'; -- Empty JSON
    v_col TEXT;
    v_val TEXT;
	v_app_user TEXT;
BEGIN
	RAISE NOTICE 'log_audit_event tetiklendi!';

	v_app_user := current_setting('app.current_user', true);
    IF v_app_user IS NULL THEN
        v_app_user := session_user;
    END IF;

    -- Prepare data according to the operation
    IF (TG_OP = 'INSERT') THEN
        v_old_data := NULL;
        v_new_data := to_jsonb(NEW);
    ELSIF (TG_OP = 'UPDATE') THEN
        v_old_data := to_jsonb(OLD);
        v_new_data := to_jsonb(NEW);
    ELSIF (TG_OP = 'DELETE') THEN
        v_old_data := to_jsonb(OLD);
        v_new_data := NULL;
    END IF;

    -- Find primary keys and store them into JSONs
    FOREACH v_col IN ARRAY TG_ARGV LOOP
        IF (TG_OP = 'DELETE') THEN
            -- For DELETE, data inside of the OLD
            EXECUTE format('SELECT ($1).%I::text', v_col) USING OLD INTO v_val;
        ELSE
            -- For INSERT ve UPDATE, data inside of the NEW
            EXECUTE format('SELECT ($1).%I::text', v_col) USING NEW INTO v_val;
        END IF;
        
        -- Add it into JSON: { "column": "value" }
        v_pk_json := jsonb_set(v_pk_json, ARRAY[v_col], to_jsonb(v_val));
    END LOOP;

    -- Insert the new log
    INSERT INTO audit_log (tablename, operation, recordpk, olddata, newdata, username)
    VALUES (TG_TABLE_NAME, TG_OP, v_pk_json, v_old_data, v_new_data, v_app_user);

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER log_neighbor
AFTER INSERT OR UPDATE OR DELETE ON neighbor
FOR EACH ROW EXECUTE FUNCTION log_audit_event('neighborid');

CREATE TRIGGER log_neighbor_passwords
AFTER INSERT OR UPDATE OR DELETE ON neighbor_passwords
FOR EACH ROW EXECUTE FUNCTION log_audit_event('authid');

CREATE TRIGGER log_disposal
AFTER INSERT OR UPDATE OR DELETE ON disposal
FOR EACH ROW EXECUTE FUNCTION log_audit_event('disposalid');

CREATE TRIGGER log_discarded_disposal
AFTER INSERT OR UPDATE OR DELETE ON discarded_disposal
FOR EACH ROW EXECUTE FUNCTION log_audit_event('ddno');

CREATE TRIGGER log_company
AFTER INSERT OR UPDATE OR DELETE ON company
FOR EACH ROW EXECUTE FUNCTION log_audit_event('companyid');

CREATE TRIGGER log_company_passwords
AFTER INSERT OR UPDATE OR DELETE ON company_passwords
FOR EACH ROW EXECUTE FUNCTION log_audit_event('authid');

CREATE TRIGGER log_company_disposal
AFTER INSERT OR UPDATE OR DELETE ON company_disposal
FOR EACH ROW EXECUTE FUNCTION log_audit_event('cid', 'did');

CREATE TRIGGER log_reservation
AFTER INSERT OR UPDATE OR DELETE ON reservation
FOR EACH ROW EXECUTE FUNCTION log_audit_event('reservationno');

CREATE TRIGGER log_reservation_disposal
AFTER INSERT OR UPDATE OR DELETE ON reservation_disposal
FOR EACH ROW EXECUTE FUNCTION log_audit_event('ddnumber');

-- Inserting all Turkiye provinces into province table
INSERT INTO province (provinceid, provincename) 
VALUES
	(1, 'ADANA'), (2, 'ADIYAMAN'), (3, 'AFYONKARAHİSAR'), (4, 'AĞRI'), (5, 'AMASYA'),
	(6, 'ANKARA'), (7, 'ANTALYA'), (8, 'ARTVİN'), (9, 'AYDIN'), (10, 'BALIKESİR'),
	(11, 'BİLECİK'), (12, 'BİNGÖL'), (13, 'BİTLİS'), (14, 'BOLU'), (15, 'BURDUR'),
	(16, 'BURSA'), (17, 'ÇANAKKALE'), (18, 'ÇANKIRI'), (19, 'ÇORUM'), (20, 'DENİZLİ'),
	(21, 'DİYARBAKIR'), (22, 'EDİRNE'), (23, 'ELAZIĞ'), (24, 'ERZİNCAN'), (25, 'ERZURUM'),
	(26, 'ESKİŞEHİR'), (27, 'GAZİANTEP'), (28, 'GİRESUN'), (29, 'GÜMÜŞHANE'), (30, 'HAKKARİ'),
	(31, 'HATAY'), (32, 'ISPARTA'), (33, 'MERSİN'), (34, 'İSTANBUL'), (35, 'İZMİR'),
	(36, 'KARS'), (37, 'KASTAMONU'), (38, 'KAYSERİ'), (39, 'KIRKLARELİ'), (40, 'KIRŞEHİR'),
	(41, 'KOCAELİ'), (42, 'KONYA'), (43, 'KÜTAHYA'), (44, 'MALATYA'), (45, 'MANİSA'),
	(46, 'KAHRAMANMARAŞ'), (47, 'MARDİN'), (48, 'MUĞLA'), (49, 'MUŞ'), (50, 'NEVŞEHİR'),
	(51, 'NİĞDE'), (52, 'ORDU'), (53, 'RİZE'), (54, 'SAKARYA'), (55, 'SAMSUN'),
	(56, 'SİİRT'), (57, 'SİNOP'), (58, 'SİVAS'), (59, 'TEKİRDAĞ'), (60, 'TOKAT'),
	(61, 'TRABZON'), (62, 'TUNCELİ'), (63, 'ŞANLIURFA'), (64, 'UŞAK'), (65, 'VAN'),
	(66, 'YOZGAT'), (67, 'ZONGULDAK'), (68, 'AKSARAY'), (69, 'BAYBURT'), (70, 'KARAMAN'),
	(71, 'KIRIKKALE'), (72, 'BATMAN'), (73, 'ŞIRNAK'), (74, 'BARTIN'), (75, 'ARDAHAN'),
	(76, 'IĞDIR'), (77, 'YALOVA'), (78, 'KARABÜK'), (79, 'KİLİS'), (80, 'OSMANİYE'),
	(81, 'DÜZCE');

-- Inserting random districts into district table
INSERT INTO district (provinceID, districtname) 
VALUES
-- 01 ADANA
(1, 'ALADAĞ'), (1, 'CEYHAN'), (1, 'ÇUKUROVA'), (1, 'FEKE'), (1, 'İMAMOĞLU'), (1, 'KARAİSALI'), (1, 'KARATAŞ'), (1, 'KOZAN'), (1, 'POZANTI'), (1, 'SAİMBEYLİ'), (1, 'SARIÇAM'), (1, 'SEYHAN'), (1, 'TUFANBEYLİ'), (1, 'YUMURTALIK'), (1, 'YÜREĞİR'),
-- 02 ADIYAMAN
(2, 'BESNİ'), (2, 'ÇELİKHAN'), (2, 'GERGER'), (2, 'GÖLBAŞI'), (2, 'KAHTA'), (2, 'MERKEZ'), (2, 'SAMSAT'), (2, 'SİNCİK'), (2, 'TUT'),
-- 03 AFYONKARAHİSAR
(3, 'BAŞMAKÇI'), (3, 'BAYAT'), (3, 'BOLVADİN'), (3, 'ÇAY'), (3, 'ÇOBANLAR'), (3, 'DAZKIRI'), (3, 'DİNAR'), (3, 'EMİRDAĞ'), (3, 'EVCİLER'), (3, 'HOCALAR'), (3, 'İHSANİYE'), (3, 'İSCEHİSAR'), (3, 'KIZILÖREN'), (3, 'MERKEZ'), (3, 'SANDIKLI'), (3, 'SİNANPAŞA'), (3, 'SULTANDAĞI'), (3, 'ŞUHUT'),
-- 04 AĞRI
(4, 'DİYADİN'), (4, 'DOĞUBAYAZIT'), (4, 'ELEŞKİRT'), (4, 'HAMUR'), (4, 'MERKEZ'), (4, 'PATNOS'), (4, 'TAŞLIÇAY'), (4, 'TUTAK'),
-- 05 AMASYA
(5, 'GÖYNÜCEK'), (5, 'GÜMÜŞHACIKÖY'), (5, 'HAMAMÖZÜ'), (5, 'MERKEZ'), (5, 'MERZİFON'), (5, 'SULUOVA'), (5, 'TAŞOVA'),
-- 06 ANKARA
(6, 'AKYURT'), (6, 'ALTINDAĞ'), (6, 'AYAŞ'), (6, 'BALA'), (6, 'BEYPAZARI'), (6, 'ÇAMLIDERE'), (6, 'ÇANKAYA'), (6, 'ÇUBUK'), (6, 'ELMADAĞ'), (6, 'ETİMESGUT'), (6, 'EVREN'), (6, 'GÖLBAŞI'), (6, 'GÜDÜL'), (6, 'HAYMANA'), (6, 'KALECİK'), (6, 'KAZAN'), (6, 'KEÇİÖREN'), (6, 'KIZILCAHAMAM'), (6, 'MAMAK'), (6, 'NALLIHAN'), (6, 'POLATLI'), (6, 'PURSAKLAR'), (6, 'SİNCAN'), (6, 'ŞEREFLİKOÇHİSAR'), (6, 'YENİMAHALLE'),
-- 07 ANTALYA
(7, 'AKSEKİ'), (7, 'AKSU'), (7, 'ALANYA'), (7, 'DEMRE'), (7, 'DÖŞEMEALTI'), (7, 'ELMALI'), (7, 'FİNİKE'), (7, 'GAZİPAŞA'), (7, 'GÜNDOĞMUŞ'), (7, 'İBRADI'), (7, 'KAŞ'), (7, 'KEMER'), (7, 'KEPEZ'), (7, 'KONYAALTI'), (7, 'KORKUTELİ'), (7, 'KUMLUCA'), (7, 'MANAVGAT'), (7, 'MURATPAŞA'), (7, 'SERİK'),
-- 08 ARTVİN
(8, 'ARDANUÇ'), (8, 'ARHAVİ'), (8, 'BORÇKA'), (8, 'HOPA'), (8, 'MERKEZ'), (8, 'MURGUL'), (8, 'ŞAVŞAT'), (8, 'YUSUFELİ'),
-- 09 AYDIN
(9, 'BOZDOĞAN'), (9, 'BUHARKENT'), (9, 'ÇİNE'), (9, 'DİDİM'), (9, 'EFELER'), (9, 'GERMENCİK'), (9, 'İNCİRLİOVA'), (9, 'KARACASU'), (9, 'KARPUZLU'), (9, 'KOÇARLI'), (9, 'KÖŞK'), (9, 'KUŞADASI'), (9, 'KUYUCAK'), (9, 'NAZİLLİ'), (9, 'SÖKE'), (9, 'SULTANHİSAR'), (9, 'YENİPAZAR'),
-- 10 BALIKESİR
(10, 'ALTIEYLÜL'), (10, 'AYVALIK'), (10, 'BALYA'), (10, 'BANDIRMA'), (10, 'BİGADİÇ'), (10, 'BURHANİYE'), (10, 'DURSUNBEY'), (10, 'EDREMİT'), (10, 'ERDEK'), (10, 'GÖMEÇ'), (10, 'GÖNEN'), (10, 'HAVRAN'), (10, 'İVRİNDİ'), (10, 'KARESİ'), (10, 'KEPSUT'), (10, 'MANYAS'), (10, 'MARMARA'), (10, 'SAVAŞTEPE'), (10, 'SINDIRGI'), (10, 'SUSURLUK'),
-- 11 BİLECİK
(11, 'BOZÜYÜK'), (11, 'GÖLPAZARI'), (11, 'İNHİSAR'), (11, 'MERKEZ'), (11, 'OSMANELİ'), (11, 'PAZARYERİ'), (11, 'SÖĞÜT'), (11, 'YENİPAZAR'),
-- 12 BİNGÖL
(12, 'ADAKLI'), (12, 'GENÇ'), (12, 'KARLIOVA'), (12, 'KİĞI'), (12, 'MERKEZ'), (12, 'SOLHAN'), (12, 'YAYLADERE'), (12, 'YEDİSU'),
-- 13 BİTLİS
(13, 'ADİLCEVAZ'), (13, 'AHLAT'), (13, 'GÜROYMAK'), (13, 'HİZAN'), (13, 'MERKEZ'), (13, 'MUTKİ'), (13, 'TATVAN'),
-- 14 BOLU
(14, 'DÖRTDİVAN'), (14, 'GEREDE'), (14, 'GÖYNÜK'), (14, 'KIBRISCIK'), (14, 'MENGEN'), (14, 'MERKEZ'), (14, 'MUDURNU'), (14, 'SEBEN'), (14, 'YENİÇAĞA'),
-- 15 BURDUR
(15, 'AĞLASUN'), (15, 'ALTINYAYLA'), (15, 'BUCAK'), (15, 'ÇAVDIR'), (15, 'ÇELTİKÇİ'), (15, 'GÖLHİSAR'), (15, 'KARAMANLI'), (15, 'KEMER'), (15, 'MERKEZ'), (15, 'TEFENNİ'), (15, 'YEŞİLOVA'),
-- 16 BURSA
(16, 'BÜYÜKORHAN'), (16, 'GEMLİK'), (16, 'GÜRSU'), (16, 'HARMANCIK'), (16, 'İNEGÖL'), (16, 'İZNİK'), (16, 'KARACABEY'), (16, 'KELES'), (16, 'KESTEL'), (16, 'MUDANYA'), (16, 'MUSTAFAKEMALPAŞA'), (16, 'NİLÜFER'), (16, 'ORHANELİ'), (16, 'ORHANGAZİ'), (16, 'OSMANGAZİ'), (16, 'YENİŞEHİR'), (16, 'YILDIRIM'),
-- 17 ÇANAKKALE
(17, 'AYVACIK'), (17, 'BAYRAMİÇ'), (17, 'BİGA'), (17, 'BOZCAADA'), (17, 'ÇAN'), (17, 'ECEABAT'), (17, 'EZİNE'), (17, 'GELİBOLU'), (17, 'GÖKÇEADA'), (17, 'LAPSEKİ'), (17, 'MERKEZ'), (17, 'YENİCE'),
-- 18 ÇANKIRI
(18, 'ATKARACALAR'), (18, 'BAYRAMÖREN'), (18, 'ÇERKEŞ'), (18, 'ELDİVAN'), (18, 'ILGAZ'), (18, 'KIZILIRMAK'), (18, 'KORGUN'), (18, 'KURŞUNLU'), (18, 'MERKEZ'), (18, 'ORTA'), (18, 'ŞABANÖZÜ'), (18, 'YAPRAKLI'),
-- 19 ÇORUM
(19, 'ALACA'), (19, 'BAYAT'), (19, 'BOĞAZKALE'), (19, 'DODURGA'), (19, 'İSKİLİP'), (19, 'KARGI'), (19, 'LAÇİN'), (19, 'MECİTÖZÜ'), (19, 'MERKEZ'), (19, 'OĞUZLAR'), (19, 'ORTAKÖY'), (19, 'OSMANCIK'), (19, 'SUNGURLU'), (19, 'UĞURLUDAĞ'),
-- 20 DENİZLİ
(20, 'ACIPAYAM'), (20, 'BABADAĞ'), (20, 'BAKLAN'), (20, 'BEKİLLİ'), (20, 'BEYAĞAÇ'), (20, 'BOZKURT'), (20, 'BULDAN'), (20, 'ÇAL'), (20, 'ÇAMELİ'), (20, 'ÇARDAK'), (20, 'ÇİVRİL'), (20, 'GÜNEY'), (20, 'HONAZ'), (20, 'KALE'), (20, 'MERKEZEFENDİ'), (20, 'PAMUKKALE'), (20, 'SARAYKÖY'), (20, 'SERİNHİSAR'), (20, 'TAVAS'),
-- 21 DİYARBAKIR
(21, 'BAĞLAR'), (21, 'BİSMİL'), (21, 'ÇERMİK'), (21, 'ÇINAR'), (21, 'ÇÜNGÜŞ'), (21, 'DİCLE'), (21, 'EĞİL'), (21, 'ERGANİ'), (21, 'HANİ'), (21, 'HAZRO'), (21, 'KAYAPINAR'), (21, 'KOCAKÖY'), (21, 'KULP'), (21, 'LİCE'), (21, 'SİLVAN'), (21, 'SUR'), (21, 'YENİŞEHİR'),
-- 22 EDİRNE
(22, 'ENEZ'), (22, 'HAVSA'), (22, 'İPSALA'), (22, 'KEŞAN'), (22, 'LALAPAŞA'), (22, 'MERİÇ'), (22, 'MERKEZ'), (22, 'SÜLOĞLU'), (22, 'UZUNKÖPRÜ'),
-- 23 ELAZIĞ
(23, 'AĞIN'), (23, 'ALACAKAYA'), (23, 'ARICAK'), (23, 'BASKİL'), (23, 'KARAKOÇAN'), (23, 'KEBAN'), (23, 'KOVANCILAR'), (23, 'MADEN'), (23, 'MERKEZ'), (23, 'PALU'), (23, 'SİVRİCE'),
-- 24 ERZİNCAN
(24, 'ÇAYIRLI'), (24, 'İLİÇ'), (24, 'KEMAH'), (24, 'KEMALİYE'), (24, 'MERKEZ'), (24, 'OTLUKBELİ'), (24, 'REFAHİYE'), (24, 'TERCAN'), (24, 'ÜZÜMLÜ'),
-- 25 ERZURUM
(25, 'AŞKALE'), (25, 'AZİZİYE'), (25, 'ÇAT'), (25, 'HINIS'), (25, 'HORASAN'), (25, 'İSPİR'), (25, 'KARAÇOBAN'), (25, 'KARAYAZI'), (25, 'KÖPRÜKÖY'), (25, 'NARMAN'), (25, 'OLTU'), (25, 'OLUR'), (25, 'PALANDÖKEN'), (25, 'PASİNLER'), (25, 'PAZARYOLU'), (25, 'ŞENKAYA'), (25, 'TEKMAN'), (25, 'TORTUM'), (25, 'UZUNDERE'), (25, 'YAKUTİYE'),
-- 26 ESKİŞEHİR
(26, 'ALPU'), (26, 'BEYLİKOVA'), (26, 'ÇİFTELER'), (26, 'GÜNYÜZÜ'), (26, 'HAN'), (26, 'İNÖNÜ'), (26, 'MAHMUDİYE'), (26, 'MİHALGAZİ'), (26, 'MİHALIÇÇIK'), (26, 'ODUNPAZARI'), (26, 'SARICAKAYA'), (26, 'SEYİTGAZİ'), (26, 'SİVRİHİSAR'), (26, 'TEPEBAŞI'),
-- 27 GAZİANTEP
(27, 'ARABAN'), (27, 'İSLAHİYE'), (27, 'KARKAMIŞ'), (27, 'NİZİP'), (27, 'NURDAĞI'), (27, 'OĞUZELİ'), (27, 'ŞAHİNBEY'), (27, 'ŞEHİTKAMİL'), (27, 'YAVUZELİ'),
-- 28 GİRESUN
(28, 'ALUCRA'), (28, 'BULANCAK'), (28, 'ÇAMOLUK'), (28, 'ÇANAKÇI'), (28, 'DERELİ'), (28, 'DOĞANKENT'), (28, 'ESPİYE'), (28, 'EYNESİL'), (28, 'GÖRELE'), (28, 'GÜCE'), (28, 'KEŞAP'), (28, 'MERKEZ'), (28, 'PİRAZİZ'), (28, 'ŞEBİNKARAHİSAR'), (28, 'TİREBOLU'), (28, 'YAĞLIDERE'),
-- 29 GÜMÜŞHANE
(29, 'KELKİT'), (29, 'KÖSE'), (29, 'KÜRTÜN'), (29, 'MERKEZ'), (29, 'ŞİRAN'), (29, 'TORUL'),
-- 30 HAKKARİ
(30, 'ÇUKURCA'), (30, 'DERECİK'), (30, 'MERKEZ'), (30, 'ŞEMDİNLİ'), (30, 'YÜKSEKOVA'),
-- 31 HATAY
(31, 'ALTINÖZÜ'), (31, 'ANTAKYA'), (31, 'ARSUZ'), (31, 'BELEN'), (31, 'DEFNE'), (31, 'DÖRTYOL'), (31, 'ERZİN'), (31, 'HASSA'), (31, 'İSKENDERUN'), (31, 'KIRIKHAN'), (31, 'KUMLU'), (31, 'PAYAS'), (31, 'REYHANLI'), (31, 'SAMANDAĞ'), (31, 'YAYLADAĞI'),
-- 32 ISPARTA
(32, 'AKSU'), (32, 'ATABEY'), (32, 'EĞİRDİR'), (32, 'GELENDOST'), (32, 'GÖNEN'), (32, 'KEÇİBORLU'), (32, 'MERKEZ'), (32, 'SENİRKENT'), (32, 'SÜTÇÜLER'), (32, 'ŞARKİKARAAĞAÇ'), (32, 'ULUBORLU'), (32, 'YALVAÇ'), (32, 'YENİŞARBADEMLİ'),
-- 33 MERSİN
(33, 'AKDENİZ'), (33, 'ANAMUR'), (33, 'AYDINCIK'), (33, 'BOZYAZI'), (33, 'ÇAMLIYAYLA'), (33, 'ERDEMLİ'), (33, 'GÜLNAR'), (33, 'MEZİTLİ'), (33, 'MUT'), (33, 'SİLİFKE'), (33, 'TARSUS'), (33, 'TOROSLAR'), (33, 'YENİŞEHİR'),
-- 34 İSTANBUL
(34, 'ADALAR'), (34, 'ARNAVUTKÖY'), (34, 'ATAŞEHİR'), (34, 'AVCILAR'), (34, 'BAĞCILAR'), (34, 'BAHÇELİEVLER'), (34, 'BAKIRKÖY'), (34, 'BAŞAKŞEHİR'), (34, 'BAYRAMPAŞA'), (34, 'BEŞİKTAŞ'), (34, 'BEYKOZ'), (34, 'BEYLİKDÜZÜ'), (34, 'BEYOĞLU'), (34, 'BÜYÜKÇEKMECE'), (34, 'ÇATALCA'), (34, 'ÇEKMEKÖY'), (34, 'ESENLER'), (34, 'ESENYURT'), (34, 'EYÜPSULTAN'), (34, 'FATİH'), (34, 'GAZİOSMANPAŞA'), (34, 'GÜNGÖREN'), (34, 'KADIKÖY'), (34, 'KAĞITHANE'), (34, 'KARTAL'), (34, 'KÜÇÜKÇEKMECE'), (34, 'MALTEPE'), (34, 'PENDİK'), (34, 'SANCAKTEPE'), (34, 'SARIYER'), (34, 'SİLİVRİ'), (34, 'SULTANBEYLİ'), (34, 'SULTANGAZİ'), (34, 'ŞİLE'), (34, 'ŞİŞLİ'), (34, 'TUZLA'), (34, 'ÜMRANİYE'), (34, 'ÜSKÜDAR'), (34, 'ZEYTİNBURNU'),
-- 35 İZMİR
(35, 'ALİAĞA'), (35, 'BALÇOVA'), (35, 'BAYINDIR'), (35, 'BAYRAKLI'), (35, 'BERGAMA'), (35, 'BEYDAĞ'), (35, 'BORNOVA'), (35, 'BUCA'), (35, 'ÇEŞME'), (35, 'ÇİĞLİ'), (35, 'DİKİLİ'), (35, 'FOÇA'), (35, 'GAZİEMİR'), (35, 'GÜZELBAHÇE'), (35, 'KARABAĞLAR'), (35, 'KARABURUN'), (35, 'KARŞIYAKA'), (35, 'KEMALPAŞA'), (35, 'KINIK'), (35, 'KİRAZ'), (35, 'KONAK'), (35, 'MENDERES'), (35, 'MENEMEN'), (35, 'NARLIDERE'), (35, 'ÖDEMİŞ'), (35, 'SEFERİHİSAR'), (35, 'SELÇUK'), (35, 'TİRE'), (35, 'TORBALI'), (35, 'URLA'),
-- 36 KARS
(36, 'AKYAKA'), (36, 'ARPAÇAY'), (36, 'DİGOR'), (36, 'KAĞIZMAN'), (36, 'MERKEZ'), (36, 'SARIKAMIŞ'), (36, 'SELİM'), (36, 'SUSUZ'),
-- 37 KASTAMONU
(37, 'ABANA'), (37, 'AĞLI'), (37, 'ARAÇ'), (37, 'AZDAVAY'), (37, 'BOZKURT'), (37, 'CİDE'), (37, 'ÇATALZEYTİN'), (37, 'DADAY'), (37, 'DEVREKANİ'), (37, 'DOĞANYURT'), (37, 'HANÖNÜ'), (37, 'İHSANGAZİ'), (37, 'İNEBOLU'), (37, 'KÜRE'), (37, 'MERKEZ'), (37, 'PINARBAŞI'), (37, 'SEYDİLER'), (37, 'ŞENPAZAR'), (37, 'TAŞKÖPRÜ'), (37, 'TOSYA'),
-- 38 KAYSERİ
(38, 'AKKIŞLA'), (38, 'BÜNYAN'), (38, 'DEVELİ'), (38, 'FELAHİYE'), (38, 'HACILAR'), (38, 'İNCESU'), (38, 'KOCASİNAN'), (38, 'MELİKGAZİ'), (38, 'ÖZVATAN'), (38, 'PINARBAŞI'), (38, 'SARIOĞLAN'), (38, 'SARIZ'), (38, 'TALAS'), (38, 'TOMARZA'), (38, 'YAHYALI'), (38, 'YEŞİLHİSAR'),
-- 39 KIRKLARELİ
(39, 'BABAESKİ'), (39, 'DEMİRKÖY'), (39, 'KOFÇAZ'), (39, 'LÜLEBURGAZ'), (39, 'MERKEZ'), (39, 'PEHLİVANKÖY'), (39, 'PINARHİSAR'), (39, 'VİZE'),
-- 40 KIRŞEHİR
(40, 'AKÇAKENT'), (40, 'AKPINAR'), (40, 'BOZTEPE'), (40, 'ÇİÇEKDAĞI'), (40, 'KAMAN'), (40, 'MERKEZ'), (40, 'MUCUR'),
-- 41 KOCAELİ
(41, 'BAŞİSKELE'), (41, 'ÇAYIROVA'), (41, 'DARICA'), (41, 'DERİNCE'), (41, 'DİLOVASI'), (41, 'GEBZE'), (41, 'GÖLCÜK'), (41, 'İZMİT'), (41, 'KANDIRA'), (41, 'KARAMÜRSEL'), (41, 'KARTEPE'), (41, 'KÖRFEZ'),
-- 42 KONYA
(42, 'AHIRLI'), (42, 'AKÖREN'), (42, 'AKŞEHİR'), (42, 'ALTINEKİN'), (42, 'BEYŞEHİR'), (42, 'BOZKIR'), (42, 'CİHANBEYLİ'), (42, 'ÇELTİK'), (42, 'ÇUMRA'), (42, 'DERBENT'), (42, 'DEREBUCAK'), (42, 'DOĞANHİSAR'), (42, 'EMİRGAZİ'), (42, 'EREĞLİ'), (42, 'GÜNEYSINIR'), (42, 'HADİM'), (42, 'HALKAPINAR'), (42, 'HÜYÜK'), (42, 'ILGIN'), (42, 'KADINHANI'), (42, 'KARAPINAR'), (42, 'KARATAY'), (42, 'KULU'), (42, 'MERAM'), (42, 'SARAYÖNÜ'), (42, 'SELÇUKLU'), (42, 'SEYDİŞEHİR'), (42, 'TAŞKENT'), (42, 'TUZLUKÇU'), (42, 'YALIHÜYÜK'), (42, 'YUNAK'),
-- 43 KÜTAHYA
(43, 'ALTINTAŞ'), (43, 'ASLANAPA'), (43, 'ÇAVDARHİSAR'), (43, 'DOMANİÇ'), (43, 'DUMLUPINAR'), (43, 'EMET'), (43, 'GEDİZ'), (43, 'HİSARCIK'), (43, 'MERKEZ'), (43, 'PAZARLAR'), (43, 'SİMAV'), (43, 'ŞAPHANE'), (43, 'TAVŞANLI'),
-- 44 MALATYA
(44, 'AKÇADAĞ'), (44, 'ARAPGİR'), (44, 'ARGUVAN'), (44, 'BATTALGAZİ'), (44, 'DARENDE'), (44, 'DOĞANŞEHİR'), (44, 'DOĞANYOL'), (44, 'HEKİMHAN'), (44, 'KALE'), (44, 'KULUNCAK'), (44, 'PÜTÜRGE'), (44, 'YAZIHAN'), (44, 'YEŞİLYURT'),
-- 45 MANİSA
(45, 'AHMETLİ'), (45, 'AKHİSAR'), (45, 'ALAŞEHİR'), (45, 'DEMİRCİ'), (45, 'GÖLMARMARA'), (45, 'GÖRDES'), (45, 'KIRKAĞAÇ'), (45, 'KÖPRÜBAŞI'), (45, 'KULA'), (45, 'SALİHLİ'), (45, 'SARIGÖL'), (45, 'SARUHANLI'), (45, 'SELENDİ'), (45, 'SOMA'), (45, 'ŞEHZADELER'), (45, 'TURGUTLU'), (45, 'YUNUSEMRE'),
-- 46 KAHRAMANMARAŞ
(46, 'AFŞİN'), (46, 'ANDIRIN'), (46, 'ÇAĞLAYANCERİT'), (46, 'DULKADİROĞLU'), (46, 'EKİNÖZÜ'), (46, 'ELBİSTAN'), (46, 'GÖKSUN'), (46, 'NURHAK'), (46, 'ONİKİŞUBAT'), (46, 'PAZARCIK'), (46, 'TÜRKOĞLU'),
-- 47 MARDİN
(47, 'ARTUKLU'), (47, 'DARGEÇİT'), (47, 'DERİK'), (47, 'KIZILTEPE'), (47, 'MAZIDAĞI'), (47, 'MİDYAT'), (47, 'NUSAYBİN'), (47, 'ÖMERLİ'), (47, 'SAVUR'), (47, 'YEŞİLLİ'),
-- 48 MUĞLA
(48, 'BODRUM'), (48, 'DALAMAN'), (48, 'DATÇA'), (48, 'FETHİYE'), (48, 'KAVAKLIDERE'), (48, 'KÖYCEĞİZ'), (48, 'MARMARİS'), (48, 'MENTEŞE'), (48, 'MİLAS'), (48, 'ORTACA'), (48, 'SEYDİKEMER'), (48, 'ULA'), (48, 'YATAĞAN'),
-- 49 MUŞ
(49, 'BULANIK'), (49, 'HASKÖY'), (49, 'KORKUT'), (49, 'MALAZGİRT'), (49, 'MERKEZ'), (49, 'VARTO'),
-- 50 NEVŞEHİR
(50, 'ACIGÖL'), (50, 'AVANOS'), (50, 'DERİNKUYU'), (50, 'GÜLŞEHİR'), (50, 'HACIBEKTAŞ'), (50, 'KOZAKLI'), (50, 'MERKEZ'), (50, 'ÜRGÜP'),
-- 51 NİĞDE
(51, 'ALTUNHİSAR'), (51, 'BOR'), (51, 'ÇAMARDI'), (51, 'ÇİFTLİK'), (51, 'MERKEZ'), (51, 'ULUKIŞLA'),
-- 52 ORDU
(52, 'AKKUŞ'), (52, 'ALTINORDU'), (52, 'AYBASTI'), (52, 'ÇAMAŞ'), (52, 'ÇATALPINAR'), (52, 'ÇAYBAŞI'), (52, 'FATSA'), (52, 'GÖLKÖY'), (52, 'GÜLYALI'), (52, 'GÜRGENTEPE'), (52, 'İKİZCE'), (52, 'KABADÜZ'), (52, 'KABATAŞ'), (52, 'KORGAN'), (52, 'KUMRU'), (52, 'MESUDİYE'), (52, 'PERŞEMBE'), (52, 'ULUBEY'), (52, 'ÜNYE'),
-- 53 RİZE
(53, 'ARDEŞEN'), (53, 'ÇAMLIHEMŞİN'), (53, 'ÇAYELİ'), (53, 'DEREPAZARI'), (53, 'FINDIKLI'), (53, 'GÜNEYSU'), (53, 'HEMŞİN'), (53, 'İKİZDERE'), (53, 'İYİDERE'), (53, 'KALKANDERE'), (53, 'MERKEZ'), (53, 'PAZAR'),
-- 54 SAKARYA
(54, 'ADAPAZARI'), (54, 'AKYAZI'), (54, 'ARİFİYE'), (54, 'ERENLER'), (54, 'FERİZLİ'), (54, 'GEYVE'), (54, 'HENDEK'), (54, 'KARAPÜRÇEK'), (54, 'KARASU'), (54, 'KAYNARCA'), (54, 'KOCAALİ'), (54, 'PAMUKOVA'), (54, 'SAPANCA'), (54, 'SERDİVAN'), (54, 'SÖĞÜTLÜ'), (54, 'TARAKLI'),
-- 55 SAMSUN
(55, 'ALAÇAM'), (55, 'ASARCIK'), (55, 'ATAKUM'), (55, 'AYVACIK'), (55, 'BAFRA'), (55, 'CANİK'), (55, 'ÇARŞAMBA'), (55, 'HAVZA'), (55, 'İLKADIM'), (55, 'KAVAK'), (55, 'LADİK'), (55, 'ONDOKUZMAYIS'), (55, 'SALIPAZARI'), (55, 'TEKKEKÖY'), (55, 'TERME'), (55, 'VEZİRKÖPRÜ'), (55, 'YAKAKENT'),
-- 56 SİİRT
(56, 'BAYKAN'), (56, 'ERUH'), (56, 'KURTALAN'), (56, 'MERKEZ'), (56, 'PERVARİ'), (56, 'ŞİRVAN'), (56, 'TİLLO'),
-- 57 SİNOP
(57, 'AYANCIK'), (57, 'BOYABAT'), (57, 'DİKMEN'), (57, 'DURAĞAN'), (57, 'ERFELEK'), (57, 'GERZE'), (57, 'MERKEZ'), (57, 'SARAYDÜZÜ'), (57, 'TÜRKELİ'),
-- 58 SİVAS
(58, 'AKINCILAR'), (58, 'ALTINYAYLA'), (58, 'DİVRİĞİ'), (58, 'DOĞANŞAR'), (58, 'GEMEREK'), (58, 'GÖLOVA'), (58, 'GÜRÜN'), (58, 'HAFİK'), (58, 'İMRANLI'), (58, 'KANGAL'), (58, 'KOYULHİSAR'), (58, 'MERKEZ'), (58, 'SUŞEHRİ'), (58, 'ŞARKIŞLA'), (58, 'ULAŞ'), (58, 'YILDIZELİ'), (58, 'ZARA'),
-- 59 TEKİRDAĞ
(59, 'ÇERKEZKÖY'), (59, 'ÇORLU'), (59, 'ERGENE'), (59, 'HAYRABOLU'), (59, 'KAPAKLI'), (59, 'MALKARA'), (59, 'MARMARAEREĞLİSİ'), (59, 'MURATLI'), (59, 'SARAY'), (59, 'SÜLEYMANPAŞA'), (59, 'ŞARKÖY'),
-- 60 TOKAT
(60, 'ALMUS'), (60, 'ARTOVA'), (60, 'BAŞÇİFTLİK'), (60, 'ERBAA'), (60, 'MERKEZ'), (60, 'NİKSAR'), (60, 'PAZAR'), (60, 'REŞADİYE'), (60, 'SULUSARAY'), (60, 'TURHAL'), (60, 'YEŞİLYURT'), (60, 'ZİLE'),
-- 61 TRABZON
(61, 'AKÇAABAT'), (61, 'ARAKLI'), (61, 'ARSİN'), (61, 'BEŞİKDÜZÜ'), (61, 'ÇARŞIBAŞI'), (61, 'ÇAYKARA'), (61, 'DERNEKPAZARI'), (61, 'DÜZKÖY'), (61, 'HAYRAT'), (61, 'KÖPRÜBAŞI'), (61, 'MAÇKA'), (61, 'OF'), (61, 'ORTAHİSAR'), (61, 'SÜRMENE'), (61, 'ŞALPAZARI'), (61, 'TONYA'), (61, 'VAKFIKEBİR'), (61, 'YOMRA'),
-- 62 TUNCELİ
(62, 'ÇEMİŞGEZEK'), (62, 'HOZAT'), (62, 'MAZGİRT'), (62, 'MERKEZ'), (62, 'NAZIMİYE'), (62, 'OVACIK'), (62, 'PERTEK'), (62, 'PÜLÜMÜR'),
-- 63 ŞANLIURFA
(63, 'AKÇAKALE'), (63, 'BİRECİK'), (63, 'BOZOVA'), (63, 'CEYLANPINAR'), (63, 'EYYÜBİYE'), (63, 'HALFETİ'), (63, 'HALİLİYE'), (63, 'HARRAN'), (63, 'HİLVAN'), (63, 'KARAKÖPRÜ'), (63, 'SİVEREK'), (63, 'SURUÇ'), (63, 'VİRANŞEHİR'),
-- 64 UŞAK
(64, 'BANAZ'), (64, 'EŞME'), (64, 'KARAHALLI'), (64, 'MERKEZ'), (64, 'SİVASLI'), (64, 'ULUBEY'),
-- 65 VAN
(65, 'BAHÇESARAY'), (65, 'BAŞKALE'), (65, 'ÇALDIRAN'), (65, 'ÇATAK'), (65, 'EDREMİT'), (65, 'ERCİŞ'), (65, 'GEVAŞ'), (65, 'GÜRPINAR'), (65, 'İPEKYOLU'), (65, 'MURADİYE'), (65, 'ÖZALP'), (65, 'SARAY'), (65, 'TUŞBA'),
-- 66 YOZGAT
(66, 'AKDAĞMADENİ'), (66, 'AYDINCIK'), (66, 'BOĞAZLIYAN'), (66, 'ÇANDIR'), (66, 'ÇAYIRALAN'), (66, 'ÇEKEREK'), (66, 'KADIŞEHRİ'), (66, 'MERKEZ'), (66, 'SARAYKENT'), (66, 'SARIKAYA'), (66, 'SORGUN'), (66, 'ŞEFAATLİ'), (66, 'YENİFAKILI'), (66, 'YERKÖY'),
-- 67 ZONGULDAK
(67, 'ALAPLI'), (67, 'ÇAYCUMA'), (67, 'DEVREK'), (67, 'EREĞLİ'), (67, 'GÖKÇEBEY'), (67, 'KİLİMLİ'), (67, 'KOZLU'), (67, 'MERKEZ'),
-- 68 AKSARAY
(68, 'AĞAÇÖREN'), (68, 'ESKİL'), (68, 'GÜLAĞAÇ'), (68, 'GÜZELYURT'), (68, 'MERKEZ'), (68, 'ORTAKÖY'), (68, 'SARIYAHŞİ'), (68, 'SULTANHANI'),
-- 69 BAYBURT
(69, 'AYDINTEPE'), (69, 'DEMİRÖZÜ'), (69, 'MERKEZ'),
-- 70 KARAMAN
(70, 'AYRANCI'), (70, 'BAŞYAYLA'), (70, 'ERMENEK'), (70, 'KAZIMKARABEKİR'), (70, 'MERKEZ'), (70, 'SARIVELİLER'),
-- 71 KIRIKKALE
(71, 'BAHŞILI'), (71, 'BALIŞEYH'), (71, 'ÇELEBİ'), (71, 'DELİCE'), (71, 'KARAKEÇİLİ'), (71, 'KESKİN'), (71, 'MERKEZ'), (71, 'SULAKYURT'), (71, 'YAHŞİHAN'),
-- 72 BATMAN
(72, 'BEŞİRİ'), (72, 'GERCÜŞ'), (72, 'HASANKEYF'), (72, 'KOZLUK'), (72, 'MERKEZ'), (72, 'SASON'),
-- 73 ŞIRNAK
(73, 'BEYTÜŞŞEBAP'), (73, 'CİZRE'), (73, 'GÜÇLÜKONAK'), (73, 'İDİL'), (73, 'MERKEZ'), (73, 'SİLOPİ'), (73, 'ULUDERE'),
-- 74 BARTIN
(74, 'AMASRA'), (74, 'KURUCAŞİLE'), (74, 'MERKEZ'), (74, 'ULUS'),
-- 75 ARDAHAN
(75, 'ÇILDIR'), (75, 'DAMAL'), (75, 'GÖLE'), (75, 'HANAK'), (75, 'MERKEZ'), (75, 'POSOF'),
-- 76 IĞDIR
(76, 'ARALIK'), (76, 'KARAKOYUNLU'), (76, 'MERKEZ'), (76, 'TUZLUCA'),
-- 77 YALOVA
(77, 'ALTINOVA'), (77, 'ARMUTLU'), (77, 'ÇINARCIK'), (77, 'ÇİFTLİKKÖY'), (77, 'MERKEZ'), (77, 'TERMAL'),
-- 78 KARABÜK
(78, 'EFLANİ'), (78, 'ESKİPAZAR'), (78, 'MERKEZ'), (78, 'OVACIK'), (78, 'SAFRANBOLU'), (78, 'YENİCE'),
-- 79 KİLİS
(79, 'ELBEYLİ'), (79, 'MERKEZ'), (79, 'MUSABEYLİ'), (79, 'POLATELİ'),
-- 80 OSMANİYE
(80, 'BAHÇE'), (80, 'DÜZİÇİ'), (80, 'HASANBEYLİ'), (80, 'KADİRLİ'), (80, 'MERKEZ'), (80, 'SUMBAS'), (80, 'TOPRAKKALE'),
-- 81 DÜZCE
(81, 'AKÇAKOCA'), (81, 'CUMAYERİ'), (81, 'ÇİLİMLİ'), (81, 'GÖLYAKA'), (81, 'GÜMÜŞOVA'), (81, 'KAYNAŞLI'), (81, 'MERKEZ'), (81, 'YIĞILCA');

DO $$
DECLARE
    dist_rec RECORD;
    new_neigh_id INT;
    n_name TEXT;
    s_name TEXT;
    rand_idx INT;
    
    -- 200 neighborhood names
    neigh_pool TEXT[] := ARRAY[
        'CUMHURİYET', 'ATATÜRK', 'FATİH', 'YENİ', 'MERKEZ', 'BAHÇELİEVLER', 'HÜRRİYET', 'İSTİKLAL', 'ZAFER', 'GAZİ',
        'ESENTEPE', 'BARBAROS', 'MİMAR SİNAN', 'YAVUZ SELİM', 'GÜLTEPE', 'ŞİRİNEVLER', 'AYDINLIKEVLER', 'YILDIZ', 'ALTINŞEHİR', 'GÜZELYALI',
        'FEVZİ ÇAKMAK', 'KAZIM KARABEKİR', 'MEVLANA', 'YUNUS EMRE', 'AKŞEMSETTİN', 'NAMIK KEMAL', 'MEHMET AKİF ERSOY', 'SULTANBEYLİ', 'YEŞİLYURT', 'PINARBAŞI',
        'ULUS', 'ESENYURT', 'KARŞIYAKA', 'BOSTANCI', 'ERENKÖY', 'GÖZTEPE', 'ÇANKAYA', 'KOCATEPE', 'BEŞTEPE', 'EMEK',
        'SEYRANBAĞLARI', 'KURTULUŞ', '19 MAYIS', '23 NİSAN', '30 AĞUSTOS', 'İNÖNÜ', 'ADNAN MENDERES', 'TURGUT ÖZAL', 'ERTUĞRUL GAZİ', 'OSMANGAZİ',
        'ORHANGAZİ', 'NİLÜFER', 'YILDIRIM', 'SELÇUKLU', 'MERAM', 'KARATAY', 'TEPEBAŞI', 'ODUNPAZARI', 'MURATPAŞA', 'KEPEZ',
        'KONYAALTI', 'ALSANCAK', 'KONAK', 'BUCA', 'BORNOVA', 'KARABAĞLAR', 'BAYRAKLI', 'ÇİĞLİ', 'MAVİŞEHİR', 'ATAŞEHİR',
        'ÜMRANİYE', 'ÇEKMEKÖY', 'SANCAKTEPE', 'MALTEPE', 'KARTAL', 'PENDİK', 'TUZLA', 'BEYKOZ', 'SARIYER', 'BEŞİKTAŞ',
        'ŞİŞLİ', 'BEYOĞLU', 'ZEYTİNBURNU', 'BAKIRKÖY', 'BAHÇEŞEHİR', 'BAŞAKŞEHİR', 'HALKALI', 'İKİTELLİ', 'GÜNEŞLİ', 'AKINCILAR',
        'KOCASİNAN', 'TALAS', 'MELİKGAZİ', 'BATTALGAZİ', 'HALİLİYE', 'EYYÜBİYE', 'KARAKÖPRÜ', 'ŞAHİNBEY', 'ŞEHİTKAMİL', 'KARŞIYAKA',
        'DEMETEVLER', 'ŞENTEPE', 'DİKMEN', 'ETLİK', 'BATIKENT', 'ERYAMAN', 'BAĞLICA', 'ÇAYYOLU', 'ÜMİTKÖY', 'İNCEK',
        'BEYSUKENT', 'BİLKENT', 'ORAN', 'GAZİOSMANPAŞA', 'AYRANCI', 'MALTEPE', 'ANITTEPE', 'TANDOĞAN', 'BEŞEVLER', 'SÖĞÜTÖZÜ',
        'ÇUKURAMBAR', 'BALGAT', 'CEBECİ', 'ABİDİNPAŞA', 'AKDERE', 'TUZLUÇAYIR', 'MAMAK', 'KAYAŞ', 'SİTELER', 'HASKÖY',
        'SUBAYEVLERİ', 'KALABA', 'UFUKTEPE', 'OVACIK', 'YAKACIK', 'BAĞLUM', 'PURSAKLAR', 'SARAY', 'KAZAN', 'TEMELLİ',
        'POLATLI', 'GÖLBAŞI', 'HAYMANA', 'BALA', 'ELMADAĞ', 'AKYURT', 'ÇUBUK', 'KALECİK', 'KIZILCAHAMAM', 'GÜMÜŞPALA',
        'SOĞUKSU', 'CENNET', 'KANARYA', 'SEFAKÖY', 'FLORYA', 'YEŞİLKÖY', 'ATAKÖY', 'MERTER', 'GÜNGÖREN', 'HAZNEDAR',
        'BAĞCILAR', 'ESENLER', 'BAYRAMPAŞA', 'SULTANGAZİ', 'ARNAVUTKÖY', 'HADIMKÖY', 'BÜYÜKÇEKMECE', 'MİMAROBA', 'SİNANOBA', 'KUMBURGAZ',
        'CELALİYE', 'KAMİLHOBA', 'SİLİVRİ', 'SELİMPAŞA', 'ÇANTAKÖY', 'GÜMÜŞYAKA', 'ÇATALCA', 'BİNKILIÇ', 'KARACAKÖY', 'VİZE',
        'PINARHİSAR', 'BABAESKİ', 'LÜLEBURGAZ', 'MURATLI', 'ÇORLU', 'ÇERKEZKÖY', 'KAPAKLI', 'MALKARA', 'KEŞAN', 'İPSALA'
    ];
    
    -- 200 street names
    street_pool TEXT[] := ARRAY[
        'KARANFİL', 'LALE', 'GÜL', 'PAPATYA', 'MENEKŞE', 'SÜMBÜL', 'ÇİĞDEM', 'AKASYA', 'ZAMBAK', 'YASEMİN', 
        'NERGİS', 'BEGONYA', 'ORKİDE', 'MANOLYA', 'AÇELYA', 'KAMELYA', 'NİLÜFER', 'LEYLAK', 'MİMOZA', 'SARDUNYA',
        'ÇINAR', 'MEŞE', 'SÖĞÜT', 'KAVAK', 'SELVİ', 'ARDIÇ', 'LADİN', 'SEDİR', 'ÇAM', 'IHLAMUR', 
        'KESTANE', 'CEVİZ', 'BADEM', 'ZEYTİN', 'DEFNE', 'PALMİYE', 'GÜRGEN', 'KAYIN', 'PALAMUT', 'KÖKNAR',
        'BÜLBÜL', 'KANARYA', 'GÜVERCİN', 'KARTAL', 'ŞAHİN', 'ATMACA', 'KUĞULU', 'TURNA', 'KIRLANGIÇ', 'SERÇE', 
        'KUMRU', 'SAKA', 'KEKLİK', 'MARTI', 'PELİKAN', 'LEYLEK', 'ANKA', 'ŞAHAN', 'DOĞAN', 'PUHU',
        'KİRAZ', 'VİŞNE', 'ERİK', 'ELMA', 'ARMUT', 'AYVA', 'NAR', 'İNCİR', 'ÜZÜM', 'ÇİLEK', 
        'BÖĞÜRTLEN', 'DUT', 'TURUNÇ', 'LİMON', 'PORTAKAL', 'MANDALİNA', 'GREYFURT', 'KAVUN', 'KARPUZ', 'FISTIK',
        'BARIŞ', 'SEVGİ', 'UMUT', 'DOSTLUK', 'KARDEŞLİK', 'BİRLİK', 'HUZUR', 'NEŞE', 'ŞENLİK', 'GÜNEŞ',
        'AY', 'YILDIZ', 'BULUT', 'YAĞMUR', 'RÜZGAR', 'DENİZ', 'IRMAK', 'DERE', 'PINAR', 'ÇEŞME',
        'YAKUT', 'ZÜMRÜT', 'ELMAS', 'ALTIN', 'GÜMÜŞ', 'BAKIR', 'DEMİR', 'ÇELİK', 'MERCAN', 'İNCİ',
        'SEDEF', 'KEHRİBAR', 'OLTU', 'LÜLE', 'PIRLANTA', 'SAFİR', 'TOPAZ', 'OPAL', 'TURKUAZ', 'AMETİST',
        'VADİ', 'TEPE', 'BAYIR', 'YAMAÇ', 'SIRT', 'DÜZLÜK', 'OVA', 'KIR', 'ÇAYIR', 'MERA',
        'SAHİL', 'KUMSAL', 'YALI', 'KÖRFEZ', 'LİMAN', 'İSKELE', 'BOĞAZ', 'ADA', 'YARIMADA', 'BURUN',
        'SABAH', 'ÖĞLE', 'AKŞAM', 'GECE', 'ŞAFAK', 'TAN', 'GÜN', 'YIL', 'ASIR', 'ÇAĞ',
        'MAVİ', 'YEŞİL', 'SARI', 'KIRMIZI', 'MOR', 'PEMBE', 'TURUNCU', 'LACİVERT', 'BEYAZ', 'SİYAH',
        'POYRAZ', 'LODOS', 'MELTEM', 'SAMYELİ', 'KARAYEL', 'GÜNDOĞDU', 'BATI', 'KUZEY', 'GÜNEY', 'DOĞU',
        'KAHRAMAN', 'YİĞİT', 'EFE', 'ZEYBEK', 'DADAŞ', 'SEYMEN', 'ÇAKABEY', 'ALPARSLAN', 'KANUNİ', 'FATİH'
    ];
    
    -- Dynamic sizes
    n_size INT := array_length(neigh_pool, 1);
    s_size INT := array_length(street_pool, 1);

BEGIN
    -- For each district
    FOR dist_rec IN SELECT districtid FROM district LOOP
        
        -- Add random 5 neighborhood to each district
        FOR i IN 1..5 LOOP
            -- Choose neighborhoods randomly
            rand_idx := floor(random() * n_size + 1)::INT;
            n_name := neigh_pool[rand_idx]; 
            
            -- Insert the neighborhood
            INSERT INTO neighborhood (neighborhoodname, districtID) 
            VALUES (n_name, dist_rec.districtid)
            RETURNING neighborhoodID INTO new_neigh_id;
            
            -- Add random 3 streets to each neighborhood
            FOR j IN 1..3 LOOP
                rand_idx := floor(random() * s_size + 1)::INT;
                s_name := street_pool[rand_idx]; 
                
                -- Insert the street
                INSERT INTO street (streetname, neighborhoodID)
                VALUES (s_name, new_neigh_id);
            END LOOP;
            
        END LOOP;
        
    END LOOP;
    
    RAISE NOTICE 'İşlem Tamamlandı! % farklı mahalle ve % farklı sokak ismi havuzundan dağıtıldı.', n_size, s_size;
END $$;

-- Inserting values into address table
INSERT INTO address (streetID, buildingno, floorno, doorno)
VALUES
	(23, 12, 7, 24),
	(23, 6, 6, 23),
	(23, 54, 5, 22),
	(54, 56, 4, 21),
	(54, 2, 3, 20),
	(54, 3, 2, 19),
	(67, 12, 1, 18),
	(67, 5, 0, 17),
	(67, 12, 1, 16),
	(67, 7, 2, 15),
	(101, 11, 3, 14),
	(199, 32, 4, 13),
	(31, 45, 5, 12),
	(54, 65, 6, 11),
	(67, 12, 7, 10),
	(67, 11, 8, 9),
	(67, 10, 9, 8),
	(67, 9, 8, 7),
	(67, 8, 7, 6),
	(67, 7, 6, 5);

-- Inserting 10 values into neighbor table
INSERT INTO neighbor (
    fname,
	mname,
    lname, 
    tckn, 
    bdate, 
    email, 
    contactnumber, 
    addressID, 
    sex
) 
VALUES 
    ('Ahmet', 'Refik', 'Sener', '84630673916', '1985-04-23', 'refik.sener@std.yildiz.edu.tr', '+905551112233', 1, 'M'),
    ('Muhammet', 'Masum', 'Kocaman', '65936281965', '1990-06-15', 'masum.kocaman@std.yildiz.edu.tr', '+905552223344', 2, 'M'),
    ('Emre', 'Talha', 'Çiper', '17594730018', '1978-11-02', 'emre.ciper@std.yildiz.edu.tr', '+905553334455', 3, 'M'),
    ('Fitnat', NULL, 'Korkmaz', '29765946391', '1995-01-20', 'f_korkmazz@gmail.com', '+905554445566', 4, 'F'),
    ('Ramiz', NULL, 'Karaeski', '11976837501', '1982-08-30', 'reski@outlook.com', '+905555556677', 5, 'M'),
    ('Bihter', NULL, 'Ziyagil', '33869612365', '1988-03-12', 'zveb@outlook.com', '+905556667788', 6, 'F'),
    ('İsmail', NULL, 'Kartal', '99564912755', '1992-09-09', 'beyazkazak@gmail.com', '+905557778899', 7, 'M'),
    ('Debra', NULL, 'Morgan', '10056266691', '1999-12-05', 'd_morgan@hotmail.com', '+15558889900', 8, 'F'),
    ('Dilber', NULL, 'Koçarslanlı', '83765838812', '1986-07-18', 'dilberkocarslanli@gmail.com', '+905559990011', 9, 'F'),
    ('Esra', NULL, 'Bulut', '19503759274', '1993-05-25', 'esrabblt@gmail.com', '+905550001122', 10, 'F');

-- Inserting 10 values into neighbor_passwords table (Used bcrypt with rounds=15)
INSERT INTO neighbor_passwords ( 
    nID, 
    passwordhash
) 
VALUES 
    (6, '$2a$15$MbYHuz/.jCBCmhmEN73WRuH3Fr2iBQCiM.kSY9y5XFyhzFK9AA6Nq'),
    (3, '$2a$15$s7ZfkYzJODE3tImEYNqpeefxnA1wk4Dh58x6aZiplqZAiv.P22E9W'),
    (1, '$2a$15$yp0CV4mlamtq.DRUTizjteDc/eBtSXplT5.DyMbN22ouKCxmlDAKy'),
    (7, '$2a$15$mmosUy0A7u5XYrPvh5io.eScIqA5hgKN6qtSGFra63aPaEgJwXozK'),
    (2, '$2a$15$HzMD7LNAm38APPq99cI7gOPxo8tpjs5gBqGAbgiX0C/L29AsYukMC'),
    (4, '$2a$15$0UxVKZq4uTf207zHVL.IEOH5I8rUhejuFVWma3XhhQtr9ZlCZvL5W'),
    (9, '$2a$15$0Z2U3H.o/Ulb24NrHcGmg.oKi/jc75.BVRndnk19rNrRx.QQn9/.K'),
    (8, '$2a$15$fs4HtPKQ8ggW.5in7ei4s.Qw94fNM5ewI0SVLsLqFoXa766SbpCS2'),
    (10,'$2a$15$KxPiHjNrwp4Gt/4iuXdRY.6g80uTAE2.05LerfTgqI4pgkM4WzI4a'),
    (5, '$2a$15$kWJWIStvdY5DOvTc.TYsMeM3spXqmHU1YZT6BhipixFpfUJ86m8zq');

-- Inserting 10 values into disposal table
INSERT INTO disposal ( 
	disposalname,
    tcostcoef, 
    scorecoef
) 
VALUES 
    ('Paper', 1.0, 1.1),
    ('Plastic', 1.2, 2.1),
    ('Glass', 1.1, 1.0),
    ('Metal', 2.3, 1.4),
    ('Battery', 3.2, 2.4),
    ('Organic', 1.0, 0.7),
    ('Textile', 1.1, 1.6),
    ('Medical', 5.0, 1.9),
    ('Electronic', 2.9, 1.6),
    ('Wood', 1.6, 1.2);

-- Inserting into discarded_disposal table (kg for weight, m^3 for volume)
-- tcost = tcostcoef x max(weight, volume x 10)
-- ddscore = scorecoef x (weight + volume x 10)
INSERT INTO discarded_disposal ( 
	dID,
	neighbortckn,
	ddate,
	weight,
	volume,
	rstatus,
	ddscore,
	tcost
) 
VALUES 
    (1, '84630673916', '2025-11-01', 3.24, 0.21, FALSE, 5.784, 3.24),
    (2, '19503759274', '2025-11-20', 1.67, 0.50, FALSE, 14.007, 6.00),
    (3, '65936281965', '2025-11-15', 6.70, 1.90, FALSE, 25.7, 20.9),
    (4, '83765838812', '2025-11-04', 3.00, 0.14, FALSE, 6.16, 6.90),
    (5, '17594730018', '2025-11-06', 5.76, 0.87, FALSE, 34.704, 27.84),
    (6, '10056266691', '2025-11-09', 3.21, 0.65, FALSE, 6.797, 6.50),
    (7, '29765946391', '2025-11-15', 1.01, 1.10, FALSE, 19.216, 12.1),
    (8, '99564912755', '2025-11-06', 0.97, 0.43, FALSE, 10.013, 21.5),
    (9, '11976837501', '2025-11-17', 4.45, 1.32, FALSE, 28.24, 38.28),
    (10,'33869612365', '2025-11-02', 2.22, 0.79, FALSE, 12.144, 12.64),
	(1, '33869612365', '2025-11-02', 2.50, 0.10, FALSE, 3.85, 2.50),
    (2, '11976837501', '2025-11-02', 1.20, 0.50, FALSE, 13.02, 6.00),
    (3, '99564912755', '2025-11-03', 5.00, 0.20, FALSE, 7.00, 5.50),
    (4, '29765946391', '2025-11-04', 10.0, 0.30, FALSE, 18.20, 23.00),
    (5, '10056266691', '2025-11-05', 0.50, 0.01, FALSE, 1.44, 1.60),
    (6, '17594730018', '2025-11-05', 4.00, 0.60, FALSE, 7.00, 6.00),
    (7, '83765838812', '2025-11-06', 3.00, 0.40, FALSE, 11.20, 4.40),
    (8, '65936281965', '2025-11-07', 1.50, 0.10, TRUE, 4.75, 7.50), -- t, cid:8, ddno:18
    (9, '19503759274', '2025-11-08', 2.00, 0.15, FALSE, 5.60, 5.80),
    (10,'84630673916', '2025-11-09', 8.00, 0.50, FALSE, 15.60, 12.80),
	(1, '29765946391', '2025-11-10', 4.10, 0.35, FALSE, 8.36, 4.10),
    (1, '10056266691', '2025-11-12', 2.00, 0.50, TRUE, 7.70, 5.00), -- t, cid:2, ddno:22
    (1, '33869612365', '2025-11-25', 15.20, 1.20, FALSE, 29.92, 15.20),
    (2, '83765838812', '2025-11-11', 0.50, 0.80, FALSE, 17.85, 9.60),
    (2, '99564912755', '2025-11-14', 2.30, 0.40, FALSE, 13.23, 4.80),
    (2, '17594730018', '2025-11-28', 1.10, 1.50, FALSE, 33.81, 18.00),
    (2, '10056266691', '2025-11-30', 0.80, 1.20, TRUE, 26.88, 14.40), -- t, cid:4, ddno:27
    (3, '65936281965', '2025-11-13', 8.40, 0.15, FALSE, 9.90, 9.24),
    (3, '11976837501', '2025-11-18', 3.60, 0.10, FALSE, 4.60, 3.96),
    (3, '84630673916', '2025-11-22', 12.00, 0.40, FALSE, 16.00, 13.20),
    (4, '19503759274', '2025-11-15', 25.00, 0.20, FALSE, 37.80, 57.50),
    (4, '29765946391', '2025-11-19', 5.50, 0.10, FALSE, 9.10, 12.65),
    (4, '10056266691', '2025-11-29', 18.30, 0.50, FALSE, 32.62, 42.09),
    (5, '99564912755', '2025-11-20', 0.20, 0.01, FALSE, 0.72, 0.64),
    (5, '33869612365', '2025-11-26', 1.50, 0.05, FALSE, 4.80, 4.80),
    (6, '17594730018', '2025-11-05', 6.70, 0.90, TRUE, 10.99, 9.00), -- t, cid:7, ddno:36
    (6, '83765838812', '2025-11-08', 2.20, 0.30, FALSE, 3.64, 3.00),
    (6, '65936281965', '2025-11-21', 10.50, 1.20, FALSE, 15.75, 12.00),
    (7, '11976837501', '2025-11-16', 14.00, 2.00, FALSE, 54.40, 22.00),
    (7, '84630673916', '2025-11-23', 5.80, 0.60, FALSE, 18.88, 6.60),
    (7, '19503759274', '2025-11-27', 2.40, 0.30, FALSE, 8.64, 3.30),
    (8, '29765946391', '2025-11-11', 0.60, 0.10, TRUE, 3.04, 5.00), -- t, cid:7, ddno:42
    (8, '10056266691', '2025-11-24', 3.10, 0.25, FALSE, 10.64, 15.50),
    (9, '99564912755', '2025-11-09', 5.40, 0.40, FALSE, 15.04, 15.66),
    (9, '33869612365', '2025-11-15', 12.10, 0.80, TRUE, 32.16, 35.09), -- t, cid:10, ddno:45
    (9, '17594730018', '2025-11-28', 0.90, 0.10, FALSE, 3.04, 2.90),
    (10, '83765838812', '2025-11-14', 25.00, 1.50, TRUE, 48.00, 40.00), -- t, cid:5, ddno:47
    (10, '65936281965', '2025-11-18', 7.70, 0.60, FALSE, 16.44, 12.32),
    (10, '11976837501', '2025-11-25', 14.30, 0.90, FALSE, 27.96, 22.88),
    (10, '84630673916', '2025-11-29', 30.00, 2.00, TRUE, 60.00, 48.00); -- t, cid:3, ddno:50

-- Inserting 10 values into company table
INSERT INTO company ( 
	cname,
	taxnumber,
	contactnumber,
	faxnumber,
	addressID,
	governmentservice
) 
VALUES 
    ('İstanbul Büyükşehir Belediyesi', '8347196047', '+908503125676', '212-693-2377', 11, TRUE),
    ('Fatih Belediyesi', '2347542347', '+908503425678', '212-633-2568', 12, TRUE),
    ('Kadıköy Belediyesi', '1955382648', '+908503527688', '212-657-8735', 13, TRUE),
    ('Göve Plastik', '5493628125', '+902125064366', '212-235-4356', 14, FALSE),
    ('YNC Geri Dönüşüm', '8356472839', '+908502346541', '212-633-1522', 15, FALSE),
    ('Hurdacı Coşar Geri Dönüşüm', '1749372047', '+902123468777', '212-856-2674', 16, FALSE),
    ('Alan Medikal Dönüşüm', '5672830456', '+908504562233', '212-123-2332', 17, FALSE),
    ('Babadan Oğula Tekstil', '2956473811', '+908505486456', '212-184-9365', 18, FALSE),
    ('Özbaşollar Geri Dönüşüm', '9501839421', '+902123451264', '212-631-2343', 19, FALSE),
    ('Soysal Elektronik', '4879261732', '+908503451276', '212-623-1565', 20, FALSE);

-- Inserting 10 values into company_passwords table (Used bcrypt with rounds=15)
INSERT INTO company_passwords ( 
	cID,
	passwordhash
) 
VALUES 
    (5, '$2a$15$q7HmoHwzpfx/RS7lfPg7ju8yStncLHPryn6KLLeR7CSFESMuzsxTu'),
    (10,'$2a$15$nguC3v7ssiUJpIwRaW73Te6BCR7q5T3DWgHsvKfxXn.h9/gxhQUOe'),
    (7, '$2a$15$H7fihU2QUdocwrVom5mdeOf8mGneGRvF5SInOA.nR1wjafpoiehOa'),
    (4, '$2a$15$ujO.qj2Hd1rwP.P.WN4QG.nki/2EKx/1tUoT/CTV4PdNF4JVJvtBK'),
    (2, '$2a$15$EVZDUiVbqCZFWRhK.ykPhO7a/PObKLMAopkTDfnOK0.Ui4du/E/ha'),
    (1, '$2a$15$puLbErsVQDzu.TrK4ArCLOe2ocSZkaIXS95sTYftouEXJtROTY2ae'),
    (9, '$2a$15$C28Ejwwh.cBrwi3QkAE52eLqTvd3IJ2Ash3ezUBbdXzzhj8.KQAn2'),
    (8, '$2a$15$PC0VUTb3lWZYtFbz.UvvuugBE7sKbbSsDfxgqLQOUeARpS6JA6IaG'),
    (3, '$2a$15$YF1g9HgusKNIKzXrhLGSGOkE/wHiU1yqYemfWTFA7W8ezMSzZHNeu'),
    (6, '$2a$15$.jh.5f9wvYUw6tFccIoLKevsbvaBZGzjYRwR5AqlLkpS0vOH27k9y');

-- Inserting at least 10 values into company_disposal table
INSERT INTO company_disposal ( 
	cID,
	dID
) 
VALUES 
    (1, 1),
	(1, 2),
	(1, 3),
	(1, 4),
	(1, 5),
	(1, 6),
	(1, 7),
	(1, 8),
	(1, 9),
	(1, 10),
	(2, 1),
	(2, 2),
	(2, 3),
	(2, 4),
	(2, 5),
	(2, 6),
	(2, 7),
	(2, 8),
	(2, 9),
	(2, 10),
	(3, 1),
	(3, 2),
	(3, 3),
	(3, 4),
	(3, 5),
	(3, 6),
	(3, 7),
	(3, 8),
	(3, 9),
	(3, 10),
	(4, 1),
	(4, 2),
	(4, 9),
	(5, 1),
	(5, 7),
	(5, 10),
	(6, 2),
	(6, 4),
	(7, 6),
	(7, 8),
	(8, 2),
	(8, 7),
	(8, 8),
	(9, 3),
	(9, 5),
	(9, 9),
	(10, 4),
	(10, 5),
	(10, 9),
	(10, 10);

-- Inserting values into reservation table
INSERT INTO reservation ( 
	cID,
	reservationdate,
	recycledate
) 
VALUES 
    (1, '2025-12-06', NULL),
    (2, '2025-12-01', NULL),
    (3, '2025-11-27', NULL),
    (4, '2025-11-29', NULL),
    (5, '2025-12-01', NULL),
    (6, '2025-12-02', NULL),
    (7, '2025-12-03', NULL),
    (8, '2025-12-04', NULL),
    (9, '2025-12-05', NULL),
    (10, '2025-12-06', NULL),
	(8, '2026-01-01', '2026-01-02'),
	(2, '2026-01-02', '2026-01-03'),
	(4, '2026-01-03', '2026-01-04'),
	(7, '2026-01-04', '2026-01-05'),
	(7, '2026-01-05', '2026-01-05'),
	(10, '2026-01-05', '2026-01-05'),
	(5, '2026-01-05', '2026-01-05'),
	(3, '2026-01-05', '2026-01-05');

-- Inserting values into reservation_disposal table
INSERT INTO reservation_disposal ( 
	rnumber,
	ddnumber
) 
VALUES 
    (1, 1),
    (2, 2),
    (3, 3),
    (6, 4),
    (9, 5),
    (7, 6),
    (5, 7),
    (8, 8),
    (4, 9),
    (10, 10),
	(11, 18),
	(12, 22),
	(13, 27),
	(14, 36),
	(15, 42),
	(16, 45),
	(17, 47),
	(18, 50);


-- Runs when a record is deleted or updated in either the neighbor or company tables
CREATE OR REPLACE FUNCTION delete_orphan_address()
RETURNS TRIGGER AS $$
BEGIN
	RAISE NOTICE 'delete_orphan_address tetiklendi!';

    -- If the operation is UPDATE and the address has not changed, skip
    IF (TG_OP = 'UPDATE' AND OLD.addressID = NEW.addressID) THEN
        RETURN NULL;
    END IF;

    IF OLD.addressID IS NOT NULL THEN
        -- Is there another neighbor record using this old address?
        IF NOT EXISTS (SELECT 1 FROM neighbor WHERE addressID = OLD.addressID) 
           AND 
           -- Is there another company record using this old address?
           NOT EXISTS (SELECT 1 FROM company WHERE addressID = OLD.addressID) 
        THEN
            -- If no one uses this address, delete it
            DELETE FROM address WHERE addressid = OLD.addressID;
            RAISE NOTICE '% işlemi sonucu yetim kalan % numaralı adres silindi.', TG_OP, OLD.addressID;
        END IF;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_cleanup_address_neighbor ON neighbor;
CREATE TRIGGER trg_cleanup_address_neighbor
AFTER DELETE OR UPDATE OF addressID
ON neighbor
FOR EACH ROW
EXECUTE FUNCTION delete_orphan_address();

DROP TRIGGER IF EXISTS trg_cleanup_address_company ON company;
CREATE TRIGGER trg_cleanup_address_company
AFTER DELETE OR UPDATE OF addressID
ON company
FOR EACH ROW
EXECUTE FUNCTION delete_orphan_address();

-- Runs when the rstatus value of a discarded disposal is set to TRUE
CREATE OR REPLACE FUNCTION update_recycle_date_on_status_change()
RETURNS TRIGGER AS $$
DECLARE
    v_rnumber INT;
BEGIN
	RAISE NOTICE 'update_recycle_date_on_status_change tetiklendi!';
	
    -- If rstatus became TRUE
    IF NEW.rstatus = TRUE AND OLD.rstatus = FALSE THEN
    
        -- Find reservation number of that discarded disposal
        SELECT rnumber INTO v_rnumber
        FROM reservation_disposal rd
        WHERE rd.ddnumber = NEW.ddno
        LIMIT 1;

        -- If there is a reservation, set it's recycledate as today's date
        IF v_rnumber IS NOT NULL THEN
            UPDATE reservation
            SET recycledate = CURRENT_DATE
            WHERE reservationNo = v_rnumber;

            RAISE NOTICE 'Rezervasyon tablosundaki % numaralı rezervasyonun geri dönüşüm tarihi % olarak ayarlandı.', v_rnumber, CURRENT_DATE; 
        END IF;
        
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_set_recycle_date
AFTER UPDATE OF rstatus ON discarded_disposal
FOR EACH ROW
EXECUTE FUNCTION update_recycle_date_on_status_change();

CREATE OR REPLACE FUNCTION get_neighbor_profile(ntckn CHAR(11))
RETURNS TABLE (
	neighborid INT,
	fname VARCHAR(30),
	mname VARCHAR(30),
	lname VARCHAR(30),
	tckn CHAR(11),
	bdate DATE,
	email VARCHAR(70),
	contactnumber VARCHAR(16),
	addressID INT,
	sex CHAR,
	tot_w NUMERIC,
	tot_v NUMERIC,
	rec_w NUMERIC,
	rec_v NUMERIC,
	score NUMERIC
) AS $$
BEGIN
	RETURN QUERY
	SELECT
		n.neighborid, n.fname, n.mname, n.lname, n.tckn, n.bdate, n.email, n.contactnumber, n.addressID, n.sex,
		COALESCE(SUM(dd.weight), 0) as tot_w,
		COALESCE(SUM(dd.volume), 0) as tot_v,
		COALESCE(SUM(CASE WHEN dd.rstatus = TRUE THEN dd.weight ELSE 0 END), 0) as rec_w,
		COALESCE(SUM(CASE WHEN dd.rstatus = TRUE THEN dd.volume ELSE 0 END), 0) as rec_v,
		COALESCE(SUM(CASE WHEN dd.rstatus = TRUE THEN dd.ddscore ELSE 0 END), 0) as score
	FROM neighbor n
	LEFT JOIN discarded_disposal dd ON n.tckn = dd.neighbortckn
	WHERE n.tckn = ntckn
	GROUP BY n.tckn;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION get_current_discarded_disposals(limitcount INT)
RETURNS TABLE (
	ddno INT,
	disposalname VARCHAR(30),
	weight NUMERIC,
	volume NUMERIC,
	ddscore NUMERIC,
	ddate DATE,
	rstatus BOOL,
	reservationdate DATE,
	recycledate DATE,
	fname VARCHAR(30),
	mname VARCHAR(30),
	lname VARCHAR(30),
	cname VARCHAR(100),
	is_reserved BOOL
) AS $$
BEGIN
	RETURN QUERY
	SELECT
		dd.ddno, d.disposalname, dd.weight, dd.volume, dd.ddscore, dd.ddate, dd.rstatus, r.reservationdate, r.recycledate,
		n.fname, n.mname, n.lname, c.cname, 
		CASE WHEN rd.ddnumber IS NOT NULL THEN TRUE ELSE FALSE END AS is_reserved
	FROM discarded_disposal dd
	LEFT JOIN disposal d ON dd.dID = d.disposalID
	LEFT JOIN neighbor n ON n.tckn = dd.neighbortckn
	LEFT JOIN reservation_disposal rd ON dd.ddno = rd.ddnumber
	LEFT JOIN reservation r ON rd.rnumber = r.reservationNo
	LEFT JOIN company c ON r.cID = c.companyID
	ORDER BY dd.ddate DESC 
	LIMIT limitcount;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION get_current_recycled_disposals(limitcount INT)
RETURNS TABLE (
	ddno INT,
	disposalname VARCHAR(30),
	weight NUMERIC,
	volume NUMERIC,
	ddscore NUMERIC,
	ddate DATE,
	reservationdate DATE,
	recycledate DATE,
	cname VARCHAR(100)
) AS $$
BEGIN
	RETURN QUERY
	SELECT dd.ddno, d.disposalname, dd.weight, dd.volume, dd.ddscore, dd.ddate, r.reservationdate, r.recycledate, c.cname
	FROM reservation r
	JOIN reservation_disposal rd ON r.reservationNo = rd.rnumber
	JOIN discarded_disposal dd ON rd.ddnumber = dd.ddno
	JOIN disposal d ON dd.dID = d.disposalID
	JOIN company c ON r.cID = c.companyID
	WHERE dd.rstatus = TRUE
	ORDER BY r.recycledate DESC 
	LIMIT limitcount;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION get_neighborhood_leaderboard(
    p_tckn VARCHAR,
    p_limit INT
)
RETURNS TABLE (
    fname VARCHAR,
    mname VARCHAR,
    lname VARCHAR,
    out_tckn VARCHAR,
    total_score DOUBLE PRECISION
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        n.fname::VARCHAR,
        n.mname::VARCHAR, 
        n.lname::VARCHAR, 
        n.tckn::VARCHAR,
        
        COALESCE(SUM(CASE WHEN dd.rstatus = TRUE THEN dd.ddscore ELSE 0 END), 0)::DOUBLE PRECISION as score
    FROM neighbor n
    JOIN address a ON n.addressID = a.addressid
    JOIN street s ON a.streetID = s.streetid
    LEFT JOIN discarded_disposal dd ON n.tckn = dd.neighbortckn
    WHERE s.neighborhoodID = (
        SELECT s2.neighborhoodID
        FROM neighbor n2
        JOIN address a2 ON n2.addressID = a2.addressid
        JOIN street s2 ON a2.streetID = s2.streetid
        WHERE n2.tckn = p_tckn::CHAR(11)
    )
    GROUP BY n.tckn, n.fname, n.mname, n.lname
    ORDER BY score DESC
    LIMIT p_limit;
END;
$$ LANGUAGE plpgsql;

-- Neighbor leaderboard
CREATE OR REPLACE VIEW neighbor_leaderboard_view AS
SELECT 
    n.tckn, 
    n.fname, 
    n.mname, 
    n.lname, 
    COALESCE(SUM(dd.ddscore), 0) as total_score 
FROM 
    neighbor n 
LEFT JOIN 
    discarded_disposal dd ON n.tckn = dd.neighbortckn AND dd.rstatus = TRUE 
GROUP BY 
    n.tckn, n.fname, n.mname, n.lname 
HAVING 
    COALESCE(SUM(dd.ddscore), 0) > 0
ORDER BY 
    total_score DESC;

-- Company leaderboard
CREATE OR REPLACE VIEW company_leaderboard_view AS
SELECT
	c.cname,
	c.taxnumber,
	COALESCE(SUM(CASE WHEN dd.rstatus = TRUE THEN dd.ddscore ELSE 0 END), 0) as totalScore
FROM
	company c
LEFT JOIN 
	reservation r ON c.companyid = r.cID
LEFT JOIN 
	reservation_disposal rd ON r.reservationNo = rd.rnumber
LEFT JOIN 
	discarded_disposal dd ON rd.ddnumber = dd.ddno
GROUP BY 
	c.companyid, c.cname
HAVING 
    COALESCE(SUM(CASE WHEN dd.rstatus = TRUE THEN dd.ddscore ELSE 0 END), 0) > 0
ORDER BY 
	totalScore DESC;

-- Contribution score for discarded disposal
CREATE OR REPLACE FUNCTION calculate_disposal_score(
    p_type_id INTEGER, 
    p_weight FLOAT, 
    p_volume FLOAT
)
RETURNS FLOAT AS $$
DECLARE
    v_score_coef FLOAT;
    final_score FLOAT;
BEGIN
    SELECT scorecoef INTO v_score_coef 
    FROM disposal 
    WHERE disposalID = p_type_id;
    
    IF v_score_coef IS NULL THEN
        RETURN 0;
    END IF;

    -- scoreCoef * (weight + volume * 10)
    final_score := v_score_coef * (p_weight + (p_volume * 10));
    
    RETURN final_score;
END;
$$ LANGUAGE plpgsql;

-- Transportation cost for discarded disposal
CREATE OR REPLACE FUNCTION calculate_disposal_cost(
    p_type_id INTEGER, 
    p_weight FLOAT, 
    p_volume FLOAT
)
RETURNS FLOAT AS $$
DECLARE
    v_cost_coef FLOAT;
    final_cost FLOAT;
BEGIN
    SELECT tcostcoef INTO v_cost_coef 
    FROM disposal
    WHERE disposalID = p_type_id;
    
    IF v_cost_coef IS NULL THEN
        RETURN 0;
    END IF;

    -- tCostCoef * MAX(weight, volume * 10)
    final_cost := v_cost_coef * GREATEST(p_weight, (p_volume * 10));
    
    RETURN final_cost;
END;
$$ LANGUAGE plpgsql;

-- Returns user history based on given interval
CREATE OR REPLACE FUNCTION get_user_history_filtered(
    p_tckn VARCHAR, 
    p_days_limit INTEGER -- 0: All Times, 30: Last 1 Month, 365: Last 1 Year
)
RETURNS TABLE (
    dd_no INTEGER,      
    waste_type VARCHAR,
    weight NUMERIC,
    volume NUMERIC,
    d_date DATE,
    score NUMERIC,
    status_text VARCHAR 
) AS $$
BEGIN
    RETURN QUERY 
    SELECT 
        dd.ddno,
        d.disposalname::VARCHAR,
        dd.weight,
        dd.volume,
        dd.ddate,
        dd.ddscore,
        CASE 
            WHEN dd.rstatus = TRUE THEN 'Dönüştürüldü'::VARCHAR
            WHEN rd.ddnumber IS NOT NULL THEN 'Rezerve'::VARCHAR
            ELSE 'Bekliyor'::VARCHAR
        END
    FROM discarded_disposal dd
    JOIN disposal d ON dd.dID = d.disposalID
    LEFT JOIN reservation_disposal rd ON dd.ddno = rd.ddnumber
    WHERE dd.neighbortckn = p_tckn
    AND (p_days_limit = 0 OR dd.ddate >= CURRENT_DATE - (p_days_limit * INTERVAL '1 day'))
    ORDER BY dd.ddate DESC;
END;
$$ LANGUAGE plpgsql;

-- Returns not reserved and not recycled discarded disposals
CREATE OR REPLACE FUNCTION get_available_discarded_disposals_list()
RETURNS TABLE (
    dd_no INT,
    waste_type VARCHAR,
    weight FLOAT,
    volume FLOAT,
    score FLOAT,
    discard_date DATE,
    full_name VARCHAR
) AS $$
DECLARE
    -- CURSOR
    cur_waste CURSOR FOR 
        SELECT dd.ddno, d.disposalname, dd.weight, dd.volume, dd.ddscore, dd.ddate, 
               n.fname, n.mname, n.lname
        FROM discarded_disposal dd
        JOIN disposal d ON dd.dID = d.disposalID
        JOIN neighbor n ON n.tckn = dd.neighbortckn
        LEFT JOIN reservation_disposal rd ON dd.ddno = rd.ddnumber
        WHERE rd.ddnumber IS NULL -- Rezerve edilmemişler
        ORDER BY dd.ddate DESC;

    -- RECORD
    rec_waste RECORD;
BEGIN
    OPEN cur_waste;

    LOOP
        FETCH cur_waste INTO rec_waste;
        EXIT WHEN NOT FOUND;

        dd_no := rec_waste.ddno;
        waste_type := rec_waste.disposalname;
        weight := rec_waste.weight;
        volume := rec_waste.volume;
        score := rec_waste.ddscore;
        discard_date := rec_waste.ddate;

		IF rec_waste.mname IS NOT NULL AND rec_waste.mname <> '' THEN
            full_name := rec_waste.fname || ' ' || rec_waste.mname || ' ' || rec_waste.lname;
        ELSE
            full_name := rec_waste.fname || ' ' || rec_waste.lname;
        END IF;

        RETURN NEXT;
    END LOOP;

    CLOSE cur_waste;
END;
$$ LANGUAGE plpgsql;

-- Returns reserved but not recycled discarded disposals
CREATE OR REPLACE FUNCTION get_company_reserved_discarded_disposals_list(p_tax_number VARCHAR)
RETURNS TABLE (
    dd_no INT,
    waste_type VARCHAR,
    weight FLOAT,
    volume FLOAT,
    score FLOAT,
    discard_date DATE,
    res_date DATE,
    full_name VARCHAR,
    company_name VARCHAR
) AS $$
DECLARE
    -- CURSOR
    cur_res CURSOR FOR 
        SELECT dd.ddno, d.disposalname, dd.weight, dd.volume, dd.ddscore, 
               dd.ddate, r.reservationdate, 
               n.fname, n.mname, n.lname, c.cname
        FROM company c
        JOIN reservation r ON c.companyID = r.cID
        JOIN reservation_disposal rd ON r.reservationNo = rd.rnumber
        JOIN discarded_disposal dd ON rd.ddnumber = dd.ddno
        JOIN disposal d ON dd.dID = d.disposalID
        JOIN neighbor n ON n.tckn = dd.neighbortckn
        WHERE c.taxnumber = p_tax_number
        
        EXCEPT 
        
        SELECT dd.ddno, d.disposalname, dd.weight, dd.volume, dd.ddscore, 
               dd.ddate, r.reservationdate, 
               n.fname, n.mname, n.lname, c.cname
        FROM company c
        JOIN reservation r ON c.companyID = r.cID
        JOIN reservation_disposal rd ON r.reservationNo = rd.rnumber
        JOIN discarded_disposal dd ON rd.ddnumber = dd.ddno
        JOIN disposal d ON dd.dID = d.disposalID
        JOIN neighbor n ON n.tckn = dd.neighbortckn
        WHERE c.taxnumber = p_tax_number AND dd.rstatus = TRUE
        
        ORDER BY reservationdate DESC;

    -- RECORD
    rec_res RECORD;
BEGIN
    OPEN cur_res;
    LOOP
        FETCH cur_res INTO rec_res;
        EXIT WHEN NOT FOUND;

        dd_no := rec_res.ddno;
        waste_type := rec_res.disposalname;      
        weight := rec_res.weight;
        volume := rec_res.volume;
        score := rec_res.ddscore;
        discard_date := rec_res.ddate;
        res_date := rec_res.reservationdate;
        company_name := rec_res.cname;

        -- Name concatenation
        IF rec_res.mname IS NOT NULL AND rec_res.mname <> '' THEN
            full_name := rec_res.fname || ' ' || rec_res.mname || ' ' || rec_res.lname;
        ELSE
            full_name := rec_res.fname || ' ' || rec_res.lname;
        END IF;

        RETURN NEXT;
    END LOOP;
    CLOSE cur_res;
END;
$$ LANGUAGE plpgsql;

-- Returns only recycled discarded disposals
CREATE OR REPLACE FUNCTION get_company_recycled_discarded_disposals_list(p_tax_number VARCHAR)
RETURNS TABLE (
    dd_no INT,
    waste_type VARCHAR,
    weight FLOAT,
    volume FLOAT,
    score FLOAT,
    discard_date DATE,
    res_date DATE,
    rec_date DATE,
    full_name VARCHAR,
    company_name VARCHAR
) AS $$
DECLARE
    cur_rec CURSOR FOR 
        SELECT dd.ddno, d.disposalname, dd.weight, dd.volume, dd.ddscore, 
               dd.ddate, r.reservationdate, r.recycledate, 
               n.fname, n.mname, n.lname, c.cname 
        FROM company c 
        JOIN reservation r ON c.companyID = r.cID 
        JOIN reservation_disposal rd ON r.reservationNo = rd.rnumber 
        JOIN discarded_disposal dd ON rd.ddnumber = dd.ddno 
        JOIN disposal d ON dd.dID = d.disposalID 
        JOIN neighbor n ON n.tckn = dd.neighbortckn 
        WHERE c.taxnumber = p_tax_number
        
        INTERSECT 
        
        SELECT dd.ddno, d.disposalname, dd.weight, dd.volume, dd.ddscore, 
               dd.ddate, r.reservationdate, r.recycledate, 
               n.fname, n.mname, n.lname, c.cname 
        FROM company c 
        JOIN reservation r ON c.companyID = r.cID 
        JOIN reservation_disposal rd ON r.reservationNo = rd.rnumber 
        JOIN discarded_disposal dd ON rd.ddnumber = dd.ddno 
        JOIN disposal d ON dd.dID = d.disposalID 
        JOIN neighbor n ON n.tckn = dd.neighbortckn 
        WHERE dd.rstatus = TRUE
        
        ORDER BY ddate DESC;

    rec_rec RECORD;
BEGIN
    OPEN cur_rec;
    LOOP
        FETCH cur_rec INTO rec_rec;
        EXIT WHEN NOT FOUND;

        dd_no := rec_rec.ddno;
        waste_type := rec_rec.disposalname;
        weight := rec_rec.weight;
        volume := rec_rec.volume;
        score := rec_rec.ddscore;
        discard_date := rec_rec.ddate;
        res_date := rec_rec.reservationdate;
        rec_date := rec_rec.recycledate;
        
        IF rec_rec.mname IS NOT NULL AND rec_rec.mname <> '' THEN
            full_name := rec_rec.fname || ' ' || rec_rec.mname || ' ' || rec_rec.lname;
        ELSE
            full_name := rec_rec.fname || ' ' || rec_rec.lname;
        END IF;
        
        company_name := rec_rec.cname;

        RETURN NEXT;
    END LOOP;
    CLOSE cur_rec;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION get_available_disposals_filtered(
    p_filter_types VARCHAR[],
    p_min_weight NUMERIC,
    p_max_weight NUMERIC,
    p_min_volume NUMERIC,
    p_max_volume NUMERIC,
    p_start_date DATE,
    p_end_date DATE,
    p_tax_number VARCHAR,
    p_only_allowed BOOLEAN
)
RETURNS TABLE (
    dd_no INT,
    type_name VARCHAR,
    weight NUMERIC,
    volume NUMERIC,
    score NUMERIC,
    d_date DATE
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        dd.ddno,
        d.disposalname::VARCHAR,
        dd.weight,
        dd.volume,
        dd.ddscore,
        dd.ddate
    FROM discarded_disposal dd
    JOIN disposal d ON dd.dID = d.disposalID
    LEFT JOIN reservation_disposal rd ON dd.ddno = rd.ddnumber
    WHERE 
        rd.ddnumber IS NULL -- Not reserved
        
        -- Disposal type filter
        AND (p_filter_types IS NULL OR cardinality(p_filter_types) = 0 OR d.disposalname = ANY(p_filter_types))
        
        -- Numeric filters
        AND (p_min_weight IS NULL OR dd.weight >= p_min_weight)
        AND (p_max_weight IS NULL OR dd.weight <= p_max_weight)
        AND (p_min_volume IS NULL OR dd.volume >= p_min_volume)
        AND (p_max_volume IS NULL OR dd.volume <= p_max_volume)
        
        -- Date filter
        AND (p_start_date IS NULL OR dd.ddate >= p_start_date)
        AND (p_end_date IS NULL OR dd.ddate <= p_end_date)
        
        AND (
            p_only_allowed = FALSE 
            OR 
            EXISTS (
                SELECT 1 
                FROM company_disposal cdt
                JOIN company c ON cdt.cID = c.companyID
                WHERE c.taxnumber = p_tax_number
                AND cdt.dID = dd.dID
            )
        )
    ORDER BY dd.ddate DESC;
END;
$$ LANGUAGE plpgsql;

-- ROLES
CREATE ROLE app_admin;
CREATE ROLE app_user;

-- Admin privileges
GRANT ALL PRIVILEGES ON SCHEMA public TO app_admin;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO app_admin;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO app_admin;

ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO app_admin;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO app_admin;

-- User privileges
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO app_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO app_user;

ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO app_user;

-- Special protection for audit log table
REVOKE SELECT, UPDATE, DELETE, TRUNCATE ON TABLE audit_log FROM app_user;
GRANT INSERT ON TABLE audit_log TO app_user;

CREATE USER java_client WITH PASSWORD 'CENSORED';
GRANT app_user TO java_client;