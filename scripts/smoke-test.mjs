import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';
import { readFile } from 'node:fs/promises';

const base = process.env.API_URL || 'http://localhost:8080/api';
const credentials = process.env.TEST_ACCOUNT_FILE
  ? JSON.parse(await readFile(process.env.TEST_ACCOUNT_FILE, 'utf8'))
  : { name: 'Smoke Test', email: `smoke-${randomUUID()}@example.com`, password: `${randomUUID()}Aa1!` };
let token;
async function request(path, method = 'GET', body, expected = 200, authenticated = true) {
  const response = await fetch(base + path, {
    method,
    headers: { 'Content-Type': 'application/json', Origin: process.env.FRONTEND_ORIGIN || 'http://localhost:5173',
      ...(authenticated && token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body ? JSON.stringify(body) : undefined,
    signal: AbortSignal.timeout(60000)
  });
  assert.equal(response.status, expected, `${method} ${path}: expected ${expected}, received ${response.status}`);
  const text = await response.text();
  console.log(`PASS ${method} ${path} (${response.status})`);
  return text ? JSON.parse(text) : null;
}
// External profile creation is opt-in; local smoke tests need no third-party service.
if (process.env.TEST_STORE_NAME) credentials.storeName ||= process.env.TEST_STORE_NAME;
if (!process.env.TEST_ACCOUNT_FILE) {
  token = (await request('/auth/register', 'POST', credentials, 200, false)).token;
}
const loginCredentials = { email: credentials.email, password: credentials.password };
token = (await request('/auth/login', 'POST', loginCredentials, 200, false)).token;
assert.ok(token);
const applicationBody = { companyName: 'Smoke Test Company', jobTitle: 'Test Engineer', location: 'Remote',
  jobType: 'Full Time', status: 'APPLIED', appliedDate: '2026-10-18', notes: 'Automated deployment check' };
let application;
let interview;
try {
  application = await request('/applications', 'POST', applicationBody);
  assert.ok((await request('/applications')).some(item => item.id === application.id));
  const stats = await request('/dashboard/stats');
  assert.ok(stats.total >= 1 && stats.applied >= 1);
  await request(`/applications/${application.id}`, 'PUT', { ...applicationBody, status: 'INTERVIEW' });
  assert.ok((await request('/applications/status/INTERVIEW')).some(item => item.id === application.id));
  interview = await request('/interviews', 'POST', { applicationId: application.id, round: 'Technical',
    interviewDate: '2026-10-20T10:00:00', mode: 'Online', notes: 'Smoke test' });
  assert.ok((await request('/interviews')).some(item => item.id === interview.id));
  await request(`/interviews/${interview.id}`, 'PUT', { ...interview, round: 'Final' });
  assert.ok((await request('/dashboard/stats')).interview >= 1);
} finally {
  if (interview) await request(`/interviews/${interview.id}`, 'DELETE', undefined, 204);
  if (application) await request(`/applications/${application.id}`, 'DELETE', undefined, 204);
}
assert.ok(!(await request('/applications')).some(item => item.id === application?.id));
if (process.env.CHECK_AUTH_ERRORS === 'true') {
  await request('/auth/login', 'POST', { ...loginCredentials, password: 'wrong-password' }, 401, false);
  await request('/applications', 'GET', undefined, 401, false);
  token = 'invalid-token';
  await request('/applications', 'GET', undefined, 401);
  // Public login must still work when a browser has an old token.
  await request('/auth/login', 'POST', loginCredentials);
}
console.log('All smoke checks passed; application and interview test records removed.');
