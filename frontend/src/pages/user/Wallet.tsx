import { CreditCard, ShieldCheck } from 'lucide-react';
import { useUser } from '../../auth/session';
import { Alert, PageHead } from '../../components/ui';
import { PRICES, formatCents } from '../../domain/pricing';

/**
 * US-16: saved payment method. The card form is Stripe.js (Elements), so card details go straight
 * to Stripe and never reach the backend; it is wired when the Payments epic starts (I3).
 */
export function Wallet() {
  const user = useUser();

  return (
    <>
      <PageHead eyebrow="Wallet" title="Payment Method and Balance" />
      <div className="grid-2">
        <section className="card stack" aria-labelledby="card-title">
          <div className="card-head">
            <h2 id="card-title">Saved Card</h2>
            <ShieldCheck aria-hidden="true" className="muted" />
          </div>
          {user.hasPaymentMethod ? (
            <div className="saved-card">
              <CreditCard aria-hidden="true" />
              <div>
                <strong>Visa ending 4242</strong>
                <p className="small muted">Test card · expires 12/29</p>
              </div>
            </div>
          ) : (
            <Alert tone="warn">Add a card before you reserve or unlock a boat.</Alert>
          )}
          <p className="small muted">
            Card details are typed into Stripe&rsquo;s secure form and go straight to Stripe. Barkito keeps only a reference to the card.
          </p>
          <button type="button" className="btn btn-secondary" disabled title="Available when payments go live in iteration 3">
            Replace Card
          </button>
        </section>

        <section className="card stack" aria-labelledby="balance-title">
          <h2 id="balance-title">Balance</h2>
          {user.debtCents > 0 ? (
            <Alert tone="danger">
              You owe {formatCents(user.debtCents)}. Settle it to reserve or unlock boats again.
            </Alert>
          ) : (
            <Alert tone="ok">No pending debt. You can reserve and unlock boats.</Alert>
          )}
          <dl className="facts">
            <div>
              <dt>Deposit on a Standard trip</dt>
              <dd>{formatCents(PRICES.deposit.STANDARD)}</dd>
            </div>
            <div>
              <dt>Deposit on an XL trip</dt>
              <dd>{formatCents(PRICES.deposit.XL)}</dd>
            </div>
          </dl>
          <p className="small muted">
            The deposit is held when you unlock a boat and released when you end the trip, minus what you owe. Anything above the deposit
            becomes debt.
          </p>
        </section>
      </div>
    </>
  );
}
