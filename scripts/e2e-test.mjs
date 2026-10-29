import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { once } from 'node:events';
import { randomUUID } from 'node:crypto';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { resolve } from 'node:path';
import { MongoMemoryServer } from 'mongodb-memory-server';
import { chromium } from 'playwright';

const root = fileURLToPath(new URL('../', import.meta.url));
const apiPort = process.env.QA_API_PORT || '18080';
const uiPort = process.env.QA_UI_PORT || '15173';
const api = `http://127.0.0.1:${apiPort}/api`;
const ui = `http://localhost:${uiPort}`;
const children = [];
let logs = '';
let mongo;
let browser;
function launch(command, args, env = {}) {
  const child = spawn(command, args, { cwd: root, env: { ...process.env, ...env }, windowsHide: true,
    stdio: ['ignore', 'pipe', 'pipe'] });
  child.on('error', error => { logs += `${error}\n`; });
  child.stdout.on('data', data => { logs = (logs + data).slice(-20000); });
  child.stderr.on('data', data => { logs = (logs + data).slice(-20000); });
  children.push(child);
  return child;
}
async function ready(url, child) {
  for (let attempt = 0; attempt < 180; attempt++) {
    if (child.exitCode !== null) throw new Error(`Server exited: ${logs}`);
    try { await fetch(url, { signal: AbortSignal.timeout(1000) }); return; } catch {}
    await new Promise(done => setTimeout(done, 500));
  }
  throw new Error(`Server did not start: ${logs}`);
}
async function request(path, method = 'GET', body, token, status = 200) {
  const response = await fetch(api + path, { method,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body) });
  assert.equal(response.status, status, `${method} ${path}: ${await response.clone().text()}`);
  const text = await response.text();
  if (!text) return null;
  try { return JSON.parse(text); } catch { return text; }
}
const credentials = { name: 'Browser QA', email: `qa-${randomUUID()}@example.com`, password: 'Browser-qa-123!' };
try {
  mongo = await MongoMemoryServer.create({ instance: { dbName: 'careertrack_qa' } });
  const backend = launch(process.env.JAVA_HOME ? resolve(process.env.JAVA_HOME, 'bin', 'java') : 'java',
    ['-jar', resolve(root, 'careertrack-backend/target/careertrack-backend-0.0.1-SNAPSHOT.jar')],
    { PORT: apiPort, SPRING_DATA_MONGODB_URI: mongo.getUri('careertrack_qa'), APP_CORS_ALLOWED_ORIGIN: ui });
  // Vite's root is supplied explicitly because this script runs from the repository root.
  const vite = launch(process.execPath,
    [resolve(root, 'careertrack-frontend/node_modules/vite/bin/vite.js'), resolve(root, 'careertrack-frontend'),
      '--host', 'localhost', '--port', uiPort, '--strictPort'], { VITE_API_BASE_URL: api });
  await Promise.all([ready(`${api}/applications`, backend), ready(ui, vite)]);
  console.log('PASS isolated backend and frontend startup');

  const smoke = launch(process.execPath, [resolve(root, 'scripts/smoke-test.mjs')],
    { API_URL: api, FRONTEND_ORIGIN: ui, CHECK_AUTH_ERRORS: 'true', TEST_ACCOUNT_FILE: '', TEST_STORE_NAME: '' });
  smoke.stdout.on('data', data => process.stdout.write(data));
  const [smokeCode] = await once(smoke, 'exit');
  assert.equal(smokeCode, 0, logs);

  const owner = await request('/auth/register', 'POST', credentials);
  const other = await request('/auth/register', 'POST', { ...credentials, email: `other-${credentials.email}` });
  const body = { companyName: 'API QA', jobTitle: 'Engineer', status: 'APPLIED' };
  const application = await request('/applications', 'POST', body, owner.token);
  const foreign = await request('/applications', 'POST', body, other.token);
  await request('/auth/register', 'POST', { ...credentials, password: 'attacker-password' }, null, 400);
  await request('/auth/login', 'POST', credentials);
  await request('/applications', 'POST', {}, owner.token, 400);
  await request('/interviews', 'POST', {}, owner.token, 400);
  await request(`/applications/${application.id}`, 'PUT', body, other.token, 404);
  await request(`/applications/${application.id}`, 'DELETE', undefined, other.token, 404);
  const interviewBody = { applicationId: application.id, round: 'API round', interviewDate: '2026-10-29T10:00:00', mode: 'Online' };
  await request('/interviews', 'POST', interviewBody, other.token, 404);
  await request('/interviews', 'POST', { ...interviewBody, applicationId: 'missing' }, owner.token, 404);
  const interview = await request('/interviews', 'POST', interviewBody, owner.token);
  await request(`/interviews/${interview.id}`, 'PUT', { ...interviewBody, applicationId: foreign.id }, owner.token, 404);
  await request(`/interviews/${interview.id}`, 'DELETE', undefined, other.token, 404);
  await request(`/users/${application.userId}`, 'GET', undefined, other.token, 404);
  await request(`/applications/${application.id}`, 'DELETE', undefined, owner.token, 204);
  assert.equal((await request('/interviews', 'GET', undefined, owner.token)).length, 0);
  const cors = await fetch(`${api}/applications`, { method: 'OPTIONS',
    headers: { Origin: ui, 'Access-Control-Request-Method': 'GET' } });
  assert.equal(cors.headers.get('access-control-allow-origin'), ui);
  const denied = await fetch(`${api}/applications`, { method: 'OPTIONS',
    headers: { Origin: 'https://unconfigured.example', 'Access-Control-Request-Method': 'GET' } });
  assert.equal(denied.status, 403);
  console.log('PASS live API validation, duplicate registration, ownership, cascade deletion, and CORS');

  browser = await chromium.launch({ headless: true,
    ...(process.env.QA_BROWSER_CHANNEL ? { channel: process.env.QA_BROWSER_CHANNEL } : {}) });
  const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
  const errors = [];
  page.on('pageerror', error => errors.push(error.message));
  await page.goto(ui);
  await page.getByRole('button', { name: 'Register', exact: true }).click();
  await page.getByLabel('Name', { exact: true }).fill('UI New User');
  const uiEmail = `ui-${randomUUID()}@example.com`;
  await page.getByLabel('Email', { exact: true }).fill(uiEmail);
  await page.getByLabel('Password', { exact: true }).fill(credentials.password);
  await page.getByRole('button', { name: 'Create Account', exact: true }).click();
  await page.getByText('Registration successful!', { exact: false }).waitFor();
  await page.getByLabel('Password', { exact: true }).fill('wrong-password');
  await page.locator('button[type=submit]').click();
  await page.getByText('Email or password is incorrect.', { exact: true }).waitFor();
  await page.getByLabel('Password', { exact: true }).fill(credentials.password);
  await page.locator('button[type=submit]').click();
  await page.getByRole('button', { name: 'Logout', exact: true }).waitFor();
  await page.getByText('No applications found.', { exact: true }).waitFor();
  const applicationForm = page.locator('form').filter({ has: page.getByPlaceholder('Company name') });
  await page.getByPlaceholder('Company name').fill('Browser Company');
  await page.getByPlaceholder('Job title').fill('QA Engineer');
  let submissions = 0;
  await page.route('**/api/applications', async route => {
    if (route.request().method() === 'POST') submissions++;
    await route.continue();
  });
  await applicationForm.evaluate(form => {
    form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
  });
  await page.locator('.application-card').filter({ hasText: 'Browser Company' }).waitFor();
  assert.equal(await page.locator('.application-card').count(), 1);
  assert.equal(submissions, 1, 'Duplicate submission must send only one POST');
  await page.unroute('**/api/applications');
  await page.getByRole('button', { name: 'Edit', exact: true }).click();
  await applicationForm.locator('select').nth(1).selectOption('INTERVIEW');
  await page.getByRole('button', { name: 'Update Application', exact: true }).click();
  await page.locator('.status.interview').waitFor();
  await page.locator('.filter-select').selectOption('OFFER');
  await page.getByText('No applications found.', { exact: true }).waitFor();
  await page.locator('.filter-select').selectOption('ALL');
  const interviewForm = page.locator('form').filter({ has: page.getByPlaceholder('Round', { exact: true }) });
  await interviewForm.locator('select').first().selectOption({ index: 1 });
  await page.getByPlaceholder('Round', { exact: true }).fill('UI Technical');
  await interviewForm.locator('input[type=datetime-local]').fill('2026-10-29T14:30');
  await page.getByRole('button', { name: 'Add Interview', exact: true }).click();
  await page.getByText('UI Technical', { exact: true }).waitFor();
  await page.getByRole('button', { name: 'Edit', exact: true }).click();
  await page.getByRole('button', { name: 'Cancel editing', exact: true }).click();
  assert.equal(await page.getByPlaceholder('Company name').inputValue(), '');
  console.log('PASS browser registration, wrong password, login, application create/edit/filter, interview creation, and cancel');

  for (const width of [320, 375, 768, 1440]) {
    await page.setViewportSize({ width, height: 1000 });
    assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), `Overflow at ${width}px`);
  }
  await page.reload();
  await page.getByText('UI Technical', { exact: true }).waitFor();
  await page.getByRole('button', { name: 'Edit', exact: true }).click();
  await page.getByRole('button', { name: 'Delete', exact: true }).click();
  await page.getByText('No applications found.', { exact: true }).waitFor();
  await page.getByText('No interviews scheduled.', { exact: true }).waitFor();
  assert.equal(await page.getByPlaceholder('Company name').inputValue(), '');
  assert.equal(await page.getByRole('button', { name: 'Update Application', exact: true }).count(), 0);
  let releaseOldRequest;
  let requestStarted;
  const oldRequest = new Promise(done => { releaseOldRequest = done; });
  const started = new Promise(done => { requestStarted = done; });
  let intercepted = false;
  await page.route('**/api/applications', async route => {
    if (!intercepted) {
      intercepted = true;
      requestStarted();
      await oldRequest;
      await route.fulfill({ status: 401, contentType: 'application/json', body: '{"message":"Old session"}' });
    } else await route.continue();
  });
  await page.reload();
  await started;
  await page.getByRole('button', { name: 'Logout', exact: true }).click();
  await page.getByLabel('Email', { exact: true }).fill(uiEmail);
  await page.getByLabel('Password', { exact: true }).fill(credentials.password);
  await page.locator('button[type=submit]').click();
  await page.getByText('No applications found.', { exact: true }).waitFor();
  const oldResponse = page.waitForResponse(response => response.url().endsWith('/api/applications') && response.status() === 401);
  releaseOldRequest();
  await (await oldResponse).finished();
  await page.unroute('**/api/applications');
  await page.getByRole('button', { name: 'Logout', exact: true }).waitFor();
  console.log('PASS duplicate-submit prevention and stale requests across logout/login');
  await page.evaluate(() => localStorage.setItem('token', 'invalid-token'));
  await page.reload();
  await page.getByText('Your session expired. Please log in again.', { exact: true }).waitFor();
  assert.equal(await page.evaluate(() => localStorage.getItem('token')), null);
  await page.evaluate(() => {
    localStorage.setItem('user', '42');
    localStorage.setItem('token', 'invalid-token');
  });
  await page.reload();
  await page.getByLabel('Email', { exact: true }).waitFor();
  assert.equal(await page.evaluate(() => localStorage.getItem('token')), null);
  assert.deepEqual(errors, []);
  console.log('PASS responsive widths 320/375/768/1440, reload persistence, delete while editing, and expired session');
  await page.goto(pathToFileURL(resolve(root, 'portfolio/index.html')).href);
  for (const width of [320, 375, 768, 1440]) {
    await page.setViewportSize({ width, height: 1000 });
    assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), `Portfolio overflow at ${width}px`);
  }
  await page.setViewportSize({ width: 375, height: 1000 });
  await page.getByRole('button', { name: 'Toggle menu' }).click();
  assert.equal(await page.getByRole('button', { name: 'Toggle menu' }).getAttribute('aria-expanded'), 'true');
  await page.locator('.nav-links').getByRole('link', { name: 'About', exact: true }).click();
  assert.equal(await page.getByRole('button', { name: 'Toggle menu' }).getAttribute('aria-expanded'), 'false');
  assert.deepEqual(errors, []);
  console.log('PASS portfolio layout and mobile menu');
  console.log('All end-to-end checks passed against an isolated disposable database.');
} catch (error) {
  console.error(logs);
  throw error;
} finally {
  await browser?.close();
  for (const child of children.reverse()) {
    if (child.exitCode === null && child.signalCode === null) {
      const stopped = once(child, 'exit').catch(() => {});
      child.kill();
      await stopped;
    }
  }
  await mongo?.stop();
}
