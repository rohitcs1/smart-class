import { StrictMode, useEffect, useMemo, useState } from "react";
import { createRoot } from "react-dom/client";
import axios from "axios";
import "./styles.css";

type User = { token: string; email: string; role: string };
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
  return axios.isAxiosError(error) ? error.response?.data?.message ?? "Request failed" : "Something went wrong";
}

function App() {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  useEffect(() => {
    if (!localStorage.getItem("elneb_token")) return setLoading(false);
    api.get<User>("/auth/me").then(({ data }) => setUser(data)).catch(() => localStorage.removeItem("elneb_token")).finally(() => setLoading(false));
  }, []);
  if (loading) return <main className="center">Loading ELNEB...</main>;
  if (!user) return <Auth onSuccess={setUser} />;
  return <Dashboard user={user} onLogout={() => { localStorage.removeItem("elneb_token"); setUser(null); }} />;
}

function Auth({ onSuccess }: { onSuccess: (user: User) => void }) {
  const [registering, setRegistering] = useState(false);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");
  async function submit(event: React.FormEvent) {
    event.preventDefault(); setMessage("");
    try {
      const { data } = await api.post<User>(registering ? "/auth/register" : "/auth/login", { email, password });
      localStorage.setItem("elneb_token", data.token); onSuccess(data);
    } catch (error) { setMessage(errorText(error)); }
  }
  return <main className="auth"><form className="auth-card" onSubmit={submit}>
    <div className="brand-mark">E</div><p className="eyebrow">ELNEB SMART CLASSROOM</p>
    <h1>{registering ? "Create your workspace" : "Welcome back"}</h1>
    <p className="muted">{registering ? "Start your institution's 15-day free trial." : "Sign in to manage your smart campus."}</p>
    <label>Email<input type="email" required value={email} onChange={e => setEmail(e.target.value)} /></label>
    <label>Password<input type="password" minLength={8} required value={password} onChange={e => setPassword(e.target.value)} /></label>
    {message && <p className="error">{message}</p>}<button type="submit">{registering ? "Create account" : "Sign in"}</button>
    <button className="link-button" type="button" onClick={() => { setRegistering(!registering); setMessage(""); }}>{registering ? "Already registered? Sign in" : "Create institution account"}</button>
  </form></main>;
}

function Dashboard({ user, onLogout }: { user: User; onLogout: () => void }) {
  const [section, setSection] = useState("Overview");
  const role = user.role.replace(/_/g, " ");
  const menu = user.role === "SUPER_ADMIN"
    ? ["Overview", "Tenants", "Institutions", "Subscriptions"]
    : ["Overview", "Institutions", "Classrooms", "Teachers", "RFID & Attendance", "Devices", "Reports", "Subscription"];
  return <main className="app-shell">
    <aside><div className="logo"><span className="logo-icon">E</span> ELNEB</div><p className="sidebar-label">WORKSPACE</p>
      {menu.map(item => <button key={item} className={section === item ? "nav-item active" : "nav-item"} onClick={() => setSection(item)}><span>{icon(item)}</span>{item}</button>)}
      <div className="sidebar-bottom"><span className="avatar">{user.email[0].toUpperCase()}</span><div><b>{user.email.split("@")[0]}</b><small>{role}</small></div><button className="logout" onClick={onLogout}>↪</button></div>
    </aside>
    <section className="main-panel"><header className="topbar"><div><p className="eyebrow">ELNEB CONTROL CENTER</p><h2>{section}</h2></div><span className="status-dot">● System operational</span></header>
      <div className="content"><Section section={section} user={user} /></div>
    </section>
  </main>;
}

function Section({ section, user }: { section: string; user: User }) {
  if (section === "Subscriptions" || section === "Subscription") return <Subscription user={user} />;
  if (section === "Tenants") return <TenantManager />;
  if (section === "Institutions") return <InstitutionManager user={user} />;
  if (section === "Classrooms") return <Placeholder title="Classroom management" items={["Campus structure", "Buildings and floors", "Room capacity", "Live occupancy"]} />;
  if (section === "Teachers") return <Placeholder title="Teacher management" items={["Teacher profiles", "Departments", "RFID cards", "Assignments"]} />;
  if (section === "RFID & Attendance") return <Placeholder title="Attendance center" items={["Today's check-ins", "Merged sessions", "Late arrivals", "Attendance reports"]} />;
  if (section === "Devices") return <Placeholder title="IoT device health" items={["Online devices", "Offline alerts", "Provision ESP32", "Download firmware"]} />;
  if (section === "Reports") return <Placeholder title="Reports and analytics" items={["Classroom utilization", "Teacher activity", "Device uptime", "Export CSV"]} />;
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
  const [tenantId, setTenantId] = useState(""); const [name, setName] = useState(""); const [code, setCode] = useState(""); const [address, setAddress] = useState(""); const [items, setItems] = useState<Institution[]>([]); const [message, setMessage] = useState("");
  async function create(event: React.FormEvent) { event.preventDefault(); try { const { data } = await api.post<Institution>(`/institutions/tenant/${tenantId}`, { name, code, address }); setItems([...items, data]); setMessage("Institution created."); } catch (e) { setMessage(errorText(e)); } }
  return <div className="two-col"><article className="panel form-panel"><h3>Add institution</h3><p className="muted">Connect an institution to a tenant workspace.</p><form onSubmit={create}><label>Tenant ID<input required value={tenantId} onChange={e => setTenantId(e.target.value)} placeholder="UUID from tenant setup" /></label><label>Institution name<input required value={name} onChange={e => setName(e.target.value)} /></label><label>Code<input required value={code} onChange={e => setCode(e.target.value)} /></label><label>Address<input value={address} onChange={e => setAddress(e.target.value)} /></label><button>Create institution</button>{message && <p className="success">{message}</p>}</form></article><article className="panel"><h3>Institutions</h3>{items.length ? items.map(item => <div className="list-row" key={item.id}><span className="avatar">{item.name[0]}</span><div><b>{item.name}</b><small>{item.code} · {item.active ? "Active" : "Inactive"}</small></div></div>) : <div className="empty-state"><span className="empty-icon">▦</span><b>No institutions loaded</b><p>Use the form to create your first institution.</p></div>}</article></div>;
}

function Subscription({ user }: { user: User }) {
  const [plans, setPlans] = useState<Plan[]>([]); const [code, setCode] = useState(""); const [quote, setQuote] = useState<Quote | null>(null); const [selected, setSelected] = useState<Plan | null>(null); const [message, setMessage] = useState("");
  const [tokenCode, setTokenCode] = useState(""); const [discountPercent, setDiscountPercent] = useState("10"); const [appliesTo, setAppliesTo] = useState("BOTH"); const [expiresAt, setExpiresAt] = useState(""); const [createdToken, setCreatedToken] = useState<DiscountToken | null>(null);
  useEffect(() => { api.get<Plan[]>("/subscriptions/plans").then(({ data }) => setPlans(data)).catch(e => setMessage(errorText(e))); }, []);
  async function calculate(planId: string) { try { const plan = plans.find(item => item.id === planId) ?? null; setSelected(plan); const { data } = await api.post<Quote>("/subscriptions/quote", { planId, discountCode: code }); setQuote(data); setMessage(""); } catch (e) { setQuote(null); setMessage(errorText(e)); } }
  async function checkout() {
    if (!selected) return;
    try {
      const { data } = await api.post<RazorpayOrder>("/payments/razorpay/orders", { planId: selected.id, customerName: user.email.split("@")[0], customerEmail: user.email });
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
          setMessage("Payment verified successfully. Subscription activation will be completed by the backend.");
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
  return <><div className="page-intro"><div><h1>{user.role === "SUPER_ADMIN" ? "Subscription control" : "Choose your plan"}</h1><p className="muted">{user.role === "SUPER_ADMIN" ? "Manage plans and generate discount tokens for institutions." : "Start with a 15-day free trial. Upgrade whenever you are ready."}</p></div><span className="trial-badge">15 DAYS FREE</span></div>{user.role === "SUPER_ADMIN" && <article className="panel form-panel"><h3>Generate discount token</h3><p className="muted">Create a coupon that institution admins can apply at checkout.</p><form onSubmit={createToken}><label>Token code<input required value={tokenCode} onChange={e => setTokenCode(e.target.value)} placeholder="WELCOME25" /></label><label>Discount percentage<input required type="number" min="0" max="100" step="0.01" value={discountPercent} onChange={e => setDiscountPercent(e.target.value)} /></label><label>Applies to<select value={appliesTo} onChange={e => setAppliesTo(e.target.value)}><option value="BOTH">Monthly and yearly</option><option value="MONTHLY">Monthly only</option><option value="YEARLY">Yearly only</option></select></label><label>Expiry date (optional)<input type="date" value={expiresAt} onChange={e => setExpiresAt(e.target.value)} /></label><button type="submit">Generate token</button></form>{createdToken && <p className="success">Token: <strong>{createdToken.code}</strong> · {createdToken.discountPercent}% off · {createdToken.appliesTo}</p>}</article>}{user.role !== "SUPER_ADMIN" && <><div className="plans">{plans.map(plan => <article className="plan-card" key={plan.id}><span className="plan-period">{plan.billingPeriod === "YEARLY" ? "BEST VALUE" : "FLEXIBLE"}</span><h3>{plan.name}</h3><strong>₹{plan.priceInr.toLocaleString("en-IN")}<small> / {plan.billingPeriod.toLowerCase()}</small></strong><p>Classroom monitoring, RFID attendance, device health and reports.</p><button onClick={() => calculate(plan.id)}>Choose plan</button></article>)}</div><article className="panel coupon-panel"><h3>Have a discount token?</h3><div className="inline-form"><input value={code} onChange={e => setCode(e.target.value)} placeholder="Enter token code" /><button disabled={!plans[0]} onClick={() => plans[0] && calculate(plans[0].id)}>Apply and calculate</button></div>{quote && <div className="checkout-summary"><p className="success">Payable: ₹{quote.payableAmount.toLocaleString("en-IN")} ({quote.discountPercent}% discount).</p><button onClick={checkout}>Continue to Razorpay</button></div>}</article></>}{message && <p className="error">{message}</p>}</>;
}

function loadRazorpay() {
  return new Promise<void>((resolve, reject) => {
    const existing = document.querySelector('script[src="https://checkout.razorpay.com/v1/checkout.js"]');
    if (existing) { existing.addEventListener("load", () => resolve()); return; }
    const script = document.createElement("script");
    script.src = "https://checkout.razorpay.com/v1/checkout.js";
    script.onload = () => resolve();
    script.onerror = () => reject(new Error("Unable to load Razorpay checkout"));
    document.body.appendChild(script);
  });
}

function Placeholder({ title, items }: { title: string; items: string[] }) { return <article className="panel feature-panel"><h1>{title}</h1><p className="muted">This workspace is ready for the connected backend module.</p><div className="feature-grid">{items.map(item => <div className="feature-tile" key={item}><span className="tile-icon">✦</span><b>{item}</b><small>Configure and manage from this workspace</small></div>)}</div></article>; }
function icon(item: string) { return ({ Overview: "⌂", Tenants: "◈", Institutions: "▦", Subscriptions: "◇", Subscription: "◇", Classrooms: "▤", Teachers: "♙", "RFID & Attendance": "◉", Devices: "⌁", Reports: "▥" } as Record<string, string>)[item] ?? "•"; }

createRoot(document.getElementById("root")!).render(<StrictMode><App /></StrictMode>);
