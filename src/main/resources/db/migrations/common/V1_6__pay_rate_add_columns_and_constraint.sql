alter table pay_rate
    add column updated_by varchar(80),
    add column updated_date_time timestamp;

alter table pay_rate
add constraint unique_pay_rate_prison_type_start_date
unique (prison_code, type, start_date);
