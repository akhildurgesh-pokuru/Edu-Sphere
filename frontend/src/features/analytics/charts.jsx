import { Bar, BarChart, CartesianGrid, Cell, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';

export const PALETTE = ['#4f46e5', '#8b5cf6', '#10b981', '#0ea5e9', '#f59e0b', '#ec4899'];
const axis = { stroke: 'hsl(var(--muted-foreground))', fontSize: 12, tickLine: false, axisLine: false };

function ChartTooltip({ active, payload, label, suffix = '' }) {
  if (!active || !payload?.length) return null;
  return (
    <div className="rounded-lg border bg-popover px-3 py-2 text-xs shadow-lg">
      <p className="mb-1 font-semibold">{label}</p>
      {payload.map((p) => <p key={p.dataKey} style={{ color: p.color || p.fill }}>{p.name}: <strong>{p.value}{suffix}</strong></p>)}
    </div>
  );
}

const short = (s, n = 14) => (s && s.length > n ? `${s.slice(0, n)}…` : s);

export function BarsChart({ data, xKey, yKey, name, suffix = '', height = 260, domain }) {
  return (
    <div style={{ height }} role="img" aria-label={`${name} bar chart`}>
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={data} margin={{ top: 8, right: 8, left: -16, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" vertical={false} />
          <XAxis dataKey={xKey} {...axis} tickFormatter={(v) => short(String(v))} />
          <YAxis {...axis} allowDecimals={false} domain={domain} />
          <Tooltip content={<ChartTooltip suffix={suffix} />} cursor={{ fill: 'hsl(var(--muted))' }} />
          <Bar dataKey={yKey} name={name} radius={[8, 8, 0, 0]} maxBarSize={48}>
            {data.map((_, i) => <Cell key={i} fill={PALETTE[i % PALETTE.length]} />)}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}

export function TrendChart({ data, xKey, yKey, name, height = 260 }) {
  return (
    <div style={{ height }} role="img" aria-label={`${name} line chart`}>
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={data} margin={{ top: 8, right: 8, left: -16, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" vertical={false} />
          <XAxis dataKey={xKey} {...axis} tickFormatter={(v) => String(v).slice(5)} interval="preserveStartEnd" minTickGap={24} />
          <YAxis {...axis} allowDecimals={false} />
          <Tooltip content={<ChartTooltip />} />
          <Line type="monotone" dataKey={yKey} name={name} stroke="#4f46e5" strokeWidth={3} dot={false} activeDot={{ r: 5 }} />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
