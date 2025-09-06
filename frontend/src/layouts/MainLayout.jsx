import React from 'react';
import { Outlet } from 'react-router-dom';
import Footer from '../components/Footer';
import BottomNav from '../components/BottomNav';

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
    </div>
  );
};

export default MainLayout;