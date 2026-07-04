import React, { useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import './styles.css';

function App() {
  const [flow, setFlow] = useState(null);
  const [audit, setAudit] = useState([]);

  useEffect(() => {
    fetch('/auth/flow')
      .then((response) => response.json())
      .then(setFlow)
      .catch(() => setFlow({ error: 'Unable to load flow metadata' }));
  }, []);

  const loadAudit = () => {
    fetch('/auth/audit')
      .then((response) => response.json())
      .then(setAudit)
      .catch(() => setAudit([]));
  };

  return (
    <main className="shell">
      <section className="hero">
        <p className="eyebrow">Spring Boot 3.5 · OAuth2/OIDC · XML State Engine</p>
        <h1>OIDC Authentication State Engine</h1>
        <p>
          Auth0 performs identity authentication while Spring Boot executes a configurable post-login journey,
          authorization decision, and audit trail.
        </p>
        <div className="actions">
          <a className="primary" href="/oauth2/authorization/auth0">Login with Auth0</a>
          <a className="secondary" href="/auth/session">View Session JSON</a>
          <button onClick={loadAudit}>Load Audit</button>
        </div>
      </section>

      <section className="card">
        <h2>Loaded Authentication Flow</h2>
        <pre>{JSON.stringify(flow, null, 2)}</pre>
      </section>

      <section className="card">
        <h2>Recent Audit Records</h2>
        <pre>{JSON.stringify(audit, null, 2)}</pre>
      </section>
    </main>
  );
}

createRoot(document.getElementById('root')).render(<App />);
