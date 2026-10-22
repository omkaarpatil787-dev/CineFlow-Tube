# 🎬 CineFlow Tube — Ultra-Fluid LibreTube Alternative for Android
> **A cinema-grade, privacy-first YouTube streaming client engineered with Jetpack Compose, AndroidX Media3, and Jadzumi's CineFlow motion engine.**

---

## 📽️ CineFlow Immersive Player ("Less chrome. More cinema.")

Directly finalized from [`design-references/cineflow_immersive_player.html`](file:///C:/Users/onkar/OneDrive/Documents/GitHub/CineFlow-Tube/design-references/cineflow_immersive_player.html):

* **Signature Wavy Line Timeline (`WavyProgressTimeline`)**:
  * **Animated Wave Flow**: Mathematical sinusoidal wave flowing continuously during playback (`y = centerY + sin(x/38dp * 2π - phase) * 4dp * taper`) with seamless 12dp/16dp endpoint tapering.
  * **Three Selectable Timeline Styles**:
    1. **`Wavy line · reference` (Default)**: Dynamic flowing sine wave played track with straight unplayed/buffer tracks and glowing head bead.
    2. **`Wavy + circular`**: Animated sine wave paired with a rotating 12-petal hypotrochoid rosette flower scrub handle (`7s linear infinite`).
    3. **`Minimal straight`**: Clean linear progress bar with 5dp active scrub thumb.
  * **Scrub Preview Floating Tooltip**: Real-time floating capsule (`[ 02:18 ]`) directly following the scrub thumb along the track.

* **Rotatable 10s Seek Controls (`RotatableSeekButton`)**:
  * 54x54dp circular buttons for Rewind (-10s) and Forward (+10s) with central "10" indicator.
  * Tap triggers an animated 380ms rotation (`±32°` curve `CubicBezier(0.2, 0.8, 0.2, 1.0)`).
  * Paired with expanding double-tap seek ripple feedback (`SeekFeedbackOverlay`).

* **"Your viewing space" Options Sheet (`CineFlowOptionsSheet`)**:
  * Deep frosted glass bottom sheet (`#0B2031F5` with specular hairline border `#C0E7F82C`).
  * **2-Level Hierarchical Navigation**:
    * **Level 1 (Main Settings)**: Playback controls (Quality, Speed, Audio, Subtitles) & Experience controls (Timeline style, Auto-hide 3s, Repeat video, Ambient glow, Volume slider 0-100%, and Reset preferences).
    * **Level 2 (Sub-Picker)**: Fluid lateral transition (`slideInHorizontally`) with individual radio option tiles, active checkmarks (`✓`), and instant live preview.

* **Atmospheric Glacier Immersion**:
  * **Ambient Glacier Glow**: Subtle radiant azure backlight (`#68CCE518`) projecting softly behind the player container.
  * **Filmic Vignette Scrim**: Vertical gradient vignette (`#8C031019 → Transparent → #CF020B16`) keeping video colors vibrant while ensuring high-contrast readable controls.
  * **Captions Subtitle Capsule**: Dark frosted subtitle overlay (`#C900101E`) displaying active closed captions.
  * **Toast Notification HUD**: Centered frosted pill at 25% height providing instant 1300ms feedback on seeks, quality changes, and speed adjustments.
  * **Theater / Fullscreen Toggle**: One-tap expansion into full-bleed cinematic mode.

---

## ⚡ CineFlow Motion Lab & Animation System

Directly integrated from [`design-references/cineflow_motion_lab.html`](file:///C:/Users/onkar/OneDrive/Documents/GitHub/CineFlow-Tube/design-references/cineflow_motion_lab.html), CineFlow Tube implements the **Six Restrained Motion Patterns**:

| # | Motion Pattern | Spec & Easing | Compose Implementation |
| :---: | :--- | :--- | :--- |
| **01** | **Icon Morph** | `300ms` · `CubicBezier(0.2, 0.8, 0.2, 1.0)` | [`MorphingPlayPauseButton`](file:///C:/Users/onkar/OneDrive/Documents/GitHub/CineFlow-Tube/app/src/main/java/com/cineflow/tube/ui/player/MorphingPlayPauseButton.kt): Continuous vector interpolation between Play triangle and Pause double bars with stable touch bounds. |
| **02** | **Spring Response** | `Damping 0.72` · `Stiffness 380` | [`Modifier.springPress()`](file:///C:/Users/onkar/OneDrive/Documents/GitHub/CineFlow-Tube/app/src/main/java/com/cineflow/tube/ui/theme/Motion.kt): Tactile scale `1.0 → 0.94` on touch down; springs back upon release with zero layout recalculation. |
| **03** | **Spatial Continuity** | `420ms` · `CubicBezier(0.2, 0.9, 0.2, 1.08)` | [`FrostedBottomDock`](file:///C:/Users/onkar/OneDrive/Documents/GitHub/CineFlow-Tube/app/src/main/java/com/cineflow/tube/ui/components/FrostedDock.kt): Active indicator capsule glides seamlessly across destinations with subtle spring settle. |
| **04** | **Container Transform** | `460ms` · `CubicBezier(0.2, 0.8, 0.2, 1.0)` | [`CineFlowMiniplayerContainer`](file:///C:/Users/onkar/OneDrive/Documents/GitHub/CineFlow-Tube/app/src/main/java/com/cineflow/tube/ui/player/CineFlowMiniplayer.kt): Smooth bounds and radius transform (`18dp ↔ 22dp`) preserving the single Media3 player instance. |
| **05** | **Seek Feedback** | `360ms` · `FastOutSlowInEasing` | [`SeekFeedbackOverlay`](file:///C:/Users/onkar/OneDrive/Documents/GitHub/CineFlow-Tube/app/src/main/java/com/cineflow/tube/ui/player/SeekFeedbackOverlay.kt): Expanding ripple ring (`scale: 0.85 → 2.1`, `alpha: 0.6 → 0.0`) confirming double-tap seek without blocking playback. |
| **06** | **Theme Bloom** | `580ms` · Circular expansion | Tap-anchored circular reveal transitioning Light ↔ Dark themes. |

---

## ❄️ Glacier Splash Launch Experience

Adapted directly from [`design-references/cineflow_glacier_splash.html`](file:///C:/Users/onkar/OneDrive/Documents/GitHub/CineFlow-Tube/design-references/cineflow_glacier_splash.html):

* **Atmospheric Glacier Glow**:
  * Triple-radial bloom canvas: Cyan Bloom (`#00CCE5`), Ice Blue (`#80B7FF`), and Frost Lavender (`#B2CEF4`) with continuous gentle drift.
  * Theme-aware ambient luminance: `100%` on Light, `60%` on Dark, and `34%` on AMOLED for OLED battery conservation.
* **Jadzumi Motion Choreography**:
  * **Layered Matte Mark (`.mark`)**: Scaled up with cubic-bezier ease (`CubicBezier(0.2, 0.75, 0.2, 1.0)`) and gentle levitation floating loop.
  * **Brand Typography**: `CineFlow Tube` with `BY JADZUMI` subtext entering with 120ms staggered delay.
  * **Hairline Micro-Loader**: 34dp × 3dp progressive loading line with 200ms staggered delay.
  * **Symmetrical Fluid Exit**: Scales to `1.08x` and fades to `0` over 450ms (`CubicBezier(0.4, 0.0, 1.0, 1.0)`), seamlessly handing off to the feed without abrupt cutoffs.

---

## 🌟 Visual Design System & Adaptive Icons

* **Layered Matte Icon System** ([`cineflow_icon_studio.html`](file:///C:/Users/onkar/OneDrive/Documents/GitHub/CineFlow-Tube/design-references/cineflow_icon_studio.html)):
  * 3-tier matte cyan card stack (`#007280`, `#00A3B7`, `#00CCE5`) with specular hairline edges and deep ink center play crest (`#003D4B`).
  * Full support for Android Adaptive Icons and **Android 13+ Material You Themed Icons** (`ic_launcher_monochrome.xml`).
* **Theme Tiers**:
  * **Light Canvas**: Ice Blue (`#F4F9FD`), Deep Ink (`#143E56`), Cyan Accent (`#00CCE5`).
  * **Dark Sapphire**: Midnight Sapphire (`#0B1620`), Cinema Slate (`#1D293A`), Radiant Azure.
  * **OLED Black**: Absolute zero (`#000000`), Onyx cards (`#101010`).
* **0.75dp Specular Hairline Rims**: Delicate border contours catching ambient light without boxy visual clutter.

---

## 🧭 Navigation & Layout Architecture

* **Home (`⌂`)**: Category filter chips + "Recommended for you ✦" feed.
* **Shorts (`▣`)**: 9:16 vertical video cards with bottom title overlays and gradient vignettes.
* **Subscriptions (`▤`)**: Private, local creator uploads with zero Google account connection.
* **Your Space (`◉`)**: "Saved for Later ♡", Atmospheric Theme Switcher (*Light / Dark / AMOLED*), Immersive Player preferences (*Timeline style, Glacier Glow, Auto-hide*), and streaming backend selector.

---

## 🛠️ Architecture & Tech Stack

* **Platform & Language**: Android (minSdk 26, targetSdk 35/36), Kotlin 2.2.10
* **UI**: Jetpack Compose + Compose Compiler 2.2.10, Material 3 Expressive
* **Engine**: AndroidX Media3 (ExoPlayer 1.11.1) + HLS/DASH + NextLib FFmpeg 6.1 extensions
* **Streaming Backend**: Hybrid Piped API (primary) + Invidious API (failover)
* **Background Service**: `MediaSessionService` foreground service for uninterrupted audio playback and lockscreen notification controls
