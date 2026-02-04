create table pay_rate (
    id uuid default uuidv7() primary key,
    prison_code varchar(5) not null,
    type varchar(20) not null,
    start_date date not null,
    rate integer not null,
    created_date_time timestamp not null,
    created_by varchar(80) not null
);

create index pay_rate__prison_code_start_date_idx on pay_rate (prison_code, start_date);

insert into pay_rate(prison_code, type, start_date, rate, created_date_time, created_by) values
    ('RSI', 'LONG_TERM_SICK', '2026-01-01', 120, now(), 'USER1'),
    ('BCI', 'LONG_TERM_SICK', '2026-01-10', 65, now(), 'USER1');
