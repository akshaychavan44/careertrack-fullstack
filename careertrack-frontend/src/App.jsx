import React, { useEffect, useMemo, useRef, useState } from 'react';
import api, { errorMessage } from './api';

const newApplication = () => ({
  companyName: '',
  jobTitle: '',
  location: '',
  jobType: 'Full Time',
  status: 'APPLIED',
  appliedDate: localDate(),
  notes: ''
});

function localDate() {
  const date = new Date();
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

const emptyInterview = {
  applicationId: '',
  round: '',
  interviewDate: '',
  mode: 'Online',
  notes: ''
};

function App() {
  const [user, setUser] = useState(() => {
    try {
      const saved = localStorage.getItem('user');
      const token = localStorage.getItem('token');
      const parsed = saved && token ? JSON.parse(saved) : null;
      if (parsed && typeof parsed.name === 'string' && typeof parsed.email === 'string') return parsed;
      localStorage.removeItem('user');
      localStorage.removeItem('token');
      return null;
    } catch {
      localStorage.removeItem('user');
      localStorage.removeItem('token');
      return null;
    }
  });
  const [authMode, setAuthMode] = useState('login');
  const [authForm, setAuthForm] = useState({ name: '', email: '', password: '' });
  const [applications, setApplications] = useState([]);
  const [interviews, setInterviews] = useState([]);
  const [stats, setStats] = useState({ total: 0, applied: 0, interview: 0, offer: 0, rejected: 0 });
  const [applicationForm, setApplicationForm] = useState(newApplication);
  const [interviewForm, setInterviewForm] = useState(emptyInterview);
  const [editingId, setEditingId] = useState(null);
  const [filter, setFilter] = useState('ALL');
  const [message, setMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [busy, setBusy] = useState(false);
  const [saving, setSaving] = useState(false);
  const mutationPending = useRef(false);
  const authPending = useRef(false);
  const sessionVersion = useRef(0);
  const loadVersion = useRef(0);

  useEffect(() => {
    if (user) {
      loadData();
    }
  }, [user]);

  const filteredApplications = useMemo(() => {
    if (filter === 'ALL') return applications;
    return applications.filter((app) => app.status === filter);
  }, [applications, filter]);

  async function loadData() {
    const session = sessionVersion.current;
    const version = ++loadVersion.current;
    try {
      const [appsRes, interviewsRes, statsRes] = await Promise.all([
        api.get('/applications'),
        api.get('/interviews'),
        api.get('/dashboard/stats')
      ]);
      if (session !== sessionVersion.current || version !== loadVersion.current) return;
      setApplications(appsRes.data);
      setInterviews(interviewsRes.data);
      setStats(statsRes.data);
      setMessage('');
    } catch (error) {
      if (session !== sessionVersion.current || version !== loadVersion.current) return;
      if ([401, 403].includes(error.response?.status)) logout();
      setMessage(errorMessage(error));
    }
  }

  async function handleAuth(event) {
    event.preventDefault();
    if (authPending.current) return;
    authPending.current = true;
    setMessage('');
    setSuccessMessage('');
    setBusy(true);
    try {
      if (authMode === 'login') {
        const payload = { email: authForm.email.trim(), password: authForm.password };
        const response = await api.post('/auth/login', payload);
        localStorage.setItem('token', response.data.token);
        localStorage.setItem('user', JSON.stringify({ name: response.data.name, email: response.data.email }));
        setUser({ name: response.data.name, email: response.data.email });
      } else {
        const payload = { name: authForm.name.trim(), email: authForm.email.trim(), password: authForm.password };
        await api.post('/auth/register', payload);
        setAuthMode('login');
        setAuthForm((prev) => ({ ...prev, password: '' }));
        setSuccessMessage('Registration successful! Please log in with your credentials.');
      }
    } catch (error) {
      setMessage(errorMessage(error));
    } finally {
      authPending.current = false;
      setBusy(false);
    }
  }

  async function saveApplication(event) {
    event.preventDefault();
    if (mutationPending.current) return;
    mutationPending.current = true;
    setSaving(true);
    try {
      if (editingId) {
        await api.put(`/applications/${editingId}`, applicationForm);
        setEditingId(null);
      } else {
        await api.post('/applications', applicationForm);
      }
      setApplicationForm(newApplication());
      await loadData();
    } catch (error) { handleError(error); }
    finally { mutationPending.current = false; setSaving(false); }
  }

  function editApplication(app) {
    setEditingId(app.id);
    setApplicationForm({
      companyName: app.companyName || '',
      jobTitle: app.jobTitle || '',
      location: app.location || '',
      jobType: app.jobType || 'Full Time',
      status: app.status || 'APPLIED',
      appliedDate: app.appliedDate || localDate(),
      notes: app.notes || ''
    });
  }

  async function deleteApplication(id) {
    if (mutationPending.current) return;
    mutationPending.current = true;
    setSaving(true);
    try {
      await api.delete(`/applications/${id}`);
      if (editingId === id) {
        setEditingId(null);
        setApplicationForm(newApplication());
      }
      setInterviewForm((prev) => prev.applicationId === id ? emptyInterview : prev);
      await loadData();
    } catch (error) { handleError(error); }
    finally { mutationPending.current = false; setSaving(false); }
  }

  async function saveInterview(event) {
    event.preventDefault();
    if (mutationPending.current) return;
    mutationPending.current = true;
    setSaving(true);
    try {
      const payload = {
        ...interviewForm,
        interviewDate: interviewForm.interviewDate || null
      };
      await api.post('/interviews', payload);
      setInterviewForm(emptyInterview);
      await loadData();
    } catch (error) { handleError(error); }
    finally { mutationPending.current = false; setSaving(false); }
  }

  function handleError(error) {
    if ([401, 403].includes(error.response?.status)) logout();
    setMessage(errorMessage(error));
  }

  function logout() {
    sessionVersion.current += 1;
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setUser(null);
    setApplications([]);
    setInterviews([]);
    setStats({ total: 0, applied: 0, interview: 0, offer: 0, rejected: 0 });
    setMessage('');
    setEditingId(null);
    setApplicationForm(newApplication());
    setInterviewForm(emptyInterview);
    setFilter('ALL');
    setAuthForm({ name: '', email: '', password: '' });
    setSuccessMessage('');
    setAuthMode('login');
  }

  if (!user) {
    return (
      <main className="auth-shell">
        <section className="auth-panel">
          <h1>CareerTrack</h1>
          <p className="text-secondary">Track job applications, interviews, and offers in one place.</p>

          <div className="btn-group w-100 mb-3">
            <button disabled={busy} className={`btn ${authMode === 'login' ? 'btn-dark' : 'btn-outline-dark'}`} onClick={() => { setAuthMode('login'); setMessage(''); setSuccessMessage(''); }}>
              Login
            </button>
            <button disabled={busy} className={`btn ${authMode === 'register' ? 'btn-dark' : 'btn-outline-dark'}`} onClick={() => { setAuthMode('register'); setMessage(''); setSuccessMessage(''); }}>
              Register
            </button>
          </div>

          <form onSubmit={handleAuth}>
            {authMode === 'register' && (
              <input className="form-control mb-3" placeholder="Name" aria-label="Name" required value={authForm.name}
                onChange={(e) => setAuthForm({ ...authForm, name: e.target.value })} />
            )}
            <input className="form-control mb-3" type="email" placeholder="Email" aria-label="Email" required value={authForm.email}
              onChange={(e) => setAuthForm({ ...authForm, email: e.target.value })} />
            <input className="form-control mb-3" type="password" placeholder="Password" aria-label="Password" required value={authForm.password}
              onChange={(e) => setAuthForm({ ...authForm, password: e.target.value })} />
            {message && <div className="alert alert-danger py-2">{message}</div>}
            {successMessage && <div className="alert alert-success py-2">{successMessage}</div>}
            <button className="btn btn-primary w-100" type="submit" disabled={busy}>
              {busy ? 'Please wait…' : authMode === 'login' ? 'Login' : 'Create Account'}
            </button>
          </form>
        </section>
      </main>
    );
  }

  return (
    <main>
      <nav className="navbar navbar-dark bg-dark">
        <div className="container">
          <span className="navbar-brand mb-0 h1">CareerTrack</span>
          <div className="d-flex align-items-center gap-3 text-white">
            <span>{user.name}</span>
            <button disabled={saving} className="btn btn-outline-light btn-sm" onClick={logout}>Logout</button>
          </div>
        </div>
      </nav>

      <div className="container py-4">
        {message && <div className="alert alert-warning">{message}</div>}

        <section className="stats-grid mb-4">
          <Stat label="Total" value={stats.total} />
          <Stat label="Applied" value={stats.applied} />
          <Stat label="Interview" value={stats.interview} />
          <Stat label="Offer" value={stats.offer} />
          <Stat label="Rejected" value={stats.rejected} />
        </section>

        <div className="row g-4">
          <section className="col-lg-5">
            <div className="panel">
              <h2>{editingId ? 'Edit Application' : 'Add Application'}</h2>
              <form onSubmit={saveApplication}>
                <fieldset disabled={saving}>
                <input className="form-control mb-2" placeholder="Company name" aria-label="Company name" value={applicationForm.companyName}
                  onChange={(e) => setApplicationForm({ ...applicationForm, companyName: e.target.value })} required />
                <input className="form-control mb-2" placeholder="Job title" aria-label="Job title" value={applicationForm.jobTitle}
                  onChange={(e) => setApplicationForm({ ...applicationForm, jobTitle: e.target.value })} required />
                <input className="form-control mb-2" placeholder="Location" aria-label="Location" value={applicationForm.location}
                  onChange={(e) => setApplicationForm({ ...applicationForm, location: e.target.value })} />
                <select className="form-select mb-2" aria-label="Job type" value={applicationForm.jobType}
                  onChange={(e) => setApplicationForm({ ...applicationForm, jobType: e.target.value })}>
                  <option>Full Time</option>
                  <option>Internship</option>
                  <option>Contract</option>
                  <option>Remote</option>
                </select>
                <select className="form-select mb-2" aria-label="Application status" value={applicationForm.status}
                  onChange={(e) => setApplicationForm({ ...applicationForm, status: e.target.value })}>
                  <option value="APPLIED">Applied</option>
                  <option value="INTERVIEW">Interview</option>
                  <option value="OFFER">Offer</option>
                  <option value="REJECTED">Rejected</option>
                </select>
                <input className="form-control mb-2" type="date" aria-label="Applied date" value={applicationForm.appliedDate}
                  onChange={(e) => setApplicationForm({ ...applicationForm, appliedDate: e.target.value })} />
                <textarea className="form-control mb-3" placeholder="Notes" aria-label="Application notes" value={applicationForm.notes}
                  onChange={(e) => setApplicationForm({ ...applicationForm, notes: e.target.value })} />
                <button className="btn btn-primary w-100" type="submit">
                  {editingId ? 'Update Application' : 'Add Application'}
                </button>
                {editingId && <button className="btn btn-link w-100" type="button" onClick={() => {
                  setEditingId(null); setApplicationForm(newApplication());
                }}>Cancel editing</button>}
                </fieldset>
              </form>
            </div>

            <div className="panel mt-4">
              <h2>Add Interview</h2>
              <form onSubmit={saveInterview}>
                <fieldset disabled={saving}>
                <select className="form-select mb-2" aria-label="Interview application" value={interviewForm.applicationId}
                  onChange={(e) => setInterviewForm({ ...interviewForm, applicationId: e.target.value })} required>
                  <option value="">Select application</option>
                  {applications.map((app) => (
                    <option key={app.id} value={app.id}>{app.companyName} - {app.jobTitle}</option>
                  ))}
                </select>
                <input className="form-control mb-2" placeholder="Round" aria-label="Interview round" value={interviewForm.round}
                  onChange={(e) => setInterviewForm({ ...interviewForm, round: e.target.value })} required />
                <input className="form-control mb-2" type="datetime-local" aria-label="Interview date and time" value={interviewForm.interviewDate}
                  onChange={(e) => setInterviewForm({ ...interviewForm, interviewDate: e.target.value })} required />
                <select className="form-select mb-2" aria-label="Interview mode" value={interviewForm.mode}
                  onChange={(e) => setInterviewForm({ ...interviewForm, mode: e.target.value })}>
                  <option>Online</option>
                  <option>Offline</option>
                  <option>Phone</option>
                </select>
                <textarea className="form-control mb-3" placeholder="Interview notes" aria-label="Interview notes" value={interviewForm.notes}
                  onChange={(e) => setInterviewForm({ ...interviewForm, notes: e.target.value })} />
                <button className="btn btn-outline-primary w-100" type="submit">Add Interview</button>
                </fieldset>
              </form>
            </div>
          </section>

          <section className="col-lg-7">
            <div className="panel">
              <div className="d-flex justify-content-between align-items-center mb-3">
                <h2>Applications</h2>
                <select className="form-select filter-select" aria-label="Filter applications" value={filter} onChange={(e) => setFilter(e.target.value)}>
                  <option value="ALL">All</option>
                  <option value="APPLIED">Applied</option>
                  <option value="INTERVIEW">Interview</option>
                  <option value="OFFER">Offer</option>
                  <option value="REJECTED">Rejected</option>
                </select>
              </div>

              <div className="application-list">
                {filteredApplications.map((app) => (
                  <article className="application-card" key={app.id}>
                    <div>
                      <h3>{app.companyName}</h3>
                      <p>{app.jobTitle} - {app.location || 'Location not added'}</p>
                      <span className={`status ${app.status.toLowerCase()}`}>{app.status}</span>
                    </div>
                    <div className="text-end">
                      <p className="small text-secondary mb-2">{app.appliedDate}</p>
                      <button disabled={saving} className="btn btn-sm btn-outline-dark me-2" onClick={() => editApplication(app)}>Edit</button>
                      <button disabled={saving} className="btn btn-sm btn-outline-danger" onClick={() => deleteApplication(app.id)}>Delete</button>
                    </div>
                  </article>
                ))}
                {filteredApplications.length === 0 && <p className="text-secondary mb-0">No applications found.</p>}
              </div>
            </div>

            <div className="panel mt-4">
              <h2>Interviews</h2>
              {interviews.map((interview) => (
                <article className="interview-row" key={interview.id}>
                  <strong>{interview.round}</strong>
                  <span>{new Date(interview.interviewDate).toLocaleString()}</span>
                  <span>{interview.mode}</span>
                </article>
              ))}
              {interviews.length === 0 && <p className="text-secondary mb-0">No interviews scheduled.</p>}
            </div>
          </section>
        </div>
      </div>
    </main>
  );
}

function Stat({ label, value }) {
  return (
    <article className="stat-card">
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
  );
}

export default App;
