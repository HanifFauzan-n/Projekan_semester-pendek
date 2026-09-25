-- SKPL-F21 (usulan): rating toko "Nilai Kami". Satu rating per pelanggan, bisa diubah.
set role zelatan_app;

create table public.store_ratings (
  id         bigint generated always as identity primary key,
  user_id    integer not null unique references public.users (id) on delete cascade,
  rating     smallint not null check (rating between 1 and 5),
  comment    text check (comment is null or char_length(comment) <= 500),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
-- user_id sudah terindeks oleh constraint UNIQUE.
create index store_ratings_updated_at_idx on public.store_ratings (updated_at);

alter table public.store_ratings enable row level security;

reset role;
