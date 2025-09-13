// src/App.jsx
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
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
import ProtectedRoute from './components/ProtectedRoute.jsx';
import NovoPedidoDeliveryPage from './Pages/NovoPedidoDeliveryPage.jsx';
import DeliveryPage from './Pages/DeliveryPage.jsx';

const App = () => {
  return (
    <BrowserRouter>
      <Routes>
        {/* Rotas 100% públicas que não usam o layout principal */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/registrar" element={<RegisterPage />} />

        {/* Rotas que usam o layout principal */}
        <Route path="/" element={<MainLayout />}>
          {/* A página inicial é pública e renderizada diretamente */}
          <Route index element={<HomePage />} />

          {/* As rotas a seguir são aninhadas dentro do ProtectedRoute */}
          {/* Elas só serão acessíveis se o usuário estiver logado */}
          <Route element={<ProtectedRoute />}>
            <Route path="mesas" element={<MesasPage />} />
            <Route path="mesas/:id" element={<DetalheMesaPage />} />
            <Route path="caixa" element={<CaixaPage />} />
            <Route path="delivery" element={<DeliveryPage />} />
            <Route path="delivery/novo" element={<NovoPedidoDeliveryPage />} />
            <Route path="admin" element={<AdminPage />} />
            <Route path="admin/cardapio" element={<GerenciarCardapioPage />} />
            <Route path="admin/relatorios" element={<RelatorioPage />} />
          </Route>
        </Route>
      </Routes>
      <ToastContainer
        position="top-right"
        autoClose={3000}
        hideProgressBar={false}
        newestOnTop={false}
        closeOnClick
        rtl={false}
        pauseOnFocusLoss
        draggable
        pauseOnHover
        theme="light"
      />
    </BrowserRouter>
  );
};

export default App;