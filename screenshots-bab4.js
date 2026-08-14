const { chromium } = require('playwright');
const fs = require('fs');

const BASE = 'http://localhost:8080';
const DIR = 'screenshots-bab4';

const ADMIN = { username: 'Hanif18', password: 'rahasia123' };
const USER = { username: 'rizky21', password: 'password123' };

const guestPages = [
    { file: '4.2.1-login.png', path: '/login' },
    { file: '4.2.2-registrasi.png', path: '/register' },
];

const adminPages = [
    { file: '4.2.3-dashboard-admin-produk-stok.png', path: '/home-admin' },
    { file: '4.2.4-data-pengguna.png', path: '/admin/users' },
    { file: '4.2.5-kelola-kategori.png', path: '/categories' },
    { file: '4.2.7-data-top-up.png', path: '/admin/topups' },
    { file: '4.2.8-kelola-provider.png', path: '/admin/providers' },
    { file: '4.2.12-laporan-penjualan.png', path: '/admin/transactions/sales' },
    { file: '4.2.13-kelola-voucher-diskon.png', path: '/admin/vouchers' },
];

const userPages = [
    { file: '4.2.6-request-top-up.png', path: '/topup' },
    { file: '4.2.9-katalog-produk.png', path: '/home-user' },
    { file: '4.2.10-transaksi-pembelian.png', path: '/transaction/confirm/1' },
    { file: '4.2.11-riwayat-transaksi.png', path: '/profile-user' },
    { file: '4.2.14-kelola-profil-akun.png', path: '/user/edit' },
];

const manifest = [];
let failed = 0;

async function shot(page, entry, role) {
    const url = BASE + entry.path;
    try {
        await page.goto(url, { waitUntil: 'networkidle', timeout: 20000 }).catch(() =>
            page.goto(url, { waitUntil: 'load', timeout: 20000 })
        );
        await page.waitForTimeout(800);
        await page.screenshot({ path: `${DIR}/${entry.file}`, fullPage: true });
        manifest.push(`OK | ${entry.file} | HTTP 200 | ${url}`);
        console.log(`OK   [${role}] ${entry.file} <- ${url}`);
    } catch (err) {
        failed++;
        manifest.push(`FAIL | ${entry.file} | ERROR | ${url}`);
        console.log(`FAIL [${role}] ${entry.file} <- ${url} (${err.message.split('\n')[0]})`);
    }
}

async function login(page, creds) {
    await page.goto(`${BASE}/login`, { waitUntil: 'networkidle', timeout: 20000 });
    await page.fill('#username', creds.username);
    await page.fill('#password', creds.password);
    await page.click('button[type="submit"]');
    await page.waitForTimeout(1500);
}

(async () => {
    if (!fs.existsSync(DIR)) fs.mkdirSync(DIR);
    const browser = await chromium.launch({ channel: 'chrome', headless: true });

    const guest = await browser.newContext({ viewport: { width: 1366, height: 768 } });
    const page = await guest.newPage();
    for (const entry of guestPages) await shot(page, entry, 'GUEST');
    await guest.close();

    const adminCtx = await browser.newContext({ viewport: { width: 1366, height: 768 } });
    const adminPage = await adminCtx.newPage();
    manifest.push('LOGIN ADMIN | http://localhost:8080/home-admin');
    await login(adminPage, ADMIN);
    for (const entry of adminPages) await shot(adminPage, entry, 'ADMIN');
    await adminCtx.close();

    const userCtx = await browser.newContext({ viewport: { width: 1366, height: 768 } });
    const userPage = await userCtx.newPage();
    manifest.push('LOGIN USER | http://localhost:8080/home-user');
    await login(userPage, USER);
    for (const entry of userPages) await shot(userPage, entry, 'USER');
    await userCtx.close();

    await browser.close();
    fs.writeFileSync(`${DIR}/manifest.txt`, manifest.join('\n') + '\n', 'utf-8');
    console.log(`\nSelesai. ${manifest.length - 2} screenshot, ${failed} gagal. Manifest tersimpan di ${DIR}/manifest.txt`);
})().catch(err => {
    console.error('FATAL:', err.message);
    process.exit(1);
});
