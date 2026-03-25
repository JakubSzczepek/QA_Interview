/* ============================================================
   SkyBook REST Client — Application Logic
   ============================================================ */

// ---- Task Modal ----
function toggleTaskModal() {
  document.getElementById('taskModal').classList.toggle('open');
}
function closeTaskModal(e) {
  if (e.target === e.currentTarget) toggleTaskModal();
}
document.addEventListener('keydown', e => {
  if (e.key === 'Escape') {
    document.getElementById('taskModal').classList.remove('open');
    document.getElementById('recruiterModal').classList.remove('open');
  }
});

// ---- Recruiter Modal (password-protected) ----
function openRecruiterModal() {
  document.getElementById('recruiterModal').classList.add('open');
  const gate = document.getElementById('recruiterGate');
  if (gate.style.display !== 'none') {
    setTimeout(() => document.getElementById('recruiterPassword').focus(), 100);
  }
}
function closeRecruiterModal(e) {
  if (e.target === e.currentTarget) document.getElementById('recruiterModal').classList.remove('open');
}
async function checkRecruiterPassword() {
  const input = document.getElementById('recruiterPassword');
  const error = document.getElementById('gateError');
  const btn = document.querySelector('.gate-btn');
  btn.disabled = true;
  btn.textContent = '...';
  try {
    const res = await fetch('/api/v1/recruiter/verify', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ password: input.value })
    });
    if (res.ok) {
      const data = await res.json();
      document.getElementById('recruiterGate').style.display = 'none';
      const el = document.getElementById('recruiterContent');
      el.innerHTML = data.content;
      el.style.display = 'block';
      error.textContent = '';
    } else {
      error.textContent = '\u274c Nieprawid\u0142owe has\u0142o';
      input.value = '';
      input.focus();
    }
  } catch (e) {
    console.error('[recruiter]', e);
    error.textContent = '\u274c B\u0142\u0105d po\u0142\u0105czenia';
  } finally {
    btn.disabled = false;
    btn.textContent = 'Odblokuj';
  }
}

// ---- Pre-filled endpoint definitions ----
const ENDPOINTS = {
  // --- Flights ---
  getAllFlights: {
    method: 'GET',
    url: '/api/v1/flights?page=0&size=10',
    body: null,
    label: 'GET All Flights'
  },
  getFlightById: {
    method: 'GET',
    url: '/api/v1/flights/1',
    body: null,
    label: 'GET Flight by ID'
  },
  createFlight: {
    method: 'POST',
    url: '/api/v1/flights',
    body: {
      flightNumber: "SB-1234",
      airline: "SkyBook Airlines",
      status: "SCHEDULED",
      segments: [
        {
          segmentNumber: 1,
          departure: {
            airportCode: "WAW",
            airportName: "Warsaw Chopin Airport",
            terminal: "A",
            dateTime: "2026-06-15T08:30:00"
          },
          arrival: {
            airportCode: "FRA",
            airportName: "Frankfurt Airport",
            terminal: "1",
            dateTime: "2026-06-15T10:45:00"
          },
          aircraft: {
            model: "Boeing 737-800",
            registration: "SP-LKE",
            seatConfiguration: {
              economy: 162,
              business: 12,
              first: 0
            }
          },
          durationMinutes: 135
        }
      ],
      pricing: {
        currency: "EUR",
        baseFare: 120.00,
        taxes: [
          { code: "YQ", name: "Fuel Surcharge", amount: 45.00 },
          { code: "PL", name: "Passenger Service Fee", amount: 20.50 }
        ],
        fees: [
          { feeCode: "BAG", description: "Checked Baggage", amount: 30.00, optional: true }
        ],
        discount: null
      },
      availableSeats: {
        economy: 150,
        business: 10,
        first: 0
      },
      tags: ["direct", "europe", "morning"]
    },
    label: 'POST Create Flight'
  },
  updateFlight: {
    method: 'PUT',
    url: '/api/v1/flights/1',
    body: {
      flightNumber: "SB-1234",
      airline: "SkyBook Airlines",
      status: "DELAYED",
      segments: [
        {
          segmentNumber: 1,
          departure: {
            airportCode: "WAW",
            airportName: "Warsaw Chopin Airport",
            terminal: "A",
            dateTime: "2026-06-15T09:00:00"
          },
          arrival: {
            airportCode: "FRA",
            airportName: "Frankfurt Airport",
            terminal: "1",
            dateTime: "2026-06-15T11:15:00"
          },
          aircraft: {
            model: "Boeing 737-800",
            registration: "SP-LKE",
            seatConfiguration: {
              economy: 162,
              business: 12,
              first: 0
            }
          },
          durationMinutes: 135
        }
      ],
      pricing: {
        currency: "EUR",
        baseFare: 120.00,
        taxes: [
          { code: "YQ", name: "Fuel Surcharge", amount: 45.00 },
          { code: "PL", name: "Passenger Service Fee", amount: 20.50 }
        ],
        fees: [
          { feeCode: "BAG", description: "Checked Baggage", amount: 30.00, optional: true }
        ],
        discount: {
          code: "SUMMER10",
          percentage: 10,
          validUntil: "2026-08-31"
        }
      },
      availableSeats: {
        economy: 148,
        business: 10,
        first: 0
      },
      tags: ["direct", "europe", "delayed"]
    },
    label: 'PUT Update Flight'
  },
  deleteFlight: {
    method: 'DELETE',
    url: '/api/v1/flights/1',
    body: null,
    label: 'DELETE Flight'
  },
  searchFlights: {
    method: 'GET',
    url: '/api/v1/flights/search?origin=Warsaw&destination=Frankfurt&page=0&size=10',
    body: null,
    label: 'GET Search Flights'
  },

  // --- Bookings ---
  createBooking: {
    method: 'POST',
    url: '/api/v1/bookings',
    body: {
      flightId: 2,
      passengers: [
        {
          personalInfo: {
            firstName: "Jan",
            lastName: "Kowalski",
            dateOfBirth: "1990-05-15",
            nationality: "PL"
          },
          contact: {
            email: "jan.kowalski@example.com",
            phone: "+48501234567"
          },
          seatAssignment: {
            seatNumber: "12A",
            "class": "ECONOMY",
            type: "WINDOW"
          },
          baggage: [
            { type: "CHECKED", weightKg: 23, count: 1 },
            { type: "CABIN", weightKg: 8, count: 1 }
          ],
          specialRequests: ["VEGETARIAN_MEAL"]
        }
      ],
      payment: {
        method: "CREDIT_CARD",
        amount: 185.50,
        currency: "EUR",
        cardDetails: {
          number: "4111111111111111",
          holderName: "Jan Kowalski",
          brand: "VISA",
          expiryDate: "12/28",
          cvv: "123"
        }
      }
    },
    label: 'POST Create Booking'
  },
  getBookingById: {
    method: 'GET',
    url: '/api/v1/bookings/1',
    body: null,
    label: 'GET Booking by ID'
  },
  patchBookingStatus: {
    method: 'PATCH',
    url: '/api/v1/bookings/1/status',
    body: {
      status: "CONFIRMED"
    },
    label: 'PATCH Booking Status'
  },

  // --- Airports ---
  getAllAirports: {
    method: 'GET',
    url: '/api/v1/airports',
    body: null,
    label: 'GET All Airports'
  },

  // --- Pricing ---
  convertCurrency: {
    method: 'GET',
    url: '/api/v1/pricing/convert?amount=100&from=EUR&to=GBP',
    body: null,
    label: 'GET Currency Convert'
  },
  getExchangeRates: {
    method: 'GET',
    url: '/api/v1/pricing/rates',
    body: null,
    label: 'GET Exchange Rates'
  }
};

// ---- State ----
let currentEndpoint = 'getAllFlights';
let history = [];

// ---- DOM refs ----
const methodSelect = document.getElementById('methodSelect');
const urlInput = document.getElementById('urlInput');
const bodyEditor = document.getElementById('bodyEditor');
const bodySection = document.getElementById('bodySection');
const responseStatus = document.getElementById('responseStatus');
const responseTime = document.getElementById('responseTime');
const responseBody = document.getElementById('responseBody');
const sendBtn = document.getElementById('sendBtn');
const historyItems = document.getElementById('historyItems');

// ---- Initialize ----
document.addEventListener('DOMContentLoaded', () => {
  // Bind sidebar clicks
  document.querySelectorAll('.sidebar-items li').forEach(li => {
    li.addEventListener('click', () => selectEndpoint(li.dataset.endpoint));
  });

  // Allow Ctrl+Enter to send
  document.addEventListener('keydown', e => {
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
      e.preventDefault();
      sendRequest();
    }
  });

  // Tab in textarea inserts spaces
  bodyEditor.addEventListener('keydown', e => {
    if (e.key === 'Tab') {
      e.preventDefault();
      const start = bodyEditor.selectionStart;
      const end = bodyEditor.selectionEnd;
      const val = bodyEditor.value;
      bodyEditor.value = val.substring(0, start) + '  ' + val.substring(end);
      bodyEditor.selectionStart = bodyEditor.selectionEnd = start + 2;
    }
  });

  // Load initial endpoint
  selectEndpoint('getAllFlights');
});

// ---- Sidebar ----
function selectEndpoint(key) {
  currentEndpoint = key;
  const ep = ENDPOINTS[key];
  if (!ep) return;

  // Update active state
  document.querySelectorAll('.sidebar-items li').forEach(li => {
    li.classList.toggle('active', li.dataset.endpoint === key);
  });

  // Update request fields
  methodSelect.value = ep.method;
  urlInput.value = ep.url;

  if (ep.body !== null && ep.body !== undefined) {
    bodySection.classList.remove('hidden');
    bodyEditor.value = JSON.stringify(ep.body, null, 2);
  } else {
    bodySection.classList.add('hidden');
    bodyEditor.value = '';
  }

  // Clear response
  responseStatus.textContent = '';
  responseStatus.className = 'response-status';
  responseTime.textContent = '';
  responseBody.innerHTML = '<span class="placeholder">Click "Send" to see response...</span>';
}

function toggleGroup(titleEl) {
  titleEl.parentElement.classList.toggle('collapsed');
}

// ---- Send Request ----
async function sendRequest() {
  const method = methodSelect.value;
  const url = urlInput.value;

  if (!url) return;

  sendBtn.classList.add('loading');
  sendBtn.textContent = '...';
  responseBody.innerHTML = '<span class="placeholder">Loading...</span>';
  responseStatus.textContent = '';
  responseTime.textContent = '';

  const options = {
    method,
    headers: { 'Content-Type': 'application/json' }
  };

  if (['POST', 'PUT', 'PATCH'].includes(method) && bodyEditor.value.trim()) {
    options.body = bodyEditor.value;
  }

  const startTime = performance.now();

  try {
    const response = await fetch(url, options);
    const elapsed = Math.round(performance.now() - startTime);
    const statusCode = response.status;
    const statusText = response.statusText;

    // Read response body
    let body = '';
    const contentType = response.headers.get('content-type') || '';
    if (contentType.includes('application/json')) {
      try {
        const json = await response.json();
        body = JSON.stringify(json, null, 2);
      } catch {
        body = await response.text();
      }
    } else {
      body = await response.text();
    }

    // Display response
    displayResponse(statusCode, statusText, elapsed, body);

    // Add to history
    addToHistory(method, url, statusCode);

  } catch (err) {
    const elapsed = Math.round(performance.now() - startTime);
    displayResponse(0, 'Error', elapsed, `Network error: ${err.message}`);
    addToHistory(method, url, 0);
  } finally {
    sendBtn.classList.remove('loading');
    sendBtn.textContent = 'Send ▶';
  }
}

// ---- Display Response ----
function displayResponse(statusCode, statusText, elapsed, body) {
  // Status badge
  responseStatus.textContent = `${statusCode} ${statusText}`;
  responseStatus.className = 'response-status';
  if (statusCode >= 200 && statusCode < 300) responseStatus.classList.add('s2xx');
  else if (statusCode >= 300 && statusCode < 400) responseStatus.classList.add('s3xx');
  else if (statusCode >= 400 && statusCode < 500) responseStatus.classList.add('s4xx');
  else responseStatus.classList.add('s5xx');

  // Time
  responseTime.textContent = `${elapsed}ms`;

  // Body with syntax highlighting
  responseBody.innerHTML = syntaxHighlight(body);
}

// ---- Syntax Highlighting ----
function syntaxHighlight(json) {
  if (!json) return '';
  // Escape HTML
  const escaped = json.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  // Color tokens
  return escaped.replace(
    /("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?)/g,
    match => {
      let cls = 'json-number';
      if (/^"/.test(match)) {
        cls = /:$/.test(match) ? 'json-key' : 'json-string';
      } else if (/true|false/.test(match)) {
        cls = 'json-boolean';
      } else if (/null/.test(match)) {
        cls = 'json-null';
      }
      return `<span class="${cls}">${match}</span>`;
    }
  );
}

// Add inline styles for syntax highlighting (injected into head)
const styleSheet = document.createElement('style');
styleSheet.textContent = `
  .json-key    { color: #89b4fa; }
  .json-string { color: #a6e3a1; }
  .json-number { color: #fab387; }
  .json-boolean{ color: #cba6f7; }
  .json-null   { color: #6c7086; font-style: italic; }
`;
document.head.appendChild(styleSheet);

// ---- Format Body ----
function formatBody() {
  const raw = bodyEditor.value.trim();
  if (!raw) return;
  try {
    const parsed = JSON.parse(raw);
    bodyEditor.value = JSON.stringify(parsed, null, 2);
  } catch (e) {
    alert('Invalid JSON: ' + e.message);
  }
}

// ---- History ----
function addToHistory(method, url, statusCode) {
  // Extract path from URL
  const path = url.split('?')[0];

  history.unshift({ method, path, url, statusCode, time: new Date() });
  if (history.length > 20) history.pop();

  renderHistory();
}

function renderHistory() {
  if (history.length === 0) {
    historyItems.innerHTML = '<span class="history-empty">No requests yet</span>';
    return;
  }

  historyItems.innerHTML = history.map((item, i) => {
    const statusClass = item.statusCode >= 200 && item.statusCode < 300 ? 's2xx'
      : item.statusCode >= 400 && item.statusCode < 500 ? 's4xx'
      : item.statusCode >= 500 ? 's5xx' : '';
    return `<div class="history-item" onclick="replayHistory(${i})" title="${item.method} ${item.url}">
      <span class="method">${item.method}</span>
      <span class="path">${item.path}</span>
      <span class="status ${statusClass}">${item.statusCode || 'ERR'}</span>
    </div>`;
  }).join('');
}

function replayHistory(index) {
  const item = history[index];
  if (!item) return;
  methodSelect.value = item.method;
  urlInput.value = item.url;
  // Don't change body — keep current
}

function clearHistory() {
  history = [];
  renderHistory();
}
