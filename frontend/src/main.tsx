import { StrictMode, useEffect, useMemo, useState } from "react";
import { createRoot } from "react-dom/client";
import axios from "axios";
import "./styles.css";

type User = { token: string; email: string; role: string; tenantId?: string };
type Plan = { id: string; name: string; billingPeriod: "MONTHLY" | "YEARLY"; priceInr: number };
type Institution = { id: string; name: string; code: string; address?: string; active: boolean };
type Tenant = { id: string; name: string; slug: string; active: boolean };
type Quote = { planId: string; baseAmount: number; discountPercent: number; payableAmount: number; currency: string };
type DiscountToken = { id: string; code: string; discountPercent: number; appliesTo: string; active: boolean; expiresAt?: string };
type RazorpayOrder = { orderId: string; amount: number; currency: string; keyId: string; customerName: string; customerEmail: string };
declare global { interface Window { Razorpay?: new (options: Record<string, unknown>) => { open: () => void }; } }

const api = axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api" });
api.interceptors.request.use((config) => {
  const token = localStorage.getItem("elneb_token");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

function errorText(error: unknown) {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data;
    if (typeof data?.message === "string") return data.message;
    if (typeof data?.error?.description === "string") return data.error.description;
    return `Request failed (${error.response?.status ?? "network error"})`;
  }
  return error instanceof Error ? error.message : "Something went wrong";
}

function App() {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const superAdminRoute = window.location.pathname === "/super-admin/login";
  useEffect(() => {
    if (!localStorage.getItem("elneb_token")) return setLoading(false);
    api.get<User>("/auth/me").then(({ data }) => setUser(data)).catch(() => localStorage.removeItem("elneb_token")).finally(() => setLoading(false));
  }, []);
  if (loading) return <main className="center">Loading ELNEB...</main>;
  if (!user) return superAdminRoute ? <SuperAdminAuth onSuccess={setUser} /> : <Auth onSuccess={setUser} />;
  if (superAdminRoute && user.role !== "SUPER_ADMIN") return <SuperAdminAuth onSuccess={setUser} />;
  return <Dashboard user={user} onLogout={() => { localStorage.removeItem("elneb_token"); setUser(null); }} />;
}

function Auth({ onSuccess }: { onSuccess: (user: User) => void }) {
  const [registering, setRegistering] = useState(false);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [collegeName, setCollegeName] = useState("");
  const [collegeCode, setCollegeCode] = useState("");
  const [adminName, setAdminName] = useState("");
  const [contactNumber, setContactNumber] = useState("");
  const [address, setAddress] = useState("");
  const [message, setMessage] = useState("");
  async function submit(event: React.FormEvent) {
    event.preventDefault(); setMessage("");
    try {
      const { data } = await api.post<User>(registering ? "/auth/register/college" : "/auth/login",
        registering ? { collegeName, collegeCode, adminName, email, password, contactNumber, address } : { email, password });
      if (data.role === "SUPER_ADMIN") throw new Error("Use the Super Admin login route.");
      localStorage.setItem("elneb_token", data.token); onSuccess(data);
    } catch (error) { setMessage(errorText(error)); }
  }
  return <main className="auth"><form className="auth-card" onSubmit={submit}>
    <div className="brand-mark">E</div><p className="eyebrow">ELNEB SMART CLASSROOM</p>
    <h1>{registering ? "Create your workspace" : "Welcome back"}</h1>
    <p className="muted">{registering ? "Start your institution's 15-day free trial." : "Sign in to manage your smart campus."}</p>
    {registering && <div className="registration-grid">
      <label>College name<input required value={collegeName} onChange={e => setCollegeName(e.target.value)} placeholder="Rungta College" /></label>
      <label>College code<input required value={collegeCode} onChange={e => setCollegeCode(e.target.value.toUpperCase())} placeholder="RCET" /></label>
      <label>Admin name<input required value={adminName} onChange={e => setAdminName(e.target.value)} placeholder="College administrator" /></label>
      <label>Contact number<input required value={contactNumber} onChange={e => setContactNumber(e.target.value)} /></label>
      <label className="full-field">College address<textarea required value={address} onChange={e => setAddress(e.target.value)} rows={2} /></label>
    </div>}
    <label>Email<input type="email" required value={email} onChange={e => setEmail(e.target.value)} /></label>
    <label>Password<input type="password" minLength={8} required value={password} onChange={e => setPassword(e.target.value)} /></label>
    {message && <p className="error">{message}</p>}<button type="submit">{registering ? "Create account" : "Sign in"}</button>
    <button className="link-button" type="button" onClick={() => { setRegistering(!registering); setMessage(""); }}>{registering ? "Already registered? Sign in" : "Create institution account"}</button>
  </form></main>;
}

function SuperAdminAuth({ onSuccess }: { onSuccess: (user: User) => void }) {
  const [email, setEmail] = useState(""); const [password, setPassword] = useState(""); const [message, setMessage] = useState("");
  async function submit(event: React.FormEvent) {
    event.preventDefault(); setMessage("");
    try {
      const { data } = await api.post<User>("/auth/login", { email, password });
      if (data.role !== "SUPER_ADMIN") throw new Error("This account is not a Super Admin account.");
      localStorage.setItem("elneb_token", data.token); onSuccess(data);
    } catch (error) { setMessage(error instanceof Error ? error.message : errorText(error)); }
  }
  return <main className="auth super-auth"><div className="auth-glow" /><form className="auth-card super-auth-card" onSubmit={submit}>
    <div className="brand-mark super-brand">E</div><p className="eyebrow">ELNEB PLATFORM CONTROL</p><h1>Super Admin access</h1>
    <p className="muted">Manage every institution, subscription, device and offer from one secure command center.</p>
    <label>Platform email<input type="email" required value={email} onChange={e => setEmail(e.target.value)} /></label>
    <label>Password<input type="password" required value={password} onChange={e => setPassword(e.target.value)} /></label>
    {message && <p className="error">{message}</p>}<button type="submit">Enter command center <span>→</span></button>
    <a className="link-button" href="/">← College login</a>
  </form></main>;
}

function Dashboard({ user, onLogout }: { user: User; onLogout: () => void }) {
  const [section, setSection] = useState("Overview");
  const role = user.role.replace(/_/g, " ");
  const menu = user.role === "SUPER_ADMIN"
    ? ["Overview", "Colleges", "Subscriptions", "Devices", "Coupons", "Settings"]
    : ["Overview", "Institutions", "Users", "Classrooms", "Teachers", "Timetable", "RFID & Attendance", "Devices", "Live Monitoring", "Reports", "Subscription"];
  return <main className="app-shell">
    <aside><div className="logo"><span className="logo-icon">E</span> ELNEB</div><p className="sidebar-label">WORKSPACE</p>
      {menu.map(item => <button key={item} className={section === item ? "nav-item active" : "nav-item"} onClick={() => setSection(item)}><span>{icon(item)}</span>{item}</button>)}
      <div className="sidebar-bottom"><span className="avatar">{user.email[0].toUpperCase()}</span><div><b>{user.email.split("@")[0]}</b><small>{role}</small></div><button className="logout" onClick={onLogout}>↪</button></div>
    </aside>
    <section className="main-panel"><header className="topbar"><div><p className="eyebrow">ELNEB CONTROL CENTER</p><h2>{section}</h2></div><span className="status-dot">● System operational</span></header>
      <div className="content">{user.role === "SUPER_ADMIN" ? <SuperAdminPanel section={section} user={user} /> : <Section section={section} user={user} />}</div>
    </section>
  </main>;
}

type SuperCollege = { tenantId: string; name: string; slug: string; active: boolean; subscriptionStatus: string; subscriptionEndsAt?: string; deviceCount: number };
type SuperDashboard = { totalColleges: number; activeSubscriptions: number; trialColleges: number; expiredColleges: number; totalClassrooms: number; totalDevices: number; onlineDevices: number; totalCoupons: number; colleges: SuperCollege[] };
type SuperSubscription = { tenantId: string; collegeName: string; plan: string; status: string; trial: boolean; startsAt: string; endsAt: string };

function SuperAdminPanel({ section, user }: { section: string; user: User }) {
  const [dashboard, setDashboard] = useState<SuperDashboard | null>(null); const [subscriptions, setSubscriptions] = useState<SuperSubscription[]>([]); const [query, setQuery] = useState(""); const [loading, setLoading] = useState(true); const [message, setMessage] = useState("");
  async function load() { try { const { data } = await api.get<SuperDashboard>("/super-admin/dashboard"); setDashboard(data); setMessage(""); } catch (e) { setMessage(errorText(e)); } finally { setLoading(false); } }
  useEffect(() => { load(); }, []);
  async function loadSubscriptions() { try { const { data } = await api.get<SuperSubscription[]>("/super-admin/subscriptions"); setSubscriptions(data); } catch (e) { setMessage(errorText(e)); } }
  if (loading) return <div className="dashboard-loading"><span className="loading-orb">E</span><b>Preparing command center</b><small>Syncing platform intelligence...</small></div>;
  if (!dashboard) return <article className="panel"><p className="error">{message || "Unable to load platform dashboard."}</p><button onClick={load}>Try again</button></article>;
  if (section === "Colleges") return <SuperColleges data={dashboard.colleges} query={query} setQuery={setQuery} onRefresh={load} />;
  if (section === "Subscriptions") return <SuperSubscriptions rows={subscriptions} onLoad={loadSubscriptions} />;
  if (section === "Devices") return <SuperDevices data={dashboard} />;
  if (section === "Coupons") return <CouponManager />;
  if (section === "Settings") return <SuperSettings user={user} />;
  return <SuperOverview data={dashboard} />;
}

function SuperOverview({ data }: { data: SuperDashboard }) {
  const cards = [["Registered colleges", data.totalColleges, "Platform workspaces", "◈"], ["Active subscriptions", data.activeSubscriptions, "Paying institutions", "✦"], ["Trial colleges", data.trialColleges, "Currently exploring ELNEB", "◷"], ["Expired accounts", data.expiredColleges, "Require attention", "⌁"]];
  return <><div className="super-hero"><div><p className="eyebrow">PLATFORM PULSE · LIVE</p><h1>Good evening, platform owner.</h1><p className="muted">Your complete ELNEB ecosystem, beautifully organized.</p></div><div className="pulse-badge"><span /> All systems operational</div></div><div className="stats super-stats">{cards.map(([label, value, note, icon], index) => <article className={`stat super-stat stat-${index}`} key={String(label)}><div className="stat-icon">{icon}</div><span>{label}</span><strong>{value}</strong><small>{note}</small></article>)}</div><div className="super-grid"><article className="panel chart-panel"><div className="page-intro"><div><p className="eyebrow">ECOSYSTEM SNAPSHOT</p><h3>Infrastructure at a glance</h3></div><span className="live-label">● LIVE</span></div><div className="metric-bars"><div><span>Classrooms</span><b>{data.totalClassrooms}</b><i style={{ width: `${Math.min(100, data.totalClassrooms * 4 + 8)}%` }} /></div><div><span>Connected devices</span><b>{data.totalDevices}</b><i style={{ width: `${data.totalDevices ? Math.max(12, data.onlineDevices / data.totalDevices * 100) : 12}%` }} /></div><div><span>Online now</span><b>{data.onlineDevices}</b><i style={{ width: `${data.totalDevices ? Math.max(8, data.onlineDevices / data.totalDevices * 100) : 8}%` }} /></div></div></article><article className="panel command-panel"><p className="eyebrow">QUICK ACTIONS</p><h3>Keep your platform moving</h3><div className="quick-actions"><div><span>✦</span><b>Review colleges</b><small>Monitor institution health</small></div><div><span>♢</span><b>Launch an offer</b><small>Create a discount coupon</small></div><div><span>⌁</span><b>Check devices</b><small>See connectivity status</small></div></div></article></div></>;
}

function SuperColleges({ data, query, setQuery, onRefresh }: { data: SuperCollege[]; query: string; setQuery: (value: string) => void; onRefresh: () => void }) {
  const rows = data.filter(row => `${row.name} ${row.slug}`.toLowerCase().includes(query.toLowerCase()));
  return <article className="panel"><div className="page-intro"><div><p className="eyebrow">TENANT DIRECTORY</p><h3>Registered colleges</h3><p className="muted">Every institution using the ELNEB platform.</p></div><button className="small-button" onClick={onRefresh}>Refresh</button></div><div className="toolbar"><input value={query} onChange={e => setQuery(e.target.value)} placeholder="Search colleges..." /><span className="result-count">{rows.length} colleges</span></div><div className="super-table"><table><thead><tr><th>College</th><th>Status</th><th>Subscription expiry</th><th>Devices</th><th>Workspace</th></tr></thead><tbody>{rows.map(row => <tr key={row.tenantId}><td><span className="table-avatar">{row.name[0]}</span><div><b>{row.name}</b><small>{row.slug}</small></div></td><td><span className={`status-pill ${row.subscriptionStatus.toLowerCase()}`}>{row.subscriptionStatus}</span></td><td>{row.subscriptionEndsAt ? new Date(row.subscriptionEndsAt).toLocaleDateString() : "—"}</td><td><strong>{row.deviceCount}</strong></td><td><span className={row.active ? "online-dot" : "offline-dot"} /> {row.active ? "Active" : "Inactive"}</td></tr>)}</tbody></table>{!rows.length && <div className="empty-state"><b>No colleges found</b><p>Try another search term.</p></div>}</div></article>;
}

function SuperSubscriptions({ rows, onLoad }: { rows: SuperSubscription[]; onLoad: () => void }) {
  return <article className="panel"><div className="page-intro"><div><p className="eyebrow">REVENUE OPERATIONS</p><h3>Subscription lifecycle</h3><p className="muted">Track trials, renewals and expiry across colleges.</p></div><button className="small-button" onClick={onLoad}>Load subscriptions</button></div>{rows.length ? <div className="super-table"><table><thead><tr><th>College</th><th>Plan</th><th>Status</th><th>Started</th><th>Expires</th></tr></thead><tbody>{rows.map(row => <tr key={`${row.tenantId}-${row.endsAt}`}><td><b>{row.collegeName}</b></td><td>{row.plan}</td><td><span className={`status-pill ${row.status.toLowerCase()}`}>{row.trial ? "TRIAL" : row.status}</span></td><td>{new Date(row.startsAt).toLocaleDateString()}</td><td>{new Date(row.endsAt).toLocaleDateString()}</td></tr>)}</tbody></table></div> : <div className="empty-state"><span className="empty-icon">◷</span><b>Load subscription intelligence</b><p>See every college plan and expiry date here.</p></div>}</article>;
}

function SuperDevices({ data }: { data: SuperDashboard }) {
  const percent = data.totalDevices ? Math.round(data.onlineDevices / data.totalDevices * 100) : 0;
  return <div className="super-grid"><article className="panel device-health"><p className="eyebrow">DEVICE NETWORK</p><h3>Connectivity health</h3><div className="health-ring" style={{ "--progress": `${percent * 3.6}deg` } as React.CSSProperties}><strong>{percent}%</strong><small>online</small></div><div className="health-legend"><span><i className="online-dot" /> Online <b>{data.onlineDevices}</b></span><span><i className="offline-dot" /> Offline <b>{data.totalDevices - data.onlineDevices}</b></span></div></article><article className="panel"><p className="eyebrow">GLOBAL INVENTORY</p><h3>Platform infrastructure</h3><div className="inventory-row"><span>All devices</span><strong>{data.totalDevices}</strong></div><div className="inventory-row"><span>Active classrooms</span><strong>{data.totalClassrooms}</strong></div><div className="inventory-row"><span>Average devices / college</span><strong>{data.totalColleges ? (data.totalDevices / data.totalColleges).toFixed(1) : "0"}</strong></div></article></div>;
}

function CouponManager() {
  const [code, setCode] = useState(""); const [discount, setDiscount] = useState("10"); const [appliesTo, setAppliesTo] = useState("BOTH"); const [message, setMessage] = useState("");
  async function create(event: React.FormEvent) { event.preventDefault(); try { await api.post("/subscriptions/discount-tokens", { code, discountPercent: Number(discount), appliesTo, expiresAt: null }); setMessage("Coupon created successfully."); setCode(""); } catch (e) { setMessage(errorText(e)); } }
  return <div className="two-col"><article className="panel form-panel coupon-hero"><div className="coupon-symbol">%</div><p className="eyebrow">GROWTH TOOLKIT</p><h3>Create a platform offer</h3><p className="muted">Reward a college with a beautiful, trackable discount code.</p><form onSubmit={create}><label>Coupon code<input required value={code} onChange={e => setCode(e.target.value.toUpperCase())} placeholder="WELCOME25" /></label><label>Discount percentage<input required type="number" min="1" max="100" value={discount} onChange={e => setDiscount(e.target.value)} /></label><label>Applies to<select value={appliesTo} onChange={e => setAppliesTo(e.target.value)}><option value="BOTH">Monthly & yearly</option><option value="MONTHLY">Monthly only</option><option value="YEARLY">Yearly only</option></select></label><button type="submit">Generate coupon <span>✦</span></button></form>{message && <p className={message.includes("successfully") ? "success" : "error"}>{message}</p>}</article><article className="panel offer-preview"><p className="eyebrow">LIVE PREVIEW</p><div className="offer-card"><span>ELNEB OFFER</span><strong>{discount}% OFF</strong><b>{code || "YOURCODE"}</b><small>For {appliesTo === "BOTH" ? "all plans" : `${appliesTo.toLowerCase()} plans`}</small></div><p className="muted">This code can be shared with a college during checkout.</p></article></div>;
}

function SuperSettings({ user }: { user: User }) {
  return <article className="panel settings-card"><div className="settings-orb">E</div><p className="eyebrow">PLATFORM SETTINGS</p><h3>Super Admin profile</h3><p className="muted">You are signed in as the ELNEB platform owner.</p><div className="setting-row"><span>Email</span><b>{user.email}</b></div><div className="setting-row"><span>Access level</span><span className="status-pill active">GLOBAL ADMIN</span></div><div className="setting-row"><span>Security</span><b>JWT protected</b></div></article>;
}

function Section({ section, user }: { section: string; user: User }) {
  if (section === "Subscriptions" || section === "Subscription") return <Subscription user={user} />;
  if (section === "Tenants") return <TenantManager />;
  if (section === "Institutions") return <InstitutionManager user={user} />;
  if (section === "Users") return <UserManager user={user} />;
  if (section === "Classrooms") return <ClassroomManager user={user} />;
  if (section === "Teachers") return <TeacherManager />;
  if (section === "Timetable") return <TimetableManager />;
  if (section === "RFID & Attendance") return <AttendanceCenter />;
  if (section === "Devices") return <DeviceManager />;
  if (section === "Live Monitoring") return <LiveMonitoring />;
  if (section === "Reports") return <Reports />;
  return <Overview user={user} />;
}

function Overview({ user }: { user: User }) {
  const cards = [["Classrooms monitored", "0", "Create your campus structure"], ["Teachers checked in", "0", "RFID events will appear here"], ["Devices online", "0", "Connect an ESP32 device"], ["Active subscription", "Trial", "15-day trial available"]];
  return <><div className="welcome"><div><p className="eyebrow">GOOD DAY</p><h1>Your smart campus, at a glance.</h1><p className="muted">Welcome back, {user.email.split("@")[0]}. Here is what is happening across your workspace.</p></div><button>＋ Add classroom</button></div><div className="stats">{cards.map(([label, value, note]) => <article className="stat" key={label}><span>{label}</span><strong>{value}</strong><small>{note}</small></article>)}</div><div className="panel-grid"><article className="panel"><h3>Quick setup</h3><div className="setup-row"><span className="step done">1</span><div><b>Account created</b><small>Your secure workspace is ready</small></div></div><div className="setup-row"><span className="step">2</span><div><b>Add your institution</b><small>Set up campuses, buildings and rooms</small></div><button className="small-button">Start</button></div><div className="setup-row"><span className="step">3</span><div><b>Connect an ESP32 device</b><small>Monitor your first classroom</small></div></div></article><article className="panel notice"><h3>Live monitoring</h3><div className="empty-state"><span className="empty-icon">◌</span><b>No classrooms configured yet</b><p>Add your institution structure to start monitoring.</p></div></article></div></>;
}

function TenantManager() {
  const [name, setName] = useState(""); const [slug, setSlug] = useState(""); const [message, setMessage] = useState("");
  async function create(event: React.FormEvent) { event.preventDefault(); try { const { data } = await api.post<Tenant>("/tenants", { name, slug }); setMessage(`Tenant ${data.name} created successfully.`); setName(""); setSlug(""); } catch (e) { setMessage(errorText(e)); } }
  return <article className="panel form-panel"><h3>Create tenant</h3><p className="muted">Super Admin can create an institution workspace.</p><form onSubmit={create}><label>Tenant name<input required value={name} onChange={e => setName(e.target.value)} /></label><label>Unique slug<input required pattern="[a-z0-9-]+" value={slug} onChange={e => setSlug(e.target.value)} /></label><button>Create tenant</button>{message && <p className="success">{message}</p>}</form></article>;
}

function InstitutionManager({ user }: { user: User }) {
  const [tenantId, setTenantId] = useState(user.tenantId ?? ""); const [name, setName] = useState(""); const [code, setCode] = useState(""); const [address, setAddress] = useState(""); const [items, setItems] = useState<Institution[]>([]); const [message, setMessage] = useState("");
  useEffect(() => { if (user.tenantId) setTenantId(user.tenantId); }, [user.tenantId]);
  async function create(event: React.FormEvent) { event.preventDefault(); try { const { data } = await api.post<Institution>(`/institutions/tenant/${tenantId}`, { name, code, address }); setItems([...items, data]); setMessage("Institution created."); } catch (e) { setMessage(errorText(e)); } }
  return <div className="two-col"><article className="panel form-panel"><h3>Add institution</h3><p className="muted">Your college registration already created the institution. Additional records can be added here.</p><form onSubmit={create}>{user.role === "SUPER_ADMIN" && <label>Tenant ID<input required value={tenantId} onChange={e => setTenantId(e.target.value)} placeholder="Tenant UUID" /></label>}<label>Institution name<input required value={name} onChange={e => setName(e.target.value)} /></label><label>Code<input required value={code} onChange={e => setCode(e.target.value)} /></label><label>Address<input value={address} onChange={e => setAddress(e.target.value)} /></label><button>Create institution</button>{message && <p className={message.includes("created") ? "success" : "error"}>{message}</p>}</form></article><article className="panel"><h3>Institutions</h3>{items.length ? items.map(item => <div className="list-row" key={item.id}><span className="avatar">{item.name[0]}</span><div><b>{item.name}</b><small>{item.code} · {item.active ? "Active" : "Inactive"}</small></div></div>) : <div className="empty-state"><span className="empty-icon">▦</span><b>No institutions loaded</b><p>Use the form to create your first institution.</p></div>}</article></div>;
}

type ManagedUser = { id: string; email: string; displayName?: string; contactNumber?: string; role: string; active: boolean };
function UserManager({ user }: { user: User }) {
  const [items, setItems] = useState<ManagedUser[]>([]);
  const [displayName, setDisplayName] = useState(""); const [email, setEmail] = useState("");
  const [password, setPassword] = useState(""); const [role, setRole] = useState("HOD");
  const [contactNumber, setContactNumber] = useState(""); const [message, setMessage] = useState("");
  async function load() {
    if (!user.tenantId) return;
    try { const { data } = await api.get<ManagedUser[]>(`/users/tenant/${user.tenantId}`); setItems(data); setMessage(""); }
    catch (e) { setMessage(errorText(e)); }
  }
  useEffect(() => { load(); }, [user.tenantId]);
  async function create(event: React.FormEvent) {
    event.preventDefault();
    if (!user.tenantId) return setMessage("Your account is not linked to an institution.");
    try {
      const { data } = await api.post<ManagedUser>(`/users/tenant/${user.tenantId}`,
        { displayName, email, password, role, contactNumber });
      setItems([data, ...items]); setDisplayName(""); setEmail(""); setPassword(""); setContactNumber("");
      setMessage("Staff account created successfully.");
    } catch (e) { setMessage(errorText(e)); }
  }
  return <div className="two-col"><article className="panel form-panel"><h3>Invite college staff</h3><p className="muted">Create secure HOD, reception and device-admin accounts for this college.</p>
    <form onSubmit={create}><label>Full name<input required value={displayName} onChange={e => setDisplayName(e.target.value)} /></label>
      <label>Email<input required type="email" value={email} onChange={e => setEmail(e.target.value)} /></label>
      <label>Temporary password<input required minLength={8} type="password" value={password} onChange={e => setPassword(e.target.value)} /></label>
      <label>Role<select value={role} onChange={e => setRole(e.target.value)}><option value="HOD">HOD</option><option value="RECEPTION">Reception viewer</option><option value="DEVICE_ADMIN">Device admin</option><option value="TEACHER">Teacher</option></select></label>
      <label>Contact number<input value={contactNumber} onChange={e => setContactNumber(e.target.value)} /></label>
      <button type="submit">Create staff account</button></form>{message && <p className={message.includes("successfully") ? "success" : "error"}>{message}</p>}
    </article><article className="panel"><div className="panel-heading"><div><h3>College users</h3><p className="muted">Only users belonging to your institution are shown.</p></div><button className="small-button" onClick={load}>Refresh</button></div>
      {items.length ? items.map(item => <div className="list-row" key={item.id}><span className="avatar">{(item.displayName ?? item.email)[0].toUpperCase()}</span><div><b>{item.displayName || item.email}</b><small>{item.email} · {item.role.replace(/_/g, " ")} · {item.active ? "Active" : "Inactive"}</small></div><button className="small-button" onClick={async () => { try { const { data } = await api.patch<ManagedUser>(`/users/${item.id}/active`, null, { params: { active: !item.active } }); setItems(items.map(row => row.id === item.id ? data : row)); } catch (e) { setMessage(errorText(e)); } }}>{item.active ? "Deactivate" : "Activate"}</button></div>) : <div className="empty-state"><b>No staff accounts yet</b><p>Create the first HOD or reception viewer above.</p></div>}
    </article></div>;
}

type Campus = { id: string; name: string; address?: string; active: boolean };
type NamedItem = { id: string; name: string; active: boolean };
type Classroom = { id: string; name: string; capacity: number; roomType: string; active: boolean };
type Teacher = { id: string; employeeCode: string; fullName: string; email?: string; department?: string; active: boolean };

function ClassroomManager({ user }: { user: User }) {
  const [institutionId, setInstitutionId] = useState(""); const [campusId, setCampusId] = useState("");
  const [buildingId, setBuildingId] = useState(""); const [floorId, setFloorId] = useState("");
  const [name, setName] = useState(""); const [address, setAddress] = useState("");
  const [capacity, setCapacity] = useState("40"); const [roomType, setRoomType] = useState("LECTURE");
  const [campuses, setCampuses] = useState<Campus[]>([]); const [buildings, setBuildings] = useState<NamedItem[]>([]);
  const [floors, setFloors] = useState<NamedItem[]>([]); const [classrooms, setClassrooms] = useState<Classroom[]>([]);
  const [institutions, setInstitutions] = useState<Institution[]>([]);
  const [message, setMessage] = useState("");
  useEffect(() => {
    if (!user.tenantId) return;
    api.get<Institution[]>(`/institutions/tenant/${user.tenantId}`).then(({ data }) => {
      setInstitutions(data);
      if (data[0]) { setInstitutionId(data[0].id); setMessage("Institution selected automatically."); }
    }).catch(e => setMessage(errorText(e)));
  }, [user.tenantId]);
  async function loadCampuses() { try { const { data } = await api.get<Campus[]>(`/campus/institution/${institutionId}`); setCampuses(data); setMessage(""); } catch (e) { setMessage(errorText(e)); } }
  async function loadBuildings() { try { const { data } = await api.get<NamedItem[]>(`/campus/${campusId}/buildings`); setBuildings(data); } catch (e) { setMessage(errorText(e)); } }
  async function loadFloors() { try { const { data } = await api.get<NamedItem[]>(`/campus/buildings/${buildingId}/floors`); setFloors(data); } catch (e) { setMessage(errorText(e)); } }
  async function loadClassrooms() { try { const { data } = await api.get<Classroom[]>(`/campus/floors/${floorId}/classrooms`); setClassrooms(data); } catch (e) { setMessage(errorText(e)); } }
  async function create(event: React.FormEvent) {
    event.preventDefault();
    try {
      if (!campusId) { const { data } = await api.post<Campus>(`/campus/institution/${institutionId}`, { name, address }); setCampuses([...campuses, data]); setCampusId(data.id); setMessage("Campus created. Add a building next."); }
      else if (!buildingId) { const { data } = await api.post<NamedItem>(`/campus/${campusId}/buildings`, { name }); setBuildings([...buildings, data]); setBuildingId(data.id); setMessage("Building created. Add a floor next."); }
      else if (!floorId) { const { data } = await api.post<NamedItem>(`/campus/buildings/${buildingId}/floors`, { name }); setFloors([...floors, data]); setFloorId(data.id); setMessage("Floor created. Add a classroom next."); }
      else { const { data } = await api.post<Classroom>(`/campus/floors/${floorId}/classrooms`, { name, capacity: Number(capacity), roomType }); setClassrooms([...classrooms, data]); setMessage("Classroom created."); }
      setName(""); setAddress(""); await loadClassrooms();
    } catch (e) { setMessage(errorText(e)); }
  }
  return <div className="two-col"><article className="panel form-panel"><h3>Build campus structure</h3><p className="muted">Create campus, building, floor and classroom records through the live API.</p>
    <label>Institution<select required value={institutionId} onChange={e => setInstitutionId(e.target.value)}><option value="">Select institution</option>{institutions.map(item => <option key={item.id} value={item.id}>{item.name} ({item.code})</option>)}</select></label><button className="small-button" type="button" onClick={loadCampuses} disabled={!institutionId}>Load campuses</button>
    <label>Campus<select value={campusId} onChange={e => { setCampusId(e.target.value); setBuildingId(""); setFloorId(""); }}>{campuses.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label>
    {campusId && <button className="small-button" type="button" onClick={loadBuildings}>Load buildings</button>}
    <label>Building<select value={buildingId} onChange={e => { setBuildingId(e.target.value); setFloorId(""); }}>{buildings.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label>
    {buildingId && <div className="inline-form"><button className="small-button" type="button" onClick={loadFloors}>Load floors</button><button className="small-button" type="button" onClick={async () => { const next = prompt("Building name", buildings.find(item => item.id === buildingId)?.name); if (!next) return; try { const { data } = await api.put<NamedItem>(`/campus/buildings/${buildingId}`, { name: next }); setBuildings(buildings.map(item => item.id === buildingId ? data : item)); setMessage("Building updated."); } catch (e) { setMessage(errorText(e)); } }}>Edit building</button><button className="small-button" type="button" onClick={async () => { try { await api.delete(`/campus/buildings/${buildingId}`); setBuildings(buildings.filter(item => item.id !== buildingId)); setBuildingId(""); setFloorId(""); setMessage("Building deactivated."); } catch (e) { setMessage(errorText(e)); } }}>Deactivate building</button></div>}
    <label>Floor<select value={floorId} onChange={e => setFloorId(e.target.value)}>{floors.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label>
    {floorId && <div className="inline-form"><button className="small-button" type="button" onClick={loadClassrooms}>Load classrooms</button><button className="small-button" type="button" onClick={async () => { const next = prompt("Floor name", floors.find(item => item.id === floorId)?.name); if (!next) return; try { const { data } = await api.put<NamedItem>(`/campus/floors/${floorId}`, { name: next }); setFloors(floors.map(item => item.id === floorId ? data : item)); setMessage("Floor updated."); } catch (e) { setMessage(errorText(e)); } }}>Edit floor</button><button className="small-button" type="button" onClick={async () => { try { await api.delete(`/campus/floors/${floorId}`); setFloors(floors.filter(item => item.id !== floorId)); setFloorId(""); setMessage("Floor deactivated."); } catch (e) { setMessage(errorText(e)); } }}>Deactivate floor</button></div>}
    <form onSubmit={create}><label>{floorId ? "Classroom name" : buildingId ? "Floor name" : campusId ? "Building name" : "Campus name"}<input required value={name} onChange={e => setName(e.target.value)} /></label>
      {!campusId && <label>Address<input value={address} onChange={e => setAddress(e.target.value)} /></label>}
      {floorId && <><label>Capacity<input type="number" min="1" value={capacity} onChange={e => setCapacity(e.target.value)} /></label><label>Room type<input value={roomType} onChange={e => setRoomType(e.target.value)} /></label></>}
      <button type="submit">Create {floorId ? "classroom" : buildingId ? "floor" : campusId ? "building" : "campus"}</button></form>{message && <p className={message.includes("created") ? "success" : "error"}>{message}</p>}</article>
    <article className="panel"><h3>Classrooms</h3>{classrooms.length ? classrooms.map(item => <div className="list-row" key={item.id}><div><b>{item.name}</b><small>{item.roomType} · Capacity {item.capacity} · {item.active ? "Active" : "Inactive"}</small></div><button className="small-button" onClick={async () => { const next = prompt("Classroom name", item.name); if (!next) return; try { const { data } = await api.put<Classroom>(`/campus/classrooms/${item.id}`, { name: next, capacity: item.capacity, roomType: item.roomType }); setClassrooms(classrooms.map(row => row.id === item.id ? data : row)); } catch (e) { setMessage(errorText(e)); } }}>Edit</button><button className="small-button" onClick={async () => { try { const { data } = await api.patch<Classroom>(`/campus/classrooms/${item.id}/active`, null, { params: { active: !item.active } }); setClassrooms(classrooms.map(row => row.id === item.id ? data : row)); } catch (e) { setMessage(errorText(e)); } }}>{item.active ? "Deactivate" : "Activate"}</button></div>) : <div className="empty-state"><b>No classrooms loaded</b><p>Select a floor and load its classrooms.</p></div>}</article></div>;
}

function TeacherManager() {
  const [institutionId, setInstitutionId] = useState(""); const [teacherId, setTeacherId] = useState("");
  const [employeeCode, setEmployeeCode] = useState(""); const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState(""); const [department, setDepartment] = useState(""); const [cardUid, setCardUid] = useState("");
  const [teachers, setTeachers] = useState<Teacher[]>([]); const [message, setMessage] = useState("");
  async function load() { try { const { data } = await api.get<Teacher[]>(`/teachers/institution/${institutionId}`); setTeachers(data); setMessage(""); } catch (e) { setMessage(errorText(e)); } }
  async function create(event: React.FormEvent) { event.preventDefault(); try { const { data } = await api.post<Teacher>(`/teachers/institution/${institutionId}`, { employeeCode, fullName, email, department }); setTeachers([...teachers, data]); setTeacherId(data.id); setMessage("Teacher created. You can assign an RFID card."); setEmployeeCode(""); setFullName(""); } catch (e) { setMessage(errorText(e)); } }
  async function assign(event: React.FormEvent) { event.preventDefault(); try { await api.post(`/teachers/${teacherId}/rfid`, { cardUid }); setMessage("RFID card assigned."); setCardUid(""); } catch (e) { setMessage(errorText(e)); } }
  return <div className="two-col"><div><article className="panel form-panel"><h3>Add teacher</h3><form onSubmit={create}><label>Institution ID<input required value={institutionId} onChange={e => setInstitutionId(e.target.value)} placeholder="Institution UUID" /></label><label>Employee code<input required value={employeeCode} onChange={e => setEmployeeCode(e.target.value)} /></label><label>Full name<input required value={fullName} onChange={e => setFullName(e.target.value)} /></label><label>Email<input type="email" value={email} onChange={e => setEmail(e.target.value)} /></label><label>Department<input value={department} onChange={e => setDepartment(e.target.value)} /></label><button type="submit">Create teacher</button></form></article><article className="panel form-panel"><h3>Assign RFID</h3><form onSubmit={assign}><label>Teacher ID<input required value={teacherId} onChange={e => setTeacherId(e.target.value)} /></label><label>Card UID<input required value={cardUid} onChange={e => setCardUid(e.target.value)} /></label><button type="submit">Assign card</button></form>{teacherId && <button className="small-button" onClick={async () => { try { await api.delete(`/teachers/${teacherId}/rfid`); setMessage("RFID card unassigned."); } catch (e) { setMessage(errorText(e)); } }}>Unassign RFID</button>}{message && <p className={message.includes("created") || message.includes("assigned") || message.includes("unassigned") ? "success" : "error"}>{message}</p>}</article></div><article className="panel"><div className="page-intro"><h3>Teachers</h3><button className="small-button" onClick={load} disabled={!institutionId}>Refresh</button></div>{teachers.length ? teachers.map(item => <div className="list-row" key={item.id}><div><b>{item.fullName}</b><small>{item.employeeCode} · {item.department || "No department"} · {item.email || "No email"}</small></div><button className="small-button" onClick={async () => { const next = prompt("Teacher name", item.fullName); if (!next) return; try { const { data } = await api.put<Teacher>(`/teachers/${item.id}`, { employeeCode: item.employeeCode, fullName: next, email: item.email, department: item.department }); setTeachers(teachers.map(row => row.id === item.id ? data : row)); } catch (e) { setMessage(errorText(e)); } }}>Edit</button><button className="small-button" onClick={async () => { try { await api.delete(`/teachers/${item.id}`); setTeachers(teachers.filter(row => row.id !== item.id)); setMessage("Teacher deactivated."); } catch (e) { setMessage(errorText(e)); } }}>Deactivate</button></div>) : <div className="empty-state"><b>No teachers loaded</b><p>Enter an institution ID and refresh.</p></div>}</article></div>;
}

type ReportSummary = { sessions: number; attendanceRecords: number; classrooms: number; teachers: number; devices: number; onlineDevices: number; present: number; completed: number };
type ReportRow = Record<string, unknown>;

function Reports() {
  const [from, setFrom] = useState(new Date(Date.now() - 30 * 86400000).toISOString().slice(0, 10)); const [to, setTo] = useState(new Date().toISOString().slice(0, 10)); const [teacherId, setTeacherId] = useState(""); const [classroomId, setClassroomId] = useState(""); const [page, setPage] = useState(0);
  const [type, setType] = useState("attendance"); const [summary, setSummary] = useState<ReportSummary | null>(null); const [rows, setRows] = useState<ReportRow[]>([]); const [message, setMessage] = useState("");
  async function load(nextPage = page) { try { const params = { from, to, page: nextPage, size: 25, teacherId: teacherId || undefined, classroomId: classroomId || undefined }; const [summaryResponse, dataResponse] = await Promise.all([api.get<ReportSummary>("/reports/summary", { params }), api.get<ReportRow[]>(`/reports/${type}`, { params })]); setSummary(summaryResponse.data); setRows(dataResponse.data); setPage(nextPage); setMessage(""); } catch (e) { setMessage(errorText(e)); } }
  async function download(format: "csv" | "pdf") { try { const response = await api.get("/reports/export", { params: { type, format, from, to }, responseType: "blob" }); const url = URL.createObjectURL(response.data); const link = document.createElement("a"); link.href = url; link.download = `elneb-${type}.${format}`; link.click(); URL.revokeObjectURL(url); } catch (e) { setMessage(errorText(e)); } }
  const columns = rows.length ? Object.keys(rows[0]) : [];
  return <><article className="panel"><div className="page-intro"><div><h3>Reports and analytics</h3><p className="muted">Attendance, classroom utilization, teacher activity and device uptime.</p></div><div className="inline-form"><label>From<input type="date" value={from} onChange={e => setFrom(e.target.value)} /></label><label>To<input type="date" value={to} onChange={e => setTo(e.target.value)} /></label></div></div><div className="inline-form"><label>Report<select value={type} onChange={e => { setType(e.target.value); setPage(0); }}><option value="attendance">Attendance</option><option value="classroom-utilization">Classroom utilization</option><option value="teacher-activity">Teacher activity</option><option value="device-uptime">Device uptime</option></select></label><label>Teacher ID<input value={teacherId} onChange={e => setTeacherId(e.target.value)} placeholder="Optional UUID" /></label><label>Classroom ID<input value={classroomId} onChange={e => setClassroomId(e.target.value)} placeholder="Optional UUID" /></label><button onClick={() => load(0)}>Load report</button><button className="small-button" onClick={() => download("csv")} disabled={!rows.length}>CSV</button><button className="small-button" onClick={() => download("pdf")} disabled={!rows.length}>PDF</button></div>{message && <p className="error">{message}</p>}</article>{summary && <div className="stats"><article className="stat"><span>Sessions</span><strong>{summary.sessions}</strong></article><article className="stat"><span>Attendance records</span><strong>{summary.attendanceRecords}</strong></article><article className="stat"><span>Classrooms</span><strong>{summary.classrooms}</strong></article><article className="stat"><span>Devices online</span><strong>{summary.onlineDevices}/{summary.devices}</strong></article></div>}<article className="panel"><h3>{type.replace("-", " ")} data</h3>{rows.length ? <div className="report-table"><table><thead><tr>{columns.map(column => <th key={column}>{column}</th>)}</tr></thead><tbody>{rows.map((row, index) => <tr key={index}>{columns.map(column => <td key={column}>{Array.isArray(row[column]) ? (row[column] as string[]).join(", ") : String(row[column] ?? "-")}</td>)}</tr>)}</tbody></table><div className="inline-form"><button className="small-button" disabled={page === 0} onClick={() => load(page - 1)}>Previous</button><span className="muted">Page {page + 1}</span><button className="small-button" disabled={rows.length < 25} onClick={() => load(page + 1)}>Next</button></div></div> : <div className="empty-state"><b>No report loaded</b><p>Choose a date range and load a report.</p></div>}</article></>;
}

type AttendanceSession = { id: string; teacherId: string; sessionDate: string; startsAt: string; endsAt: string; status: string; classroomIds: string[] };
type AttendanceRecord = { id: string; sessionId: string; teacherId: string; cardUid: string; checkInAt: string; checkOutAt?: string; status: string };
type TimetableEntry = { id: string; classroomId: string; teacherId: string; dayOfWeek: string; subject: string; startsAt: string; endsAt: string; active: boolean };
type Device = { id: string; deviceCode: string; status: string; lastSeenAt?: string };
type ProvisionedDevice = Device & { setupToken: string; classroomId: string };
type LiveEvent = { type: string; data: { deviceCode?: string; status?: string; teacherId?: string } };

function LiveMonitoring() {
  const [events, setEvents] = useState<LiveEvent[]>([]); const [connected, setConnected] = useState(false); const [classroomId, setClassroomId] = useState(""); const [cameraUrl, setCameraUrl] = useState(""); const [sourceUrl, setSourceUrl] = useState(""); const [permissionUserId, setPermissionUserId] = useState(""); const [message, setMessage] = useState(""); const token = localStorage.getItem("elneb_token");
  useEffect(() => {
    if (!token) return;
    let socket: WebSocket | undefined; let cancelled = false;
    api.post<{ token: string }>("/monitoring/access-token", null, { params: { scope: "monitoring:ws" } }).then(({ data }) => {
      if (cancelled) return;
      socket = new WebSocket(`${location.protocol === "https:" ? "wss" : "ws"}://${location.host.replace(":5173", ":8080")}/ws/monitoring?token=${encodeURIComponent(data.token)}`);
      socket.onopen = () => setConnected(true); socket.onclose = () => setConnected(false);
      socket.onmessage = event => { try { setEvents(current => [JSON.parse(event.data), ...current].slice(0, 30)); } catch { setMessage("Received an invalid monitoring event."); } };
    }).catch(error => setMessage(errorText(error)));
    return () => { cancelled = true; socket?.close(); };
  }, [token]);
  async function showCamera() { if (!classroomId) return; try { const { data } = await api.post<{ token: string }>("/monitoring/access-token", null, { params: { scope: "camera:stream", classroomId } }); setCameraUrl(`${api.defaults.baseURL}/cameras/${classroomId}/stream?token=${encodeURIComponent(data.token)}`); } catch (e) { setMessage(errorText(e)); } }
  async function saveSource() { try { await api.put(`/cameras/${classroomId}/source`, { cameraUrl: sourceUrl }); setMessage("Camera source saved."); } catch (e) { setMessage(errorText(e)); } }
  async function grant() { try { await api.post(`/cameras/${classroomId}/permission`, { userId: permissionUserId }); setMessage("Camera permission granted."); } catch (e) { setMessage(errorText(e)); } }
  async function revoke() { try { await api.delete(`/cameras/${classroomId}/permission/${permissionUserId}`); setMessage("Camera permission revoked."); } catch (e) { setMessage(errorText(e)); } }
  return <div className="two-col"><article className="panel"><div className="page-intro"><div><h3>Live classroom monitoring</h3><p className="muted">Device heartbeats and teacher check-in events appear instantly.</p></div><span className={connected ? "success" : "error"}>{connected ? "● Connected" : "● Disconnected"}</span></div>{events.length ? events.map((event, index) => <div className="list-row" key={`${event.type}-${index}`}><div><b>{event.type.replace("_", " ")}</b><small>{event.data.deviceCode ?? "classroom"} · {event.data.status ?? "received"} · {new Date().toLocaleTimeString()}</small></div></div>) : <div className="empty-state"><b>Waiting for live events</b><p>Connect an ESP32 device to see heartbeats.</p></div>}</article><article className="panel form-panel"><h3>Camera access management</h3><label>Classroom ID<input value={classroomId} onChange={e => setClassroomId(e.target.value)} placeholder="Classroom UUID" /></label><label>Camera source URL<input value={sourceUrl} onChange={e => setSourceUrl(e.target.value)} placeholder="http://camera.local/stream" /></label><button onClick={saveSource} disabled={!classroomId || !sourceUrl}>Save camera source</button><label>User ID for permission<input value={permissionUserId} onChange={e => setPermissionUserId(e.target.value)} placeholder="User UUID" /></label><div className="inline-form"><button onClick={grant} disabled={!classroomId || !permissionUserId}>Grant access</button><button onClick={revoke} disabled={!classroomId || !permissionUserId}>Revoke access</button></div><button onClick={showCamera} disabled={!classroomId}>Open camera</button>{message && <p className={message.includes("saved") || message.includes("granted") || message.includes("revoked") ? "success" : "error"}>{message}</p>}{cameraUrl && <img className="camera-preview" src={cameraUrl} alt="Authorized classroom stream" onError={() => setCameraUrl("")} />}</article></div>;
}

function DeviceManager() {
  const [classroomId, setClassroomId] = useState(""); const [deviceCode, setDeviceCode] = useState(""); const [wifiSsid, setWifiSsid] = useState(""); const [wifiPassword, setWifiPassword] = useState(""); const [apiBaseUrl, setApiBaseUrl] = useState("http://YOUR_COMPUTER_IP:8080/api"); const [devices, setDevices] = useState<Device[]>([]); const [provisioned, setProvisioned] = useState<ProvisionedDevice | null>(null); const [message, setMessage] = useState("");
  async function load() { try { const { data } = await api.get<Device[]>(`/devices/classroom/${classroomId}`); setDevices(data); setMessage(""); } catch (e) { setMessage(errorText(e)); } }
  async function provision(event: React.FormEvent) { event.preventDefault(); try { const { data } = await api.post<ProvisionedDevice>("/devices", { classroomId, deviceCode }); setProvisioned(data); setMessage("Device provisioned. Save the setup token; it is required for firmware."); await load(); } catch (e) { setMessage(errorText(e)); } }
  async function downloadFirmware() { if (!provisioned) return; try { const response = await api.get(`/devices/${provisioned.id}/firmware`, { params: { setupToken: provisioned.setupToken, wifiSsid, wifiPassword, apiBaseUrl }, responseType: "blob" }); const url = URL.createObjectURL(response.data); const link = document.createElement("a"); link.href = url; link.download = `ELNEB_${provisioned.deviceCode}.ino`; link.click(); URL.revokeObjectURL(url); setMessage("Personalized firmware downloaded."); } catch (e) { setMessage(errorText(e)); } }
  return <div className="two-col"><article className="panel form-panel"><h3>Provision ESP32 device</h3><p className="muted">Create a device binding for one classroom. The setup token is shown only after provisioning.</p><form onSubmit={provision}><label>Classroom ID<input required value={classroomId} onChange={e => setClassroomId(e.target.value)} placeholder="Classroom UUID" /></label><label>Device code<input required value={deviceCode} onChange={e => setDeviceCode(e.target.value)} placeholder="ROOM-101-ESP32" /></label><label>Wi-Fi SSID<input required value={wifiSsid} onChange={e => setWifiSsid(e.target.value)} /></label><label>Wi-Fi password<input required type="password" value={wifiPassword} onChange={e => setWifiPassword(e.target.value)} /></label><label>Backend API URL<input required value={apiBaseUrl} onChange={e => setApiBaseUrl(e.target.value)} /></label><button type="submit">Provision device</button></form>{provisioned && <div className="checkout-summary"><p className="success">Device <strong>{provisioned.deviceCode}</strong> created.</p><p className="muted">Setup token: <code>{provisioned.setupToken}</code></p><button onClick={downloadFirmware}>Download personalized .ino</button></div>}{message && <p className={message.includes("successfully") || message.includes("provisioned") || message.includes("downloaded") ? "success" : "error"}>{message}</p>}</article><article className="panel"><div className="page-intro"><div><h3>Device health</h3><p className="muted">Use the same classroom ID to load its devices.</p></div><button className="small-button" onClick={load} disabled={!classroomId}>Refresh</button></div>{devices.length ? devices.map(device => <div className="list-row" key={device.id}><span className={device.status === "ONLINE" ? "status-dot" : "muted"}>●</span><div><b>{device.deviceCode}</b><small>{device.status} · {device.lastSeenAt ? new Date(device.lastSeenAt).toLocaleString() : "Never connected"}</small></div></div>) : <div className="empty-state"><b>No devices loaded</b><p>Provision an ESP32 device to see its health.</p></div>}</article></div>;
}

function TimetableManager() {
  const [classroomId, setClassroomId] = useState(""); const [teacherId, setTeacherId] = useState(""); const [dayOfWeek, setDayOfWeek] = useState("MONDAY"); const [subject, setSubject] = useState(""); const [startsAt, setStartsAt] = useState("09:00"); const [endsAt, setEndsAt] = useState("10:00"); const [editingId, setEditingId] = useState(""); const [items, setItems] = useState<TimetableEntry[]>([]); const [message, setMessage] = useState("");
  async function load() { try { const { data } = await api.get<TimetableEntry[]>(`/timetable/classrooms/${classroomId}`, { params: { day: dayOfWeek } }); setItems(data); setMessage(""); } catch (e) { setMessage(errorText(e)); } }
  async function create(event: React.FormEvent) { event.preventDefault(); try { const payload = { teacherId, dayOfWeek, subject, startsAt, endsAt }; if (editingId) { const { data } = await api.put<TimetableEntry>(`/timetable/${editingId}`, payload); setItems(items.map(item => item.id === editingId ? data : item)); setMessage("Timetable entry updated."); } else { await api.post(`/timetable/classrooms/${classroomId}`, payload); setMessage("Timetable entry created."); await load(); } setEditingId(""); setSubject(""); } catch (e) { setMessage(errorText(e)); } }
  function edit(item: TimetableEntry) { setEditingId(item.id); setClassroomId(item.classroomId); setTeacherId(item.teacherId); setDayOfWeek(item.dayOfWeek); setSubject(item.subject); setStartsAt(item.startsAt.slice(0, 5)); setEndsAt(item.endsAt.slice(0, 5)); }
  return <div className="two-col"><article className="panel form-panel"><h3>{editingId ? "Edit timetable entry" : "Add timetable entry"}</h3><p className="muted">Assign a teacher and subject to a classroom schedule.</p><form onSubmit={create}><label>Classroom ID<input required value={classroomId} onChange={e => setClassroomId(e.target.value)} placeholder="Classroom UUID" /></label><label>Teacher ID<input required value={teacherId} onChange={e => setTeacherId(e.target.value)} placeholder="Teacher UUID" /></label><label>Day<select value={dayOfWeek} onChange={e => setDayOfWeek(e.target.value)}>{["MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY","SUNDAY"].map(day => <option key={day}>{day}</option>)}</select></label><label>Subject<input required value={subject} onChange={e => setSubject(e.target.value)} /></label><div className="inline-form"><label>Starts<input type="time" required value={startsAt} onChange={e => setStartsAt(e.target.value)} /></label><label>Ends<input type="time" required value={endsAt} onChange={e => setEndsAt(e.target.value)} /></label></div><button type="submit">{editingId ? "Update timetable" : "Save timetable"}</button>{editingId && <button type="button" className="link-button" onClick={() => setEditingId("")}>Cancel edit</button>}{message && <p className={message.includes("created") || message.includes("updated") ? "success" : "error"}>{message}</p>}</form></article><article className="panel"><div className="page-intro"><div><h3>Weekly schedule</h3><p className="muted">Load entries for the selected classroom and day.</p></div><button className="small-button" onClick={load} disabled={!classroomId}>Refresh</button></div>{items.length ? items.map(item => <div className="list-row" key={item.id}><div><b>{item.subject}</b><small>{item.startsAt} - {item.endsAt} · Teacher {item.teacherId}</small></div><button className="small-button" onClick={() => edit(item)}>Edit</button><button className="small-button" onClick={async () => { try { await api.delete(`/timetable/${item.id}`); setItems(items.filter(row => row.id !== item.id)); setMessage("Timetable entry deactivated."); } catch (e) { setMessage(errorText(e)); } }}>Delete</button></div>) : <div className="empty-state"><b>No timetable entries loaded</b></div>}</article></div>;
}

function AttendanceCenter() {
  const [date, setDate] = useState(new Date().toISOString().slice(0, 10)); const [sessions, setSessions] = useState<AttendanceSession[]>([]);
  const [teacherId, setTeacherId] = useState(""); const [classroomIds, setClassroomIds] = useState(""); const [startsAt, setStartsAt] = useState("09:00"); const [endsAt, setEndsAt] = useState("10:00");
  const [selectedSession, setSelectedSession] = useState(""); const [checkInTeacher, setCheckInTeacher] = useState(""); const [cardUid, setCardUid] = useState(""); const [records, setRecords] = useState<AttendanceRecord[]>([]); const [message, setMessage] = useState("");
  async function loadSessions() { try { const { data } = await api.get<AttendanceSession[]>("/attendance/sessions", { params: { date } }); setSessions(data); setMessage(""); } catch (e) { setMessage(errorText(e)); } }
  async function createSession(event: React.FormEvent) { event.preventDefault(); try { await api.post("/attendance/sessions", { teacherId, classroomIds: classroomIds.split(",").map(value => value.trim()).filter(Boolean), sessionDate: date, startsAt: `${date}T${startsAt}`, endsAt: `${date}T${endsAt}` }); setMessage("Merged session created."); await loadSessions(); } catch (e) { setMessage(errorText(e)); } }
  async function loadRecords(sessionId: string) { setSelectedSession(sessionId); try { const { data } = await api.get<AttendanceRecord[]>(`/attendance/sessions/${sessionId}/records`); setRecords(data); } catch (e) { setMessage(errorText(e)); } }
  async function checkIn(event: React.FormEvent) { event.preventDefault(); try { await api.post("/attendance/check-in", { sessionId: selectedSession, teacherId: checkInTeacher, cardUid }); setMessage("Teacher checked in."); setCardUid(""); await loadRecords(selectedSession); } catch (e) { setMessage(errorText(e)); } }
  async function checkOut(recordId: string) { try { await api.post(`/attendance/${recordId}/check-out`); setMessage("Teacher checked out."); await loadRecords(selectedSession); } catch (e) { setMessage(errorText(e)); } }
  return <div className="two-col"><div><article className="panel form-panel"><h3>Create merged session</h3><p className="muted">Combine multiple classrooms into one session for the selected date.</p><form onSubmit={createSession}><label>Date<input type="date" required value={date} onChange={e => setDate(e.target.value)} /></label><label>Teacher ID<input required value={teacherId} onChange={e => setTeacherId(e.target.value)} placeholder="Teacher UUID" /></label><label>Classroom IDs<input required value={classroomIds} onChange={e => setClassroomIds(e.target.value)} placeholder="UUID, UUID" /></label><div className="inline-form"><label>Starts<input type="time" required value={startsAt} onChange={e => setStartsAt(e.target.value)} /></label><label>Ends<input type="time" required value={endsAt} onChange={e => setEndsAt(e.target.value)} /></label></div><button type="submit">Create session</button></form></article><article className="panel form-panel"><h3>RFID check-in</h3><form onSubmit={checkIn}><label>Session<select required value={selectedSession} onChange={e => { setSelectedSession(e.target.value); loadRecords(e.target.value); }}><option value="">Select session</option>{sessions.map(session => <option key={session.id} value={session.id}>{session.startsAt.slice(11, 16)} · {session.status}</option>)}</select></label><label>Teacher ID<input required value={checkInTeacher} onChange={e => setCheckInTeacher(e.target.value)} /></label><label>RFID card UID<input required value={cardUid} onChange={e => setCardUid(e.target.value)} placeholder="A1B2C3D4" /></label><button type="submit" disabled={!selectedSession}>Check in teacher</button></form></article></div><article className="panel"><div className="page-intro"><div><h3>Sessions</h3><p className="muted">Select a date and refresh to load attendance.</p></div><button className="small-button" onClick={loadSessions}>Refresh</button></div>{sessions.map(session => <button className="list-row" key={session.id} onClick={() => loadRecords(session.id)}><div><b>{session.startsAt.slice(11, 16)} - {session.endsAt.slice(11, 16)}</b><small>{session.status} · {session.classroomIds.length} classroom(s)</small></div></button>)}{selectedSession && <><h3>Attendance records</h3>{records.length ? records.map(record => <div className="list-row" key={record.id}><div><b>{record.cardUid}</b><small>{record.status} · {new Date(record.checkInAt).toLocaleTimeString()}</small></div>{!record.checkOutAt && <button className="small-button" onClick={() => checkOut(record.id)}>Check out</button>}</div>) : <div className="empty-state"><b>No attendance yet</b></div>}</>}{message && <p className={message.includes("successfully") || message.includes("created") || message.includes("checked") ? "success" : "error"}>{message}</p>}</article></div>;
}

function Subscription({ user }: { user: User }) {
  const [plans, setPlans] = useState<Plan[]>([]); const [code, setCode] = useState(""); const [quote, setQuote] = useState<Quote | null>(null); const [selected, setSelected] = useState<Plan | null>(null); const [message, setMessage] = useState(""); const [subscriptionStatus, setSubscriptionStatus] = useState<{status:string;monitoringEnabled:boolean;historicalViewOnly:boolean;endsAt?:string;trial?:boolean} | null>(null);
  const [tokenCode, setTokenCode] = useState(""); const [discountPercent, setDiscountPercent] = useState("10"); const [appliesTo, setAppliesTo] = useState("BOTH"); const [expiresAt, setExpiresAt] = useState(""); const [createdToken, setCreatedToken] = useState<DiscountToken | null>(null);
  useEffect(() => { api.get<Plan[]>("/subscriptions/plans").then(({ data }) => setPlans(data)).catch(e => setMessage(errorText(e))); api.get("/subscriptions/status").then(({ data }) => setSubscriptionStatus(data)).catch(e => setMessage(errorText(e))); }, []);
  async function calculate(planId: string) { try { const plan = plans.find(item => item.id === planId) ?? null; setSelected(plan); const { data } = await api.post<Quote>("/subscriptions/quote", { planId, discountCode: code }); setQuote(data); setMessage(""); } catch (e) { setQuote(null); setMessage(errorText(e)); } }
  async function checkout() {
    if (!selected) return;
    try {
      const { data } = await api.post<RazorpayOrder>("/payments/razorpay/orders", { planId: selected.id, discountCode: code, customerName: user.email.split("@")[0], customerEmail: user.email });
      if (!window.Razorpay) {
        await loadRazorpay();
      }
      if (!window.Razorpay) throw new Error("Razorpay checkout could not be loaded");
      const checkout = new window.Razorpay({
        key: data.keyId, amount: data.amount, currency: data.currency, name: "ELNEB",
        description: `${selected.name} subscription`, order_id: data.orderId,
        prefill: { name: data.customerName, email: data.customerEmail },
        theme: { color: "#4467e8" },
        handler: async (response: { razorpay_order_id: string; razorpay_payment_id: string; razorpay_signature: string }) => {
          await api.post("/payments/razorpay/verify", {
            razorpayOrderId: response.razorpay_order_id,
            razorpayPaymentId: response.razorpay_payment_id,
            razorpaySignature: response.razorpay_signature,
          });
          const status = await api.get("/subscriptions/status"); setSubscriptionStatus(status.data); setMessage("Payment verified and subscription activated successfully.");
        },
      });
      checkout.open();
    } catch (e) { setMessage(errorText(e)); }
  }
  async function createToken(event: React.FormEvent) {
    event.preventDefault();
    try {
      const { data } = await api.post<DiscountToken>("/subscriptions/discount-tokens", {
        code: tokenCode,
        discountPercent: Number(discountPercent),
        appliesTo,
        expiresAt: expiresAt ? new Date(`${expiresAt}T23:59:59`).toISOString() : null,
      });
      setCreatedToken(data); setTokenCode(""); setMessage("Discount token created successfully.");
    } catch (e) { setCreatedToken(null); setMessage(errorText(e)); }
  }
  return <><div className="page-intro"><div><h1>{user.role === "SUPER_ADMIN" ? "Subscription control" : "Choose your plan"}</h1><p className="muted">{user.role === "SUPER_ADMIN" ? "Manage plans and generate discount tokens for institutions." : "Start with a 15-day free trial. Upgrade whenever you are ready."}</p></div><span className="trial-badge">15 DAYS FREE</span></div>{subscriptionStatus && user.role !== "SUPER_ADMIN" && <article className="panel"><h3>Current subscription</h3><p className={subscriptionStatus.monitoringEnabled ? "success" : "error"}>{subscriptionStatus.status}{subscriptionStatus.trial ? " · 15-day trial" : ""}</p><small>{subscriptionStatus.monitoringEnabled ? `Monitoring enabled until ${subscriptionStatus.endsAt ? new Date(subscriptionStatus.endsAt).toLocaleDateString() : "renewal"}.` : "Monitoring is locked; historical data remains view-only."}</small></article>}{user.role === "SUPER_ADMIN" && <article className="panel form-panel"><h3>Generate discount token</h3><p className="muted">Create a coupon that institution admins can apply at checkout.</p><form onSubmit={createToken}><label>Token code<input required value={tokenCode} onChange={e => setTokenCode(e.target.value)} placeholder="WELCOME25" /></label><label>Discount percentage<input required type="number" min="0" max="100" step="0.01" value={discountPercent} onChange={e => setDiscountPercent(e.target.value)} /></label><label>Applies to<select value={appliesTo} onChange={e => setAppliesTo(e.target.value)}><option value="BOTH">Monthly and yearly</option><option value="MONTHLY">Monthly only</option><option value="YEARLY">Yearly only</option></select></label><label>Expiry date (optional)<input type="date" value={expiresAt} onChange={e => setExpiresAt(e.target.value)} /></label><button type="submit">Generate token</button></form>{createdToken && <p className="success">Token: <strong>{createdToken.code}</strong> · {createdToken.discountPercent}% off · {createdToken.appliesTo}</p>}</article>}{user.role !== "SUPER_ADMIN" && <><div className="plans">{plans.map(plan => <article className="plan-card" key={plan.id}><span className="plan-period">{plan.billingPeriod === "YEARLY" ? "BEST VALUE" : "FLEXIBLE"}</span><h3>{plan.name}</h3><strong>₹{plan.priceInr.toLocaleString("en-IN")}<small> / {plan.billingPeriod.toLowerCase()}</small></strong><p>Classroom monitoring, RFID attendance, device health and reports.</p><button onClick={() => calculate(plan.id)}>Choose plan</button></article>)}</div><article className="panel coupon-panel"><h3>Have a discount token?</h3><div className="inline-form"><input value={code} onChange={e => setCode(e.target.value)} placeholder="Enter token code" /><button disabled={!plans[0]} onClick={() => plans[0] && calculate(plans[0].id)}>Apply and calculate</button></div>{quote && <div className="checkout-summary"><p className="success">Payable: ₹{quote.payableAmount.toLocaleString("en-IN")} ({quote.discountPercent}% discount).</p><button onClick={checkout}>Continue to Razorpay</button></div>}</article></>}{message && <p className="error">{message}</p>}</>;
}

function loadRazorpay() {
  return new Promise<void>((resolve, reject) => {
    const existing = document.querySelector('script[src="https://checkout.razorpay.com/v1/checkout.js"]');
    if (window.Razorpay) { resolve(); return; }
    if (existing) {
      existing.addEventListener("load", () => resolve(), { once: true });
      existing.addEventListener("error", () => reject(new Error("Unable to load Razorpay checkout")), { once: true });
      return;
    }
    const script = document.createElement("script");
    script.src = "https://checkout.razorpay.com/v1/checkout.js";
    script.onload = () => resolve();
    script.onerror = () => reject(new Error("Unable to load Razorpay checkout"));
    document.body.appendChild(script);
  });
}

function Placeholder({ title, items }: { title: string; items: string[] }) { return <article className="panel feature-panel"><h1>{title}</h1><p className="muted">This workspace is ready for the connected backend module.</p><div className="feature-grid">{items.map(item => <div className="feature-tile" key={item}><span className="tile-icon">✦</span><b>{item}</b><small>Configure and manage from this workspace</small></div>)}</div></article>; }
function icon(item: string) { return ({ Overview: "⌂", Tenants: "◈", Colleges: "◈", Institutions: "▦", Users: "♙", Subscriptions: "◇", Subscription: "◇", Coupons: "✦", Settings: "⚙", Classrooms: "▤", Teachers: "♙", Timetable: "◫", "RFID & Attendance": "◉", Devices: "⌁", "Live Monitoring": "◉", Reports: "▥" } as Record<string, string>)[item] ?? "•"; }

createRoot(document.getElementById("root")!).render(<StrictMode><App /></StrictMode>);
