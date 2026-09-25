-- 009: username tidak membedakan huruf besar/kecil ("Budi" = "budi").
-- UserRepository.findByUsername mencari dengan lower(username) = lower(:username); index ini
-- membuat pencarian itu tetap cepat dan menolak pendaftaran ganda beda huruf di tingkat database.
-- Sebelum dijalankan: pastikan tidak ada duplikat (select lower(username), count(*) ... having count(*) > 1).

create unique index if not exists users_username_lower_key on public.users (lower(username));
