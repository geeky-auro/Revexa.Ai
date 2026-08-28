-- Revexa.Ai initial schema.
-- Written in the subset of SQL shared by PostgreSQL and H2's PostgreSQL compatibility mode, so the
-- local and production schemas come from exactly the same migrations.

create table user_account (
    id                 uuid primary key,
    email              varchar(320) not null unique,
    display_name       varchar(120) not null,
    password_hash      varchar(200) not null,
    role               varchar(20)  not null default 'USER',
    theme              varchar(16)  not null default 'system',
    preferred_language varchar(32)  not null default 'python',
    mentor_mode        varchar(16)  not null default 'guided',
    last_active_at     timestamp with time zone,
    created_at         timestamp with time zone not null,
    updated_at         timestamp with time zone not null
);

create table problem (
    id               uuid primary key,
    title            varchar(300) not null,
    slug             varchar(300) not null,
    source           varchar(32)  not null default 'MANUAL',
    external_id      varchar(120),
    url              varchar(500),
    difficulty       varchar(16)  not null default 'UNKNOWN',
    statement        text         not null,
    constraints_text text,
    examples         text,
    owner_id         uuid references user_account (id) on delete cascade,
    curated          boolean      not null default false,
    created_at       timestamp with time zone not null,
    updated_at       timestamp with time zone not null
);

create index idx_problem_owner on problem (owner_id);
create index idx_problem_curated on problem (curated);

create table problem_topic (
    problem_id uuid        not null references problem (id) on delete cascade,
    topic      varchar(80) not null
);

create index idx_problem_topic_problem on problem_topic (problem_id);

create table submission (
    id         uuid primary key,
    user_id    uuid        not null references user_account (id) on delete cascade,
    problem_id uuid        not null references problem (id) on delete cascade,
    language   varchar(32) not null,
    code       text        not null,
    notes      varchar(1000),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_submission_user on submission (user_id, created_at desc);
create index idx_submission_problem on submission (user_id, problem_id);

create table review (
    id               uuid primary key,
    user_id          uuid        not null references user_account (id) on delete cascade,
    problem_id       uuid        not null references problem (id) on delete cascade,
    submission_id    uuid        not null references submission (id) on delete cascade,
    payload          text        not null,
    verdict          varchar(24) not null,
    score            integer     not null,
    time_complexity  varchar(40) not null,
    space_complexity varchar(40) not null,
    optimal_time     varchar(40) not null,
    optimal_space    varchar(40) not null,
    time_optimal     boolean     not null,
    language         varchar(32) not null,
    approach_name    varchar(120) not null,
    provider         varchar(40) not null,
    model            varchar(80) not null,
    created_at       timestamp with time zone not null,
    updated_at       timestamp with time zone not null
);

create index idx_review_user on review (user_id, created_at desc);
create index idx_review_submission on review (submission_id);

create table review_topic (
    review_id uuid        not null references review (id) on delete cascade,
    topic     varchar(80) not null
);

create index idx_review_topic_review on review_topic (review_id);

create table review_issue (
    review_id uuid         not null references review (id) on delete cascade,
    issue     varchar(160) not null
);

create index idx_review_issue_review on review_issue (review_id);

create table hint_session (
    id                uuid primary key,
    user_id           uuid    not null references user_account (id) on delete cascade,
    problem_id        uuid    not null references problem (id) on delete cascade,
    submission_id     uuid,
    current_level     integer not null default 0,
    solution_revealed boolean not null default false,
    created_at        timestamp with time zone not null,
    updated_at        timestamp with time zone not null
);

create unique index idx_hint_session_user_problem on hint_session (user_id, problem_id);

create table hint_record (
    id         uuid primary key,
    session_id uuid         not null references hint_session (id) on delete cascade,
    level      integer      not null,
    title      varchar(160) not null,
    payload    text         not null,
    spoiler    boolean      not null default false,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create unique index idx_hint_record_session_level on hint_record (session_id, level);

create table chat_thread (
    id                uuid primary key,
    user_id           uuid         not null references user_account (id) on delete cascade,
    problem_id        uuid         not null references problem (id) on delete cascade,
    submission_id     uuid,
    title             varchar(200) not null,
    max_spoiler_level integer      not null default 1,
    created_at        timestamp with time zone not null,
    updated_at        timestamp with time zone not null
);

create index idx_chat_thread_user on chat_thread (user_id, updated_at desc);

create table chat_message (
    id                uuid        primary key,
    thread_id         uuid        not null references chat_thread (id) on delete cascade,
    role              varchar(16) not null,
    content           text        not null,
    follow_ups        text,
    spoiler_level     integer     not null default 1,
    revealed_solution boolean     not null default false,
    created_at        timestamp with time zone not null,
    updated_at        timestamp with time zone not null
);

create index idx_chat_message_thread on chat_message (thread_id, created_at);

create table bookmark (
    id           uuid primary key,
    user_id      uuid         not null references user_account (id) on delete cascade,
    kind         varchar(24)  not null,
    problem_id   uuid,
    reference_id uuid,
    title        varchar(200) not null,
    content      text         not null,
    note         varchar(500),
    created_at   timestamp with time zone not null,
    updated_at   timestamp with time zone not null
);

create index idx_bookmark_user on bookmark (user_id, created_at desc);
