# Asystent Krzyśka – MVP 1.1 AI

Natywna aplikacja Android: dotykasz ikony, aplikacja automatycznie słucha i przekazuje wypowiedź do warstwy AI. AI interpretuje potoczny polski, daty, względne terminy i kilka terminów w jednym zdaniu. Następnie aplikacja ustawia lokalny dokładny alarm i powiadomienie.

## Przykłady naturalnych poleceń
- „Kurde, przypomnij mi w piątek rano, a jak zapomnę to jeszcze koło drugiej, żebym napisał Bartkowi o tych plisach.”
- „Jutro po obiedzie mam zadzwonić do klienta, przypomnij mi.”
- „Za jakieś pół godziny przypomnij mi wyjąć rzeczy z auta.”
- „11 września o 9:30 i potem o 13 przypomnij mi o dokumentach.”

## Architektura
Android -> prywatny backend /interpret -> model AI -> strukturalny wynik -> lokalny AlarmManager.
Klucz API pozostaje na backendzie i nigdy nie jest zapisany w APK.
Jeśli backend jest niedostępny lub AI_BASE_URL nie jest skonfigurowany, aplikacja automatycznie używa lokalnego parsera z MVP 1.0.

## Uruchomienie backendu
1. `cd backend`
2. utwórz środowisko Python i `pip install -r requirements.txt`
3. ustaw `OPENAI_API_KEY`
4. `uvicorn server:app --host 0.0.0.0 --port 8000`
5. wystaw usługę przez HTTPS i wpisz jej adres w `app/build.gradle` jako `AI_BASE_URL`.

## Aktualizacje
Zachowujemy stale `applicationId = pl.krzysiek.assistant` i ten sam klucz podpisujący APK/AAB. Każda nowa wersja dostaje wyższy `versionCode`. Wtedy Android instaluje nową wersję NAD starą, zachowując dane aplikacji i przypomnienia.

Docelowo są dwa kanały zmian:
- zmiany „mózgu” (prompt, model, integracje, logika serwerowa) – aktualizujemy backend, bez instalowania nowego APK;
- zmiany samej aplikacji (ekrany, nowe uprawnienia, obsługa powiadomień itd.) – wydajemy nową wersję APK/AAB.

Na później przewidziane moduły: internet/search, Gmail, Google Calendar, Excel/kalendarz montaży i serwisów.

## Automatyczna kompilacja APK
Projekt zawiera workflow `.github/workflows/android-apk.yml`. Po wrzuceniu repozytorium na GitHub akcja buduje `app-debug.apk` i zapisuje go jako artefakt `Asystent-Krzyska-APK`.

## Wersja 1.2-reminders
- przypomnienia nie znikają po wyświetleniu powiadomienia,
- historia wykonanych przypomnień,
- ręczne dodawanie treści + wybór daty i godziny,
- usuwanie przypomnień z listy,
- nowa ikona aplikacji.
