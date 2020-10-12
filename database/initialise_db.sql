CREATE SCHEMA ais_etl;

CREATE TABLE ais_etl.failed_message(
timestamp timestamp default current_timestamp,
errors text
);
CREATE TABLE ais_etl.ais_message(
timestamp bigint,
mobile_type text,
mmsi bigint,
latitude double precision,
longitude double precision,
status text,
vessel_type text,
imo bigint null);

CREATE INDEX ON ais_etl.ais_message(timestamp, mmsi);

CREATE TABLE ais_etl.ais_message_geo_enrichment(
timestamp bigint,
mmsi bigint,
country_code text,
admin_one text,
admin_two text,
latitude double precision,
longitude double precision,
name text);

CREATE INDEX ON ais_etl.ais_message_geo_enrichment(timestamp, mmsi);

CREATE VIEW ais_etl.v_ship_locations_by_minute AS (
Select
date_trunc('MINUTE', to_timestamp(ais.timestamp / 1000)) as minute,
count(distinct ais.mmsi) as comms_id,
count(distinct imo) as maritime_id,
geo.country_code,
ais.vessel_type
from ais_etl.ais_message ais
LEFT JOIN ais_etl.ais_message_geo_enrichment geo ON ais.timestamp = geo.timestamp
AND ais.mmsi = geo.mmsi
Group By
minute,
vessel_type,
country_code,
vessel_type
ORDER BY minute desc
)

