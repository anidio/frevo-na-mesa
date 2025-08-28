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
import LoginPage from './Pages/LoginPage.jsx';
import RegisterPage from './Pages/RegisterPage.jsx';
import ProtectedRoute from './components/ProtectedRoute.jsx'; // 1. Importe o ProtectedRoute

const App = () => {
  return (
    <BrowserRouter>
      <Routes>
        {/* Rotas públicas que não usam o MainLayout */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/registrar" element={<RegisterPage />} />

        {/* 2. Rotas protegidas que precisam de login */}
        <Route element={<ProtectedRoute />}>
          <Route path="/" element={<MainLayout />}>
            <Route index element={<HomePage />} />
            <Route path="mesas" element={<MesasPage />} />
            <Route path="mesas/:id" element={<DetalheMesaPage />} />
            <Route path="caixa" element={<CaixaPage />} />
            <Route path="admin" element={<AdminPage />} />
            <Route path="admin/cardapio" element={<GerenciarCardapioPage />} />
            <Route path="admin/relatorios" element={<RelatorioPage />} />
          </Route>
        </Route>
      </Routes>
    </BrowserRouter>
  );
};

export default App;