create table payment (
    id uuid default uuidv7() primary key,
    prison_code varchar(5) not null,
    prisoner_number varchar(7) not null,
    event_date date not null, -- The date that the pay status period relates to, activity attendance, etc.
    time_slot varchar(2), -- e.g. time slot such as AM or PM,
    payment_type varchar(20) not null,
    payment_date_time timestamp not null,
    payment_amount integer not null, -- in pence
    reference varchar(12) not null
);

create index payment__prisoner_number_idx on payment (prisoner_number);
create index payment__prison_code_idx on payment (prison_code);
create index payment__event_date_idx on payment (event_date);
create index payment__payment_date_time_idx on payment (payment_date_time);
create unique index payment__reference_idx on payment (reference);
