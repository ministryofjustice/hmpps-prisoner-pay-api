create table pay_status_period (
    id uuid default uuidv7() primary key,
    prisoner_number varchar(7) not null,
    type varchar(20) not null,
    start_date date not null,
    end_date date,
    created_date_time timestamp not null,
    created_by varchar(80) not null
);

create index pay_status_period__prisoner_number_idx on pay_status_period (prisoner_number);