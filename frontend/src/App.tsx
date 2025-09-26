import { useEffect, useState } from 'react';
import './App.css';

type PingResponse = {
  status: string;
};

function App() {
  const [ping, setPing] = useState<PingResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetch('/api/ping')
      .then(async (response) => {
        if (!response.ok) {
          throw new Error(`Request failed with status ${response.status}`);
        }
        const body = (await response.json()) as PingResponse;
        setPing(body);
      })
      .catch((err) => {
        setError(err.message);
      });
  }, []);

  return (
    <div className="app">
      <h1>BOB MTA Maintain Assistants</h1>
      <p>最简前后端联通性验证页面。</p>
      <section className="status-panel">
        <h2>后端连通性</h2>
        {ping && <p className="success">后端响应：{ping.status}</p>}
        {error && <p className="error">请求失败：{error}</p>}
        {!ping && !error && <p>检查后端连接中...</p>}
      </section>
    </div>
  );
}

export default App;
