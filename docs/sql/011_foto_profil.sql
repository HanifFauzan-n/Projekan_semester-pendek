-- 011: foto profil yang diunggah pelanggan. Tabel terpisah dari users supaya setiap query
-- pengguna (login, saldo) tidak ikut memuat gambar. Server selalu menyimpan JPEG 256x256
-- hasil encode ulang (UserService.saveAvatar), bukan file asli dari pengguna.
set role zelatan_app;

create table public.user_avatars (
  user_id    integer primary key references public.users (id) on delete cascade,
  image      bytea not null check (octet_length(image) <= 512000),
  updated_at timestamptz not null default now()
);

alter table public.user_avatars enable row level security;

reset role;
