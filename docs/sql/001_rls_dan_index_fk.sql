-- Projek-hanif (Supabase). Dijalankan 25 Sep 2026 setelah Hibernate membentuk tabel awal.
-- Tabel dimiliki role aplikasi zelatan_app (koneksi JDBC), jadi RLS tanpa policy tidak
-- menghalangi backend, tetapi menutup akses lewat Data API Supabase (anon/authenticated).
set role zelatan_app;

alter table public.category            enable row level security;
alter table public.flash_sales         enable row level security;
alter table public.product             enable row level security;
alter table public.provider            enable row level security;
alter table public.top_up              enable row level security;
alter table public.transaction_history enable row level security;
alter table public.users               enable row level security;
alter table public.vouchers            enable row level security;

-- Postgres tidak mengindeks kolom foreign key secara otomatis.
create index if not exists flash_sales_product_id_idx          on public.flash_sales (product_id);
create index if not exists product_category_id_idx             on public.product (category_id);
create index if not exists product_provider_id_idx             on public.product (provider_id);
create index if not exists top_up_user_id_idx                  on public.top_up (user_id);
create index if not exists transaction_history_product_id_idx  on public.transaction_history (product_id);
create index if not exists transaction_history_purchaser_idx   on public.transaction_history (purchaser);

-- Laporan, dashboard, dan AI chat menyaring transaksi/top up per rentang tanggal.
create index if not exists transaction_history_timestamp_idx   on public.transaction_history ("timestamp");
create index if not exists top_up_date_idx                     on public.top_up (date);

reset role;
