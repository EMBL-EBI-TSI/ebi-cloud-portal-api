create table IF NOT EXISTS team_contact_emails (
	email varchar(100),
	team_id bigint,
	foreign key (team_id) references team(id),
	constraint unique_met unique(email, team_id)
);
