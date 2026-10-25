# Onboarding design sources

The onboarding layout and gold Arch lantern artwork follow the user-supplied
`Architectural Teal (black text)_ onboarding flow.html`. `OnboardingDesign.kt`
reproduces its `arcL` paths, staggered `trio` tile and fading as native vector drawing.
The rest of the application retains its existing theme.

The onboarding-specific male/female symbols, map pin, calculation sliders, phone,
speaker and Back chevron use the export's `IG`/`BK` geometry. They are separate
drawables so shared app icons keep their existing appearance. The location hero
also follows the export's two offset 2.8-second ripples and 3dp pin bob. Disabling
system animations leaves a static ring and pin.

For actual phone windows, onboarding uses larger body text and row padding than
the compact HTML preview. Spare body height is shared across functional rows with
per-step bounds and room reserved for helper text. Content stays near the heading;
taller content and enlarged fonts scroll without being shrunk.

Fonts are bundled for offline use from the official Google Fonts repository:

- Cormorant Garamond: https://github.com/google/fonts/tree/main/ofl/cormorantgaramond
- Outfit: https://github.com/google/fonts/tree/main/ofl/outfit
- Scheherazade New: https://github.com/google/fonts/tree/main/ofl/scheherazadenew

Each uses the SIL Open Font License. The accompanying license files are bundled in
`app/src/main/assets/onboarding_fonts/`. Urdu localized UI continues to use the
existing bundled Noto Nastaliq Urdu font for readability.
