-- SKPL-F20 (usulan): sesi AI chat admin. Hapus sesi otomatis menghapus pesannya.
set role zelatan_app;

create table public.ai_chat_sessions (
  id            bigint generated always as identity primary key,
  admin_user_id integer not null references public.users (id) on delete cascade,
  title         text not null default 'Sesi baru' check (char_length(title) <= 100),
  created_at    timestamptz not null default now(),
  updated_at    timestamptz not null default now()
);
create index ai_chat_sessions_admin_user_id_updated_at_idx
  on public.ai_chat_sessions (admin_user_id, updated_at desc);

create table public.ai_chat_messages (
  id         bigint generated always as identity primary key,
  session_id bigint not null references public.ai_chat_sessions (id) on delete cascade,
  role       text not null check (role in ('USER', 'AI')),
  content    text not null,
  created_at timestamptz not null default now()
);
create index ai_chat_messages_session_id_created_at_idx
  on public.ai_chat_messages (session_id, created_at);

alter table public.ai_chat_sessions enable row level security;
alter table public.ai_chat_messages enable row level security;

reset role;
