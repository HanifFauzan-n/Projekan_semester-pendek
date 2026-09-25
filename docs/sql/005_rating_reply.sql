-- SKPL-F21 (usulan): admin membalas ulasan pelanggan. Ulasan kini bisa dilihat publik.
set role zelatan_app;

alter table public.store_ratings
  add column admin_reply text check (admin_reply is null or char_length(admin_reply) <= 1000),
  add column replied_at  timestamptz;

reset role;
