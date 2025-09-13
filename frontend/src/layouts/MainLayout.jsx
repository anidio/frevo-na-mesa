import React from 'react';
import { Outlet } from 'react-router-dom';
import Footer from '../components/Footer';
import BottomNav from '../components/BottomNav';
import Header from '../components/Header';

const MainLayout = () => {
  return (
    <div className="font-sans bg-tema-fundo dark:bg-tema-fundo-dark">
      <Header /> 
      <div id="page-container" className="relative min-h-screen pb-24">
        <main>
          <Outlet />
        </main>
        <Footer />
      </div>
      <BottomNav />
    </div>
  );
};

export default MainLayout;