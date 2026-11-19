// Public demo site (GitHub Pages): forms, language switch, rows and playground keep working. | 공개 데모 사이트(GitHub Pages)의 폼·언어 전환·행·플레이그라운드 동작 확인
const { test, expect } = require('@playwright/test');

const ready = async (page, query = '') => {
  await page.goto(`/site/${query}`);
  await page.waitForFunction(() => window.__siteReady === true);
};
const errorOf = (page, form, name) => page.locator(`#${form} [data-s2-error-for="${name}"]`);
const submit = (page, form) => page.locator(`#${form} .actions button`).click();

test.describe('public demo site', () => {
  test('sign-up: empty submit shows server-made Korean messages', async ({ page }) => {
    await ready(page);
    await submit(page, 'join');
    await expect(errorOf(page, 'join', 'name')).toHaveText('이름은 필수 입력 항목입니다.');
    await expect(errorOf(page, 'join', 'agree')).not.toHaveText('');
    await expect(page.locator('#join')).not.toHaveAttribute('data-submitted', /./);
  });

  test('sign-up: business number is required and checked only for business members', async ({ page }) => {
    await ready(page);
    const form = page.locator('#join');
    await form.locator('[name="memberType"]').selectOption('PERSONAL');
    await submit(page, 'join');
    await expect(errorOf(page, 'join', 'bizNo')).toHaveText('');
    await form.locator('[name="memberType"]').selectOption('BUSINESS');
    await form.locator('[name="bizNo"]').fill('123-45-67890');
    await submit(page, 'join');
    await expect(errorOf(page, 'join', 'bizNo')).not.toHaveText('');
  });

  test('sign-up: a valid form passes', async ({ page }) => {
    await ready(page);
    const form = page.locator('#join');
    await form.locator('[name="name"]').fill('홍길동');
    await form.locator('[name="email"]').fill('hong@example.com');
    await form.locator('[name="mobile"]').fill('010-1234-5678');
    await form.locator('[name="memberType"]').selectOption('PERSONAL');
    await form.locator('[name="password"]').fill('Passw0rd!');
    await form.locator('[name="passwordConfirm"]').fill('Passw0rd!');
    await form.locator('[name="interests"][value="java"]').check();
    await form.locator('[name="agree"]').check();
    await submit(page, 'join');
    await expect(form).toHaveAttribute('data-submitted', '1');
  });

  test('language switch uses the English rules made by the server', async ({ page }) => {
    await ready(page);
    await page.locator('#lang').click();
    await submit(page, 'join');
    await expect(errorOf(page, 'join', 'name')).toHaveText('Name is required.');
  });

  test('order: row rules and date comparison, rows reindexed after delete', async ({ page }) => {
    await ready(page);
    await submit(page, 'order');
    await expect(errorOf(page, 'order', 'items[1].name')).not.toHaveText('');
    await expect(errorOf(page, 'order', 'items[1].qty')).not.toHaveText('');
    await expect(errorOf(page, 'order', 'endDate')).not.toHaveText('');
    await page.locator('#order [data-row]').nth(0).locator('[data-delete-row]').click();
    await expect(page.locator('#order [name="items[0].qty"]')).toHaveValue('0');
    await page.locator('#order [data-add-row]').click();
    await expect(page.locator('#order [name="items[1].name"]')).toHaveValue('');
  });

  test('bean validation: converted rules run in the browser, @Past stays server-only', async ({ page }) => {
    await ready(page);
    const form = page.locator('#profile');
    await form.locator('[name="tags"][value="a"]').check();
    await form.locator('[name="tags"][value="b"]').check();
    await form.locator('[name="tags"][value="c"]').check();
    await form.locator('[name="zip"]').fill('123');
    await submit(page, 'profile');
    await expect(errorOf(page, 'profile', 'tags')).not.toHaveText('');
    await expect(errorOf(page, 'profile', 'zip')).not.toHaveText('');
    await expect(errorOf(page, 'profile', 'nickname')).toHaveText('닉네임은 필수 입력 항목입니다.');
    await expect(form.locator('[data-server-only]')).toHaveText('birthDate @Past');
  });

  test('playground: check() reports errors for the edited data', async ({ page }) => {
    await ready(page);
    await page.locator('#playRun').click();
    await expect(page.locator('#playOut')).toContainText('vipCode');
    await page.locator('#playData').fill('{"age": "20", "type": "NORMAL"}');
    await page.locator('#playRun').click();
    await expect(page.locator('#playOut')).toHaveText('{}  // valid');
  });
});
