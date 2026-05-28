// screens.jsx — Each top-level screen + the bottom sheet for the camera tab.

// ── Camera DISCONNECTED: connect guide ────────────────────────
function ConnectGuide({ onConnect }) {
  const steps = [
  {
    n: 1, h: 'On your camera',
    body: <>
        <div style={{ color: PALETTE.textMute, fontSize: 13.5, lineHeight: 1.55, marginTop: 4 }}>
          Menu → Connection Setting → USB Mode
        </div>
        <div style={{ marginTop: 12 }}>
          <span style={{
          display: 'inline-block',
          padding: '8px 14px',
          background: PALETTE.goldDim,
          border: `1px solid ${PALETTE.gold}`,
          borderRadius: 8,
          fontFamily: '"IBM Plex Sans", system-ui, sans-serif',
          fontSize: 11.5, fontWeight: 700, letterSpacing: '0.14em',
          color: PALETTE.gold
        }}>USB RAW CONV.</span>
        </div>
      </>
  },
  {
    n: 2, h: 'Plug in a USB-C data cable',
    body: <div style={{ color: PALETTE.textMute, fontSize: 13.5, lineHeight: 1.55, marginTop: 4 }}>
        Phone to camera, directly — avoid USB hubs.
      </div>
  },
  {
    n: 3, h: 'Accept the USB prompt',
    body: <div style={{ color: PALETTE.textMute, fontSize: 13.5, lineHeight: 1.55, marginTop: 4 }}>
        Tap OK on the permission dialog when it appears.
      </div>
  }];

  return (
    <div style={{
      flex: 1, display: 'flex', flexDirection: 'column',
      padding: '20px 20px 24px', overflowY: 'auto'
    }}>
      <div style={{
        display: 'flex', flexDirection: 'column', alignItems: 'center',
        marginTop: 38, marginBottom: 28
      }}>
        <div style={{
          width: 78, height: 78, borderRadius: 20,
          background: PALETTE.panelLow, border: `1px solid ${PALETTE.border}`,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          color: PALETTE.gold
        }}>
          <IconUSB size={36} />
        </div>
      </div>
      <h1 style={{
        margin: '0 0 32px', textAlign: 'center',
        fontFamily: '"IBM Plex Sans", system-ui, sans-serif',
        fontSize: 22, fontWeight: 700, letterSpacing: '0.12em',
        color: PALETTE.text, textTransform: 'uppercase'
      }}>Connect Your Camera</h1>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 0 }}>
        {steps.map((s) =>
        <div key={s.n} style={{
          display: 'flex', gap: 16,
          padding: '20px 0',
          borderTop: s.n === 1 ? `1px solid ${PALETTE.border}` : 'none',
          borderBottom: `1px solid ${PALETTE.border}`
        }}>
            <div style={{
            width: 28, height: 28, borderRadius: '50%',
            background: PALETTE.goldFaint,
            border: `1px solid ${PALETTE.gold}`,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontFamily: '"IBM Plex Mono", monospace',
            fontSize: 13, fontWeight: 600, color: PALETTE.gold,
            flexShrink: 0
          }}>{s.n}</div>
            <div style={{ flex: 1 }}>
              <div style={{
              fontFamily: '"IBM Plex Sans", system-ui, sans-serif',
              fontSize: 15.5, fontWeight: 600, color: PALETTE.text
            }}>{s.h}</div>
              {s.body}
            </div>
          </div>
        )}
      </div>

      <div style={{ flex: 1, minHeight: 24 }} />

      <button onClick={onConnect} style={{
        background: 'none', border: 'none', cursor: 'pointer',
        color: PALETTE.gold, padding: '20px 0 8px',
        fontFamily: '"IBM Plex Sans", system-ui, sans-serif',
        fontSize: 13.5, fontWeight: 500,
        textDecoration: 'underline', textDecorationThickness: 1,
        textUnderlineOffset: 4
      }}>Trouble connecting?</button>
      <div style={{
        textAlign: 'center', color: PALETTE.textDim, fontSize: 10.5,
        fontFamily: '"IBM Plex Mono", monospace', letterSpacing: '0.1em',
        marginTop: 14
      }}>
        <button onClick={onConnect} style={{
          background: 'none', border: `1px dashed ${PALETTE.border}`,
          color: PALETTE.textMute, cursor: 'pointer',
          padding: '8px 14px', borderRadius: 8,
          fontFamily: '"IBM Plex Mono", monospace', fontSize: 10.5,
          letterSpacing: '0.14em'
        }}>SIMULATE CONNECT →</button>
      </div>
    </div>);

}

// ── Camera CONNECTED: hero + bottom sheet (peek / expanded) ──
function CameraConnected({ cameraModel, slotIdx, setSlotIdx, sheetState, setSheetState,
  onOpenDetail, onWrite, writeBusy,
  onOpenCameraDetail, cameraNames }) {
  const recipe = RECIPES[slotIdx];

  const CAMERAS = [
  { model: cameraModel, firmware: '7.10', battery: '87%', connected: true, img: 'assets/XH2.png', usbId: '04CB:0128', lastSync: 'Today, 9:14 AM' },
  { model: 'X-T5', firmware: '2.20', battery: '—', connected: false, img: 'assets/Xt5.png', usbId: '04CB:0131', lastSync: 'May 24, 2:33 PM' },
  { model: 'X100VI', firmware: '2.01', battery: '—', connected: false, img: 'assets/X100VI.png', usbId: '04CB:0145', lastSync: 'May 21, 11:05 AM' }];

  const [camIdx, setCamIdx] = React.useState(0);

  // Swipe state
  const trackRef = React.useRef(null);
  const dragStart = React.useRef(null);
  const [dragOffset, setDragOffset] = React.useState(0);
  const [isDragging, setIsDragging] = React.useState(false);

  const onSwipeDown = (e) => {
    trackRef.current?.setPointerCapture(e.pointerId);
    dragStart.current = { x: e.clientX, idx: camIdx };
    setIsDragging(true);
  };
  const onSwipeMove = (e) => {
    if (!dragStart.current) return;
    setDragOffset(e.clientX - dragStart.current.x);
  };
  const onSwipeUp = (e) => {
    if (!dragStart.current) return;
    const dx = e.clientX - dragStart.current.x;
    const w = trackRef.current?.parentElement?.offsetWidth || 340;
    if (Math.abs(dx) < 6) {onOpenCameraDetail?.(dragStart.current.idx, CAMERAS[dragStart.current.idx]);} else
    if (dx < -w * 0.25 && camIdx < CAMERAS.length - 1) setCamIdx((i) => i + 1);else
    if (dx > w * 0.25 && camIdx > 0) setCamIdx((i) => i - 1);
    setDragOffset(0);
    setIsDragging(false);
    dragStart.current = null;
  };

  // Sheet drag
  const PEEK_H = 220;
  const sheetRef = React.useRef(null);
  const [drag, setDrag] = React.useState(null);
  const [slotGridCollapsed, setSlotGridCollapsed] = React.useState(false);
  React.useEffect(() => {if (sheetState === 'peek') setSlotGridCollapsed(false);}, [sheetState]);
  const onPointerDown = (e) => {
    e.currentTarget.setPointerCapture(e.pointerId);
    setDrag({ startY: e.clientY, base: sheetState });
  };
  const onPointerMove = (e) => {
    if (!drag) return;
    const dy = e.clientY - drag.startY;
    if (drag.base === 'peek' && dy < -40) setSheetState('expanded');
    if (drag.base === 'expanded' && dy > 60) setSheetState('peek');
  };
  const onPointerUp = () => setDrag(null);

  return (
    <div style={{ position: 'relative', height: '100%', overflow: 'hidden' }}>
      {/* Top zone */}
      <div style={{ padding: '16px 16px 24px', height: '100%', boxSizing: 'border-box', position: 'relative' }}>
        <div style={{ position: 'relative', zIndex: 1 }}>

          {/* Swipeable camera card */}
          <div style={{ overflow: 'hidden', borderRadius: 18 }}>
            <div
              ref={trackRef}
              onPointerDown={onSwipeDown}
              onPointerMove={onSwipeMove}
              onPointerUp={onSwipeUp}
              onPointerCancel={onSwipeUp}
              style={{
                display: 'flex',
                transform: `translateX(calc(${-camIdx * 100}% + ${dragOffset}px))`,
                transition: isDragging ? 'none' : 'transform 0.32s cubic-bezier(.22,.61,.36,1)',
                touchAction: 'pan-y',
                cursor: isDragging ? 'grabbing' : 'grab',
                userSelect: 'none'
              }}>
              {CAMERAS.map((cam, i) =>
              <div key={i} style={{
                flexShrink: 0, width: '100%',
                background: PALETTE.panelLow,
                border: `1px solid ${PALETTE.border}`,
                borderRadius: 18,
                padding: '18px 20px 20px',
                position: 'relative', overflow: 'hidden',
                boxSizing: 'border-box'
              }}>
                  {/* Camera PNG */}
                  <img src={cam.img} alt={cam.model} style={{
                  position: 'absolute',
                  top: 0, right: -30,
                  width: 260, height: 'auto',
                  pointerEvents: 'none',
                  filter: 'drop-shadow(0 20px 30px rgba(0,0,0,0.6))',
                  maskImage: 'linear-gradient(to right, transparent 0%, #000 35%, #000 100%)',
                  WebkitMaskImage: 'linear-gradient(to right, transparent 0%, #000 35%, #000 100%)'
                }} />
                  <div style={{ position: 'relative', zIndex: 1 }}>
                    <SectionLabel style={{ marginBottom: 10 }}>{cameraNames[i]}</SectionLabel>
                    <h1 style={{
                    margin: '0 0 18px',
                    fontFamily: '"IBM Plex Sans", system-ui, sans-serif',
                    fontSize: 44, fontWeight: 700, letterSpacing: '-0.01em',
                    color: PALETTE.text, lineHeight: 1
                  }}>{cam.model}</h1>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                      <Meta label="Firmware" value={cam.firmware} />
                      <Meta label="Battery" value={cam.battery} />
                    </div>
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* Action buttons */}
          <div style={{
            display: 'flex', gap: 0, marginTop: 10,
            border: `1px solid ${PALETTE.border}`,
            borderRadius: 12, overflow: 'hidden'
          }}>
            <button style={{
              flex: 1, padding: '14px 12px', background: PALETTE.panelLow,
              border: 'none', borderRight: `1px solid ${PALETTE.border}`,
              cursor: 'pointer',
              fontFamily: '"IBM Plex Sans", system-ui, sans-serif',
              fontSize: 11.5, fontWeight: 600, letterSpacing: '0.12em',
              textTransform: 'uppercase', color: PALETTE.textMute,
              display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8
            }}>
              <IconSort size={14} style={{ color: PALETTE.gold }} />
              Backup Settings
            </button>
            <button style={{
              flex: 1, padding: '14px 12px', background: PALETTE.panelLow,
              border: 'none', cursor: 'pointer',
              fontFamily: '"IBM Plex Sans", system-ui, sans-serif',
              fontSize: 11.5, fontWeight: 600, letterSpacing: '0.12em',
              textTransform: 'uppercase', color: PALETTE.textMute,
              display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8
            }}>
              <IconRefresh size={14} style={{ color: PALETTE.gold }} />
              Restore Previous
            </button>
          </div>

          {/* Pagination dots below action buttons */}
          <div style={{
            display: 'flex', justifyContent: 'center', alignItems: 'center',
            gap: 8, padding: '10px 0 0'
          }}>
            {CAMERAS.map((c, i) =>
            <button key={i} onClick={() => setCamIdx(i)} style={{
              background: 'none', border: 'none', padding: 4, cursor: 'pointer',
              display: 'flex', alignItems: 'center', justifyContent: 'center'
            }}>
                <span style={{
                display: 'block',
                width: 7, height: 7, borderRadius: 999,
                background: i === camIdx ?
                c.connected ? '#52C76A' : 'rgba(255,255,255,0.7)' :
                c.connected ? '#52C76A' : 'rgba(255,255,255,0.22)',
                opacity: i === camIdx ? 1 : 0.5,
                transition: 'opacity 0.2s, background 0.2s'
              }} />
              </button>
            )}
          </div>
        </div>
      </div>


            {/* Bottom sheet */}
      <div ref={sheetRef} style={{
        position: 'absolute', left: 0, right: 0, bottom: 0,
        height: sheetState === 'expanded' ? '98%' : PEEK_H,
        background: PALETTE.panelLow,
        borderTop: `1px solid ${PALETTE.borderStrong}`,
        borderRadius: '20px 20px 0 0',
        boxShadow: '0 -30px 60px rgba(0,0,0,0.6)',
        transition: 'height 0.32s cubic-bezier(.22,.61,.36,1)',
        display: 'flex', flexDirection: 'column',
        touchAction: 'none', overflow: 'hidden',
        zIndex: 2
      }}>
        {/* Drag handle area */}
        <div
          onPointerDown={onPointerDown}
          onPointerMove={onPointerMove}
          onPointerUp={onPointerUp}
          onPointerCancel={onPointerUp}
          onClick={() => setSheetState((s) => s === 'peek' ? 'expanded' : 'peek')}
          style={{ cursor: 'grab', userSelect: 'none' }}>
          <DragHandle />
        </div>

        {/* Slot selector row / grid */}
        <div style={{ padding: '4px 0 14px', position: 'relative' }}>
          {sheetState === 'expanded' ?
          <div style={{
            maxHeight: slotGridCollapsed ? 0 : 320,
            opacity: slotGridCollapsed ? 0 : 1,
            overflow: 'hidden',
            transition: 'max-height 0.38s cubic-bezier(.22,.61,.36,1), opacity 0.25s ease'
          }}>
              <div style={{
              display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 8,
              padding: '0 18px'
            }} data-comment-anchor="42098dc5fb-div-327-15">
                {RECIPES.map((r, i) =>
              <SlotChip key={r.slot} slot={r.slot} name={r.name}
              active={i === slotIdx} grid
              onClick={() => setSlotIdx(i)} />
              )}
              </div>
            </div> :

          <>
              <div style={{
              display: 'flex', gap: 8, overflowX: 'auto', overflowY: 'hidden',
              padding: '0 18px', scrollbarWidth: 'none'
            }} className="no-scrollbar">
                {RECIPES.map((r, i) =>
              <SlotChip key={r.slot} slot={r.slot} name={r.name}
              active={i === slotIdx}
              onClick={() => setSlotIdx(i)} />
              )}
              </div>
              {/* Right fade */}
              <div style={{
              position: 'absolute', top: 0, right: 0, bottom: 0, width: 48,
              background: `linear-gradient(to right, transparent, ${PALETTE.panelLow})`,
              pointerEvents: 'none'
            }} />
            </>
          }
        </div>

        {/* Selected slot summary (peek) OR full detail (expanded) */}
        {sheetState === 'peek' ?
        <div style={{ padding: '6px 20px 16px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: 10 }}>
              <div>
                <div style={{
                fontFamily: '"IBM Plex Sans", system-ui, sans-serif',
                fontSize: 10.5, fontWeight: 500, letterSpacing: '0.18em',
                textTransform: 'uppercase', color: PALETTE.textMute
              }}>{recipe.sim}</div>
                <div style={{
                fontFamily: '"IBM Plex Sans", system-ui, sans-serif',
                fontSize: 26, fontWeight: 700, color: PALETTE.text,
                letterSpacing: '0.01em', marginTop: 2
              }}>{recipe.name}</div>
              </div>
              <button onClick={onOpenDetail} style={{
              background: 'none', border: 'none', cursor: 'pointer',
              color: PALETTE.gold, display: 'flex', alignItems: 'center', gap: 6,
              fontFamily: '"IBM Plex Sans", system-ui, sans-serif',
              fontSize: 11, fontWeight: 600, letterSpacing: '0.18em',
              padding: 0
            }}>EDIT <IconChevron size={14} /></button>
            </div>
            <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
              {recipe.pills.map((p) => <Pill key={p} large>{p}</Pill>)}
            </div>
            <button onClick={() => setSheetState('expanded')} style={{
            background: 'none', border: 'none', cursor: 'pointer',
            marginTop: 18, color: PALETTE.textMute,
            fontFamily: '"IBM Plex Sans", system-ui, sans-serif',
            fontSize: 11, letterSpacing: '0.18em', fontWeight: 500,
            padding: 0
          }}>↑ PULL UP FOR FULL RECIPE</button>
          </div> :

        <ExpandedRecipe recipe={recipe} onWrite={onWrite} writeBusy={writeBusy} onOpenDetail={onOpenDetail} onScrollChange={(top) => setSlotGridCollapsed(top > 40)} />
        }
      </div>
    </div>);

}

function Meta({ label, value }) {
  return (
    <div style={{ display: 'flex', alignItems: 'baseline', gap: 10 }}>
      <span style={{
        fontFamily: '"IBM Plex Sans", system-ui, sans-serif',
        fontSize: 10.5, letterSpacing: '0.22em', textTransform: 'uppercase',
        color: PALETTE.textDim, fontWeight: 500, width: 92
      }}>{label}</span>
      <span style={{
        fontFamily: '"IBM Plex Mono", monospace', fontSize: 12,
        color: PALETTE.textMute
      }}>{value}</span>
    </div>);

}

// ── Expanded recipe (inside the sheet, scrolls, sticky CTA) ──
function ExpandedRecipe({ recipe, onWrite, writeBusy, onOpenDetail, onScrollChange }) {
  return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minHeight: 0 }}>
      <div style={{ padding: '6px 20px 8px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }} data-comment-anchor="744c1158da-div-424-9">
          <div>
            <div style={{
              fontFamily: '"IBM Plex Sans", system-ui, sans-serif',
              fontSize: 10.5, fontWeight: 500, letterSpacing: '0.18em',
              textTransform: 'uppercase', color: PALETTE.textMute
            }}>{recipe.sim}</div>
            <div style={{
              fontFamily: '"IBM Plex Sans", system-ui, sans-serif',
              fontSize: 32, fontWeight: 700, color: PALETTE.text,
              marginTop: 2, letterSpacing: '0.01em'
            }}>{recipe.name}</div>
          </div>
          <button onClick={onOpenDetail} style={{
            background: 'none', border: 'none',
            padding: '4px 0 4px 8px', cursor: 'pointer',
            color: PALETTE.textMute, display: 'flex', alignItems: 'center', gap: 4,
            fontFamily: '"IBM Plex Sans", system-ui, sans-serif',
            fontSize: 10, letterSpacing: '0.18em', fontWeight: 600,
            alignSelf: 'flex-start', marginTop: 6
          }}>EDIT <IconChevron size={10} /></button>
        </div>
      </div>

      <div style={{ flex: 1, minHeight: 0, position: 'relative' }}>
        <div style={{ height: '100%', overflowY: 'auto', padding: '14px 18px 0' }} className="no-scrollbar"
        onScroll={(e) => onScrollChange?.(e.currentTarget.scrollTop)}>
          <PropSection label="Effects" data={recipe.effects} />
          <PropSection label="Tone" data={recipe.tone} />
        <PropSection label="White Balance" data={recipe.wb} />
        <div style={{ height: 100 }} />
        </div>
        {/* Bottom scroll fade */}
        <div style={{
          position: 'absolute', bottom: 0, left: 0, right: 0, height: 56,
          background: `linear-gradient(to top, ${PALETTE.panelLow}, transparent)`,
          pointerEvents: 'none'
        }} />
      </div>

      <div style={{
        padding: '12px 16px 18px',
        background: `linear-gradient(to top, ${PALETTE.panelLow} 70%, rgba(22,20,19,0))`,
        borderTop: `1px solid ${PALETTE.border}`
      }}>
        <PrimaryCTA onClick={onWrite} busy={writeBusy}>
          {writeBusy ? 'Writing to camera…' : 'Save to Library'}
        </PrimaryCTA>
      </div>
    </div>);

}

// ── LIBRARY screen ────────────────────────────────────────────
function LibraryScreen({ onOpenLibraryItem, sortBy, setSortBy }) {
  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
      <div style={{
        padding: '8px 20px 14px',
        display: 'flex', alignItems: 'center', justifyContent: 'space-between'
      }}>
        <h1 style={{
          margin: 0, fontFamily: '"IBM Plex Sans", system-ui, sans-serif',
          fontSize: 28, fontWeight: 700, letterSpacing: '0.04em',
          color: PALETTE.text, textTransform: 'uppercase'
        }}>Library</h1>
        <div style={{ display: 'flex', gap: 14, color: PALETTE.text }}>
          <button style={iconBtn}><IconSearch size={20} /></button>
          <button style={iconBtn}><IconFilter size={20} /></button>
          <button style={{
            ...iconBtn,
            background: PALETTE.gold, color: '#0D0D0D', borderRadius: '50%',
            width: 32, height: 32
          }}><IconPlus size={18} /></button>
        </div>
      </div>

      <div style={{
        padding: '8px 20px 12px', display: 'flex',
        justifyContent: 'space-between', alignItems: 'center',
        borderBottom: `1px solid ${PALETTE.border}`
      }}>
        <span style={{
          fontFamily: '"IBM Plex Mono", monospace', fontSize: 11,
          color: PALETTE.textMute, letterSpacing: '0.14em'
        }}>{LIBRARY.length} RECIPES</span>
        <button onClick={() => setSortBy(sortBy === 'NEWEST' ? 'NAME' : 'NEWEST')} style={{
          background: 'none', border: 'none', cursor: 'pointer',
          color: PALETTE.text, display: 'flex', alignItems: 'center', gap: 6,
          fontFamily: '"IBM Plex Sans", system-ui, sans-serif',
          fontSize: 11, letterSpacing: '0.18em', fontWeight: 600
        }}><IconSort size={14} /> {sortBy}</button>
      </div>

      <div style={{ flex: 1, overflowY: 'auto' }} className="no-scrollbar">
        {[...LIBRARY].sort((a, b) => sortBy === 'NAME' ? a.name.localeCompare(b.name) : 0).
        map((r) =>
        <button key={r.id} onClick={() => onOpenLibraryItem(r)} style={{
          display: 'block', width: '100%', textAlign: 'left',
          background: 'none', border: 'none', cursor: 'pointer',
          padding: '20px 20px',
          borderBottom: `1px solid ${PALETTE.border}`
        }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{
              padding: '4px 8px', border: `1px solid ${PALETTE.gold}`,
              color: PALETTE.gold, borderRadius: 4,
              fontFamily: '"IBM Plex Mono", monospace',
              fontSize: 9.5, fontWeight: 600, letterSpacing: '0.14em',
              textTransform: 'uppercase'
            }}>{r.sim}</span>
              <span style={{
              fontFamily: '"IBM Plex Mono", monospace', fontSize: 10.5,
              color: PALETTE.textDim, letterSpacing: '0.1em'
            }}>SAVED {r.saved.toUpperCase()}</span>
            </div>
            <div style={{
            fontFamily: '"IBM Plex Sans", system-ui, sans-serif',
            fontSize: 28, fontWeight: 700, color: PALETTE.text,
            margin: '8px 0 12px', letterSpacing: '0.01em'
          }}>{r.name}</div>
            <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
              {r.pills.map((p) => <Pill key={p}>{p}</Pill>)}
            </div>
          </button>
        )}
        <div style={{ height: 24 }} />
      </div>
    </div>);

}

const iconBtn = {
  background: 'none', border: 'none', cursor: 'pointer',
  color: PALETTE.text, padding: 4, display: 'flex', alignItems: 'center', justifyContent: 'center'
};

// ── PROFILE screen ────────────────────────────────────────────
function ProfileScreen({ cameraModel, connected }) {
  return (
    <div style={{ padding: '12px 20px', overflowY: 'auto', height: '100%' }} className="no-scrollbar">
      <h1 style={{
        margin: '8px 0 24px',
        fontFamily: '"IBM Plex Sans", system-ui, sans-serif',
        fontSize: 28, fontWeight: 700, letterSpacing: '0.04em',
        color: PALETTE.text, textTransform: 'uppercase'
      }}>Profile</h1>

      <div style={{
        background: PALETTE.panelLow, borderRadius: 14, padding: 18,
        border: `1px solid ${PALETTE.border}`, marginBottom: 16
      }}>
        <SectionLabel>Camera</SectionLabel>
        <div style={{
          fontFamily: '"IBM Plex Sans", system-ui, sans-serif',
          fontSize: 22, fontWeight: 700, color: PALETTE.text,
          marginTop: 8
        }}>{cameraModel}</div>
        <div style={{
          marginTop: 4, fontFamily: '"IBM Plex Mono", monospace',
          fontSize: 11, color: connected ? PALETTE.gold : PALETTE.textMute,
          letterSpacing: '0.14em'
        }}>{connected ? '● CONNECTED VIA USB' : '○ OFFLINE'}</div>
      </div>

      {[
      ['Account', 'm.takahashi@studio.jp'],
      ['Cloud sync', 'Off'],
      ['Default film sim', 'Classic Chrome'],
      ['Units', 'Metric'],
      ['App version', '0.4.1 (preview)'],
      ['Open source licenses', '›'],
      ['Send feedback', '›']].
      map(([k, v]) =>
      <div key={k} style={{
        display: 'flex', justifyContent: 'space-between', alignItems: 'center',
        padding: '16px 4px', borderBottom: `1px solid ${PALETTE.border}`
      }}>
          <span style={{
          fontFamily: '"IBM Plex Sans", system-ui, sans-serif',
          fontSize: 14.5, color: PALETTE.text
        }}>{k}</span>
          <span style={{
          fontFamily: '"IBM Plex Mono", monospace', fontSize: 12,
          color: PALETTE.textMute
        }}>{v}</span>
        </div>
      )}
      <div style={{ height: 24 }} />
    </div>);

}

// ── Recipe Detail / Edit (full screen overlay) ────────────────
function RecipeDetail({ recipe, connected, onClose, onAction, busy }) {
  return (
    <div style={{
      position: 'absolute', inset: 0, background: PALETTE.bg,
      zIndex: 10, display: 'flex', flexDirection: 'column',
      animation: 'fr-slide-up 0.3s cubic-bezier(.22,.61,.36,1)'
    }}>
      <div style={{
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        padding: '14px 14px'
      }}>
        <button onClick={onClose} style={{
          background: 'none', border: 'none', cursor: 'pointer',
          color: PALETTE.text, display: 'flex', alignItems: 'center', gap: 8,
          padding: 4, fontFamily: '"IBM Plex Sans", system-ui, sans-serif',
          fontSize: 13, letterSpacing: '0.18em', fontWeight: 600
        }}><IconClose size={18} /> CLOSE</button>
        <div style={{ display: 'flex', gap: 6 }}>
          <button style={{
            ...iconBtn, color: PALETTE.gold, gap: 8,
            fontFamily: '"IBM Plex Sans", system-ui, sans-serif',
            fontSize: 12, letterSpacing: '0.18em', fontWeight: 600
          }}><IconFilter size={16} /> EDIT</button>
          <button style={{ ...iconBtn, color: PALETTE.textMute }}><IconStar size={20} /></button>
          <button style={{ ...iconBtn, color: PALETTE.textMute }}><IconMore size={20} /></button>
        </div>
      </div>

      <div style={{ flex: 1, overflowY: 'auto' }} className="no-scrollbar">
        <div style={{
          padding: '12px 22px 22px',
          background: PALETTE.panelLow,
          margin: '0 14px',
          borderRadius: 16,
          border: `1px solid ${PALETTE.border}`
        }}>
          <span style={{
            display: 'inline-block',
            padding: '4px 8px', border: `1px solid ${PALETTE.gold}`,
            color: PALETTE.gold, borderRadius: 4,
            fontFamily: '"IBM Plex Mono", monospace',
            fontSize: 9.5, fontWeight: 600, letterSpacing: '0.14em',
            textTransform: 'uppercase'
          }}>{recipe.sim}</span>
          <h1 style={{
            margin: '10px 0 14px',
            fontFamily: '"IBM Plex Sans", system-ui, sans-serif',
            fontSize: 38, fontWeight: 700, color: PALETTE.text,
            letterSpacing: '0.01em', lineHeight: 1
          }}>{recipe.name}</h1>
          <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginBottom: 12 }}>
            {recipe.pills.slice(0, 3).map((p) => <Pill key={p} large>{p}</Pill>)}
          </div>
          {recipe.description &&
          <p style={{
            margin: '4px 0 0',
            fontFamily: '"IBM Plex Sans", system-ui, sans-serif',
            fontSize: 13, lineHeight: 1.6, color: PALETTE.textMute,
            textWrap: 'pretty'
          }}>{recipe.description}</p>
          }
        </div>

        <div style={{ padding: '20px 14px 0' }}>
          <PropSection label="Effects" data={recipe.effects} />
          <PropSection label="Tone" data={recipe.tone} />
          <PropSection label="White Balance" data={recipe.wb} />
        </div>

        {recipe.slot &&
        <div style={{ padding: '0 14px 24px' }}>
            <SectionLabel style={{ padding: '0 4px 10px' }}>Install to slot</SectionLabel>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7,1fr)', gap: 8 }}>
              {RECIPES.map((r) =>
            <div key={r.slot} style={{
              aspectRatio: '1/1', borderRadius: 10,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              background: r.slot === recipe.slot ? PALETTE.gold : PALETTE.panelHigh,
              color: r.slot === recipe.slot ? '#0D0D0D' : PALETTE.textMute,
              fontFamily: '"IBM Plex Mono", monospace',
              fontSize: 12, fontWeight: 700, letterSpacing: '0.04em',
              border: r.slot === recipe.slot ? 'none' : `1px solid ${PALETTE.border}`
            }}>{r.slot}</div>
            )}
            </div>
          </div>
        }
        <div style={{ height: 100 }} />
      </div>

      <div style={{
        padding: '12px 16px 18px',
        background: PALETTE.bg,
        borderTop: `1px solid ${PALETTE.border}`
      }}>
        <PrimaryCTA onClick={onAction} busy={busy} disabled={!connected && !recipe.slot}>
          {busy ? 'Writing…' : connected ? `Write to ${recipe.slot || 'C1'}` : 'Save to Library'}
        </PrimaryCTA>
      </div>
    </div>);

}

// ── Write toast ───────────────────────────────────────────────
function WriteToast({ show, slot, name }) {
  if (!show) return null;
  return (
    <div style={{
      position: 'absolute', left: '50%', bottom: 84,
      transform: 'translateX(-50%)', zIndex: 20,
      background: PALETTE.panelHigh, color: PALETTE.text,
      border: `1px solid ${PALETTE.gold}`,
      padding: '12px 18px', borderRadius: 12,
      display: 'flex', alignItems: 'center', gap: 12,
      animation: 'fr-toast 0.3s cubic-bezier(.22,.61,.36,1)'
    }}>
      <span style={{ color: PALETTE.gold, display: 'flex' }}><IconCheck size={20} /></span>
      <div>
        <div style={{
          fontFamily: '"IBM Plex Sans", system-ui, sans-serif',
          fontSize: 13, fontWeight: 600
        }}>Wrote {name} → {slot}</div>
        <div style={{
          fontFamily: '"IBM Plex Mono", monospace', fontSize: 10.5,
          color: PALETTE.textMute, letterSpacing: '0.1em', marginTop: 2
        }}>RECIPE LIVE ON CAMERA</div>
      </div>
    </div>);

}

// ── Camera detail modal ───────────────────────────────────────
function CameraDetailModal({ cam, name, onRename, onDelete, onClose }) {
  const [editing, setEditing] = React.useState(false);
  const [draft, setDraft] = React.useState(name);
  const inputRef = React.useRef(null);

  React.useEffect(() => {setDraft(name);}, [name]);
  React.useEffect(() => {if (editing) inputRef.current?.focus();}, [editing]);

  const commitRename = () => {
    const trimmed = draft.trim() || 'My Camera';
    onRename(trimmed);
    setDraft(trimmed);
    setEditing(false);
  };

  const rows = [
  { label: 'Model', value: cam.model },
  { label: 'Firmware', value: cam.firmware },
  { label: 'Battery', value: cam.battery },
  { label: 'Last Sync', value: cam.lastSync }];


  return (
    <div style={{
      position: 'absolute', inset: 0, zIndex: 30,
      background: 'rgba(0,0,0,0.6)',
      display: 'flex', alignItems: 'flex-end'
    }} onClick={onClose}>
      <div onClick={(e) => e.stopPropagation()} style={{
        width: '100%',
        background: PALETTE.panelHigh,
        borderRadius: '22px 22px 0 0',
        borderTop: `1px solid ${PALETTE.borderStrong}`,
        boxShadow: '0 -24px 60px rgba(0,0,0,0.7)',
        animation: 'fr-slide-up 0.28s cubic-bezier(.22,.61,.36,1)',
        overflow: 'hidden'
      }}>
        {/* Drag handle */}
        <div style={{ padding: '12px 0 4px', display: 'flex', justifyContent: 'center' }}>
          <div style={{ width: 40, height: 4, borderRadius: 2, background: 'rgba(255,255,255,0.14)' }} />
        </div>

        {/* Title row */}
        <div style={{
          display: 'flex', justifyContent: 'space-between', alignItems: 'center',
          padding: '6px 22px 16px',
          borderBottom: `1px solid ${PALETTE.border}`
        }}>
          <div>
            <div style={{
              fontFamily: '"IBM Plex Mono", monospace',
              fontSize: 11, fontWeight: 700, letterSpacing: '0.12em',
              color: PALETTE.gold, marginBottom: 3
            }}>{cam.model}</div>
            <div style={{
              fontFamily: '"IBM Plex Sans", system-ui, sans-serif',
              fontSize: 20, fontWeight: 700, color: PALETTE.text, letterSpacing: '-0.01em'
            }}>{name}</div>
          </div>
          <button onClick={onClose} style={{
            width: 32, height: 32, borderRadius: '50%',
            background: 'rgba(255,255,255,0.08)', border: 'none',
            cursor: 'pointer', color: PALETTE.textMute,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontSize: 18, lineHeight: 1
          }}>×</button>
        </div>

        {/* Rename field */}
        <div style={{ padding: '16px 22px', borderBottom: `1px solid ${PALETTE.border}` }}>
          <div style={{
            fontSize: 9.5, fontWeight: 600, letterSpacing: '0.2em',
            textTransform: 'uppercase', color: PALETTE.textDim,
            fontFamily: '"IBM Plex Sans", system-ui, sans-serif', marginBottom: 8
          }}>Custom Label</div>
          {editing ?
          <div style={{ display: 'flex', gap: 8 }}>
              <input
              ref={inputRef}
              value={draft}
              onChange={(e) => setDraft(e.target.value)}
              onKeyDown={(e) => {if (e.key === 'Enter') commitRename();if (e.key === 'Escape') setEditing(false);}}
              style={{
                flex: 1, background: PALETTE.panelLow,
                border: `1.5px solid ${PALETTE.gold}`, borderRadius: 10,
                color: PALETTE.text, fontFamily: '"IBM Plex Sans", system-ui, sans-serif',
                fontSize: 15, fontWeight: 600, padding: '10px 13px', outline: 'none'
              }} />
            
              <button onClick={commitRename} style={{
              background: PALETTE.gold, border: 'none', borderRadius: 10,
              color: '#161005', fontFamily: '"IBM Plex Sans", system-ui, sans-serif',
              fontSize: 10.5, fontWeight: 700, letterSpacing: '0.13em',
              padding: '0 16px', cursor: 'pointer', flexShrink: 0
            }}>SAVE</button>
            </div> :

          <button onClick={() => setEditing(true)} style={{
            background: PALETTE.panelLow, border: `1px solid ${PALETTE.border}`,
            borderRadius: 10, width: '100%', textAlign: 'left',
            padding: '11px 14px', cursor: 'pointer',
            display: 'flex', justifyContent: 'space-between', alignItems: 'center'
          }}>
              <span style={{
              fontFamily: '"IBM Plex Sans", system-ui, sans-serif',
              fontSize: 15, fontWeight: 600, color: PALETTE.text
            }}>{name}</span>
              <span style={{
              fontSize: 9.5, letterSpacing: '0.18em', color: PALETTE.gold,
              fontFamily: '"IBM Plex Sans", system-ui, sans-serif', fontWeight: 600
            }}>RENAME ›</span>
            </button>
          }
        </div>

        {/* Info rows */}
        <div style={{ padding: '8px 22px 0' }}>
          {rows.map(({ label, value }, i) =>
          <div key={label} style={{
            display: 'flex', justifyContent: 'space-between', alignItems: 'center',
            padding: '11px 0',
            borderBottom: i < rows.length - 1 ? `1px solid ${PALETTE.border}` : 'none'
          }}>
              <span style={{
              fontFamily: '"IBM Plex Sans", system-ui, sans-serif',
              fontSize: 13, color: PALETTE.textMute, letterSpacing: '0.02em'
            }}>{label}</span>
              <span style={{
              fontFamily: '"IBM Plex Mono", monospace',
              fontSize: 12.5, color: PALETTE.text, fontWeight: 500
            }}>{value}</span>
            </div>
          )}
        </div>

        {/* USB ID + disclaimer */}
        <div style={{
          margin: '4px 22px 0',
          padding: '12px 14px',
          background: PALETTE.panelLow,
          border: `1px solid ${PALETTE.border}`,
          borderRadius: 10
        }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
            <span style={{ fontFamily: '"IBM Plex Sans", system-ui, sans-serif', fontSize: 12, color: PALETTE.textMute }}>USB ID</span>
            <span style={{ fontFamily: '"IBM Plex Mono", monospace', fontSize: 12, color: PALETTE.text, letterSpacing: '0.06em' }}>{cam.usbId}</span>
          </div>
          <div style={{
            fontFamily: '"IBM Plex Sans", system-ui, sans-serif',
            fontSize: 10.5, color: PALETTE.textDim, lineHeight: 1.55
          }}>Not the camera serial number — this is the USB device identifier assigned by the host.</div>
        </div>

        {/* Remove */}
        <div style={{ padding: '16px 22px 28px' }}>
          <button onClick={onDelete} style={{
            width: '100%', padding: '13px', borderRadius: 11,
            background: 'rgba(210,55,55,0.1)', border: '1px solid rgba(210,55,55,0.25)',
            color: '#D94040', fontFamily: '"IBM Plex Sans", system-ui, sans-serif',
            fontSize: 13, fontWeight: 600, letterSpacing: '0.06em', cursor: 'pointer'
          }}>Remove Camera</button>
        </div>
      </div>
    </div>);

}

Object.assign(window, {
  ConnectGuide, CameraConnected, LibraryScreen, ProfileScreen, RecipeDetail, WriteToast
});