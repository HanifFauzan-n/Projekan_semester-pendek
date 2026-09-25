-- 007: produk Voucher Game (data, bukan skema). Aman dijalankan berulang.
-- Top up langsung ke akun game: pembeli wajib mengisi ID game, lihat PurchaseTarget.java (GAME_ID).
-- Logo penerbit dimuat DataDummy dari static/img/<nama provider>.png setiap aplikasi start.

insert into public.category (id, code, type)
select gen_random_uuid()::text, 'CD-005', 'VOUCHER GAME'
where not exists (select 1 from public.category where upper(type) like '%GAME%');

insert into public.provider (name)
select g.name from (values ('Mobile Legends'), ('Free Fire'), ('PUBG Mobile'), ('Genshin Impact'), ('Valorant')) as g(name)
where not exists (select 1 from public.provider p where lower(p.name) = lower(g.name));

insert into public.product (product_name, price, cost_price, stock, description, provider_id, category_id)
select v.name, v.price, v.cost, v.stock, v.description,
       (select id from public.provider p where lower(p.name) = lower(v.provider) order by id limit 1),
       (select id from public.category where upper(type) like '%GAME%' order by code limit 1)
from (values
    ('Mobile Legends 86 Diamonds', 'Mobile Legends', 22000, 20500, 200, 'Top up langsung ke akun. Isi User ID dan Zone ID, contoh 12345678 (1234).'),
    ('Mobile Legends 172 Diamonds', 'Mobile Legends', 44000, 41000, 200, 'Top up langsung ke akun. Isi User ID dan Zone ID, contoh 12345678 (1234).'),
    ('Mobile Legends 257 Diamonds', 'Mobile Legends', 65000, 61000, 150, 'Top up langsung ke akun. Isi User ID dan Zone ID, contoh 12345678 (1234).'),
    ('Mobile Legends 706 Diamonds', 'Mobile Legends', 175000, 164000, 100, 'Top up langsung ke akun. Isi User ID dan Zone ID, contoh 12345678 (1234).'),
    ('Mobile Legends Weekly Diamond Pass', 'Mobile Legends', 28000, 26000, 200, 'Top up langsung ke akun. Isi User ID dan Zone ID, contoh 12345678 (1234).'),
    ('Free Fire 70 Diamonds', 'Free Fire', 10000, 9200, 200, 'Top up langsung ke akun. Isi ID pemain Free Fire.'),
    ('Free Fire 140 Diamonds', 'Free Fire', 19500, 18300, 200, 'Top up langsung ke akun. Isi ID pemain Free Fire.'),
    ('Free Fire 355 Diamonds', 'Free Fire', 48500, 45500, 150, 'Top up langsung ke akun. Isi ID pemain Free Fire.'),
    ('Free Fire 720 Diamonds', 'Free Fire', 97000, 91000, 100, 'Top up langsung ke akun. Isi ID pemain Free Fire.'),
    ('Free Fire Membership Mingguan', 'Free Fire', 29000, 27000, 200, 'Top up langsung ke akun. Isi ID pemain Free Fire.'),
    ('PUBG Mobile 60 UC', 'PUBG Mobile', 15000, 14000, 200, 'Top up langsung ke akun. Isi Character ID PUBG Mobile.'),
    ('PUBG Mobile 325 UC', 'PUBG Mobile', 75000, 70500, 100, 'Top up langsung ke akun. Isi Character ID PUBG Mobile.'),
    ('PUBG Mobile 660 UC', 'PUBG Mobile', 150000, 141000, 80, 'Top up langsung ke akun. Isi Character ID PUBG Mobile.'),
    ('Genshin Impact 60 Genesis Crystals', 'Genshin Impact', 16000, 15000, 200, 'Top up langsung ke akun. Isi UID Genshin Impact.'),
    ('Genshin Impact 330 Genesis Crystals', 'Genshin Impact', 79000, 74000, 100, 'Top up langsung ke akun. Isi UID Genshin Impact.'),
    ('Genshin Impact Blessing of the Welkin Moon', 'Genshin Impact', 79000, 74000, 100, 'Top up langsung ke akun. Isi UID Genshin Impact.'),
    ('Valorant 475 VP', 'Valorant', 55000, 51500, 100, 'Top up langsung ke akun. Isi Riot ID, contoh Nama#TAG.'),
    ('Valorant 1000 VP', 'Valorant', 110000, 103000, 80, 'Top up langsung ke akun. Isi Riot ID, contoh Nama#TAG.')
) as v(name, provider, price, cost, stock, description)
where not exists (select 1 from public.product p where p.product_name = v.name);
