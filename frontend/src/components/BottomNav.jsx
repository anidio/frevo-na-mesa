// src/components/BottomNav.jsx

import React, { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

// --- ÍCONES (sem alteração) ---
const HomeIcon = () => <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor"><path d="M10.707 2.293a1 1 0 00-1.414 0l-7 7a1 1 0 001.414 1.414L4 10.414V17a1 1 0 001 1h2a1 1 0 001-1v-2a1 1 0 011-1h2a1 1 0 011 1v2a1 1 0 001 1h2a1 1 0 001-1v-6.586l.293.293a1 1 0 001.414-1.414l-7-7z" /></svg>;
const UserIcon = () => <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor"><path fillRule="evenodd" d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z" clipRule="evenodd" /></svg>;
const CalculatorIcon = () => <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor"><path fillRule="evenodd" d="M6 2a2 2 0 00-2 2v12a2 2 0 002 2h8a2 2 0 002-2V4a2 2 0 00-2-2H6zm1 2a1 1 0 000 2h6a1 1 0 100-2H7zm6 4a1 1 0 10-2 0v6a1 1 0 102 0V8z" clipRule="evenodd" /></svg>;
const SettingsIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
    <path fillRule="evenodd" d="M11.49 3.17c-.38-1.56-2.6-1.56-2.98 0a1.532 1.532 0 01-2.286.948c-1.372-.836-2.942.734-2.106 2.106.54.886.061 2.042-.947 2.287-1.561.379-1.561 2.6 0 2.978a1.532 1.532 0 01.947 2.287c-.836 1.372.734 2.942 2.106 2.106a1.532 1.532 0 012.287.947c.379 1.561 2.6 1.561 2.978 0a1.532 1.532 0 012.287-.947c1.372.836 2.942-.734 2.106-2.106a1.532 1.532 0 01-.947-2.287c1.561-.379 1.561-2.6 0-2.978a1.532 1.532 0 01-.947-2.287c.836-1.372-.734-2.942-2.106-2.106a1.532 1.532 0 01-2.287-.947zM10 13a3 3 0 100-6 3 3 0 000 6z" clipRule="evenodd" />
  </svg>
);
const DeliveryIcon = () => <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor"><path d="M10 2a6 6 0 00-6 6c0 1.224.363 2.358 1 3.32V17.5a.5.5 0 00.5.5h1.5a.5.5 0 00.5-.5v-2.5a.5.5 0 01.5-.5h4a.5.5 0 01.5.5v2.5a.5.5 0 00.5.5h1.5a.5.5 0 00.5-.5v-6.18A6.002 6.002 0 0010 2zm0 8a2 2 0 110-4 2 2 0 010 4z" /></svg>;
const MoreIcon = () => <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><path strokeLinecap="round" strokeLinejoin="round" d="M5 12h.01M12 12h.01M19 12h.01M6 12a1 1 0 11-2 0 1 1 0 012 0zm7 0a1 1 0 11-2 0 1 1 0 012 0zm7 0a1 1 0 11-2 0 1 1 0 012 0z" /></svg>;


const NavItem = ({ icon, label, to, isHiddenOnMobile = false }) => {
  const location = useLocation();
  const isActive = location.pathname === to || (to !== "/" && location.pathname.startsWith(to));

  // CORREÇÃO APLICADA AQUI: flex-row para PC, flex-col para celular
  const baseClasses = "flex flex-col md:flex-row items-center justify-center p-2 rounded-full transition-all duration-300 min-w-[60px] md:min-w-fit md:gap-2 md:px-4 md:py-2";
  const activeClasses = "bg-tema-primary text-white shadow-md";
  const inactiveClasses = "text-tema-text-muted hover:bg-gray-200 dark:hover:bg-gray-700";

  return (
    <Link
      to={to}
      className={`${baseClasses} ${isActive ? activeClasses : inactiveClasses} ${isHiddenOnMobile ? 'hidden md:flex' : 'flex'}`}
    >
      {icon}
      {/* A classe para o nome também foi ajustada */}
      <span className="text-xs mt-1 md:mt-0 md:text-sm">{label}</span>
    </Link>
  );
};


const BottomNav = () => {
  const { userProfile } = useAuth();
  const [isMenuOpen, setIsMenuOpen] = useState(false);

  if (!userProfile) return null;

  const temMesas = userProfile.tipo === 'APENAS_MESAS' || userProfile.tipo === 'MESAS_E_DELIVERY';
  const temDelivery = userProfile.tipo === 'APENAS_DELIVERY' || userProfile.tipo === 'MESAS_E_DELIVERY';

  return (
    <div className="fixed bottom-6 left-1/2 -translate-x-1/2 z-10">
      <div className="flex items-center gap-2 bg-white/70 dark:bg-gray-800/70 backdrop-blur-sm p-2 rounded-full shadow-lg border dark:border-gray-700">
        <NavItem icon={<HomeIcon />} label="Home" to="/" />

        {temMesas && <NavItem icon={<UserIcon />} label="Garçom" to="/mesas" />}
        
        {temDelivery && <NavItem icon={<DeliveryIcon />} label="Delivery" to="/delivery" />}
        
        {/* BOTÕES VISÍVEIS APENAS EM TELAS GRANDES */}
        {temMesas && <NavItem icon={<CalculatorIcon />} label="Caixa" to="/caixa" isHiddenOnMobile={true} />}
        <NavItem icon={<SettingsIcon />} label="Admin" to="/admin" isHiddenOnMobile={true} />

        {/* BOTÃO PARA MOSTRAR MAIS OPÇÕES, VISÍVEL APENAS EM TELAS PEQUENAS */}
        <div className="relative md:hidden">
          <button 
            onClick={() => setIsMenuOpen(!isMenuOpen)}
            className="flex flex-col items-center justify-center p-2 rounded-full transition-all duration-300 min-w-[60px] text-tema-text-muted hover:bg-gray-200 dark:hover:bg-gray-700"
          >
            <MoreIcon />
            <span className="text-xs mt-1">Mais</span>
          </button>

          {isMenuOpen && (
            <div className="absolute bottom-full mb-2 right-0 bg-white dark:bg-gray-800 rounded-lg shadow-xl border dark:border-gray-700 p-2 min-w-[150px] space-y-1">
              {temMesas && (
                <Link to="/caixa" className="flex items-center gap-2 p-2 rounded-md hover:bg-gray-100 dark:hover:bg-gray-700 text-sm">
                  <CalculatorIcon /> Caixa
                </Link>
              )}
              <Link to="/admin" className="flex items-center gap-2 p-2 rounded-md hover:bg-gray-100 dark:hover:bg-gray-700 text-sm">
                <SettingsIcon /> Admin
              </Link>
            </div>
          )}
        </div>

      </div>
    </div>
  );
};

export default BottomNav;