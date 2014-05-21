create database mnitter;
grant all on mnitter.* to testdb@localhost identified by 'mnitter';
CREATE TABLE mnitter.user
(
id int auto_increment primary key,
username varchar(50) not null,
email varchar(100) not null,
password varchar(32) not null
);

ALTER TABLE mnitter.user ADD UNIQUE (username);
ALTER TABLE mnitter.user ADD UNIQUE (email);



CREATE TABLE mnitter.note
(
id int auto_increment primary key,
user_id int not null,
date datetime not null,
content varchar(255) not null
);
