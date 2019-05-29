create table application_input_values(
  id BIGSERIAL PRIMARY KEY NOT NULL,
  value varchar(255),
  application_input_id bigint,
  foreign key (application_input_id) references application_input(id)
 );
