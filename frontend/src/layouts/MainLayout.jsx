import React from 'react';
import { Outlet } from 'react-router-dom';
import Footer from '../components/Footer';
import BottomNav from '../components/BottomNav';
import { ToastContainer } from 'react-toastify';

const MainLayout = () => {
  return (
    <div className="bg-orange-50 font-sans">
      <div id="page-container" className="relative min-h-screen pb-24">
        <main>
          {/*
            O <Outlet /> é um portal mágico do React Router.
            É aqui que o conteúdo da página atual (HomePage, MesasPage, etc.)
            será renderizado.
          */}
          <Outlet />
        </main>
        <Footer />
      </div>
      <BottomNav />
      <ToastContainer
        position="top-right"
        autoClose={3000} // Fecha automaticamente após 3 segundos
        hideProgressBar={false}
        newestOnTop={false}
        closeOnClick
        rtl={false}
        pauseOnFocusLoss
        draggable
        pauseOnHover
        theme="light"
      />
    </div>
  );
};

export default MainLayout;