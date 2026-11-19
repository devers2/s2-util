// 공개 데모 페이지: 서버(PublicDemoRules)가 만든 rules.json 을 폼에 붙이고, 언어 전환·행 추가/삭제·플레이그라운드를 처리
import { S2Validator } from './s2.validator.js';

window.S2Validator = S2Validator;
S2Validator.setRenderer(S2Validator.classRenderer());

const rules = await (await fetch('./rules.json')).json();
let lang = new URLSearchParams(location.search).get('lang') === 'en' ? 'en' : 'ko';

const applyLanguage = () => {
  document.documentElement.lang = lang;
  document.querySelectorAll('[data-ko]').forEach((el) => {
    el.innerHTML = el.dataset[lang];
  });
  document.querySelectorAll('[data-ko-href]').forEach((el) => {
    if (!el.dataset.enHref) el.dataset.enHref = el.getAttribute('href');
    el.setAttribute('href', lang === 'ko' ? el.dataset.koHref : el.dataset.enHref);
  });
  document.getElementById('lang').textContent = lang === 'ko' ? 'English' : '한국어';
  // 규칙(메시지)도 언어별로 서버가 만든 것을 붙임
  document.querySelectorAll('form[data-rules-key]').forEach((form) => {
    const key = `${form.dataset.rulesKey}.${lang}`;
    form.setAttribute('data-s2-rules', JSON.stringify(rules[key]));
    S2Validator.classRenderer().clear(form);
  });
  document.querySelectorAll('[data-rules-view]').forEach((el) => {
    el.textContent = JSON.stringify(rules[`${el.dataset.rulesView}.${lang}`], null, 2);
  });
};

document.getElementById('lang').addEventListener('click', () => {
  lang = lang === 'ko' ? 'en' : 'ko';
  applyLanguage();
});

// s2.validator.js 의 submit 리스너 다음에 등록되므로, 막히지 않은 제출만 여기로 옴 (데모라 전송하지 않음)
document.addEventListener('submit', (e) => {
  const form = e.target;
  const result = form.querySelector('[data-result]');
  if (e.defaultPrevented) {
    if (result) result.textContent = '';
    return;
  }
  e.preventDefault();
  form.dataset.submitted = String(Number(form.dataset.submitted || 0) + 1);
  if (result) result.textContent = lang === 'ko' ? '✅ 통과 — 서버로 전송됩니다 (데모라 전송하지 않음)' : '✅ Valid — would be sent to the server (not in this demo)';
});

// 주문 행: 추가·삭제 후 이름의 인덱스를 다시 매김
const rowsBox = document.querySelector('[data-rows]');
const bindDelete = (button) => {
  button.addEventListener('click', () => {
    const form = button.form;
    button.closest('[data-row]').remove();
    S2Validator.reindex(form, 'items');
  });
};
document.querySelectorAll('[data-delete-row]').forEach(bindDelete);
document.querySelector('[data-add-row]').addEventListener('click', () => {
  const template = rowsBox.querySelector('[data-row]');
  const index = rowsBox.querySelectorAll('[data-row]').length;
  const row = template
    ? template.cloneNode(true)
    : Object.assign(document.createElement('div'), { className: 'row' });
  row.querySelectorAll('[name], [data-s2-error-for]').forEach((el) => {
    ['name', 'data-s2-error-for'].forEach((attr) => {
      const value = el.getAttribute(attr);
      if (value) el.setAttribute(attr, value.replace(/items\[\d+\]/, `items[${index}]`));
    });
    if (el.matches('input')) el.value = '';
    if (el.matches('small')) el.textContent = '';
  });
  row.querySelectorAll('.is-invalid').forEach((el) => el.classList.remove('is-invalid'));
  bindDelete(row.querySelector('[data-delete-row]'));
  rowsBox.appendChild(row);
});

document.querySelector('[data-server-only]').textContent = rules['profile.serverOnly'].join(', ');

// 플레이그라운드
const playRules = document.getElementById('playRules');
const playData = document.getElementById('playData');
const playOut = document.getElementById('playOut');
playRules.value = JSON.stringify(
  {
    schemaVersion: 1,
    fields: [
      { name: 'age', label: '나이', rules: [{ type: 'MIN_VALUE', value: 19, message: '나이는 19 이상이어야 합니다.' }] },
      {
        name: 'vipCode',
        label: 'VIP 코드',
        rules: [{ type: 'REQUIRED', message: 'VIP 코드는 필수입니다.' }],
        conditions: [[{ field: 'type', value: 'VIP' }]]
      }
    ]
  },
  null,
  2
);
playData.value = JSON.stringify({ age: '17', type: 'VIP', vipCode: '' }, null, 2);
document.getElementById('playRun').addEventListener('click', () => {
  try {
    const errors = S2Validator.check(JSON.parse(playRules.value), JSON.parse(playData.value));
    playOut.textContent = Object.keys(errors).length === 0 ? '{}  // valid' : JSON.stringify(errors, null, 2);
  } catch (err) {
    playOut.textContent = `JSON error: ${err.message}`;
  }
});

applyLanguage();
window.__siteReady = true;
