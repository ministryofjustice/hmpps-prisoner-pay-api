insert into pay_rate(prison_code, type, start_date, rate, created_date_time, created_by, updated_date_time, updated_by)
values ('RSI', 'LONG_TERM_SICK', '2025-01-01', 60, now(), 'USER1', null, null),
       ('RSI', 'LONG_TERM_SICK', '2026-01-16', 70, now(), 'USER1', null, null),
       ('RSI', 'LONG_TERM_SICK', '2026-01-21', 80, now(), 'USER1', null, null),
       ('BCI', 'LONG_TERM_SICK', '2026-01-25', 80, now(), 'USER1', null, null);

insert into pay_status_period(prisoner_number, type, start_date, end_date, created_date_time, created_by, prison_code)
values ('A1111AA', 'LONG_TERM_SICK', '2026-01-01', '2026-01-16', now(), 'USER1', 'RSI'),
       ('B2222BB', 'LONG_TERM_SICK', '2026-01-13', null, now(), 'USER1', 'RSI'),
       ('C3333CC', 'LONG_TERM_SICK', '2025-01-01', '2026-01-13', now(), 'USER1', 'RSI');