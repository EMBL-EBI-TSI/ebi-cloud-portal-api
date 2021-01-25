-- Persisting two accounts , Karo and Ajay , these must be exist in AAP explore
INSERT INTO account(id,reference, username, password, email, first_joined_date,organisation, avatar_image_url, given_name) VALUES (10001,'acc1574349954247','usr-b070585b-a340-4a98-aff1-f3de48da8c38', '', 'embl.ebi.tsi@gmail.com','2019-11-21','','','Karo Testing');
INSERT INTO account(id,reference, username, password, email, first_joined_date, organisation, avatar_image_url, given_name) VALUES (10002,'acc1566222724022','usr-e8c1d6d5-6bf4-4636-a70e-41b8f32c70b4', '', 'ajay@email.uk','2019-08-19','','','Ajay User');
INSERT INTO account(id,reference, username, password, email, first_joined_date, organisation, avatar_image_url, given_name) VALUES (10003,'acc1566222789899','usr-eeaa9825-44e6-46ad-9da6-26fcfdba6f8b', '', 'panther@ebi.ac.uk','2021-01-25','','','Panther User');
-- Two teams owned by Ajay, Karo is member of both the teams
INSERT INTO team(id, name, owner_account_id, domain_reference) VALUES (9001, 'test-team1', 10002, 'dom-e0de1881-d284-401a-935e-8979b328b158');
INSERT INTO team(id, name, owner_account_id, domain_reference) VALUES (9002, 'test-team2', 10002, 'dom-4f412d31-cde5-452d-8536-b650a0b7b5d4');
INSERT INTO account_team(account_id, team_id) VALUES (10001, 9001);
INSERT INTO account_team(account_id, team_id) VALUES (10001, 9002);
