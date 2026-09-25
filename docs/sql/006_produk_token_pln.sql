-- 006: produk Token Listrik PLN (data, bukan skema). Aman dijalankan berulang.
-- Harga jual = nominal + Rp 1.500 biaya admin; harga modal = nominal + Rp 200.
-- Pembeli wajib mengisi nomor meter / ID pelanggan (11-12 digit), lihat PurchaseTarget.java.

insert into public.category (id, code, type)
select gen_random_uuid()::text, 'CD-004', 'TOKEN PLN'
where not exists (select 1 from public.category where upper(type) like '%PLN%');

insert into public.provider (name)
select 'PLN'
where not exists (select 1 from public.provider where lower(name) = 'pln');

insert into public.product (product_name, price, cost_price, stock, description, provider_id, category_id)
select v.name, v.nominal + 1500, v.nominal + 200, v.stock,
       'Token listrik prabayar PLN Rp ' || replace(to_char(v.nominal, 'FM999,999,999'), ',', '.') || '. Isi nomor meter / ID pelanggan saat membeli.',
       (select id from public.provider where lower(name) = 'pln' order by id limit 1),
       (select id from public.category where upper(type) like '%PLN%' order by code limit 1)
from (values
    ('Token Listrik PLN 20.000', 20000, 100),
    ('Token Listrik PLN 50.000', 50000, 100),
    ('Token Listrik PLN 100.000', 100000, 100),
    ('Token Listrik PLN 200.000', 200000, 50),
    ('Token Listrik PLN 500.000', 500000, 30),
    ('Token Listrik PLN 1.000.000', 1000000, 20)
) as v(name, nominal, stock)
where not exists (select 1 from public.product p where p.product_name = v.name);
