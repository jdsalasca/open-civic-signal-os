export function App() {
  const cards = [
    { title: "New Signals", value: "128" },
    { title: "Top Unresolved", value: "10" },
    { title: "In Progress", value: "24" },
    { title: "Resolved", value: "52" }
  ];

  return (
    <main className="page">
      <header>
        <h1>Open Civic Signal OS</h1>
        <p>Transparent civic prioritization dashboard (React shell).</p>
      </header>
      <section className="grid">
        {cards.map((card) => (
          <article key={card.title} className="card">
            <h2>{card.value}</h2>
            <p>{card.title}</p>
          </article>
        ))}
      </section>
    </main>
  );
}
