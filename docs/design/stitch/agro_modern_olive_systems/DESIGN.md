---
name: Agro-Modern Olive Systems
colors:
  surface: '#faf8ff'
  surface-dim: '#d2d9f4'
  surface-bright: '#faf8ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f2f3ff'
  surface-container: '#eaedff'
  surface-container-high: '#e2e7ff'
  surface-container-highest: '#dae2fd'
  on-surface: '#131b2e'
  on-surface-variant: '#42493e'
  inverse-surface: '#283044'
  inverse-on-surface: '#eef0ff'
  outline: '#72796e'
  outline-variant: '#c2c9bb'
  surface-tint: '#3b6934'
  primary: '#154212'
  on-primary: '#ffffff'
  primary-container: '#2d5a27'
  on-primary-container: '#9dd090'
  inverse-primary: '#a1d494'
  secondary: '#904d00'
  on-secondary: '#ffffff'
  secondary-container: '#fe932c'
  on-secondary-container: '#663500'
  tertiary: '#004319'
  on-tertiary: '#ffffff'
  tertiary-container: '#005d25'
  on-tertiary-container: '#5dda7a'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#bcf0ae'
  primary-fixed-dim: '#a1d494'
  on-primary-fixed: '#002201'
  on-primary-fixed-variant: '#23501e'
  secondary-fixed: '#ffdcc3'
  secondary-fixed-dim: '#ffb77d'
  on-secondary-fixed: '#2f1500'
  on-secondary-fixed-variant: '#6e3900'
  tertiary-fixed: '#7ffc97'
  tertiary-fixed-dim: '#62df7d'
  on-tertiary-fixed: '#002109'
  on-tertiary-fixed-variant: '#005320'
  background: '#faf8ff'
  on-background: '#131b2e'
  surface-variant: '#dae2fd'
typography:
  display-lg:
    fontFamily: Montserrat
    fontSize: 40px
    fontWeight: '700'
    lineHeight: 48px
    letterSpacing: -0.02em
  display-lg-mobile:
    fontFamily: Montserrat
    fontSize: 30px
    fontWeight: '700'
    lineHeight: 38px
    letterSpacing: -0.01em
  headline-lg:
    fontFamily: Montserrat
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Montserrat
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
  headline-sm:
    fontFamily: Montserrat
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  label-lg:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-sm:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 14px
    letterSpacing: 0.03em
  metric-val:
    fontFamily: Montserrat
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 36px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1.5rem
  gutter-mobile: 1rem
  margin: 2rem
  margin-mobile: 1rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
  space-2xl: 3rem
---

## Brand & Style

The platform marries agrarian heritage with precision enterprise utility. Designed for two intersecting user bases—cooperative executives managing bulk logistics and yield distributions, and field operators logging orchard inspections under harsh sunlight—the visual atmosphere is dependable, rooted, and radically legible.

The aesthetic fuses **Modern Agritech SaaS** with **Tactile Functionalism**. It avoids ethereal, fragile interfaces in favor of grounded structure: robust strokes, distinct tonal framing, high-density clarity for administrative cockpits, and enlarged touch surfaces for in-field actions. The emotional tone projects agricultural authority, biological precision, and operational resilience.

## Colors

The palette balances organic vegetation tones with urgent industrial cues and warm Mediterranean earthenware. 

- **Primary (`#2D5A27`, `#3D6A32`)**: Dense olive tones signify authority, yield continuity, and system vitality. Primary acts as the anchor for global navigation bars, major actions, active filters, and institutional branding.
- **Secondary (`#D97706`, `#B45309`)**: Sunlit terracotta and harvest amber. Reserved for critical yield milestones, payment authorizations, batch grading highlights, and operational attention cues.
- **Tertiary & Functional (`#16A34A`, `#DC2626`)**: Biological emerald for healthy sensor telemetries and certified batch confirmations; alert red for pest incursions, equipment faults, and moisture deficit warnings.
- **Canvas & Surface (`#F8FAF5`, `#F1F4ED`, `#FFFFFF`)**: Cream-infused outdoor neutrals that soften screen glare under direct sunlight compared to sterile stark whites, providing clean contrast against dark typography.
- **Neutrals & Text (`#0F172A`, `#1E293B`, `#64748B`)**: Deep mineral slates and charcoal engineered to pass WCAG AAA contrast metrics across both digital displays and field-grade ruggedized tablets.

## Typography

Typography prioritizes fast scannability, dense tabular alignment, and zero ambiguity across metric values (e.g., metric tons, acidity levels, moisture percentages). 

- **Montserrat** provides an architectural, geometric frame for top-level figures, page headlines, batch codes, and sensor metrics. Its open counters ensure readability at quick glance.
- **Inter** provides neutral, systematic clarity for high-volume logs, cooperative shareholder rosters, and complex data inputs. Tabular numerical figures (`tnum`) must be enforced for all weight distributions, financial tallies, and chemical metrics.

## Layout & Spacing

The layout is built on an adaptive 12-column grid for administration workspaces, shifting to a structured 4-column stack for the farmer field interface.

- **Desktop & Administration**: Max canvas container of 1440px with a persistent 280px left navigation rail for cooperative tools (Milling Operations, Agronomy Logs, Member Ledger). Data views use dense vertical rhythm (`space-sm` to `space-md`) to maximize vertical screen efficiency.
- **Field Tablet / Mobile**: 100% fluid edge-to-edge layouts using `margin-mobile` (1rem). Touch targets increase to a minimum boundary of 48px to accommodate one-handed operation or gloved hands in harvest conditions.
- **Rhythm**: Built strictly around 4px/8px increments. Component-internal spacing uses `space-sm` (8px) and `space-md` (16px); section separations leverage `space-lg` (24px) and `space-xl` (32px).

## Elevation & Depth

To guarantee visibility in variable ambient outdoor lighting, depth is defined primarily through **tonal layering** and **crisp 1px boundary outlines**, reinforced by low-diffusion, tinted ambient shadows.

- **Layer 0 (Canvas Base)**: `#F8FAF5` provides the natural substrate.
- **Layer 1 (Cards & Data Panels)**: `#FFFFFF` encased in a crisp structural border (`#E2E8F0` or `#E5EADF`).
- **Layer 2 (Floating Overlays, Flyouts, Slide-over Panels)**: Pure white background supported by a directional tinted shadow: `0 10px 25px -5px rgba(15, 23, 42, 0.08), 0 8px 10px -6px rgba(45, 90, 39, 0.04)`.
- **Active Field Highlight**: In-progress harvest bins and critical field warnings apply an elevated inner edge tint using the secondary amber or olive hue to emphasize active operational state.

## Shapes

The design uses balanced, modern radii (`roundedness: 2` / 0.5rem base) to provide an approachable yet industrial feel. 

- Form inputs, standard buttons, and data cards utilize a 0.5rem (8px) base radius.
- Large contextual group cards, modal dialogs, and field status modules scale to 1rem (16px).
- Small tags, status chips, and batch state pills utilize full-pill treatment (9999px) for fast visual separation against rectangular data grids.

## Components

### Buttons
- **Primary Action**: Background `#2D5A27`, text `#FFFFFF`, radius 8px. Hover shifts to `#3D6A32`. Minimum height of 44px on desktop, 52px in Field View.
- **Secondary / Operational**: Border 1.5px solid `#2D5A27`, text `#2D5A27`, background transparent. Hover fills with 8% opacity tint of `#2D5A27`.
- **Harvest / Urgent Accent**: Background `#D97706`, text `#FFFFFF`. Reserved for final submission triggers (e.g., "Submit Weighbridge Ticket", "Authorize Milling Run").

### Data Cards & Field Telemetry Panels
- Structured with an explicit 1px stroke (`#E2E8F0`), `#FFFFFF` fill, and internal padding of `1.5rem` (`space-lg`).
- Cards group parcel diagnostics into split layouts: left-hand side displays tree density and hydration indices; right-hand side provides quick status chips and contextual actions.

### Tables & Data Grids
- **Header**: `#F1F4ED` background, text `#0F172A`, font `Inter SemiBold` (12px, uppercase with `0.05em` tracking), height 44px.
- **Rows**: Explicit 1px border bottom (`#E5EADF`), alternating subtle background hover state (`#F8FAF5`), cell padding 12px vertical by 16px horizontal.
- **Numbers**: Monospaced tabular alignment for weights, olive acidity levels, and payout sums.

### Input Fields & Controls
- High-contrast 1.5px slate borders (`#CBD5E1`), focusing to a 2px primary ring in `#2D5A27` with 0px blur offset.
- Checkboxes and radio targets feature enlarged interactive hitboxes (20px control, 44px minimum tap surface) for field compliance.

### Chips & Status Badges
- **Organic Quality / Operational Active**: Fill `#DCFCE7`, text `#166534`, border 1px solid `#BBF7D0`.
- **Pending Analysis / In-Transit**: Fill `#FEF3C7`, text `#92400E`, border 1px solid `#FDE68A`.
- **Quarantine / High Acidity Alert**: Fill `#FEE2E2`, text `#991B1B`, border 1px solid `#FECACA`.

### Specialized Domain Components
- **Weighbridge Ticket Module**: Dual-readout card detailing Gross, Tare, and Net fruit weights with high-legibility `metric-val` typography.
- **Parcel Weather & Soil Ribbon**: Compact inline strip featuring real-time wind speed, ambient heat, and ground moisture readings tied to yield predictions.