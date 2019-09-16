BEGIN;
ALTER TABLE application ADD COLUMN reference character varying(255);
UPDATE application SET reference = 'app-' || substring(md5(random()::text),28) || '-' || (FLOOR(((EXTRACT(EPOCH FROM CURRENT_TIMESTAMP))*10000 + (random()*10000))/10))::text;
ALTER TABLE application ALTER reference SET NOT NULL;
ALTER TABLE application ADD CONSTRAINT application_unique_reference UNIQUE (reference);
END;