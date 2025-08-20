// src/components/BottomNav.jsx

import React from 'react';
// 1. O useState não é mais necessário aqui
import { Link, useLocation } from 'react-router-dom';

// Ícones simples para o menu (Heroicons)
const HomeIcon = () => <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor"><path d="M10.707 2.293a1 1 0 00-1.414 0l-7 7a1 1 0 001.414 1.414L4 10.414V17a1 1 0 001 1h2a1 1 0 001-1v-2a1 1 0 011-1h2a1 1 0 011 1v2a1 1 0 001 1h2a1 1 0 001-1v-6.586l.293.293a1 1 0 001.414-1.414l-7-7z" /></svg>;
const UserIcon = () => <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor"><path fillRule="evenodd" d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z" clipRule="evenodd" /></svg>;
const CalculatorIcon = () => <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor"><path fillRule="evenodd" d="M6 2a2 2 0 00-2 2v12a2 2 0 002 2h8a2 2 0 002-2V4a2 2 0 00-2-2H6zm1 2a1 1 0 000 2h6a1 1 0 100-2H7zm6 4a1 1 0 10-2 0v6a1 1 0 102 0V8z" clipRule="evenodd" /></svg>;

const SettingsIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
    <path fillRule="evenodd" d="M11.49 3.17c-.38-1.56-2.6-1.56-2.98 0a1.532 1.532 0 01-2.286.948c-1.372-.836-2.942.734-2.106 2.106.54.886.061 2.042-.947 2.287-1.561.379-1.561 2.6 0 2.978a1.532 1.532 0 01.947 2.287c-.836 1.372.734 2.942 2.106 2.106a1.532 1.532 0 012.287.947c.379 1.561 2.6 1.561 2.978 0a1.532 1.532 0 012.287-.947c1.372.836 2.942-.734 2.106-2.106a1.532 1.532 0 01-.947-2.287c1.561-.379 1.561-2.6 0-2.978a1.532 1.532 0 01-.947-2.287c.836-1.372-.734-2.942-2.106-2.106a1.532 1.532 0 01-2.287-.947zM10 13a3 3 0 100-6 3 3 0 000 6z" clipRule="evenodd" />
  </svg>
);

// --- AQUI ESTÁ A MUDANÇA PRINCIPAL ---
const NavItem = ({ icon, label, to }) => {
  // 2. Usamos o hook useLocation para saber a URL atual
  const location = useLocation();
  // 3. O item é considerado "ativo" se a URL atual for a mesma do link
  const isActive = location.pathname === to;

  const baseClasses = "flex items-center justify-center gap-2 px-4 py-2 rounded-full transition-all duration-300";
  const activeClasses = "bg-orange-500 text-white shadow-md";
  const inactiveClasses = "text-gray-500 hover:bg-gray-200";

  return (
    // 4. Agora o NavItem é um Link e usa a prop 'to'
    <Link
      to={to}
      className={`${baseClasses} ${isActive ? activeClasses : inactiveClasses}`}
    >
      {icon}
      <span>{label}</span>
    </Link>
  );
};


const BottomNav = () => {
  return (
    <div className="fixed bottom-6 left-1/2 -translate-x-1/2">
      <div className="flex items-center gap-2 bg-white p-2 rounded-full shadow-lg">
        <NavItem icon={<HomeIcon />} label="Home" to="/" />
        <NavItem icon={<UserIcon />} label="Garçom" to="/mesas" />
        <NavItem icon={<CalculatorIcon />} label="Caixa" to="/caixa" />
        <NavItem icon={<SettingsIcon />} label="Admin" to="/admin" />
      </div>
    </div>
  );
};

export default BottomNav;