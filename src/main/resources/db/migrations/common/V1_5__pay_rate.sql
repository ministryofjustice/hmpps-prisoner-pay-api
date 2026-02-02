create table pay_rate (
    id uuid default uuidv7() primary key,
    prison_code varchar(5) not null,
    type varchar(20) not null,
    start_date date not null,
    rate integer not null,
    created_date_time timestamp not null,
    created_by varchar(80) not null
);

create index pay_rate_prison_code_idx on pay_rate (prison_code);

insert into pay_rate(prison_code, type, start_date, rate, created_date_time, created_by) values
    ('RSI', 'LONG_TERM_SICK', '2026-01-10', 120, now(), 'USER1'),
    ('RSI', 'LONG_TERM_SICK', '2026-01-15', 100, now(), 'USER1'),
    ('RSI', 'LONG_TERM_SICK', '2026-02-10', 80, now(), 'USER1'),
    ('RSI', 'LONG_TERM_SICK', '2026-02-20', 110, now(), 'USER1'),
    ('RSI', 'LONG_TERM_SICK', '2026-01-10', 90, now(), 'USER1'),
    ('BCI', 'LONG_TERM_SICK', '2026-01-10', 65, now(), 'USER1'),
    ('BCI', 'LONG_TERM_SICK', '2026-01-26', 65, now(), 'USER1'),
    ('BCI', 'LONG_TERM_SICK', '2026-01-20', 75, now(), 'USER1'),
    ('BCI', 'LONG_TERM_SICK', '2026-01-25', 85, now(), 'USER1'),
    ('BCI', 'LONG_TERM_SICK', '2026-03-10', 99, now(), 'USER1');