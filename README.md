# Sinclair Terrel 5.3 Remote (Hybryda Wi-Fi + IR)

Aplikacja na Androida odwzorowująca biały pilot **YAC1FB9** klimatyzatora **Sinclair Terrel 5.3 kW (SIH-18BIT)** z przełącznikiem:
- **Wi-Fi LAN** – dwukierunkowe sterowanie lokalne (protokół Gree UDP port 7000, obsługa AES-128-ECB oraz nowszego AES-128-GCM, odczyt temperatury z czujnika w jednostce, zero chmury EWPE Smart).
- **IR Pilot** – sterowanie przez wbudowaną diodę podczerwieni (np. Honor Magic 6 Pro, 38 kHz, 67-bitowa ramka Gree).

## Jak skompilować plik `.apk` w GitHubie (bez Android Studio)

1. Utwórz nowe repozytorium na GitHubie (może być **Private**), np. `sinclair-terrel-remote`.
2. Rozpakuj archiwum `SinclairTerrelRemote-Android.zip` i wrzuć wszystkie pliki do głównego katalogu repozytorium (pamiętaj o ukrytym folderze `.github/workflows/build-apk.yml`!).
3. Wejdź w zakładkę **Actions** w repozytorium na GitHubie.
4. Poczekaj ok. 2–3 minuty na zakończenie zadania **Build Sinclair Terrel Remote APK** (albo kliknij **Run workflow**).
5. Kliknij zakończony build, zjedź do sekcji **Artifacts** i pobierz **SinclairTerrelRemote-APK** (w środku jest plik `app-debug.apk`).
6. Skopiuj `app-debug.apk` na telefon i zainstaluj.

## Pierwsze uruchomienie
- **Podczerwień (IR)**: przełącz górny suwak na `IR Pilot` i wyceluj telefonem w klimatyzator – działa od razu.
- **Wi-Fi LAN**: upewnij się, że telefon jest w tej samej domowej sieci Wi-Fi co klimatyzator (wcześniej dodany do sieci Wi-Fi przez EWPE Smart), kliknij ikonę zębatki w prawym górnym rogu i wybierz **Skanuj sieć Wi-Fi automatycznie** (lub wpisz adres IP klimatyzatora z routera). Po jednorazowym sparowaniu aplikacja zapamięta klucz urządzenia.
