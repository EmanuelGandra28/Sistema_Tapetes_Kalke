// Gráficos simples e leves (SVG/CSS, sem biblioteca): usam as cores do tema ativo e têm texto para leitores de tela.
export const CORES = ['var(--primary)', 'var(--accent)', 'var(--success)', 'var(--warning)', 'var(--danger)'];

// Barras horizontais: um rótulo, a barra e o valor. `dados` = [{ rotulo, valor, cor? }].
export function GraficoBarras({ dados, formato = String, vazio = 'Sem dados no período.' }) {
  const maior = Math.max(...dados.map((d) => Math.abs(Number(d.valor))), 0);
  if (!dados.length || maior === 0) return <p className="hint">{vazio}</p>;
  return <ul className="grafico-barras">{dados.map((d, i) => <li key={d.rotulo}>
    <span className="rotulo" title={d.rotulo}>{d.rotulo}</span>
    <span className="trilho"><span className="barra" style={{ width: `${(Math.abs(Number(d.valor)) / maior) * 100}%`, background: d.cor ?? CORES[i % CORES.length] }} /></span>
    <strong className="valor">{formato(d.valor)}</strong>
  </li>)}</ul>;
}

// Linha com área preenchida (evolução no tempo). `dados` = [{ rotulo, valor }]; o rótulo aparece ao passar o mouse.
export function GraficoLinha({ dados, formato = String, titulo }) {
  const L = 640, A = 200, mx = 8, topo = 12, base = 24;
  const maior = Math.max(...dados.map((d) => Number(d.valor)), 0);
  if (!dados.length || maior === 0) return <p className="hint">Sem vendas no período.</p>;
  const x = (i) => mx + (dados.length === 1 ? (L - 2 * mx) / 2 : (i * (L - 2 * mx)) / (dados.length - 1));
  const y = (v) => topo + (A - topo - base) * (1 - Number(v) / maior);
  const pontos = dados.map((d, i) => `${x(i).toFixed(1)},${y(d.valor).toFixed(1)}`);
  const area = `${x(0)},${A - base} ${pontos.join(' ')} ${x(dados.length - 1)},${A - base}`;
  const marcas = [0, Math.floor((dados.length - 1) / 2), dados.length - 1].filter((v, i, a) => a.indexOf(v) === i);
  return <svg className="grafico-linha" viewBox={`0 0 ${L} ${A}`} role="img" aria-label={titulo}>
    <title>{titulo}</title>
    {[0, 0.5, 1].map((f) => <line key={f} x1={mx} x2={L - mx} y1={y(maior * f)} y2={y(maior * f)} className="grade" />)}
    <polygon points={area} className="area" />
    <polyline points={pontos.join(' ')} className="linha" fill="none" />
    {dados.map((d, i) => <circle key={d.rotulo} cx={x(i)} cy={y(d.valor)} r={dados.length > 40 ? 3 : 4} className="ponto"><title>{`${d.rotulo}: ${formato(d.valor)}`}</title></circle>)}
    {marcas.map((i) => <text key={i} x={x(i)} y={A - 6} textAnchor={i === 0 ? 'start' : i === dados.length - 1 ? 'end' : 'middle'} className="eixo">{dados[i].rotulo}</text>)}
    <text x={L - mx} y={topo - 2} textAnchor="end" className="eixo">máx. {formato(maior)}</text>
  </svg>;
}

// Rosca com legenda: participação de cada item no total. `dados` = [{ rotulo, valor }].
export function GraficoRosca({ dados, formato = String }) {
  const total = dados.reduce((s, d) => s + Number(d.valor), 0);
  if (!dados.length || total === 0) return <p className="hint">Sem vendas no período.</p>;
  const R = 15.9155; // circunferência 100
  let acumulado = 0;
  return <div className="grafico-rosca">
    <svg viewBox="0 0 42 42" role="img" aria-label="Participação de cada forma de pagamento nas vendas">
      <circle cx="21" cy="21" r={R} fill="none" className="fundo" strokeWidth="6" />
      {dados.map((d, i) => { const parte = (Number(d.valor) / total) * 100; const el = <circle key={d.rotulo} cx="21" cy="21" r={R} fill="none" strokeWidth="6"
        stroke={CORES[i % CORES.length]} strokeDasharray={`${parte} ${100 - parte}`} strokeDashoffset={25 - acumulado}><title>{`${d.rotulo}: ${formato(d.valor)}`}</title></circle>; acumulado += parte; return el; })}
    </svg>
    <ul>{dados.map((d, i) => <li key={d.rotulo}><span className="cor" style={{ background: CORES[i % CORES.length] }} />{d.rotulo}<strong>{((Number(d.valor) / total) * 100).toFixed(0)}%</strong><small>{formato(d.valor)}</small></li>)}</ul>
  </div>;
}
