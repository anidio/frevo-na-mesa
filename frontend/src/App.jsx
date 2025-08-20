// src/App.jsx
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import MainLayout from './layouts/MainLayout';
import MesasPage from './Pages/MesasPage.jsx';
import HomePage from './Pages/HomePage.jsx';
import DetalheMesaPage from './Pages/DetalheMesaPage.jsx';
import CaixaPage from './Pages/CaixaPage.jsx';
import GerenciarCardapioPage from './Pages/GerenciarCardapioPage.jsx';
import RelatorioPage from './Pages/RelatorioPage.jsx';
import AdminPage from './Pages/AdminPage.jsx';

const App = () => {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<MainLayout />}>
          <Route index element={<HomePage />} />
          <Route path="mesas" element={<MesasPage />} /> 
          <Route path="mesas/:id" element={<DetalheMesaPage />} />
          <Route path="caixa" element={<CaixaPage />} />
          <Route path="admin" element={<AdminPage />} />
          <Route path="admin/cardapio" element={<GerenciarCardapioPage />} />
          <Route path="admin/relatorios" element={<RelatorioPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
};

export default App;