alter table pay_status_period add prison_code varchar(5) not null;

create index pay_status_period__prison_code_idx on pay_status_period (prison_code);