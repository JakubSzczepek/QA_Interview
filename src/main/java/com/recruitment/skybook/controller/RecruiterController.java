package com.recruitment.skybook.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * Server-side gate for recruiter content.
 * The HTML content is ONLY returned after password verification.
 * Password is stored as SHA-256 hash — never in plaintext.
 * This prevents AI crawlers/scrapers from reading bug details from page source.
 */
@RestController
@RequestMapping("/api/v1/recruiter")
@RequiredArgsConstructor
@Hidden
public class RecruiterController {

    // SHA-256 hash — plaintext password is NEVER stored in source code
    private static final String PASSWORD_HASH = "b608aa8b2ce145be28d7f38735a4be4f61941cbc0c9b8006c406c4c172e04a48";

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody Map<String, String> body) {
        String password = body.get("password");
        if (password != null && PASSWORD_HASH.equals(sha256(password))) {
            return ResponseEntity.ok(Map.of("content", getRecruiterHtml()));
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Nieprawidłowe hasło"));
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Recruiter-only content. Generated server-side, never in page source.
     */
    private String getRecruiterHtml() {
        return """
          <div class="tip-box" style="border-color: rgba(243,139,168,0.4); background: rgba(243,139,168,0.06);">
            ⚠ <strong>Dokument poufny</strong> — NIE pokazuj kandydatowi. Zawiera listę bugów, kryteria oceny i wskazówki.
          </div>

          <h3>1. Przygotowanie przed rozmową</h3>
          <table>
            <thead><tr><th>Krok</th><th>Opis</th></tr></thead>
            <tbody>
              <tr><td><strong>5 min przed</strong></td><td>Uruchom aplikację / zrób "ping" na API</td></tr>
              <tr><td><strong>Restart DB</strong></td><td>Zrestartuj usługę, aby zresetować dane do stanu początkowego</td></tr>
              <tr><td><strong>Materiały</strong></td><td>Kandydat wyświetla zakładkę „📋 Zadanie" na stronie głównej</td></tr>
              <tr><td><strong>Narzędzia</strong></td><td>Kandydat potrzebuje tylko przeglądarki — REST Client jest wbudowany</td></tr>
            </tbody>
          </table>

          <h3>2. Przebieg zadania (30 min)</h3>
          <table>
            <thead><tr><th>Czas</th><th>Faza</th><th>Notatki</th></tr></thead>
            <tbody>
              <tr><td>0–2 min</td><td><strong>Wprowadzenie</strong></td><td>„SkyBook to API rezerwacji lotów. Na stronie głównej masz REST Client z gotowymi requestami. Jest też Swagger. Kliknij „Zadanie" — tam są reguły biznesowe. Przetestuj i zaraportuj bugi."</td></tr>
              <tr><td>2–35 min</td><td><strong>Testowanie</strong></td><td>Obserwuj podejście. Czy ma strategię? Odpowiadaj na pytania o kontekst, ale nie naprowadzaj na bugi.</td></tr>
              <tr><td>35–45 min</td><td><strong>Podsumowanie</strong></td><td>Kandydat prezentuje bugi. Notuj jakość raportów.</td></tr>
            </tbody>
          </table>

          <h4>Pytania follow-up (opcjonalne):</h4>
          <ol>
            <li>„Jakie testy zautomatyzowałbyś w pierwszej kolejności?"</li>
            <li>„Jak zorganizowałbyś regression test suite dla tego API?"</li>
            <li>„Jakie testy niefunkcjonalne byś dodał?" (performance, security)</li>
            <li>„Jak podszedłbyś do testowania tego API w CI/CD pipeline?"</li>
            <li>„Jak przetestowałbyś poprawność kalkulacji cenowych?"</li>
          </ol>

          <h3>3. Lista osadzonych bugów (12)</h3>

          <h4>🔴 Krytyczne (4)</h4>
          <table class="bugs-table">
            <thead><tr><th>ID</th><th>Reguła</th><th>Bug</th><th>Reprodukcja</th></tr></thead>
            <tbody>
              <tr><td class="bug-id">BUG-01</td><td>BR-13</td><td><strong>Duplikaty miejsc</strong> — ten sam seatNumber przypisany wielu pasażerom</td><td><code>POST /api/v1/bookings</code> — dwóch pasażerów z tym samym seatNumber "12A"</td></tr>
              <tr><td class="bug-id">BUG-02</td><td>BR-16</td><td><strong>DELETE lotu z rezerwacjami</strong> — lot z bookings może być usunięty</td><td><code>DELETE /api/v1/flights/2</code> → 204, ale booking z flightId:2 osierocony</td></tr>
              <tr><td class="bug-id">BUG-03</td><td>BR-10</td><td><strong>Cancel nie zwalnia miejsc</strong> — anulowanie nie przywraca availableSeats</td><td>Sprawdź availableSeats przed/po <code>PATCH .../status</code> → CANCELLED</td></tr>
              <tr><td class="bug-id">BUG-04</td><td>BR-11</td><td><strong>Pusta tablica passengers[]</strong> — akceptowana bez walidacji</td><td><code>POST /api/v1/bookings</code> z <code>"passengers": []</code> → 200</td></tr>
            </tbody>
          </table>

          <h4>🟠 Średnie (5)</h4>
          <table class="bugs-table">
            <thead><tr><th>ID</th><th>Reguła</th><th>Bug</th><th>Reprodukcja</th></tr></thead>
            <tbody>
              <tr><td class="bug-id">BUG-05</td><td>BR-06</td><td><strong>Ujemna kwota podatku</strong> — taxes[].amount &lt; 0 akceptowany</td><td><code>POST /api/v1/flights</code> z <code>"amount": -50</code> w taxes</td></tr>
              <tr><td class="bug-id">BUG-06</td><td>BR-09</td><td><strong>Discount > 100%</strong> — percentage poza 0–100</td><td><code>POST /api/v1/flights</code> z <code>"percentage": 200</code></td></tr>
              <tr><td class="bug-id">BUG-07</td><td>BR-17</td><td><strong>EUR→GBP kurs = 1.0</strong> — błędny przelicznik</td><td><code>GET .../convert?amount=100&amp;from=EUR&amp;to=GBP</code> → 100.00 zamiast ~86.14</td></tr>
              <tr><td class="bug-id">BUG-08</td><td>BR-08</td><td><strong>totalAmount ignoruje fees</strong> — brakuje opłat w kalkulacji</td><td>Porównaj ręcznie: baseFare + taxes + fees − discount vs. zwrócone totalAmount</td></tr>
              <tr><td class="bug-id">BUG-09</td><td>BR-20</td><td><strong>POST → 200 zamiast 201</strong> — oba endpointy POST</td><td><code>POST /api/v1/flights</code> lub <code>/bookings</code> → HTTP 200</td></tr>
            </tbody>
          </table>

          <h4>🟡 Niskie (3)</h4>
          <table class="bugs-table">
            <thead><tr><th>ID</th><th>Reguła</th><th>Bug</th><th>Reprodukcja</th></tr></thead>
            <tbody>
              <tr><td class="bug-id">BUG-10</td><td>BR-19</td><td><strong>Case-sensitive search</strong> — wyszukiwanie rozróżnia wielkość liter</td><td><code>GET .../search?destination=frankfurt</code> → 0 wyników vs <code>Frankfurt</code> → wyniki</td></tr>
              <tr><td class="bug-id">BUG-11</td><td>BR-20</td><td><strong>Puste body {} → 500</strong> — Internal Server Error</td><td><code>POST /api/v1/bookings</code> z body <code>{}</code></td></tr>
              <tr><td class="bug-id">BUG-12</td><td>BR-14</td><td><strong>null payment → 500</strong> — crash zamiast walidacji</td><td><code>POST /api/v1/bookings</code> z <code>"payment": null</code></td></tr>
            </tbody>
          </table>

          <h3>4. Mapa głębokości testowania</h3>
          <table>
            <thead><tr><th>Poziom</th><th>Opis</th><th>Bugi</th><th>Typowe znalezienie</th></tr></thead>
            <tbody>
              <tr><td><strong>Root</strong></td><td>Status codes, edge cases</td><td>BUG-09, BUG-11</td><td>Łatwe — widać od razu</td></tr>
              <tr><td><strong>Level 1</strong></td><td>Zagnieżdżone obiekty</td><td>BUG-04, BUG-10, BUG-12</td><td>Wymaga świadomego testowania</td></tr>
              <tr><td><strong>Level 2</strong></td><td>Obiekty w obiektach</td><td>BUG-01, BUG-05, BUG-06, BUG-08</td><td>Wymaga analizy payload</td></tr>
              <tr><td><strong>Cross-entity</strong></td><td>Relacje lot ↔ booking</td><td>BUG-02, BUG-03</td><td>Wymaga testowania lifecycle</td></tr>
              <tr><td><strong>External</strong></td><td>Przeliczanie walut</td><td>BUG-07</td><td>Wymaga weryfikacji matematycznej</td></tr>
            </tbody>
          </table>

          <h3>5. Kryteria oceny</h3>

          <h4>5.1 Podejście do testowania (0–10 pkt)</h4>
          <table>
            <tbody>
              <tr><td><strong>0–3</strong></td><td>Losowe klikanie, brak struktury</td></tr>
              <tr><td><strong>4–6</strong></td><td>Happy path + negatywne na poziomie root</td></tr>
              <tr><td><strong>7–8</strong></td><td>Boundary values, equivalence partitioning, nested obiekty</td></tr>
              <tr><td><strong>9–10</strong></td><td>CRUD lifecycle, cross-entity, paginacja, deep nested + currency</td></tr>
            </tbody>
          </table>

          <h4>5.2 Znalezione bugi (0–10 pkt)</h4>
          <table>
            <tbody>
              <tr><td><strong>0–2</strong></td><td>0–2 bugi, przypadkowe</td></tr>
              <tr><td><strong>3–4</strong></td><td>3–4 bugi, root/Level 1</td></tr>
              <tr><td><strong>5–6</strong></td><td>5–7 bugów, mix krytycznych + średnich</td></tr>
              <tr><td><strong>7–8</strong></td><td>8–10, cross-entity + kalkulacje</td></tr>
              <tr><td><strong>9–10</strong></td><td>11–12, pełna kategoryzacja</td></tr>
            </tbody>
          </table>

          <h4>5.3 Raportowanie bugów (0–10 pkt)</h4>
          <table>
            <tbody>
              <tr><td><strong>0–3</strong></td><td>„Coś nie działa" — brak szczegółów</td></tr>
              <tr><td><strong>4–6</strong></td><td>Kroki reprodukcji, ale brak expected/actual</td></tr>
              <tr><td><strong>7–8</strong></td><td>Pełny raport: steps, expected, actual, severity</td></tr>
              <tr><td><strong>9–10</strong></td><td>+ root cause, edge cases, impact biznesowy</td></tr>
            </tbody>
          </table>

          <h4>5.4 Narzędzia i techniki (0–5 pkt)</h4>
          <table>
            <tbody>
              <tr><td><strong>0–1</strong></td><td>Tylko REST Client bez modyfikacji</td></tr>
              <tr><td><strong>2–3</strong></td><td>Modyfikuje requesty, Swagger "Try it out"</td></tr>
              <tr><td><strong>4–5</strong></td><td>+ Postman/curl, scripting, walidacja kalkulacji</td></tr>
            </tbody>
          </table>

          <h4>5.5 Komunikacja (0–5 pkt)</h4>
          <table>
            <tbody>
              <tr><td><strong>0–2</strong></td><td>Pracuje w ciszy</td></tr>
              <tr><td><strong>3–4</strong></td><td>Opisuje co robi i dlaczego</td></tr>
              <tr><td><strong>5</strong></td><td>Proaktywnie pyta, priorytetyzuje, argumentuje severity</td></tr>
            </tbody>
          </table>

          <h3>6. Arkusz punktacji</h3>
          <table class="scorecard">
            <thead><tr><th>Kategoria</th><th>Max</th><th>Punkty</th></tr></thead>
            <tbody>
              <tr><td>Podejście do testowania</td><td>10</td><td class="score-cell"></td></tr>
              <tr><td>Znalezione bugi</td><td>10</td><td class="score-cell"></td></tr>
              <tr><td>Raportowanie bugów</td><td>10</td><td class="score-cell"></td></tr>
              <tr><td>Narzędzia i techniki</td><td>5</td><td class="score-cell"></td></tr>
              <tr><td>Komunikacja</td><td>5</td><td class="score-cell"></td></tr>
              <tr><td><strong>SUMA</strong></td><td><strong>40</strong></td><td class="score-cell"></td></tr>
            </tbody>
          </table>

          <table>
            <tbody>
              <tr><td><strong>0–15</strong></td><td>❌ Nie spełnia wymagań</td></tr>
              <tr><td><strong>16–24</strong></td><td>⚠ Poniżej oczekiwań</td></tr>
              <tr><td><strong>25–32</strong></td><td>✅ Spełnia oczekiwania (Senior QA)</td></tr>
              <tr><td><strong>33–40</strong></td><td>🌟 Powyżej oczekiwań</td></tr>
            </tbody>
          </table>

          <h3>Checklist bugów</h3>
          <div class="checklist">
            <label class="check-item"><input type="checkbox" /> <span class="bug-id">BUG-01</span> 🔴 Duplikaty miejsc</label>
            <label class="check-item"><input type="checkbox" /> <span class="bug-id">BUG-02</span> 🔴 DELETE lotu z rezerwacjami</label>
            <label class="check-item"><input type="checkbox" /> <span class="bug-id">BUG-03</span> 🔴 Cancel nie zwalnia miejsc</label>
            <label class="check-item"><input type="checkbox" /> <span class="bug-id">BUG-04</span> 🔴 Pusta passengers[]</label>
            <label class="check-item"><input type="checkbox" /> <span class="bug-id">BUG-05</span> 🟠 Ujemna kwota podatku</label>
            <label class="check-item"><input type="checkbox" /> <span class="bug-id">BUG-06</span> 🟠 Discount > 100%</label>
            <label class="check-item"><input type="checkbox" /> <span class="bug-id">BUG-07</span> 🟠 EUR→GBP kurs = 1.0</label>
            <label class="check-item"><input type="checkbox" /> <span class="bug-id">BUG-08</span> 🟠 totalAmount ignoruje fees</label>
            <label class="check-item"><input type="checkbox" /> <span class="bug-id">BUG-09</span> 🟠 POST → 200 zamiast 201</label>
            <label class="check-item"><input type="checkbox" /> <span class="bug-id">BUG-10</span> 🟡 Case-sensitive search</label>
            <label class="check-item"><input type="checkbox" /> <span class="bug-id">BUG-11</span> 🟡 Puste body → 500</label>
            <label class="check-item"><input type="checkbox" /> <span class="bug-id">BUG-12</span> 🟡 null payment → 500</label>
          </div>
          <p style="margin-top:12px; font-size:13px;"><strong>Znalezione: ___ / 12</strong> (Critical: ___/4, Major: ___/5, Minor: ___/3)</p>

          <h3>7. Lokalizacja bugów w kodzie</h3>
          <table class="bugs-table">
            <thead><tr><th>Bug</th><th>Plik</th><th>Szczegóły</th></tr></thead>
            <tbody>
              <tr><td class="bug-id">BUG-01</td><td><code>BookingService.java</code></td><td>Brak walidacji unikalności seatNumber</td></tr>
              <tr><td class="bug-id">BUG-02</td><td><code>FlightService.java</code></td><td>deleteFlight() nie sprawdza bookings</td></tr>
              <tr><td class="bug-id">BUG-03</td><td><code>BookingService.java</code></td><td>CANCELLED nie modyfikuje availableSeats</td></tr>
              <tr><td class="bug-id">BUG-04</td><td><code>BookingService.java</code></td><td>Brak if (passengers.isEmpty())</td></tr>
              <tr><td class="bug-id">BUG-05</td><td><code>FlightService.java</code></td><td>Brak walidacji tax.amount ≥ 0</td></tr>
              <tr><td class="bug-id">BUG-06</td><td><code>FlightService.java</code></td><td>Brak walidacji discount.percentage 0–100</td></tr>
              <tr><td class="bug-id">BUG-07</td><td><code>PricingService.java</code></td><td>EUR_RATES: GBP → 1.0 zamiast 0.8614</td></tr>
              <tr><td class="bug-id">BUG-08</td><td><code>FlightService.java</code></td><td>calculateTotalAmount() pomija fees[]</td></tr>
              <tr><td class="bug-id">BUG-09</td><td><code>FlightController / BookingController</code></td><td>ResponseEntity.ok() zamiast .status(CREATED)</td></tr>
              <tr><td class="bug-id">BUG-10</td><td><code>FlightRepository.java</code></td><td>JPQL LIKE bez LOWER()</td></tr>
              <tr><td class="bug-id">BUG-11</td><td><code>BookingService + GlobalExceptionHandler</code></td><td>NPE na getPayment() gdy body={}</td></tr>
              <tr><td class="bug-id">BUG-12</td><td><code>BookingService + GlobalExceptionHandler</code></td><td>NPE na getPayment() gdy payment=null</td></tr>
            </tbody>
          </table>

          <p style="text-align:center; margin-top:24px; font-size:12px; color:var(--text-muted);">Dokument poufny — tylko dla zespołu rekrutacyjnego</p>
        """;
    }
}
