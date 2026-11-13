# Remote Block (Android TV)

APK koji blokira tastere daljinskog. Posebna kombinacija tastera uključuje/isključuje blokadu.

## Kako radi
Koristi **AccessibilityService** sa `flagRequestFilterKeyEvents` — jedini zvaničan način da app
presretne tastere van sopstvenog prozora. Dok je blokada aktivna, svi tasteri se „gutaju“.
Kombinacija (sekvenca, razmak među tasterima do 3 s) radi i dok je blokada aktivna.

- Podrazumevana kombinacija: **GORE, GORE, DOLE, DOLE, OK**
- Nova kombinacija: u aplikaciji „Snimi novu kombinaciju“ (3–8 tastera)
- Opcija: propusti Vol+/Vol−/Mute tokom blokade

## Ograničenja
- **HOME** se ne može blokirati, ali opcija „Zaključaj trenutnu aplikaciju“ vraća aplikaciju koja je bila otvorena u trenutku blokade (na trenutak se vidi početni ekran).
- Isto važi za (i na nekim uređajima Power/namenski tasteri) Android ne dozvoljava da se presretne.
- Ako se aplikacija deinstalira/servis ugasi, blokade nema.

## Build
GitHub Actions → „Build RemoteBlock APK“ → Artifacts → `RemoteBlock-apk` (APK potpisan debug ključem).
Lokalno: `cd RemoteBlock && gradle assembleRelease` (JDK 17 + Android SDK).

## Instalacija
```
adb connect <IP_TV>
adb install app-release.apk
```
Uključi servis: Podešavanja → Pristupačnost → Remote Block → Uključi.
Ako TV nema taj meni, uključi preko adb:
```
adb shell settings put secure enabled_accessibility_services rs.remoteblock/rs.remoteblock.BlockService
adb shell settings put secure accessibility_enabled 1
```
