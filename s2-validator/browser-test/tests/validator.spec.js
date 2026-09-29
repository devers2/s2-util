// Real-browser tests for s2.validator.js: native constraint UI, hidden-field anchors, checkboxes, dynamic rows, and the
// custom renderer. Rules are the server-generated demo/rules.json. | s2.validator.js 실제 브라우저 시험: 기본 제약 UI, 히든 필드 앵커,
// 체크박스, 동적 행, 사용자 렌더러. 규칙은 서버가 만든 demo/rules.json
const { test, expect } = require('@playwright/test');

const ready = async (page, url) => {
  await page.goto(url);
  await page.waitForFunction(() => window.__demoReady === true);
};
const submit = (page, formId) => page.locator(`#${formId} button:not([type="button"])`).click();
const submitted = (page, formId) => page.locator(`#${formId}`).getAttribute('data-submitted');
const message = (page, selector) => page.locator(selector).evaluate((el) => el.validationMessage);

test.describe('basic form', () => {
  test('empty submit is blocked, shows the server message, and focuses the first invalid field', async ({ page }) => {
    await ready(page, '/');
    await submit(page, 'basic');

    expect(await submitted(page, 'basic')).toBeNull();
    expect(await message(page, '#basic [name="name"]')).toBe('이름은 필수 입력 항목입니다.');
    await expect(page.locator('#basic [name="name"]')).toBeFocused();
    expect(await page.locator('#basic').evaluate((f) => f.noValidate)).toBe(true);
  });

  test('number with comma is invalid, surrounding spaces are ignored, valid input submits', async ({ page }) => {
    await ready(page, '/');
    const form = page.locator('#basic');
    await form.locator('[name="name"]').fill('홍길동');
    await form.locator('[name="email"]').fill('a@b.technology');
    await form.locator('[name="qty"]').fill('1,000');
    await form.locator('[name="code"]').fill(' abc ');
    await submit(page, 'basic');

    expect(await submitted(page, 'basic')).toBeNull();
    expect(await message(page, '#basic [name="qty"]')).not.toBe('');
    expect(await message(page, '#basic [name="code"]')).toBe('');

    await form.locator('[name="qty"]').fill(' 50 ');
    await submit(page, 'basic');
    expect(await submitted(page, 'basic')).toBe('1');
  });

  test('editing a field clears its error', async ({ page }) => {
    await ready(page, '/');
    await submit(page, 'basic');
    await page.locator('#basic [name="name"]').fill('x');
    expect(await message(page, '#basic [name="name"]')).toBe('');
  });
});

test.describe('hidden fields', () => {
  test('hidden input, closed tab, and hidden radio group get rendered, focusable anchors', async ({ page }) => {
    await ready(page, '/');
    await submit(page, 'hidden');
    expect(await submitted(page, 'hidden')).toBeNull();

    const anchors = page.locator('#hidden .__s2_dummy_anchor__');
    await expect(anchors).toHaveCount(3); // token + memo + one for the radio group
    const info = await anchors.evaluateAll((els) =>
      els.map((el) => ({
        rendered: el.getClientRects().length > 0,
        message: el.validationMessage,
        label: el.getAttribute('aria-label'),
        ariaHidden: el.getAttribute('aria-hidden'),
        name: el.getAttribute('name'),
        prev: el.previousElementSibling?.id || el.previousElementSibling?.getAttribute('name') || el.previousElementSibling?.className
      }))
    );
    for (const a of info) {
      expect(a.rendered).toBe(true);
      expect(a.message).not.toBe('');
      expect(a.label).toBe(a.message);
      expect(a.ariaHidden).toBeNull();
      expect(a.name).toBeNull();
    }
    // Anchors sit outside the closed tab, in field order, so the first bubble is the first field | 앵커는 닫힌 탭 밖에 필드 순서대로 놓여 첫 말풍선이 첫 필드가 됨
    expect(info.map((a) => [a.message, a.prev])).toEqual([
      ['토큰은 필수 입력 항목입니다.', 'token'],
      ['메모는 필수 입력 항목입니다.', 'closedTab'],
      ['등급은 필수 입력 항목입니다.', '__s2_dummy_anchor__']
    ]);
  });

  test('the first bubble follows field order when the first field is valid', async ({ page }) => {
    await ready(page, '/');
    await page.locator('#hidden [name="token"]').evaluate((el) => (el.value = 't'));
    await submit(page, 'hidden');
    const first = await page.locator('#hidden').evaluate((f) => f.querySelector(':invalid').validationMessage);
    expect(first).toBe('메모는 필수 입력 항목입니다.');
  });

  test('anchors are removed on the next validation', async ({ page }) => {
    await ready(page, '/');
    await submit(page, 'hidden');
    await page.locator('#hidden [name="token"]').evaluate((el) => (el.value = 't'));
    await page.locator('#hidden').evaluate((form) => {
      form.querySelector('#closedTab').style.display = 'block';
    });
    await page.locator('#hidden [name="memo"]').fill('m');
    await page.locator('#hidden [name="grade"][value="A"]').check();
    await submit(page, 'hidden');

    await expect(page.locator('#hidden .__s2_dummy_anchor__')).toHaveCount(0);
    expect(await submitted(page, 'hidden')).toBe('1');
  });
});

test.describe('checkboxes', () => {
  test('unchecked consent and empty group are blocked; checking them submits', async ({ page }) => {
    await ready(page, '/');
    await submit(page, 'checkbox');
    expect(await submitted(page, 'checkbox')).toBeNull();
    expect(await message(page, '#checkbox [name="agree"]')).toBe('이용약관을 선택(동의)해야 합니다.');
    expect(await message(page, '#checkbox [name="hobbies"][value="a"]')).not.toBe('');

    await page.locator('#checkbox [name="agree"]').check();
    await page.locator('#checkbox [name="hobbies"][value="b"]').check();
    await submit(page, 'checkbox');
    expect(await submitted(page, 'checkbox')).toBe('1');
  });
});

test.describe('dynamic rows', () => {
  test('deleting a row renumbers indices and errors use the new index', async ({ page }) => {
    await ready(page, '/');
    const rows = page.locator('#rows [data-row]');
    await rows.nth(1).locator('[data-delete-row]').click();

    const names = await page.locator('#rows [name]').evaluateAll((els) => els.map((e) => e.name));
    expect(names).toEqual(['items[0].name', 'items[0].qty', 'items[1].name', 'items[1].qty']);

    await page.locator('#rows [name="items[1].name"]').fill('');
    await submit(page, 'rows');
    expect(await submitted(page, 'rows')).toBeNull();
    expect(await message(page, '#rows [name="items[1].name"]')).toBe('품목명은 필수 입력 항목입니다.');

    const errors = await page.evaluate(() => Object.keys(window.S2Validator.validate('#rows')));
    expect(errors).toEqual(['items[1].name']);
  });
});

test.describe('custom renderer', () => {
  test('classRenderer marks fields, writes messages (hidden field too), and does not use native UI', async ({ page }) => {
    await ready(page, '/renderer.html');
    await submit(page, 'renderer');
    expect(await submitted(page, 'renderer')).toBeNull();

    const name = page.locator('#renderer [name="name"]');
    await expect(name).toHaveClass(/is-invalid/);
    await expect(name).toBeFocused();
    await expect(page.locator('[data-s2-error-for="name"]')).toHaveText('이름은 필수 입력 항목입니다.');
    await expect(page.locator('[data-s2-error-for="token"]')).toHaveText('토큰은 필수 입력 항목입니다.');
    await expect(page.locator('[data-s2-error-for="token"]')).toBeVisible();
    expect(await message(page, '#renderer [name="name"]')).toBe('');
    await expect(page.locator('#renderer .__s2_dummy_anchor__')).toHaveCount(0);
  });

  test('typing clears the field; fixing all errors submits', async ({ page }) => {
    await ready(page, '/renderer.html');
    await submit(page, 'renderer');
    const name = page.locator('#renderer [name="name"]');
    await name.fill('홍길동');
    await expect(name).not.toHaveClass(/is-invalid/);
    await expect(page.locator('[data-s2-error-for="name"]')).toHaveText('');

    await page.locator('#renderer [name="email"]').fill('a@b.com');
    await page.locator('#renderer [name="token"]').evaluate((el) => (el.value = 't'));
    await submit(page, 'renderer');
    expect(await submitted(page, 'renderer')).toBe('1');
  });
});
