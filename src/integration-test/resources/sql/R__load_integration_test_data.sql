INSERT INTO account(id,reference, username, password, email, first_joined_date,organisation, avatar_image_url, given_name) VALUES (1,'acc1574349954247','usr-d8749acf-6a22-4438-accc-cc8d1877ba36', '', 'embl.ebi.tsi@gmail.com','2019-11-21','','','Karo Testing');
INSERT INTO account(id,reference, username, password, email, first_joined_date, organisation, avatar_image_url, given_name) VALUES (2,'acc1566222724022','usr-9832620d-ec53-43a1-873d-efdc50d34ad1', '', 'ajay@email.uk','2019-08-19','','','Ajay User');
INSERT INTO application(id, account_id, repo_uri, repo_path, name, about, contact, version,reference) VALUES (1, 1,'https://github.com/EMBL-EBI-TSI/cpa-instance', '/mnt/c/Work/ECP/Volumes/be_applications_folder/usr-ba053d51-b223-45ff-9d7c-a08e44a3672a/cpa-instance',	'Generic server instance', 'Generic server instance', 'gianni@ebi.ac.uk', '0.6','app-1c2f0d3f-2dea-4369-a6af-7048a720ffa5');
INSERT INTO application(id, account_id, repo_uri, repo_path, name, about, contact, version,reference) VALUES (2, 2,'https://github.com/EMBL-EBI-TSI/cpa-redis', '/mnt/c/Work/ECP/Volumes/be_applications_folder/usr-ba053d51-b223-45ff-9d7c-a08e44a3672a/cpa-redis',	'redis', 'Redis Server instance', 'gianni@ebi.ac.uk', '0.1','app-801993bb-dfc9-48d3-b0ac-348255f45b73');
INSERT INTO cloud_provider_parameters(id, name, cloud_provider, account_id, reference) VALUES (10, 'ostack provider','OSTACK',2,'f19a8469-692d-4196-8a08-fe5a71c5ace7');
INSERT INTO configuration_deployment_parameters(name, account_id, reference) VALUES ('ostack deploy params', 2, '18a43ec5-5adf-41df-9f10-b8e4c4ae52a1');
INSERT INTO configuration(id, name, ssh_key, cloud_provider_parameters_name, account_id,configuration_deployment_parameters_id, cloud_provider_parameters_id,reference, cloud_provider_params_reference, config_deployment_params_reference, config_deployment_params_name, soft_usage_limit, hard_usage_limit) VALUES (10, 'config1', 'ssh-key332dasd213', 'ostack provider', 2, null, null, 'b701128f-4fca-4b9f-b42c-f31ddb0823af', 'f19a8469-692d-4196-8a08-fe5a71c5ace7', 'ostack deploy params','18a43ec5-5adf-41df-9f10-b8e4c4ae52a1', null, null);
INSERT INTO team(id, name, owner_account_id, domain_reference) VALUES (1, 'test-team1', 2, 'dom-e0de1881-d284-401a-935e-8979b328b158');
INSERT INTO team(id, name, owner_account_id, domain_reference) VALUES (2, 'test-team2', 2, 'dom-4f412d31-cde5-452d-8536-b650a0b7b5d4');
INSERT INTO account_team(account_id, team_id) VALUES (1, 1);
INSERT INTO account_team(account_id, team_id) VALUES (1, 2);
INSERT INTO team_shared_applications(team_id, application_id) VALUES (2, 2);
INSERT INTO team_shared_configurations(team_id, configuration_id) VALUES (1, 10);
INSERT INTO team_shared_cloud_provider_parameters(team_id, cloud_provider_parameters_id) VALUES (1, 10);




