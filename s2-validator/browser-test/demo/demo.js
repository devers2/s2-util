// Shared setup for the demo pages: attaches the server-generated rules (rules.json) to each form, records submits that
// s2.validator.js did not block, and exposes S2Validator for tests. | 데모 페이지 공통 설정: 서버가 만든 규칙(rules.json)을 각 폼에
// 붙이고, s2.validator.js 가 막지 않은 제출을 기록하며, 시험용으로 S2Validator 를 노출
import { S2Validator } from '/s2-util/js/s2.validator.js';

window.S2Validator = S2Validator;

// Registered after s2.validator.js's own submit listener, so defaultPrevented tells whether validation blocked it | s2.validator.js 의 submit 리스너 다음에 등록되므로 defaultPrevented 로 검증이 막았는지 알 수 있음
document.addEventListener('submit', (e) => {
  const form = e.target;
  if (!e.defaultPrevented) {
    e.preventDefault();
    form.dataset.submitted = String(Number(form.dataset.submitted || 0) + 1);
  }
});

const rules = await (await fetch('/rules.json')).json();
document.querySelectorAll('form[data-rules-key]').forEach((form) => {
  form.setAttribute('data-s2-rules', JSON.stringify(rules[form.dataset.rulesKey]));
});

document.querySelectorAll('[data-delete-row]').forEach((button) => {
  button.addEventListener('click', () => {
    const form = button.form;
    button.closest('[data-row]').remove();
    S2Validator.reindex(form, 'items');
  });
});

window.__demoReady = true;
