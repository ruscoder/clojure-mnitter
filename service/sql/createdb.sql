create database mnitter;
grant all on mnitter.* to testdb@localhost identified by 'mnitter';
CREATE TABLE "mnitter"."user"
(
id serial primary key,
username varchar(50) not null,
email varchar(100) not null,
password varchar(32) not null
);

ALTER TABLE "user" ADD UNIQUE (username);
ALTER TABLE "user" ADD UNIQUE (email);



CREATE TABLE "note"
(
id serial primary key,
user_id int not null,
date timestamp not null,
content varchar(255) not null
);
