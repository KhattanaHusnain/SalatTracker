# Future Features — Istiqamah

_Researched and compiled: 2026-10-18. Updated: 2026-10-22 — the offline Quran reader and prayer-time accuracy features shipped._

Ideas for Istiqamah's next features. The document covers:
- how the app compares with other prayer and habit apps
- what users complain about in those apps
- features others have that we lack
- new ideas no competitor offers
- a phased roadmap

**How to read this:**
- Competitor features come from public app store listings, developer sites and review roundups. The developers describe these features themselves; they weren't checked by installing the apps.
- Effort estimates are rough, for one developer:
  - **S**: a few days
  - **M**: one to two weeks
  - **L**: several weeks, or it needs a backend
- **Fit** means how much existing code a feature can reuse.

---

## 1. Where Istiqamah stands today

This was checked against the code, not taken from memory.

| Area | What exists | Gap found in the code |
|---|---|---|
| Prayer times | Calculated on the phone with `adhan-kotlin` (`com.batoulapps.adhan:adhan2`) — the AlAdhan network call is gone. 23 calculation methods, a school-of-thought setting (Standard/Hanafi/**Ja'fari**), a high-latitude rule, per-prayer minute offsets, and Jumu'ah awareness (Friday Dhuhr is labelled and can be treated separately). | None of the coordinates leave the device anymore. Ja'fari (Shia) users get Zuhrain/Maghribain labelling and matching Asr/Isha times — this goes beyond what any competitor reviewed offers (see 2.2). |
| Prayer logging | The 5 obligatory prayers, logged as `CONGREGATION`, `INDIVIDUAL`, `QAZA_PENDING`, `QAZA_COMPLETED`, `MENSTRUATION` or `NOT_RECORDED`. Bulk update for catching up on history. | There are no sunnah or voluntary prayers. There's no on-time vs. late distinction, because `updatedAt` changes on every edit. Jumu'ah has no special handling. |
| Qaza | A backlog dashboard and per-prayer pending counts (`QazaBalanceEntity`). | There's no calculator for estimating years of missed prayers, and no make-up plan or projection. |
| Goals | Categories `SALAT`, `QURAN`, `FASTING`, `CHARITY` and `DHIKR`. Goals are daily, weekly or monthly, with numeric check-ins. | These are generic counters. There's no dedicated tasbih counter, fasting log or khatam planner. |
| Stats | Vico charts and a per-prayer breakdown. | There's no year-long heatmap and no badges. |
| Calendar | Shows Hijri dates through the `HijriDate` util. | The Hijri date isn't used for any reminders yet. |
| Reminders | A device sound, snooze, full-screen alert and vibration. | There's no bundled azan audio, and no Friday or sunnah reminders. |
| Mosque Mode | Geofenced auto-vibrate, nearby search through OpenStreetMap, background location. | Unique in the market. It isn't connected to prayer logging yet. |
| Content | A verse of the day from the Quran.com API, plus a full offline Quran reader: Uthmani Arabic, a Nastaliq Urdu translation, Surah and Juz browsing, printed Mushaf pages, grid lines, Juz-bounded page navigation, and a saved resume position. | There's no hadith, dua or adhkar content, Quran audio/recitation playback, or a user-selectable translation/tafsir catalogue yet. |
| Platform | Google Drive backup. English, Arabic, French and Urdu, with right-to-left layout for Arabic and Urdu; Urdu-localized labels use the same Nastaliq typeface as the Quran translation. Dark mode. Donate and Developers screens. | There's no widget, no Wear OS app and no Qibla compass. |

---

## 2. Competitor landscape

### 2.1 Apps reviewed

| App | Highlights |
|---|---|
| Muslim Pro | Market leader. Azan with several muezzin voices, full Quran, Qibla, tasbih, Hijri calendar, duas, 99 Names, community features. Ad-supported, with a premium tier. |
| Pillars | Privacy-first, no ads, minimal permissions. |
| Al Taqweem | Free and ad-free. Full Quran, Qibla compass, tasbih. |
| PrayPath | Offline Qibla, 12-month streak heatmap, tasbih. |
| A Simple Salah | Streaks with grace days, home-screen and lock-screen widgets, works offline. |
| Prayer Tracker & Daily Hadith | Streaks, 9 badges, Qibla, widget, daily hadith. |
| Salat Now | Tasbih with goals, Qibla with haptic feedback. |
| Salah Tracker: Namaz & Qibla | Qibla, Quran, tasbih, habit streaks. Has a name very close to ours. |
| Prayer Pause | Prayer times, Qibla, focus mode that silences or blocks the phone, on-time vs. late streaks (premium). |
| My Sujoods | Consistency charts, heatmaps, qaza calculator and tracking. |
| FivePrayer / Khushu / DeenFocus | Lock or block the phone during prayer time. |
| Mizan / HalalYouNeed / Everyday Muslim | Private prayer groups, family leaderboards, shared progress. |
| Muslim Habits Tracker / Niyyah / Muslim Daily | Track sunnah rawatib, witr, tahajjud, duha, fasting and adhkar. |
| rakaah.ai / Sahw / 2026 Sujood App | Count rak'ahs with sensors on Apple Watch or iPhone. |
| Tanwir / Niyat / Ask Quran / Quran Study | AI chat companions for questions about Islam. |
| Tarteel | AI feedback on Quran recitation and memorisation. |

### 2.2 Where Istiqamah is already ahead

- **Mosque Mode.** It switches the ringer automatically near saved mosques, using geofences. None of the apps reviewed do this.
- **Qaza bulk update.** It catches up historical logs more thoroughly than most trackers do.
- **Right-to-left support for both Arabic *and* Urdu**, alongside English and French.
- **No ads, no subscription, no account.** The Donate and Contribute screens are open and direct. Most competitors rely on ads or paywalls.
- **On-device prayer-time calculation.** No coordinates sent anywhere — this is the privacy opening from 2.3 and N7, and it's already shipped, not a plan.
- **Ja'fari (Shia) school of thought.** Asr follows Dhuhr ("Zuhrain") and Isha follows Maghrib ("Maghribain"), with matching labels and times, while all 5 prayers stay independently tracked. No app reviewed in section 2.1 offers this — most of the market treats "Asr school" as a two-way Hanafi/Shafi'i choice and stops there.

### 2.3 What users complain about, and the opening for us

1. **Ads in a religious app.** Users report full-screen video ads around the adhan and banners next to Quran verses.
   - Our opening: we have no ads. Say so in the store listing.
2. **Privacy of location data.**
   - Muslim Pro was widely reported in 2020 to have passed location data to a data broker (X-Mode).
   - Pillars wins users by avoiding data collection altogether.
   - Our opening: calculate times on the device and keep data local.
3. **Inaccurate times that can't be corrected.** Users want the right method, the right Asr school and small manual adjustments.
   - Our opening: the accuracy settings in section 3A.
4. **Adhan notifications silently failing on Android**, killed by battery optimisation and the phone maker's own app-killers. We hit this ourselves while debugging Mosque Mode on a Samsung phone.
   - Our opening: a notification health check (idea N8).
5. **Religious content behind a paywall.**
   - Our opening: stay free, funded by donations.

### 2.4 Market trends

| Trend | Examples | What it means for us |
|---|---|---|
| Focus mode during prayer | FivePrayer, Khushu, Prayer Pause, DeenFocus | **High.** We already control the ringer. Don't use the Accessibility API to block apps: Google Play restricts that use, so it's a rejection risk. |
| Social and family accountability | Mizan, HalalYouNeed, Everyday Muslim, Sunnah Way | **Medium.** It needs a backend and careful handling of children's data. |
| Fuller habit tracking | Muslim Habits Tracker, Niyyah, Muslim Daily, Sunnah Helper | **High.** Tracking is our core strength. |
| AI companions | Tanwir, Niyat, Ask Quran, Mufti AI | **Low, and risky.** AI answers to fiqh (Islamic law) questions are a reputational risk. |
| Sensor-based rak'ah counting | rakaah.ai, Sahw, 2026 Sujood App | **Medium.** No Wear OS equivalent turned up, so this is an open gap on Android. |
| Ramadan features | RafiQ, Ramadan & Quran Tracker, Muslim & Quran | **Medium to high**, but seasonal. |
| Gamification | Prayer Tracker & Daily Hadith, PrayPath, A Simple Salah | **High.** We already have the data. |

---

## 3. Features others have that we lack

### A. Prayer-time accuracy and reliability

All five of these shipped on 2026-10-20, together, by switching the whole prayer-time pipeline to on-device calculation.

| Feature | Status | Notes |
|---|---|---|
| **Asr school (Hanafi/Shafi'i, plus Ja'fari)** | ✅ Done | Went further than planned: added a third option, Ja'fari (Shia), not just the planned Hanafi/Shafi'i toggle — see 2.2. |
| **High-latitude adjustment** | ✅ Done | An `AUTOMATIC` default (seventh-of-the-night above 48°, otherwise middle-of-the-night) plus manual override, mirroring `adhan2`'s `HighLatitudeRule`. |
| **Per-prayer minute offsets** | ✅ Done | Users can nudge any of the 5 prayer times to match their mosque's timetable. |
| **Prayer times calculated on the phone** | ✅ Done | `adhan-kotlin` (`com.batoulapps.adhan:adhan2`, pinned at `0.0.6` — `0.0.7` needs Kotlin 2.4.0 and breaks this project's Hilt annotation processor on Kotlin 2.3.0). The AlAdhan network call and `PrayerTimeCacheEntity` are gone; no coordinates leave the device. |
| **Jumu'ah awareness** | ✅ Done | Friday Dhuhr is labelled Jumu'ah wherever a single prayer is shown (Home, reminders), taking precedence over Ja'fari's Zuhrain label on that one day. No earlier Friday reminder yet — still open, see section E. |

**Not yet built from this section:** an earlier Jumu'ah reminder specifically (the labelling is done, the extra reminder timing is not).

### B. Core worship tools

| Feature | Effort | Fit | Notes |
|---|---|---|---|
| **Qibla compass** | S–M | Strong | Uses the phone's rotation-vector sensor plus the compass bearing to the Kaaba. `GeoDistance` already does similar maths. Show a calibration hint and a warning about magnetic interference. Almost every competitor has one. |
| **Tasbih (dhikr) counter** | S | Very strong | Reaching 33 or 100 automatically checks in to the existing `DHIKR` goals. |
| **Home-screen widget** | M | Strong | A Jetpack Glance widget with the next prayer, a countdown and one-tap logging (see N2). |
| **Azan audio** | M | Medium | Bundled recitations add to the app's size and need licensing; an optional download would avoid the size cost. |
| **Zakat calculator** | S–M | Weak | Common in all-in-one Islamic apps (Muslim Pro, premium). Rules differ by school, so the wording needs care. |
| **99 Names of Allah** | S | Weak | A static content screen. Little gain beyond matching competitors. |

### C. Daily content

| Feature | Effort | Fit | Notes |
|---|---|---|---|
| **Hadith of the day** | S | Strong | Reuse the verse-of-the-day pattern. `fawazahmed0/hadith-api` is free to reuse (Unlicense), comes in several languages and includes authenticity grades. Show the grade with each hadith. |
| **Morning and evening adhkar with reminders** | M | Medium | **Licensing is unresolved.** No clear licence turned up for Hisnul Muslim data. Confirm a content source before building. |
| **Full Quran reader** | ✅ Done | Strong | Shipped 2026-10-22 as an offline reader with Uthmani Arabic, Fateh Muhammad Jalandhry Urdu translation, Surah and Juz lists, real Mushaf page layout, Juz-bounded pagination and resume reading. Quran content sources and licences are documented in `QURAN_CONTENT_SOURCES.md`. |

### D. Deeper tracking

These build on our core strength.

| Feature | Effort | Fit | Notes |
|---|---|---|---|
| **Qaza estimator** (years missed, or age at puberty) | S | Very strong | Writes straight into the existing qaza counts. Label the result as an estimate, since the schools count differently. Some count Witr, making 6 a day. |
| **Sunnah and voluntary prayers** (rawatib, witr, tahajjud, duha) | M | Medium | Store these in a **separate table**, so streaks, day colours, qaza and backup stay unchanged. Needs a database migration and a backup format update. |
| **On-time vs. late** | M | Medium | Needs a `loggedAt` timestamp, or simply ask the user. Gives stats like "92% of Fajr on time." |
| **Fasting log** (Ramadan, sunnah fasts, missed fasts to make up) | M | Medium | Builds on the existing `FASTING` goal category. Missed Ramadan fasts would work like qaza prayers. |
| **Yearly heatmap** | S | Strong | A GitHub-style grid of each day's result, built from data that already exists. |
| **Badges** | S–M | Strong | Worked out from existing streaks and stats, for example "40 days of Fajr in congregation." Keep the tone modest. |
| **Streak grace days** | S | Strong | Missing one day doesn't reset the streak. This stops users giving up after a single slip. |

### E. Seasonal and recurring reminders

These are cheap to build because the app already converts dates to Hijri.

| Feature | Effort | Notes |
|---|---|---|
| **Sunnah fasting reminders** | S | Monday/Thursday, the 13th–15th of each Hijri month (Ayyam al-Bid), Arafah and Ashura.<br>**Skip** Eid and 11th–13th Dhul-Hijjah (the Days of Tashreeq), and don't single out Fridays.<br>Merge overlapping days into one reminder. |
| **Friday reminders** | S | Surah al-Kahf, salawat (blessings on the Prophet), and the hour of dua on Friday afternoon. |
| **Ramadan mode** | M | Suhoor and iftar countdowns, a fasting log, one-tap taraweeh logging, and a khatam planner (a target number of juz per day). |
| **Last ten nights of Ramadan** | S | Reminders on the odd nights, for Laylat al-Qadr. |

### F. Social

| Feature | Effort | Notes |
|---|---|---|
| **Private accountability groups** | L | Needs a backend, which we don't have. Only share "prayed / not prayed," and only by choice, to protect privacy. |
| **Family or kids mode** | L | Google Play's Families policy and children's privacy laws apply. A lighter option: profiles kept only on the phone. |

---

## 4. New ideas: what no competitor reviewed offers

These build on what only Istiqamah has, mainly Mosque Mode and its ringer and geofence code.

### N1. Mosque check-in that logs congregation automatically ⭐ top pick
- **What it does:** when Mosque Mode registers a dwell inside a saved mosque during a prayer window, a notification asks "Prayed Asr in congregation at Masjid Al-Noor?" One tap logs `CONGREGATION`.
- **Extra stats:** congregational prayers this month, broken down per mosque.
- **Why it's new:** no app reviewed links mosque presence to prayer logging, and we already have both halves.
- **Effort:** M.
- **Conditions:**
  - The user always confirms; just being near a mosque never logs a prayer.
  - The new use of location must go into the Google Play location declaration and the in-app disclosure text.

### N2. Widget with one-tap logging
- **What it does:** shows the next prayer and a countdown, with buttons to log the current prayer from the home screen.
- **Why it's new:** other apps' widgets only *display* times. For a tracker, fewer taps means more logging and better retention.
- **Effort:** M.

### N3. Qaza make-up planner
- **What it does:** combines the qaza estimator with a plan and a projected finish date. For example: "1 qaza with each prayer clears 2,340 Fajr in about 6.4 years. 2 per prayer takes about 3.2 years."
- **Reminder:** after each prayer, an optional "Add a qaza Fajr now?" notification.
- **Why it's new:** competitors stop at the calculator.
- **Effort:** S–M.

### N4. Prayer quiet window
- **What it does:** switches the phone to vibrate for N minutes from each adhan, wherever the user is, then restores the sound. This is Mosque Mode's ringer logic triggered by time instead of location.
- **Permissions:** vibrate needs no Do Not Disturb access.
- **Why it matters:** Prayer Pause sells this as a premium feature on iOS. We can offer it free on Android, without app-blocking hacks.
- **Effort:** S–M.

### N5. Travel (musafir) mode
- **What it does:** notices when the user is more than about 80 km from their saved home. Sources give anywhere from 80 to 90 km.
- **Then it:**
  - suggests shortening the four-rak'ah prayers (qasr)
  - shows options for combining prayers according to the user's school. The default Hanafi position is no combining.
- **Why it's new:** this turned up only as a user feature request and in academic prototypes.
- **Effort:** M.
- **Needs:** careful wording that offers guidance rather than rulings. The school-of-thought (`Madhab`) setting this originally called for is now in place (Standard/Hanafi/Ja'fari), so this can key combining rules off it directly.

### N6. Wear OS app
- **What it does:**
  - a watch tile showing the next prayer
  - a vibration on the wrist at adhan time
  - one-tap logging from the watch
- **Later:** a rak'ah counter using the watch's motion sensor.
- **Why it's new:** every sensor-based rak'ah counter found is Apple-only.
- **Effort:** L.

### N7. Privacy-first, and saying so
- **What it is:**
  - prayer times calculated on the phone — ✅ done (2026-10-20)
  - no ads, no accounts, no analytics
  - data kept on the phone, backed up only to the user's own Google Drive
- **How to show it:** a short "Your data" page in Settings, a clean Data safety section on Google Play, and privacy as a headline in the store description. **Still open** — on-device calculation is in, but nothing surfaces it to the user yet.
- **Why it matters:** it answers the market's second-loudest complaint.
- **Effort:** S — the hard dependency (on-device calculation) is already done.

### N8. Notification health check
- **What it does:** a Settings screen that checks what silently breaks adhans on Android. Each item gets a "Fix" button that opens the right system screen.
- **What it checks:**
  - notifications or notification channels switched off
  - exact-alarm permission
  - battery optimisation
  - Samsung's "Sleeping apps" list and its equivalents on other brands
  - background location for Mosque Mode
- **Why it matters:** it answers a top Android complaint, and we learned these failures first-hand while debugging Mosque Mode.
- **Effort:** S–M.

### N9. Iqamah times per mosque
- **What it does:** users enter each saved mosque's iqamah (jamaat) times. Reminders then say "Iqamah at Masjid Al-Noor in 15 min."
- **Why it matters:** in South Asia, our Urdu audience, iqamah often runs well after the adhan, and it's what people plan around.
- **Effort:** S–M.
- **Later:** importing timetables from mosque platforms. Check their API terms first.

### N10. Khushu journal
- **What it does:** an optional one-line note or focus rating after each logged prayer.
- **Extra stats:** for example, "You rated Fajr 'focused' 70% of the time."
- **No AI needed**, so there's no risk of AI answering fiqh questions.
- **Effort:** S–M.

---

## 5. Roadmap

### Phase 1: quick wins
Small effort and high impact. Most of it reuses existing code.
1. ~~Asr school, high-latitude rule and per-prayer offsets~~ — ✅ done 2026-10-20, together with on-device calculation (originally Phase 3)
2. Qibla compass
3. Tasbih counter that checks in to `DHIKR` goals
4. Qaza estimator that fills `QazaBalanceEntity`
5. Sunnah fasting and Friday reminders, based on Hijri dates
6. Notification health check (N8)
7. Privacy page surfacing the on-device calculation switch (N7) — now cheap since the switch itself is done

### Phase 2: features that set us apart
1. Mosque check-in that logs congregation (N1)
2. Widget with one-tap logging (N2)
3. Qaza make-up planner (N3)
4. Prayer quiet window (N4)
5. Hadith of the day
6. Yearly heatmap and streak grace days

### Phase 3: bigger investments
1. ~~Prayer times calculated on the phone~~ — ✅ done 2026-10-20; the privacy page itself (N7) moved to Phase 1
2. Sunnah and voluntary prayer tracking
3. On-time vs. late logging
4. Ramadan mode
5. Travel mode (N5) — now has a real madhhab setting to key off (`Madhab`), which this item originally called out as a prerequisite
6. Badges

### Phase 4: evaluate before committing
- Wear OS app (N6)
- ~~Full Quran reader~~ — ✅ done 2026-10-22; the next Quran priorities are audio recitation and selectable translations/tafsir
- Bundled azan audio
- Social groups and family mode (need a backend and children's privacy compliance)
- Any AI feature. Never use AI to answer fiqh questions.

---

## 6. Risks and policy notes

- **Google Play location declaration.** Every new use of background location (N1, N5) must go into the Play Console location declaration and the in-app disclosure. Today these only cover ringer switching.
- **Accessibility API.** Don't use it to block apps during prayer. Google Play restricts it to genuine accessibility uses.
- **Children's data.** A family or kids mode brings in Google Play's Families policy and children's privacy laws.
- **Content licences:**
  - Most Quran translations are copyrighted.
  - hadith-api is free to reuse (Unlicense).
  - No licence was found for Hisnul Muslim data.
  - Azan recordings need the rights-holder's permission.
- **Differences between schools of thought (madhhabs).** They differ on Asr time, combining prayers, how qaza is counted and whether Witr is included. Offer settings, use neutral wording, cite sources, and never present results as rulings. Asr timing and prayer-combining (Ja'fari's Zuhrain/Maghribain) are handled this way already, via the `Madhab` setting; qaza counting and Witr inclusion are still open, and matter most for N3 and the fasting/Ramadan work in Phase 3.
- **Exact alarms.** Android 12+ restricts them. The notification health check should cover this permission.

---

## Sources

**Competitor apps**
- [Muslim Pro](https://apps.apple.com/app/id1087850108)
- [Prayer Pause](https://apps.apple.com/us/app/-/id6757287456)
- [PrayPath](https://mwm.ai/apps/praypath-prayer-times-hatim/6764774442)
- [Al Taqweem](https://apps.apple.com/app/id6774800495)
- [My Sujoods](https://mwm.ai/apps/my-sujoods/6760788492)
- [Prayer Tracker & Daily Hadith](https://mwm.ai/apps/prayer-tracker-daily-hadith/6759488114)
- [Prayer Times & Qibla Tracker](https://apps.apple.com/app/id1462959683)
- [Salat Now](https://mwm.ai/apps/salat-now/6746818848)
- [A Simple Salah](https://mwm.ai/de/apps/a-simple-salah/6759878475)
- [Salah Tracker: Namaz & Qibla](https://www.producthunt.com/products/salah-tracker-namaz-qibla/reviews)
- [Pillars](https://itsallwidgets.com/pillars)

**Privacy, ads and user complaints**
- [Comparitech: Muslim prayer app privacy study](https://comparitech.com/blog/vpn-privacy/muslim-prayer-app-study)
- [Unstar: prayer apps ranked 2026](https://unstar.app/blog/muslim-pro-quran-majeed-athan-pillars-tarteel-quran-prayer-apps-ranked-2026)
- [FivePrayer: Muslim Pro alternatives](https://www.fiveprayer.app/blog/muslim-pro-alternative)
- [FivePrayer: privacy-focused Muslim apps](https://www.fiveprayer.app/blog/privacy-focused-muslim-apps)
- [Islamtics: best Islamic apps for Android 2026](https://islamtics.com/best-islamic-apps-android/)

**Focus mode**
- [FivePrayer](https://fiveprayer.app)
- [FivePrayer: lock phone for prayer](https://fiveprayer.app/blog/lock-phone-for-prayer)
- [Khushu](https://apps.apple.com/be/app/id6758948715)

**Social and family**
- [Mizan](https://apps.apple.com/tw/app/6745764531)
- [HalalYouNeed](https://apps.apple.com/ca/app/halalyouneed/id6758355562)
- [Salah Tracker for Kids](https://simplyislam.sg/muslimbuzz/ipad-app-for-kids-salah-prayer-tracker-review/)

**Sensors and wearables**
- [rakaah.ai](https://apps.apple.com/us/app/id1562760269)
- [Prayer Memory](https://mwm.ai/apps/prayer-memory-rakat-counter/6756967666)
- [Sahw](https://apps.apple.com/app/id6743373991)
- [2026 Sujood App](https://mwm.ai/apps/2026-sujood-app/6763619303)

**Habit and sunnah tracking**
- [Muslim Habits Tracker](https://apps.apple.com/app/id6741549316)
- [Niyyah](https://mwm.ai/apps/niyyah-salah-tracker/6781700285)
- [Muslim Daily](https://mwm.ai/apps/muslimdaily/1438254449)
- [Sunnah Helper](https://yourstory.com/companies/sunnah-helper)
- [Everyday Muslim](https://apps.apple.com/us/app/-/id1572387370)
- [Sunnah Way](https://mwm.ai/apps/sunnah-way/6761305443)

**Qaza calculators**
- [Qadaa Prayer Calculator](https://apps.apple.com/gt/app/6748410215)
- [Salah Journal](https://apps.apple.com/us/app/-/id6747736982)
- [Qada Mate](https://mwm.ai/apps/qada-mate/1672815361)
- [I.M.A.M. on years of missed prayers](https://ask.imam-us.org/qa/12493/qada-prayers-a-i-have-many-years-of-qada-prayers-i-do-not-remember-how-many)

**Travel**
- [GoSolat musafir mode request](https://gosolat.userjot.com/board/p/musafir-mode)
- [FivePrayer: jamak and qasar](https://www.fiveprayer.app/blog/jamak-qasar-traveler-prayer)
- [e-Pocket Musafir paper](https://penerbit.uthm.edu.my/periodicals/index.php/mari/article/view/3531)
- [IslamQA: combining prayers for travellers (Hanafi)](https://islamqa.org/hanafi/islamicportal/117730/combining-prayers-for-travellers/)

**Ramadan and fasting**
- [RafiQ](https://apps.apple.com/app/id6743010247)
- [Ramadan & Quran Tracker](https://apps.apple.com/us/app/-/id6758881752)
- [Muslim & Quran](https://apps.apple.com/us/app/muslim-quran-ramadan-2026/id1508575206)
- [TechCabal: Ramadan apps 2026](https://techcabal.com/2026/02/19/ramadan-apps-muslims-stay-focused-deen/)
- [Al Hakam: days of voluntary fasting](https://www.alhakam.org/what-are-the-days-of-voluntary-fasting-in-islam)
- [Sahih Muslim 1162](https://sunnah.com/muslim:1162b)

**Friday and adhkar reminders**
- [Azkar](https://apps.appfollow.io/ios/azkar-dhkr-athan-prayer/1454509502?country=us)
- [Iqra](https://mwm.ai/apps/app/6756839411)

**AI apps**
- [Tanwir](https://apps.apple.com/tc/app/tanwir/id6450836093)
- [Niyat](https://apps.apple.com/app/id6760214927)
- [Ask Quran](https://mwm.ai/apps/ask-quran-verses-chat/6758209894)
- [Quran Study](https://apps.apple.com/us/app/quran-study-read-talk-chat/id6741719702)
- [Tarteel: best Quran apps](https://tarteel.ai/blog/the-best-quran-apps-of-2025)

**Libraries and data**
- [adhan-kotlin](https://github.com/batoulapps/adhan-kotlin)
- [hadith-api](https://github.com/fawazahmed0/hadith-api)
- [dua-dhikr API](https://github.com/fitrahive/dua-dhikr)
- [House of Islam developer datasets](https://developers.thehouseofislam.com/)
