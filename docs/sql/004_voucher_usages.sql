-- SKPL-F04/F07/F13: satu pelanggan hanya boleh memakai voucher yang sama satu kali.
set role zelatan_app;

create table public.voucher_usages (
  id             bigint generated always as identity primary key,
  voucher_id     integer not null references public.vouchers (id) on delete cascade,
  user_id        integer not null references public.users (id) on delete cascade,
  transaction_id integer references public.transaction_history (id) on delete set null,
  used_at        timestamptz not null default now(),
  unique (voucher_id, user_id)
);
-- voucher_id sudah terindeks sebagai kolom pertama constraint UNIQUE.
create index voucher_usages_user_id_idx on public.voucher_usages (user_id);
create index voucher_usages_transaction_id_idx on public.voucher_usages (transaction_id);

alter table public.voucher_usages enable row level security;

reset role;
