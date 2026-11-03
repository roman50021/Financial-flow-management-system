import React, { useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react';
import { createRoot } from 'react-dom/client';
import './styles.css';

const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api';
const PERIODS = [
  { id: 'DAY', label: 'День' },
  { id: 'WEEK', label: 'Тиждень' },
  { id: 'MONTH', label: 'Місяць' },
  { id: 'YEAR', label: 'Рік' },
];
const CHART_COLORS = ['#7C5CFC', '#FF5C7A', '#3B82F6', '#F59E0B', '#A855F7', '#64748B', '#EC4899'];
const MONTH_FORMATTER = new Intl.DateTimeFormat('uk-UA', { month: 'long', year: 'numeric' });
const DAY_FORMATTER = new Intl.DateTimeFormat('uk-UA', { day: 'numeric', month: 'long', year: 'numeric' });
const SHORT_DATE_FORMATTER = new Intl.DateTimeFormat('uk-UA', { day: 'numeric', month: 'short' });
const TIME_FORMATTER = new Intl.DateTimeFormat('uk-UA', { hour: '2-digit', minute: '2-digit' });

async function apiRequest(path, options = {}) {
  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers: {
      Accept: 'application/json',
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      ...options.headers,
    },
  });

  if (response.status === 204) return null;
  const data = await response.json().catch(() => null);
  if (!response.ok) {
    throw new Error(data?.message || `API повернув помилку ${response.status}`);
  }
  return data;
}

function App() {
  const [transactions, setTransactions] = useState([]);
  const [categories, setCategories] = useState([]);
  const [screen, setScreen] = useState('overview');
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [initialTransactionType, setInitialTransactionType] = useState('EXPENSE');
  const [editingTransaction, setEditingTransaction] = useState(null);
  const [message, setMessage] = useState(null);
  const [theme, setTheme] = useState(() => {
    const saved = localStorage.getItem('ffms-theme');
    if (saved === 'light' || saved === 'dark') return saved;
    return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  });

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    localStorage.setItem('ffms-theme', theme);
  }, [theme]);

  const notify = useCallback((text, tone = 'success') => {
    setMessage({ text, tone, id: Date.now() });
  }, []);

  useEffect(() => {
    if (!message) return undefined;
    const timer = window.setTimeout(() => setMessage(null), 4200);
    return () => window.clearTimeout(timer);
  }, [message]);

  const loadData = useCallback(async ({ silent = false } = {}) => {
    try {
      const [transactionData, categoryData] = await Promise.all([
        apiRequest('/transactions'),
        apiRequest('/categories'),
      ]);
      setTransactions(transactionData);
      setCategories(categoryData);
    } catch (error) {
      if (!silent) {
        notify(error.message || 'Не вдалося завантажити дані', 'error');
      }
    }
  }, [notify]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  useEffect(() => {
    const events = new EventSource(`${API_URL}/events`);
    let synchronizationTimer;
    const synchronize = () => {
      window.clearTimeout(synchronizationTimer);
      synchronizationTimer = window.setTimeout(() => loadData({ silent: true }), 80);
    };
    events.addEventListener('data-change', synchronize);

    return () => {
      window.clearTimeout(synchronizationTimer);
      events.removeEventListener('data-change', synchronize);
      events.close();
    };
  }, [loadData]);

  const balance = useMemo(
    () => transactions.reduce(
      (sum, transaction) => sum + (transaction.type === 'INCOME' ? 1 : -1) * Number(transaction.amount),
      0,
    ),
    [transactions],
  );

  function navigate(nextScreen) {
    setScreen(nextScreen);
    setDrawerOpen(false);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  function openAdd(type) {
    setEditingTransaction(null);
    setInitialTransactionType(type);
    navigate('add');
  }

  function openEdit(transaction) {
    setEditingTransaction(transaction);
    setInitialTransactionType(transaction.type);
    navigate('add');
  }

  async function saveTransaction(draft) {
    const transactionId = editingTransaction?.id;
    const saved = await apiRequest(transactionId ? `/transactions/${transactionId}` : '/transactions', {
      method: transactionId ? 'PUT' : 'POST',
      body: JSON.stringify(draft),
    });
    setTransactions((current) => transactionId
      ? current.map((item) => (item.id === saved.id ? saved : item))
      : [saved, ...current.filter((item) => item.id !== saved.id)]);
    notify(transactionId ? 'Операцію оновлено' : 'Операцію додано');
    setEditingTransaction(null);
    navigate('overview');
  }

  async function deleteTransaction(id) {
    try {
      await apiRequest(`/transactions/${id}`, { method: 'DELETE' });
      setTransactions((current) => current.filter((item) => item.id !== id));
      notify('Операцію видалено');
    } catch (error) {
      notify(error.message, 'error');
    }
  }

  async function saveCategory(categoryId, draft) {
    const saved = await apiRequest(categoryId ? `/categories/${categoryId}` : '/categories', {
      method: categoryId ? 'PUT' : 'POST',
      body: JSON.stringify({ ...draft, userId: null }),
    });
    setCategories((current) => {
      const next = categoryId
        ? current.map((item) => (item.id === categoryId ? saved : item))
        : [...current.filter((item) => item.id !== saved.id), saved];
      return next.sort((left, right) => left.name.localeCompare(right.name, 'uk'));
    });
    if (categoryId) {
      setTransactions((current) => current.map((item) => (
        item.categoryId === categoryId ? { ...item, categoryName: saved.name } : item
      )));
    }
    notify(categoryId ? 'Категорію оновлено' : 'Категорію створено');
    return saved;
  }

  async function deleteCategory(category) {
    await apiRequest(`/categories/${category.id}`, { method: 'DELETE' });
    setCategories((current) => current.filter((item) => item.id !== category.id));
    notify('Категорію видалено');
  }

  return (
    <div className="app-shell">
      <Drawer
        open={drawerOpen}
        screen={screen}
        balance={balance}
        theme={theme}
        onClose={() => setDrawerOpen(false)}
        onNavigate={navigate}
        onThemeToggle={() => setTheme((current) => (current === 'dark' ? 'light' : 'dark'))}
      />

      {screen === 'overview' && (
        <Dashboard
          transactions={transactions}
          onOpenMenu={() => setDrawerOpen(true)}
          onAdd={openAdd}
          onEdit={openEdit}
          onDelete={deleteTransaction}
        />
      )}

      {screen === 'categories' && (
        <CategoriesScreen
          categories={categories}
          onOpenMenu={() => setDrawerOpen(true)}
          onSave={saveCategory}
          onDelete={deleteCategory}
          onError={(error) => notify(error.message || String(error), 'error')}
        />
      )}

      {screen === 'add' && (
        <AddTransactionScreen
          categories={categories}
          initialType={initialTransactionType}
          transaction={editingTransaction}
          onBack={() => { setEditingTransaction(null); navigate('overview'); }}
          onManageCategories={() => navigate('categories')}
          onSave={saveTransaction}
        />
      )}

      {message && (
        <div className={`snackbar ${message.tone}`} role="status" aria-live="polite">
          {message.text}
        </div>
      )}
    </div>
  );
}

function Drawer({ open, screen, balance, theme, onClose, onNavigate, onThemeToggle }) {
  const navItems = [
    { id: 'overview', icon: '⌂', label: 'Огляд' },
    { id: 'categories', icon: '▦', label: 'Категорії' },
  ];

  return (
    <>
      <button
        className={`drawer-scrim ${open ? 'visible' : ''}`}
        aria-label="Закрити меню"
        tabIndex={open ? 0 : -1}
        onClick={onClose}
      />
      <aside className={`drawer ${open ? 'open' : ''}`} aria-hidden={!open}>
        <div className="drawer-brand">
          <div className="brand-mark">₴</div>
          <div>
            <strong>Мої фінанси</strong>
            <span>{formatMoney(balance)}</span>
          </div>
          <button className="icon-button drawer-close" onClick={onClose} aria-label="Закрити меню">×</button>
        </div>

        <nav className="drawer-nav" aria-label="Головна навігація">
          {navItems.map((item) => (
            <button
              key={item.id}
              className={`drawer-item ${screen === item.id ? 'active' : ''}`}
              onClick={() => onNavigate(item.id)}
            >
              <span className="drawer-icon">{item.icon}</span>
              {item.label}
            </button>
          ))}

          <p className="drawer-section-label">Майбутні функції</p>
          {[
            ['▤', 'Бюджети'],
            ['↻', 'Регулярні платежі'],
            ['◷', 'Нагадування'],
            ['⚙', 'Налаштування'],
          ].map(([icon, label]) => (
            <button className="drawer-item muted" key={label} disabled>
              <span className="drawer-icon">{icon}</span>
              {label}
            </button>
          ))}
        </nav>

        <div className="drawer-footer">
          <button className="drawer-item" onClick={onThemeToggle}>
            <span className="drawer-icon">{theme === 'dark' ? '☀' : '◐'}</span>
            {theme === 'dark' ? 'Світла тема' : 'Темна тема'}
          </button>
          <span>Financial Flow · web v2.0</span>
        </div>
      </aside>
    </>
  );
}

function Dashboard({ transactions, onOpenMenu, onAdd, onEdit, onDelete }) {
  const [type, setType] = useState('EXPENSE');
  const [period, setPeriod] = useState('MONTH');
  const [periodOffset, setPeriodOffset] = useState(0);
  const pointerStart = useRef(null);

  const balance = useMemo(
    () => transactions.reduce(
      (sum, transaction) => sum + (transaction.type === 'INCOME' ? 1 : -1) * Number(transaction.amount),
      0,
    ),
    [transactions],
  );
  const range = useMemo(() => periodRange(period, periodOffset), [period, periodOffset]);
  const currentTransactions = useMemo(
    () => transactions.filter((transaction) => (
      transaction.type === type
      && transaction.transactionDate >= toIsoDate(range.start)
      && transaction.transactionDate <= toIsoDate(range.end)
    )),
    [transactions, type, range],
  );
  const slices = useMemo(() => groupTransactions(currentTransactions), [currentTransactions]);
  const total = useMemo(
    () => currentTransactions.reduce((sum, transaction) => sum + Number(transaction.amount), 0),
    [currentTransactions],
  );

  function choosePeriod(nextPeriod) {
    setPeriod(nextPeriod);
    setPeriodOffset(0);
  }

  function handlePointerDown(event) {
    pointerStart.current = event.clientX;
  }

  function handlePointerUp(event) {
    if (pointerStart.current === null) return;
    const distance = event.clientX - pointerStart.current;
    pointerStart.current = null;
    if (Math.abs(distance) < 48) return;
    setPeriodOffset((current) => Math.min(0, current + (distance < 0 ? 1 : -1)));
  }

  return (
    <main className="page dashboard-page">
      <header className="dashboard-header">
        <button className="icon-button menu-button" onClick={onOpenMenu} aria-label="Відкрити меню">☰</button>
        <div className="wallet-heading">
          <span>Основний гаманець</span>
          <strong>{formatMoney(balance)}</strong>
        </div>

        <div className="type-tabs" role="tablist">
          <TypeTab active={type === 'EXPENSE'} onClick={() => setType('EXPENSE')}>ВИТРАТИ</TypeTab>
          <TypeTab active={type === 'INCOME'} onClick={() => setType('INCOME')}>ДОХОДИ</TypeTab>
        </div>
      </header>

      <div className="content-wrap">
        <div className="period-tabs" aria-label="Період аналітики">
          {PERIODS.map((item) => (
            <button
              key={item.id}
              className={period === item.id ? 'active' : ''}
              onClick={() => choosePeriod(item.id)}
            >
              {item.label}
            </button>
          ))}
        </div>

        <section className="analytics-layout">
          <article
            className="chart-card"
            onPointerDown={handlePointerDown}
            onPointerUp={handlePointerUp}
            onPointerCancel={() => { pointerStart.current = null; }}
          >
            <div className="period-navigation">
              <button className="icon-button" onClick={() => setPeriodOffset((current) => current - 1)} aria-label="Попередній період">‹</button>
              <h2>{formatRange(period, range)}</h2>
              {periodOffset < 0 ? (
                <button className="icon-button" onClick={() => setPeriodOffset((current) => Math.min(0, current + 1))} aria-label="Наступний період">›</button>
              ) : (
                <span aria-hidden="true" />
              )}
            </div>

            <div className="chart-stage">
              <DonutChart slices={slices} total={total} type={type} />
              <button className="fab" onClick={() => onAdd(type)} aria-label="Додати операцію">+</button>
            </div>
            <p className="swipe-hint">Свайпніть або використайте стрілки, щоб змінити період</p>
          </article>

          <CategoryBreakdown
            title={type === 'EXPENSE' ? 'За категоріями' : 'Джерела доходу'}
            subtitle={formatRange(period, range)}
            slices={slices}
            total={total}
            onAdd={() => onAdd(type)}
          />
        </section>

        <TransactionsPanel
          transactions={currentTransactions}
          type={type}
          onEdit={onEdit}
          onDelete={onDelete}
          onAdd={() => onAdd(type)}
        />
      </div>
    </main>
  );
}

function TypeTab({ active, onClick, children }) {
  return (
    <button className={active ? 'active' : ''} onClick={onClick} role="tab" aria-selected={active}>
      {children}
    </button>
  );
}

function DonutChart({ slices, total, type }) {
  const background = useMemo(() => {
    if (!slices.length || total <= 0) return 'conic-gradient(var(--chart-track) 0 100%)';
    let cursor = 0;
    const segments = slices.map((slice) => {
      const start = cursor;
      cursor += (slice.amount / total) * 100;
      return `${slice.color} ${start}% ${cursor}%`;
    });
    return `conic-gradient(${segments.join(', ')})`;
  }, [slices, total]);

  return (
    <div className="donut" style={{ '--donut-background': background }} role="img" aria-label={`${typeLabel(type)} ${formatMoney(total)}`}>
      <div className="donut-center">
        <span>{type === 'EXPENSE' ? 'Витрати' : 'Доходи'}</span>
        <AutoFitAmount value={total} type={type} />
      </div>
    </div>
  );
}

function AutoFitAmount({ value, type }) {
  const amountRef = useRef(null);
  const formattedValue = formatMoney(value);

  useLayoutEffect(() => {
    const amount = amountRef.current;
    const container = amount?.parentElement;
    if (!amount || !container) return undefined;

    const fitAmount = () => {
      amount.style.removeProperty('font-size');
      const maximumSize = Number.parseFloat(window.getComputedStyle(amount).fontSize);
      const availableWidth = container.clientWidth;
      const measuredWidth = amount.scrollWidth;

      if (measuredWidth > availableWidth) {
        const fittedSize = Math.max(12, Math.floor(maximumSize * availableWidth / measuredWidth));
        amount.style.fontSize = `${fittedSize}px`;
      }
    };

    fitAmount();
    const observer = new ResizeObserver(fitAmount);
    observer.observe(container);
    return () => observer.disconnect();
  }, [formattedValue]);

  return (
    <strong ref={amountRef} className={type.toLowerCase()}>
      {formattedValue}
    </strong>
  );
}

function CategoryBreakdown({ title, subtitle, slices, total, onAdd }) {
  return (
    <section className="breakdown-panel">
      <div className="section-heading">
        <h2>{title}</h2>
        <span>{subtitle}</span>
      </div>
      {!slices.length ? (
        <EmptyState
          title="Операцій за цей період немає"
          description="Додайте операцію — графік оновиться автоматично"
          action="Додати операцію"
          onAction={onAdd}
        />
      ) : (
        <div className="category-list">
          {slices.map((slice) => (
            <article className="category-summary" key={slice.key}>
              <span className="category-color" style={{ '--slice-color': slice.color }} />
              <strong>{slice.name}</strong>
              <span>{Math.round((slice.amount / total) * 100)}%</span>
              <b>{formatMoney(slice.amount)}</b>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

function TransactionsPanel({ transactions, type, onEdit, onDelete, onAdd }) {
  return (
    <section className="transactions-panel">
      <div className="section-heading">
        <h2>Операції періоду</h2>
        <span>{formatOperationCount(transactions.length)}</span>
      </div>
      {!transactions.length ? (
        <EmptyState
          title={type === 'EXPENSE' ? 'Витрат немає' : 'Доходів немає'}
          description="Створіть першу операцію для вибраного періоду"
          action="Додати операцію"
          onAction={onAdd}
        />
      ) : (
        <div className="transaction-list">
          {transactions.map((transaction) => {
            const customTitle = transaction.title?.trim();
            const title = customTitle || transaction.categoryName || 'Операція';
            const metadata = [
              customTitle ? transaction.categoryName : null,
              formatShortDate(transaction.transactionDate),
              formatTransactionTime(transaction.createdAt),
            ]
              .filter(Boolean)
              .join(' · ');
            return (
              <article className="transaction-row" key={transaction.id}>
                <div className="transaction-copy">
                  <strong>{title}</strong>
                  <span>{metadata}</span>
                </div>
                <b className={transaction.type.toLowerCase()}>{formatMoney(transaction.amount)}</b>
                <div className="transaction-actions">
                  <button className="edit-button" onClick={() => onEdit(transaction)} aria-label={`Редагувати ${title}`}>✎</button>
                  <button className="delete-button" onClick={() => onDelete(transaction.id)} aria-label={`Видалити ${title}`}>×</button>
                </div>
              </article>
            );
          })}
        </div>
      )}
    </section>
  );
}

function CategoriesScreen({ categories, onOpenMenu, onSave, onDelete, onError }) {
  const [type, setType] = useState('EXPENSE');
  const [dialog, setDialog] = useState(null);
  const [pendingDelete, setPendingDelete] = useState(null);
  const [saving, setSaving] = useState(false);
  const visibleCategories = useMemo(
    () => categories.filter((category) => category.type === type).sort((a, b) => a.name.localeCompare(b.name, 'uk')),
    [categories, type],
  );

  async function handleSave(draft) {
    setSaving(true);
    try {
      await onSave(dialog?.category?.id, draft);
      setDialog(null);
      setType(draft.type);
    } catch (error) {
      onError(error);
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete() {
    setSaving(true);
    try {
      await onDelete(pendingDelete);
      setPendingDelete(null);
    } catch (error) {
      onError(error);
    } finally {
      setSaving(false);
    }
  }

  return (
    <main className="page inner-page">
      <PageHeader
        title="Категорії"
        subtitle="Організуйте витрати та доходи"
        onMenu={onOpenMenu}
        action={<button className="primary-button compact" onClick={() => setDialog({ category: null, type })}>+ Додати</button>}
      />
      <div className="content-wrap narrow-content categories-content">
        <div className="type-switch">
          <button className={type === 'EXPENSE' ? 'active' : ''} onClick={() => setType('EXPENSE')}>Витрати</button>
          <button className={type === 'INCOME' ? 'active' : ''} onClick={() => setType('INCOME')}>Доходи</button>
        </div>

        {!visibleCategories.length ? (
          <EmptyState
            title="Категорій поки немає"
            description={`Створіть першу категорію для ${type === 'EXPENSE' ? 'витрат' : 'доходів'}`}
            action="Створити категорію"
            onAction={() => setDialog({ category: null, type })}
          />
        ) : (
          <div className="category-management-grid">
            {visibleCategories.map((category) => (
              <article className="category-card" key={category.id}>
                <span className="category-monogram" style={{ '--slice-color': category.color || fallbackCategoryColor(category.id) }}>
                  {category.name.slice(0, 1).toUpperCase()}
                </span>
                <div>
                  <strong>{category.name}</strong>
                  <span>{type === 'EXPENSE' ? 'Категорія витрат' : 'Категорія доходів'}</span>
                </div>
                <button className="text-button" onClick={() => setDialog({ category, type: category.type })}>Змінити</button>
                <button className="delete-button" onClick={() => setPendingDelete(category)} aria-label={`Видалити ${category.name}`}>×</button>
              </article>
            ))}
          </div>
        )}
      </div>

      {dialog && (
        <CategoryDialog
          category={dialog.category}
          initialType={dialog.type}
          saving={saving}
          onClose={() => setDialog(null)}
          onSave={handleSave}
        />
      )}
      {pendingDelete && (
        <ConfirmDialog
          title="Видалити категорію?"
          description={`Категорію «${pendingDelete.name}» буде видалено. Використані категорії API захищає від видалення.`}
          loading={saving}
          onCancel={() => setPendingDelete(null)}
          onConfirm={handleDelete}
        />
      )}
    </main>
  );
}

function AddTransactionScreen({ categories, initialType, transaction, onBack, onManageCategories, onSave }) {
  const isEditing = Boolean(transaction);
  const [type, setType] = useState(transaction?.type || initialType);
  const [amount, setAmount] = useState(transaction ? String(transaction.amount) : '');
  const [title, setTitle] = useState(transaction?.title || '');
  const [description, setDescription] = useState(transaction?.description || '');
  const [date, setDate] = useState(transaction?.transactionDate || toIsoDate(new Date()));
  const [categoryId, setCategoryId] = useState(transaction?.categoryId ? String(transaction.categoryId) : '');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const matchingCategories = useMemo(
    () => categories.filter((category) => category.type === type).sort((a, b) => a.name.localeCompare(b.name, 'uk')),
    [categories, type],
  );

  useEffect(() => {
    if (!matchingCategories.some((category) => String(category.id) === String(categoryId))) {
      setCategoryId(matchingCategories[0] ? String(matchingCategories[0].id) : '');
    }
  }, [matchingCategories, categoryId]);

  async function submit(event) {
    event.preventDefault();
    setError('');
    const parsedAmount = Number(String(amount).replace(',', '.'));
    if (!Number.isFinite(parsedAmount) || parsedAmount <= 0) {
      setError('Вкажіть суму більшу за нуль');
      return;
    }
    if (!categoryId) {
      setError('Оберіть категорію операції');
      return;
    }
    if (!date) {
      setError('Оберіть дату операції');
      return;
    }

    setSaving(true);
    try {
      await onSave({
        title: title.trim() || null,
        description: description.trim() || null,
        amount: parsedAmount,
        type,
        transactionDate: date,
        categoryId: Number(categoryId),
        userId: null,
      });
    } catch (saveError) {
      setError(saveError.message || 'Не вдалося зберегти операцію');
      setSaving(false);
    }
  }

  return (
    <main className="page inner-page">
      <PageHeader
        title={isEditing ? 'Редагування операції' : 'Нова операція'}
        subtitle={isEditing ? 'Виправте потрібні дані та збережіть зміни' : 'Оберіть категорію, вкажіть суму та збережіть'}
        onBack={onBack}
      />
      <form className="transaction-form content-wrap narrow-content" onSubmit={submit}>
        <div className="type-switch">
          <button type="button" className={type === 'EXPENSE' ? 'active' : ''} onClick={() => setType('EXPENSE')}>Витрата</button>
          <button type="button" className={type === 'INCOME' ? 'active' : ''} onClick={() => setType('INCOME')}>Дохід</button>
        </div>

        <FormSection title="Основне">
          <label className="field amount-field">
            <span>Сума, ₴</span>
            <input
              value={amount}
              onChange={(event) => { setAmount(event.target.value.replace(/[^0-9.,]/g, '')); setError(''); }}
              inputMode="decimal"
              placeholder="0,00"
              autoFocus
            />
          </label>
          <label className="field">
            <span>Назва <small>необов’язково</small></span>
            <input value={title} onChange={(event) => setTitle(event.target.value)} placeholder="Наприклад, обід із друзями" />
          </label>
        </FormSection>

        <FormSection title="Категорія">
          {matchingCategories.length ? (
            <div className="category-chips">
              {matchingCategories.map((category) => (
                <button
                  type="button"
                  key={category.id}
                  className={String(category.id) === String(categoryId) ? 'active' : ''}
                  onClick={() => { setCategoryId(String(category.id)); setError(''); }}
                >
                  {category.name}
                </button>
              ))}
            </div>
          ) : (
            <div className="no-categories">
              <p>Для цього типу ще немає категорій.</p>
              <button type="button" className="secondary-button" onClick={onManageCategories}>Створити категорію</button>
            </div>
          )}
        </FormSection>

        <FormSection title="Деталі">
          <label className="field">
            <span>Дата</span>
            <input type="date" value={date} onChange={(event) => setDate(event.target.value)} required />
            <small>Автоматично встановлено сьогодні</small>
          </label>
          <label className="field">
            <span>Опис <small>необов’язково</small></span>
            <textarea value={description} onChange={(event) => setDescription(event.target.value)} placeholder="Додаткова інформація" />
          </label>
        </FormSection>

        {error && <p className="form-error" role="alert">{error}</p>}
        <button className="primary-button save-operation" type="submit" disabled={saving || !matchingCategories.length}>
          {saving ? 'Зберігаємо…' : isEditing ? 'Зберегти зміни' : 'Зберегти операцію'}
        </button>
      </form>
    </main>
  );
}

function PageHeader({ title, subtitle, onMenu, onBack, action }) {
  return (
    <header className="page-header">
      <button className="icon-button" onClick={onBack || onMenu} aria-label={onBack ? 'Назад' : 'Відкрити меню'}>
        {onBack ? '←' : '☰'}
      </button>
      <div>
        <h1>{title}</h1>
        <p>{subtitle}</p>
      </div>
      <div className="header-action">{action}</div>
    </header>
  );
}

function FormSection({ title, children }) {
  return (
    <section className="form-section">
      <h2>{title}</h2>
      {children}
    </section>
  );
}

function CategoryDialog({ category, initialType, saving, onClose, onSave }) {
  const [name, setName] = useState(category?.name || '');
  const [type, setType] = useState(category?.type || initialType);
  const [error, setError] = useState('');

  function submit(event) {
    event.preventDefault();
    const normalized = name.trim();
    if (!normalized) {
      setError('Вкажіть назву категорії');
      return;
    }
    onSave({ name: normalized, type });
  }

  return (
    <Modal onClose={onClose}>
      <form className="dialog" onSubmit={submit}>
        <div className="dialog-heading">
          <div>
            <h2>{category ? 'Редагувати категорію' : 'Нова категорія'}</h2>
            <p>Категорії використовуються для автоматичної аналітики</p>
          </div>
          <button type="button" className="icon-button" onClick={onClose} aria-label="Закрити">×</button>
        </div>
        <label className="field">
          <span>Назва категорії</span>
          <input value={name} onChange={(event) => { setName(event.target.value); setError(''); }} autoFocus />
        </label>
        <div className="type-switch">
          <button type="button" className={type === 'EXPENSE' ? 'active' : ''} onClick={() => setType('EXPENSE')}>Витрати</button>
          <button type="button" className={type === 'INCOME' ? 'active' : ''} onClick={() => setType('INCOME')}>Доходи</button>
        </div>
        {error && <p className="form-error">{error}</p>}
        <div className="dialog-actions">
          <button type="button" className="secondary-button" onClick={onClose}>Скасувати</button>
          <button className="primary-button" disabled={saving}>{saving ? 'Зберігаємо…' : 'Зберегти'}</button>
        </div>
      </form>
    </Modal>
  );
}

function ConfirmDialog({ title, description, loading, onCancel, onConfirm }) {
  return (
    <Modal onClose={onCancel}>
      <div className="dialog confirm-dialog">
        <h2>{title}</h2>
        <p>{description}</p>
        <div className="dialog-actions">
          <button className="secondary-button" onClick={onCancel}>Скасувати</button>
          <button className="danger-button" onClick={onConfirm} disabled={loading}>{loading ? 'Видаляємо…' : 'Видалити'}</button>
        </div>
      </div>
    </Modal>
  );
}

function Modal({ children, onClose }) {
  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={(event) => {
      if (event.target === event.currentTarget) onClose();
    }}>
      <div role="dialog" aria-modal="true">{children}</div>
    </div>
  );
}

function EmptyState({ title, description, action, onAction }) {
  return (
    <div className="empty-state">
      <div className="empty-symbol">₴</div>
      <strong>{title}</strong>
      <p>{description}</p>
      {action && <button className="primary-button compact" onClick={onAction}>{action}</button>}
    </div>
  );
}

function groupTransactions(transactions) {
  const groups = new Map();
  transactions.forEach((transaction) => {
    const key = transaction.categoryId ?? `uncategorized-${transaction.type}`;
    const existing = groups.get(key) || {
      key,
      name: transaction.categoryName || 'Без категорії',
      amount: 0,
      color: transaction.categoryColor,
    };
    existing.amount += Number(transaction.amount);
    groups.set(key, existing);
  });
  return [...groups.values()]
    .sort((left, right) => right.amount - left.amount)
    .map((slice, index) => ({
      ...slice,
      color: slice.color || fallbackCategoryColor(slice.key, index),
    }));
}

function fallbackCategoryColor(categoryId, fallbackIndex = 0) {
  const numericId = Number(categoryId);
  const index = Number.isInteger(numericId) && numericId > 0
    ? (numericId - 1) % CHART_COLORS.length
    : fallbackIndex % CHART_COLORS.length;
  return CHART_COLORS[index];
}

function periodRange(period, offset) {
  const now = startOfDay(new Date());
  if (period === 'DAY') {
    const date = addDays(now, offset);
    return { start: date, end: date };
  }
  if (period === 'WEEK') {
    const day = now.getDay() || 7;
    const monday = addDays(now, 1 - day + offset * 7);
    return { start: monday, end: addDays(monday, 6) };
  }
  if (period === 'YEAR') {
    const year = now.getFullYear() + offset;
    return { start: new Date(year, 0, 1), end: new Date(year, 11, 31) };
  }
  const start = new Date(now.getFullYear(), now.getMonth() + offset, 1);
  return { start, end: new Date(start.getFullYear(), start.getMonth() + 1, 0) };
}

function formatRange(period, range) {
  if (period === 'DAY') return capitalize(DAY_FORMATTER.format(range.start));
  if (period === 'MONTH') return capitalize(MONTH_FORMATTER.format(range.start));
  if (period === 'YEAR') return `${range.start.getFullYear()} рік`;
  const sameMonth = range.start.getMonth() === range.end.getMonth();
  if (sameMonth) {
    const monthYear = new Intl.DateTimeFormat('uk-UA', { month: 'long', year: 'numeric' }).format(range.end);
    return `${range.start.getDate()}–${range.end.getDate()} ${monthYear}`;
  }
  return `${SHORT_DATE_FORMATTER.format(range.start)} – ${SHORT_DATE_FORMATTER.format(range.end)} ${range.end.getFullYear()}`;
}

function startOfDay(date) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

function addDays(date, days) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate() + days);
}

function toIsoDate(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function parseIsoDate(value) {
  const [year, month, day] = value.split('-').map(Number);
  return new Date(year, month - 1, day);
}

function formatShortDate(value) {
  return SHORT_DATE_FORMATTER.format(parseIsoDate(value));
}

function formatTransactionTime(value) {
  if (!value) return null;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : TIME_FORMATTER.format(date);
}

function capitalize(value) {
  return value.charAt(0).toUpperCase() + value.slice(1);
}

function typeLabel(type) {
  return type === 'EXPENSE' ? 'Витрати' : 'Доходи';
}

function formatOperationCount(count) {
  const mod10 = count % 10;
  const mod100 = count % 100;
  if (mod10 === 1 && mod100 !== 11) return `${count} операція`;
  if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return `${count} операції`;
  return `${count} операцій`;
}

function formatMoney(value) {
  return new Intl.NumberFormat('uk-UA', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  }).format(Number(value || 0)) + '\u00A0₴';
}

createRoot(document.getElementById('root')).render(<App />);
