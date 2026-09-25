-- 010: pelanggan boleh membayar produk langsung lewat Xendit (tanpa saldo), dengan biaya admin 10%.
-- product_orders menyimpan pesanan sampai webhook Xendit PAID masuk; setelah itu produk diproses
-- lewat jalur pembelian biasa dan tercatat di transaction_history (payment_method = 'XENDIT').
set role zelatan_app;

alter table public.transaction_history
  add column if not exists payment_method text not null default 'SALDO'
    check (payment_method in ('SALDO', 'XENDIT')),
  add column if not exists admin_fee double precision not null default 0 check (admin_fee >= 0);

create table public.product_orders (
  id                bigint generated always as identity primary key,
  external_id       text not null unique,
  user_id           integer not null references public.users (id) on delete cascade,
  product_id        integer not null references public.product (id),
  customer_number   text not null check (char_length(customer_number) between 4 and 40),
  voucher_code      text,
  price             integer not null check (price >= 0),
  admin_fee         integer not null check (admin_fee >= 0),
  total             integer not null check (total = price + admin_fee),
  status            text not null default 'PENDING'
                    check (status in ('PENDING', 'PAID', 'SUCCESS', 'FAILED', 'EXPIRED')),
  note              text,
  invoice_url       text,
  xendit_invoice_id text,
  payment_channel   text,
  transaction_id    text,
  created_at        timestamptz not null default now(),
  paid_at           timestamptz
);
create index product_orders_user_id_idx on public.product_orders (user_id);
create index product_orders_product_id_idx on public.product_orders (product_id);

alter table public.product_orders enable row level security;

reset role;
