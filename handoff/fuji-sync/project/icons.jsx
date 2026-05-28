// icons.jsx — Small gold line-icons + UI glyphs.
// All icons are stroke-based, currentColor, 1.5 stroke-width, viewBox 24×24.

function Icon({ size = 18, children, style }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none"
      stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"
      style={style}>{children}</svg>
  );
}

const IconUSB = (p) => (
  <Icon {...p}>
    <path d="M12 21V6" />
    <path d="M8 9l4-4 4 4" />
    <circle cx="12" cy="21" r="1.5" fill="currentColor" stroke="none" />
    <path d="M9 13h6" />
    <path d="M15 13l2 2v2l-3 2" />
  </Icon>
);
const IconSearch = (p) => (
  <Icon {...p}>
    <circle cx="11" cy="11" r="6" />
    <path d="M16 16l4 4" />
  </Icon>
);
const IconSort = (p) => (
  <Icon {...p}>
    <path d="M7 4v16" /><path d="M3 8l4-4 4 4" />
    <path d="M17 20V4" /><path d="M21 16l-4 4-4-4" />
  </Icon>
);
const IconFilter = (p) => (
  <Icon {...p}>
    <path d="M4 6h16" /><path d="M7 12h10" /><path d="M10 18h4" />
  </Icon>
);
const IconPlus = (p) => (
  <Icon {...p}>
    <path d="M12 5v14M5 12h14" />
  </Icon>
);
const IconChevron = (p) => (
  <Icon {...p}>
    <path d="M9 6l6 6-6 6" />
  </Icon>
);
const IconClose = (p) => (
  <Icon {...p}>
    <path d="M6 6l12 12M18 6L6 18" />
  </Icon>
);
const IconCheck = (p) => (
  <Icon {...p}>
    <path d="M5 12l5 5 9-11" />
  </Icon>
);
const IconStar = (p) => (
  <Icon {...p}>
    <path d="M12 4l2.5 5.5 6 .7-4.5 4 1.2 6L12 17.3 6.8 20.2l1.2-6L3.5 10.2l6-.7L12 4z" />
  </Icon>
);
const IconMore = (p) => (
  <Icon {...p}>
    <circle cx="6" cy="12" r="1" fill="currentColor" stroke="none" />
    <circle cx="12" cy="12" r="1" fill="currentColor" stroke="none" />
    <circle cx="18" cy="12" r="1" fill="currentColor" stroke="none" />
  </Icon>
);
const IconRefresh = (p) => (
  <Icon {...p}>
    <path d="M21 12a9 9 0 0 1-15.5 6.3L3 16" />
    <path d="M3 12a9 9 0 0 1 15.5-6.3L21 8" />
    <path d="M21 3v5h-5" /><path d="M3 21v-5h5" />
  </Icon>
);
const IconCamera = (p) => (
  <Icon {...p}>
    <path d="M3 8h3l2-3h8l2 3h3v11H3V8z" />
    <circle cx="12" cy="13" r="3.5" />
  </Icon>
);
const IconFolder = (p) => (
  <Icon {...p}>
    <path d="M3 6.5A1.5 1.5 0 0 1 4.5 5h4l2 2h9A1.5 1.5 0 0 1 21 8.5V18a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 18V6.5z" />
  </Icon>
);
const IconProfile = (p) => (
  <Icon {...p}>
    <circle cx="12" cy="8" r="4" />
    <path d="M4 20c1.5-3.5 4.5-5 8-5s6.5 1.5 8 5" />
  </Icon>
);

// Property icons (each section row)
const IconDR = (p) => (
  <Icon {...p}>
    <circle cx="12" cy="12" r="4" />
    <path d="M12 3v2M12 19v2M3 12h2M19 12h2M5.6 5.6l1.4 1.4M17 17l1.4 1.4M5.6 18.4L7 17M17 7l1.4-1.4" />
  </Icon>
);
const IconGrain = (p) => (
  <Icon {...p}>
    <circle cx="6" cy="8" r=".8" fill="currentColor" stroke="none" />
    <circle cx="11" cy="6" r=".8" fill="currentColor" stroke="none" />
    <circle cx="17" cy="9" r=".8" fill="currentColor" stroke="none" />
    <circle cx="8" cy="13" r=".8" fill="currentColor" stroke="none" />
    <circle cx="14" cy="14" r=".8" fill="currentColor" stroke="none" />
    <circle cx="6" cy="18" r=".8" fill="currentColor" stroke="none" />
    <circle cx="12" cy="19" r=".8" fill="currentColor" stroke="none" />
    <circle cx="18" cy="17" r=".8" fill="currentColor" stroke="none" />
    <circle cx="19" cy="5" r=".8" fill="currentColor" stroke="none" />
    <circle cx="4" cy="13" r=".8" fill="currentColor" stroke="none" />
  </Icon>
);
const IconCC = (p) => (
  <Icon {...p}>
    <path d="M6 18l8-12 4 6-8 12-4-6z" />
    <path d="M14 6l4 6" />
  </Icon>
);
const IconCCFXB = (p) => (
  <Icon {...p}>
    <circle cx="12" cy="12" r="7" />
    <path d="M12 5a7 7 0 0 0 0 14V5z" fill="currentColor" stroke="none" />
  </Icon>
);
const IconSkin = (p) => (
  <Icon {...p}>
    <circle cx="12" cy="12" r="7" />
    <path d="M9 14c1 1.2 5 1.2 6 0" />
    <circle cx="9.5" cy="10.5" r=".4" fill="currentColor" stroke="none" />
    <circle cx="14.5" cy="10.5" r=".4" fill="currentColor" stroke="none" />
  </Icon>
);
const IconHL = (p) => (
  <Icon {...p}>
    <circle cx="12" cy="12" r="3" fill="currentColor" stroke="none" />
    <path d="M12 3v2M12 19v2M3 12h2M19 12h2M5.6 5.6l1.4 1.4M17 17l1.4 1.4M5.6 18.4L7 17M17 7l1.4-1.4" />
  </Icon>
);
const IconShadow = (p) => (
  <Icon {...p}>
    <path d="M17 14A6 6 0 1 1 10 7a5 5 0 0 0 7 7z" />
  </Icon>
);
const IconColor = (p) => (
  <Icon {...p}>
    <path d="M9 19l-3-3 9-9 3 3-9 9z" />
    <path d="M14 7l3-3" />
    <path d="M3 21l4-2" />
  </Icon>
);
const IconSharpness = (p) => (
  <Icon {...p}>
    <path d="M12 4l9 16H3L12 4z" />
  </Icon>
);
const IconNR = (p) => (
  <Icon {...p}>
    <circle cx="12" cy="12" r="3" />
    <circle cx="12" cy="12" r="7" />
  </Icon>
);
const IconClarity = (p) => (
  <Icon {...p}>
    <rect x="4" y="4" width="16" height="16" rx="2" transform="rotate(45 12 12)" />
  </Icon>
);
const IconWB = (p) => (
  <Icon {...p}>
    <circle cx="12" cy="12" r="7" />
    <path d="M12 5v14" />
    <path d="M5 12h14" />
  </Icon>
);
const IconShift = (p) => (
  <Icon {...p}>
    <path d="M4 12h16" />
    <path d="M10 7l-5 5 5 5" />
    <path d="M14 7l5 5-5 5" />
  </Icon>
);

const PROP_ICONS = {
  'Dynamic Range': IconDR,
  'Grain Effect': IconGrain,
  'Color Chrome': IconCC,
  'Color Chrome FX Blue': IconCCFXB,
  'Smooth Skin': IconSkin,
  'Highlight Tone': IconHL,
  'Shadow Tone': IconShadow,
  'Color': IconColor,
  'Sharpness': IconSharpness,
  'High ISO NR': IconNR,
  'Clarity': IconClarity,
  'White Balance': IconWB,
  'WB Shift R': IconShift,
  'WB Shift B': IconShift,
};

Object.assign(window, {
  Icon, IconUSB, IconSearch, IconSort, IconFilter, IconPlus, IconChevron,
  IconClose, IconCheck, IconStar, IconMore, IconRefresh,
  IconCamera, IconFolder, IconProfile, IconWB, IconShift,
  PROP_ICONS,
});
