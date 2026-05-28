// app.jsx — top-level state + screen routing

const TWEAK_DEFAULTS = /*EDITMODE-BEGIN*/{
  "connected": true,
  "cameraModel": "X-H2",
  "sheetState": "peek",
  "accent": "#C99A4E"
}/*EDITMODE-END*/;

function App() {
  const [t, setTweak] = useTweaks(TWEAK_DEFAULTS);
  const [tab, setTab] = React.useState('camera');
  const [slotIdx, setSlotIdx] = React.useState(0);
  const [librarySort, setLibrarySort] = React.useState('NEWEST');
  const [detail, setDetail] = React.useState(null); // recipe object when open
  const [writeBusy, setWriteBusy] = React.useState(false);
  const [toast, setToast] = React.useState(null);
  const [camDetail, setCamDetail] = React.useState(null); // {cam, idx}
  const [cameraNames, setCameraNames] = React.useState(['My Camera', 'My Camera', 'My Camera']);

  // Apply accent color override at runtime
  React.useEffect(() => {
    PALETTE.gold = t.accent || '#C99A4E';
    PALETTE.goldDim = hexToRgba(PALETTE.gold, 0.16);
    PALETTE.goldFaint = hexToRgba(PALETTE.gold, 0.08);
  }, [t.accent]);

  const handleWrite = () => {
    setWriteBusy(true);
    setTimeout(() => {
      setWriteBusy(false);
      const r = detail || RECIPES[slotIdx];
      setToast({ slot: r.slot || 'C1', name: r.name });
      setTimeout(() => setToast(null), 2400);
      if (detail) setDetail(null);
    }, 1400);
  };

  const openSlotDetail = () => setDetail(RECIPES[slotIdx]);
  const openLibraryItem = (r) => setDetail({ ...r, slot: null });

  let screen;
  if (tab === 'camera') {
    screen = t.connected
      ? <CameraConnected
          cameraModel={t.cameraModel}
          slotIdx={slotIdx}
          setSlotIdx={setSlotIdx}
          sheetState={t.sheetState}
          setSheetState={(v) => setTweak('sheetState', typeof v === 'function' ? v(t.sheetState) : v)}
          onOpenDetail={openSlotDetail}
          onWrite={handleWrite}
          writeBusy={writeBusy}
          cameraNames={cameraNames}
          onOpenCameraDetail={(idx, cam) => setCamDetail({ idx, cam })}
        />
      : <ConnectGuide onConnect={() => setTweak('connected', true)} />;
  } else if (tab === 'library') {
    screen = <LibraryScreen onOpenLibraryItem={openLibraryItem}
      sortBy={librarySort} setSortBy={setLibrarySort} />;
  } else {
    screen = <ProfileScreen cameraModel={t.cameraModel} connected={t.connected} />;
  }

  return (
    <div style={{
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      minHeight: '100vh', background: '#1a1a1a',
      padding: '24px 0',
    }}>
      <AndroidDevice dark width={412} height={892}>
        <div style={{
          height: '100%', background: PALETTE.bg, color: PALETTE.text,
          display: 'flex', flexDirection: 'column', overflow: 'hidden',
          position: 'relative',
        }}>
          <AppHeader connected={t.connected}
            cameraModel={t.cameraModel}
            sheetExpanded={t.sheetState === 'expanded'}
            tab={tab}
            onReconnect={() => setTweak('connected', !t.connected)} />
          <div style={{ flex: 1, position: 'relative', overflow: 'hidden' }}>
            {screen}
            {detail && (
              <RecipeDetail recipe={detail} connected={t.connected}
                onClose={() => setDetail(null)}
                onAction={handleWrite} busy={writeBusy} />
            )}
            {camDetail && (
              <CameraDetailModal
                cam={camDetail.cam}
                name={cameraNames[camDetail.idx]}
                onRename={(n) => setCameraNames(ns => ns.map((v, i) => i === camDetail.idx ? n : v))}
                onDelete={() => setCamDetail(null)}
                onClose={() => setCamDetail(null)}
              />
            )}
            <WriteToast show={!!toast} {...(toast || {})} />
          </div>
          <TabBar tab={tab} onChange={setTab} />
        </div>
      </AndroidDevice>

      <TweaksPanel>
        <TweakSection label="Camera state" />
        <TweakToggle label="USB connected" value={t.connected}
          onChange={(v) => setTweak('connected', v)} />
        <TweakRadio label="Camera model" value={t.cameraModel}
          options={['X-H2', 'X-T5', 'X100VI']}
          onChange={(v) => setTweak('cameraModel', v)} />
        <TweakRadio label="Bottom sheet" value={t.sheetState}
          options={['peek', 'expanded']}
          onChange={(v) => setTweak('sheetState', v)} />

        <TweakSection label="Accent" />
        <TweakColor label="Accent color" value={t.accent}
          options={['#C99A4E', '#B07A2B', '#A8895A', '#D4AF37', '#E8E8E8']}
          onChange={(v) => setTweak('accent', v)} />
      </TweaksPanel>
    </div>
  );
}

function hexToRgba(hex, a) {
  const m = hex.replace('#','').match(/.{2}/g);
  const [r,g,b] = m.map(x => parseInt(x, 16));
  return `rgba(${r},${g},${b},${a})`;
}

ReactDOM.createRoot(document.getElementById('root')).render(<App />);
