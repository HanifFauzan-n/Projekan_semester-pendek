-- 008: kategori PULSA dan PAKET DATA dengan nominal lengkap per operator (data, bukan skema).
-- Aman dijalankan berulang. Produk lama tidak dihapus karena dipakai riwayat transaksi.

update public.category set type = 'PULSA' where upper(type) = 'MOBILE CREDIT';
update public.category set type = 'PAKET DATA' where upper(type) = 'DATA PLAN';
update public.product set product_name = regexp_replace(product_name, ' Data Plan$', ' / 30 Hari')
where product_name like '% Data Plan';

-- Pulsa: 16 nominal x 6 operator. Harga jual = nominal + biaya konter; modal = harga distributor.
insert into public.product (product_name, price, cost_price, stock, description, provider_id, category_id)
select pr.name || ' ' || replace(to_char(n.nominal, 'FM9,999,999'), ',', '.'),
       n.nominal + case when n.nominal <= 5000 then 1000 when n.nominal < 100000 then 1500 else 2000 end,
       n.nominal + case when n.nominal < 100000 then 100 else -500 end,
       case when n.nominal <= 100000 then 100 when n.nominal <= 300000 then 50 when n.nominal <= 500000 then 20 else 10 end,
       'Pulsa reguler ' || pr.name || ' Rp ' || replace(to_char(n.nominal, 'FM9,999,999'), ',', '.'),
       pr.id,
       (select id from public.category where type = 'PULSA' order by code limit 1)
from (values ('Telkomsel'), ('Indosat'), ('XL'), ('Tri'), ('Smartfren'), ('Axis')) as op(name)
join lateral (select id, name from public.provider p where lower(p.name) = lower(op.name) order by id limit 1) pr on true
cross join (values (2000), (5000), (10000), (15000), (20000), (25000), (30000), (40000), (50000), (75000),
                   (100000), (150000), (200000), (300000), (500000), (1000000)) as n(nominal)
where not exists (select 1 from public.product x
                  where x.product_name = pr.name || ' ' || replace(to_char(n.nominal, 'FM9,999,999'), ',', '.'));

-- Paket data: 7 paket x 6 operator. Harga dasar dikali faktor operator, dibulatkan ke Rp 500.
insert into public.product (product_name, price, cost_price, stock, description, provider_id, category_id)
select pr.name || ' ' || pk.label,
       round(pk.base * op.factor / 500) * 500,
       round(round(pk.base * op.factor / 500) * 500 * 0.92 / 100) * 100,
       100,
       'Paket data ' || pr.name || ' ' || pk.label || ', kuota 24 jam semua jaringan.',
       pr.id,
       (select id from public.category where type = 'PAKET DATA' order by code limit 1)
from (values ('Telkomsel', 1.2), ('Indosat', 1.0), ('XL', 1.0), ('Tri', 0.9), ('Smartfren', 0.9), ('Axis', 0.85)) as op(name, factor)
join lateral (select id, name from public.provider p where lower(p.name) = lower(op.name) order by id limit 1) pr on true
cross join (values ('1GB / 3 Hari', 10000), ('3GB / 7 Hari', 20000), ('5GB / 30 Hari', 30000), ('10GB / 30 Hari', 50000),
                   ('20GB / 30 Hari', 80000), ('35GB / 30 Hari', 110000), ('50GB / 30 Hari', 150000)) as pk(label, base)
where not exists (select 1 from public.product x where x.product_name = pr.name || ' ' || pk.label);

-- Deskripsi produk lama (bahasa Inggris) diseragamkan ke Bahasa Indonesia.
update public.product p set description = 'Pulsa reguler ' || p.product_name
from public.category c where c.id = p.category_id and c.type = 'PULSA' and p.description not like 'Pulsa reguler%';
update public.product p set description = 'Paket data ' || p.product_name || ', kuota 24 jam semua jaringan.'
from public.category c where c.id = p.category_id and c.type = 'PAKET DATA' and p.description not like 'Paket data%';
