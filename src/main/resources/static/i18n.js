// i18n.js — SkyBook API bilingual support (PL / EN)
(function () {
  'use strict';

  // ── Translations ──────────────────────────────────────────────────────────────
  const STRINGS = {
    pl: {
      // Page titles
      'page.title.main':    'SkyBook API — REST Client',
      'page.title.logs':    'SkyBook API — Logi',
      'page.title.starter': 'SkyBook API — Test Starter Kit',

      // Topbar (shared)
      'nav.task':            '📋 Zadanie',
      'nav.task.title':      'Opis zadania',
      'nav.recruiter':       '🔒 Rekruter',
      'nav.recruiter.title': 'Arkusz rekrutera',
      'nav.logs':            '📊 Logi',
      'nav.logs.title':      'Logi API (RQ/RS)',
      'nav.starter.title':   'Test Starter Kit — szkielet projektu testowego',

      'task.writeTests':       'Napisz testy automatyczne',

      // Recruiter modal gate
      'recruiter.modal.title':    '🔒 Arkusz rekrutera (POUFNE)',
      'recruiter.modal.close':    'Zamknij',
      'recruiter.gate.intro':     'Ten panel jest przeznaczony wyłącznie dla rekrutera.',
      'recruiter.gate.prompt':    'Podaj hasło dostępu:',
      'recruiter.gate.placeholder': 'Hasło...',
      'recruiter.gate.btn':       'Odblokuj',
      'recruiter.unlock':         'Odblokuj',
      'err.wrongPassword':        '❌ Nieprawidłowe hasło',
      'err.connection':           '❌ Błąd połączenia',

      // Task modal close
      'modal.task.close': 'Zamknij',

      // Logs page
      'logs.brand.suffix':      '— Logi',
      'logs.btn.refresh':       '↻ Odśwież',
      'logs.btn.refresh.title': 'Odśwież',
      'logs.btn.clear':         '🗑 Wyczyść',
      'logs.btn.clear.title':   'Wyczyść logi',
      'logs.auto.off':          '⏱ Auto: OFF',
      'logs.auto.on':           '⏱ Auto: ON (2s)',
      'logs.empty':             'Brak logów — wyślij request z REST Clienta',
      'logs.no.results':        'Brak wyników dla aktywnych filtrów',
      'logs.clear.confirm':     'Wyczyścić wszystkie logi?',
      'logs.connect.error':     'Błąd połączenia z API',
      'logs.last.refresh':      'Ostatnie odświeżenie: ',
      'logs.max.entries':       'Max 500 wpisów w pamięci',

      // Starter page
      'starter.heading':    '🧪 Test Starter Kit',
      'starter.subtitle':   'Gotowy szkielet projektu do automatycznych testów API. Skopiuj pliki do swojego IDE, zamień <code style="font-family:var(--font-mono);color:var(--warning)">BASE_URL</code> na adres API i zacznij pisać testy.',
      'starter.step1.num':  'Krok 1',
      'starter.step1.desc': 'Skopiuj <code>pom.xml</code> do nowego projektu Maven w IDE',
      'starter.step2.num':  'Krok 2',
      'starter.step2.desc': 'Utwórz pakiet <code>com.recruitment.skybook</code> w <code>src/test/java</code>',
      'starter.step3.num':  'Krok 3',
      'starter.step3.desc': 'Wklej <code>BaseTest.java</code> i <code>SkyBookApiTest.java</code>',
      'starter.step4.num':  'Krok 4',
      'starter.step4.desc': 'Uzupełnij testy dla znalezionych bugów i uruchom: <code>mvn test</code>',
      'starter.tip.label':  'Wskazówka:',
      'starter.tip.body':   '<code>BaseTest.java</code> konfiguruje wspólny <code>RequestSpecification</code> z base URL, content-type i accept header. Każda klasa testowa rozszerza <code>BaseTest</code> i korzysta z pola <code>spec</code>. Jeden przykładowy test w <code>SkyBookApiTest.java</code> pokazuje wzorzec — Twoim zadaniem jest dopisanie testów dla znalezionych bugów.',
      'starter.copy':       '📋 Kopiuj',
      'starter.copied':     '✅ Skopiowano',
    },

    en: {
      // Page titles
      'page.title.main':    'SkyBook API — REST Client',
      'page.title.logs':    'SkyBook API — Logs',
      'page.title.starter': 'SkyBook API — Test Starter Kit',

      // Topbar (shared)
      'nav.task':            '📋 Task',
      'nav.task.title':      'Task description',
      'nav.recruiter':       '🔒 Recruiter',
      'nav.recruiter.title': 'Recruiter sheet',
      'nav.logs':            '📊 Logs',
      'nav.logs.title':      'API Logs (RQ/RS)',
      'nav.starter.title':   'Test Starter Kit — test project skeleton',

      'task.writeTests':       'Write automated tests',

      // Recruiter modal gate
      'recruiter.modal.title':    '🔒 Recruiter Sheet (CONFIDENTIAL)',
      'recruiter.modal.close':    'Close',
      'recruiter.gate.intro':     'This panel is for the recruiter only.',
      'recruiter.gate.prompt':    'Enter access password:',
      'recruiter.gate.placeholder': 'Password...',
      'recruiter.gate.btn':       'Unlock',
      'recruiter.unlock':         'Unlock',
      'err.wrongPassword':        '❌ Invalid password',
      'err.connection':           '❌ Connection error',

      // Task modal close
      'modal.task.close': 'Close',

      // Logs page
      'logs.brand.suffix':      '— Logs',
      'logs.btn.refresh':       '↻ Refresh',
      'logs.btn.refresh.title': 'Refresh',
      'logs.btn.clear':         '🗑 Clear',
      'logs.btn.clear.title':   'Clear logs',
      'logs.auto.off':          '⏱ Auto: OFF',
      'logs.auto.on':           '⏱ Auto: ON (2s)',
      'logs.empty':             'No logs — send a request from REST Client',
      'logs.no.results':        'No results for active filters',
      'logs.clear.confirm':     'Clear all logs?',
      'logs.connect.error':     'API connection error',
      'logs.last.refresh':      'Last refresh: ',
      'logs.max.entries':       'Max 500 entries in memory',

      // Starter page
      'starter.heading':    '🧪 Test Starter Kit',
      'starter.subtitle':   'Ready-to-use test project skeleton for API automation. Copy the files to your IDE, replace <code style="font-family:var(--font-mono);color:var(--warning)">BASE_URL</code> with the API address and start writing tests.',
      'starter.step1.num':  'Step 1',
      'starter.step1.desc': 'Copy <code>pom.xml</code> to a new Maven project in your IDE',
      'starter.step2.num':  'Step 2',
      'starter.step2.desc': 'Create package <code>com.recruitment.skybook</code> in <code>src/test/java</code>',
      'starter.step3.num':  'Step 3',
      'starter.step3.desc': 'Paste <code>BaseTest.java</code> and <code>SkyBookApiTest.java</code>',
      'starter.step4.num':  'Step 4',
      'starter.step4.desc': 'Complete tests for bugs you found and run: <code>mvn test</code>',
      'starter.tip.label':  'Tip:',
      'starter.tip.body':   '<code>BaseTest.java</code> configures a shared <code>RequestSpecification</code> with base URL, content-type and accept header. Each test class extends <code>BaseTest</code> and uses the <code>spec</code> field. One sample test in <code>SkyBookApiTest.java</code> shows the pattern — your task is to add tests for the bugs you discover.',
      'starter.copy':       '📋 Copy',
      'starter.copied':     '✅ Copied',
    }
  };

  // ── State ─────────────────────────────────────────────────────────────────────
  let currentLang = localStorage.getItem('skybook-lang') || 'pl';

  // ── Public API (set synchronously so app.js can call window.i18n.t() immediately)
  function t(key) {
    return (STRINGS[currentLang] || STRINGS.pl)[key] || key;
  }

  function locale() {
    return currentLang === 'pl' ? 'pl-PL' : 'en-US';
  }

  function setLang(lang) {
    currentLang = lang;
    localStorage.setItem('skybook-lang', lang);
    applyI18n();
    window.dispatchEvent(new CustomEvent('langChanged', { detail: { lang } }));
  }

  window.i18n = { t, setLang, locale };

  // ── DOM helpers ───────────────────────────────────────────────────────────────
  function applyI18n() {
    // Plain text
    document.querySelectorAll('[data-i18n]').forEach(el => {
      el.textContent = t(el.dataset.i18n);
    });
    // HTML content (subtitles with <code> etc.)
    document.querySelectorAll('[data-i18n-html]').forEach(el => {
      el.innerHTML = t(el.dataset.i18nHtml);
    });
    // title attribute
    document.querySelectorAll('[data-i18n-title]').forEach(el => {
      el.title = t(el.dataset.i18nTitle);
    });
    // placeholder attribute
    document.querySelectorAll('[data-i18n-placeholder]').forEach(el => {
      el.placeholder = t(el.dataset.i18nPlaceholder);
    });
    // Show/hide language blocks
    document.querySelectorAll('[data-lang]').forEach(el => {
      el.style.display = el.dataset.lang === currentLang ? '' : 'none';
    });
    // Page <title>
    const titleKey = document.documentElement.dataset.i18nTitle;
    if (titleKey) document.title = t(titleKey);
    // <html lang> attribute
    document.documentElement.lang = currentLang;
    // Sync toggle buttons
    document.querySelectorAll('[data-lang-btn]').forEach(btn => {
      btn.classList.toggle('active', btn.dataset.langBtn === currentLang);
    });
  }

  // ── Language toggle widget ────────────────────────────────────────────────────
  function injectStyles() {
    if (document.getElementById('i18n-styles')) return;
    const style = document.createElement('style');
    style.id = 'i18n-styles';
    style.textContent = [
      '.lang-toggle{display:flex;background:var(--bg-surface);border:1px solid var(--border);border-radius:4px;overflow:hidden;}',
      '.lang-toggle span{padding:3px 9px;font-size:12px;font-weight:600;cursor:pointer;color:var(--text-muted);transition:all .15s;user-select:none;}',
      '.lang-toggle span:hover{color:var(--text-primary);background:var(--bg-hover);}',
      '.lang-toggle span.active{background:var(--accent);color:var(--bg-primary);}'
    ].join('');
    document.head.appendChild(style);
  }

  function injectToggle() {
    const container = document.querySelector('.topbar-links');
    if (!container || container.querySelector('.lang-toggle')) return;
    const wrap = document.createElement('div');
    wrap.className = 'lang-toggle';
    ['pl', 'en'].forEach(lang => {
      const span = document.createElement('span');
      span.textContent = lang.toUpperCase();
      span.dataset.langBtn = lang;
      span.addEventListener('click', () => setLang(lang));
      wrap.appendChild(span);
    });
    container.insertBefore(wrap, container.firstChild);
  }

  // ── Init ──────────────────────────────────────────────────────────────────────
  document.addEventListener('DOMContentLoaded', () => {
    injectStyles();
    injectToggle();
    applyI18n();
  });
})();
