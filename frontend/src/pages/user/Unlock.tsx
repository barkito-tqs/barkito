import { Camera, Keyboard, QrCode, ScanLine } from 'lucide-react';
import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ApiError, activeReservationOf, activeTripOf, getState, unlock, useStore } from '../../api/store';
import { useUser } from '../../auth/session';
import { Alert, PageHead } from '../../components/ui';
import { PRICES, formatCents } from '../../domain/pricing';
import { RULES } from '../../domain/rules';

type Mode = 'scan' | 'code';

const CODE_LENGTH = 6;
const MAX_PASSENGERS = Math.max(...Object.values(RULES.capacity));

/** The QR code holds the boat's unlock code, alone or at the end of a link (barkito.example/u/482915). */
export function codeFromQr(raw: string): string | null {
  const match = raw.match(/(\d{6})\s*$/);
  return match ? match[1] : null;
}

interface BarcodeDetectorLike {
  detect(source: CanvasImageSource): Promise<{ rawValue: string }[]>;
}

declare global {
  interface Window {
    BarcodeDetector?: new (options: { formats: string[] }) => BarcodeDetectorLike;
  }
}

/** Camera view that reads a QR code with the browser's BarcodeDetector, where it exists. */
function QrScanner({ onCode, onUnavailable }: { onCode: (code: string) => void; onUnavailable: (reason: string) => void }) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const [status, setStatus] = useState('Starting the camera…');
  // Kept in refs so a parent re-render does not restart the camera.
  const onCodeRef = useRef(onCode);
  const onUnavailableRef = useRef(onUnavailable);
  onCodeRef.current = onCode;
  onUnavailableRef.current = onUnavailable;

  useEffect(() => {
    let stream: MediaStream | null = null;
    let frame = 0;
    let cancelled = false;

    async function start() {
      if (!window.BarcodeDetector || !navigator.mediaDevices?.getUserMedia) {
        onUnavailableRef.current('This browser cannot read QR codes. Type the code printed below the QR code instead.');
        return;
      }
      try {
        stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } });
      } catch {
        onUnavailableRef.current('The camera is not available. Type the code printed below the QR code instead.');
        return;
      }
      if (cancelled || !videoRef.current) return;
      videoRef.current.srcObject = stream;
      await videoRef.current.play();
      setStatus('Point the camera at the QR code on the boat.');
      const detector = new window.BarcodeDetector({ formats: ['qr_code'] });
      const scan = async () => {
        if (cancelled || !videoRef.current) return;
        const found = await detector.detect(videoRef.current).catch(() => []);
        const code = found.map((f) => codeFromQr(f.rawValue)).find(Boolean);
        if (code) onCodeRef.current(code);
        else frame = window.requestAnimationFrame(scan);
      };
      frame = window.requestAnimationFrame(scan);
    }

    start();
    return () => {
      cancelled = true;
      window.cancelAnimationFrame(frame);
      stream?.getTracks().forEach((t) => t.stop());
    };
  }, []);

  return (
    <div className="scanner">
      <video ref={videoRef} muted playsInline aria-label="Camera preview" />
      <div className="scanner-frame" aria-hidden="true">
        <ScanLine />
      </div>
      <p className="scanner-status" aria-live="polite">
        {status}
      </p>
    </div>
  );
}

/** US-12: unlock a boat by scanning its QR code or typing the code below it. */
export function Unlock() {
  const user = useUser();
  const navigate = useNavigate();
  const boats = useStore((s) => s.boats);
  const docks = useStore((s) => s.docks);
  useStore((s) => s.reservations);
  const [mode, setMode] = useState<Mode>('scan');
  const [code, setCode] = useState('');
  const [passengers, setPassengers] = useState(1);
  const [error, setError] = useState<string | null>(null);
  const [cameraNote, setCameraNote] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const codeRef = useRef<HTMLInputElement>(null);

  const reservation = activeReservationOf(user.id);
  const reservedBoat = reservation ? boats.find((b) => b.id === reservation.boatId) : undefined;
  const reservedDock = reservedBoat ? docks.find((d) => d.id === reservedBoat.dockId) : undefined;
  const trip = activeTripOf(user.id);

  function submit(value: string) {
    setBusy(true);
    setError(null);
    try {
      unlock(user.id, value, passengers);
      navigate('/trip');
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Something went wrong. Try again.');
      setMode('code');
      setCode(value);
      window.requestAnimationFrame(() => codeRef.current?.focus());
    } finally {
      setBusy(false);
    }
  }

  function onSubmit(event: FormEvent) {
    event.preventDefault();
    if (code.trim().length !== CODE_LENGTH) {
      setError(`The code has ${CODE_LENGTH} digits. You find it printed below the QR code on the boat.`);
      codeRef.current?.focus();
      return;
    }
    submit(code);
  }

  if (trip) {
    return (
      <>
        <PageHead eyebrow="Unlock" title="You Are Already on a Trip" />
        <Alert tone="info">
          One boat at a time. <Link to="/trip">Open your trip</Link> to end it at a dock.
        </Alert>
      </>
    );
  }

  return (
    <>
      <PageHead
        eyebrow="Unlock"
        title="Unlock a Boat"
        lead="Scan the QR code on the boat, or type the 6-digit code printed below it if your phone has no camera."
      />

      {reservedBoat && (
        <Alert tone="info">
          You reserved <strong translate="no">{reservedBoat.code}</strong> at {reservedDock?.name}. Scan its QR code at the dock to
          start your trip; the hold fee is added to the trip.
        </Alert>
      )}

      <div className="grid-2 grid-split">
        <section className="card stack" aria-labelledby="unlock-title">
          <div className="row between">
            <h2 id="unlock-title">{mode === 'scan' ? 'Scan the QR Code' : 'Type the Code'}</h2>
            <div className="segmented" role="group" aria-label="How to unlock">
              <button type="button" aria-pressed={mode === 'scan'} onClick={() => setMode('scan')}>
                <Camera aria-hidden="true" width={16} height={16} />
                &nbsp;Scan
              </button>
              <button
                type="button"
                aria-pressed={mode === 'code'}
                onClick={() => {
                  setMode('code');
                  window.requestAnimationFrame(() => codeRef.current?.focus());
                }}
              >
                <Keyboard aria-hidden="true" width={16} height={16} />
                &nbsp;Code
              </button>
            </div>
          </div>

          {mode === 'scan' ? (
            cameraNote ? (
              <Alert tone="warn">
                {cameraNote}{' '}
                <button type="button" className="btn btn-sm btn-secondary" onClick={() => setMode('code')}>
                  Type the Code
                </button>
              </Alert>
            ) : (
              <QrScanner onCode={submit} onUnavailable={setCameraNote} />
            )
          ) : null}

          <form className="stack" onSubmit={onSubmit} noValidate hidden={mode === 'scan'}>
            <div className="field">
              <label htmlFor="unlock-code">Code below the QR code</label>
              <input
                ref={codeRef}
                id="unlock-code"
                name="unlockCode"
                className="input code-input"
                inputMode="numeric"
                autoComplete="off"
                spellCheck={false}
                maxLength={CODE_LENGTH}
                placeholder="e.g. 482915…"
                value={code}
                aria-invalid={error ? true : undefined}
                aria-describedby={error ? 'unlock-error' : 'unlock-help'}
                onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))}
              />
              {error ? (
                <p id="unlock-error" className="error" role="alert">
                  {error}
                </p>
              ) : (
                <p id="unlock-help" className="help">
                  Each boat has its own code. A boat reserved by someone else stays locked.
                </p>
              )}
            </div>
            <PassengerField value={passengers} onChange={setPassengers} />
            <button type="submit" className="btn btn-primary btn-lg btn-block" disabled={busy}>
              {busy ? 'Unlocking…' : 'Unlock and Start Trip'}
            </button>
          </form>

          {mode === 'scan' && (
            <>
              <PassengerField value={passengers} onChange={setPassengers} />
              {error && <Alert tone="danger">{error}</Alert>}
            </>
          )}
        </section>

        <aside className="card stack" aria-labelledby="price-title">
          <h2 id="price-title">What You Pay</h2>
          <dl className="facts">
            <div>
              <dt>Standard · up to {RULES.capacity.STANDARD} people</dt>
              <dd>
                {formatCents(PRICES.unlockFee.STANDARD)} + {formatCents(PRICES.perMinute.STANDARD)}/min
              </dd>
            </div>
            <div>
              <dt>XL · up to {RULES.capacity.XL} people</dt>
              <dd>
                {formatCents(PRICES.unlockFee.XL)} + {formatCents(PRICES.perMinute.XL)}/min
              </dd>
            </div>
            <div>
              <dt>Deposit held on your card</dt>
              <dd>
                {formatCents(PRICES.deposit.STANDARD)} / {formatCents(PRICES.deposit.XL)}
              </dd>
            </div>
          </dl>
          <p className="small muted">
            Every started minute counts. After {getState().settings.maxTripMinutes} minutes, minutes are charged as overtime. The
            deposit is released when you end the trip, minus what you owe.
          </p>
          {import.meta.env.DEV && <DemoCodes />}
        </aside>
      </div>
    </>
  );
}

function PassengerField({ value, onChange }: { value: number; onChange: (n: number) => void }) {
  return (
    <div className="field">
      <span className="label" id="passengers-label">
        People on board, including you
      </span>
      <div className="row">
        <div className="stepper" role="group" aria-labelledby="passengers-label">
          <button type="button" aria-label="One person fewer" disabled={value <= 1} onClick={() => onChange(value - 1)}>
            −
          </button>
          <output aria-live="polite">{value}</output>
          <button type="button" aria-label="One person more" disabled={value >= MAX_PASSENGERS} onClick={() => onChange(value + 1)}>
            +
          </button>
        </div>
        <span className="small muted">
          Standard up to {RULES.capacity.STANDARD}, XL up to {RULES.capacity.XL}
        </span>
      </div>
    </div>
  );
}

/** Development only: the codes printed on the boats, since there are no physical QR codes yet. */
function DemoCodes() {
  const boats = useStore((s) => s.boats);
  return (
    <details className="demo">
      <summary>
        <QrCode aria-hidden="true" width={16} height={16} /> Demo: codes on the boats
      </summary>
      <ul>
        {boats
          .filter((b) => ['AVAILABLE', 'RESERVED'].includes(b.state))
          .map((b) => (
            <li key={b.id}>
              <span translate="no">{b.code}</span>
              <code>{b.unlockCode}</code>
              <span className="muted">{b.state === 'RESERVED' ? 'reserved' : `${b.battery} %`}</span>
            </li>
          ))}
      </ul>
    </details>
  );
}
