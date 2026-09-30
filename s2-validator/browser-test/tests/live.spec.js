// Real-browser tests for live validation (data-s2-live) and the form-free check(). | 실시간 검증(data-s2-live)과 폼 없는 check() 실제 브라우저 시험
const { test, expect } = require('@playwright/test');

const ready = async (page, url) => {
  await page.goto(url);
  await page.waitForFunction(() => window.__demoReady === true);
};
const message = (page, selector) => page.locator(selector).evaluate((el) => el.validationMessage);

test.describe('live validation, native UI (blur)', () => {
  test('leaving an invalid field marks it without moving focus or showing a bubble', async ({ page }) => {
    await ready(page, '/');
    const email = page.locator('#liveNative [name="email"]');
    await email.fill('bad');
    await email.press('Tab');

    await expect(page.locator('#liveNative [name="qty"]')).toBeFocused();
    expect(await message(page, '#liveNative [name="email"]')).not.toBe('');
    expect(await page.locator('#liveNative [name="email"]').evaluate((el) => el.matches(':invalid'))).toBe(true);
  });

  test('a field with an error is re-checked while typing and clears once fixed', async ({ page }) => {
    await ready(page, '/');
    const email = page.locator('#liveNative [name="email"]');
    await email.fill('bad');
    await email.press('Tab');
    expect(await message(page, '#liveNative [name="email"]')).not.toBe('');

    await email.fill('a@b.com');
    expect(await message(page, '#liveNative [name="email"]')).toBe('');
  });

  test('forms without data-s2-live are not validated before submit', async ({ page }) => {
    await ready(page, '/');
    const email = page.locator('#basic [name="email"]');
    await email.fill('bad');
    await email.press('Tab');
    expect(await message(page, '#basic [name="email"]')).toBe('');
  });
});

test.describe('live validation with a renderer (input)', () => {
  test('errors appear while typing and disappear when fixed, focus stays', async ({ page }) => {
    await ready(page, '/live.html');
    const email = page.locator('#live [name="email"]');
    await email.pressSequentially('a');
    await expect(email).toHaveClass(/is-invalid/);
    await expect(page.locator('[data-s2-error-for="email"]')).not.toHaveText('');
    await expect(email).toBeFocused();

    await email.pressSequentially('@b.com');
    await expect(email).not.toHaveClass(/is-invalid/);
    await expect(page.locator('[data-s2-error-for="email"]')).toHaveText('');
  });

  test('leaving an empty required field shows its message; other fields are untouched', async ({ page }) => {
    await ready(page, '/live.html');
    await page.locator('#live [name="name"]').focus();
    await page.locator('#live [name="name"]').press('Tab');
    await expect(page.locator('[data-s2-error-for="name"]')).toHaveText('이름은 필수 입력 항목입니다.');
    await expect(page.locator('[data-s2-error-for="token"]')).toHaveText('');
    await expect(page.locator('#live [name="email"]')).toBeFocused();
  });

  test('submit still validates every field and focuses the first error', async ({ page }) => {
    await ready(page, '/live.html');
    await page.locator('#live button').click();
    await expect(page.locator('[data-s2-error-for="token"]')).toHaveText('토큰은 필수 입력 항목입니다.');
    await expect(page.locator('#live [name="name"]')).toBeFocused();
  });
});

test.describe('form-free check()', () => {
  test('validates plain nested data in the browser', async ({ page }) => {
    await ready(page, '/');
    const errors = await page.evaluate(() => {
      const rules = document.querySelector('#rows').getAttribute('data-s2-rules');
      return window.S2Validator.check(rules, { items: [{ name: 'A', qty: 1 }, { name: '', qty: 0 }] });
    });
    expect(Object.keys(errors).sort()).toEqual(['items[1].name', 'items[1].qty']);
  });
});
