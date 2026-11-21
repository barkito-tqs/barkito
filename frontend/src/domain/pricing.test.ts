import { holdFee, startedMinutes, tripCharges } from './pricing';

describe('trip price (BR11)', () => {
  it('counts every started minute: 12 min 20 s is 13 min and costs 7.50 € on a Standard boat', () => {
    const charges = tripCharges('STANDARD', 12 * 60 + 20);
    expect(charges.normalMinutes).toBe(13);
    expect(charges.total).toBe(750);
  });

  it('charges 2 € + 1.00 €/min on an XL boat', () => {
    expect(tripCharges('XL', 10 * 60).total).toBe(1200);
  });

  it('treats an exact minute as one minute and one more second as two', () => {
    expect(startedMinutes(60)).toBe(1);
    expect(startedMinutes(61)).toBe(2);
  });
});

describe('overtime (BR12)', () => {
  it('has no overtime at 90 minutes', () => {
    expect(tripCharges('STANDARD', 90 * 60).overtimeMinutes).toBe(0);
  });

  it('charges minute 91 as overtime: Standard 0.75 €/min', () => {
    const charges = tripCharges('STANDARD', 90 * 60 + 1);
    expect(charges.overtimeMinutes).toBe(1);
    expect(charges.overtimeCost).toBe(75);
  });

  it('charges XL overtime at 1.50 €/min', () => {
    expect(tripCharges('XL', 92 * 60).overtimeCost).toBe(300);
  });
});

describe('hold fee (BR8)', () => {
  it('charges the started minutes of hold: 6 min on Standard is 1.50 €', () => {
    expect(holdFee('STANDARD', 6 * 60)).toBe(150);
  });

  it('is capped at 15 minutes: Standard 3.75 €, XL 7.50 €', () => {
    expect(holdFee('STANDARD', 20 * 60)).toBe(375);
    expect(holdFee('XL', 15 * 60)).toBe(750);
  });

  it('is added to the trip when the reserved boat is unlocked', () => {
    expect(tripCharges('STANDARD', 60, 6 * 60).total).toBe(100 + 50 + 150);
  });
});
