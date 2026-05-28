// components.jsx — Shared chrome and atoms

const PALETTE = {
  bg: '#0D0D0D',
  panelLow: '#161413',
  panelHigh: '#1F1B18',
  panelHover: '#28231F',
  border: 'rgba(255,255,255,0.06)',
  borderStrong: 'rgba(255,255,255,0.10)',
  gold: '#C99A4E',
  goldDim: 'rgba(201,154,78,0.16)',
  goldFaint: 'rgba(201,154,78,0.08)',
  text: '#F5F1EC',
  textMute: '#8A847E',
  textDim: '#5B5650',
};

// ── Wordmark ──────────────────────────────────────────────────
// Single-weight monospace mark with a small gold tick — distinct from
// the bold/thin grotesque split common in this category.
function Wordmark({ size = 16 }) {
  return (
    <div style={{
      display: 'inline-flex', alignItems: 'center', gap: 8,
      userSelect: 'none',
    }}>
      <span style={{
        display: 'inline-block', width: 14, height: 14,
        background: PALETTE.gold, borderRadius: 2,
        position: 'relative',
      }}>
        <span style={{
          position: 'absolute', inset: 3,
          background: PALETTE.bg, borderRadius: 1,
        }} />
        <span style={{
          position: 'absolute', left: 5, top: 5, right: 5, bottom: 5,
          background: PALETTE.gold,
        }} />
      </span>
      <span style={{
        fontFamily: '"IBM Plex Mono", ui-monospace, monospace',
        fontSize: size - 2, fontWeight: 600, letterSpacing: '0.08em',
        color: PALETTE.text,
      }}>
        FUJISYNC<span style={{ color: PALETTE.gold }}>.</span>
      </span>
    </div>
  );
}

// ── App header (under status bar) ─────────────────────────────
function AppHeader({ connected, onReconnect, cameraModel, sheetExpanded, tab }) {
  const showModel = connected && tab === 'camera' && sheetExpanded;
  return (
    <div style={{
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '14px 20px 12px', background: PALETTE.bg,
    }}>
      <Wordmark size={15} />
      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        {connected && (
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <span style={{
              width: 7, height: 7, borderRadius: '50%',
              background: PALETTE.gold,
              boxShadow: `0 0 8px ${PALETTE.gold}`,
            }} />
            <span style={{
              fontFamily: '"IBM Plex Mono", ui-monospace, monospace',
              fontSize: 10.5, fontWeight: 600, letterSpacing: '0.1em',
              color: PALETTE.text,
              opacity: showModel ? 1 : 0,
              maxWidth: showModel ? 80 : 0,
              overflow: 'hidden',
              transition: 'opacity 0.25s, max-width 0.25s',
              whiteSpace: 'nowrap',
            }}>{cameraModel}</span>
          </div>
        )}
        {!connected && (
          <span style={{
            fontFamily: '"IBM Plex Mono", ui-monospace, monospace',
            fontSize: 10, fontWeight: 500, letterSpacing: '0.18em',
            color: PALETTE.textDim,
          }}>OFFLINE</span>
        )}
        <button onClick={onReconnect} style={{
          background: 'none', border: 'none', cursor: 'pointer',
          color: PALETTE.textMute, padding: 2, display: 'flex',
          alignItems: 'center',
        }}><IconRefresh size={16} /></button>
      </div>
    </div>
  );
}

// ── Section label ─────────────────────────────────────────────
function SectionLabel({ children, style }) {
  return (
    <div style={{
      fontFamily: '"IBM Plex Sans", system-ui, sans-serif',
      fontSize: 11, fontWeight: 500, letterSpacing: '0.22em',
      textTransform: 'uppercase', color: PALETTE.textMute,
      ...style,
    }}>{children}</div>
  );
}

// ── Pill chip ─────────────────────────────────────────────────
function Pill({ children, active, large }) {
  return (
    <span style={{
      display: 'inline-flex', alignItems: 'center',
      padding: large ? '8px 12px' : '6px 10px',
      borderRadius: 999,
      background: active ? PALETTE.goldDim : PALETTE.panelHigh,
      color: active ? PALETTE.gold : PALETTE.text,
      fontFamily: '"IBM Plex Mono", ui-monospace, monospace',
      fontSize: large ? 11 : 10.5, fontWeight: 500,
      letterSpacing: '0.06em',
      whiteSpace: 'nowrap',
      border: active ? `1px solid ${PALETTE.gold}` : '1px solid transparent',
    }}>{children}</span>
  );
}

// ── Property row (icon + label + right-aligned value) ─────────
function PropRow({ label, value, last }) {
  const Ico = PROP_ICONS[label];
  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 14,
      padding: '14px 18px',
      borderBottom: last ? 'none' : `1px solid ${PALETTE.border}`,
    }}>
      <span style={{ color: PALETTE.gold, display: 'flex', width: 18 }}>
        {Ico ? <Ico size={18} /> : null}
      </span>
      <span style={{
        flex: 1, fontFamily: '"IBM Plex Sans", system-ui, sans-serif',
        fontSize: 14.5, color: PALETTE.text, fontWeight: 400,
      }}>{label}</span>
      <span style={{
        fontFamily: '"IBM Plex Mono", ui-monospace, monospace',
        fontSize: 14, fontWeight: 600, color: PALETTE.text,
        letterSpacing: '0.02em',
      }}>{value}</span>
    </div>
  );
}

// ── Section group (label + rows in panel) ─────────────────────
function PropSection({ label, data }) {
  const entries = Object.entries(data);
  return (
    <div style={{ marginBottom: 22 }}>
      <SectionLabel style={{ padding: '0 18px 8px' }}>{label}</SectionLabel>
      <div style={{
        background: PALETTE.panelLow,
        borderRadius: 14,
        border: `1px solid ${PALETTE.border}`,
        overflow: 'hidden',
      }}>
        {entries.map(([k, v], i) => (
          <PropRow key={k} label={k} value={v} last={i === entries.length - 1} />
        ))}
      </div>
    </div>
  );
}

// ── Primary CTA (full-width gold) ─────────────────────────────
function PrimaryCTA({ children, onClick, disabled, secondary, busy }) {
  return (
    <button onClick={disabled ? null : onClick} style={{
      width: '100%', padding: '17px 20px',
      background: secondary ? 'transparent' : (disabled ? PALETTE.panelHigh : PALETTE.gold),
      color: secondary ? PALETTE.gold : (disabled ? PALETTE.textMute : '#0D0D0D'),
      border: secondary ? `1px solid ${PALETTE.gold}` : 'none',
      borderRadius: 14,
      fontFamily: '"IBM Plex Sans", system-ui, sans-serif',
      fontSize: 13.5, fontWeight: 700, letterSpacing: '0.18em',
      textTransform: 'uppercase', cursor: disabled ? 'default' : 'pointer',
      display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 10,
    }}>
      {busy && (
        <span style={{
          width: 14, height: 14, borderRadius: '50%',
          border: '2px solid currentColor', borderTopColor: 'transparent',
          animation: 'fr-spin 0.8s linear infinite',
        }} />
      )}
      {children}
    </button>
  );
}

// ── Tab bar (3 tabs) ──────────────────────────────────────────
function TabBar({ tab, onChange }) {
  const TABS = [
    { id: 'camera', label: 'CAMERA', Icon: IconCamera },
    { id: 'library', label: 'LIBRARY', Icon: IconFolder },
    { id: 'profile', label: 'PROFILE', Icon: IconProfile },
  ];
  return (
    <div style={{
      display: 'flex', background: PALETTE.bg,
      borderTop: `1px solid ${PALETTE.border}`,
      padding: '8px 8px 4px',
    }}>
      {TABS.map(t => {
        const active = tab === t.id;
        return (
          <button key={t.id} onClick={() => onChange(t.id)} style={{
            flex: 1, position: 'relative',
            display: 'flex', flexDirection: 'column',
            alignItems: 'center', gap: 5, padding: '10px 0 6px',
            background: 'none', border: 'none', cursor: 'pointer',
            color: active ? PALETTE.gold : PALETTE.textMute,
          }}>
            <span style={{
              position: 'absolute', top: 0, left: '50%', transform: 'translateX(-50%)',
              width: active ? 24 : 0, height: 2, background: PALETTE.gold,
              transition: 'width 0.2s',
            }} />
            <t.Icon size={20} />
            <span style={{
              fontFamily: '"IBM Plex Mono", ui-monospace, monospace',
              fontSize: 9.5, fontWeight: 500, letterSpacing: '0.16em',
            }}>{t.label}</span>
          </button>
        );
      })}
    </div>
  );
}

// ── Slot chip (compact, scrollable row) ───────────────────────
function SlotChip({ slot, name, active, onClick, grid }) {
  return (
    <button onClick={onClick} style={{
      flexShrink: grid ? undefined : 0,
      width: grid ? '100%' : undefined,
      display: 'flex', alignItems: 'center', gap: 9,
      padding: '10px 14px 10px 11px',
      background: active ? PALETTE.panelHigh : PALETTE.panelLow,
      border: active ? `1px solid ${PALETTE.gold}` : `1px solid ${PALETTE.border}`,
      borderRadius: 12, cursor: 'pointer',
      transition: 'border-color 0.18s, background 0.18s',
      overflow: 'hidden',
    }}>
      <span style={{
        fontFamily: '"IBM Plex Mono", ui-monospace, monospace',
        fontSize: 12, fontWeight: 700, color: PALETTE.gold,
        letterSpacing: '0.04em', flexShrink: 0,
      }}>{slot}</span>
      <span style={{
        fontFamily: '"IBM Plex Sans", system-ui, sans-serif',
        fontSize: 12.5, fontWeight: 600, color: active ? PALETTE.text : PALETTE.textMute,
        letterSpacing: '0.05em', textTransform: 'uppercase',
        overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
      }}>{name}</span>
    </button>
  );
}

// ── Drag handle ───────────────────────────────────────────────
function DragHandle() {
  return (
    <div style={{
      width: 44, height: 4, borderRadius: 2,
      background: 'rgba(255,255,255,0.16)', margin: '10px auto 6px',
    }} />
  );
}

Object.assign(window, {
  PALETTE, Wordmark, AppHeader, SectionLabel, Pill, PropRow, PropSection,
  PrimaryCTA, TabBar, SlotChip, DragHandle,
});
